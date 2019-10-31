package com.fvp.kubeson.logs.gui;

import java.io.File;
import java.util.List;

import com.fvp.kubeson.common.gui.MainTab;
import com.fvp.kubeson.common.gui.MainToolbar;
import com.fvp.kubeson.common.model.K8SConfigMap;
import com.fvp.kubeson.common.model.SelectedItem;
import com.fvp.kubeson.configmap.gui.ConfigMapTab;
import com.fvp.kubeson.metrics.gui.MetricsTab;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class LogTabPane {

    private static TabPane logTabPane;

    static {
        initLogTabPane();
    }

    private LogTabPane() {
    }

    private static void initLogTabPane() {
        logTabPane = new TabPane();
        logTabPane.setStyle("-fx-background-color: black;");
        logTabPane.setFocusTraversable(false);
        logTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            MainTab tab = (MainTab) newValue;
            if (tab != null) {
                tab.onSelected();
            } else {
                MainToolbar.clear();
            }
        });
        logTabPane.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.UP || keyEvent.getCode() == KeyCode.DOWN) {
                keyEvent.consume();
            }
        });
        logTabPane.setOnDragOver((event) -> {
            if (event.getGestureSource() != logTabPane && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        logTabPane.setOnDragDropped((event) -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    success = createLogTab(file);
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
        VBox.setVgrow(logTabPane, Priority.ALWAYS);
    }

    public static Parent draw() {
        return logTabPane;
    }

    public static boolean createMetricTab(SelectedItem selectedItem, String name) {
        // Return false if Tab with same name already exists
        for (Tab tab : logTabPane.getTabs()) {
            if (tab.getText().equals(name)) {
                return false;
            }
        }

        logTabPane.getTabs().add(new MetricsTab(selectedItem, name));
        logTabPane.getSelectionModel().selectLast();

        return true;
    }

    public static boolean createConfigMapTab(K8SConfigMap configMap, String name) {
        // Return false if Tab with same name already exists
        for (Tab tab : logTabPane.getTabs()) {
            if (tab.getText().equals(name)) {
                return false;
            }
        }

        logTabPane.getTabs().add(new ConfigMapTab(configMap, name));
        logTabPane.getSelectionModel().selectLast();

        return true;
    }

    public static boolean createLogTab(List<SelectedItem> selectedItems, String name) {
        // Return false if Tab with same name already exists
        for (Tab tab : logTabPane.getTabs()) {
            if (tab.getText().equals(name)) {
                return false;
            }
        }

        logTabPane.getTabs().add(new LogTab(selectedItems, name, true));
        logTabPane.getSelectionModel().selectLast();

        return true;
    }

    private static boolean createLogTab(File logFile) {
        // Return false if Tab with same name already exists
        for (Tab tab : logTabPane.getTabs()) {
            if (tab.getTooltip() != null && logFile.toString().equals(tab.getTooltip().getText())) {
                return false;
            }
        }
        LogTab logTab = new LogTab(logFile);
        logTabPane.getTabs().add(logTab);
        logTabPane.getSelectionModel().selectLast();

        return true;
    }

    public static MainTab getSelectedTab() {
        return (MainTab) logTabPane.getSelectionModel().getSelectedItem();
    }
}
