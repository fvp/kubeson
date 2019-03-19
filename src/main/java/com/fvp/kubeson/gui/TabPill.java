package com.fvp.kubeson.gui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.apache.commons.lang3.StringUtils;

public class TabPill<T extends TabPillButtonList> extends HBox {

    private LogTab currentTab;

    private List<TabPillButton> tabPillButtons;

    public TabPill(Class<T> buttonList) {
        this.tabPillButtons = new ArrayList<>();
        for (final T enumConstant : buttonList.getEnumConstants()) {
            this.tabPillButtons.add(new TabPillButton(enumConstant, enumConstant.getText()));
        }
        draw();
    }

    public void changeTab(LogTab newLogTab) {
        currentTab = null;
        for (TabPillButton button : tabPillButtons) {
            button.setSelected(newLogTab.getFilterState(button.key));
        }
        currentTab = newLogTab;
    }

    public void destroyTab(LogTab logTab) {
        if (logTab.equals(currentTab)) {
            tabPillButtons.forEach(tabPillButton -> tabPillButton.setSelected(false));
        }
    }

    private void broadcast(TabPillButton tabPillButton, boolean selected) {
        if (currentTab != null) {
            currentTab.filter(tabPillButton.key, selected);
        }
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
            this.toggleButton.setSelected(false);
            this.toggleButton.setPrefHeight(45);
            this.toggleButton.setMaxWidth(70);
            this.toggleButton.setPrefWidth(Region.USE_PREF_SIZE);
            this.toggleButton.setMinWidth(58);
            this.toggleButton.setFocusTraversable(false);
            if (key.getStyleClass() != null) {
                this.toggleButton.getStyleClass().add(key.getStyleClass());
            }
            if (!StringUtils.isEmpty(key.getIcon())) {
                InputStream is = getClass().getClassLoader().getResourceAsStream(key.getIcon());
                if (is != null) {
                    this.toggleButton.setGraphic(new ImageView(new Image(is)));
                    Tooltip tooltip = new Tooltip(text);
                    tooltip.getStyleClass().add("tooltips");
                    this.toggleButton.setTooltip(tooltip);
                }
            } else {
                this.toggleButton.setText(text);
            }

        }

        void setSelected(boolean selected) {
            toggleButton.setSelected(selected);
        }
    }
}
