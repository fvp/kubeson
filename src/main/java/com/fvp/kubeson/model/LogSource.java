package com.fvp.kubeson.model;

import javafx.scene.paint.Color;

public class LogSource {

    private String name;

    private Color color;

    public LogSource(String name, Color color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LogSource{");
        sb.append("name='").append(name).append('\'');
        sb.append(", color=").append(color);
        sb.append('}');
        return sb.toString();
    }
}
