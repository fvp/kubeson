package com.fvp.kubeson.model;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fvp.kubeson.Configuration;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogLine {

    private static Logger LOGGER = LogManager.getLogger();

    private static ObjectMapper mapper = new ObjectMapper();

    private boolean json;

    private Color color;

    private LogLevel logLevel;

    private LogCategory logCategory;

    private String[] logText;

    public LogLine(String text) {
        this(text, false);
    }

    public LogLine(String text, boolean system) {
        final String subtext;
        if (text.startsWith("{")) {
            subtext = StringUtils.substring(text, 0, 140).toUpperCase();
            json = true;
            setLogCategory(subtext, text);
        } else {
            subtext = StringUtils.substring(text, 0, 60).toUpperCase();
        }
        logLevel = LogLevel.findLogLevel(subtext);

        if (json && text.length() > Configuration.MAX_JSON_SIZE) {
            this.logText = minifyJson(text);
        } else {
            this.logText = new String[]{text};
        }

        if (system) {
            this.color = Configuration.SYSTEM_MSG_COLOR;
        } else {
            this.color = logLevel.getColor();
        }
    }

    private static String[] minifyJson(String originalJson) {
        try {
            final JsonNode jsonNode = mapper.readTree(originalJson);
            minifyJson(jsonNode);

            return jsonNode.toString().split("<<>>"); // TODO: Use less common UTF-8 Characters
        } catch (IOException e) {
            LOGGER.error("Failed to minify Json: " + originalJson, e);
        }

        return new String[]{originalJson};
    }

    private static void minifyJson(JsonNode currentNode) {
        if (currentNode.isArray()) {
            for (final JsonNode jn : currentNode) {
                minifyJson(jn);
            }
        } else if (currentNode.isObject()) {
            currentNode.fields().forEachRemaining(entry -> {
                final JsonNode jn = entry.getValue();
                if (jn.getNodeType() == JsonNodeType.STRING && jn.asText().length() > Configuration.MAX_JSON_FIELD_SIZE) {
                    ((ObjectNode) currentNode).put(entry.getKey(), "<<>>" + jn.asText() + "<<>>");
                } else if (jn.isObject() || jn.isArray()) {
                    minifyJson(jn);
                }
            });
        }
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    private void setLogLevel(String text) {
        for (LogLevel level : LogLevel.values()) {
            if (text.contains(level.name())) {
                logLevel = level;
                return;
            }
        }
    }

    public LogCategory getLogCategory() {
        return logCategory;
    }

    private void setLogCategory(String subText, String text) {
        for (LogCategory category : LogCategory.values()) {
            if (subText.contains(category.getName())
                    && (category.getSubCategorySearch() == null || text.contains(category.getSubCategorySearch()))
            ) {
                logCategory = category;
                return;
            }
        }
    }

    public String getText() {
        if (logText.length == 1) {
            return logText[0];
        }
        StringBuilder sb = new StringBuilder(logText[0].length());
        for (int i = 0; i < logText.length; i++) {
            if (i % 2 == 0) { //Even
                sb.append(logText[i]);
            } else { // Odds
                sb.append(String.format(Configuration.MAX_JSON_FIELD_MESSAGE, logText[i].length()));
            }
        }

        return sb.toString();
    }

    public String[] getTextArray() {
        String[] res = new String[logText.length];
        for (int i = 0; i < logText.length; i++) {
            if (i % 2 == 0) { //Even
                res[i] = logText[i];
            } else { // Odds
                res[i] = String.format(Configuration.MAX_JSON_FIELD_MESSAGE, logText[i].length());
            }
        }

        return res;
    }

    public String getRawText() {
        if (logText.length == 1) {
            return logText[0];
        }
        StringBuilder sb = new StringBuilder(logText[0].length());
        for (String txt : logText) {
            sb.append(txt);
        }

        return sb.toString();
    }

    public String[] getLogText() {
        return logText;
    }

    public boolean isJson() {
        return json;
    }

    public boolean isSystem() {
        return Configuration.SYSTEM_MSG_COLOR.equals(color);
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LogLine{");
        sb.append("logText='").append(getRawText()).append('\'');
        sb.append(", json=").append(json);
        sb.append(", color=").append(color);
        sb.append(", logLevel=").append(logLevel);
        sb.append(", logCategory=").append(logCategory);
        sb.append('}');
        return sb.toString();
    }
}
