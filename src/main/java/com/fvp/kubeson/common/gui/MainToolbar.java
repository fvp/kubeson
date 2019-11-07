package com.fvp.kubeson.common.gui;

import javafx.scene.Parent;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class MainToolbar {

    private static ToolBar toolbar;

    private static HBox emptyToolbox;

    static {
        emptyToolbox = new HBox();
        HBox.setHgrow(emptyToolbox, Priority.ALWAYS);
    }

    private MainToolbar() {
    }

    public static Parent draw() {
        //Tool Bar
        toolbar = new ToolBar();
        // Set Pod Selector
        toolbar.getItems().add(ResourceSelector.draw());

        // Central Pane
        toolbar.getItems().add(emptyToolbox);

        // Set Info Button
        toolbar.getItems().add(InfoButton.draw());

        return toolbar;
    }

    public static void selectToolbar(IToolbar toolbarComponent) {
        toolbar.getItems().remove(1, toolbar.getItems().size() - 1);
        toolbar.getItems().addAll(1, toolbarComponent.getToolbarItems());
    }

    public static void clear() {
        toolbar.getItems().remove(1, toolbar.getItems().size() - 1);
        toolbar.getItems().add(1, emptyToolbox);
    }
}
