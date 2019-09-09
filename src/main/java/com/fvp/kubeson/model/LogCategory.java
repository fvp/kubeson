package com.fvp.kubeson.model;

import com.fvp.kubeson.gui.TabPillButtonList;

public enum LogCategory implements TabPillButtonList {
    SERVICE("SERVICE", "icons/service.png", null),
    COMMUNICATION_IN("COMMUNICATION", "icons/network_in.png", "\"flow\":\"IN\""),
    COMMUNICATION_OUT("COMMUNICATION", "icons/network_out.png", "\"flow\":\"OUT\""),
    DATABASE("DATABASE", "icons/database.png", null),
    HEALTH("HEALTH", "icons/health.png", null),
    CRYPTO("CRYPTO", "icons/crypto.png", null);

    private final String icon;

    private final String name;

    private final String subCategorySearch;

    LogCategory(String name, String icon, String subCategorySearch) {
        this.icon = icon;
        this.name = name;
        this.subCategorySearch = subCategorySearch;
    }

    @Override
    public String getText() {
        return this.name().replace("_", " ");
    }

    @Override
    public String getStyleClass() {
        return "log-category";
    }

    @Override
    public String getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public String getSubCategorySearch() {
        return subCategorySearch;
    }
}
