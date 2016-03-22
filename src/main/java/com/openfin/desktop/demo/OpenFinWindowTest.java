package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.System;
import com.openfin.desktop.animation.AnimationTransitions;
import com.openfin.desktop.animation.PositionTransition;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.*;
import static junit.framework.TestCase.fail;

/**
 * Created by wche on 1/20/16.
 *
 */
public class OpenFinWindowTest {
    private static Logger logger = LoggerFactory.getLogger(JUnitDemo.class.getName());

    private static DesktopConnection desktopConnection;

    private static final String DESKTOP_UUID = "DESKTOP_UUID";

    private static int windowUuidCounter = 0;

    //set this to around 2000ms in order to see the chromium windows
    private final long SLEEP_FOR_HUMAN_OBSERVATION = 3000L;

    private static CountDownLatch openFinDisconnectedLatch = new CountDownLatch(1);

    @BeforeClass
    public static void setup() throws Exception {
        setupDesktopConnection();
    }

    @AfterClass
    public static void teardown() throws Exception {
        printf("teardown");
        if (desktopConnection.isConnected()) {
            teardownDesktopConnection();
        } else {
            printf("Not connected, no need to teardown");
        }
    }

    private static void setupDesktopConnection() throws Exception {
        CountDownLatch openFinConnectedLatch = new CountDownLatch(1);
        // if RVM needs to download the version of Runtime specified, waitTime may need to be increased for slow download
        int waitTime = 60;
        String swaiTime = java.lang.System.getProperty("com.openfin.demo.runtime.connect.wait.time");
        if (swaiTime != null) {
            waitTime = Integer.parseInt(swaiTime);
        }

        desktopConnection = null;
        desktopConnection = new DesktopConnection(DESKTOP_UUID);
        desktopConnection.setAdditionalRuntimeArguments(" --v=1 ");  // turn on Chromium debug log
        String desktopVersion = java.lang.System.getProperty("com.openfin.demo.runtime.version");
        if (desktopVersion == null) {
            desktopVersion = "5.44.10.26";  // 5.44.10.26 has fix for cross-app docking, which is required for windowsInShameGroupMoveTogether
        }
        desktopConnection.connectToVersion(desktopVersion, new DesktopStateListener() {
            @Override
            public void onReady() {
                printf("Connected to OpenFin runtime");
                openFinConnectedLatch.countDown();
                openFinDisconnectedLatch = new CountDownLatch(1);
            }

            @Override
            public void onClose() {
                printf("Connection to Runtime is closed");
            }

            @Override
            public void onError(String reason) {
                printf("Connection failed: %s", reason);
                openFinDisconnectedLatch.countDown();
            }

            @Override
            public void onMessage(String message) {
                printf("openfin message: %s", message);
            }

            @Override
            public void onOutgoingMessage(String message) {
                printf("openfin outgoing message: %s", message);
            }
        }, waitTime);//this timeout (in 4.40.2.9) is ignored.  Not anymore in 5.44.2.2

        printf("waiting for desktop to connect");
        openFinConnectedLatch.await(waitTime, TimeUnit.SECONDS);

        if (desktopConnection.isConnected()) {
            printf("desktop connected");
        } else {
            throw new RuntimeException("failed to initialise desktop connection");
        }
    }

    private static void teardownDesktopConnection() throws Exception {
        printf("teardownDesktopConnection");
        new System(desktopConnection).exit();
        // OpenFin update: wait for websocket to be disconnected so re-launch of OpenFin Runtime will work
        openFinDisconnectedLatch.await(20, TimeUnit.SECONDS);
        assertFalse(desktopConnection.isConnected());
        printf("desktop connection closed");
    }

    private Application openWindow(String uuid, String url) throws Exception {
        //default options for all test windows
        int top = 10;
        int left = 10;
        int width = 200;
        int height = 300;
        boolean withFrame = true;
        boolean resizable = true;
        return openWindow(uuid, url, left, top, width, height, withFrame, resizable);
    }

    private Application openWindow(String uuid, String url, int left, int top, int width, int height, boolean withFrame, boolean resizable) throws Exception {
        final WindowOptions windowOptions = new WindowOptions();
        windowOptions.setAutoShow(true);
        windowOptions.setDefaultLeft(left);
        windowOptions.setDefaultTop(top);
        windowOptions.setDefaultHeight(height);
        windowOptions.setDefaultWidth(width);
        windowOptions.setFrame(withFrame);
        windowOptions.setResizable(resizable);

        ApplicationOptions applicationOptions = new ApplicationOptions(uuid, uuid, url);
        applicationOptions.setMainWindowOptions(windowOptions);

        //used to block JUnit thread until OpenFin has iniitialised
        CountDownLatch windowCreatedLatch = new CountDownLatch(1);

        //if this reference gets set, something went wrong creating the window
        final AtomicReference<String> failedReason = new AtomicReference<String>();

        printf("creating new chromium window (uuid: %s) (left: %s) (top: %s) (width: %s) (height: %s) (withFrame: %s) (resizable: %s)",
                uuid, left, top, width, height, withFrame, resizable);

        Application application = new Application(applicationOptions, desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                try {
                    Application application = (Application) ack.getSource();
                    application.run();
                    printf("window running: %s", application.getOptions().getUUID());
                    application.getWindow().setBounds(left, top, width, height, new AckListener() {
                        @Override
                        public void onSuccess(Ack ack) {
                            printf("successfully set bounds (uuid: %s) (left: %s) (top: %s) (width: %s) (height: %s)", uuid, left, top, width, height);
                        }

                        @Override
                        public void onError(Ack ack) {
                            printf("failed to set window bounds (uuid: %s)", uuid);
                        }
                    });
                    printf("explicity setting bounds (uuid: %s) (left: %s) (top: %s) (width: %s) (height: %s)", uuid, left, top, width, height);
                } catch (Exception e) {
                    failedReason.set("failed to run window: " + ack.getReason());
                } finally {
                    windowCreatedLatch.countDown();
                }
            }

            @Override
            public void onError(Ack ack) {
                try {
                    failedReason.set("failed to open window: " + ack.getReason());
                } finally {
                    windowCreatedLatch.countDown();
                }
            }
        });

        //wait for OpenFin callback
        windowCreatedLatch.await(20, TimeUnit.SECONDS);

        if (failedReason.get() != null) {
            throw new RuntimeException(failedReason.get());
        } else {
            return application;
        }
    }

    private static synchronized String nextTestUuid() {
        return String.format("test_uuid_%s", windowUuidCounter++);
    }

    @Test
    public void canStopAndRestartOpenFinDesktopConnection() throws Exception {
        //setupDesktopConnection() called from setup()
        teardownDesktopConnection();
        setupDesktopConnection();
        //teardownDesktopConnection() will be caled from teardown()
    }

    @Test
    public void throwingExceptionFromOpenFinThreadRecoversCleanly() throws Exception {

        Application application = openWindow(nextTestUuid(), "http://www.google.com");
        WindowBounds bounds1 = getWindowBounds(application.getWindow());

        //throw an exception from the OpenFin event thread
        final int delta = 10;
        application.getWindow().moveBy(delta, delta, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                throw new RuntimeException("thrown intentionally - OpenFin should recover from this");
            }
            @Override
            public void onError(Ack ack) {
                fail("could not open a window to run test: " + ack.getReason());
            }
        });

        try {
            WindowBounds bounds2 = getWindowBounds(application.getWindow());
            assertEquals("window was not moved", new Integer(bounds1.getLeft() + delta), bounds2.getLeft());
        } catch (Throwable t) {
            fail("OpenFin did not respond after an exception was thrown, caused by: " + t.getMessage());
            t.printStackTrace();
        }
        application.close();
    }



    @Test
    public void canOpenAndCloseMultipleWindowsWithDifferentUUIDS() throws Exception {
        Application application1 = openWindow(nextTestUuid(), "http://www.google.com");
        Application application2 = openWindow(nextTestUuid(), "http://www.google.co.uk");
        Thread.sleep(SLEEP_FOR_HUMAN_OBSERVATION);//allow the windows time to appear
        application1.close();
        application2.close();
    }

    @Test
    public void cannotOpenMultipleWindowsWithSameUUID() throws Exception {
        Application application1 = null;
        Application application2 = null;
        try {
            String uuid = nextTestUuid();
            application1 = openWindow(uuid, "http://www.google.com");
            application2 = openWindow(uuid, "http://www.google.co.uk");
            //above lines should throw an exception and not get past here
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Application with specified UUID already exists"));
        } finally {
            if (application1 != null) application1.close();
            if (application2 != null) application2.close();
        }
    }

    @Test
    public void windowMoves() throws Exception {
        Application application = openWindow(nextTestUuid(), "http://www.google.com");

        //set the initial position of the window and check
        CountDownLatch moveLatch = new CountDownLatch(1);
        // OpenFin update: need to pass AckListener to make sure moveTo operations is completed before checking bounds
        application.getWindow().moveTo(10, 20, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                moveLatch.countDown();
            }
            @Override
            public void onError(Ack ack) {
            }
        });
        moveLatch.await(5, TimeUnit.SECONDS);
        WindowBounds initialBounds = getWindowBounds(application.getWindow());
        printf("initial bounds top:%s left:%s", initialBounds.getTop(), initialBounds.getLeft());
        assertEquals(10, initialBounds.getLeft().intValue());
        assertEquals(20, initialBounds.getTop().intValue());
        Thread.sleep(SLEEP_FOR_HUMAN_OBSERVATION);

        //move the window and check again
        // OpenFin update: need to pass AckListener to make sure moveTo operations is completed before checking bounds
        CountDownLatch moveLatch2 = new CountDownLatch(1);
        application.getWindow().moveTo(100, 200, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                moveLatch2.countDown();
            }
            @Override
            public void onError(Ack ack) {
            }
        });
        moveLatch2.await(5, TimeUnit.SECONDS);
        WindowBounds movedBounds = getWindowBounds(application.getWindow());
        printf("moved bounds top:%s left:%s", movedBounds.getTop(), movedBounds.getLeft());

        assertEquals(100, movedBounds.getLeft().intValue());
        assertEquals(200, movedBounds.getTop().intValue());
        Thread.sleep(SLEEP_FOR_HUMAN_OBSERVATION);
    }

    @Test
    public void windowMovesWithAnimation() throws Exception {
        Application application = openWindow(nextTestUuid(), "http://www.google.com");

        //set the initial position of the window and check
        CountDownLatch moveLatch = new CountDownLatch(1);
        application.getWindow().moveTo(10, 20, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                moveLatch.countDown();
            }
            @Override
            public void onError(Ack ack) {
            }
        });
        moveLatch.await(5, TimeUnit.SECONDS);
        WindowBounds initialBounds = getWindowBounds(application.getWindow());
        printf("initial bounds top:%s left:%s", initialBounds.getTop(), initialBounds.getLeft());
        assertEquals(10, initialBounds.getLeft().intValue());
        assertEquals(20, initialBounds.getTop().intValue());
        Thread.sleep(SLEEP_FOR_HUMAN_OBSERVATION);

        //move the window and check again
        CountDownLatch transitionLatch = new CountDownLatch(1);
        AnimationTransitions transitions = new AnimationTransitions();
        transitions.setPosition(new PositionTransition(100, 200, 2000));//duration in millisecods
        application.getWindow().animate(transitions, null, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                transitionLatch.countDown();
            }

            @Override
            public void onError(Ack ack) {
                //noop
            }
        });
        transitionLatch.await(20, TimeUnit.SECONDS);

        WindowBounds movedBounds = getWindowBounds(application.getWindow());
        printf("moved bounds top:%s left:%s", movedBounds.getTop(), movedBounds.getLeft());

        assertEquals(100, movedBounds.getLeft().intValue());
        assertEquals(200, movedBounds.getTop().intValue());
        Thread.sleep(SLEEP_FOR_HUMAN_OBSERVATION);
    }

    @Test
    public void windowEventListenersWork() throws Exception {
        Application application = openWindow(nextTestUuid(), "http://www.google.com");

        //as per the javadoc (https://cdn.openfin.co/docs/java/4.40.2/com/openfin/desktop/Window.html), the event names:
        String events = "blurred bounds-changed bounds-changing closed close-requested disabled-frame-bounds-changed disabled-frame-bounds-changing focused frame-disabled frame-enabled group-changed hidden maximized minimized restored shown";

        AtomicReference<String> eventTypeRecieved = new AtomicReference<String>();
        CountDownLatch onMinimiseEventLatch = new CountDownLatch(1);

        //register even handlers for all event types
        StringTokenizer tokenizer = new StringTokenizer(events);
        while (tokenizer.hasMoreTokens()) {
            String event = tokenizer.nextToken().trim();
            application.getWindow().addEventListener(
                    event,
                    actionEvent -> {
                        printf("eventReceived: %s", actionEvent.getType());
                        String type = actionEvent.getType();
                        eventTypeRecieved.set(type);
                        if ("minimized".equals(type)) {
                            onMinimiseEventLatch.countDown();
                        }
                    }, new AckListener() {
                        @Override
                        public void onSuccess(Ack ack) {
                            printf("window '%s' onSuccess: %s", event, ack);
                        }

                        @Override
                        public void onError(Ack ack) {
                            printf("window '%s' onError: %s", event, ack);
                        }
                    }
            );
            printf("added listener for event: %s", event);
        }

        //generate a minimized event to check that we get notification in the listener above
        application.getWindow().minimize();
        onMinimiseEventLatch.await(20, TimeUnit.SECONDS);
        assertEquals("minimized event not recieved", "minimized", eventTypeRecieved.get());
    }

    @Test
    public void windowsInShameGroupMoveTogether() throws Exception {
        final int width = 600, height = 900;

        //place two application windows next to each other
        Application applicationA = openWindow(nextTestUuid(), "http://www.google.com", 0, 0, width, height, true, true);
        Application applicationB = openWindow(nextTestUuid(), "http://www.bbc.co.uk", width, 0, width, height, true, true);

        //bind the windows in the same group so that they move togther
        applicationA.getWindow().joinGroup(applicationB.getWindow(), new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                printf("window A joined group");
            }

            @Override
            public void onError(Ack ack) {
                printf("failed to join group");
            }
        });

        //move window A and check that B has followed it
        final int moveLefPositiontBy = 100;
        final int appBLeftStart = getWindowBounds(applicationB.getWindow()).getLeft();

        Thread.sleep(SLEEP_FOR_HUMAN_OBSERVATION);
        applicationA.getWindow().moveBy(moveLefPositiontBy, 0, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                printf("moved window A");
            }

            @Override
            public void onError(Ack ack) {
                printf("failed to move window A");
            }
        });
        Thread.sleep(SLEEP_FOR_HUMAN_OBSERVATION);
        final int appBLeftEnd = getWindowBounds(applicationB.getWindow()).getLeft();

        assertEquals("the window for app B did not follow the move for app A", appBLeftStart + moveLefPositiontBy, appBLeftEnd);
    }

    @Test
    public void notificationEventListenersWork() throws Exception {
        // wait for Notification service to start up
        Window notificationService = Window.wrap("service:notifications", "queueCounter", desktopConnection);
        CountDownLatch notificationLatch = new CountDownLatch(1);
        notificationService.addEventListener("app-connected", actionEvent -> {
            if (actionEvent.getType().equals("app-connected")) {
                logger.info("notification center ready");
                notificationLatch.countDown();
            }
        }, null);
        notificationLatch.await(10, TimeUnit.SECONDS);

        //record/wait for event
        CountDownLatch onCloseEventLatch = new CountDownLatch(1);
        AtomicReference<String> eventTypeRecieved = new AtomicReference<>();

        NotificationOptions options = new NotificationOptions("http://demoappdirectory.openf.in/desktop/config/apps/OpenFin/HelloOpenFin/views/notification.html");
        options.setTimeout(1000);
        options.setMessageText("testing");
        new Notification(options, new NotificationListener() {
            @Override
            public void onClick(Ack ack) {
                eventTypeRecieved.set("onClick");
            }

            @Override
            public void onClose(Ack ack) {
                eventTypeRecieved.set("onClose");
                onCloseEventLatch.countDown();
            }

            @Override
            public void onDismiss(Ack ack) {
                eventTypeRecieved.set("onDismiss");
            }

            @Override
            public void onError(Ack ack) {
                eventTypeRecieved.set("onError");
            }

            @Override
            public void onMessage(Ack ack) {
                eventTypeRecieved.set("onMessage");
            }

            @Override
            public void onShow(Ack ack) {
                eventTypeRecieved.set("onShow");
            }
        }, this.desktopConnection, null);

        //wait for the onClose notification to arrive
        onCloseEventLatch.await(20000, TimeUnit.SECONDS);
        assertEquals("onClose", eventTypeRecieved.get());
    }

    private WindowBounds getWindowBounds(Window window) throws Exception {
        final AtomicReference<WindowBounds> atomicRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        window.getBounds(windowBounds -> {
            atomicRef.set(windowBounds);
            latch.countDown();
        }, null);
        latch.await(20, TimeUnit.SECONDS);
        WindowBounds windowBounds = atomicRef.get();
        assertNotNull("failed to get bounds for window", windowBounds);
        return windowBounds;
    }

    private static void printf(String s, Object... args) {
        logger.info(String.format(s, args));
    }

}
