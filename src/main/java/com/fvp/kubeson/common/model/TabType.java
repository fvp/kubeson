package com.fvp.kubeson.common.model;

import javafx.scene.paint.Color;

public enum TabType {
    LOG("Log", Color.web("#f0810f")),
    METRICS("Metrics", Color.web("#3CAEA3")),
    CONFIG_MAP("ConfigMap", Color.web("#cc66ff"));

    private String name;

    private Color color;

    TabType(String name, Color color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }
}
