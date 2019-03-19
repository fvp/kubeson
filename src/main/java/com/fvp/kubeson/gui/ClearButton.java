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

public final class ClearButton {

    private static LogTab currentTab;

    private static Button clearButton;

    static {
        clearButton = new Button();
        clearButton.setPrefHeight(45);
        clearButton.setFocusTraversable(false);
        InputStream is = ClearButton.class.getClassLoader().getResourceAsStream("icons/clear.png");
        if (is != null) {
            clearButton.setGraphic(new ImageView(new Image(is)));
        }
        Text t1 = new Text("CLEA");
        t1.setFill(Color.WHITE);
        Text t2 = new Text("R");
        t2.setFill(Color.WHITE);
        t2.setUnderline(true);
        Text t3 = new Text(" LOG");
        t3.setFill(Color.WHITE);
        TextFlow tf = new TextFlow(t1, t2, t3);
        tf.setPrefHeight(Region.USE_PREF_SIZE);
        Tooltip tooltip = new Tooltip();
        tooltip.setGraphic(tf);
        tooltip.getStyleClass().add("tooltips");
        clearButton.setTooltip(tooltip);
        clearButton.setOnAction(event -> {
            if (currentTab != null) {
                currentTab.reset();
            }
        });
    }

    public static Button draw() {
        return clearButton;
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
        clearButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        PauseTransition pause = new PauseTransition(Duration.seconds(0.2));
        pause.setOnFinished(evt -> {
            clearButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), false);
            clearButton.fire();
        });
        pause.play();
    }

}
