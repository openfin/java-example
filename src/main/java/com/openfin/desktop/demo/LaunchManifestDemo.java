package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.channel.ChannelAction;
import com.openfin.desktop.channel.ChannelClient;
import com.openfin.desktop.win32.ExternalWindowObserver;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.System;

public class LaunchManifestDemo extends Application implements DesktopStateListener {

    private final static Logger logger = LoggerFactory.getLogger(LaunchManifestDemo.class.getName());
    private final static String WINDOW_TITLE = "Launch Manifest Demo";

    private static String LayoutServiceChannelName = "of-layouts-service-v1";
    private static String appUuid = "LaunchManifestDemo";  // App UUID for startup app in manifest
    private final static String javaConnectUuid = "LaunchManifestDemo-Java"; // connection UUID for Java app
    private DesktopConnection desktopConnection;
    private ChannelClient channelClient;            // for communicating with layout service
    private Button btnGiantMachine, btnScottLogic;
    private Button btnUndock;
    private Stage stage;

    private ExternalWindowObserver externalWindowObserver;  // required for Layout service

    @Override
    public void start(Stage stage) {
        btnGiantMachine = new Button();
        btnGiantMachine.setText("Launch Watchlist by Giant Machines");
        btnGiantMachine.setLayoutX(10);
        btnGiantMachine.setLayoutY(10);
        btnGiantMachine.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                launchGiantMachine();
            }
        });
        btnGiantMachine.setDisable(true);

        btnScottLogic = new Button();
        btnScottLogic.setText("Launch StockFlux by Scott Logic");
        btnScottLogic.setLayoutX(10);
        btnScottLogic.setLayoutY(50);
        btnScottLogic.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                launchScottLogic();
            }
        });
        btnScottLogic.setDisable(true);

        btnUndock = new Button();
        btnUndock.setText("Undock");
        btnUndock.setLayoutX(10);
        btnUndock.setLayoutY(90);
        btnUndock.setDisable(true);

        AnchorPane anchorPane = new AnchorPane();
        anchorPane.getChildren().add(btnGiantMachine);
        anchorPane.getChildren().add(btnScottLogic);
        anchorPane.getChildren().add(btnUndock);

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

        this.stage = stage;
        launchOpenFin();
    }

    private void launchOpenFin() {
        RuntimeConfiguration config = new RuntimeConfiguration();
        config.setRuntimeVersion("stable");
//        config.setRuntimeVersion("9.61.38.40");
        config.setAdditionalRuntimeArguments("--v=1 --remote-debugging-port=9090 ");
        JSONArray serviceConfig = new JSONArray();
        // add Layout service to app manifest
        JSONObject layout = new JSONObject();
        layout.put("name", "layouts");
        JSONObject scfg = new JSONObject();
        JSONObject sfeatures = new JSONObject();
        sfeatures.put("dock", true);
        sfeatures.put("tab", false);
        scfg.put("features", sfeatures);
        layout.put("config", scfg);
        layout.put("manifestUrl", "https://cdn.openfin.co/services/openfin/layouts/1.0.0/app.json");
        serviceConfig.put(0, layout);
        config.addConfigurationItem("services", serviceConfig);

        JSONObject startupApp = new JSONObject();
        startupApp.put("uuid", appUuid);
        startupApp.put("name", appUuid);
        startupApp.put("url", "about:blank");
        startupApp.put("autoShow", false);
        config.setStartupApp(startupApp);

        try {
            this.desktopConnection = new DesktopConnection(javaConnectUuid);
            this.desktopConnection.connect(config, this, 60);
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void launchGiantMachine() {
        launchAppFromManifest("http://localhost:8000/watchlist.json");
    }

    private void launchScottLogic() {
        launchAppFromManifest("http://localhost:8000/stockflux.json");
    }

    private void launchAppFromManifest(String manifest) {
        try {
            com.openfin.desktop.Application.createFromManifest(manifest,
                    new AsyncCallback<com.openfin.desktop.Application>() {
                        @Override
                        public void onSuccess(com.openfin.desktop.Application app) {
                            try {
                                app.run();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }, new AckListener() {
                        @Override
                        public void onSuccess(Ack ack) {
                        }

                        @Override
                        public void onError(Ack ack) {
                            logger.info("error creating app: {}", ack.getReason());
                        }
                    }, desktopConnection);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void createExternalWindowObserver() {
        if (this.externalWindowObserver != null) {
            // only needs to happen once
            return;
        }
        String windowName = appUuid + "-Java-Window";
        try {
            this.externalWindowObserver = new ExternalWindowObserver(desktopConnection.getPort(), appUuid, windowName, this.stage,
                    new AckListener() {
                        @Override
                        public void onSuccess(Ack ack) {
                            ExternalWindowObserver observer = (ExternalWindowObserver) ack.getSource();
                            observer.getDesktopConnection().getChannel(LayoutServiceChannelName).connect(LayoutServiceChannelName,
                                    new AsyncCallback<ChannelClient>() {
                                        @Override
                                        public void onSuccess(ChannelClient client) {
                                            LaunchManifestDemo.this.channelClient = client;

                                            client.register("event", new ChannelAction() {
                                                @Override
                                                public JSONObject invoke(String action, JSONObject payload) {
                                                    System.out.printf("channel event " + action);
                                                    return null;
                                                }
                                            });
                                        }
                                    });
                        }

                        @Override
                        public void onError(Ack ack) {
                            System.out.println(windowName + ": unable to register external window, " + ack.getReason());
                        }
                    });
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // when this Java window is docked or undocked by layout service, "group-changed" is fired.
        // calling getGroup to determine if btnUndock should enabled.
        Window w = Window.wrap(appUuid, windowName, desktopConnection);
        w.addEventListener("group-changed", new EventListener() {
            @Override
            public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                w.getGroup(new AsyncCallback<java.util.List<Window>>() {
                    @Override
                    public void onSuccess(java.util.List<Window> result) {
                        if (result.size() > 0) {
                            btnUndock.setDisable(false);
                        } else {
                            btnUndock.setDisable(true);
                        }
                    }
                }, null);
            }
        }, null);

        try {
            this.externalWindowObserver.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReady() {
        btnGiantMachine.setDisable(false);
        btnScottLogic.setDisable(false);
        this.createExternalWindowObserver();
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

    public static void main(String args[]) {
        launch(args);
    }



}
