package com.fvp.kubeson.common.controller;

import com.fvp.kubeson.common.model.K8SConfigMap;
import com.fvp.kubeson.common.model.K8SPod;

public interface K8SClientListener {

    void onPodTerminated(K8SPod pod);

    void onNewPod(K8SPod newPod);

    void onNewConfigMap(K8SConfigMap configMap);

    void onConfigMapChange(K8SConfigMap configMap);
}
