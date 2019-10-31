package com.fvp.kubeson.configmap.gui;

import java.util.List;
import java.util.Set;

import com.fvp.kubeson.common.controller.K8SApiException;
import com.fvp.kubeson.common.controller.K8SClient;
import com.fvp.kubeson.common.gui.MainTab;
import com.fvp.kubeson.common.model.K8SConfigMap;
import com.fvp.kubeson.common.model.K8SPod;
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

    private boolean textEdited;

    public ConfigMapTab(K8SConfigMap configMap, String name) {
        super(name);

        this.configMap = configMap;

        mainTextField = new TextArea();
        mainTextField.setStyle("-fx-background-color: black;-fx-control-inner-background: black;");
        mainTextField.setFocusTraversable(false);
        mainTextField.setEditable(true);
        mainTextField.setPrefHeight(Region.USE_COMPUTED_SIZE);
        mainTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!StringUtils.isEmpty(oldValue)) {
                setTextEdited(!getSelectedData().equals(newValue));
            }
        });

        super.setToolbar(new ConfigMapToolbar(this));
        super.setContent(mainTextField);
    }

    private void setTextEdited(boolean textEdited) {
        if (this.textEdited != textEdited) {
            if (textEdited) {
                super.getStyleClass().add("tabblue");
            } else {
                super.getStyleClass().remove("tabblue");
            }
            this.textEdited = textEdited;
        }
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
        if (selectedConfigDataName != null && !StringUtils.isEmpty(mainTextField.getText())) {
            try {
                configMap.updateConfigMapData(selectedConfigDataName, mainTextField.getText());
                setTextEdited(false);
            } catch (K8SApiException e) {
                LOGGER.error("error save", e);
            }
        }
    }

    void deletePods() {
        String appLabel = configMap.getAppLabel();
        if (appLabel != null) {
            List<K8SPod> pods = K8SClient.getPodsByLabel(K8SPod.APP_LABEL, appLabel);
            for (K8SPod pod : pods) {
                try {
                    pod.delete();
                } catch (K8SApiException e) {
                    LOGGER.error("error delete", e);
                }
            }
        }
    }
}
