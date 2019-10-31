package com.fvp.kubeson.metrics.gui;

import java.util.HashSet;
import java.util.Set;

import com.fvp.kubeson.common.util.NullMultipleSelectionModel;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class MetricsList {

    private ListView<MetricListItem> listView;

    private MetricFilterListener metricFilterListener;

    private Set<String> metricsFilter;

    private boolean allMetricsChange = false;

    public MetricsList() {
        listView = new ListView<>();
        metricsFilter = new HashSet<>();

        listView.setSelectionModel(new NullMultipleSelectionModel<>());
        listView.getItems().add(new MetricListItem());
    }

    public Node draw() {
        return listView;
    }

    public boolean isEmpty() {
        return listView.getItems().size() == 1;
    }

    public void clear() {
        listView.getItems().remove(1, listView.getItems().size());
    }

    public void add(String metricName) {
        listView.getItems().add(new MetricListItem(metricName));
    }

    public boolean isMetricFiltered(String metricName) {
        return metricsFilter.contains(metricName);
    }

    public void addListener(MetricFilterListener metricFilterListener) {
        this.metricFilterListener = metricFilterListener;
    }

    public class MetricListItem extends HBox {

        private CheckBox showMetric;

        public MetricListItem() {
            CheckBox showAllMetrics = new CheckBox();
            showAllMetrics.setSelected(true);

            showAllMetrics.selectedProperty().addListener((observable, oldValue, newValue) -> {
                allMetricsChange = true;
                listView.getItems().forEach((metricListItem -> metricListItem.setSelected(newValue)));
                allMetricsChange = false;
                metricFilterListener.onFilterChange();
            });

            Text metricNameText = new Text("All Metrics");
            metricNameText.setFill(Color.WHITE);
            metricNameText.setStyle("-fx-font-weight: bolder;-fx-font-size: 14px;");

            super.getChildren().addAll(showAllMetrics, metricNameText);
        }

        public MetricListItem(String metricName) {
            showMetric = new CheckBox();
            showMetric.setSelected(!metricsFilter.contains(metricName));

            showMetric.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && newValue) {
                    metricsFilter.remove(metricName);
                } else {
                    metricsFilter.add(metricName);
                }
                if (!allMetricsChange) {
                    metricFilterListener.onFilterChange();
                }
            });

            Text metricNameText = new Text(metricName);
            metricNameText.setFill(Color.WHITE);
            metricNameText.setStyle("-fx-font-size: 14px;");

            super.getChildren().addAll(showMetric, metricNameText);
        }

        public void setSelected(boolean value) {
            if (showMetric != null) {
                showMetric.setSelected(value);
            }
        }
    }


}
