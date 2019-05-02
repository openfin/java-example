/**
 * Demo for launching OpenFin app and send messages via Inter application bus
 *
 * javascript side is in release/busdemo.html, which needs to be hosted in localhost:8888
 */

package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LauncherBusDemo extends Application {
    private final static Logger logger = LoggerFactory.getLogger(LauncherBusDemo.class.getName());
    private final static String WINDOW_TITLE = "Launcher and InterAppBus Demo";

    private DesktopConnection desktopConnection;
    private InterApplicationBus interApplicationBus;
    private Button btnOFApp1, btnOFApp2;
    private Button btnOFSendApp1, btnOFSendApp2;  // send messages to OpenFin app via Inter App Bus
    private final String app1Uuid = "OpenFin app1";
    private final String app2Uuid = "OpenFin app2";
    private final String appUrl = "http://localhost:8888/busdemo.html";
    com.openfin.desktop.Application app1, app2;  // OpenFin apps

    @Override
    public void start(Stage stage) {
        btnOFApp1 = new Button();
        btnOFApp1.setDisable(true);
        btnOFApp1.setText("Launch OpenFin app1");
        btnOFApp1.setLayoutX(10);
        btnOFApp1.setLayoutY(10);
        btnOFApp1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                launchOFApp1(app1Uuid, appUrl, 500, 100);
            }
        });

        btnOFApp2 = new Button();
        btnOFApp2.setDisable(true);
        btnOFApp2.setText("Launch OpenFin App2");
        btnOFApp2.setLayoutX(10);
        btnOFApp2.setLayoutY(50);
        btnOFApp2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                launchOFApp2(app2Uuid, appUrl, 500, 500);
            }
        });

        btnOFSendApp1 = new Button();
        btnOFSendApp1.setDisable(true);
        btnOFSendApp1.setText("Send messages OpenFin app1");
        btnOFSendApp1.setLayoutX(10);
        btnOFSendApp1.setLayoutY(90);
        btnOFSendApp1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                sendOFApp(app1);
            }
        });

        btnOFSendApp2 = new Button();
        btnOFSendApp2.setDisable(true);
        btnOFSendApp2.setText("Send messages OpenFin app2");
        btnOFSendApp2.setLayoutX(10);
        btnOFSendApp2.setLayoutY(130);
        btnOFSendApp2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                sendOFApp(app2);
            }
        });

        AnchorPane anchorPane = new AnchorPane();
        anchorPane.getChildren().add(btnOFApp1);
        anchorPane.getChildren().add(btnOFApp2);
        anchorPane.getChildren().add(btnOFSendApp1);
        anchorPane.getChildren().add(btnOFSendApp2);

        //Creating a Group object
        Group root = new Group();

        //Retrieving the observable list object
        ObservableList list = root.getChildren();

        //Creating a scene object
        Scene scene = new Scene(anchorPane, 800, 800);

        //Setting title to the Stage
        stage.setTitle(WINDOW_TITLE);

        //Adding scene to the stage
        stage.setScene(scene);

        //Displaying the contents of the stage
        stage.show();

        launchRuntime();
    }

    private void launchRuntime() {
        if (desktopConnection == null) {
            RuntimeConfiguration cfg = new RuntimeConfiguration();
            cfg.setRuntimeVersion("stable");
            cfg.setAdditionalRuntimeArguments("--v=1");   // enable verbose logging by Runtime
            try {
                desktopConnection = new DesktopConnection("Java app");
                desktopConnection.connect(cfg, new DesktopStateListener() {
                    @Override
                    public void onReady() {
                        logger.info("Connected to OpenFin Runtime");
                        interApplicationBus = new InterApplicationBus(desktopConnection);
                        btnOFApp1.setDisable(false);
                        btnOFApp2.setDisable(false);
                    }

                    @Override
                    public void onClose(String error) {
                    }

                    @Override
                    public void onError(String reason) {
                    }

                    @Override
                    public void onMessage(String message) {
                    }

                    @Override
                    public void onOutgoingMessage(String message) {
                    }
                }, 60);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void launchOFApp1(String uuid, String url, int left, int top) {
        ApplicationOptions options = createAppOptions(uuid, url, left, top);
        app1 = new com.openfin.desktop.Application(options, desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                app1.run(new AckListener() {
                    @Override
                    public void onSuccess(Ack ack) {
                        btnOFSendApp1.setDisable(false);
                    }
                    @Override
                    public void onError(Ack ack) {
                        logger.error(String.format("Error running %s", uuid));
                    }
                });
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("Error creating %s", uuid));
            }
        });
    }

    private void launchOFApp2(String uuid, String url, int left, int width) {
        ApplicationOptions options = createAppOptions(uuid, url, left, width);
        app2 = new com.openfin.desktop.Application(options, desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                app2.run(new AckListener() {
                    @Override
                    public void onSuccess(Ack ack) {
                        btnOFSendApp2.setDisable(false);
                    }
                    @Override
                    public void onError(Ack ack) {
                        logger.error(String.format("Error running %s", uuid));
                    }
                });
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("Error creating %s", uuid));
            }
        });
    }

    private ApplicationOptions createAppOptions(String uuid, String url, int left, int top) {
        ApplicationOptions options = new ApplicationOptions(uuid, uuid, url);
        WindowOptions windowOptions = new WindowOptions();
        windowOptions.setDefaultHeight(400);
        windowOptions.setDefaultWidth(400);
        windowOptions.setDefaultLeft(left);
        windowOptions.setDefaultTop(top);
        windowOptions.setSaveWindowState(false);  // so last position is not saved
        windowOptions.setContextMenu(true);       // enable Javascript Devtools
        windowOptions.setAutoShow(false);          // hide the window initially, show it when a message is sent to it
        options.setMainWindowOptions(windowOptions);
        return options;
    }

    private void sendOFApp(com.openfin.desktop.Application app) {
        JSONObject msg = new JSONObject();
        msg.put("ticker", "AAPL");
        msg.put("price", Math.random() * 100);
        try {
            interApplicationBus.send(app.getOptions().getUUID(), "messageFromJavaTopic", msg);
            app.getWindow().show();
        } catch (DesktopException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        launch(args);
    }



}
