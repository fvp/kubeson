package com.fvp.kubeson.configmap.gui;

import java.util.List;
import java.util.Set;

import com.fvp.kubeson.Main;
import com.fvp.kubeson.common.controller.K8SClient;
import com.fvp.kubeson.common.controller.K8SClientListener;
import com.fvp.kubeson.common.controller.K8SResourceChange;
import com.fvp.kubeson.common.gui.MainTab;
import com.fvp.kubeson.common.model.K8SConfigMap;
import com.fvp.kubeson.common.model.K8SPod;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigMapTab extends MainTab<ConfigMapToolbar> {

    private static Logger LOGGER = LogManager.getLogger();

    private TextArea mainTextField;

    private K8SConfigMap configMap;

    private String selectedConfigDataName;

    private K8SClientListener k8sListener;

    private boolean textEdited;

    private volatile boolean removed;

    public ConfigMapTab(K8SConfigMap k8sConfigMap, String name) {
        super(name);

        this.configMap = k8sConfigMap;

        mainTextField = new TextArea();
        mainTextField.setStyle(
                "-fx-background-color: black;-fx-control-inner-background: black;-fx-focus-color: transparent;-fx-faint-focus-color: transparent;");
        mainTextField.setFocusTraversable(false);
        mainTextField.setEditable(true);
        mainTextField.setPrefHeight(Region.USE_COMPUTED_SIZE);
        mainTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!StringUtils.isEmpty(oldValue)) {
                setTextEdited(!getSelectedData().equals(newValue));
            }
        });

        this.k8sListener = new K8SClientListener() {

            @Override
            public void onPodChange(K8SResourceChange<K8SPod> changes) {
            }

            @Override
            public void onConfigMapChange(K8SResourceChange<K8SConfigMap> changes) {
                changes
                        .forEachAdded(configMapAdded -> {
                            if (configMap.getName().equals(configMapAdded.getName())) {
                                Platform.runLater(() -> refresh(configMapAdded));
                            }
                        })
                        .forEachRemoved(configMapRemoved -> {
                            if (configMap.getUid().equals(configMapRemoved.getUid())) {
                                Platform.runLater(() -> setRemoved(true));
                            }
                        })
                        .forEachUpdated(configMapUpdated -> {
                            if (configMap.getUid().equals(configMapUpdated.getUid())) {
                                Platform.runLater(() -> refresh(configMapUpdated));
                            }
                        });
            }
        };
        K8SClient.addListener(k8sListener);

        super.setOnClosed((event) -> {
            K8SClient.removeListener(k8sListener);
        });

        super.setToolbar(new ConfigMapToolbar(this));
        super.setContent(mainTextField);
    }

    private void refresh(K8SConfigMap k8sConfigMap) {
        this.configMap = k8sConfigMap;
        getToolbar().refreshConfigMapDataNameSelector();
        setRemoved(false);
    }

    private void setTextEdited(boolean textEdited) {
        if (this.textEdited != textEdited) {
            setStyle("tabblue", textEdited);
            this.textEdited = textEdited;
        }
    }

    private void setRemoved(boolean removed) {
        this.removed = removed;
        setStyle("tabred", removed);
    }

    private String getSelectedData() {
        return configMap.getData().get(selectedConfigDataName);
    }

    Set<String> getConfigMapFiles() {
        return configMap.getData().keySet();
    }

    void selectConfigMapDataName(String configFileName) {
        selectedConfigDataName = configFileName;
        mainTextField.setText(configMap.getData().get(configFileName));
        setTextEdited(false);
    }

    void saveConfigMapFile() {
        if (!removed && selectedConfigDataName != null && !StringUtils.isEmpty(mainTextField.getText())) {
            configMap.updateConfigMapData(selectedConfigDataName, mainTextField.getText(), (result) -> {
                if (result.isSuccessful()) {
                    setTextEdited(false);
                }
            });
        }
    }

    void deletePods() {
        String appLabel = configMap.getAppLabel();
        if (appLabel != null) {
            List<K8SPod> pods = K8SClient.getPodsByLabel(K8SPod.APP_LABEL, appLabel);
            boolean deleted = false;
            for (K8SPod pod : pods) {
                if (K8SPod.STATUS_RUNNING.equals(pod.getState())) {
                    deleted = true;
                    pod.delete();
                }
            }
            if (!deleted) {
                Main.showWarningMessage("No running pods found", "No running pods with app label \"" + appLabel + "\" were found");
            }
        }
    }
}
