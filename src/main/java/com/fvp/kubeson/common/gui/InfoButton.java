package com.fvp.kubeson.common.gui;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

public final class InfoButton {

    private static ImageView imageView;

    private static Image blueInfo;

    private static Image yellowInfo;

    private static Image redInfo;

    private static InfoDialog dialog;

    static Label infoLabel = new Label();

    private static PopOver upgradePopUp = new PopOver();

    static {
        blueInfo = new Image(InfoButton.class.getClassLoader().getResourceAsStream("icons/info-blue.png"));
        yellowInfo = new Image(InfoButton.class.getClassLoader().getResourceAsStream("icons/info-yellow.png"));
        redInfo = new Image(InfoButton.class.getClassLoader().getResourceAsStream("icons/info-red.png"));

        imageView = new ImageView(blueInfo);

        upgradePopUp.setContentNode(new Text("Upgrade\nAvailable!"));
        upgradePopUp.setDetachable(false);
        upgradePopUp.setTitle("   Tip");
        upgradePopUp.setCloseButtonEnabled(true);
        upgradePopUp.setArrowLocation(ArrowLocation.RIGHT_TOP);
        upgradePopUp.setFadeInDuration(Duration.millis(500));
        upgradePopUp.setFadeOutDuration(Duration.millis(500));
        upgradePopUp.setHeaderAlwaysVisible(true);
        upgradePopUp.setAutoHide(true);
        upgradePopUp.getStyleClass().add("popover");
        upgradePopUp.setHideOnEscape(true);
    }

    private InfoButton() {
    }

    public static Node draw(Scene scene) {
        infoLabel = new Label();
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
        showPopup();
    }

    public static void setRed() {
        imageView.setImage(redInfo);
    }

    public static void refreshUpgrade() {
        if (dialog != null) {
            dialog.refreshUpgrade();
        }
    }

    public static void showPopup() {
        upgradePopUp.show(infoLabel, -5);
        PauseTransition pause = new PauseTransition(Duration.seconds(15));
        pause.setOnFinished(evt -> upgradePopUp.hide());
        pause.play();
    }
}
