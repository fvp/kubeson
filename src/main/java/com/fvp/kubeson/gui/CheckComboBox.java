package com.fvp.kubeson.gui;

import com.fvp.kubeson.model.ItemType;
import com.fvp.kubeson.model.SelectorItem;
import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Skin;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

public class CheckComboBox extends ComboBox<SelectorItem> {

    public CheckComboBox() {
        super.getStyleClass().add("selector-pod-name");
        super.setFocusTraversable(false);
        super.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        super.setVisibleRowCount(30);

        super.setCellFactory(listView -> {
            CheckBoxListCell result = new CheckBoxListCell();
            result.setOnMouseClicked(e -> super.hide());

            return result;
        });

        super.setButtonCell(new ListCell<SelectorItem>() {

            @Override
            protected void updateItem(SelectorItem item, boolean empty) {
            }
        });
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ComboBoxListViewSkin<SelectorItem>(this) {

            // overridden to prevent the popup from disappearing
            @Override
            protected boolean isHideOnClickEnabled() {
                return false;
            }
        };
    }

    private class CheckBoxListCell extends ListCell<SelectorItem> {

        private final CheckBox checkBox = new CheckBox();

        private Property<Boolean> selectedProperty;

        private Property<Boolean> disabledProperty;

        public CheckBoxListCell() {
            checkBox.setPrefHeight(Region.USE_COMPUTED_SIZE);
            checkBox.setStyle("-fx-padding: 2 2 2 1;");
        }

        @Override
        public void updateItem(SelectorItem selectorItem, boolean empty) {
            super.updateItem(selectorItem, empty);

            if (selectorItem == null || empty) {
                setGraphic(null);
                return;
            }

            Text text = new Text(selectorItem.getText());
            text.setStyle(selectorItem.getStyle());

            if (selectorItem.getType() != ItemType.TEXT) {
                HBox hBox = new HBox(checkBox, text);
                hBox.setAlignment(Pos.CENTER_LEFT);
                setGraphic(hBox);

                if (selectedProperty != null) {
                    checkBox.selectedProperty().unbindBidirectional(selectedProperty);
                }
                selectedProperty = selectorItem.getCheckedProperty();
                if (selectedProperty == null) {
                    selectedProperty = new SimpleBooleanProperty();
                    selectorItem.setCheckedProperty(selectedProperty);
                    selectedProperty.addListener((observable, oldValue, newValue) -> checkForDuplicatedPod(selectorItem, newValue));
                }
                checkBox.selectedProperty().bindBidirectional(selectedProperty);

                if (disabledProperty != null) {
                    checkBox.disableProperty().unbindBidirectional(disabledProperty);
                }
                disabledProperty = selectorItem.getDisabledProperty();
                checkBox.disableProperty().bindBidirectional(disabledProperty);
            } else {
                setGraphic(text);
            }
        }

        private void checkForDuplicatedPod(SelectorItem selectorItem, Boolean newValue) {
            if (selectorItem.getContainer() == null) {
                for (SelectorItem item : getListView().getItems()) {
                    if (!item.equals(selectorItem) && item.getPod() != null && item.getContainer() == null && item.getPod()
                        .getPodName()
                        .equals(selectorItem.getPod().getPodName())) {
                        item.setDisabledProperty(newValue);
                    }
                }
            }
        }
    }
}
