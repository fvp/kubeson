package com.fvp.kubeson.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fvp.kubeson.Configuration;
import com.fvp.kubeson.model.ItemType;
import com.fvp.kubeson.model.Pod;
import com.fvp.kubeson.model.SelectorItem;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Kubernetes {

    private static Logger LOGGER = LogManager.getLogger();

    private static KubernetesClient client;

    private static List<Pod> pods;

    private static List<KubernetesListener> kubernetesListeners;

    private static int kubernetesClientAttempts;

    static {
        pods = Collections.synchronizedList(new ArrayList<>());
        kubernetesListeners = new ArrayList<>();
        client = new DefaultKubernetesClient();
        kubernetesClientAttempts = 0;
        startKubernetesWorker();
    }

    private Kubernetes() {
    }

    private static void startKubernetesWorker() {
        ThreadFactory.newThread(() -> {
            try {
                for (; ; ) {
                    TimeUnit.MILLISECONDS.sleep(Configuration.KUBERNETES_WORKER_WAIT_TIME_MS);
                    updatePods();
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
            for (io.fabric8.kubernetes.api.model.Pod item : podList.getItems()) {
                if (!Pod.STATUS_PENDING.equals(item.getStatus().getPhase())) {
                    found = false;
                    for (Pod pod : pods) {
                        if (pod.getPodName().equals(item.getMetadata().getName())) {
                            found = true;
                            pod.setState(item.getStatus().getPhase());
                            break;
                        }
                    }
                    if (!found) {
                        Pod newPod = createNewPod(item);
                        kubernetesListeners.forEach(kubernetesListener -> kubernetesListener.onNewPod(newPod));
                        pods.add(newPod);
                    }
                }
            }

            // Terminate Pods
            for (Iterator<Pod> iterator = pods.iterator(); iterator.hasNext(); ) {
                Pod pod = iterator.next();
                found = false;
                for (io.fabric8.kubernetes.api.model.Pod item : podList.getItems()) {
                    if (pod.getPodName().equals(item.getMetadata().getName())) {
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
            if (kubernetesClientAttempts > Configuration.MAX_KUBERNETES_CLIENT_ATTEMPTS) {
                LOGGER.error("Failed to get kubernetes pod info after " + Configuration.MAX_KUBERNETES_CLIENT_ATTEMPTS + " attempts. Stopping all pods", e);
                pods.forEach(Kubernetes::terminatePod);
                pods.clear();
                kubernetesClientAttempts = 0;
            } else {
                kubernetesClientAttempts++;
            }
        }
    }

    private static void terminatePod(Pod pod) {
        pod.terminate();
        kubernetesListeners.forEach(kubernetesListener -> kubernetesListener.onPodTerminated(pod));
    }

    private static Pod createNewPod(io.fabric8.kubernetes.api.model.Pod item) {
        List<String> containers = new ArrayList<>();
        List<String> initContainers = new ArrayList<>();
        ObjectMeta meta = item.getMetadata();

        if (item.getStatus().getContainerStatuses() != null && item.getStatus().getContainerStatuses().size() > 1) {
            for (ContainerStatus containerStatus : item.getStatus().getContainerStatuses()) {
                containers.add(containerStatus.getName());
            }
        }
        if (item.getStatus().getInitContainerStatuses() != null) {
            for (ContainerStatus containerStatus : item.getStatus().getInitContainerStatuses()) {
                initContainers.add(containerStatus.getName());
            }
        }
        if (meta.getLabels() == null) {
            meta.setLabels(new HashMap<>());
        }

        return new Pod(meta.getNamespace(), meta.getName(), meta.getLabels(), item.getStatus().getPhase(), item.getStatus().getStartTime(), containers,
            initContainers);
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
        Map<String, Pod> appLabelPods = new HashMap<>();
        res.add(new SelectorItem("App Labels:"));
        pods.forEach((pod) -> {
            if (pod.getNamespace().equals(namespace)) {
                String appLabel = pod.getAppLabel();
                if (appLabel != null) {
                    Pod appLabelPod = appLabelPods.get(appLabel);
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

        return res;
    }

    public static LogWatch getKubernetesLogs(String namespace, String podName, String containerName, Integer tailingLines) {
        LOGGER.debug("Streaming Kubernetes POD with namespace={}, pod={}, container={}", namespace, podName, containerName);
        return client.pods().inNamespace(namespace).withName(podName).inContainer(containerName).tailingLines(tailingLines).watchLog();
    }

    public static void addListener(KubernetesListener kubernetesListener) {
        kubernetesListeners.add(kubernetesListener);
    }

    public static void removeListener(KubernetesListener kubernetesListener) {
        kubernetesListeners.remove(kubernetesListener);
    }
}
