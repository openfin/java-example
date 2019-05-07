/**
 * Demo for launching OpenFin app and send messages via Inter application bus
 *
 * javascript side is in release/busdemo.html, which needs to be hosted in localhost:8888
 */

package com.openfin.desktop.demo;

import com.openfin.desktop.*;
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

public class LauncherBusDemo extends Application {
    private final static Logger logger = LoggerFactory.getLogger(LauncherBusDemo.class.getName());
    private final static String WINDOW_TITLE = "Launcher and InterAppBus Demo";

    private DesktopConnection desktopConnection;
    private InterApplicationBus interApplicationBus;
    private Button btnOFApp1, btnOFApp2;
    private Button btnUndock;   // button to undock this Java window
    private Button btnOFSendApp1, btnOFSendApp2;  // send messages to OpenFin app via Inter App Bus
    private static String appUuid = "LaunchManifestDemo";  // App UUID for startup app in manifest
    private final String app1Uuid = "Layout Client1";       // defined in layoutclient1.json
    private final String app2Uuid = "Layout Client2";       // defined in layoutclient2.json
    private final String appUrl = "http://localhost:8888/busdemo.html";
    com.openfin.desktop.Application app1, app2;  // OpenFin apps

    private ExternalWindowObserver externalWindowObserver;  // required for Layout service to control Java window
    private Stage stage;

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
                launchAppFromManifest("http://localhost:8888/layoutclient1.json");
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
                launchAppFromManifest("http://localhost:8888/layoutclient2.json");
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
                sendOFApp(app1Uuid);
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
                sendOFApp(app2Uuid);
            }
        });

        btnUndock = new Button();
        btnUndock.setText("Undock");
        btnUndock.setLayoutX(10);
        btnUndock.setLayoutY(170);
        btnUndock.setDisable(true);

        AnchorPane anchorPane = new AnchorPane();
        anchorPane.getChildren().add(btnOFApp1);
        anchorPane.getChildren().add(btnOFApp2);
        anchorPane.getChildren().add(btnOFSendApp1);
        anchorPane.getChildren().add(btnOFSendApp2);
        anchorPane.getChildren().add(btnUndock);

        //Creating a Group object
        Group root = new Group();

        //Retrieving the observable list object
        ObservableList list = root.getChildren();

        //Creating a scene object
        Scene scene = new Scene(anchorPane, 400, 400);

        //Setting title to the Stage
        stage.setTitle(WINDOW_TITLE);

        //Adding scene to the stage
        stage.setScene(scene);

        //Displaying the contents of the stage
        stage.show();

        this.stage = stage;
        this.stage.setResizable(false);
        this.stage.setOnCloseRequest(event -> cleanup());

        launchRuntime();
    }

    private void launchRuntime() {
        if (desktopConnection == null) {
            RuntimeConfiguration cfg = new RuntimeConfiguration();
            cfg.setRuntimeVersion("stable");
            cfg.setSecurityRealm("java-test");
            cfg.setAdditionalRuntimeArguments("--v=1 --enable-mesh ");   // --v=1  => enable verbose logging by Runtime
                                                                         // --enable-mesh  => enable multi-Runtime for the security realm
            // Add Layout Service to the manifest
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
//            layout.put("manifestUrl", "https://cdn.openfin.co/services/openfin/layouts/1.0.0/app.json");
            serviceConfig.put(0, layout);
            cfg.addConfigurationItem("services", serviceConfig);

            JSONObject startupApp = new JSONObject();
            startupApp.put("uuid", appUuid);
            startupApp.put("name", appUuid);
            startupApp.put("url", "about:blank");
            startupApp.put("autoShow", false);
            cfg.setStartupApp(startupApp);

            try {
                desktopConnection = new DesktopConnection("Java app");
                desktopConnection.connect(cfg, new DesktopStateListener() {
                    @Override
                    public void onReady() {
                        logger.info("Connected to OpenFin Runtime");
                        interApplicationBus = new InterApplicationBus(desktopConnection);
                        btnOFApp1.setDisable(false);
                        btnOFApp2.setDisable(false);
                        configAppEventListener();
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

    private void configAppEventListener() {
        try {
            // set up listener for "started" and "closed" event for layoutclient1.json
            app1 = com.openfin.desktop.Application.wrap(app1Uuid, this.desktopConnection);
            app1.addEventListener("started", new EventListener() {
                @Override
                public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                    btnOFApp1.setDisable(true);
                    btnOFSendApp1.setDisable(false);
                }
            }, null);
            app1.addEventListener("closed", new EventListener() {
                @Override
                public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                    btnOFApp1.setDisable(false);
                    btnOFSendApp1.setDisable(true);
                }
            }, null);

            // set up listener for "started and "closed" event for layoutclient2.json
            app2 = com.openfin.desktop.Application.wrap(app2Uuid, this.desktopConnection);
            app2.addEventListener("started", new EventListener() {
                @Override
                public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                    btnOFApp2.setDisable(true);
                    btnOFSendApp2.setDisable(false);
                }
            }, null);
            app2.addEventListener("closed", new EventListener() {
                @Override
                public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                    btnOFApp2.setDisable(false);
                    btnOFSendApp2.setDisable(true);
                }
            }, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Create externalWindowObserver for this Java frame so Runtime & Layout Service can keep track of location & size
     */
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
                            observer.getDesktopConnection().getChannel().connect("of-layouts-service-v1",
                                    new AsyncCallback<ChannelClient>() {
                                        @Override
                                        public void onSuccess(ChannelClient client) {
                                            btnUndock.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
                                                @Override
                                                public void handle(javafx.event.ActionEvent e) {
                                                    JSONObject payload = new JSONObject();
                                                    payload.put("uuid", appUuid);
                                                    payload.put("name", windowName);
                                                    client.dispatch("UNDOCK-WINDOW", payload, null);
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

    private void launchAppFromManifest(String manifest) {
        try {
            com.openfin.desktop.Application.createFromManifest(manifest,
                    new AsyncCallback<com.openfin.desktop.Application>() {
                        @Override
                        public void onSuccess(com.openfin.desktop.Application app) {
                            try {
                                app.run();
                                createExternalWindowObserver();
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

    private void sendOFApp(String desticationAppUuid) {
        JSONObject msg = new JSONObject();
        msg.put("ticker", "AAPL");
        msg.put("price", Math.random() * 100);
        try {
            interApplicationBus.send(desticationAppUuid, "messageFromJavaTopic", msg);
        } catch (DesktopException e) {
            e.printStackTrace();
        }
    }

    public void cleanup() {
        try {
            if (this.externalWindowObserver != null) {
                this.externalWindowObserver.dispose();
            }
            if (this.desktopConnection != null) {
                OpenFinRuntime runtime = new OpenFinRuntime(desktopConnection);
                runtime.exit();
                Thread.sleep(1000);
                java.lang.System.exit(0);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        this.externalWindowObserver = null;
    }

    public static void main(String args[]) {
        launch(args);
    }



}
