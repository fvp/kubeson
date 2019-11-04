package com.fvp.kubeson.configmap.gui;

import com.fvp.kubeson.common.gui.ButtonBase;

public final class DeletePodsButton extends ButtonBase {

    public DeletePodsButton(ConfigMapTab configMapTab) {
        super("icons/delete_35x35.png", "_DELETE PODS WITH SAME APP LABEL");
        super.setOnAction(event -> configMapTab.deletePods());
    }
}
