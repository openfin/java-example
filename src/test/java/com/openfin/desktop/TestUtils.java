package com.openfin.desktop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

/**
 *
 * Created by wche on 1/23/16.
 *
 */
public class TestUtils {
    private static Logger logger = LoggerFactory.getLogger(TestUtils.class.getName());
    private static boolean connectionClosing;
    private static String runtimeVersion;
    private static CountDownLatch disconnectedLatch;
    public static final String openfin_app_url = "http://test.openf.in/test.html";  // simple test app

    public static DesktopConnection setupConnection(String connectionUuid) throws Exception {
        logger.debug("starting");
        CountDownLatch connectedLatch = new CountDownLatch(1);
        disconnectedLatch = new CountDownLatch(1);
        // if RVM needs to download the version of Runtime specified, waitTime may need to be increased for slow download
        int waitTime = 60;
        String swaiTime = java.lang.System.getProperty("com.openfin.test.runtime.connect.wait.time");
        if (swaiTime != null) {
            waitTime = Integer.parseInt(swaiTime);
        }

        DesktopConnection desktopConnection = new DesktopConnection(connectionUuid);
        desktopConnection.setAdditionalRuntimeArguments(" --v=1 ");  // turn on Chromium debug log
        runtimeVersion = java.lang.System.getProperty("com.openfin.test.runtime.version");
        if (runtimeVersion == null) {
            runtimeVersion = "alpha";
        }
        desktopConnection.connectToVersion(runtimeVersion, new DesktopStateListener() {
            @Override
            public void onReady() {
                logger.info("Connected to OpenFin runtime");
                connectionClosing = false;
                connectedLatch.countDown();
            }

            @Override
            public void onError(String reason) {
                if (!connectionClosing) {
                    logger.error("Connection failed: %s", reason);
                } else {
                    logger.debug("Connection closed");
                }
                disconnectedLatch.countDown();
            }

            @Override
            public void onMessage(String message) {
                logger.debug("openfin message: %s", message);
            }

            @Override
            public void onOutgoingMessage(String message) {
                logger.debug("openfin outgoing message: %s", message);
            }
        }, waitTime);//this timeout (in 4.40.2.9) is ignored

        logger.debug("waiting for desktop to connect");
        connectedLatch.await(waitTime, TimeUnit.SECONDS);
        if (desktopConnection.isConnected()) {
            logger.debug("desktop connected");
        } else {
            throw new RuntimeException("failed to initialise desktop connection");
        }
        return desktopConnection;
    }

    public static void teardownDesktopConnection(DesktopConnection desktopConnection) throws Exception {
        if (desktopConnection.isConnected()) {
            connectionClosing = true;
            new com.openfin.desktop.System(desktopConnection).exit();
            disconnectedLatch.await(20, TimeUnit.SECONDS);
            assertFalse(desktopConnection.isConnected());
            logger.debug("desktop connection closed");
        } else {
            logger.info("Not connected, no need to teardown");
        }
    }

    public static String getRuntimeVersion() {
        return runtimeVersion;
    }

    public static ApplicationOptions getAppOptions() {
        return getAppOptions(UUID.randomUUID().toString());
    }
    public static ApplicationOptions getAppOptions(String uuid) {
        ApplicationOptions options = new ApplicationOptions(uuid, uuid, openfin_app_url);
        WindowOptions windowOptions = new WindowOptions();
        windowOptions.setAutoShow(true);
        windowOptions.setSaveWindowState(false);
        windowOptions.setDefaultTop(100);
        windowOptions.setDefaultLeft(100);
        windowOptions.setDefaultHeight(200);
        windowOptions.setDefaultWidth(200);
        options.setMainWindowOptions(windowOptions);
        return options;
    }

    public static Application createApplication(ApplicationOptions options, DesktopConnection desktopConnection) throws Exception {
        CountDownLatch createLatch = new CountDownLatch(1);
        Application application = new Application(options, desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                createLatch.countDown();
            }

            @Override
            public void onError(Ack ack) {
                logger.error("Error creating application", ack.getReason());
            }
        });
        createLatch.await(5, TimeUnit.SECONDS);
        assertEquals("Create application timeout " + options.getUUID(), createLatch.getCount(), 0);
        return application;
    }

    public static Application runApplication(ApplicationOptions options, DesktopConnection desktopConnection) throws Exception {
        Application application = createApplication(options, desktopConnection);
        runApplication(application, true);
        return application;
    }

    /**
     * Run an application.
     *
     * @param application
     * @param checkAppConnected true if it needs to wait for app-connected event
     * @throws Exception
     */
    public static void runApplication(Application application, boolean checkAppConnected) throws Exception {
        CountDownLatch startedLatch  = new CountDownLatch(1);
        CountDownLatch connectedLatch = new CountDownLatch(1);
        EventListener listener = new EventListener() {
            @Override
            public void eventReceived(ActionEvent actionEvent) {
                if (actionEvent.getType().equals("started")) {
                    startedLatch.countDown();
                }
                else if (actionEvent.getType().equals("app-connected")) {
                    connectedLatch.countDown();
                }
            }
        };
        addEventListener(application, "started", listener);
        if (checkAppConnected) {
            addEventListener(application.getWindow(), "app-connected", listener);
        }

        CountDownLatch runLatch = new CountDownLatch(1);
        application.run(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                runLatch.countDown();
            }

            @Override
            public void onError(Ack ack) {
                logger.error("Error running application", ack.getReason());
            }
        });
        runLatch.await(5, TimeUnit.SECONDS);
        assertEquals("Run application timeout " + application.getUuid(), runLatch.getCount(), 0);
        assertEquals("Start application timeout " + application.getUuid(), startedLatch.getCount(), 0);
        if (checkAppConnected) {
            connectedLatch.await(5, TimeUnit.SECONDS);
            assertEquals(connectedLatch.getCount(), 0);
        }
    }

    public static void addEventListener(Application application, String evenType, EventListener eventListener) throws Exception {
        logger.debug("addEventListener " + evenType);
        CountDownLatch latch = new CountDownLatch(1);
        application.addEventListener(evenType, eventListener, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                latch.countDown();
                logger.debug("addEventListener ack " + ack.isSuccessful());
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("Error adding event listener %s %s", evenType, ack.getReason()));
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("addEventListener timeout " + evenType, latch.getCount(), 0);
    }

    public static void closeApplication(Application application) throws Exception {
        logger.debug("getApplicationManifest");
        CountDownLatch stoppedLatch = new CountDownLatch(1);
        EventListener listener = new EventListener() {
            @Override
            public void eventReceived(ActionEvent actionEvent) {
                if (actionEvent.getType().equals("closed")) {
                    stoppedLatch.countDown();
                }
            }
        };
        addEventListener(application, "closed", listener);

        stoppedLatch.await(5, TimeUnit.SECONDS);
        application.close();
        stoppedLatch.await(5, TimeUnit.SECONDS);
        assertEquals("Close application timeout " + application.getUuid(), stoppedLatch.getCount(), 0);
    }

    public static WindowOptions getWindowOptions(String name, String url) throws Exception {
        WindowOptions options = new WindowOptions(name, url);
        options.setDefaultWidth(200);
        options.setDefaultHeight(200);
        options.setDefaultTop(200);
        options.setDefaultLeft(200);
        options.setSaveWindowState(false);  // so the window opens with the same default bounds every time
        options.setAutoShow(true);
        options.setFrame(true);
        options.setResizable(true);
        return options;
    }

    public static Window createChildWindow(Application application, WindowOptions childOptions, DesktopConnection desktopConnection) throws Exception {

        final CountDownLatch windowCreatedLatch = new CountDownLatch(1);
        // use window-end-load event to wait for the window to finish loading
        addEventListener(application, "window-end-load", actionEvent -> {
            logger.info(actionEvent.getEventObject().toString());
            if (actionEvent.getEventObject().has("name")) {
                if (childOptions.getName().equals(actionEvent.getEventObject().getString("name"))) {
                    windowCreatedLatch.countDown();
                }
            }
        });
        application.createChildWindow(childOptions, null);
        windowCreatedLatch.await(10, TimeUnit.SECONDS);
        assertEquals("createChildWindow timeout", windowCreatedLatch.getCount(), 0);
        return Window.wrap(application.getOptions().getUUID(), childOptions.getName(), desktopConnection);
    }

    public static void addEventListener(Window window, String evenType, EventListener eventListener) throws Exception {
        logger.debug("addEventListener " + evenType);
        CountDownLatch latch = new CountDownLatch(1);
        window.addEventListener(evenType, eventListener, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                latch.countDown();
                logger.debug("addEventListener ack " + ack.isSuccessful());
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("Error adding event listener %s %s", evenType, ack.getReason()));
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("addEventListener timeout " + evenType, latch.getCount(), 0);
    }

    public static WindowBounds getBounds(Window window) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<WindowBounds> windowBoundsAtomicReference = new AtomicReference<>();
        window.getBounds(result -> {
            windowBoundsAtomicReference.set(result);
            latch.countDown();
        }, null);
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getBounds timeout", latch.getCount(), 0);
        return windowBoundsAtomicReference.get();
    }

}
