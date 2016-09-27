package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.System;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Performance test with pub/sub on InterApplicationBus
 *
 *  1. to specify version OpenFin Runtime
 *      -Dcom.openfin.demo.runtime.version=stable
 *  2. to specify frequency for publishing messages as number of messages per second
 *      -Dcom.openfin.demo.publish.frequency=1000
 *  3. to specify frequency for showing stats as number of seconds
 *      -Dcom.openfin.demo.stats.frequency=20  (show stats every 20 seconds)
 *  4. to specify size of each message
 *      -Dcom.openfin.demo.publish.size=1024
 **
 * Created by wche on 9/26/2016.
 */
public class PubSubTest {
    private static Logger logger = LoggerFactory.getLogger(PubSubTest.class.getName());

    private static String TOPIC = "Java_performance_test";

    private Publisher publisher;
    private Subscriber subscriber;
    void startPublisher() {
        this.publisher = new Publisher();
        this.publisher.launch();
    }
    void startSubscriber() {
        this.subscriber = new Subscriber();
        this.subscriber.launch();
    }

    private void startRuntime(DesktopConnection desktopConnection, DesktopStateListener listener) {
        String desktopVersion = java.lang.System.getProperty("com.openfin.demo.runtime.version");
        if (desktopVersion == null) {
            desktopVersion = "stable";
        }
        try {
            logger.info(String.format("Connecting to Runtime %s", desktopVersion));
            desktopConnection.connectToVersion(desktopVersion, listener, 60);
        } catch (Exception ex) {
            logger.error("Error launching Runtime", ex);
        }
    }
    private class Publisher implements DesktopStateListener {
        private DesktopConnection desktopConnection;
        private long publishFrequency;
        private long publishMessageSize;
        private String body;  // message body
        private Timer publishTimer;
        private long totalSent, startTime;
        private Thread statsThread;

        Publisher() {
            try {
                String value = java.lang.System.getProperty("com.openfin.demo.publish.frequency");
                if (value != null) {
                    this.publishFrequency = Long.parseLong(value);
                } else {
                    this.publishFrequency = 200;
                }
                value = java.lang.System.getProperty("com.openfin.demo.publish.size");
                if (value != null) {
                    this.publishMessageSize = Long.parseLong(value);
                } else {
                    this.publishMessageSize = 1024;
                }
                this.body = createMessageBody(this.publishMessageSize);
                publishTimer = new java.util.Timer();
                desktopConnection = new DesktopConnection(UUID.randomUUID().toString());

                this.statsThread = new Thread() {
                    public void run() {
                        long sleepTime = 10000;
                        String value = java.lang.System.getProperty("com.openfin.demo.stats.frequency");
                        if (value != null) {
                            sleepTime = Long.parseLong(value) * 1000;
                        }
                        while (true) {
                            try {
                                Thread.sleep(sleepTime);
                                if (totalSent > 0) {
                                    long rate = totalSent / ((System.currentTimeMillis() - startTime) / 1000);
                                    logger.info(String.format("Total Sent %d Rate %d", totalSent, rate));
                                }
                            } catch (InterruptedException e) {
                                logger.error("Error", e);
                            }
                        }
                    }
                };
                this.statsThread.setDaemon(false); // keep running
                this.statsThread.start();

            }catch (Exception ex) {
                logger.error("Error creating publisher", ex);
            }
        }
        void launch() {
            startRuntime(this.desktopConnection, this);
        }

        private String createMessageBody(long size) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < size; i++) {
                sb.append("A");
            }
            return sb.toString();
        }
        @Override
        public void onReady() {
            logger.info(String.format("Starting publish timer with frequency %d and message size %d", this.publishFrequency, this.publishMessageSize));
            startTime = 0;
            publishTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (startTime == 0) {
                        startTime = System.currentTimeMillis();
                    }
                    for (int i = 0; i < publishFrequency; i++) {
                        JSONObject msg = new JSONObject();
                        msg.put("body", body);
                        try {
                            desktopConnection.getInterApplicationBus().publish(TOPIC, msg);
                            totalSent++;
                        } catch (DesktopException e) {
                            logger.error("Error publishing messages");
                        }
                    }
                }
            }, 1000, 1000);
        }

        @Override
        public void onClose() {
            logger.info("onClose");
        }

        @Override
        public void onError(String reason) {
            logger.error(String.format("onError %s", reason));
        }

        @Override
        public void onMessage(String message) {
        }

        @Override
        public void onOutgoingMessage(String message) {
        }
    }

    private class Subscriber implements DesktopStateListener{
        private DesktopConnection desktopConnection;
        private long totalReceived, startTime;
        private Thread statsThread;

        protected Subscriber() {
            try {
                desktopConnection = new DesktopConnection(UUID.randomUUID().toString());
                this.statsThread = new Thread() {
                    public void run() {
                        long sleepTime = 10000;
                        String value = java.lang.System.getProperty("com.openfin.demo.stats.frequency");
                        if (value != null) {
                            sleepTime = Long.parseLong(value) * 1000;
                        }
                        while (true) {
                            try {
                                Thread.sleep(sleepTime);
                                if (totalReceived > 0) {
                                    long rate = totalReceived / ((System.currentTimeMillis() - startTime) / 1000);
                                    logger.info(String.format("Total Received %d Rate %d", totalReceived, rate));
                                } else {
                                    logger.info("Waiting for messages");
                                }
                            } catch (InterruptedException e) {
                                logger.error("Error", e);
                            }
                        }
                    }
                };
                this.statsThread.setDaemon(false);
                this.statsThread.start();

            }catch (Exception ex) {
                logger.error("Error creating subscriber", ex);
            }
        }

        @Override
        public void onReady() {
            try {
                logger.info("Connected to Runtime, waiting for messages");
                this.desktopConnection.getInterApplicationBus().subscribe("*", TOPIC, (sourceUuid, receivingTopic, payload) -> {
                    totalReceived++;
                    if (startTime == 0) {
                        startTime = System.currentTimeMillis();
                        logger.info(String.format("First message received with length %d", payload.toString().length()));
                    }
                });
            } catch (Exception e) {
                logger.error("Error subscribing", e);
            }
        }

        void launch() {
            startRuntime(this.desktopConnection, this);
        }

        @Override
        public void onClose() {

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
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        PubSubTest test = new PubSubTest();
        if (args[0].equals("publisher")) {
            test.startPublisher();
        }
        else if (args[0].equals("subscriber")) {
            test.startSubscriber();
        }
    }

}
