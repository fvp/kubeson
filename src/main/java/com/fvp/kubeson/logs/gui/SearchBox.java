package com.fvp.kubeson.logs.gui;

import com.fvp.kubeson.common.gui.GlobalKeyPressedEventListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * Search field with styling and a clear button
 */
public class SearchBox extends TextField implements ChangeListener<String>, GlobalKeyPressedEventListener {

    protected static KeyCombination ctrlF = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_ANY);

    private Button clearButton;

    private Button upButton;

    private Button downButton;

    private Region innerBackground;

    private Region icon;

    private String lastKey;

    private LogTab logTab;

    public SearchBox(LogTab logTab) {
        this.logTab = logTab;

        setPrefWidth(370);
        setPrefHeight(30);

        clearButton = new Button();
        upButton = new Button();
        downButton = new Button();
        innerBackground = new Region();
        icon = new Region();

        getStyleClass().addAll("search-box");
        icon.getStyleClass().setAll("search-box-icon");
        innerBackground.getStyleClass().setAll("search-box-inner");
        setPromptText("Search");
        textProperty().addListener(this);

        upButton.getStyleClass().add("search-up-button");
        upButton.setCursor(Cursor.DEFAULT);
        upButton.setOnMouseClicked((MouseEvent t) -> {
            super.requestFocus();
            logTab.getSearchManager().moveUp();
        });
        upButton.setVisible(false);
        upButton.setManaged(false);
        upButton.setFocusTraversable(false);

        downButton.getStyleClass().add("search-down-button");
        downButton.setCursor(Cursor.DEFAULT);
        downButton.setOnMouseClicked((MouseEvent t) -> {
            super.requestFocus();
            logTab.getSearchManager().moveDown();
        });
        downButton.setVisible(false);
        downButton.setManaged(false);
        downButton.setFocusTraversable(false);

        clearButton.getStyleClass().add("search-clear-button");
        clearButton.setCursor(Cursor.DEFAULT);
        clearButton.setOnMouseClicked((MouseEvent t) -> {
            setText("");
            logTab.getLogListView().requestFocus();
        });
        clearButton.setVisible(false);
        clearButton.setManaged(false);

        innerBackground.setManaged(false);
        icon.setManaged(false);

        // Keyboard Shortcuts
        super.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.UP) {
                logTab.getSearchManager().moveUp();
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.DOWN) {
                logTab.getSearchManager().moveDown();
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.PAGE_UP) {
                logTab.getSearchManager().moveFirst();
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.PAGE_DOWN) {
                logTab.getSearchManager().moveLast();
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                setText("");
                logTab.getLogListView().requestFocus();
                keyEvent.consume();
            }
        });
        // Avoid space after some special accent characters
        super.setOnKeyTyped(e -> {
            if (" ".equals(e.getCharacter()) && ("'".equals(lastKey) || "\"".equals(lastKey)) || "`".equals(lastKey) || "~".equals(lastKey)) {
                e.consume();
            }
            lastKey = e.getCharacter();
        });
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (upButton.getParent() != this) {
            getChildren().add(upButton);
        }
        if (downButton.getParent() != this) {
            getChildren().add(downButton);
        }
        if (clearButton.getParent() != this) {
            getChildren().add(clearButton);
        }
        if (innerBackground.getParent() != this) {
            getChildren().add(0, innerBackground);
        }
        if (icon.getParent() != this) {
            getChildren().add(icon);
        }
        innerBackground.setLayoutX(0);
        innerBackground.setLayoutY(0);
        innerBackground.resize(getWidth(), getHeight());
        icon.setLayoutX(0);
        icon.setLayoutY(0);
        icon.resize(35, 30);
        upButton.setLayoutX(getWidth() - 70);
        upButton.setLayoutY(0);
        upButton.resize(30, 30);
        downButton.setLayoutX(getWidth() - 50);
        downButton.setLayoutY(0);
        downButton.resize(30, 30);
        clearButton.setLayoutX(getWidth() - 30);
        clearButton.setLayoutY(0);
        clearButton.resize(30, 30);
    }

    @Override
    public void changed(ObservableValue<? extends String> ov, String oldValue, String newValue) {
        clearButton.setVisible(newValue.length() > 0);
        upButton.setVisible(newValue.length() > 0);
        downButton.setVisible(newValue.length() > 0);

        logTab.getSearchManager().setSearchText(newValue);
    }

    @Override
    public void onGlobalKeyPressedEvent(KeyEvent keyEvent) {
        if (ctrlF.match(keyEvent)) {
            requestFocus();
        }
    }
}

