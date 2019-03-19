package com.fvp.kubeson.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fvp.kubeson.gui.TabPillButtonList;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum LogLevel implements TabPillButtonList {
    TRACE(Color.ORANGE, "log-level-trace", "(?i)trace[ \"\\]\\[]"),
    DEBUG(Color.CYAN, "log-level-debug", "(?i)debug[ \"\\]]"),
    INFO(Color.WHITE, "log-level-info", "^I[0-9]|(?i)info[ \"\\]]|(?i)note[ \"\\]]"),
    WARN(Color.YELLOW, "log-level-warn", "^W[0-9]|(?i)warn[ \"\\]]|(?i)warning[ \"\\]]"),
    ERROR(Color.RED, "log-level-error", "^E[0-9]|(?i)erro[r]*[ \"\\]]"),
    FATAL(Color.FIREBRICK, "log-level-fatal", "(?i)fatal[ \"\\]]"),
    UNK(Color.LIGHTGRAY, "log-level-unk", null);

    private static final Pattern searchPattern;

    private static Logger LOGGER = LogManager.getLogger();

    static {
        StringBuilder searchKeysPattern = new StringBuilder();
        LogLevel[] logLevels = LogLevel.values();
        for (int i = 0; i < logLevels.length; i++) {
            if (logLevels[i] != UNK) {
                if (i > 0) {
                    searchKeysPattern.append('|');
                }
                searchKeysPattern.append("(?<").append(logLevels[i].name()).append(">");
                searchKeysPattern.append(logLevels[i].getSearchRegex());
                searchKeysPattern.append(')');
            }
        }
        LOGGER.debug("Log Level search pattern = " + searchKeysPattern.toString());
        searchPattern = Pattern.compile(searchKeysPattern.toString());
    }

    private final Color color;

    private final String styleClass;

    private final String searchRegex;

    LogLevel(final Color color, final String styleClass, final String searchRegex) {
        this.color = color;
        this.styleClass = styleClass;
        this.searchRegex = searchRegex;
    }

    public static LogLevel findLogLevel(String searchText) {
        Matcher matcher = searchPattern.matcher(searchText);
        if (matcher.find()) {
            for (LogLevel logLevel : LogLevel.values()) {
                if (logLevel != UNK && matcher.group(logLevel.name()) != null) {
                    return logLevel;
                }
            }
        }
        return UNK;
    }

    public Color getColor() {
        return this.color;
    }

    @Override
    public String getText() {
        return this.name();
    }

    @Override
    public String getStyleClass() {
        return styleClass;
    }

    @Override
    public String getIcon() {
        return null;
    }

    public String getSearchRegex() {
        return searchRegex;
    }
}
