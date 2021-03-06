package com.fvp.kubeson.logs.gui;

import com.fvp.kubeson.common.gui.IToolbar;
import com.fvp.kubeson.common.gui.TabPill;
import com.fvp.kubeson.common.gui.TabPill.Orientation;
import com.fvp.kubeson.logs.model.HttpMethod;
import com.fvp.kubeson.logs.model.LogCategory;
import com.fvp.kubeson.logs.model.LogLevel;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class LogToolbar extends IToolbar {

    private Text searchCounter;

    private ClearButton clearButton;

    private StopButton stopButton;

    private SearchBox searchBox;

    public LogToolbar(LogTab logTab) {
        // Set Log Level Pill
        TabPill<LogLevel> logLevelPill = new TabPill<>(54, Orientation.HORIZONTAL, logTab, LogLevel.class);

        // Buttons
        clearButton = new ClearButton(logTab);
        stopButton = new StopButton(logTab);
        HBox buttons = new HBox(clearButton, stopButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setStyle("-fx-padding: 0 20 0 20");
        buttons.setSpacing(23);
        buttons.setPrefWidth(Region.USE_PREF_SIZE);
        //HBox.setHgrow(buttons, Priority.ALWAYS);

        // Set Log Category Pill
        TabPill<LogCategory> ulfCategoryPill = new TabPill<>(54, Orientation.HORIZONTAL, logTab, LogCategory.class);

        TabPill<HttpMethod> httpMethodsUp = new TabPill<>(69, Orientation.VERTICAL, logTab, HttpMethod.class);
        ulfCategoryPill.setPopup(1, httpMethodsUp);

        TabPill<HttpMethod> httpMethodsDown = new TabPill<>(69, Orientation.VERTICAL, logTab, HttpMethod.class);
        ulfCategoryPill.setPopup(2, httpMethodsDown);

        HBox centralArea = new HBox(logLevelPill.draw(), buttons, ulfCategoryPill.draw());
        centralArea.setAlignment(Pos.CENTER);
        centralArea.setStyle("-fx-padding: 0 20 0 20");
        //centralArea.setSpacing(23);
        addToolbarItem(centralArea);
        HBox.setHgrow(centralArea, Priority.ALWAYS);

        // Set Search Area
        searchCounter = new Text();
        searchCounter.getStyleClass().add("search-counter");
        searchCounter.setFill(Color.WHITE);

        searchBox = new SearchBox(logTab);

        HBox hBox = new HBox(searchBox, searchCounter);
        hBox.setSpacing(9);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setMinWidth(470);
        hBox.prefWidth(470);

        registerListeners(clearButton, stopButton, searchBox);
        addToolbarItem(hBox);
    }

    public SearchBox getSearchBox() {
        return searchBox;
    }

    public void printCounter(String text) {
        searchCounter.setText(text);
    }
}
