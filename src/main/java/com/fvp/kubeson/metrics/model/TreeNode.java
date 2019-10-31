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

    private String value;

    private TreeNode() {
    }

    public static TreeItem<TreeNode> create() {
        return new TreeItem<>();
    }

    public static TreeItem<TreeNode> create(Metric metric) {
        TreeNode treeNode = new TreeNode();
        treeNode.init(metric);
        return new TreeItem<>(treeNode);
    }

    public static TreeItem<TreeNode> create(MetricValue metricValue) {
        TreeNode treeNode = new TreeNode();
        treeNode.init(metricValue);
        return new TreeItem<>(treeNode);
    }

    private void init(Node... nodes) {
        super.getChildren().addAll(nodes);
        super.setAlignment(Pos.CENTER_LEFT);
    }

    private void init(Metric metric) {
        TextFlow tf = new TextFlow();

        Text metricName = new Text(metric.getName());
        metricName.setFill(Color.WHITE);
        metricName.setStyle("-fx-font-weight: bolder;-fx-font-size: 14px;");
        tf.getChildren().add(metricName);

        if (metric.getDescription() != null) {
            Text slash = new Text(" - ");
            slash.setFill(Color.web("#e6df44"));
            slash.setStyle("-fx-font-weight: bolder;-fx-font-size: 14px;");

            Text description = new Text(metric.getDescription());
            description.setFill(Color.web("#90CAF9"));
            description.setStyle("-fx-font-weight: bolder;-fx-font-size: 14px;");

            tf.getChildren().addAll(slash, description);
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
        ret.setStyle("-fx-font-weight: bolder;-fx-font-size: 14px;");

        return ret;
    }

    public String getValue() {
        return value;
    }
}
