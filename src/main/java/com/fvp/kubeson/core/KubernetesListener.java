package com.fvp.kubeson.core;

import com.fvp.kubeson.model.Pod;

public interface KubernetesListener {

    void onPodTerminated(Pod pod);

    void onNewPod(Pod newPod);
}
