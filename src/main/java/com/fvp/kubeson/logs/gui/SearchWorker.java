package com.fvp.kubeson.logs.gui;

import java.util.concurrent.Semaphore;

import com.fvp.kubeson.common.util.ThreadFactory;
import com.fvp.kubeson.logs.model.LogLineContainer;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SearchWorker {

    private static Logger LOGGER = LogManager.getLogger();

    private static Semaphore semaphore;

    private static LogTab logTab;

    static {
        semaphore = new Semaphore(0);
        startSearchWorker();
    }

    private SearchWorker() {
    }

    public static void startSearch(LogTab logTab) {
        SearchWorker.logTab = logTab;

        if (semaphore.availablePermits() == 0) {
            semaphore.release();
        }
    }

    private static void startSearchWorker() {
        ThreadFactory.newThread(() -> {
            try {
                for (; ; ) {
                    semaphore.acquire();

                    SearchManager searchManager = logTab.getSearchManager();

                    if (searchManager.hasSearch()) {
                        searchManager.reset();
                        int i = 0;
                        int line = -1;
                        boolean found = false;
                        int selectedIdx = logTab.getLogListView().getSelectedIndex();
                        for (final LogLineContainer logLineContainer : logTab.getLogLines()) {
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
                            Platform.runLater(logTab::clearAll);
                        }
                    } else { //Empty search, clear up previous search
                        for (final LogLineContainer logLineContainer : logTab.getLogLines()) {
                            logLineContainer.setSearchItems(null);
                        }
                        Platform.runLater(logTab::clearAll);
                    }
                    Platform.runLater(searchManager::printCounter);
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
        Platform.runLater(() -> logTab.getLogListView().selectAtOffset(line));
    }
}
