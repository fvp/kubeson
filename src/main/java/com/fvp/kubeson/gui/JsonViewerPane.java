package com.fvp.kubeson.gui;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fvp.kubeson.model.LogLine;
import com.fvp.kubeson.model.LogLineContainer;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JsonViewerPane {

    private static Logger LOGGER = LogManager.getLogger();

    private static ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
    }

    private LogTab logTab;

    private WebView webview;

    private WebEngine webEngine;

    private String selection;

    public JsonViewerPane(LogTab logTab) {
        this.logTab = logTab;
    }

    private static String colorToHex(Color color) {
        return "#" + color.toString().substring(2, 8);
    }

    private static String prettyPrintJson(String json) {
        try {
            final JsonNode jsonNode = mapper.readTree(json);
            transformJsonStringsIntoJsonObject(jsonNode);

            return escapeJsonInput(mapper.writeValueAsString(jsonNode));
        } catch (IOException e) {
            ObjectNode msg = mapper.createObjectNode();
            msg.put("jsonParserError", e.getMessage());

            return escapeJsonInput(msg.toString());
        }
    }

    private static String escapeJsonInput(String json) {
        return StringUtils.replaceEach(json, new String[]{"\\", "'"}, new String[]{"\\\\", "\\'"});
    }

    private static void transformJsonStringsIntoJsonObject(JsonNode currentNode) {
        if (currentNode.isArray()) {
            for (final JsonNode jn : currentNode) {
                transformJsonStringsIntoJsonObject(jn);
            }
        } else if (currentNode.isObject()) {
            currentNode.fields().forEachRemaining(entry -> {
                final JsonNode jn = entry.getValue();

                if (jn.getNodeType() == JsonNodeType.STRING) {
                    final String trimmedValue = jn.asText().trim();
                    if (trimmedValue.startsWith("{") || trimmedValue.startsWith("[")) {
                        try {
                            final JsonNode newJsonNode = mapper.readTree(jn.asText());
                            ((ObjectNode) currentNode).set(entry.getKey(), newJsonNode);
                        } catch (IOException e) {
                        }
                    }
                } else if (jn.isObject() || jn.isArray()) {
                    transformJsonStringsIntoJsonObject(jn);
                }
            });
        }
    }

    public boolean isDrawn() {
        return webview != null;
    }

    public Node draw() {
        webview = new WebView();
        webview.setContextMenuEnabled(false);
        webEngine = webview.getEngine();
        String jsonViewer = this.getClass().getClassLoader().getResource("json-viewer/index.html").toExternalForm();
        webEngine.load(jsonViewer);
        createContextMenu();
        return webview;
        //StackPane s = new StackPane(webview);
        //s.setStyle("-fx-padding: 30px;");

        //return s;
    }

    private void createContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(e -> {
            ClipboardContent clipboard = new ClipboardContent();
            clipboard.putString(selection);
            Clipboard.getSystemClipboard().setContent(clipboard);
        });
        MenuItem search = new MenuItem("Search");
        search.setOnAction(e -> SearchBoxController.setSearchText(selection));
        contextMenu.getItems().addAll(copy, new SeparatorMenuItem(), search);

        webview.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                selection = (String) webview.getEngine().executeScript("window.getSelection().toString()");
                if (!StringUtils.isEmpty(selection)) {
                    contextMenu.show(webview, e.getScreenX(), e.getScreenY());
                } else {
                    contextMenu.hide();
                }
            } else {
                contextMenu.hide();
            }
        });
    }

    public void clear() {
        if (isDrawn() && webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            webEngine.executeScript("clear()");
        }
    }

    public void updateLogLine() {
        if (isDrawn()) {
            LogLineContainer logLineContainer = logTab.getLogListView().getSelectedLogLine();
            if (logLineContainer != null && logLineContainer.getLogLine().isJson()) {
                LogLine logLine = logLineContainer.getLogLine();
                String json = prettyPrintJson(logLine.getRawText());

                String script =
                    "showJson('" + logLineContainer.getId() + "', '" + colorToHex(logLineContainer.getIdColor()) + "', '" + json + "', '" + colorToHex(
                        logLine.getColor()) + "', '" + escapeJsonInput(logTab.getSearchManager().getSearchText()) + "')";
                try {
                    webEngine.executeScript(script);
                } catch (Exception e) {
                    LOGGER.error("Failed to execute JS: " + script, e);
                }
            } else {
                clear();
            }
        }
    }

    public void updateSearch() {
        if (isDrawn()) {
            String script = "search('" + escapeJsonInput(logTab.getSearchManager().getSearchText()) + "')";
            try {
                webEngine.executeScript(script);
            } catch (Exception e) {
                LOGGER.error("Failed to execute JS: " + script, e);
            }
        }
    }
}
