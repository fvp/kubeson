package com.fvp.kubeson.common.gui;

import java.util.ArrayList;
import java.util.List;

import com.fvp.kubeson.logs.gui.LogTab;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

public class TabPill<T extends TabPillButtonList> {

    private int minButtonWidth;

    private List<TabPillButton> tabPillButtons;

    private Orientation orientation;

    private TabPillButton parent;

    private LogTab logTab;

    public TabPill(int minButtonWidth, Orientation orientation, LogTab logTab, Class<T> buttonList) {
        this.minButtonWidth = minButtonWidth;
        this.orientation = orientation;
        this.logTab = logTab;
        this.tabPillButtons = new ArrayList<>();
        for (final T enumConstant : buttonList.getEnumConstants()) {
            this.tabPillButtons.add(new TabPillButton(enumConstant, enumConstant.getText()));
        }
    }

    public void setPopup(int idx, TabPill content) {
        content.parent = tabPillButtons.get(idx);
        content.parent.setPopup(content);
    }

    private void broadcast(TabPillButton tabPillButton, boolean selected) {
        if (parent != null) {
            logTab.filter(parent.key, tabPillButton.key, selected);
        } else {
            logTab.filter(tabPillButton.key, null, selected);
        }
    }

    private void setDefault() {
        for (int i = 0; i < tabPillButtons.size(); i++) {
            tabPillButtons.get(i).setSelected(false, false);
        }
    }

    private boolean isDefault() {
        for (int i = 0; i < tabPillButtons.size(); i++) {
            if (tabPillButtons.get(i).toggleButton.isSelected()) {
                return false;
            }
        }
        return true;
    }

    public Pane draw() {
        Node[] buttons = new Node[tabPillButtons.size()];

        for (int i = 0; i < tabPillButtons.size(); i++) {
            ToggleButton toggleButton = tabPillButtons.get(i).toggleButton;
            if (i == 0) {
                toggleButton.getStyleClass().add("left-pill-" + orientation.name().toLowerCase());
            } else if (i == tabPillButtons.size() - 1) {
                toggleButton.getStyleClass().add("right-pill-" + orientation.name().toLowerCase());
            } else {
                toggleButton.getStyleClass().add("center-pill-" + orientation.name().toLowerCase());
            }

            HBox.setHgrow(toggleButton, Priority.ALWAYS);
            VBox.setVgrow(toggleButton, Priority.ALWAYS);
            buttons[i] = toggleButton;
        }

        if (orientation == Orientation.VERTICAL) {
            VBox vBox = new VBox(buttons);
            vBox.setAlignment(Pos.CENTER);
            VBox.setVgrow(vBox, Priority.ALWAYS);

            return vBox;
        } else {
            HBox hBox = new HBox(buttons);
            hBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(hBox, Priority.ALWAYS);

            return hBox;
        }
    }

    public enum Orientation {HORIZONTAL, VERTICAL}

    private class TabPillButton {

        T key;

        ToggleButton toggleButton;

        boolean enteredPopup;

        boolean enteredButton;

        boolean broadcast;

        TabPill popUp;

        TabPillButton(T key, String text) {
            this.key = key;
            this.broadcast = true;
            this.toggleButton = new ToggleButton();
            this.toggleButton.setSelected(false);
            this.toggleButton.setPrefHeight(45);
            this.toggleButton.setMaxWidth(70);
            this.toggleButton.setPrefWidth(Region.USE_PREF_SIZE);
            this.toggleButton.setMinWidth(minButtonWidth);
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
            this.toggleButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (this.broadcast) {
                    broadcast(this, newValue);
                } else {
                    this.broadcast = true;
                }

                if (newValue) {
                    if (popUp != null) {
                        setSubTabPillSelection(false);
                        popUp.setDefault();
                    }
                    if (parent != null) {
                        parent.setSubTabPillSelection(true);
                        parent.setSelected(false, false);
                    }
                } else if (parent != null && isDefault()) {
                    parent.setSubTabPillSelection(false);
                }
            });
        }

        void setSelected(boolean selected, boolean broadcast) {
            if (this.toggleButton.isSelected() != selected) {
                this.broadcast = broadcast;
                this.toggleButton.setSelected(selected);
            }
        }

        void setPopup(TabPill popUp) {
            this.popUp = popUp;
            Pane pane = popUp.draw();
            pane.setStyle("-fx-padding: 7");
            PopOver popOver = new PopOver(pane);
            popOver.setArrowLocation(ArrowLocation.TOP_CENTER);
            popOver.setDetachable(false);
            //popOver.setAutoHide(false);

            popOver.getScene().setOnMouseEntered((event) -> {
                enteredPopup = true;
            });
            popOver.getScene().setOnMouseExited((event) -> {
                enteredPopup = false;
                if (!enteredButton) {
                    popOver.hide();
                }
            });
            this.toggleButton.setOnMouseEntered((event) -> {
                enteredButton = true;
                if (!popOver.isShowing()) {
                    popOver.show(toggleButton);
                    Path border = (Path) popOver.getScene().lookup(".border");
                    border.setFill(Color.rgb(60, 63, 65));
                    border.setStroke(Color.web("#6F7375"));
                    border.setStrokeWidth(2.0);
                }
            });
            this.toggleButton.setOnMouseExited((event) -> {
                enteredButton = false;
                if (!enteredPopup) {
                    popOver.hide();
                }
            });
        }

        void setSubTabPillSelection(boolean selected) {
            if (selected) {
                if (!this.toggleButton.getStyleClass().contains("sub-tabpill-selected")) {
                    this.toggleButton.getStyleClass().add("sub-tabpill-selected");
                }
            } else {
                this.toggleButton.getStyleClass().remove("sub-tabpill-selected");
            }
        }
    }
}
