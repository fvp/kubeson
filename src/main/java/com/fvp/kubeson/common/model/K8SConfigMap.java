package com.fvp.kubeson.common.model;

import static com.fvp.kubeson.common.model.K8SPod.APP_LABEL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fvp.kubeson.common.controller.K8SApiException;
import com.fvp.kubeson.common.controller.K8SClient;
import io.fabric8.kubernetes.api.model.ConfigMap;

public class K8SConfigMap implements Comparable<K8SConfigMap> {

    private ConfigMap configMap;

    private List<ConfigMapChangeListener> listeners;

    public K8SConfigMap(ConfigMap configMap) {
        this.configMap = configMap;
        this.listeners = new ArrayList<>();
    }

    public void updateConfigMap(ConfigMap configMap) {
        this.configMap = configMap;
        this.listeners.forEach(listener -> listener.onConfigMapChange(this));
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

    public void updateConfigMapData(String filename, String content) throws K8SApiException {
        K8SClient.updateConfigMapData(getNamespace(), getName(), filename, content);
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
        return getName();
    }
}
