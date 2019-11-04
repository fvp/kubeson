package com.fvp.kubeson.common.model;

import static com.fvp.kubeson.common.model.K8SPod.APP_LABEL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fvp.kubeson.Main;
import com.fvp.kubeson.common.controller.K8SApiException;
import com.fvp.kubeson.common.controller.K8SClient;
import com.fvp.kubeson.common.controller.K8SRequestCallback;
import com.fvp.kubeson.common.controller.K8SRequestResult;
import com.fvp.kubeson.common.util.ThreadFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class K8SConfigMap implements Comparable<K8SConfigMap> {

    private static final Logger LOGGER = LogManager.getLogger();

    private ConfigMap configMap;

    private long flag;

    private List<ConfigMapChangeListener> listeners;

    public K8SConfigMap(ConfigMap configMap, long flag) {
        this.configMap = configMap;
        this.flag = flag;
        this.listeners = new ArrayList<>();
    }

    public void updateConfigMap(ConfigMap configMap) {
        this.configMap = configMap;
        this.listeners.forEach(listener -> listener.onConfigMapChange(this));
    }

    public long getFlag() {
        return flag;
    }

    public void setFlag(long flag) {
        this.flag = flag;
    }

    public String getUid() {
        return configMap.getMetadata().getUid();
    }

    public String getNamespace() {
        return configMap.getMetadata().getNamespace();
    }

    public String getName() {
        return configMap.getMetadata().getName();
    }

    public Map<String, String> getLabels() {
        return configMap.getMetadata().getLabels();
    }

    public String getAppLabel() {
        return getLabels().get(APP_LABEL);
    }

    public String getResourceVersion() {
        return configMap.getMetadata().getResourceVersion();
    }

    public Map<String, String> getData() {
        return configMap.getData();
    }

    public void setData(String key, String data) {
        configMap.getData().put(key, data);
    }

    public void updateConfigMapData(String dataName, String content, K8SRequestCallback requestCallback) {
        ThreadFactory.newThread(() -> {
            try {
                K8SClient.updateConfigMapData(getNamespace(), getName(), dataName, content);
                K8SRequestResult.apply(requestCallback, true);
            } catch (K8SApiException e) {
                LOGGER.error("Failed to update config map " + getName(), e);
                Platform.runLater(() -> {
                    Main.showErrorMessage("Failed to update config map " + getName(), e);
                    K8SRequestResult.apply(requestCallback, false, e);
                });
            }
        });
    }

    public void addListener(ConfigMapChangeListener configMapChangeListener) {
        listeners.add(configMapChangeListener);
    }

    public void removeListener(ConfigMapChangeListener configMapChangeListener) {
        listeners.remove(configMapChangeListener);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        K8SConfigMap configMap = (K8SConfigMap) o;
        return Objects.equals(getUid(), configMap.getUid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUid());
    }

    @Override
    public int compareTo(K8SConfigMap o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("K8SConfigMap{");
        sb.append("configMap=").append(configMap);
        sb.append('}');
        return sb.toString();
    }
}
