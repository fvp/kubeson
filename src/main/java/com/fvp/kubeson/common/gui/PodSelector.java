package com.fvp.kubeson.common.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fvp.kubeson.common.controller.K8SClient;
import com.fvp.kubeson.common.controller.K8SClientListener;
import com.fvp.kubeson.common.model.ItemType;
import com.fvp.kubeson.common.model.K8SConfigMap;
import com.fvp.kubeson.common.model.K8SPod;
import com.fvp.kubeson.common.model.SelectedItem;
import com.fvp.kubeson.common.model.SelectorItem;
import com.fvp.kubeson.logs.gui.LogTabPane;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public final class PodSelector {

    private static final String DEFAULT_NAMESPACE = "default";

    private static ChoiceBox<String> namespaceBox;

    private static CheckComboBox podNameBox;

    private static String selectedNamespace;

    private static int groupNumber;

    static {
        selectedNamespace = DEFAULT_NAMESPACE;
        groupNumber = 1;
        init();
    }

    private PodSelector() {
    }

    private static void init() {
        namespaceBox = new ChoiceBox<>();
        namespaceBox.setMinWidth(110);
        namespaceBox.maxWidth(110);
        namespaceBox.setPrefWidth(110);
        namespaceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectedNamespace = newValue;
                updatePodNameBox(selectedNamespace);
            }
        });

        podNameBox = new CheckComboBox();
        podNameBox.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                List<SelectedItem> selected = new ArrayList<>();
                for (SelectorItem item : podNameBox.getItems()) {
                    if (item.isChecked()) {
                        selected.add(new SelectedItem(item));
                        item.setChecked(false);
                    }
                }

                if (selected.size() > 1) {
                    LogTabPane.createLogTab(selected, "[Log] Group " + groupNumber);
                    groupNumber++;
                } else if (podNameBox.getSelectionModel().getSelectedItem() != null
                        && podNameBox.getSelectionModel().getSelectedItem().getType() != ItemType.TEXT) {
                    SelectedItem selectedItem = new SelectedItem(podNameBox.getSelectionModel().getSelectedItem());
                    if (selectedItem.getType() == ItemType.METRICS) {
                        LogTabPane.createMetricTab(selectedItem, "[Metrics] " + selectedItem.getText());
                    } else if (selectedItem.getType() == ItemType.CONFIG_MAP) {
                        LogTabPane.createConfigMapTab(selectedItem.getConfigMap(), "[ConfigMap] " + selectedItem.getText());
                    } else {
                        LogTabPane.createLogTab(Collections.singletonList(selectedItem), "[Log] " + selectedItem.getText());
                    }
                } else if (selected.size() == 1) {
                    LogTabPane.createLogTab(selected, "[Log] " + selected.get(0).getText());
                }
                podNameBox.getSelectionModel().clearSelection();
            }
        });

        K8SClient.addListener(new K8SClientListener() {

            @Override
            public void onPodTerminated(K8SPod pod) {
                Platform.runLater(PodSelector::update);
            }

            @Override
            public void onNewPod(K8SPod newPod) {
                Platform.runLater(PodSelector::update);
            }

            @Override
            public void onNewConfigMap(K8SConfigMap configMap) {
                Platform.runLater(PodSelector::update);
            }

            @Override
            public void onConfigMapChange(K8SConfigMap configMap) {
                Platform.runLater(PodSelector::update);
            }
        });
    }

    private static void update() {
        updateNamespaceBox();
        updatePodNameBox(selectedNamespace);
    }

    private static void updateNamespaceBox() {
        namespaceBox.getItems().clear();
        namespaceBox.getItems().addAll(K8SClient.getNamespaces());
        namespaceBox.getSelectionModel().select(selectedNamespace);
    }

    private static void updatePodNameBox(String nameSpace) {
        podNameBox.getItems().clear();
        if (K8SClient.getNamespaces().contains(nameSpace)) {
            podNameBox.getItems().addAll(K8SClient.getPodSelectorList(nameSpace));
        }
    }

    public static Parent draw() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(4);
        gridPane.setVgap(3.5);
        Text t1 = new Text("Namespace");
        t1.getStyleClass().add("selector-label");
        t1.setFill(Color.WHITE);
        gridPane.add(t1, 1, 1);
        gridPane.add(namespaceBox, 1, 2);
        Text t2 = new Text("Resource");
        t2.getStyleClass().add("selector-label");
        t2.setFill(Color.WHITE);
        gridPane.add(t2, 2, 1);
        gridPane.add(podNameBox, 2, 2);

        return gridPane;
    }

}
