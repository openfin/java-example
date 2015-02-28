package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import org.json.JSONObject;

import java.lang.System;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * InterAppBus performance demo.  This class can run as message publblisher or subscriber.  See comments
 * for main for more info.
 *
 * On my Mac with Parallels, it gets the following numbers (msg/sec) with steady message flow:
 *
 * Pub-Sub:
 * java-java: 4500
 * java-js:   4100
 * js-java:   4100
 *
 *
 *
 * Created by wche on 2/28/15.
 */
public class PerformanceDemo {

    private DesktopConnection controller;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private long receiveCount = -1, receiveStartTime = -1;
    private static String TOPIC = "InterBusStressTest";
    private String myAppUUID;

    public PerformanceDemo(final Integer port, final String role, final Integer messageRate, Boolean msgLog) throws Exception {
        this.myAppUUID = role + "Java";
        this.controller = new DesktopConnection(this.myAppUUID, "localhost", port);
        if (msgLog != null) {
            this.controller.setLogLevel(msgLog);
        }
        final DesktopStateListener listener = new DesktopStateListener() {
            @Override
            public void onReady() {
                InterApplicationBus bus = controller.getInterApplicationBus();
                bus.addSubscribeListener(new SubscriptionListener() {
                    @Override
                    public void unsubscribed(String arg0, String arg1) {
                    }
                    @Override
                    public void subscribed(String uuid, String topic) {
                        System.out.printf("subscribed " + topic + " from " + uuid);
                    }
                });

                if ("publish".equals(role)) {
                    startPublish(messageRate);
                }
                else if ("subscribe".equals(role)) {
                    startSubscribe();
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
        controller.connect(listener);
    }

    private void startPublish(Integer messageRate) {
        final int rate = (messageRate == null ? 100 : messageRate);
        System.out.printf("Start publishing at rate of " + messageRate);
        final Runnable runnable = new Runnable() {
            public void run() {
                try {
                    long startTime = System.currentTimeMillis();
                    InterApplicationBus bus = controller.getInterApplicationBus();
                    int count = 0;
                    while (count < rate) {
                        JSONObject msg = new JSONObject();
                        msg.put("timestamp", System.currentTimeMillis());
                        bus.publish(TOPIC, msg);
                        count++;
                    }
                    long elapse = (System.currentTimeMillis() - startTime) / 1000;
                    if (elapse < 1) {
                        elapse = 1;
                    }
                    int avg = Math.round(count / elapse);
                    System.out.println("Actual Message Rate " + avg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        scheduler.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.SECONDS);
    }

    private void startSubscribe() {
        System.out.println("Start subscribing " + TOPIC);
        final Runnable runnable = new Runnable() {
            public void run() {
                displaySubscribeStats();
            }
        };
        scheduler.scheduleAtFixedRate(runnable, 1, 1, TimeUnit.SECONDS);

        try {
            InterApplicationBus.subscribe("*", TOPIC, new BusListener() {
                @Override
                public void onMessageReceived(String sourceUuid, String topic, Object payload) {
                    if (receiveCount < 0) {
                        receiveCount = 0;
                        receiveStartTime = System.currentTimeMillis();
                    }
                    receiveCount++;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displaySubscribeStats() {
        try {
            int elapse = (int) ((System.currentTimeMillis() - receiveStartTime) /1000);
            if (elapse == 0) {
                elapse = 1;
            }
            int avg = Math.round(receiveCount / elapse);
            System.out.println("Actual Message Rate " + avg);
            receiveCount = 0;
            receiveStartTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeApp() throws Exception {
        Application app = Application.wrap("application2", this.controller);
        app.close();
    }

    /**
     *
     * Assuming OpenFin is already running,
     *
     * To run publisher
     *                  -DOpenFinPort=9696 -DOpenFinRole=publish -DMessageRate=2000
     *
     * To run subscriber
     *                  -DOpenFinPort=9696 -DOpenFinRole=subscribe
     *
     * To enable message logging, pass -DMessageLog=true
     *
     * @param args command line args
     */
    public static void main(String[] args) throws Exception {
        final String role = java.lang.System.getProperty("OpenFinRole");
        String portStr = java.lang.System.getProperty("OpenFinPort");
        final Integer port, rate;
        if (portStr != null) {
            port = Integer.parseInt(portStr);
        } else {
            port = null;
        }
        String rateStr = java.lang.System.getProperty("MessageRate");
        if (rateStr != null) {
            rate = Integer.parseInt(rateStr);
        } else {
            rate = null;
        }
        String logStr = java.lang.System.getProperty("MessageLog");
        Boolean msgLog = false;
        if (logStr != null) {
            msgLog = Boolean.parseBoolean(logStr);
        }

        PerformanceDemo demo = new PerformanceDemo(port, role, rate, msgLog);

//        Thread.sleep(5*1000);
//        demo.closeApp();


        while (true) {
            Thread.sleep(10*1000);
        }
    }

}
