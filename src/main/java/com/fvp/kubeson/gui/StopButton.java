package com.fvp.kubeson.gui;

import java.io.InputStream;

import javafx.animation.PauseTransition;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

public class StopButton {

    private static LogTab currentTab;

    private static Button stopButton;

    static {
        stopButton = new Button();
        stopButton.setPrefHeight(45);
        stopButton.setFocusTraversable(false);
        InputStream is = ClearButton.class.getClassLoader().getResourceAsStream("icons/stop.png");
        if (is != null) {
            stopButton.setGraphic(new ImageView(new Image(is)));
        }
        Text t1 = new Text("S");
        t1.setFill(Color.WHITE);
        t1.setUnderline(true);
        Text t2 = new Text("TOP LOG FEED");
        t2.setFill(Color.WHITE);
        TextFlow tf = new TextFlow(t1, t2);
        tf.setPrefHeight(Region.USE_PREF_SIZE);
        Tooltip tooltip = new Tooltip();
        tooltip.setGraphic(tf);
        tooltip.getStyleClass().add("tooltips");
        stopButton.setTooltip(tooltip);
        stopButton.setOnAction(event -> {
            if (currentTab != null) {
                currentTab.stop();
            }
        });
    }

    public static Button draw() {
        return stopButton;
    }

    public static void changeTab(LogTab newLogTab) {
        currentTab = newLogTab;
    }

    public static void destroyTab(LogTab logTab) {
        if (logTab.equals(currentTab)) {
            currentTab = null;
        }
    }

    public static void fire() {
        stopButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        PauseTransition pause = new PauseTransition(Duration.seconds(0.2));
        pause.setOnFinished(evt -> {
            stopButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), false);
            stopButton.fire();
        });
        pause.play();
    }
}
