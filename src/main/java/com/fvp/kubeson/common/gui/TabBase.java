package com.fvp.kubeson.common.gui;

import javafx.scene.control.Tab;
import javafx.scene.input.KeyEvent;

public abstract class TabBase<Toolbar extends IToolbar & GlobalKeyPressedEventListener> extends Tab implements GlobalKeyPressedEventListener {

    private Toolbar toolbar;

    private TabLabel tabLabel;

    protected TabBase(TabLabel tabLabel) {
        this.tabLabel = tabLabel;
        super.setGraphic(tabLabel);
    }

    public Toolbar getToolbar() {
        return this.toolbar;
    }

    public TabLabel getTabLabel() {
        return tabLabel;
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
}
