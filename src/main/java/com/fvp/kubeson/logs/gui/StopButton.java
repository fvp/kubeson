package com.fvp.kubeson.logs.gui;

import com.fvp.kubeson.common.gui.ButtonBase;

public final class StopButton extends ButtonBase {

    public StopButton(LogTab logTab) {
        super("icons/stop.png", "_STOP LOG FEED");
        super.setOnAction(event -> logTab.stop());
    }
}
