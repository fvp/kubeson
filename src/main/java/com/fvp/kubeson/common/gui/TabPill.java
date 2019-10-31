package com.fvp.kubeson.common.gui;

import java.util.ArrayList;
import java.util.List;

import com.fvp.kubeson.logs.gui.LogTab;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class TabPill<T extends TabPillButtonList> extends HBox {

    private List<TabPillButton> tabPillButtons;

    private LogTab logTab;

    public TabPill(LogTab logTab, Class<T> buttonList) {
        this.logTab = logTab;
        this.tabPillButtons = new ArrayList<>();
        for (final T enumConstant : buttonList.getEnumConstants()) {
            this.tabPillButtons.add(new TabPillButton(enumConstant, enumConstant.getText()));
        }
        draw();
    }

    private void broadcast(TabPillButton tabPillButton, boolean selected) {
        logTab.filter(tabPillButton.key, selected);
    }

    private void draw() {
        super.setAlignment(Pos.CENTER);
        //super.setPrefWidth(Region.USE_PREF_SIZE);

        for (int i = 0; i < tabPillButtons.size(); i++) {
            TabPillButton tabPillButton = tabPillButtons.get(i);
            ToggleButton toggleButton = tabPillButton.toggleButton;
            toggleButton.selectedProperty().addListener((observable, oldValue, newValue) -> broadcast(tabPillButton, newValue));
            if (i == 0) {
                toggleButton.getStyleClass().add("left-pill");
            } else if (i == tabPillButtons.size() - 1) {
                toggleButton.getStyleClass().add("right-pill");
            } else {
                toggleButton.getStyleClass().add("center-pill");
            }

            super.getChildren().add(toggleButton);
            HBox.setHgrow(toggleButton, Priority.ALWAYS);
        }
    }

    private class TabPillButton {

        T key;

        ToggleButton toggleButton;

        TabPillButton(T key, String text) {
            this.key = key;
            this.toggleButton = new ToggleButton();
            this.toggleButton.setSelected(true);
            this.toggleButton.setPrefHeight(45);
            this.toggleButton.setMaxWidth(70);
            this.toggleButton.setPrefWidth(Region.USE_PREF_SIZE);
            this.toggleButton.setMinWidth(54);
            this.toggleButton.setFocusTraversable(false);
            if (key.getStyleClass() != null) {
                this.toggleButton.getStyleClass().add(key.getStyleClass());
            }
            if (key.getIcon() != null) {
                this.toggleButton.setGraphic(new ImageView(key.getIcon()));
                Tooltip tooltip = new Tooltip(text);
                tooltip.getStyleClass().add("tooltips");
                this.toggleButton.setTooltip(tooltip);
            } else {
                this.toggleButton.setText(text);
            }

        }

        void setSelected(boolean selected) {
            toggleButton.setSelected(selected);
        }
    }
}
