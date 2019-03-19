package com.fvp.kubeson.model;

import java.util.List;
import java.util.Objects;

import javafx.scene.paint.Color;

public class LogLineContainer {

    private LogSource logSource;

    private LogLine logLine;

    private int id;

    private Color idColor;

    private List<SearchItem> searchItems;

    // Used only for equality reasons
    public LogLineContainer(LogLine logLine) {
        this.logLine = logLine;
    }

    public LogLineContainer(LogSource logSource, LogLine logLine, int id, Color idColor) {
        this.logSource = logSource;
        this.logLine = logLine;
        this.id = id;
        this.idColor = idColor;
    }

    public LogLine getLogLine() {
        return logLine;
    }

    public LogSource getLogSource() {
        return logSource;
    }

    public int getId() {
        return id;
    }

    public Color getIdColor() {
        return idColor;
    }

    public boolean hasFoundSearch() {
        return searchItems != null;
    }

    public List<SearchItem> getSearchItems() {
        return searchItems;
    }

    public void setSearchItems(List<SearchItem> searchItems) {
        this.searchItems = searchItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LogLineContainer that = (LogLineContainer) o;
        return Objects.equals(logLine, that.logLine);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logLine);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LogLineContainer{");
        sb.append("logLine=").append(logLine);
        sb.append(", logSource=").append(logSource);
        sb.append(", id=").append(id);
        sb.append(", idColor=").append(idColor);
        sb.append(", foundSearch=").append(hasFoundSearch());
        sb.append('}');
        return sb.toString();
    }
}
