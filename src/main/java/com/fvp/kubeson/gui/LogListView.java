package com.fvp.kubeson.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.fvp.kubeson.Configuration;
import com.fvp.kubeson.Main;
import com.fvp.kubeson.model.LogLineContainer;
import com.fvp.kubeson.model.SearchItem;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualFlowHit;
import org.fxmisc.flowless.VirtualizedScrollPane;

public class LogListView {

    private static final Background YELLOW_BACKGROUND = new Background(new BackgroundFill(Color.YELLOW, null, null));

    private static Logger LOGGER = LogManager.getLogger();

    private LogTab logTab;

    private VirtualFlow<LogLineContainer, Cell<LogLineContainer, LogListCell>> listView;

    private VirtualizedScrollPane<VirtualFlow> listScrollPane;

    private LogListCell selectedLogLine;

    private Export export;

    private ScrollBar vbar;

    private volatile Timer maxScrollLockTimer;

    private volatile boolean maxScrollLocked;

    private boolean firstScroll;

    public LogListView(LogTab logTab) {
        this.logTab = logTab;
        this.export = new Export();
        // Second Order Listener
        // If deleted cell is selected, clear the selection
        logTab.getLogLines().addListener((ListChangeListener<? super LogLineContainer>) (changedLogLines) -> {
            while (changedLogLines.next()) {
                if (changedLogLines.wasRemoved()) {
                    if (selectedLogLine != null) {
                        for (LogLineContainer logLineContainer : changedLogLines.getRemoved()) {
                            if (selectedLogLine != null && logLineContainer.equals(selectedLogLine.getLogLineContainer())) {
                                clearSelection();
                            }
                        }
                    }
                } else if (changedLogLines.wasAdded()) {
                    setMaxScrollLockTimer();
                }
            }
        });

        listView = VirtualFlow.createVertical(logTab.getLogLines(), (logLineContainer) -> Cell.wrapNode(new LogListCell(logLineContainer)));
        listView.getStyleClass().add("log-list-view");

        listView.setOnMouseClicked((mouseEvent) -> {
            VirtualFlowHit<Cell<LogLineContainer, LogListCell>> hit = listView.hit(mouseEvent.getX(), mouseEvent.getY());

            if (hit.isCellHit()) {
                listView.requestFocus();
                if (!hit.getCell().getNode().equals(selectedLogLine)) {
                    selectedLogLine = hit.getCell().getNode();
                    refresh();

                    ////LOGGER.debug("Hitted: {}", selectedLogLine);
                    logTab.getJsonViewerPane().updateLogLine();
                    updateSearchCounter();
                }
            }
        });

        listScrollPane = new VirtualizedScrollPane<>(listView);

        vbar = (ScrollBar) listScrollPane.getChildrenUnmodifiable().get(2);
        vbar.setOnMouseClicked(e -> maxScrollLocked = false);
        vbar.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (maxScrollLocked) {
                vbar.setValue(Double.MAX_VALUE);
            }
        });

        // Keyboard Shortcuts
        KeyCombination ctrlC = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);

        listView.setOnKeyReleased(keyEvent -> {
            if (ctrlC.match(keyEvent)) {
                export.copySelectedLogLineToClipboard();
            } else if (keyEvent.getCode() == KeyCode.UP) {
                int idx = getSelectedIndex();
                if (idx > 0) {
                    select(--idx);
                }
            } else if (keyEvent.getCode() == KeyCode.DOWN) {
                int idx = getSelectedIndex();
                if (idx < logTab.getLogLines().size() - 1) {
                    select(++idx);
                }
            } else if (keyEvent.getCode() == KeyCode.T) {
                //LOGGER.error("Log Line Counter: "+logTab.tmpCounter);
            }
        });

        createContextMenu();
    }

    private boolean isLastItem() {
        if (!firstScroll && vbar.getMax() > vbar.getVisibleAmount()) {
            firstScroll = true;
            return true;
        }
        return vbar.getMax() > 0 && Math.ceil(vbar.getValue()) >= vbar.getMax();
    }

    private void setMaxScrollLockTimer() {
        if (maxScrollLockTimer == null && isLastItem()) {
            maxScrollLocked = true;
            maxScrollLockTimer = new Timer("MaxScrollLockTimer", true);
            maxScrollLockTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    if (maxScrollLocked) {
                        Platform.runLater(() -> {
                            listView.show(logTab.getLogLines().size() - 1);
                            listView.visibleCells();
                            maxScrollLocked = false;
                            maxScrollLockTimer = null;
                        });
                    } else {
                        maxScrollLockTimer = null;
                    }
                }
            }, 200);
        }
    }

    private void createContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        listView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                contextMenu.getItems().clear();
                if (getSelectedLogLine() != null) {
                    MenuItem copy = new MenuItem("Copy");
                    copy.setOnAction(e1 -> export.copySelectedLogLineToClipboard());
                    contextMenu.getItems().add(copy);
                }
                if (logTab.getSearchManager().hasSearch()) {
                    addSeparator(contextMenu);
                    if (logTab.getSearchManager().getSearchResultTotal() <= Configuration.CLIPBOARD_COPY_MAX_LOG_LINES) {
                        MenuItem copySearched = new MenuItem("Copy Searched Log Lines");
                        copySearched.setOnAction(e2 -> export.copySearchedLogLinesToClipboard());
                        contextMenu.getItems().add(copySearched);
                    }
                    MenuItem exportSearched = new MenuItem("Save Searched Log Lines");
                    exportSearched.setOnAction(e2 -> export.saveSearchedLogLinesToFile());
                    contextMenu.getItems().add(exportSearched);
                }
                if (!logTab.getLogLines().isEmpty()) {
                    addSeparator(contextMenu);
                    if (logTab.getLogLines().size() <= Configuration.CLIPBOARD_COPY_MAX_LOG_LINES) {
                        MenuItem copyAll = new MenuItem("Copy All Log Lines");
                        copyAll.setOnAction(e2 -> export.copyAllLogLinesToClipboard());
                        contextMenu.getItems().add(copyAll);
                    }
                    MenuItem exportAll = new MenuItem("Save All Log Lines");
                    exportAll.setOnAction(e2 -> export.saveAllLogLinesToFile());
                    contextMenu.getItems().add(exportAll);
                }
                if (logTab.isRunning()) {
                    addSeparator(contextMenu);
                    MenuItem stopAndCreateNewTab = new MenuItem("Stop And Continue On New Tab");
                    stopAndCreateNewTab.setOnAction(e2 -> logTab.stopAndContinueInNewTab());
                    contextMenu.getItems().add(stopAndCreateNewTab);
                }
                if (!contextMenu.getItems().isEmpty()) {
                    contextMenu.show(listView, e.getScreenX(), e.getScreenY());
                }
            } else {
                contextMenu.hide();
            }
        });
    }

    private void addSeparator(ContextMenu contextMenu) {
        if (contextMenu.getItems().size() > 0) {
            contextMenu.getItems().add(new SeparatorMenuItem());
        }
    }

    public void requestFocus() {
        listView.requestFocus();
    }

    public Node draw() {
        return listScrollPane;
    }

    public void selectAtOffset(int currentLineIdx) {
        listView.showAtOffset(currentLineIdx, 200);
        selectImpl(currentLineIdx);
    }

    public void select(int currentLineIdx) {
        listView.show(currentLineIdx);
        selectImpl(currentLineIdx);
    }

    private void selectImpl(int currentLineIdx) {
        ObservableList<Cell<LogLineContainer, LogListCell>> visibleCells = listView.visibleCells();
        selectedLogLine = listView.getCell(currentLineIdx).getNode();

        refresh(visibleCells);
        logTab.getJsonViewerPane().updateLogLine();
    }

    public void clearSelection() {
        selectedLogLine = null;
        refresh();
        logTab.getJsonViewerPane().clear();
    }

    public LogLineContainer getSelectedLogLine() {
        if (selectedLogLine != null) {
            return selectedLogLine.getLogLineContainer();
        }
        return null;
    }

    public int getSelectedIndex() {
        if (selectedLogLine != null && selectedLogLine.getLogLineContainer() != null) {
            return logTab.getLogLines().indexOf(selectedLogLine.getLogLineContainer());
        }
        return -1;
    }

    public void reset() {
        firstScroll = false;
    }

    public void refresh() {
        refresh(listView.visibleCells());
    }

    private void refresh(ObservableList<Cell<LogLineContainer, LogListCell>> visibleCells) {
        for (Cell<LogLineContainer, LogListCell> cell : visibleCells) {
            if (cell.getNode().equals(selectedLogLine)) {
                cell.getNode().select();
            } else {
                cell.getNode().clearSelection();
            }
        }
    }

    public void dispose() {
        listView.dispose();
    }

    private void updateSearchCounter() {
        if (logTab.getSearchManager().hasSearch()) {
            logTab.getSearchManager().updateSearchResultIdx(getSelectedIndex());
            logTab.getSearchManager().printCounter();
        }
    }

    private class LogListCell extends StackPane {

        private LogLineContainer logLineContainer;

        private List<SearchItem> searchItems;

        private TextFlow tf;

        public LogListCell(LogLineContainer logLineContainer) {
            super();
            this.logLineContainer = logLineContainer;
            this.searchItems = new ArrayList<>();
            tf = new TextFlow();
            tf.getStyleClass().add("log-list-view-cell");
            tf.setPrefWidth(Region.USE_PREF_SIZE);

            if (selectedLogLine != null && selectedLogLine.getLogLineContainer().equals(logLineContainer)) {
                tf.setStyle("-fx-background-color: #311B92;");
            }
            createNodes();
            super.getChildren().add(tf);
        }

        public void select() {
            tf.setStyle("-fx-background-color: #311B92;");
            updateNodes();
        }

        public void clearSelection() {
            tf.setStyle("-fx-background-color: transparent;");
            updateNodes();
        }

        private void updateNodes() {
            if (!searchItems.equals(logLineContainer.getSearchItems())) {
                createNodes();
            }
        }

        private void createNodes() {
            List<Node> nodes = new ArrayList<>();

            if (!logLineContainer.getLogLine().isSystem()) {
                Text number = new Text(logLineContainer.getId() + " ");
                number.setFill(logLineContainer.getIdColor());
                nodes.add(number);

                if (logLineContainer.getLogSource() != null) {
                    Text logSource = new Text("[" + logLineContainer.getLogSource().getName() + "] ");
                    logSource.setFill(logLineContainer.getLogSource().getColor());
                    nodes.add(logSource);
                }
            }

            if (logLineContainer.hasFoundSearch()) {
                searchItems = logLineContainer.getSearchItems();
                final String[] logText = logLineContainer.getLogLine().getTextArray();
                int j = 0;
                loop:
                for (int i = 0; i < logText.length; i++) {
                    int start = 0;
                    while (j < searchItems.size()) {
                        final SearchItem searchItem = searchItems.get(j);
                        if (searchItem.getIndex() != i) {
                            break;
                        }
                        if (searchItem.getStart() == -1) {
                            nodes.add(getHighlightedTextNode(logText[i]));
                            continue loop;
                        } else {
                            if (start != searchItem.getStart()) {
                                nodes.add(getTextNode(logText[i].substring(start, searchItem.getStart()), logLineContainer.getLogLine().getColor()));
                            }
                            nodes.add(getHighlightedTextNode(logText[i].substring(searchItem.getStart(), searchItem.getEnd())));
                            start = searchItem.getEnd();
                        }
                        j++;
                    }
                    nodes.add(getTextNode(logText[i].substring(start), logLineContainer.getLogLine().getColor()));
                }
            } else {
                Text txt = new Text(logLineContainer.getLogLine().getText());
                txt.setFill(logLineContainer.getLogLine().getColor());
                nodes.add(txt);
            }

            if (tf.getChildren().size() != 0) {
                tf.getChildren().clear();
            }
            tf.getChildren().addAll(nodes);
        }

        private Node getHighlightedTextNode(String text) {
            Text txt = new Text(text);
            txt.setFill(Color.BLACK);
            TextFlow tf = new TextFlow(txt);
            tf.setBackground(YELLOW_BACKGROUND);

            return tf;
        }

        private Node getTextNode(String text, Color color) {
            Text txt = new Text(text);
            txt.setFill(color);
            return txt;
        }

        public LogLineContainer getLogLineContainer() {
            return logLineContainer;
        }
    }

    private class Export {

        private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());

        private void copySelectedLogLineToClipboard() {
            LogLineContainer logLineContainer = getSelectedLogLine();
            if (logLineContainer != null) {
                ClipboardContent clipboard = new ClipboardContent();
                clipboard.putString(logLineContainer.getLogLine().getRawText());
                Clipboard.getSystemClipboard().setContent(clipboard);
            }
        }

        private void copySearchedLogLinesToClipboard() {
            StringBuilder sb = new StringBuilder();
            for (LogLineContainer logLineContainer : logTab.getLogLines()) {
                if (logLineContainer.hasFoundSearch() && !logLineContainer.getLogLine().isSystem()) {
                    sb.append(logLineContainer.getLogLine().getRawText()).append('\n');
                }
            }
            ClipboardContent clipboard = new ClipboardContent();
            clipboard.putString(sb.toString());
            Clipboard.getSystemClipboard().setContent(clipboard);
        }

        private void copyAllLogLinesToClipboard() {
            StringBuilder sb = new StringBuilder();
            for (LogLineContainer logLineContainer : logTab.getLogLines()) {
                if (!logLineContainer.getLogLine().isSystem()) {
                    sb.append(logLineContainer.getLogLine().getRawText()).append('\n');
                }
            }
            ClipboardContent clipboard = new ClipboardContent();
            clipboard.putString(sb.toString());
            Clipboard.getSystemClipboard().setContent(clipboard);
        }

        private void saveSearchedLogLinesToFile() {
            PrintWriter pw = getFileWriter("search_result");
            if (pw != null) {
                for (LogLineContainer logLineContainer : logTab.getLogLines()) {
                    if (logLineContainer.hasFoundSearch() && !logLineContainer.getLogLine().isSystem()) {
                        pw.println(logLineContainer.getLogLine().getRawText());
                    }
                }
                pw.close();
            }
        }

        private void saveAllLogLinesToFile() {
            PrintWriter pw = getFileWriter("all");
            if (pw != null) {
                for (LogLineContainer logLineContainer : logTab.getLogLines()) {
                    if (!logLineContainer.getLogLine().isSystem()) {
                        pw.println(logLineContainer.getLogLine().getRawText());
                    }
                }
                pw.close();
            }
        }

        private PrintWriter getFileWriter(String filename) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Log Export File");
            fileChooser.setInitialFileName(
                "ulfv_" + logTab.getTabName().replace(" ", "-") + "_" + filename + "_" + dateFormatter.format(Instant.now()) + ".log.txt");

            //Set extension filter for log files
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("LOG files (*.txt)", "*.txt");
            fileChooser.getExtensionFilters().add(extFilter);

            //Show save file dialog
            File file = fileChooser.showSaveDialog(Main.getPrimaryStage());

            if (file != null) {
                try {
                    return new PrintWriter(file);
                } catch (FileNotFoundException e) {
                    LOGGER.error("Failed to open file " + file.getName() + " to export log", e);
                }
            }

            return null;
        }
    }

}
