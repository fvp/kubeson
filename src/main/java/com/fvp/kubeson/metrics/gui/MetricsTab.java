package com.fvp.kubeson.metrics.gui;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fvp.kubeson.Configuration;
import com.fvp.kubeson.common.controller.K8SClient;
import com.fvp.kubeson.common.controller.K8SClientListener;
import com.fvp.kubeson.common.controller.K8SResourceChange;
import com.fvp.kubeson.common.gui.MainTab;
import com.fvp.kubeson.common.model.K8SConfigMap;
import com.fvp.kubeson.common.model.K8SPod;
import com.fvp.kubeson.common.model.SelectedItem;
import com.fvp.kubeson.common.util.NullMultipleSelectionModel;
import com.fvp.kubeson.common.util.ThreadFactory;
import com.fvp.kubeson.common.util.ThreadLock;
import com.fvp.kubeson.metrics.model.Metrics;
import com.fvp.kubeson.metrics.model.TreeNode;
import com.fvp.kubeson.metrics.util.PrometheusMetricsParser;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MetricsTab extends MainTab<MetricsToolbar> {

    private static Logger LOGGER = LogManager.getLogger();

    private static OkHttpClient httpClient = new OkHttpClient();

    private Request metricApi;

    private TreeItem<TreeNode> treeRoot;

    private Metrics metrics;

    private ThreadLock metricsRefreshLock;

    private Set<String> hiddenMetrics;

    private K8SClientListener k8sListener;

    private volatile boolean running;

    public MetricsTab(SelectedItem selectedItem, String name) {
        super(name);
        super.setToolbar(new MetricsToolbar(this));

        metricsRefreshLock = new ThreadLock();
        hiddenMetrics = new HashSet<>();

        treeRoot = TreeNode.create();
        treeRoot.setExpanded(true);

        TreeView<TreeNode> metricsView = new TreeView<>();
        VBox.setVgrow(metricsView, Priority.ALWAYS);
        metricsView.setShowRoot(false);
        metricsView.setRoot(treeRoot);
        metricsView.setSelectionModel(new NullMultipleSelectionModel<>());
        metricsView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            Node node = event.getPickResult().getIntersectedNode();

            if (node != null && node.getParent() != null && node.getParent().getParent() instanceof TreeNode) {
                TreeNode treeNode = (TreeNode) node.getParent().getParent();

                if (treeNode.getMetricName() != null) {
                    if (hiddenMetrics.contains(treeNode.getMetricName())) {
                        hiddenMetrics.remove(treeNode.getMetricName());
                        treeNode.setExpanded(true);
                    } else {
                        hiddenMetrics.add(treeNode.getMetricName());
                        treeNode.setExpanded(false);
                    }
                }
            }
        });

        metricApi = new Request.Builder().url("http://" + Configuration.METRICS_IP + ":" + selectedItem.getPod().getMetricsNodePort() + "/metrics").build();

        metricsView.setStyle("-fx-background-color: black;-fx-control-inner-background: black;");

        this.k8sListener = new K8SClientListener() {

            @Override
            public void onPodChange(K8SResourceChange<K8SPod> changes) {
                changes.filter(pod -> selectedItem.getPod().getMetricsNodePort() == pod.getMetricsNodePort())
                        .forEachAdded(podAdded -> {
                            setRunning(true);
                            refreshMetrics();
                        })
                        .forEachRemoved(podRemoved -> {
                            setRunning(false);
                        });
            }

            @Override
            public void onConfigMapChange(K8SResourceChange<K8SConfigMap> changes) {

            }
        };
        K8SClient.addListener(k8sListener);

        super.setOnClosed((event) -> {
            K8SClient.removeListener(k8sListener);
        });

        super.setContent(metricsView);

        setRunning(true);
        setAutomaticRefresh(false);
        startMetricsThread();
    }

    private void setRunning(boolean running) {
        this.running = running;
        Platform.runLater(() -> setStyle("tabred", !running));
    }

    void setAllExpanded(boolean value) {
        for (TreeItem<?> child : treeRoot.getChildren()) {
            child.setExpanded(value);
        }
    }

    void setAutomaticRefresh(boolean automaticRefresh) {
        if (automaticRefresh) {
            metricsRefreshLock.unlock();
        } else {
            metricsRefreshLock.lock();
        }
    }

    void drawMetrics() {
        treeRoot.getChildren().clear();
        metrics.getMetrics().forEach((key, metric) -> {
            TreeItem<TreeNode> metricItem = TreeNode.create(metric);
            metricItem.setExpanded(!hiddenMetrics.contains(metric.getName()));
            metricItem.expandedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    hiddenMetrics.remove(metric.getName());
                } else {
                    hiddenMetrics.add(metric.getName());
                }
            });

            metric.getMetricValues().forEach((metricValue) -> {
                TreeItem<TreeNode> metricValueItem = TreeNode.create(metricValue);
                metricItem.getChildren().add(metricValueItem);
            });

            treeRoot.getChildren().add(metricItem);
        });
    }

    private void startMetricsThread() {
        ThreadFactory.newThread(() -> {
            try {
                for (; ; ) {
                    if (running) {
                        requestMetrics();
                    }
                    TimeUnit.MILLISECONDS.sleep(Configuration.METRICS_AUTOMATIC_REFRESH_DELAY_MS);
                    metricsRefreshLock.waitPermission();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.info("Metrics Thread Interrupted");
            }
            LOGGER.info("Metrics Thread Completed");
        });
    }

    void refreshMetrics() {
        if (running) {
            ThreadFactory.newThread(this::requestMetrics);
        }
    }

    private void requestMetrics() {
        try (Response response = httpClient.newCall(metricApi).execute()) {
            if (response.body() != null) {
                if (this.metrics == null) {
                    this.metrics = PrometheusMetricsParser.parse(response.body().byteStream());
                } else {
                    PrometheusMetricsParser.parseAndUpdateValues(response.body().byteStream(), this.metrics);
                }
                Platform.runLater(this::drawMetrics);
            }
        } catch (Exception e) {
            LOGGER.error("Error retrieving metrics " + metricApi + ": " + e.getMessage());
        }
    }
}
