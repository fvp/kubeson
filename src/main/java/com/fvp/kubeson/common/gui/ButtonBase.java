package com.fvp.kubeson.common.gui;

import com.fvp.kubeson.Main;
import javafx.animation.PauseTransition;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

public abstract class ButtonBase extends Button implements GlobalKeyPressedEventListener {

    private KeyCombination buttonShortcut;

    protected ButtonBase(String iconPath, String shortcutText) {
        super.setPrefHeight(45);
        super.setFocusTraversable(false);
        Image img = Main.getImage(iconPath);
        if (img != null) {
            super.setGraphic(new ImageView(img));
        }
        createButtonTooltip(shortcutText);
    }

    protected void createButtonTooltip(String text) {
        int shortcutCharPos = text.indexOf('_');

        TextFlow tf = new TextFlow();
        tf.setPrefHeight(Region.USE_PREF_SIZE);

        if (shortcutCharPos > 0) {
            Text t1 = new Text(text.substring(0, shortcutCharPos));
            t1.setFill(Color.WHITE);
            tf.getChildren().add(t1);
        }

        String shortcutChar = text.substring(shortcutCharPos + 1, shortcutCharPos + 2);
        Text t2 = new Text(shortcutChar);
        t2.setFill(Color.WHITE);
        t2.setUnderline(true);
        tf.getChildren().add(t2);

        Text t3 = new Text(text.substring(shortcutCharPos + 2));
        t3.setFill(Color.WHITE);
        tf.getChildren().add(t3);

        Tooltip tooltip = new Tooltip();
        tooltip.setGraphic(tf);
        tooltip.getStyleClass().add("tooltips");

        super.setTooltip(tooltip);
        buttonShortcut = new KeyCodeCombination(KeyCode.getKeyCode(shortcutChar), KeyCombination.CONTROL_ANY);
    }

    public void fire() {
        super.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        PauseTransition pause = new PauseTransition(Duration.seconds(0.2));
        pause.setOnFinished(evt -> {
            super.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), false);
            super.fire();
        });
        pause.play();
    }

    @Override
    public void onGlobalKeyPressedEvent(KeyEvent keyEvent) {
        if (buttonShortcut != null && buttonShortcut.match(keyEvent)) {
            fire();
        }
    }
}
