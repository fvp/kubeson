package com.fvp.kubeson.metrics.gui;

import com.fvp.kubeson.common.gui.ButtonBase;

public final class ExpandAllButton extends ButtonBase {

    public ExpandAllButton(MetricsTab metricTab) {
        super("icons/clear.png", "_EXPAND ALL");
        super.setOnAction(event -> metricTab.setAllExpanded(true));
    }
}
