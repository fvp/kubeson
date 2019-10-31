package com.fvp.kubeson.configmap.gui;

import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class ConfigMapDataNameSelector extends VBox {

    public ConfigMapDataNameSelector(ConfigMapTab configMapTab) {

        Text t1 = new Text("Data Name");
        t1.getStyleClass().add("selector-label");
        t1.setFill(Color.WHITE);

        ChoiceBox<String> choiceBox = new ChoiceBox<>();
        choiceBox.getItems().addAll(configMapTab.getConfigMapFiles());
        choiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                configMapTab.selectConfigMapDataName(newValue);
            }
        });

        choiceBox.getSelectionModel().selectFirst();

        super.setSpacing(3.5);
        super.setAlignment(Pos.CENTER_LEFT);
        super.getChildren().addAll(t1, choiceBox);
    }

}
