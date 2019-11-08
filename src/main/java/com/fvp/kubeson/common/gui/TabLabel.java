package com.fvp.kubeson.common.gui;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.fvp.kubeson.common.model.TabType;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class TabLabel extends TextFlow {

    private static final Color BLUE = Color.web("#3FA9CF");

    private static final Color RED = Color.web("#e60000");

    private TabType tabType;

    private String text;

    private Text name;

    private List<Color> colors;

    public TabLabel(TabType tabType, String text) {
        this.tabType = tabType;
        this.text = text;
        this.colors = new LinkedList<>();

        Text openBracket = getText("[", Color.YELLOW);
        Text type = getText(tabType.getName(), tabType.getColor());
        Text closeBracket = getText("] ", Color.YELLOW);
        name = getText(text, Color.WHITE);

        super.getChildren().addAll(openBracket, type, closeBracket, name);
    }

    private Text getText(String text, Color color) {
        Text t = new Text(text);
        t.setFill(color);

        return t;
    }

    private void setTextColor(Color color) {
        setTextColor(color, null);
    }

    private void setTextColor(Color color, Integer priority) {
        if (!colors.contains(color)) {
            if (priority != null) {
                colors.add(priority, color);
            } else {
                colors.add(color);
            }
            name.setFill(colors.get(0));
        }
    }

    private void removeTextColor(Color color) {
        colors.remove(color);
        if (colors.size() > 0) {
            name.setFill(colors.get(0));
        } else {
            name.setFill(Color.WHITE);
        }
    }

    public void setErrorColor(boolean value) {
        if (value) {
            setTextColor(RED, 0);
        } else {
            removeTextColor(RED);
        }
    }

    public void setChangeColor(boolean value) {
        if (value) {
            setTextColor(BLUE);
        } else {
            removeTextColor(BLUE);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TabLabel tabLabel = (TabLabel) o;
        return tabType == tabLabel.tabType &&
                Objects.equals(text, tabLabel.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tabType, text);
    }
}
