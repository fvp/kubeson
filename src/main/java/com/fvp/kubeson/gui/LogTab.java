package com.fvp.kubeson.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fvp.kubeson.Configuration;
import com.fvp.kubeson.core.Kubernetes;
import com.fvp.kubeson.core.KubernetesListener;
import com.fvp.kubeson.core.TreeList;
import com.fvp.kubeson.model.ItemType;
import com.fvp.kubeson.model.LogCategory;
import com.fvp.kubeson.model.LogLevel;
import com.fvp.kubeson.model.LogLine;
import com.fvp.kubeson.model.LogLineContainer;
import com.fvp.kubeson.model.LogSource;
import com.fvp.kubeson.model.Pod;
import com.fvp.kubeson.model.PodLogFeedListener;
import com.fvp.kubeson.model.SelectedItem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogTab extends Tab {

    private static Logger LOGGER = LogManager.getLogger();

    private Text tabName;

    private int logLineId;

    private SplitPane tabSplitPane;

    private LogListView logListView;

    private JsonViewerPane jsonViewerPane;

    private List<SelectedItem> selectedItems;

    private ObservableList<LogLineContainer> logLines;

    private FilteredList<LogLineContainer> filteredLogLines;

    private Map<LogLevel, Boolean> logLevelStates;

    private Map<LogCategory, Boolean> logCategoryStates;

    private PodLogFeedListener podLogFeedListener;

    private KubernetesListener kubernetesListener;

    private SearchManager searchManager;

    private int running;

    private long lastLogLineTime;

    private int logIdColorIdx;

    private int logSourceColorIdx;

    LogTab(List<SelectedItem> selectedItems, String name, boolean showLogsFromStart) {
        this.selectedItems = selectedItems;
        this.tabName = new Text(name);
        this.tabName.setFill(Color.WHITE);
        super.setGraphic(tabName);
        setRunning(true);

        if (selectedItems.size() > 1) {
            StringBuilder sb = new StringBuilder();
            selectedItems.forEach(item -> sb.append(item.getText()).append('\n'));
            super.setTooltip(new Tooltip(sb.toString()));
        }

        this.podLogFeedListener = new PodLogFeedListener() {

            @Override
            public void onNewLogLine(LogSource logSource, LogLine logLine) {
                Platform.runLater(() -> addItem(logSource, logLine));
            }

            @Override
            public void onLogLineRemoved(LogLine logLine) {
                Platform.runLater(() -> {
                    ////LOGGER.trace(">>> REMOVED");
                    logLines.remove(new LogLineContainer(logLine));
                });
            }

            @Override
            public void onPodLogFeedTerminated(Pod pod) {
                for (SelectedItem selectedItem : selectedItems) {
                    if (selectedItem.isRunning() && !selectedItem.getPod().equals(pod)) {
                        return;
                    }
                }
                setRunning(false);
                Platform.runLater(() -> printPodTerminatedMessage());
            }
        };

        this.kubernetesListener = new KubernetesListener() {

            @Override
            public void onPodTerminated(Pod pod) {
            }

            @Override
            public void onNewPod(Pod newPod) {
                if (running >= 0) {
                    for (SelectedItem selectedItem : selectedItems) {
                        if (selectedItem.getType() == ItemType.LABEL && selectedItem.getText().equals(newPod.getAppLabel())) {
                            LOGGER.debug("New {}. Stopping previous log stream and starting stream for new pod", newPod);
                            selectedItem.getPod().removeListener(podLogFeedListener, false);

                            if (selectedItems.size() == 1) {
                                reset();
                            }

                            newPod.addListener(null, getLogSource(selectedItem.getText(), selectedItems.size()), podLogFeedListener, true);
                            selectedItem.setPod(newPod);
                            setRunning(true);
                            return;
                        }
                    }
                }
            }
        };
        Kubernetes.addListener(kubernetesListener);
        //this.logLines = FXCollections.synchronizedObservableList(FXCollections.observableList(new TreeList<>()));
        this.logLines = FXCollections.observableList(new TreeList<>());
        this.filteredLogLines = new FilteredList<>(logLines, s -> true);

        this.searchManager = new SearchManager(this);
        this.logListView = new LogListView(this);
        super.setOnClosed((event) -> {
            logLines.clear();
            logListView.dispose();
            selectedItems.forEach(selectedItem -> selectedItem.getPod().removeListener(this.podLogFeedListener, false));
            Kubernetes.removeListener(kubernetesListener);
            LogTabPane.broadcastOnTabClosed(this);
        });
        this.jsonViewerPane = new JsonViewerPane(this);
        this.tabSplitPane = new SplitPane(this.logListView.draw());
        this.tabSplitPane.setStyle("-fx-background-color: black;-fx-control-inner-background: black;");

        super.setContent(this.tabSplitPane);
        initFiltersDefaultState();

        // Start printing log lines
        selectedItems.forEach(selectedItem -> {
            selectedItem.getPod()
                .addListener(selectedItem.getContainer(), getLogSource(selectedItem.getText(), selectedItems.size()), this.podLogFeedListener,
                    showLogsFromStart);
        });
    }

    private LogSource getLogSource(String name, int size) {
        if (size <= 1) {
            return null;
        }
        final Color color = Configuration.LOG_SOURCE_COLORS[logSourceColorIdx];
        logSourceColorIdx++;
        logSourceColorIdx = logSourceColorIdx % Configuration.LOG_SOURCE_COLORS.length;

        return new LogSource(name, color);
    }

    public void stop() {
        stop(false);
    }

    public void stop(boolean keepLogSource) {
        if (running == 1) {
            selectedItems.forEach(selectedItem -> selectedItem.getPod().removeListener(podLogFeedListener, keepLogSource));
            Platform.runLater(this::printLogFeedStoppedMessage);
            setRunning(false);
        }
    }

    public void reset() {
        Platform.runLater(() -> {
            logLines.clear();
            logListView.reset();
        });
        logLineId = 0;
        lastLogLineTime = 0;
        logIdColorIdx = 0;
    }

    private int logLineId() {
        return ++logLineId;
    }

    private void addItem(LogSource logSource, LogLine logLine) {
        final long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogLineTime > 5000) {
            logIdColorIdx++;
            logIdColorIdx = logIdColorIdx % 2;
        }
        logLines.add(new LogLineContainer(logSource, logLine, logLineId(), Configuration.LOG_ID_COLORS[logIdColorIdx]));
        lastLogLineTime = currentTime;
        if (!jsonViewerPane.isDrawn() && logLine.isJson()) {
            tabSplitPane.setDividerPosition(0, Configuration.LOG_LIST_PANEL_SPLIT);
            tabSplitPane.getItems().add(jsonViewerPane.draw());
        }
    }

    private void printPodTerminatedMessage() {
        msgLogLine("");
        msgLogLine("*************************************************");
        msgLogLine("                   KUBERNETES POD TERMINATED");
        msgLogLine("*************************************************");
        msgLogLine("");
    }

    private void printLogFeedStoppedMessage() {
        msgLogLine("");
        msgLogLine("*************************************************");
        msgLogLine("                           LOG FEED STOPPED");
        msgLogLine("*************************************************");
        msgLogLine("");
    }

    private void msgLogLine(String text) {
        logLines.add(new LogLineContainer(null, new LogLine(text, true), 0, null));
    }

    private void initFiltersDefaultState() {
        logLevelStates = new HashMap<>();
        for (LogLevel logLevel : LogLevel.values()) {
            logLevelStates.put(logLevel, true);
        }
        logCategoryStates = new HashMap<>();
        for (LogCategory logCategory : LogCategory.values()) {
            logCategoryStates.put(logCategory, true);
        }
    }

/*
    private void printNewPodStartingMessage(String podName) {
        Color color = Color.GREEN;
        String msg = "STARTING POD: " + podName;
        String filler = StringUtils.repeat('*', msg.length() + 22);
        logLines.add(new LogLine(logLineId(), filler, color));
        logLines.add(new LogLine(logLineId(), "          " + msg, color));
        logLines.add(new LogLine(logLineId(), filler, color));
        logLines.add(new LogLine(logLineId(), "", color));
    }
*/

    public boolean getFilterState(Object enumField) {
        if (enumField instanceof LogLevel) {
            return logLevelStates.get(enumField);
        }
        if (enumField instanceof LogCategory) {
            return logCategoryStates.get(enumField);
        }
        return false;
    }

    public void filter(Object enumField, boolean state) {
        if (enumField instanceof LogLevel) {
            logLevelStates.put((LogLevel) enumField, state);
        }
        if (enumField instanceof LogCategory) {
            logCategoryStates.put((LogCategory) enumField, state);
        }
        filter();
    }

    private void filter() {
        jsonViewerPane.clear();
        filteredLogLines.setPredicate(logLineContainer -> {
            if (logLineContainer.getLogLine().getLogLevel() != null && logLevelStates != null && !logLevelStates.get(
                logLineContainer.getLogLine().getLogLevel())) {
                return false;
            }
            if (logLineContainer.getLogLine().getLogCategory() != null && logCategoryStates != null && !logCategoryStates.get(
                logLineContainer.getLogLine().getLogCategory())) {
                return false;
            }
            return true;
        });
        SearchBoxController.startSearch();
    }

    public void stopAndContinueInNewTab() {
        stop(true);
        running = -1;
        TabPane tabPane = super.getTabPane();
        int pos = tabPane.getTabs().indexOf(this) + 1;

        LogTab logTab = new LogTab(selectedItems, getTabName(), false);
        tabPane.getTabs().add(pos, logTab);
        tabPane.getSelectionModel().select(pos);
    }

    public FilteredList<LogLineContainer> getLogLines() {
        return filteredLogLines;
    }

    public void clearAll() {
        searchManager.clear();
        logListView.refresh();
    }

    public LogListView getLogListView() {
        return logListView;
    }

    public JsonViewerPane getJsonViewerPane() {
        return jsonViewerPane;
    }

    public SearchManager getSearchManager() {
        return searchManager;
    }

    public String getTabName() {
        return tabName.getText();
    }

    public boolean isRunning() {
        return running == 1;
    }

    private void setRunning(boolean running) {
        if (running) {
            this.running = 1;
            tabName.setFill(Color.WHITE);
        } else {
            this.running = 0;
            tabName.setFill(Color.web("#e60000"));
        }
    }

}
