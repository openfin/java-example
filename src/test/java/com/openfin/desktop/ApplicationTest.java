package com.openfin.desktop;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;

/**
 *
 * JUnit tests for com.openfin.desktop.Application class
 *
 * Created by wche on 1/22/16.
 *
 */
public class ApplicationTest {
    private static Logger logger = LoggerFactory.getLogger(ApplicationTest.class.getName());

    private static final String DESKTOP_UUID = ApplicationTest.class.getName();
    private static DesktopConnection desktopConnection;

    @BeforeClass
    public static void setup() throws Exception {
        logger.debug("starting");
        desktopConnection = TestUtils.setupConnection(DESKTOP_UUID);
    }

    @AfterClass
    public static void teardown() throws Exception {
        TestUtils.teardownDesktopConnection(desktopConnection);
    }

    @Test
    public void runAndClose() throws Exception {
        logger.debug("runAndClose");
        ApplicationOptions options = TestUtils.getAppOptions();
        Application application = TestUtils.runApplication(options, desktopConnection);

        // duplciate UUID is not allowed
        ApplicationOptions options2 = TestUtils.getAppOptions(options.getUUID());
        CountDownLatch dupLatch = new CountDownLatch(1);
        Application application2 = new Application(options2, desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
            }
            @Override
            public void onError(Ack ack) {
                if (ack.getReason().contains("Application with specified UUID already exists")) {
                    dupLatch.countDown();
                }
            }
        });
        dupLatch.await(5, TimeUnit.SECONDS);
        assertEquals("Duplicate app UUID validation timeout " + options.getUUID(), dupLatch.getCount(), 0);

        TestUtils.closeApplication(application);
    }

    @Test
    public void runAndTerminate() throws Exception {
        logger.debug("runAndTerminate");
        ApplicationOptions options = TestUtils.getAppOptions();
        Application application = TestUtils.createApplication(options, desktopConnection);

        CountDownLatch stoppedLatch = new CountDownLatch(1);
        EventListener listener = new EventListener() {
            @Override
            public void eventReceived(ActionEvent actionEvent) {
                if (actionEvent.getType().equals("closed")) {
                    stoppedLatch.countDown();
                }
            }
        };
        TestUtils.addEventListener(application, "closed", listener);
        TestUtils.runApplication(application, true);
        application.terminate();
        stoppedLatch.await(5, TimeUnit.SECONDS);
        assertEquals("Terminate application timeout  " + options.getUUID(), stoppedLatch.getCount(), 0);
    }

    @Test
    public void getApplicationManifest() throws Exception {
        logger.debug("getApplicationManifest");
        ApplicationOptions options = TestUtils.getAppOptions();
        Application application = TestUtils.runApplication(options, desktopConnection);
        CountDownLatch latch = new CountDownLatch(1);
        application.getManifest(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
            }
            @Override
            public void onError(Ack ack) {
                logger.debug(ack.getJsonObject().toString());
                if (ack.getReason().contains(" was not created from a manifest")) {
                    latch.countDown();
                }
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("getApplicationManifest timeout " + options.getUUID(), latch.getCount(), 0);
        TestUtils.closeApplication(application);
    }

    @Ignore("restart does not fire started event.  need to take a look later")
    @Test
    public void restartApplication() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions();
        Application application = TestUtils.runApplication(options, desktopConnection);
        CountDownLatch latch = new CountDownLatch(1);
        TestUtils.addEventListener(application, "started", actionEvent -> {
            if (actionEvent.getType().equals("started")) {
                latch.countDown();
            }
        });
        application.restart();
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("restartApplication timeout " + options.getUUID(), latch.getCount(), 0);
    }

    @Test
    public void runReqestedEventListeners() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions();
        Application application = TestUtils.createApplication(options, desktopConnection);
        CountDownLatch latch = new CountDownLatch(1);
        TestUtils.addEventListener(application, "run-requested", actionEvent -> {
            if (actionEvent.getType().equals("run-requested")) {
                logger.debug(String.format("%s", actionEvent.getEventObject().toString()));
                latch.countDown();
            }
        });
        TestUtils.runApplication(application, true);
        // run-requested is generated when Application.run is called on an active application
        application.run();
        latch.await(5, TimeUnit.SECONDS);
        assertEquals("run-requested timeout " + options.getUUID(), latch.getCount(), 0);
        TestUtils.closeApplication(application);
    }

    @Test
    public void createChildWindow() throws Exception {
        Application application = TestUtils.runApplication(TestUtils.getAppOptions(), desktopConnection);
        WindowOptions childOptions = TestUtils.getWindowOptions("child1", TestUtils.openfin_app_url); // use same URL as main app
        Window childWindow = TestUtils.createChildWindow(application, childOptions, desktopConnection);
        TestUtils.closeApplication(application);
    }
}
