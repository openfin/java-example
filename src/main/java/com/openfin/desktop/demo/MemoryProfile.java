package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 *  This class can be used to profile memory usage of Java Adapter for communicating with Runtime.  The initial version
 *  just keeps calling getMachineId API.  It can easily extended to test other APIs.
 *
 *  VisualVM, available from https://visualvm.github.io/, can be used to monitor memory usgage while this code is running
 */

public class MemoryProfile {
    private final static Logger logger = LoggerFactory.getLogger(MemoryProfile.class.getName());

    public static void main(String[] args) {
        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration();
        String connectionUuid = MemoryProfile.class.getName();
        String desktopVersion = java.lang.System.getProperty("com.openfin.demo.version");
        if (desktopVersion == null) {
            desktopVersion = "stable";
        }
        runtimeConfiguration.setRuntimeVersion(desktopVersion);
        try {
            final DesktopConnection desktopConnection = new DesktopConnection(connectionUuid);
            DesktopStateListener listener = new DesktopStateListener() {
                @Override
                public void onReady() {
                    launchThread(desktopConnection);
                }
                @Override
                public void onClose(String error) {

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
            };
            desktopConnection.connect(runtimeConfiguration, listener, 50);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    private static void launchThread(DesktopConnection desktopConnection) {
        Thread t = new Thread() {
            @Override
            public void run() {
                OpenFinRuntime openfinSystem = new OpenFinRuntime(desktopConnection);
                AtomicInteger callCount = new AtomicInteger();
                AtomicBoolean shouldRun = new AtomicBoolean(true);
                while (shouldRun.get()) {
                    try {
                        CountDownLatch latch = new CountDownLatch(1);
                        openfinSystem.getMachineId(new AckListener() {
                            @Override
                            public void onSuccess(Ack ack) {
                                if (ack.isSuccessful()) {
                                    logger.info(String.format("API call count %d", callCount.incrementAndGet()));
                                    latch.countDown();
                                } else {
                                    logger.error(String.format("API failed %s", ack.getReason()));
                                    shouldRun.set(false);
                                }
                            }
                            @Override
                            public void onError(Ack ack) {
                                logger.error(String.format("API failed %s", ack.getReason()));
                                shouldRun.set(false);
                            }
                        });
                        latch.await(1, TimeUnit.SECONDS);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        t.start();
    }
}
