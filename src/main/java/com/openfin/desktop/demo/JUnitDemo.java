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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.fail;

/**
 * Exmaple testing code with JUNIT
 *
 * Created by wche on 1/4/16.
 */
public class JUnitDemo {
    private static Logger logger = LoggerFactory.getLogger(JUnitDemo.class.getName());

    private static DesktopConnection desktopConnection;

    private static final String DESKTOP_UUID = "DESKTOP_UUID";

    private static int windowUuidCounter = 0;

    //set this to around 2000ms in order to see the chromium windows
    private final long SLEEP_FOR_HUMAN_OBSERVATION = 1000L;

    @BeforeClass
    public static void setup() throws Exception {
        setupDesktopConnection();
    }

    @AfterClass
    public static void teardown() throws Exception {
        teardownDesktopConnection();
    }

    private static void setupDesktopConnection() throws Exception {
        String desktopVersion = java.lang.System.getProperty("com.openfin.demo.version");
        if (desktopVersion == null) {
            desktopVersion = "stable";
        }

        final CountDownLatch openFinConnectedLatch = new CountDownLatch(1);

        desktopConnection = null;
        desktopConnection = new DesktopConnection(DESKTOP_UUID);
        desktopConnection.setAdditionalRuntimeArguments(" --v=1");  // enable additional logging from Runtime
        desktopConnection.connectToVersion(desktopVersion, new DesktopStateListener() {
            @Override
            public void onReady() {
                printf("Connected to OpenFin runtime");
                openFinConnectedLatch.countDown();
            }

            @Override
            public void onClose(String error) {
                printf("Connection to Runtime closed");
            }

            @Override
            public void onError(String reason) {
                printf("Connection failed: %s", reason);
            }

            @Override
            public void onMessage(String message) {
                printf("openfin message: %s", message);
            }

            @Override
            public void onOutgoingMessage(String message) {
                printf("openfin outgoing message: %s", message);
            }
        }, 60);//this timeout (in 4.40.2.9) is ignored

        printf("waiting for desktop to connect");
        // wait for 60 seconds here in case it takes time to download newer version of Runtime
        openFinConnectedLatch.await(60, TimeUnit.SECONDS);

        if (desktopConnection.isConnected()) {
            printf("desktop connected");
        } else {
            throw new RuntimeException("failed to initialise desktop connection");
        }
    }

    private static void teardownDesktopConnection() throws Exception {
        new System(desktopConnection).exit();
        printf("desktop connection closed");
    }

    private Application openHelloOpenFin(String uuid, String url) throws Exception {
        //default options for all test windows
        int top = 50;
        int left = 10;
        int width = 395;
        int height = 525;
        boolean withFrame = false;
        boolean resizable = false;
        return openWindow(uuid, url, left, top, width, height, withFrame, resizable);
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

    /**
     * Create an OpenFin Application
     * @param uuid uuid of the app
     * @param url  url of the app
     * @param left
     * @param top
     * @param width
     * @param height
     * @param withFrame
     * @param resizable
     * @return
     * @throws Exception
     */
    private Application openWindow(final String uuid, final String url, final int left, final int top, final int width, final int height, boolean withFrame, boolean resizable) throws Exception {
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
        final CountDownLatch windowCreatedLatch = new CountDownLatch(1);

        //if this reference gets set, something went wrong creating the window
        final AtomicReference<String> failedReason = new AtomicReference<String>();

        printf("creating new chromium window (uuid: %s) (left: %s) (top: %s) (width: %s) (height: %s) (withFrame: %s) (resizable: %s)",
                uuid, left, top, width, height, withFrame, resizable);

        Application application = new Application(applicationOptions, desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                try {
                    Application application = (Application) ack.getSource();
                    // use app-connected event to wait for the app to be connected to OpenFin Runtime
                    application.getWindow().addEventListener("app-connected", new EventListener() {
                        public void eventReceived(ActionEvent actionEvent) {
                            printf("eventReceived: %s", actionEvent.getType());
                            windowCreatedLatch.countDown();
                        }
                    }, new AckListener() {
                        public void onSuccess(Ack ack) {
                            if (ack.isSuccessful()) {
                                printf("app-connected listener added: %s", uuid);
                            }
                        }
                        public void onError(Ack ack) {
                        }
                    });

                    application.run();
                    printf("window running: %s", ack);
                } catch (Exception e) {
                    failedReason.set("failed to run window: " + ack.getReason());
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
        assertEquals("Open window timed out " + uuid, windowCreatedLatch.getCount(), 0);

        if (failedReason.get() != null) {
            throw new RuntimeException(failedReason.get());
        } else {

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
        application.getWindow().moveTo(10, 20);
        WindowBounds initialBounds = getWindowBounds(application.getWindow());
        printf("initial bounds top:%s left:%s", initialBounds.getTop(), initialBounds.getLeft());
        assertEquals(10, initialBounds.getLeft().intValue());
        assertEquals(20, initialBounds.getTop().intValue());
        Thread.sleep(SLEEP_FOR_HUMAN_OBSERVATION);

        //move the window and check again
        application.getWindow().moveTo(100, 200);
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
        application.getWindow().moveTo(10, 20);
        WindowBounds initialBounds = getWindowBounds(application.getWindow());
        printf("initial bounds top:%s left:%s", initialBounds.getTop(), initialBounds.getLeft());
        assertEquals(10, initialBounds.getLeft().intValue());
        assertEquals(20, initialBounds.getTop().intValue());
        Thread.sleep(SLEEP_FOR_HUMAN_OBSERVATION);

        //move the window and check again
        final CountDownLatch transitionLatch = new CountDownLatch(1);
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

        final AtomicReference<String> eventTypeRecieved = new AtomicReference<String>();
        final CountDownLatch onMinimiseEventLatch = new CountDownLatch(1);

        //register even handlers for all event types
        StringTokenizer tokenizer = new StringTokenizer(events);
        while (tokenizer.hasMoreTokens()) {
            final String event = tokenizer.nextToken().trim();
            application.getWindow().addEventListener(event, new EventListener() {
                @Override
                public void eventReceived(ActionEvent actionEvent) {
                    printf("eventReceived: %s", actionEvent.getType());
                    String type = actionEvent.getType();
                    eventTypeRecieved.set(type);
                    if ("minimized".equals(type)) {
                        onMinimiseEventLatch.countDown();
                    }
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

    @Ignore("Cross app docking not supported")
    @Test
    public void windowsInSameGroupMoveTogether() throws Exception {
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
        //record/wait for event
        final CountDownLatch onCloseEventLatch = new CountDownLatch(1);
        final AtomicReference<String> eventTypeRecieved = new AtomicReference<String>();

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
        onCloseEventLatch.await(20, TimeUnit.SECONDS);
        assertEquals("onClose", eventTypeRecieved.get());
    }

    @Test
    public void childWindow() throws Exception {
        Application application = openHelloOpenFin(nextTestUuid(), "http://demoappdirectory.openf.in/desktop/config/apps/OpenFin/HelloOpenFin/index.html");
        Window childWindow = this.createChildWindow(application, "ChildWindow1", "http://test.openf.in/bus/simple.html", 300, 300, 150, 150);
        Thread.sleep(SLEEP_FOR_HUMAN_OBSERVATION);
        WindowBounds bounds = getWindowBounds(childWindow);
        assertEquals(300, bounds.getHeight().intValue());

        application.close();
    }

    @Test
    public void windowsOfSameAppInSameGroupMoveTogether() throws Exception {
        //place two windows next to each other
        Application application = openHelloOpenFin(nextTestUuid(), "http://demoappdirectory.openf.in/desktop/config/apps/OpenFin/HelloOpenFin/index.html");
        Window childWindowA = this.createChildWindow(application, "ChildWindowA", "http://test.openf.in/bus/simple.html", 300, 300, 150, 150);
        Window childWindowB = this.createChildWindow(application, "ChildWindowB", "http://test.openf.in/bus/simple.html", 300, 300, 450, 150);

        Thread.sleep(SLEEP_FOR_HUMAN_OBSERVATION);

        //bind the windows in the same group so that they move togther
        childWindowA.joinGroup(childWindowB, new AckListener() {
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
        final int moveLefPositiontBy = 300;
        final int appBLeftStart = getWindowBounds(childWindowB).getLeft();

        childWindowA.moveBy(moveLefPositiontBy, 0, new AckListener() {
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
        final int appBLeftEnd = getWindowBounds(childWindowB).getLeft();

        assertEquals("the window B did not follow the move for Window A", appBLeftStart + moveLefPositiontBy, appBLeftEnd);

        application.close();
    }


    private WindowBounds getWindowBounds(Window window) throws Exception {
        final AtomicReference<WindowBounds> atomicRef = new AtomicReference<WindowBounds>();
        final CountDownLatch latch = new CountDownLatch(1);
        window.getBounds(new AsyncCallback<WindowBounds>() {
            @Override
            public void onSuccess(WindowBounds result) {
                atomicRef.set(result);
                latch.countDown();
            }
        }, null);
        latch.await(20, TimeUnit.SECONDS);
        WindowBounds windowBounds = atomicRef.get();
        assertNotNull("failed to get bounds for window", windowBounds);
        return windowBounds;
    }

    /**
     * Create a child window for an Application
     *
     * @param application owner application
     * @param name name of the child window
     * @param url  url of the child window
     * @param width
     * @param height
     * @param left
     * @param top
     * @return
     * @throws Exception
     */
    private Window createChildWindow(Application application, final String name, String url, int width, int height, int left, int top) throws Exception {

        WindowOptions options = new WindowOptions(name, url);
        options.setAutoShow(false);
        options.setSaveWindowState(false);  // so windows will always open at position specified
        options.setDefaultHeight(height);
        options.setDefaultWidth(width);
        options.setDefaultLeft(left);
        options.setDefaultTop(top);

        final CountDownLatch onWindowCreatedLatch = new CountDownLatch(1);
        // use window-end-load event to wait for the window to finish loading
        application.addEventListener("window-end-load", new EventListener() {
            public void eventReceived(ActionEvent actionEvent) {
                printf("eventReceived: %s for window %s to listener %s", actionEvent.getType(), actionEvent.getEventObject().getString("name"), name);
                if (actionEvent.getEventObject().has("name")) {
                    if (name.equals(actionEvent.getEventObject().getString("name"))) {
                        onWindowCreatedLatch.countDown();
                    }
                }
            }
        }, null);
        application.createChildWindow(options, null);
        onWindowCreatedLatch.await(20, TimeUnit.SECONDS);
        assertEquals("child window " + name + " not being created", onWindowCreatedLatch.getCount(), 0);

        Window childWindow = Window.wrap(application.getOptions().getUUID(), name, desktopConnection);
        final CountDownLatch addShownListenerLatch = new CountDownLatch(1);
        final CountDownLatch onShownLatch = new CountDownLatch(1);
        childWindow.addEventListener("shown", new EventListener() {
            @Override
            public void eventReceived(ActionEvent actionEvent) {
                Window src = (Window) actionEvent.getSource();
                printf("eventReceived: %s for window %s", actionEvent.getType(), src.getName());
                onShownLatch.countDown();
            }
        }, new AckListener() {
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    addShownListenerLatch.countDown();
                }
            }
            public void onError(Ack ack) {

            }
        });
        addShownListenerLatch.await(20, TimeUnit.SECONDS);
        assertEquals("child window " + name + " shown event listener was not added", addShownListenerLatch.getCount(), 0);
        printf("Calling show() on %s", name);
        childWindow.show();
        onShownLatch.await(20, TimeUnit.SECONDS);
        assertEquals("child window " + name + " is not shown", onShownLatch.getCount(), 0);
        return childWindow;
    }

    private static void printf(String s, Object... args) {
        logger.info(String.format(s, args));
    }




}
