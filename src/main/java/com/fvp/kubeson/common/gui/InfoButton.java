package com.fvp.kubeson.common.gui;

import com.fvp.kubeson.Main;
import javafx.scene.Node;
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

    private static Label infoLabel;

    static {
        infoLabel = new Label();
        blueInfo = Main.getImage("icons/info-blue.png");
        yellowInfo = Main.getImage("icons/info-yellow.png");
        redInfo = Main.getImage("icons/info-red.png");

        imageView = new ImageView(blueInfo);
    }

    private InfoButton() {
    }

    public static Node draw() {
        infoLabel = new Label();
        infoLabel.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        infoLabel.setGraphic(imageView);
        infoLabel.setTooltip(new Tooltip("INFO"));
        infoLabel.setOnMouseClicked(event -> {
            fire();
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

    public static void fire() {
        dialog = new InfoDialog();
        dialog.showAndWait();
    }
}
