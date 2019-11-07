package com.fvp.kubeson;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.fvp.kubeson.common.controller.Upgrade;
import com.fvp.kubeson.common.gui.InfoButton;
import com.fvp.kubeson.common.gui.MainTab;
import com.fvp.kubeson.common.gui.MainTabPane;
import com.fvp.kubeson.common.gui.MainToolbar;
import com.fvp.kubeson.common.gui.Notifications;
import com.fvp.kubeson.common.util.ThreadFactory;
import com.sun.javafx.text.GlyphLayout;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.action.Action;

public class Main extends Application {

    // Workaround fix JVM bug JDK-8146479
    private static final PseudoClass ICONIFIED_PSEUDO_CLASS = PseudoClass.getPseudoClass("iconified");

    private static Logger LOGGER = LogManager.getLogger();

    private static Application application;

    private static Stage primaryStage;

    public static void main(String[] args) {
        LOGGER.info("Running app with Java Version " + System.getProperty("java.version") + " Arch " + System.getProperty("sun.arch.data.model"));

        System.setProperty("java.util.logging.config.file", "");
        // System props
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        System.setProperty("java.util.logging.config.file", "");
        System.setProperty("java.net.useSystemProxies", "true");

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
        Image img = getImage("icons/app16.png");
        if (img != null) {
            ret.add(img);
        }
        img = getImage("icons/app32.png");
        if (img != null) {
            ret.add(img);
        }
        return ret;
    }

    public static Image getImage(String path) {
        InputStream is = Main.class.getClassLoader().getResourceAsStream(path);
        if (is != null) {
            return new Image(is);
        }

        return null;
    }

    public static void showErrorMessage(String title, Exception e) {
        int idx = e.getMessage().indexOf(':');
        String message = e.getMessage().substring(0, idx + 1) + "\n" + e.getMessage().substring(idx + 2);
        showErrorMessage(title, message);
    }

    public static void showErrorMessage(String title, String message) {
        Notifications.create()
                .owner(primaryStage)
                .darkStyle()
                .position(Pos.BOTTOM_RIGHT)
                .hideAfter(Duration.seconds(12))
                .title(title)
                .text(message)
                .showError();
    }

    public static void showWarningMessage(String title, String message) {
        Notifications.create()
                .owner(primaryStage)
                .darkStyle()
                .position(Pos.BOTTOM_RIGHT)
                .hideAfter(Duration.seconds(12))
                .title(title)
                .text(message)
                .showWarning();
    }

    public static void showUpgradeMessage(String version) {
        Notifications.create()
                .owner(primaryStage)
                .darkStyle()
                .position(Pos.BOTTOM_RIGHT)
                .hideAfter(Duration.seconds(12))
                .title("Kubeson " + version + " is now available!")
                .action(new Action("Click here to upgrade", event -> InfoButton.fire()))
                .showInformation();
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
        Main.application = this;
        Main.primaryStage = primaryStage;

        // Main Scene
        VBox root = new VBox();
        Scene scene = new Scene(root, 1600, 800, Color.BLACK);
        scene.setOnKeyPressed(keyEvent -> {
            MainTab SelectedTab = MainTabPane.getSelectedTab();
            if (SelectedTab != null) {
                SelectedTab.onGlobalKeyPressedEvent(keyEvent);
            }
        });

        // Set Main Window
        String appCss = getClass().getClassLoader().getResource("App.css").toExternalForm();

        root.getStylesheets().add(appCss);
        root.getChildren().addAll(MainToolbar.draw(), MainTabPane.draw(), preLoadJsonViewerPage());

        // Workaround fix JVM bug JDK-8146479
        primaryStage.iconifiedProperty()
                .addListener((observable, oldValue, newValue) -> root.pseudoClassStateChanged(ICONIFIED_PSEUDO_CLASS, primaryStage.isIconified()));

        primaryStage.setTitle(Configuration.APP_NAME);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setOnCloseRequest((event) -> {
            ThreadFactory.shutdownAll();
        });
        primaryStage.getIcons().addAll(getAppIcons());
        primaryStage.show();

        Upgrade.start();

        //CSSFX.start();
        //ScenicView.show(scene);
    }
}
