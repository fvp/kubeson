package com.fvp.kubeson;

import javafx.scene.paint.Color;

public final class Configuration {

    public static final Color[] LOG_ID_COLORS = new Color[]{Color.CORNFLOWERBLUE, Color.GREEN};

    public static final Color SYSTEM_MSG_COLOR = Color.GREEN;

    public static final Color[] LOG_SOURCE_COLORS = new Color[]{
        Color.web("#00bfff"), Color.web("#ff00ff"), Color.web("#94b8b8"), Color.web("#bf00ff"), Color.web("#ffccff"), Color.web("#ff794d"),
        Color.web("#a3a3c2"), Color.web("#a3a375"), Color.web("#d27979"), Color.web("#6666cc")
    };

    public static final int MAX_JSON_FIELD_SIZE = 1200;

    public static final int MAX_JSON_SIZE = MAX_JSON_FIELD_SIZE + 1000;

    public static final String MAX_JSON_FIELD_MESSAGE = "******* CONTENT REMOVED, FIELD SIZE=%d *******";

    public static final int MAX_LOG_LINES = 10000; //70

    public static final int MAX_KUBERNETES_CLIENT_ATTEMPTS = 3;

    public static final int CLIPBOARD_COPY_MAX_LOG_LINES = 500;

    public static double LOG_LIST_PANEL_SPLIT = 0.7;

    public static int KUBERNETES_WORKER_WAIT_TIME_MS = 500;
}
