package com.fvp.kubeson.metrics.gui;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fvp.kubeson.Configuration;
import com.fvp.kubeson.common.gui.MainTab;
import com.fvp.kubeson.common.model.SelectedItem;
import com.fvp.kubeson.common.util.NullMultipleSelectionModel;
import com.fvp.kubeson.common.util.ThreadFactory;
import com.fvp.kubeson.common.util.ThreadLock;
import com.fvp.kubeson.metrics.model.Metrics;
import com.fvp.kubeson.metrics.model.TreeNode;
import com.fvp.kubeson.metrics.util.PrometheusMetricsParser;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
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

    private volatile long refreshTime;

    private Set<String> hiddenMetrics;

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

        metricApi = new Request.Builder().url("http://" + Configuration.METRICS_IP + ":" + selectedItem.getPod().getMetricsNodePort() + "/metrics").build();
        refreshTime = 2000;

        metricsView.setStyle("-fx-background-color: black;-fx-control-inner-background: black;");

        super.setContent(metricsView);
        startMetricsThread();
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
            //if (!metricsList.isMetricFiltered(metric.getName())) {
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
            //}
        });
    }

    private void startMetricsThread() {
        ThreadFactory.newThread(() -> {
            try {
                for (; ; ) {
                    refreshMetrics();
                    TimeUnit.MILLISECONDS.sleep(refreshTime);
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
        try (Response response = httpClient.newCall(metricApi).execute()) {
            if (response.body() != null) {
                if (this.metrics == null) {
                    this.metrics = PrometheusMetricsParser.parse(response.body().byteStream());
                } else {
                    PrometheusMetricsParser.parseAndUpdateValues(response.body().byteStream(), this.metrics);
                }
                Platform.runLater(this::drawMetrics);
            } else {
                LOGGER.warn("No metrics body");
            }
        } catch (Exception e) {
            LOGGER.error("Error retrieving metrics", e);
        }
    }
}
