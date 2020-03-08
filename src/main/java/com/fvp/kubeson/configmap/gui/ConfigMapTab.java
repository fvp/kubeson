package com.fvp.kubeson.configmap.gui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fvp.kubeson.Main;
import com.fvp.kubeson.common.controller.K8SClient;
import com.fvp.kubeson.common.controller.K8SClientListener;
import com.fvp.kubeson.common.controller.K8SResourceChange;
import com.fvp.kubeson.common.gui.TabBase;
import com.fvp.kubeson.common.gui.TabLabel;
import com.fvp.kubeson.common.model.K8SConfigMap;
import com.fvp.kubeson.common.model.K8SPod;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigMapTab extends TabBase<ConfigMapToolbar> {

    private static Logger LOGGER = LogManager.getLogger();

    private TextArea mainTextField;

    private K8SConfigMap configMap;

    private String selectedConfigDataName;

    private String selectedConfigDataContent;

    private K8SClientListener k8sListener;

    private boolean textEdited;

    private volatile boolean removed;

    public ConfigMapTab(K8SConfigMap k8sConfigMap, TabLabel tabLabel) {
        super(tabLabel);

        this.configMap = k8sConfigMap;

        mainTextField = new TextArea();
        mainTextField.setStyle(
                "-fx-background-color: black;-fx-control-inner-background: black;-fx-focus-color: transparent;-fx-faint-focus-color: transparent;");
        mainTextField.setFocusTraversable(false);
        mainTextField.setEditable(false);
        mainTextField.setPrefHeight(Region.USE_COMPUTED_SIZE);
        mainTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!StringUtils.isEmpty(oldValue)) {
                setTextEdited(!StringUtils.equals(selectedConfigDataContent, newValue));
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
            getTabLabel().setChangeColor(textEdited);
            this.textEdited = textEdited;
        }
    }

    private void setRemoved(boolean removed) {
        this.removed = removed;
        getTabLabel().setErrorColor(removed);
    }

    Set<String> getConfigMapFiles() {
        if (configMap.getData() != null) {
            return configMap.getData().keySet();
        }
        return new HashSet<>();
    }

    void selectConfigMapDataName(String configFileName) {
        selectedConfigDataName = configFileName;
        selectedConfigDataContent = configMap.getData().get(configFileName);
        if (selectedConfigDataContent != null) {
            selectedConfigDataContent = selectedConfigDataContent.replaceAll("\r\n", "\n");
            mainTextField.setText(selectedConfigDataContent);
            mainTextField.setEditable(true);
        } else {
            mainTextField.setText("");
            mainTextField.setEditable(false);
        }
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
        List<K8SPod> pods = K8SClient.getPods();
        boolean deleted = false;
        for (K8SPod pod : pods) {
            if (K8SPod.STATUS_RUNNING.equals(pod.getState()) && pod.usesConfigMap(configMap.getName())) {
                deleted = true;
                pod.delete();
            }
        }
        if (!deleted) {
            Main.showWarningMessage("No pods found", "No running pods found using Config Map \"" + configMap.getName() + "\"");
        }
    }
}
