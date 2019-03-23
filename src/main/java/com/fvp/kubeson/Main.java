package com.fvp.kubeson;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.fvp.kubeson.core.ThreadFactory;
import com.fvp.kubeson.gui.ClearButton;
import com.fvp.kubeson.gui.InfoDialog;
import com.fvp.kubeson.gui.LogTab;
import com.fvp.kubeson.gui.LogTabPane;
import com.fvp.kubeson.gui.PodSelector;
import com.fvp.kubeson.gui.SearchBoxController;
import com.fvp.kubeson.gui.StopButton;
import com.fvp.kubeson.gui.TabListener;
import com.fvp.kubeson.gui.TabPill;
import com.fvp.kubeson.model.LogCategory;
import com.fvp.kubeson.model.LogLevel;
import com.sun.javafx.text.GlyphLayout;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main extends Application {

    // Workaround fix JVM bug JDK-8146479
    private static final PseudoClass ICONIFIED_PSEUDO_CLASS = PseudoClass.getPseudoClass("iconified");

    private static Logger LOGGER = LogManager.getLogger();

    private static Application application;

    private static Stage primaryStage;

    private static KeyCombination ctrlR = new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_ANY);

    private static KeyCombination ctrlS = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_ANY);

    private static KeyCombination ctrlF = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_ANY);

    public static void main(String[] args) {
        LOGGER.info("Running app with Java Version " + System.getProperty("java.version") + " Arch " + System.getProperty("sun.arch.data.model"));

        // Very ugly hack to use BreakIterator.getLineInstance() for word wrapping. Probably not version safe.
        try {
            Field isIdeographicMethod = GlyphLayout.class.getDeclaredField("isIdeographicMethod");
            isIdeographicMethod.setAccessible(true);
            isIdeographicMethod.set(null, Main.class.getMethod("alwaysTrue", int.class));
        } catch (Exception e) {
            LOGGER.error("Failed to hack JavaFx to use BreakIterator.getLineInstance() for word wrapping", e);
        }

        launch(args);
    }

    public static boolean alwaysTrue(int intParam) {
        return true;
    }

    public static Application getApplication() {
        return application;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static List<Image> getAppIcons() {
        List<Image> ret = new ArrayList<>();
        InputStream is = Main.class.getClassLoader().getResourceAsStream("icons/app16.png");
        if (is != null) {
            ret.add(new Image(is));
        }
        is = Main.class.getClassLoader().getResourceAsStream("icons/app32.png");
        if (is != null) {
            ret.add(new Image(is));
        }
        return ret;
    }

    private static void globalKeyPressedEvent(KeyEvent keyEvent) {
        if (ctrlR.match(keyEvent)) {
            ClearButton.fire();
        } else if (ctrlS.match(keyEvent)) {
            StopButton.fire();
        } else if (ctrlF.match(keyEvent)) {
            SearchBoxController.requestFocus();
        }
    }

    private static void logError(Thread t, Throwable e) {
        if (Platform.isFxApplicationThread()) {
            LOGGER.error("An error occurred in JavaFx thread", e);
        } else {
            LOGGER.error("An unexpected error occurred", e);
        }
    }

    private WebView preLoadJsonViewerPage() {
        WebView webview = new WebView();
        webview.setVisible(false);
        webview.setPrefSize(0, 0);
        WebEngine webEngine = webview.getEngine();
        String jsonViewer = this.getClass().getClassLoader().getResource("json-viewer/index.html").toExternalForm();
        webEngine.load(jsonViewer);

        return webview;
    }

    @Override
    public void start(Stage primaryStage) {
        // Use Log4j2 for JUL logging
        Thread.setDefaultUncaughtExceptionHandler(Main::logError);
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        System.setProperty("java.util.logging.config.file", "");
        Main.application = this;
        Main.primaryStage = primaryStage;

        // Main Scene
        VBox root = new VBox();
        Scene scene = new Scene(root, 1600, 800, Color.BLACK);
        scene.setOnKeyPressed(Main::globalKeyPressedEvent);

        //Tool Bar
        ToolBar toolbar = new ToolBar();
        // Set Pod Selector
        toolbar.getItems().add(PodSelector.draw());

        /* Central Area */

        // Set Log Level Pill
        TabPill<LogLevel> logLevelPill = new TabPill<>(LogLevel.class);
        HBox.setHgrow(logLevelPill, Priority.ALWAYS);

        // Buttons
        HBox buttons = new HBox(ClearButton.draw(), StopButton.draw());
        buttons.setAlignment(Pos.CENTER);
        buttons.setStyle("-fx-padding: 0 20 0 20");
        buttons.setSpacing(23);
        buttons.setPrefWidth(Region.USE_PREF_SIZE);
        //HBox.setHgrow(buttons, Priority.ALWAYS);

        // Set Log Category Pill
        TabPill<LogCategory> ulfCategoryPill = new TabPill<>(LogCategory.class);
        HBox.setHgrow(ulfCategoryPill, Priority.ALWAYS);

        HBox centralArea = new HBox(logLevelPill, buttons, ulfCategoryPill);
        centralArea.setAlignment(Pos.CENTER);
        centralArea.setStyle("-fx-padding: 0 20 0 20");
        //centralArea.setSpacing(23);
        toolbar.getItems().add(centralArea);
        HBox.setHgrow(centralArea, Priority.ALWAYS);

        // LogTabPane Event Listener
        LogTabPane.addListener(new TabListener() {

            @Override
            public void onTabChange(LogTab newLogTab) {
                ClearButton.changeTab(newLogTab);
                StopButton.changeTab(newLogTab);
                logLevelPill.changeTab(newLogTab);
                ulfCategoryPill.changeTab(newLogTab);
                SearchBoxController.changeTab(newLogTab);
            }

            @Override
            public void onTabClosed(LogTab logTab) {
                ClearButton.destroyTab(logTab);
                StopButton.destroyTab(logTab);
                logLevelPill.destroyTab(logTab);
                ulfCategoryPill.destroyTab(logTab);
                SearchBoxController.destroyTab(logTab);
            }
        });

        // Set Search Box
        toolbar.getItems().add(SearchBoxController.draw());

        // Set Info Button
        InputStream is = getClass().getClassLoader().getResourceAsStream("icons/info.png");
        ImageView imageView = new ImageView(new Image(is));

        Label label = new Label();
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.setGraphic(imageView);
        label.setTooltip(new Tooltip("INFO"));
        label.setOnMouseClicked(event -> {
            new InfoDialog(scene.getWindow());
        });

        toolbar.getItems().add(label);

        // Set Main Window
        String appCss = getClass().getClassLoader().getResource("App.css").toExternalForm();

        root.getStylesheets().add(appCss);
        root.getChildren().addAll(toolbar, LogTabPane.draw(), preLoadJsonViewerPage());

        // Workaround fix JVM bug JDK-8146479
        primaryStage.iconifiedProperty()
            .addListener((observable, oldValue, newValue) -> root.pseudoClassStateChanged(ICONIFIED_PSEUDO_CLASS, primaryStage.isIconified()));

        primaryStage.setTitle("Kubeson - Kubernetes Json Log Viewer");
        primaryStage.setScene(scene);
        //primaryStage.setMaximized(true);
        primaryStage.setOnCloseRequest((event) -> {
            ThreadFactory.shutdownAll();
        });
        primaryStage.getIcons().addAll(getAppIcons());
        primaryStage.show();

        //CSSFX.start();
        //ScenicView.show(scene);
    }
}
