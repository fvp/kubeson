package com.fvp.kubeson.common.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.fvp.kubeson.Configuration;
import com.fvp.kubeson.common.model.ItemType;
import com.fvp.kubeson.common.model.K8SConfigMap;
import com.fvp.kubeson.common.model.K8SPod;
import com.fvp.kubeson.common.model.SelectorItem;
import com.fvp.kubeson.common.util.ThreadFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class K8SClient {

    private static Logger LOGGER = LogManager.getLogger();

    private static KubernetesClient client;

    private static List<K8SPod> pods;

    private static Map<String, K8SConfigMap> configMaps;

    private static List<K8SClientListener> k8sListeners;

    private static int k8sClientGetPodsAttempts;

    private static int k8sClientGetConfigMapsAttempts;

    static {
        pods = Collections.synchronizedList(new ArrayList<>());
        configMaps = new ConcurrentHashMap<>();
        k8sListeners = new ArrayList<>();
        client = new DefaultKubernetesClient();
        k8sClientGetPodsAttempts = 0;
        k8sClientGetConfigMapsAttempts = 0;
        startK8SWorker();
    }

    private K8SClient() {
    }

    private static void startK8SWorker() {
        ThreadFactory.newThread(() -> {
            try {
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

    private static void updatePods() {
        try {
            PodList podList = client.pods().list();
            boolean found;

            // Add New Pods
            for (Pod item : podList.getItems()) {
                if (!K8SPod.STATUS_PENDING.equals(item.getStatus().getPhase())) {
                    found = false;
                    for (K8SPod pod : pods) {
                        if (pod.getUid().equals(item.getMetadata().getUid())) {
                            found = true;
                            pod.setState(item.getStatus().getPhase());
                            break;
                        }
                    }
                    if (!found) {
                        K8SPod newPod = createNewPod(item);
                        pods.add(newPod);
                        k8sListeners.forEach(listener -> listener.onNewPod(newPod));
                    }
                }
            }

            // Terminate Pods
            for (Iterator<K8SPod> iterator = pods.iterator(); iterator.hasNext(); ) {
                K8SPod pod = iterator.next();
                found = false;
                for (Pod item : podList.getItems()) {
                    if (pod.getUid().equals(item.getMetadata().getUid())) {
                        found = true;
                        pod.setState(item.getStatus().getPhase());
                        break;
                    }
                }
                if (!found) {
                    terminatePod(pod);
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            if (k8sClientGetPodsAttempts > Configuration.MAX_KUBERNETES_CLIENT_ATTEMPTS) {
                LOGGER.error("Failed to get kubernetes pod info after " + Configuration.MAX_KUBERNETES_CLIENT_ATTEMPTS + " attempts. Stopping all pods", e);
                pods.forEach(K8SClient::terminatePod);
                pods.clear();
                k8sClientGetPodsAttempts = 0;
            } else {
                k8sClientGetPodsAttempts++;
            }
        }
    }

    private static void terminatePod(K8SPod pod) {
        pod.terminate();
        k8sListeners.forEach(listener -> listener.onPodTerminated(pod));
    }

    private static K8SPod createNewPod(Pod item) {
        K8SPod pod = new K8SPod(item);
        setMetricsNodePort(pod);

        return pod;
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

    private static void updateConfigMaps() {
        try {
            for (ConfigMap configMap : client.configMaps().list().getItems()) {

                K8SConfigMap oldConfigMap = configMaps.get(configMap.getMetadata().getUid());
                if (oldConfigMap != null) {
                    if (!oldConfigMap.getResourceVersion().equals(configMap.getMetadata().getResourceVersion())) {
                        oldConfigMap.updateConfigMap(configMap);
                        k8sListeners.forEach(listener -> listener.onConfigMapChange(oldConfigMap));
                    }
                } else {
                    K8SConfigMap newConfigMap = new K8SConfigMap(configMap);
                    configMaps.put(configMap.getMetadata().getUid(), newConfigMap);
                    k8sListeners.forEach(listener -> listener.onNewConfigMap(newConfigMap));
                }
            }
        } catch (Exception e) {
            if (k8sClientGetConfigMapsAttempts > Configuration.MAX_KUBERNETES_CLIENT_ATTEMPTS) {
                LOGGER.error("Failed to get kubernetes config map info after " + Configuration.MAX_KUBERNETES_CLIENT_ATTEMPTS + " attempts", e);
                configMaps.clear();
                k8sClientGetConfigMapsAttempts = 0;
            } else {
                k8sClientGetConfigMapsAttempts++;
            }
        }
    }

    public static Set<String> getNamespaces() {
        final Set<String> namespaces = new HashSet<>();
        pods.forEach(pod -> namespaces.add(pod.getNamespace()));
        return namespaces;
    }

    public static List<SelectorItem> getPodSelectorList(String namespace) {
        List<SelectorItem> res = new ArrayList<>();

        // APP Labels
        List<SelectorItem> apps = new ArrayList<>();
        Map<String, K8SPod> appLabelPods = new HashMap<>();
        res.add(new SelectorItem("App Labels:"));
        pods.forEach((pod) -> {
            if (pod.getNamespace().equals(namespace)) {
                String appLabel = pod.getAppLabel();
                if (appLabel != null) {
                    K8SPod appLabelPod = appLabelPods.get(appLabel);
                    if (appLabelPod == null || pod.getStartTime().isAfter(appLabelPod.getStartTime())) {
                        appLabelPods.put(appLabel, pod);
                    }
                }
            }
        });
        appLabelPods.forEach((appLabel, pod) -> apps.add(new SelectorItem(pod, appLabel, ItemType.LABEL)));
        Collections.sort(apps);
        res.addAll(apps);

        // PODs
        List<SelectorItem> podItems = new ArrayList<>();
        res.add(new SelectorItem("Pods:"));
        pods.forEach((pod) -> {
            if (pod.getNamespace().equals(namespace)) {
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
        Collections.sort(podItems);
        res.addAll(podItems);

        //Metrics
        List<SelectorItem> metricItems = new ArrayList<>();
        res.add(new SelectorItem("Metrics:"));
        pods.forEach((pod) -> {
            if (pod.getNamespace().equals(namespace)) {
                if (pod.hasMetrics()) {
                    metricItems.add(new SelectorItem(pod, pod.getPodName(), ItemType.METRICS));
                }
            }
        });
        Collections.sort(metricItems);
        res.addAll(metricItems);

        //Config Maps
        List<SelectorItem> configMapItems = new ArrayList<>();
        res.add(new SelectorItem("Config Maps:"));
        configMaps.forEach((uid, configMap) -> {
            if (configMap.getNamespace().equals(namespace)) {
                configMapItems.add(new SelectorItem(configMap, configMap.getName(), ItemType.CONFIG_MAP));
            }
        });
        Collections.sort(configMapItems);
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

        pods.forEach((pod) -> {
            if (pod.containsLabel(labelName, labelValue)) {
                res.add(pod);
            }
        });

        return res;
    }

    public static void updateConfigMapData(String namespace, String configMapName, String filename, String content) throws K8SApiException {
        try {
            client.configMaps().inNamespace(namespace).withName(configMapName).edit().addToData(filename, content).done();
        } catch (Exception e) {
            throw new K8SApiException(e);
        }
    }
}
