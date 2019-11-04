package com.fvp.kubeson.metrics.gui;

import com.fvp.kubeson.common.gui.ToggleButtonBase;

public final class AutomaticButton extends ToggleButtonBase {

    public AutomaticButton(MetricsTab metricTab) {
        super("icons/automatic_35x35.png", "_AUTOMATIC REFRESH");
        super.setOnAction(event -> metricTab.setAutomaticRefresh(super.isSelected()));
    }
}
