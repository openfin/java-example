package com.openfin.desktop;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

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

        logger.info("Waiting for notification center to be ready");
        // @TODO currently there is no way to know Notification center is ready, so we just sleep here
        // @TODO we will fix notification center to generate an event when it finishes initialization and ready to process requests
        Thread.sleep(8000);
    }

    @AfterClass
    public static void teardown() throws Exception {
        TestUtils.teardownDesktopConnection(desktopConnection);
    }

    @Test
    public void notificationEventListenersWork() throws Exception {
        CountDownLatch onShowLatch = new CountDownLatch(1);
        CountDownLatch onCloseLatch = new CountDownLatch(1);

        NotificationOptions options = new NotificationOptions(notification_url);
        options.setTimeout(1000);
        options.setMessageText("Unit test for notification");
        new Notification(options, new NotificationListener() {
            @Override
            public void onClick(Ack ack) {
                logger.debug("onClick for notification");
            }

            @Override
            public void onClose(Ack ack) {
                logger.debug("onClick for notification");
                onCloseLatch.countDown();
            }

            @Override
            public void onDismiss(Ack ack) {
                logger.debug("onDismiss for notification");
                onCloseLatch.countDown();
            }

            @Override
            public void onError(Ack ack) {
                logger.error("onClick for notification");
            }

            @Override
            public void onMessage(Ack ack) {
                logger.debug("onMessage for notification");
            }

            @Override
            public void onShow(Ack ack) {
                logger.debug("onShow for notification");
                onShowLatch.countDown();
            }
        }, desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
            }
            @Override
            public void onError(Ack ack) {
                logger.error(ack.getReason());
                fail("onError");
            }
        });

        onShowLatch.await(5, TimeUnit.SECONDS);
        onCloseLatch.await(5, TimeUnit.SECONDS);
        assertEquals(onShowLatch.getCount(), 0);
        assertEquals(onCloseLatch.getCount(), 0);
    }
}
