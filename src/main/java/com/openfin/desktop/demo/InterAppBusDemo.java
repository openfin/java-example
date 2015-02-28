package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import java.lang.System;

/**
 *
 * Demo for InterAppBus with javascript app:
 *
 *
 <script>
 function initApp() {
 fin.desktop.main(function () {
 console.log("main callback ");
 var initialized = false;
 fin.desktop.InterApplicationBus.subscribe("application1", "initialize-communication", function (message, senderUUID) {
 if (!initialized) {
 console.log("subscribed");
 initialized = true;
 fin.desktop.InterApplicationBus.send("application1", "initialize-communication", "Hello from Javascript");
 setTimeout(onCommunicationInitialized, 5000);
 }
 console.log("receiving msg from ", senderUUID, " msg ", message);
 });

 fin.desktop.InterApplicationBus.addSubscribeListener(function (uuid, topic) {
 console.log("The application " + uuid + " has subscribed to " + topic);
 alert("The application " + uuid + " has subscribed to " + topic);
 });

 function onCommunicationInitialized() {
 fin.desktop.InterApplicationBus.send("application1", "initialize-communication", "Hello from Javascript");
 }
 });
 }

 function checkMain() {
 if (fin.desktop && fin.desktop.main) {
 console.log("main is good");
 initApp();
 } else {
 console.log("main is not good");
 setTimeout(checkMain, 100);
 }
 }
 checkMain();
 *
 *
 * Created by richard on 2/28/15.
 */
public class InterAppBusDemo {

    private DesktopConnection controller;

    public InterAppBusDemo(final String desktop_path, final String desktopCommandLine, final Integer port) throws Exception {
        this.controller = new DesktopConnection("application1", "localhost", port);

        final DesktopStateListener listener = new DesktopStateListener() {
            @Override
            public void onReady() {
                System.out.println("onReady");
                InterApplicationBus bus = controller.getInterApplicationBus();
                bus.addSubscribeListener(new SubscriptionListener() {
                    @Override
                    public void unsubscribed(String arg0, String arg1) {
                    }

                    @Override
                    public void subscribed(String uuid, String topic) {
//                              public void subscribed(String arg0, String arg1) {
                        try {
                            System.out.println("subscribed " + uuid);
                            InterApplicationBus.send(uuid, "initialize-communication", "You are subscribed");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                try {
                    InterApplicationBus.subscribe("*", "initialize-communication", new BusListener() {
                        @Override
                        public void onMessageReceived(String sourceUuid, String topic, Object payload) {
                            //                              public void onMessageReceived(String arg0, String arg1, Object arg2) {
                            if (!"application1".equals(sourceUuid)) {
                                System.out.println("###### onMessageReceived: " + sourceUuid + " : " + topic + " : " + payload.toString());
                                try {
//                                          InterApplicationBus.send(sourceUuid, "initialize-communication", "Hello from Java");
                                    InterApplicationBus.publish("initialize-communication", "Hello from Java");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            @Override
            public void onError(String reason) {
                System.out.println("Connection failed: " + reason);
            }

            @Override
            public void onMessage(String message) {
            }

            @Override
            public void onOutgoingMessage(String message) {
            }
        };

        if (desktop_path != null) {
            controller.launchAndConnect(desktop_path, desktopCommandLine, listener, 10000);
        } else {
            controller.connect(listener);
        }
    }

    public void closeDesktop() {
        if (controller != null && controller.isConnected()) {
            try {
                new com.openfin.desktop.System(controller).exit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * To start OpenFin Desktop and Connect, pass full path of OpenFin with
     *
     *    -DOpenFinPath="mypath\OpenFinRVM.exe" -DOpenFinPort=9696 -DOpenFinOption=--config=\"RemoteConfigUrl\"
     *
     * If OpenFin is already running, pass port number with
     *                  -DOpenFinPort=9696
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        final String desktop_path = java.lang.System.getProperty("OpenFinPath");
        final String desktop_option = java.lang.System.getProperty("OpenFinOption");
        String portStr = java.lang.System.getProperty("OpenFinPort");
        final Integer port;
        if (portStr != null) {
            port = Integer.parseInt(portStr);
        } else {
            port = null;
        }
        InterAppBusDemo demo = new InterAppBusDemo(desktop_path, desktop_option, port);
//        Thread.sleep(20*1000);
//        demo.closeDesktop();
    }


}
