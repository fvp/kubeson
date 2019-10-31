package com.fvp.kubeson.metrics.gui;

import com.fvp.kubeson.common.gui.ButtonBase;

public final class CollapseAllButton extends ButtonBase {

    public CollapseAllButton(MetricsTab metricTab) {
        super("icons/clear.png", "COLLA_PSE ALL");
        super.setOnAction(event -> metricTab.setAllExpanded(false));
    }
}
