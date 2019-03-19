package com.fvp.kubeson.model;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fvp.kubeson.Configuration;
import com.fvp.kubeson.core.CircularArrayList;
import com.fvp.kubeson.core.Kubernetes;
import com.fvp.kubeson.core.ThreadFactory;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Pod {

    public static final String STATUS_PENDING = "Pending";

    public static final String STATUS_RUNNING = "Running";

    public static final String STATUS_SUCCEEDED = "Succeeded";

    public static final String STATUS_FAILED = "Failed";

    public static final String STATUS_UNKNOWN = "Unknown";

    private static final String APP_LABEL = "app";

    private static final Logger LOGGER = LogManager.getLogger();

    private String namespace;

    private String podName;

    private Map<String, String> labels;

    private String state;

    private Instant startTime;

    private List<String> containers;

    private List<String> initContainers;

    private Map<String, PodContainerThread> podThreads;

    public Pod(String namespace, String podName, Map<String, String> labels, String state, String startTime, List<String> containers,
        List<String> initContainers) {
        this.namespace = namespace;
        this.podName = podName;
        this.labels = labels;
        this.state = state;
        this.startTime = Instant.parse(startTime);
        this.containers = containers;
        this.initContainers = initContainers;
        this.podThreads = Collections.synchronizedMap(new HashMap<>());
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPodName() {
        return podName;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public String getAppLabel() {
        return labels.get(APP_LABEL);
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public List<String> getContainers() {
        return containers;
    }

    public List<String> getInitContainers() {
        return initContainers;
    }

    public void addListener(String container, LogSource logSource, PodLogFeedListener podLogFeedListener, boolean showLogsFromStart) {
        if (container == null || containers.contains(container) || initContainers.contains(container)) {
            PodContainerThread podContainerThread = podThreads.get(container);
            if (podContainerThread != null) {
                if (showLogsFromStart && podContainerThread.logLines.size() > 0) {
                    podContainerThread.logLines.forEach(logLine -> podLogFeedListener.onNewLogLine(logSource, logLine));
                }
                podContainerThread.logSources.add(logSource);
                podContainerThread.podLogFeedListeners.add(podLogFeedListener);
            } else {
                podContainerThread = new PodContainerThread();
                podContainerThread.logSources.add(logSource);
                podContainerThread.podLogFeedListeners.add(podLogFeedListener);
                podContainerThread.threadFactory = ThreadFactory.newThread(createRunnable(podContainerThread, container));
                podThreads.put(container, podContainerThread);
            }
        }
    }

    public void removeListener(PodLogFeedListener podLogFeedListener, boolean keepLogSource) {
        Iterator<Map.Entry<String, PodContainerThread>> entryIt = podThreads.entrySet().iterator();

        while (entryIt.hasNext()) {
            PodContainerThread podContainerThread = entryIt.next().getValue();
            final int idx = podContainerThread.podLogFeedListeners.indexOf(podLogFeedListener);
            if (idx > -1) {
                podContainerThread.podLogFeedListeners.remove(idx);
                podContainerThread.logSources.remove(idx);

                if (!keepLogSource && podContainerThread.podLogFeedListeners.isEmpty()) {
                    podContainerThread.threadFactory.interrupt();
                    entryIt.remove();
                }
            }
        }
    }

    public boolean isRunning(String container) {
        PodContainerThread podContainerThread = podThreads.get(container);
        if (podContainerThread != null && !podContainerThread.threadFactory.isInterrupted()) {
            return true;
        }
        return false;
    }

    public void terminate() {
        podThreads.forEach((container, podContainerThread) -> podContainerThread.threadFactory.interrupt());
    }

    private Runnable createRunnable(final PodContainerThread podContainerThread, final String container) {
        return () -> {
            LOGGER.debug("Starting log stream for {} with container {}", this, container);

            LogWatch logWatch = Kubernetes.getKubernetesLogs(namespace, podName, container, Configuration.MAX_LOG_LINES * 2);
            try (InputStream is = logWatch.getOutput()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                for (; ; ) {
                    final String line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    final LogLine logLine = new LogLine(line);
                    if (podContainerThread.logLines.size() >= Configuration.MAX_LOG_LINES) {
                        podContainerThread.podLogFeedListeners.forEach(
                            podLogFeedListener -> podLogFeedListener.onLogLineRemoved(podContainerThread.logLines.get(0)));
                        podContainerThread.logLines.remove(0);
                    }
                    podContainerThread.logLines.add(logLine);
                    for (int i = 0; i < podContainerThread.podLogFeedListeners.size(); i++) {
                        podContainerThread.podLogFeedListeners.get(i).onNewLogLine(podContainerThread.logSources.get(i), logLine);
                    }
                }
            } catch (InterruptedIOException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOGGER.error("Log stream stopped unexpectedly for " + this, e);
            }
            logWatch.close();
            for (PodLogFeedListener podLogFeedListener : podContainerThread.podLogFeedListeners) {
                podLogFeedListener.onPodLogFeedTerminated(this);
            }
            podContainerThread.logLines.clear();
            LOGGER.debug("Log stream stopped for {}", this);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Pod pod = (Pod) o;
        return Objects.equals(namespace, pod.namespace) && Objects.equals(podName, pod.podName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, podName);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Pod{");
        sb.append("namespace='").append(namespace).append('\'');
        sb.append(", podName='").append(podName).append('\'');
        sb.append(", labels=").append(labels);
        sb.append(", state='").append(state).append('\'');
        sb.append(", containers=").append(containers);
        sb.append(", initContainers=").append(initContainers);
        sb.append('}');
        return sb.toString();
    }

    private static class PodContainerThread {

        private ThreadFactory threadFactory;

        private List<LogSource> logSources;

        private List<PodLogFeedListener> podLogFeedListeners;

        private List<LogLine> logLines;

        private PodContainerThread() {
            this.logSources = new ArrayList<>();
            this.podLogFeedListeners = new ArrayList<>();
            this.logLines = Collections.synchronizedList(new CircularArrayList<>(Configuration.MAX_LOG_LINES));
        }
    }
}
