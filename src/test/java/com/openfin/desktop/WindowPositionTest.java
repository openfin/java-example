package com.openfin.desktop;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 *
 * Test to verify Runtime saves Window positions between restarts.
 *
 * This test is not included in AllTests.java
 *
 * Created by wche on 2/23/16.
 *
 */
public class WindowPositionTest {
    private static Logger logger = LoggerFactory.getLogger(WindowPositionTest.class.getName());

    private static final String DESKTOP_UUID = WindowTest.class.getName();
    private static DesktopConnection desktopConnection;

    private static String appUuid;
    private static int defaultHeight, defaultWidth, defaultTop, defaultLeft;
    private static boolean successRun = true;

    @BeforeClass
    public static void setup() throws Exception {
        logger.debug("starting");
        desktopConnection = TestUtils.setupConnection(DESKTOP_UUID);

        // assign random bounds.  Runtime adjusts bounds to try to fit windows inside the monitor, so bounds need to be within boundary of the monitor
        appUuid       = UUID.randomUUID().toString();
        defaultTop    = getRandomNumber();
        defaultLeft   = getRandomNumber();
        defaultHeight = getRandomNumber() + 38;   // chromium enforces 140/38 as min height/width
        defaultWidth  = getRandomNumber() + 140;
        logger.debug(String.format("default bounds %s %d %d %d %d", appUuid, defaultHeight, defaultWidth, defaultTop, defaultLeft));
    }

    /**
     * return random int between 1 and 300
     * @return
     */
    private static int getRandomNumber()  {
        return (int) Math.round(Math.random() * 300);  // 300 should be small enough to fit most monitors
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (successRun) {
            TestUtils.teardownDesktopConnection(desktopConnection);
        }
    }


    @Ignore("later")
    @Test
    public void saveWindowStateWithoutRuntimeRestart() throws Exception {
        int repeat = 30;
        String value = java.lang.System.getProperty("com.openfin.desktop.WindowPositionTest.repeat");
        if (value != null) {
            repeat = Integer.parseInt(value);
        }
        logger.debug(String.format("Running test %d times", repeat));
        for (int i = 0; i < repeat; i++) {
            runAndClose();
        }
    }

    @Test
    public void saveWindowStateWithRuntimeRestart() throws Exception {
        int repeat = 30;
        String value = java.lang.System.getProperty("com.openfin.desktop.WindowPositionTest.repeat");
        if (value != null) {
            repeat = Integer.parseInt(value);
        }
        logger.debug(String.format("Running test %d times", repeat));
        for (int i = 0; i < repeat; i++) {
            runAndClose();
            TestUtils.teardownDesktopConnection(desktopConnection);
            desktopConnection = TestUtils.setupConnection(DESKTOP_UUID);
        }
    }

    private void runAndClose() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(appUuid, null);
        options.getMainWindowOptions().setSaveWindowState(true);
        options.getMainWindowOptions().setDefaultHeight(defaultHeight);
        options.getMainWindowOptions().setDefaultWidth(defaultWidth);
        options.getMainWindowOptions().setDefaultTop(defaultTop);
        options.getMainWindowOptions().setDefaultLeft(defaultLeft);

        Window mainWindow = Window.wrap(options.getUUID(), options.getUUID(), desktopConnection);
        CountDownLatch shownLatch = new CountDownLatch(1);
        TestUtils.addEventListener(mainWindow, "shown", actionEvent -> {
            if (actionEvent.getType().equals("shown")) {
                shownLatch.countDown();
            }
        });

        Application application = TestUtils.runApplication(options, desktopConnection);

        shownLatch.await(1000, TimeUnit.SECONDS);
        assertEquals("shown timeout " + options.getUUID(), shownLatch.getCount(), 0);

        WindowBounds bounds = TestUtils.getBounds(application.getWindow());
        logger.debug(String.format("shown bounds %d %d %d %d", bounds.getHeight(), bounds.getWidth(), bounds.getTop(), bounds.getLeft()));
        if (bounds.getHeight() != defaultHeight || bounds.getWidth() != defaultWidth || bounds.getTop() != defaultTop || bounds.getLeft() != defaultLeft) {
            successRun = false;
            fail("Window bounds do not match saved bounds");
        }

        TestUtils.closeApplication(application);
    }

}