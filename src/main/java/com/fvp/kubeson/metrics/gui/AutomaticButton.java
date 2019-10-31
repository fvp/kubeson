package com.fvp.kubeson.metrics.gui;

import com.fvp.kubeson.common.gui.ToggleButtonBase;

public final class AutomaticButton extends ToggleButtonBase {

    public AutomaticButton(MetricsTab metricTab) {
        super("icons/clear.png", "_AUTOMATIC REFRESH");
        super.setSelected(true);
        super.setOnAction(event -> metricTab.setAutomaticRefresh(super.isSelected()));
    }
}
