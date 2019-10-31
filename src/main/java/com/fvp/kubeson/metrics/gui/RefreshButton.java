package com.fvp.kubeson.metrics.gui;

import com.fvp.kubeson.common.gui.ButtonBase;

public final class RefreshButton extends ButtonBase {

    public RefreshButton(MetricsTab metricTab) {
        super("icons/clear.png", "_REFRESH");
        super.setOnAction(event -> metricTab.refreshMetrics());
    }
}