package com.fvp.kubeson.common.gui;

import javafx.scene.control.Tab;
import javafx.scene.input.KeyEvent;

public abstract class MainTab<Toolbar extends IToolbar & GlobalKeyPressedEventListener> extends Tab implements GlobalKeyPressedEventListener {

    private Toolbar toolbar;

    protected MainTab(String name) {
        super(name);
    }

    public Toolbar getToolbar() {
        return this.toolbar;
    }

    protected void setToolbar(Toolbar toolbar) {
        this.toolbar = toolbar;
    }

    public void onSelected() {
        MainToolbar.selectToolbar(this.toolbar);
    }

    @Override
    public void onGlobalKeyPressedEvent(KeyEvent keyEvent) {
        toolbar.onGlobalKeyPressedEvent(keyEvent);
    }

    public void setStyle(String style, boolean addOrRemove) {
        if (addOrRemove) {
            if (!getStyleClass().contains(style)) {
                getStyleClass().add(style);
            }
        } else {
            getStyleClass().remove(style);
        }
    }
}
