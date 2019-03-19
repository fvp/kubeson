package com.fvp.kubeson.model;

import com.fvp.kubeson.gui.TabPillButtonList;

public enum LogCategory implements TabPillButtonList {
    SERVICE("icons/service.png"), COMMUNICATION("icons/network.png"), DATABASE("icons/database.png"), HEALTH("icons/health.png"), CRYPTO("icons/crypto.png");

    private final String icon;

    LogCategory(String icon) {
        this.icon = icon;
    }

    @Override
    public String getText() {
        return this.name();
    }

    @Override
    public String getStyleClass() {
        return null;
    }

    @Override
    public String getIcon() {
        return icon;
    }
}
