package com.fvp.kubeson.logs.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fvp.kubeson.common.gui.TabPillButtonList;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public enum HttpMethod implements TabPillButtonList {
    GET("#ff794d", "http-method-get"),
    HEAD("#00bfff", "http-method-head"),
    POST("#ff00ff", "http-method-post"),
    PUT("#ffccff", "http-method-put"),
    PATCH("#bf00ff", "http-method-patch"),
    DELETE("#ff3333", "http-method-delete");

    private static Pattern HTTP_METHOD_PATTERN = Pattern.compile("\"method\":\"([A-Z]+)\"");

    private Color color;

    private String styleClass;

    HttpMethod(final String color, final String styleClass) {
        this.color = Color.web(color);
        this.styleClass = styleClass;
    }

    public static HttpMethod locate(String ulf) {
        try {
            Matcher matcher = HTTP_METHOD_PATTERN.matcher(ulf);
            if (matcher.find()) {
                return HttpMethod.valueOf(matcher.group(1));
            }
        } catch (Exception e) {

        }

        return null;
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
    public Image getIcon() {
        return null;
    }

    public Color getColor() {
        return color;
    }
}
