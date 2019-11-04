package com.fvp.kubeson.metrics.model;

import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class TreeNode extends HBox {

    private static final String YELLOW = "#e6df44";

    private static final String ORANGE = "#f0810f";

    private static final String GREEN = "#3CAEA3";

    private static final String WHITE = "#ffffff";

    private static final String LIGHT_BLUE = "#90CAF9";

    private TreeItem<TreeNode> treeItem;

    private String metricName;

    private TreeNode() {
    }

    public static TreeItem<TreeNode> create() {
        return new TreeItem<>();
    }

    public static TreeItem<TreeNode> create(Metric metric) {
        TreeNode treeNode = new TreeNode();
        treeNode.init(metric);
        treeNode.treeItem = new TreeItem<>(treeNode);
        return treeNode.treeItem;
    }

    public static TreeItem<TreeNode> create(MetricValue metricValue) {
        TreeNode treeNode = new TreeNode();
        treeNode.init(metricValue);
        treeNode.treeItem = new TreeItem<>(treeNode);
        return treeNode.treeItem;
    }

    private void init(Node... nodes) {
        super.getChildren().addAll(nodes);
        super.setAlignment(Pos.CENTER_LEFT);
    }

    private void init(Metric metric) {
        this.metricName = metric.getName();
        TextFlow tf = new TextFlow();
        tf.getChildren().add(getText(WHITE, metric.getName()));

        if (metric.getDescription() != null) {
            tf.getChildren().addAll(getText(YELLOW, " - "), getText(LIGHT_BLUE, metric.getDescription()));
        }

        init(tf);
    }

    private void init(MetricValue metricValue) {
        TextFlow tf = new TextFlow();

        tf.getChildren().add(getText(YELLOW, "{"));

        for (KeyValue dimention : metricValue.getDimensions()) {
            tf.getChildren().addAll(
                    getText(ORANGE, dimention.getKey()),
                    getText(YELLOW, "="),
                    getText(GREEN, dimention.getValue()),
                    getText(YELLOW, ",")
            );
        }

        List<String> values = metricValue.getValues();
        tf.getChildren().addAll(
                getText(YELLOW, "} "),
                getText(WHITE, values.get(0)),
                getText(YELLOW, " [")
        );

        for (int i = 1; i < values.size(); i++) {
            tf.getChildren().add(getText("#adadad", values.get(i)));
            if (i != values.size() - 1) {
                tf.getChildren().add(getText(YELLOW, ", "));
            }
        }

        tf.getChildren().add(getText(YELLOW, "]"));

        init(tf);
    }

    private Text getText(String color, String text) {
        Text ret = new Text(text);
        ret.setFill(Color.web(color));
        ret.setStyle("-fx-font-weight: bolder;-fx-font-size: 13px;");

        return ret;
    }

    public void setExpanded(boolean value) {
        treeItem.setExpanded(value);
    }

    public String getMetricName() {
        return metricName;
    }
}
