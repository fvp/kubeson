package com.fvp.kubeson.common.controller;

import com.fvp.kubeson.common.model.K8SConfigMap;
import com.fvp.kubeson.common.model.K8SPod;

public interface K8SClientListener {

    void onPodChange(K8SResourceChange<K8SPod> changes);

    void onConfigMapChange(K8SResourceChange<K8SConfigMap> changes);
}
