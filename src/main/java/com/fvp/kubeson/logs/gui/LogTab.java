package com.fvp.kubeson.logs.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fvp.kubeson.Configuration;
import com.fvp.kubeson.common.controller.K8SClient;
import com.fvp.kubeson.common.controller.K8SClientListener;
import com.fvp.kubeson.common.controller.K8SResourceChange;
import com.fvp.kubeson.common.gui.TabBase;
import com.fvp.kubeson.common.gui.TabLabel;
import com.fvp.kubeson.common.model.ItemType;
import com.fvp.kubeson.common.model.K8SConfigMap;
import com.fvp.kubeson.common.model.K8SPod;
import com.fvp.kubeson.common.model.PodLogFeedListener;
import com.fvp.kubeson.common.model.SelectedItem;
import com.fvp.kubeson.common.util.TreeList;
import com.fvp.kubeson.logs.model.LogCategory;
import com.fvp.kubeson.logs.model.LogLevel;
import com.fvp.kubeson.logs.model.LogLine;
import com.fvp.kubeson.logs.model.LogLineContainer;
import com.fvp.kubeson.logs.model.LogSource;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogTab extends TabBase<LogToolbar> {

    private static Logger LOGGER = LogManager.getLogger();

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

    private K8SClientListener k8sListener;

    private SearchManager searchManager;

    private int running;

    private long lastLogLineTime;

    private int logIdColorIdx;

    private int logSourceColorIdx;

    public LogTab(File logFile, TabLabel tabLabel) {
        super(tabLabel);
        LOGGER.debug("Creating tab for file [" + logFile + "]");

        super.setTooltip(new Tooltip(logFile.toString()));
        init();

        super.setOnClosed((event) -> {
            logLines.clear();
            logListView.dispose();
        });

        //Read file content
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line = reader.readLine();
            while (line != null) {
                LogLine logLine = new LogLine(line);
                logLines.add(new LogLineContainer(null, logLine, logLineId(), Configuration.LOG_ID_COLORS[0]));
                checkEnableJsonViewer(logLine);
                line = reader.readLine();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read contents of file [" + logFile + "]", e);
        }
    }

    public LogTab(List<SelectedItem> selectedItems, TabLabel tabLabel, boolean showLogsFromStart) {
        super(tabLabel);
        this.selectedItems = selectedItems;
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
                    logLines.remove(new LogLineContainer(logLine));
                });
            }

            @Override
            public void onPodLogFeedTerminated(K8SPod pod) {
                for (SelectedItem selectedItem : selectedItems) {
                    if (selectedItem.isRunning() && !selectedItem.getPod().equals(pod)) {
                        return;
                    }
                }
                setRunning(false);
                Platform.runLater(() -> printPodTerminatedMessage());
            }
        };

        this.k8sListener = new K8SClientListener() {

            @Override
            public void onPodChange(K8SResourceChange<K8SPod> changes) {
                if (running >= 0) {
                    for (K8SPod newPod : changes.getAdded()) {
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
            }

            @Override
            public void onConfigMapChange(K8SResourceChange<K8SConfigMap> changes) {

            }
        };
        K8SClient.addListener(k8sListener);
        init();

        super.setOnClosed((event) -> {
            logLines.clear();
            logListView.dispose();
            selectedItems.forEach(selectedItem -> selectedItem.getPod().removeListener(this.podLogFeedListener, false));
            K8SClient.removeListener(k8sListener);
        });

        // Start printing log lines
        selectedItems.forEach(selectedItem -> {
            selectedItem.getPod()
                    .addListener(selectedItem.getContainer(), getLogSource(selectedItem.getText(), selectedItems.size()), this.podLogFeedListener,
                            showLogsFromStart);
        });
    }

    private void init() {
        super.setToolbar(new LogToolbar(this));
        this.logLines = FXCollections.synchronizedObservableList(FXCollections.observableList(new TreeList<>()));
        //this.logLines = FXCollections.observableList(new TreeList<>());
        this.filteredLogLines = new FilteredList<>(logLines, s -> true);
        this.searchManager = new SearchManager(this);
        this.logListView = new LogListView(this);
        this.jsonViewerPane = new JsonViewerPane(this);
        this.tabSplitPane = new SplitPane(this.logListView.draw());
        this.tabSplitPane.setStyle("-fx-background-color: black;-fx-control-inner-background: black;");

        super.setContent(this.tabSplitPane);
        initFiltersDefaultState();
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
        checkEnableJsonViewer(logLine);
    }

    private void checkEnableJsonViewer(LogLine logLine) {
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
        searchManager.refresh();
    }

    public void stopAndContinueInNewTab() {
        stop(true);
        running = -1;
        TabPane tabPane = super.getTabPane();
        int pos = tabPane.getTabs().indexOf(this) + 1;

        LogTab logTab = new LogTab(selectedItems, getTabLabel(), false);
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

    public boolean isRunning() {
        return running == 1;
    }

    private void setRunning(boolean running) {
        if (running) {
            this.running = 1;
        } else {
            this.running = 0;
        }
        getTabLabel().setErrorColor(!running);
    }

    @Override
    public void onSelected() {
        super.onSelected();
        logListView.requestFocus();
    }
}
