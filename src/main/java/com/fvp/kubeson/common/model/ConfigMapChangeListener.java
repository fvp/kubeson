package com.fvp.kubeson.common.model;

public interface ConfigMapChangeListener {

    void onConfigMapChange(K8SConfigMap configMap);
}
