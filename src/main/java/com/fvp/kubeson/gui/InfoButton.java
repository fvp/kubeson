package com.fvp.kubeson.gui;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public final class InfoButton {

    private static ImageView imageView;

    private static Image blueInfo;

    private static Image yellowInfo;

    private static Image redInfo;

    private static InfoDialog dialog;

    static {
        blueInfo = new Image(InfoButton.class.getClassLoader().getResourceAsStream("icons/info-blue.png"));
        yellowInfo = new Image(InfoButton.class.getClassLoader().getResourceAsStream("icons/info-yellow.png"));
        redInfo = new Image(InfoButton.class.getClassLoader().getResourceAsStream("icons/info-red.png"));

        imageView = new ImageView(blueInfo);
    }

    private InfoButton() {
    }

    public static Node draw(Scene scene) {
        Label infoLabel = new Label();
        infoLabel.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        infoLabel.setGraphic(imageView);
        infoLabel.setTooltip(new Tooltip("INFO"));
        infoLabel.setOnMouseClicked(event -> {
            dialog = new InfoDialog(scene.getWindow());
            dialog.showAndWait();
        });

        return infoLabel;
    }

    public static void setBlue() {
        imageView.setImage(blueInfo);
    }

    public static void setYellow() {
        imageView.setImage(yellowInfo);
    }

    public static void setRed() {
        imageView.setImage(redInfo);
    }

    public static void refreshUpgrade() {
        if (dialog != null) {
            dialog.refreshUpgrade();
        }
    }
}
