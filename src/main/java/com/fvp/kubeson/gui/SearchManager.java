package com.fvp.kubeson.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fvp.kubeson.model.LogLineContainer;
import com.fvp.kubeson.model.SearchItem;
import javafx.collections.ListChangeListener;
import org.apache.commons.lang3.StringUtils;

public class SearchManager {

    private LogTab logTab;

    private int searchResultIdx;

    private int searchResultTotal;

    private Pattern searchTextPattern;

    private volatile boolean hasSearch;

    public SearchManager(LogTab logTab) {
        this.logTab = logTab;

        // First Order Listener
        logTab.getLogLines().addListener((ListChangeListener<? super LogLineContainer>) (changedLogLines) -> {
            if (hasSearch) {
                while (changedLogLines.next()) {
                    if (changedLogLines.wasAdded()) {
                        for (LogLineContainer logLineContainer : changedLogLines.getAddedSubList()) {
                            setLineSearchItems(logLineContainer);
                            if (logLineContainer.getSearchItems() != null) {
                                searchResultTotal++;
                                printCounter();
                            }
                        }
                    } else if (changedLogLines.wasRemoved()) {
                        for (LogLineContainer logLineContainer : changedLogLines.getRemoved()) {
                            if (logLineContainer.hasFoundSearch()) {
                                if (searchResultTotal > 0) {
                                    searchResultTotal--;
                                }
                                if (searchResultIdx > 0) {
                                    searchResultIdx--;
                                }
                                printCounter();
                            }
                        }
                    }
                }
            }
        });
    }

    public void setLineSearchItems(LogLineContainer logLineContainer) {
        List<SearchItem> searchItems = null;
        for (int j = 0; j < logLineContainer.getLogLine().getLogText().length; j++) {
            final Matcher m = getSearchTextPattern().matcher(logLineContainer.getLogLine().getLogText()[j]);
            while (m.find()) {
                if (searchItems == null) {
                    searchItems = new ArrayList<>();
                }
                if (j % 2 == 0) { //Even
                    searchItems.add(new SearchItem(j, m.start(), m.end()));
                } else { //Odd
                    searchItems.add(new SearchItem(j, -1, -1));
                    break;
                }
            }
        }
        logLineContainer.setSearchItems(searchItems);
    }

    public void setSearchResultIdx(int searchResultIdx) {
        this.searchResultIdx = searchResultIdx;
    }

    public String getSearchText() {
        if (hasSearch && searchTextPattern != null) {
            return searchTextPattern.pattern();
        }
        return "";
    }

    public void setSearchText(String searchText) {
        if (StringUtils.isEmpty(searchText)) {
            hasSearch = false;
        } else {
            hasSearch = true;
            searchTextPattern = Pattern.compile(searchText, Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        }
    }

    public int getSearchResultTotal() {
        return searchResultTotal;
    }

    public boolean hasSearch() {
        return hasSearch;
    }

    public Pattern getSearchTextPattern() {
        return searchTextPattern;
    }

    public void incrementSearchResultTotal(int value) {
        searchResultTotal += value;
    }

    public void clear() {
        logTab.getJsonViewerPane().updateSearch();
    }

    public void reset() {
        searchResultTotal = 0;
        searchResultIdx = 0;
    }

    public void moveFirst() {
        for (int i = 0; i < logTab.getLogLines().size(); i++) {
            LogLineContainer logLineContainer = logTab.getLogLines().get(i);
            if (logLineContainer.hasFoundSearch()) {
                selectLogLine(i);
                return;
            }
        }
    }

    public void moveLast() {
        for (int i = logTab.getLogLines().size() - 1; i >= 0; i--) {
            LogLineContainer logLineContainer = logTab.getLogLines().get(i);
            if (logLineContainer.hasFoundSearch()) {
                selectLogLine(i);
                return;
            }
        }
    }

    public void moveUp() {
        for (int i = logTab.getLogListView().getSelectedIndex() - 1; i >= 0; i--) {
            LogLineContainer logLineContainer = logTab.getLogLines().get(i);
            if (logLineContainer.hasFoundSearch()) {
                selectLogLine(i);
                return;
            }
        }
    }

    public void moveDown() {
        for (int i = logTab.getLogListView().getSelectedIndex() + 1; i < logTab.getLogLines().size(); i++) {
            LogLineContainer logLineContainer = logTab.getLogLines().get(i);
            if (logLineContainer.hasFoundSearch()) {
                selectLogLine(i);
                return;
            }
        }
    }

    private void selectLogLine(int i) {
        logTab.getLogListView().selectAtOffset(i);
        updateSearchResultIdx(i);
        printCounter();
    }

    public void updateSearchResultIdx(int line) {
        int counter = 0;
        for (int i = 0; i <= line; i++) {
            if (logTab.getLogLines().get(i).hasFoundSearch()) {
                counter++;
            }
        }
        searchResultIdx = counter;
    }

    public void printCounter() {
        if (hasSearch) {
            SearchBoxController.printCounter(logTab, searchResultIdx + "/" + searchResultTotal);
        } else {
            SearchBoxController.printCounter(logTab, "");
        }
    }
}
