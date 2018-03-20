package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.Window;
import com.sun.javafx.tk.TKStage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Java example to embed HTML5 window in JavaFX window
 *
 * Created by wche on 3/13/15.
 *
 */

public class FxDemo extends Application implements DesktopStateListener {
    private final static Logger logger = LoggerFactory.getLogger(FxDemo.class.getName());

    private DesktopConnection desktopConnection;
    private Button btnStart, btnStop;
    private StackPane embedPane;

    protected String openfin_app_url = "https://cdn.openfin.co/examples/junit/SimpleDockingExample.html";  // source is in release/SimpleDockingExample.html
    protected String startupUuid = "OpenFinHelloWorld";
    private long stageHWndId;
    private com.openfin.desktop.Application startupHtml5app;

    @Override
    public void start(Stage stage) {
        btnStart = new Button();
        btnStart.setText("Launch OpenFin");
        btnStart.setLayoutX(10);
        btnStart.setLayoutY(10);
        btnStart.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                launchOpenFin();
            }
        });
        btnStop = new Button();
        btnStop.setText("Shut down OpenFin");
        btnStop.setLayoutX(200);
        btnStop.setLayoutY(10);
        btnStop.setDisable(true);
        btnStop.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                shutDown();
            }
        });

        AnchorPane anchorPane = new AnchorPane();
        anchorPane.getChildren().add(btnStart);
        anchorPane.getChildren().add(btnStop);

        //Creating a Group object
        Group root = new Group();

        //Retrieving the observable list object
        ObservableList list = root.getChildren();


        embedPane = new StackPane();
        embedPane.setLayoutX(10);
        embedPane.setLayoutY(50);
        AnchorPane.setTopAnchor(embedPane, 50.0);
        AnchorPane.setBottomAnchor(embedPane, 0.0);
        AnchorPane.setLeftAnchor(embedPane, 0.0);
        AnchorPane.setRightAnchor(embedPane, 0.0);
        anchorPane.getChildren().add(embedPane);
        embedPane.widthProperty().addListener(observable -> {
            onEmbedComponentSizeChange();
        });
        embedPane.heightProperty().addListener(observable -> {
            onEmbedComponentSizeChange();
        });

        //Creating a scene object
        Scene scene = new Scene(anchorPane, 800, 800);

        //Setting title to the Stage
        stage.setTitle("FX Demo");

        //Adding scene to the stage
        stage.setScene(scene);

        //Displaying the contents of the stage
        stage.show();

        this.stageHWndId = getWindowHandle(stage);
    }

    private void launchOpenFin() {
        RuntimeConfiguration cfg = new RuntimeConfiguration();
        cfg.setRuntimeVersion("stable");
        cfg.setAdditionalRuntimeArguments(" --v=1 ");
        try {
            desktopConnection = new DesktopConnection("JavaFxDemo");
            desktopConnection.connect(cfg, this, 60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchHtmlApp() {
        // launch 5 instances of same example app
        int width = 500, height=500;
        try {
            String url = java.lang.System.getProperty("com.openfin.demo.embed.URL");
            if (url != null) {
                openfin_app_url = url;
            }
            ApplicationOptions options = new ApplicationOptions(startupUuid, startupUuid, openfin_app_url);
            options.setApplicationIcon("http://openfin.github.io/snap-and-dock/openfin.ico");
            WindowOptions mainWindowOptions = new WindowOptions();
            mainWindowOptions.setAutoShow(false);
            mainWindowOptions.setDefaultHeight(height);
            mainWindowOptions.setDefaultLeft(10);
            mainWindowOptions.setDefaultTop(50);
            mainWindowOptions.setDefaultWidth(width);
            mainWindowOptions.setShowTaskbarIcon(true);
            mainWindowOptions.setSaveWindowState(false);  // set to false so all windows start at same initial positions for each run
            mainWindowOptions.setFrame(false);
            mainWindowOptions.setContextMenu(true);
            mainWindowOptions.setResizeRegionSize(0);
            options.setMainWindowOptions(mainWindowOptions);
            DemoUtils.runApplication(options, this.desktopConnection,  new AckListener() {
                @Override
                public void onSuccess(Ack ack) {
                    com.openfin.desktop.Application app = (com.openfin.desktop.Application) ack.getSource();
                    try {
                        Thread.sleep(1000);
                        embedStartupApp();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                @Override
                public void onError(Ack ack) {
                    logger.error(String.format("Error launching %s %s", options.getUUID(), ack.getReason()));
                }
            });
        } catch (Exception e) {
            logger.error("Error launching app", e);
        }
    }

    private void embedStartupApp() {
        try {
            if (startupHtml5app == null) {
                startupHtml5app = com.openfin.desktop.Application.wrap(this.startupUuid, this.desktopConnection);
            }

            Window html5Wnd = startupHtml5app.getWindow();
            html5Wnd.embedInto(stageHWndId, (int)this.embedPane.getLayoutX(), (int)this.embedPane.getLayoutY(),
                    (int)this.embedPane.getWidth(), (int)this.embedPane.getHeight(), new AckListener() {
                @Override
                public void onSuccess(Ack ack) {
                    if (ack.isSuccessful()) {
                    } else {
                        logger.error("embedding failed: " + ack.getJsonObject().toString());
                    }
                }
                @Override
                public void onError(Ack ack) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onEmbedComponentSizeChange() {
        logger.info(String.format("%f %f ", this.embedPane.getLayoutX(), this.embedPane.getLayoutY()));
        if (startupHtml5app != null) {
            startupHtml5app.getWindow().embedComponentSizeChange((int)this.embedPane.getLayoutX(), (int)this.embedPane.getLayoutY(),
                    (int)this.embedPane.getWidth(), (int)this.embedPane.getHeight());
        }
    }

    private void shutDown() {
        try {
            this.desktopConnection.exit();
            Platform.exit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long getWindowHandle(Stage stage) {
        long handle = -1;
        try {
            TKStage tkStage = stage.impl_getPeer();
            Method getPlatformWindow = tkStage.getClass().getDeclaredMethod("getPlatformWindow" );
            getPlatformWindow.setAccessible(true);
            Object platformWindow = getPlatformWindow.invoke(tkStage);
            Method getNativeHandle = platformWindow.getClass().getMethod( "getNativeHandle" );
            getNativeHandle.setAccessible(true);
            Object nativeHandle = getNativeHandle.invoke(platformWindow);
            handle = (Long) nativeHandle;
        } catch (Throwable e) {
            logger.error("Error getting Window Pointer", e);
        }
        logger.info(String.format("Stage hwnd %d", handle));
        return handle;
    }

    @Override
    public void onReady() {
        logger.info("Connected to OpenFin Runtime");
        btnStart.setDisable(true);
        btnStop.setDisable(false);
        launchHtmlApp();
    }

    @Override
    public void onClose(String error) {
        logger.info("Disconnected from OpenFin Runtime");
        btnStart.setDisable(false);
        btnStop.setDisable(true);
    }

    @Override
    public void onError(String s) {
    }

    @Override
    public void onMessage(String s) {
    }

    @Override
    public void onOutgoingMessage(String s) {
    }

    public static void main(String args[]) {
        launch(args);
    }

}