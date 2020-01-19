package com.fvp.kubeson.logs.model;

import com.fvp.kubeson.Main;
import com.fvp.kubeson.common.gui.TabPillButtonList;
import javafx.scene.image.Image;

public enum LogCategory implements TabPillButtonList {
    SERVICE("SERVICE", "icons/service_35x35.png", "icons/service_16x16.png", null, false),
    COMMUNICATION_IN("COMMUNICATION", "icons/arrow-up_35x35.png", "icons/arrow-up_16x16.png", "\"flow\":\"IN\"", true),
    COMMUNICATION_OUT("COMMUNICATION", "icons/arrow-down_35x35.png", "icons/arrow-down_16x16.png", "\"flow\":\"OUT\"", true),
    DATABASE("DATABASE", "icons/database_35x35.png", "icons/database_16x16.png", null, false),
    HEALTH("HEALTH", "icons/health_35x35.png", "icons/health_16x16.png", null, false),
    CRYPTO("CRYPTO", "icons/crypto_35x35.png", "icons/crypto_16x16.png", null, false);

    private final Image iconBig;

    private final Image iconSmall;

    private final String name;

    private final String categorySearch;

    private final String subCategorySearch;

    private final boolean searchHttpMethod;

    LogCategory(String name, String iconPathBig, String iconPathSmall, String subCategorySearch, boolean searchHttpMethod) {
        this.name = name;
        this.categorySearch = "\"" + name + "\"";
        this.subCategorySearch = subCategorySearch;
        this.searchHttpMethod = searchHttpMethod;
        this.iconBig = Main.getImage(iconPathBig);
        this.iconSmall = Main.getImage(iconPathSmall);
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
    public Image getIcon() {
        return iconBig;
    }

    public String getName() {
        return name;
    }

    public String getCategorySearch() {
        return categorySearch;
    }

    public String getSubCategorySearch() {
        return subCategorySearch;
    }

    public boolean searchHttpMethod() {
        return searchHttpMethod;
    }

    public Image getSmallIcon() {
        return iconSmall;
    }
}
