package com.fvp.kubeson.common.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.input.KeyEvent;

public abstract class IToolbar implements GlobalKeyPressedEventListener {

    private List<Node> toolbarItems;

    private List<GlobalKeyPressedEventListener> globalKeyPressedListeners;

    protected IToolbar() {
        this.toolbarItems = new ArrayList<>();
        this.globalKeyPressedListeners = new ArrayList<>();
    }

    protected void addToolbarItem(Node toolbarItem) {
        toolbarItems.add(toolbarItem);
    }

    protected void addListeners(GlobalKeyPressedEventListener... globalKeyPressedEventListeners) {
        this.globalKeyPressedListeners.addAll(Arrays.asList(globalKeyPressedEventListeners));
    }

    public List<Node> getToolbarItems() {
        return this.toolbarItems;
    }

    @Override
    public void onGlobalKeyPressedEvent(KeyEvent keyEvent) {
        globalKeyPressedListeners.forEach(listener -> listener.onGlobalKeyPressedEvent(keyEvent));
    }
}
