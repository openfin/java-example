package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.channel.ChannelClient;
import com.openfin.desktop.win32.ExternalWindowObserver;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.json.JSONObject;

import java.lang.System;
import java.util.List;

public class FxLayoutFrame {
    private ExternalWindowObserver externalWindowObserver;
    private String windowName;
    private Stage stage;
    private static JFXPanel jFXPanel;

    public FxLayoutFrame(DesktopConnection desktopConnection, String appUuid, String windowName) {
        System.out.println(windowName + " being created ");
        this.windowName = windowName;
        if (jFXPanel == null) {
          jFXPanel = new JFXPanel();
            javafx.application.Platform.setImplicitExit(false);
        }
        javafx.application.Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Button btnUndock = new Button("undock");
                btnUndock.setDisable(true);

                StackPane secondaryLayout = new StackPane();
                secondaryLayout.getChildren().add(btnUndock);

                Scene secondScene = new Scene(secondaryLayout, 640, 480);

                FxLayoutFrame.this.stage = new Stage();
                FxLayoutFrame.this.stage.setTitle(windowName);
                FxLayoutFrame.this.stage.setScene(secondScene);

                // Set position of second window, related to primary window.
                FxLayoutFrame.this.stage.setX(640);
                FxLayoutFrame.this.stage.setY(480);
                FxLayoutFrame.this.stage.show();

//                FxLayoutFrame.this.stage.setOnCloseRequest(event -> FxLayoutFrame.this.cleanup());

                try {
                    FxLayoutFrame.this.externalWindowObserver =
                        new ExternalWindowObserver(desktopConnection.getPort(), appUuid, windowName, FxLayoutFrame.this.stage,
                            new AckListener() {
                                @Override
                                public void onSuccess(Ack ack) {
                                    ExternalWindowObserver observer = (ExternalWindowObserver) ack.getSource();
                                    observer.getDesktopConnection().getChannel().connect("of-layouts-service-v1").thenAccept(channelClient -> {
                                        btnUndock.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
                                            @Override
                                            public void handle(javafx.event.ActionEvent e) {
                                                JSONObject payload = new JSONObject();
                                                payload.put("uuid", appUuid);
                                                payload.put("name", windowName);
                                                channelClient.dispatch("UNDOCK-WINDOW", payload);
                                            }
                                        });
                                    });
                                }
                                @Override
                                public void onError(Ack ack) {
                                }
                            });

                    Window w = Window.wrap(appUuid, windowName, desktopConnection);
                    w.addEventListener("group-changed", new EventListener() {
                        @Override
                        public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                            JSONObject eventObj = actionEvent.getEventObject();
                            w.getGroup(new AsyncCallback<List<Window>>() {
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
                        FxLayoutFrame.this.externalWindowObserver.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } catch (DesktopException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public String getWindowName() {
        return windowName;
    }

    public Stage getStage() {
        return stage;
    }

    public void cleanup() {
        try {
            System.out.println(windowName + " cleaning up ");
            if (this.externalWindowObserver != null) {
                this.externalWindowObserver.dispose();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        this.externalWindowObserver = null;
    }

}
