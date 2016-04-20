package com.openfin.desktop;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.assertEquals;

/**
 * JUnit tests for com.openfin.desktop.InterApplicationBus class
 *
 * Test cases in this class need to have access to an OpenFin HTML5 app to verify sub/pub workflow.  Sources for the app can be found in release
 * directory: PubSubExample.html.  It is hosted by OpenFin at https://cdn.openfin.co/examples/junit/PubSubExample.html
 *
 * Created by wche on 1/27/16.
 *
 */
public class InterApplicationBusTest {
    private static Logger logger = LoggerFactory.getLogger(InterApplicationBusTest.class.getName());

    private static final String DESKTOP_UUID = InterApplicationBusTest.class.getName();
    private static DesktopConnection desktopConnection;
    private static OpenFinRuntime runtime;
    private static final String openfin_app_url = "https://cdn.openfin.co/examples/junit/PubSubExample.html";  // source for PubSubExample.html is in release directory
    private static final String check_in_topic = "check-in";

    @BeforeClass
    public static void setup() throws Exception {
        logger.debug("starting");
        desktopConnection = TestUtils.setupConnection(DESKTOP_UUID);
    }

    @AfterClass
    public static void teardown() throws Exception {
        TestUtils.teardownDesktopConnection(desktopConnection);
    }

    private JSONObject createMessage(String text) {
        JSONObject msg = new JSONObject();
        msg.put("text", text);
        return msg;
    }

    private void subscribeToTopic(String sourceUuid, String topic, BusListener listener) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        desktopConnection.getInterApplicationBus().subscribe(sourceUuid, topic, listener, new AckListener() {
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
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(latch.getCount(), 0);
    }

    private void unsubscribeToTopic(String sourceUuid, String topic, BusListener listener) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        desktopConnection.getInterApplicationBus().unsubscribe(sourceUuid, topic, listener, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                latch.countDown();
            }
            @Override
            public void onError(Ack ack) {
                logger.error(ack.getReason());
            }
        });
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(latch.getCount(), 0);
    }

    private void publishMessage(String topic, JSONObject payload) {
        CountDownLatch latch = new CountDownLatch(1);
        logger.debug(String.format("Publishing %s", payload.toString()));
        try {
            desktopConnection.getInterApplicationBus().publish(topic, payload, new AckListener() {
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
            latch.await(3, TimeUnit.SECONDS);
        } catch (Exception ex) {
            logger.error("Error publishing message", ex);
        }
        assertEquals(latch.getCount(), 0);
    }

    private AtomicReference<String> checkInReference;
    private CountDownLatch checkInLatch;
    private BusListener checkInListener;

    /**
     * listen to check-in topic.  Once PubSubExample.html finishes init, it publishes a message to "check-in" topic to announce it is ready
     * to receive messages
     *
     * @throws Exception
     */
    private void setupCheckInTopic() throws Exception {
        checkInReference = new AtomicReference<>();
        checkInLatch = new CountDownLatch(1);
        checkInListener = (sourceUuid, receivingTopic, payload) -> {
            checkInReference.set(sourceUuid);
            checkInLatch.countDown();
        };
        subscribeToTopic("*", check_in_topic, checkInListener);
    }

    /**
     * Wait for any app to send message to check_in_topic to announce it is ready for receiving messages
     *
     * @return UUID of sending app
     * @throws Exception
     */
    private String waitForCheckIn() throws Exception {
        logger.debug(String.format("Waiting for app check-in via %s topic", check_in_topic));
        checkInLatch.await(10, TimeUnit.SECONDS);
        assertEquals(checkInLatch.getCount(), 0);
        return checkInReference.get();
    }
    private void stopCheckInTopic() throws Exception {
        unsubscribeToTopic("*", check_in_topic, checkInListener);
        checkInLatch = null;
        checkInListener = null;
        checkInReference = null;
    }

    /**
     * Publish a test message and verify response in a subscriber with the OpenFin app in PubSubExample.html
     *
     * @throws Exception
     */
    @Test
    public void publishSubscribe() throws Exception {
        setupCheckInTopic();

        ApplicationOptions options = TestUtils.getAppOptions(openfin_app_url);
        Application application = TestUtils.createApplication(options, desktopConnection);
        TestUtils.runApplication(application, true);

        waitForCheckIn();

        String text = "Some text";
        String topic = "unit-test"; // match PubSubExample.html
        JSONObject messageToPublish = createMessage(text);
        CountDownLatch latch = new CountDownLatch(1);
        BusListener busListener = (sourceUuid, receivingTopic, payload) -> {
            JSONObject receivedMsg = (JSONObject) payload;
            logger.debug(String.format("Receiving %s", payload.toString()));
            // PubSubExample.html sends response back
            if (receivedMsg.has("response") && receivedMsg.getString("response").equals(text + " received")) {
                latch.countDown();
            }
        };
        subscribeToTopic("*", topic, busListener);
        publishMessage(topic, messageToPublish);
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(latch.getCount(), 0);

        unsubscribeToTopic("*", topic, busListener);
        stopCheckInTopic();

        TestUtils.closeApplication(application);
    }


    /**
     * Send a test message to an app and verify response in a subscriber with the OpenFin app in PubSubExample.html
     *
     * @throws Exception
     */
    @Test
    public void sendAndVerify() throws Exception {
        setupCheckInTopic();

        ApplicationOptions options = TestUtils.getAppOptions(openfin_app_url);
        Application application = TestUtils.createApplication(options, desktopConnection);
        TestUtils.runApplication(application, true);

        String guestUuid = waitForCheckIn();

        String text = "Private message";
        String topic = "private-channel"; // match PubSubExample.html
        JSONObject messageToSend = createMessage(text);
        CountDownLatch latch = new CountDownLatch(1);
        BusListener busListener = (sourceUuid, receivingTopic, payload) -> {
            JSONObject receivedMsg = (JSONObject) payload;
            logger.debug(String.format("Receiving %s", payload.toString()));
            // PubSubExample.html sends response back
            if (receivedMsg.has("response") && receivedMsg.getString("response").equals(text + " received")) {
                latch.countDown();
            }
        };
        subscribeToTopic(guestUuid, topic, busListener);
        desktopConnection.getInterApplicationBus().send(guestUuid, topic, messageToSend);  // send one-one private message
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(latch.getCount(), 0);

        unsubscribeToTopic(guestUuid, topic, busListener);
        stopCheckInTopic();

        TestUtils.closeApplication(application);
    }


    @Test
    public void addAndRemoveSubscribeListener() throws Exception {
        ApplicationOptions options = TestUtils.getAppOptions(openfin_app_url);
        CountDownLatch subscribeLatch  = new CountDownLatch(1);
        CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        SubscriptionListener subscriptionListener = new SubscriptionListener() {
            @Override
            public void subscribed(String uuid, String topic) {
                // PubSubExample.html subscribes to 'unit-test' after init
                if (topic.equals("unit-test") && uuid.equals(options.getUUID())) {
                    subscribeLatch.countDown();
                }
            }
            @Override
            public void unsubscribed(String uuid, String topic) {
                if (topic.equals("unit-test") && uuid.equals(options.getUUID())) {
                    unsubscribeLatch.countDown();
                }
            }
        };
        desktopConnection.getInterApplicationBus().addSubscribeListener(subscriptionListener);

        // listen to check-in topic.  Once PubSubExample.html finishes init, it publishes a message to "check-in" topic to announce it is ready
        // to receive messages
        AtomicReference<String> atomicReference = new AtomicReference<>();
        CountDownLatch checkInLatch = new CountDownLatch(1);
        BusListener checkInListener = (sourceUuid, receivingTopic, payload) -> {
            atomicReference.set(sourceUuid);
            checkInLatch.countDown();
        };
        subscribeToTopic("*", "check-in", checkInListener);

        Application application = TestUtils.createApplication(options, desktopConnection);
        TestUtils.runApplication(application, true);
        checkInLatch.await(5, TimeUnit.SECONDS);  // wait for check-in from PubSubExample.html

        publishMessage("unit-test", createMessage("Some text"));  // PubSubExample.html unsubscribe 'unit-test' when it receives a message

        subscribeLatch.await(5, TimeUnit.SECONDS);
        unsubscribeLatch.await(5, TimeUnit.SECONDS);
        assertEquals(subscribeLatch.getCount(), 0);
        assertEquals(unsubscribeLatch.getCount(), 0);

        desktopConnection.getInterApplicationBus().removeSubscribeListener(subscriptionListener);
        desktopConnection.getInterApplicationBus().unsubscribe("*", "check-in", checkInListener);

        TestUtils.closeApplication(application);
    }

    @Test
    public void wildCardTopic() throws Exception {

        CountDownLatch latch = new CountDownLatch(1);
        BusListener busListener = (sourceUuid, receivingTopic, payload) -> {
            logger.debug(String.format("Receiving %s", payload.toString()));
            // PubSubExample.html sends the following
            // fin.desktop.InterApplicationBus.publish('check-in', {name: 'Pub/Sub example app'});
            if (receivingTopic.equals("check-in")) {
                latch.countDown();
            }
        };
        subscribeToTopic("*", "*", busListener);

        ApplicationOptions options = TestUtils.getAppOptions(openfin_app_url);
        Application application = TestUtils.createApplication(options, desktopConnection);
        TestUtils.runApplication(application, true);

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(latch.getCount(), 0);
    }

    @Test
    public void wildCardTopicSelf() throws Exception {

        CountDownLatch latch = new CountDownLatch(1);
        BusListener busListener = (sourceUuid, receivingTopic, payload) -> {
            logger.debug(String.format("Receiving %s", payload.toString()));
            if (receivingTopic.equals("wildcard-self")) {
                latch.countDown();
            }
        };
        subscribeToTopic("*", "*", busListener);

        JSONObject msg = new JSONObject();
        msg.put("name", "wildCardTopicSelf");
        desktopConnection.getInterApplicationBus().publish("wildcard-self", msg);

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(latch.getCount(), 0);
    }

}
