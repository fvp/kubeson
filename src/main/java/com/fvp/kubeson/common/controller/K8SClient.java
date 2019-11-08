package com.fvp.kubeson.common.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.fvp.kubeson.Configuration;
import com.fvp.kubeson.common.model.ItemType;
import com.fvp.kubeson.common.model.K8SConfigMap;
import com.fvp.kubeson.common.model.K8SPod;
import com.fvp.kubeson.common.model.SelectorItem;
import com.fvp.kubeson.common.util.ThreadFactory;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class K8SClient {

    private static Logger LOGGER = LogManager.getLogger();

    private static Random random;

    private static KubernetesClient client;

    private static Map<String, K8SPod> pods;

    private static Map<String, K8SConfigMap> configMaps;

    private static List<K8SClientListener> k8sListeners;

    private static int k8sClientGetPodsAttempts;

    private static int k8sClientGetConfigMapsAttempts;

    static {
        random = new Random();
        pods = new ConcurrentHashMap<>();
        configMaps = new ConcurrentHashMap<>();
        k8sListeners = new ArrayList<>();
        k8sClientGetPodsAttempts = 0;
        k8sClientGetConfigMapsAttempts = 0;
        startK8SWorker();
    }

    private K8SClient() {
    }

    private static void startK8SWorker() {
        ThreadFactory.newThread(() -> {
            try {
                startClient();
                for (; ; ) {
                    TimeUnit.MILLISECONDS.sleep(Configuration.KUBERNETES_WORKER_WAIT_TIME_MS);
                    updatePods();
                    updateConfigMaps();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.info("Kubernetes Thread Interrupted");
            }
            LOGGER.info("Kubernetes Thread Completed, Closing Kubernetes Client");
            client.close();
            LOGGER.info("Kubernetes Client Closed");
        });
    }

    private static void startClient() throws InterruptedException {
        int sleepInterval = 8 * Configuration.KUBERNETES_WORKER_WAIT_TIME_MS;
        for (; ; ) {
            client = new DefaultKubernetesClient();
            try {
                client.getVersion();
                return;
            } catch (KubernetesClientException e) {
                LOGGER.error("Kubeson was not able to connect to Kubernetes. Retrying in {} ms", sleepInterval);
            }
            TimeUnit.MILLISECONDS.sleep(sleepInterval);
        }
    }

    private static void updatePods() {
        K8SResourceChange<K8SPod> changes = new K8SResourceChange<>();

        try {
            final long flag = random.nextLong();

            // Add New Pods
            client.pods().list().getItems().forEach(pod -> {
                if (!K8SPod.STATUS_PENDING.equals(pod.getStatus().getPhase())) {
                    K8SPod oldPod = pods.get(pod.getMetadata().getUid());
                    if (oldPod != null) {
                        oldPod.setFlag(flag);
                        if (!oldPod.getState().equals(pod.getStatus().getPhase())) {
                            oldPod.setState(pod.getStatus().getPhase());
                            changes.resourceUpdated(oldPod);
                        }
                    } else {
                        K8SPod newPod = new K8SPod(pod, flag);
                        setMetricsNodePort(newPod);
                        pods.put(pod.getMetadata().getUid(), newPod);
                        changes.resourceAdded(newPod);
                    }
                }
            });

            // Terminate Pods
            pods.forEach((uid, pod) -> {
                if (pod.getFlag() != flag) {
                    pod.terminate();
                    changes.resourceRemoved(pod);
                    pods.remove(uid);
                }
            });
        } catch (Exception e) {
            if (k8sClientGetPodsAttempts > Configuration.MAX_KUBERNETES_CLIENT_ATTEMPTS) {
                LOGGER.error("Failed to get kubernetes pod info after " + Configuration.MAX_KUBERNETES_CLIENT_ATTEMPTS + " attempts. Stopping all pods", e);
                pods.forEach((uid, pod) -> {
                    changes.resourceRemoved(pod);
                    pod.terminate();
                });
                pods.clear();
                k8sClientGetPodsAttempts = 0;
            } else {
                k8sClientGetPodsAttempts++;
            }
        }

        if (changes.hasChanges()) {
            k8sListeners.forEach(listener -> listener.onPodChange(changes));
        }
    }

    private static void updateConfigMaps() {
        K8SResourceChange<K8SConfigMap> changes = new K8SResourceChange<>();

        try {
            final long flag = random.nextLong();

            // Add or Update Config Maps
            client.configMaps().list().getItems().forEach(configMap -> {
                K8SConfigMap oldConfigMap = configMaps.get(configMap.getMetadata().getUid());
                if (oldConfigMap != null) {
                    oldConfigMap.setFlag(flag);
                    if (!oldConfigMap.getResourceVersion().equals(configMap.getMetadata().getResourceVersion())) {
                        oldConfigMap.setConfigMap(configMap);
                        changes.resourceUpdated(oldConfigMap);
                    }
                } else {
                    K8SConfigMap newConfigMap = new K8SConfigMap(configMap, flag);
                    configMaps.put(configMap.getMetadata().getUid(), newConfigMap);
                    changes.resourceAdded(newConfigMap);
                }
            });

            // Remove Config Maps
            configMaps.forEach((uid, configMap) -> {
                if (configMap.getFlag() != flag) {
                    changes.resourceRemoved(configMap);
                    configMaps.remove(uid);
                }
            });
        } catch (Exception e) {
            if (k8sClientGetConfigMapsAttempts > Configuration.MAX_KUBERNETES_CLIENT_ATTEMPTS) {
                LOGGER.error("Failed to get kubernetes config map info after " + Configuration.MAX_KUBERNETES_CLIENT_ATTEMPTS + " attempts", e);
                configMaps.forEach((uid, configMap) -> changes.resourceRemoved(configMap));
                configMaps.clear();
                k8sClientGetConfigMapsAttempts = 0;
            } else {
                k8sClientGetConfigMapsAttempts++;
            }
        }

        if (changes.hasChanges()) {
            k8sListeners.forEach(listener -> listener.onConfigMapChange(changes));
        }
    }

    private static void setMetricsNodePort(K8SPod pod) {
        String podAppLabel = pod.getAppLabel();
        if (!StringUtils.isEmpty(podAppLabel)) {
            ServiceList serviceList = client.services().list();
            for (Service service : serviceList.getItems()) {
                String serviceAppLabel = service.getMetadata().getLabels().get(K8SPod.APP_LABEL);
                if (podAppLabel.equals(serviceAppLabel)) {
                    for (ServicePort servicePort : service.getSpec().getPorts()) {
                        if (servicePort.getNodePort() != null && Configuration.METRICS_KUBERNETES_SERVICE_PORT_NAME.equalsIgnoreCase(servicePort.getName())) {
                            pod.setMetricsNodePort(servicePort.getNodePort());
                        }
                    }
                }
            }
        }
    }

    public static Set<String> getNamespaces() {
        final Set<String> namespaces = new HashSet<>();
        pods.forEach((uid, pod) -> namespaces.add(pod.getNamespace()));
        return namespaces;
    }

    public static List<SelectorItem> getPodSelectorList(String namespace) {
        Map<String, K8SPod> appLabelPods = new TreeMap<>();
        List<SelectorItem> podItems = new ArrayList<>();
        List<SelectorItem> configMapItems = new ArrayList<>();

        pods.forEach((uid, pod) -> {
            if (pod.getNamespace().equals(namespace)) {
                // APP Labels
                String appLabel = pod.getAppLabel();
                if (appLabel != null) {
                    K8SPod appLabelPod = appLabelPods.get(appLabel);
                    if (appLabelPod == null || pod.getStartTime().isAfter(appLabelPod.getStartTime())) {
                        appLabelPods.put(appLabel, pod);
                    }
                }

                // PODs
                if (pod.getContainers().size() < 2) {
                    podItems.add(new SelectorItem(pod, pod.getPodName(), ItemType.POD));
                }
                pod.getContainers().forEach((container) -> {
                    podItems.add(new SelectorItem(pod, container, pod.getPodName() + " [" + container + "]", ItemType.CONTAINER));
                });
                pod.getInitContainers().forEach((container) -> {
                    podItems.add(new SelectorItem(pod, container, pod.getPodName() + " [" + container + "]", ItemType.CONTAINER));
                });
            }
        });

        //Config Maps
        configMaps.forEach((uid, configMap) -> {
            if (configMap.getNamespace().equals(namespace)) {
                configMapItems.add(new SelectorItem(configMap, configMap.getName(), ItemType.CONFIG_MAP));
            }
        });

        Collections.sort(podItems);
        Collections.sort(configMapItems);

        List<SelectorItem> res = new ArrayList<>();
        res.add(new SelectorItem("App Labels:"));
        appLabelPods.forEach((appLabel, pod) -> res.add(new SelectorItem(pod, appLabel, ItemType.LABEL)));
        res.add(new SelectorItem("Pods:"));
        res.addAll(podItems);
        res.add(new SelectorItem("Config Maps:"));
        res.addAll(configMapItems);

        return res;
    }

    public static LogWatch getLogs(String namespace, String podName, String containerName, Integer tailingLines) {
        LOGGER.debug("Streaming Kubernetes POD with namespace={}, pod={}, container={}", namespace, podName, containerName);
        return client.pods().inNamespace(namespace).withName(podName).inContainer(containerName).tailingLines(tailingLines).watchLog();
    }

    public static void addListener(K8SClientListener listener) {
        k8sListeners.add(listener);
    }

    public static void removeListener(K8SClientListener listener) {
        k8sListeners.remove(listener);
    }

    public static boolean deletePod(Pod pod) throws K8SApiException {
        try {
            return client.pods().delete(pod);
        } catch (Exception e) {
            throw new K8SApiException(e);
        }
    }

    public static List<K8SPod> getPodsByLabel(String labelName, String labelValue) {
        List<K8SPod> res = new ArrayList<>();

        pods.forEach((uid, pod) -> {
            if (pod.containsLabel(labelName, labelValue)) {
                res.add(pod);
            }
        });

        return res;
    }

    public static void updateConfigMapData(String namespace, String configMapName, String dataName, String content) throws K8SApiException {
        try {
            client.configMaps().inNamespace(namespace).withName(configMapName).edit().addToData(dataName, content).done();
        } catch (Exception e) {
            throw new K8SApiException(e);
        }
    }
}
