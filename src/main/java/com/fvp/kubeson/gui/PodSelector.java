package com.fvp.kubeson.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fvp.kubeson.core.Kubernetes;
import com.fvp.kubeson.core.KubernetesListener;
import com.fvp.kubeson.model.ItemType;
import com.fvp.kubeson.model.Pod;
import com.fvp.kubeson.model.SelectedItem;
import com.fvp.kubeson.model.SelectorItem;
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
                    LogTabPane.createTab(selected, "Group " + groupNumber);
                    groupNumber++;
                } else if (podNameBox.getSelectionModel().getSelectedItem() != null
                    && podNameBox.getSelectionModel().getSelectedItem().getType() != ItemType.TEXT) {
                    SelectedItem selectedItem = new SelectedItem(podNameBox.getSelectionModel().getSelectedItem());
                    LogTabPane.createTab(Collections.singletonList(selectedItem), selectedItem.getText());
                } else if (selected.size() == 1) {
                    LogTabPane.createTab(selected, selected.get(0).getText());
                }
                podNameBox.getSelectionModel().clearSelection();
            }
        });

        Kubernetes.addListener(new KubernetesListener() {

            @Override
            public void onPodTerminated(Pod pod) {
                Platform.runLater(PodSelector::update);
            }

            @Override
            public void onNewPod(Pod newPod) {
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
        namespaceBox.getItems().addAll(Kubernetes.getNamespaces());
        namespaceBox.getSelectionModel().select(selectedNamespace);
    }

    private static void updatePodNameBox(String nameSpace) {
        podNameBox.getItems().clear();
        if (Kubernetes.getNamespaces().contains(nameSpace)) {
            podNameBox.getItems().addAll(Kubernetes.getPodSelectorList(nameSpace));
        }
    }

    public static Parent draw() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(4);
        gridPane.setVgap(2.5);
        Text t1 = new Text("Namespace");
        t1.getStyleClass().add("selector-label");
        t1.setFill(Color.WHITE);
        gridPane.add(t1, 1, 1);
        gridPane.add(namespaceBox, 1, 2);
        Text t2 = new Text("Pod");
        t2.getStyleClass().add("selector-label");
        t2.setFill(Color.WHITE);
        gridPane.add(t2, 2, 1);
        gridPane.add(podNameBox, 2, 2);

        return gridPane;
    }

}
