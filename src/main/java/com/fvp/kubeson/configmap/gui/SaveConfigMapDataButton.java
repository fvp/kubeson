package com.fvp.kubeson.configmap.gui;

import com.fvp.kubeson.common.gui.ButtonBase;

public final class SaveConfigMapDataButton extends ButtonBase {

    public SaveConfigMapDataButton(ConfigMapTab configMapTab) {
        super("icons/save_35x35.png", "_SAVE");
        super.setOnAction(event -> configMapTab.saveConfigMapFile());
    }
}
