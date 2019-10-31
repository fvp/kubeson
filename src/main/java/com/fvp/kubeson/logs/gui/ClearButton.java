package com.fvp.kubeson.logs.gui;

import com.fvp.kubeson.common.gui.ButtonBase;

public final class ClearButton extends ButtonBase {

    public ClearButton(LogTab logTab) {
        super("icons/clear.png", "CLEA_R LOG");
        super.setOnAction(event -> logTab.reset());
    }
}
