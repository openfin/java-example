package com.openfin.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for com.openfin.desktop.Notification class
 *
 * Created by wche on 1/26/16.
 */

public class NotificationTest {
    private static Logger logger = LoggerFactory.getLogger(NotificationTest.class.getName());

    private static final String DESKTOP_UUID = NotificationTest.class.getName();
    private static DesktopConnection desktopConnection;
    // notification used by Hello OpenFin demo app.
    private static final String notification_url = "http://demoappdirectory.openf.in/desktop/config/apps/OpenFin/HelloOpenFin/views/notification.html";

    @BeforeClass
    public static void setup() throws Exception {
        logger.debug("starting");
        desktopConnection = TestUtils.setupConnection(DESKTOP_UUID);

        // for Runtime 6.0+, needs to start at least one app for notifications to work
        ApplicationOptions options = TestUtils.getAppOptions(null);
        Application application = TestUtils.runApplication(options, desktopConnection);

        logger.info("Waiting for notification center to be ready");
        // @TODO currently there is no way to know Notification center is ready, so we just sleep here
        // @TODO we will fix notification center to generate an event when it finishes initialization and is ready to process requests
        Thread.sleep(8000);
    }

    @AfterClass
    public static void teardown() throws Exception {
        TestUtils.teardownDesktopConnection(desktopConnection);
    }

}
