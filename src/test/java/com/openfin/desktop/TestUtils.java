package com.openfin.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utitlites for Junit test code
 *
 * Test cases in this class need to have access to an OpenFin HTML5 app to verify sub/pub workflow.  Sources for the app can be found in release
 * directory: SimpleOpenFinApp.html.  It is hosted by OpenFin at https://cdn.openfin.co/examples/junit/SimpleOpenFinApp.html
 *
 * Created by wche on 1/23/16.
 *
 */
public class TestUtils {
    private static Logger logger = LoggerFactory.getLogger(TestUtils.class.getName());
    private static boolean connectionClosing;
    private static String runtimeVersion;
    private static CountDownLatch disconnectedLatch;
    public static final String openfin_app_url = "https://cdn.openfin.co/examples/junit/SimpleOpenFinApp.html";  // source is in release/SimpleOpenFinApp.html
    public static final String icon_url = "http://demoappdirectory.openf.in/desktop/config/apps/OpenFin/HelloOpenFin/img/openfin.ico";

    static  {
        runtimeVersion = java.lang.System.getProperty("com.openfin.test.runtime.version");
        if (runtimeVersion == null) {
            runtimeVersion = "alpha";
        }
        logger.debug(String.format("Runtime version %s", runtimeVersion));
    }

    public static DesktopConnection setupConnection(String connectionUuid) throws Exception {
        return setupConnection(connectionUuid, null, null);
    }
    public static DesktopConnection setupConnection(String connectionUuid, RuntimeConfiguration configuration) throws Exception {
        logger.debug("starting from Runtime configuration");
        CountDownLatch connectedLatch = new CountDownLatch(1);
        disconnectedLatch = new CountDownLatch(1);
        // if RVM needs to download the version of Runtime specified, waitTime may need to be increased for slow download
        int waitTime = 60;
        String swaiTime = java.lang.System.getProperty("com.openfin.test.runtime.connect.wait.time");
        if (swaiTime != null) {
            waitTime = Integer.parseInt(swaiTime);
        }

        DesktopConnection desktopConnection = new DesktopConnection(connectionUuid);
        desktopConnection.setAdditionalRuntimeArguments(" --v=1 --no-sandbox ");  // turn on Chromium debug log
        desktopConnection.connect(configuration, new DesktopStateListener() {
            @Override
            public void onReady() {
                logger.info("Connected to OpenFin runtime");
                connectionClosing = false;
                connectedLatch.countDown();
            }
            @Override
            public void onClose() {
                logger.debug("Connection closed");
                disconnectedLatch.countDown();
            }

            @Override
            public void onError(String reason) {
                if (!connectionClosing) {
                    logger.error(String.format("Connection failed: %s", reason));
                } else {
                    logger.debug("Connection closed");
                }
            }

            @Override
            public void onMessage(String message) {
                logger.debug(String.format("Runtime incoming message: %s", message));
            }

            @Override
            public void onOutgoingMessage(String message) {
                logger.debug(String.format("Runtime outgoing message: %s", message));
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

    public static DesktopConnection setupConnection(String connectionUuid, String rdmUrl, String assetsUrl) throws Exception {
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
        RuntimeConfiguration configuration = new RuntimeConfiguration();
        configuration.setRuntimeVersion(runtimeVersion);
        configuration.setAdditionalRuntimeArguments(" --v=1 --no-sandbox --enable-crash-reporting ");  // turn on Chromium debug log
        configuration.setDevToolsPort(9090);
        configuration.setRdmURL(rdmUrl);
        configuration.setRuntimeAssetURL(assetsUrl);
        configuration.setLicenseKey("JavaAdapterJUnitTests");
        desktopConnection.connect(configuration, new DesktopStateListener() {
            @Override
            public void onReady() {
                logger.info("Connected to OpenFin runtime");
                connectionClosing = false;
                connectedLatch.countDown();
            }
            @Override
            public void onClose() {
                logger.debug("Connection closed");
                disconnectedLatch.countDown();
                connectedLatch.countDown();  // interrupt connectedLatch.await
            }

            @Override
            public void onError(String reason) {
                if (!connectionClosing) {
                    logger.error(String.format("Connection failed: %s", reason));
                } else {
                    logger.debug("Connection closed");
                }
            }

            @Override
            public void onMessage(String message) {
                logger.debug(String.format("Runtime incoming message: %s", message));
            }

            @Override
            public void onOutgoingMessage(String message) {
                logger.debug(String.format("Runtime outgoing message: %s", message));
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
        if (desktopConnection != null && desktopConnection.isConnected()) {
            connectionClosing = true;
            disconnectedLatch = new CountDownLatch(1);
            new OpenFinRuntime(desktopConnection).exit();
            logger.debug("waiting for desktop connection teardown");
            disconnectedLatch.await(20, TimeUnit.SECONDS);
            assertFalse(desktopConnection.isConnected());
            Thread.sleep(5000); //"Workaround for a RVM issue with creating Window class");
            logger.debug("desktop connection closed");
        } else {
            logger.debug("Not connected, no need to teardown");
        }
    }

    public static String getRuntimeVersion() {
        return runtimeVersion;
    }

    public static ApplicationOptions getAppOptions(String url) {
        return getAppOptions(UUID.randomUUID().toString(), url);
    }
    public static ApplicationOptions getAppOptions(String uuid, String url) {
        ApplicationOptions options = new ApplicationOptions(uuid, uuid, url == null?openfin_app_url:url);
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
        logger.debug("closeApplication");
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

        application.close(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
            }
            @Override
            public void onError(Ack ack) {
                logger.error(ack.getReason());
            }
        });
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
            logger.debug(actionEvent.getEventObject().toString());
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

    public static void moveWindowBy(Window window, int deltaLeft, int deltaTop) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        window.moveBy(deltaLeft, deltaTop, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    latch.countDown();
                }
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("onError %s", ack.getReason()));
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(latch.getCount(), 0);
    }

    /**
     * Request Runtiem to clear all caches.  Runtime needs to be restarted in order for the caches to be actually cleared
     *
     * @param desktopConnection
     * @throws Exception
     */
    public static void clearAllCaches(DesktopConnection desktopConnection) throws Exception {
        OpenFinRuntime runtime = new OpenFinRuntime(desktopConnection);
        CountDownLatch latch = new CountDownLatch(1);
        runtime.clearCache(true, true, true, true, true, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    latch.countDown();
                }
            }
            @Override
            public void onError(Ack ack) {
                logger.error(ack.getReason());
            }
        });
        latch.await(5, TimeUnit.SECONDS);
    }


    public static WindowBounds getAvailableRect(DesktopConnection desktopConnection) throws Exception {
        AtomicReference<WindowBounds> windowBoundsAtomicReference = new AtomicReference<>();
        OpenFinRuntime runtime = new OpenFinRuntime(desktopConnection);
        CountDownLatch latch = new CountDownLatch(1);
        runtime.getMonitorInfo(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    JSONObject data = (JSONObject) ack.getData();
                    if (data.has("primaryMonitor")) {
                        JSONObject primaryMonitor = data.getJSONObject("primaryMonitor");
                        if (primaryMonitor.has("availableRect")) {
                            JSONObject availableRect = primaryMonitor.getJSONObject("availableRect");
                            WindowBounds bounds = new WindowBounds(availableRect.getInt("top"), availableRect.getInt("left"),
                                                            availableRect.getInt("right"), availableRect.getInt("bottom"));
                            windowBoundsAtomicReference.set(bounds);
                        }
                        latch.countDown();
                    }
                }
            }
            @Override
            public void onError(Ack ack) {
                logger.error("Error creating application", ack.getReason());
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getMonitorInfo timeout", latch.getCount(), 0);
        return windowBoundsAtomicReference.get();
    }
}
