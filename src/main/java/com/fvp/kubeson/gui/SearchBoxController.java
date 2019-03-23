package com.fvp.kubeson.gui;

import java.util.concurrent.Semaphore;

import com.fvp.kubeson.core.ThreadFactory;
import com.fvp.kubeson.model.LogLineContainer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SearchBoxController {

    private static Logger LOGGER = LogManager.getLogger();

    private static LogTab currentTab;

    private static SearchBox searchBox;

    private static Text searchCounter;

    private static Semaphore semaphore;

    static {
        searchBox = new SearchBox();
        searchCounter = new Text();
        searchCounter.getStyleClass().add("search-counter");
        searchCounter.setFill(Color.WHITE);

        semaphore = new Semaphore(0);
        init();
    }

    private SearchBoxController() {
    }

    private static void init() {
        searchBox.addListener(new SearchBoxListener() {

            @Override
            public void onUpButton() {
                if (currentTab != null) {
                    currentTab.getSearchManager().moveUp();
                }
            }

            @Override
            public void onDownButton() {
                if (currentTab != null) {
                    currentTab.getSearchManager().moveDown();
                }
            }

            @Override
            public void onLeftButton() {
                if (currentTab != null) {
                    currentTab.getSearchManager().moveFirst();
                }
            }

            @Override
            public void onRightButton() {
                if (currentTab != null) {
                    currentTab.getSearchManager().moveLast();
                }
            }

            @Override
            public void onClearButton() {
                if (currentTab != null) {
                    currentTab.clearAll();
                    currentTab.getLogListView().requestFocus();
                }
            }

            @Override
            public void onChange(String text) {
                startSearch();
            }
        });

        startSearchWorker();
    }

    public static Parent draw() {
        HBox hBox = new HBox(searchBox, searchCounter);
        hBox.setSpacing(9);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setMinWidth(470);
        hBox.prefWidth(470);
        return hBox;
    }

    public static void requestFocus() {
        searchBox.requestFocus();
    }

    public static void setSearchText(String searchText) {
        if (!StringUtils.isEmpty(searchText)) {
            searchBox.setText(searchText);
            searchBox.requestFocus();
        }
    }

    public static void changeTab(LogTab newLogTab) {
        currentTab = null;
        searchBox.setText(newLogTab.getSearchManager().getSearchText());
        currentTab = newLogTab;
        currentTab.getSearchManager().printCounter();
    }

    public static void destroyTab(LogTab logTab) {
        if (logTab.equals(currentTab)) {
            searchBox.setText("");
        }
    }

    public static void startSearch() {
        if (currentTab != null) {
            SearchManager searchManager = currentTab.getSearchManager();
            searchManager.setSearchText(searchBox.getText());

            if (!StringUtils.isEmpty(searchBox.getText())) {
                if (semaphore.availablePermits() == 0) {
                    semaphore.release();
                }
            } else {
                currentTab.clearAll();
            }
        }
    }

    public static void printCounter(LogTab logTab, String text) {
        if (logTab.equals(currentTab)) {
            searchCounter.setText(text);
        }
    }

    private static void startSearchWorker() {
        ThreadFactory.newThread(() -> {
            try {
                for (; ; ) {
                    semaphore.acquire();

                    if (currentTab != null) {
                        SearchManager searchManager = currentTab.getSearchManager();

                        if (searchManager.hasSearch()) {
                            searchManager.reset();
                            int i = 0;
                            int line = -1;
                            boolean found = false;
                            int selectedIdx = currentTab.getLogListView().getSelectedIndex();
                            for (final LogLineContainer logLineContainer : currentTab.getLogLines()) {
                                if (semaphore.availablePermits() > 0) {
                                    break;
                                }
                                searchManager.setLineSearchItems(logLineContainer);
                                if (logLineContainer.getSearchItems() != null) {
                                    searchManager.incrementSearchResultTotal(1);
                                    line = i;
                                    if (!found && i >= selectedIdx) {
                                        found = true;
                                        selectLine(searchManager, i);
                                    }
                                }
                                i++;
                            }

                            if (!found && line != -1) {
                                selectLine(searchManager, line);
                            }

                            if (searchManager.getSearchResultTotal() == 0) { //Empty Search
                                Platform.runLater(currentTab::clearAll);
                            }
                            Platform.runLater(searchManager::printCounter);
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.info("Search Thread Interrupted");
            }
            LOGGER.info("Search Thread Completed");
        });
    }

    private static void selectLine(SearchManager searchManager, final int line) {
        searchManager.setSearchResultIdx(searchManager.getSearchResultTotal());
        Platform.runLater(() -> currentTab.getLogListView().selectAtOffset(line));
    }
}
