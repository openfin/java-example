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
                logger.debug("onClose for notification");
                onCloseLatch.countDown();
            }

            @Override
            public void onDismiss(Ack ack) {
                logger.debug("onDismiss for notification");
                onCloseLatch.countDown();
            }

            @Override
            public void onError(Ack ack) {
                logger.error("onError for notification");
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
    
    @Test
    public void sendMessage() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        NotificationOptions options = new NotificationOptions(notification_url);
        options.setMessage(new JSONObject("{message: \"JSONMessage\"}"));
        //options.setMessageText("Notification Text");
        options.setTimeout(1000);
        
        NotificationListener nListener = new NotificationListener() {

			@Override
			public void onClick(Ack ack) {
			}

			@Override
			public void onClose(Ack ack) {
			}

			@Override
			public void onDismiss(Ack ack) {
			}

			@Override
			public void onError(Ack ack) {
                logger.error("onError for notification");
			}

			@Override
			public void onMessage(Ack ack) {
                logger.info("onMessage");
				latch.countDown();
			}

			@Override
			public void onShow(Ack ack) {
			}
		};

		AckListener ackListener = new AckListener() {
			@Override
			public void onSuccess(Ack ack) {
			}

			@Override
			public void onError(Ack ack) {
                logger.error(ack.getReason());
			}
		};
		
        Notification n = new Notification(options, nListener, desktopConnection, ackListener);
        n.sendMessage(new JSONObject("{message: \"JSONMessage2\"}"), new AckListener() {

			@Override
			public void onSuccess(Ack ack) {
				latch.countDown();
			}

			@Override
			public void onError(Ack ack) {
                logger.error("onError for sendMessage");
			}
		});
        latch.await(10, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());

    }
}
