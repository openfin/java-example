package com.openfin.desktop;

/**
 * JUnit tests for com.openfin.desktop.Channel class with support for rtc and classic protocols
 *
 *
 * Created by wche on 11/02/2021.
 *
 */

import com.openfin.desktop.channel.*;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class RtcChannelTest {

    private static final Logger logger = LoggerFactory.getLogger(ChannelTest.class.getName());

    private static final String DESKTOP_UUID = ChannelTest.class.getName();
    private static DesktopConnection desktopConnection;
    private static final List<ProtocolOptions> AllProtocols = Arrays.asList(Channel.RTC_PROTOCOL, Channel.CLASSIC_PROTOCOL);
    private static final String JS_CHANNEL_NAME = "adapter-channel-test-js";  // provider on js side
    private static final String ADAPTER_CHANNEL_NAME = "adapter-channel-test-adapter";  // provider on adapter side
    private static final String GET_VALUE_ACTION = "getValue";
    private static final String CLIENT_ACTION = "hellToClient";  // registered by channel client
    private static final String TRIGGER_CLIENT_ACTION = "triggerClientAction";  //  when ready, client send this to trigger a provider to send CLIENT_ACTION

    @BeforeClass
    public static void setup() throws Exception {
        logger.debug("starting");
        RuntimeConfiguration cfg = new RuntimeConfiguration();
//        cfg.setManifestLocation("https://testing-assets.openfin.co/adapters/channel/app.json");
		cfg.setManifestLocation("http://localhost:5555/app.json");
        desktopConnection = TestUtils.setupConnection(DESKTOP_UUID, cfg);
    }

    @AfterClass
    public static void teardown() throws Exception {
        TestUtils.teardownDesktopConnection(desktopConnection);
    }

    @Test
    public void createChannelProvider() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        desktopConnection.getChannel("createChannelProvider").createAsync(AllProtocols).thenAccept(provider -> {
            if (Objects.nonNull(provider)) {
                latch.countDown();
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void createChannelClient() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        desktopConnection.getChannel(JS_CHANNEL_NAME).connectAsync(Channel.RTC_PROTOCOL).thenAccept(client -> {
            if (Objects.nonNull(client)) {
                latch.countDown();
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void multipleChannelClients() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        //first channel client
        desktopConnection.getChannel(JS_CHANNEL_NAME).connectAsync(Channel.CLASSIC_PROTOCOL).thenAccept(client -> {
            client.register(CLIENT_ACTION, new ChannelAction() {
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
                    latch1.countDown();
                    return null;
                }
            });
        });
        //second channel client
        desktopConnection.getChannel(JS_CHANNEL_NAME).connectAsync(Channel.RTC_PROTOCOL).thenAccept(client -> {
            client.register(CLIENT_ACTION, new ChannelAction() {
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
                    latch2.countDown();
                    return null;
                }
            });
            client.dispatchAsync(TRIGGER_CLIENT_ACTION, new JSONObject()).thenAccept(ack -> {
            });
        });

        latch1.await(10, TimeUnit.SECONDS);
        latch2.await(10, TimeUnit.SECONDS);

        assertEquals(0, latch1.getCount());
        assertEquals(0, latch2.getCount());
    }

    @Test
    public void registerAction() throws Exception {
        final String channelName = "registerActionTest";
        CountDownLatch latch = new CountDownLatch(1);
        desktopConnection.getChannel(channelName).createAsync(AllProtocols).thenAccept(provider -> {
            provider.register("currentTime", new ChannelAction() {
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
                    return ((JSONObject)payload).put("currentTime", java.lang.System.currentTimeMillis());
                }
            });
            latch.countDown();
        });
        latch.await(10, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    /**
     * test with provider on adapter side and client on js side
     *
     * @throws Exception
     */
    @Test
    public void invokeProviderAction() throws Exception {
        final CountDownLatch latch = new CountDownLatch(3);
        final String value = UUID.randomUUID().toString();

        desktopConnection.getChannel(ADAPTER_CHANNEL_NAME).addChannelListener(new ChannelListener() {
            @Override
            public void onChannelConnect(ConnectionEvent connectionEvent) {
                latch.countDown();
            }
            @Override
            public void onChannelDisconnect(ConnectionEvent connectionEvent) {
            }
        });
        desktopConnection.getChannel(ADAPTER_CHANNEL_NAME).createAsync(AllProtocols).thenAccept(provider -> {
            provider.register(GET_VALUE_ACTION, new ChannelAction() {
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
                    latch.countDown();
                    JSONObject res = new JSONObject();
                    res.put("value", 10);

                    JSONObject clientAction = new JSONObject();
                    clientAction.put("value", value);
                    provider.dispatchAsync(senderIdentity, "client-action", clientAction).thenAccept(ack -> {
                        String retValue = ack.getJsonObject().getString("value");
                        if (Objects.equals(value, retValue)) {
                            latch.countDown();
                        }
                    });
                    return res;
                }
            });
        });

        latch.await(30, TimeUnit.SECONDS);

        assertEquals(0, latch.getCount());
    }

    /**
     * test client action for both provider and client created by adapter
     * @throws Exception
     */
    @Test
    public void invokeClientAction() throws Exception {
        final String channelName = "invokeClientActionTest";
        final String providerActionName = "invokeClientAction";
        final String clientActionName = "clientAction";

        CountDownLatch latch = new CountDownLatch(1);
        desktopConnection.getChannel(channelName).createAsync(AllProtocols).thenAccept(provider -> {
            provider.register(providerActionName, new ChannelAction() {
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
                    provider.dispatchAsync(senderIdentity, clientActionName, new JSONObject()).thenAccept(ack -> {
                        if (!ack.isSuccessful()) {
                            logger.error("Error dispatching message");
                        }
                    });
                    return null;
                }
            });

            desktopConnection.getChannel(channelName).connectAsync(Channel.RTC_PROTOCOL).thenAccept(client -> {
                client.register(clientActionName, new ChannelAction() {
                    @Override
                    public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
                        latch.countDown();
                        return null;
                    }
                });
                client.dispatchAsync(providerActionName, new JSONObject()).thenAccept(ack -> {
                    if (!ack.isSuccessful()) {
                        logger.error("Error dispatching message");
                    }
                });
            });
        });

        latch.await(10, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    /**
     * test client action for provider created by js and client created by adapter
     * @throws Exception
     */
    @Test
    public void invokeClientActionFromJS() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        desktopConnection.getChannel(JS_CHANNEL_NAME).connectAsync(Channel.RTC_PROTOCOL).thenAccept(client -> {
            client.register(CLIENT_ACTION, new ChannelAction() {
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
                    if (Objects.equals("Hello", payload.toString())) {
                        latch.countDown();
                    }
                    return null;
                }
            });
            client.dispatchAsync(TRIGGER_CLIENT_ACTION, new JSONObject()).thenAccept(ack -> {
                if (ack.isSuccessful()) {
                    latch.countDown();
                }
            });
        });

        latch.await(10, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    /**
     * test publish with both provider and client created by adapter
     * @throws Exception
     */
    @Test
    public void publishToClient() throws Exception {
        final String channelName = "publishToClientTest";
        final String actionName = "message";
        final String actionMessage = "actionMessage";

        CountDownLatch latch = new CountDownLatch(1);
        desktopConnection.getChannel(channelName).createAsync(AllProtocols).thenAccept(provider -> {
            desktopConnection.getChannel(channelName).connectAsync(Channel.RTC_PROTOCOL).thenAccept(client -> {
                client.register(actionName, new ChannelAction() {
                    @Override
                    public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
                        if (actionName.equals(action) && actionMessage.equals(((JSONObject) payload).getString("message"))) {
                            latch.countDown();
                        }
                        return null;
                    }
                });

                JSONObject payload = new JSONObject();
                payload.put("message", actionMessage);
                provider.publishAsync(actionName, payload);
            });
        });
        latch.await(10, TimeUnit.SECONDS);

        assertEquals(0, latch.getCount());
    }

    /**
     * test connect event with both provider and client created by adapter
     * @throws Exception
     */
    @Test
    public void connectionListener() throws Exception {
        final String channelName = "connectionListenerTest";
        CountDownLatch latch = new CountDownLatch(2);

        desktopConnection.getChannel(channelName).createAsync(AllProtocols).thenAccept(provider -> {
            desktopConnection.getChannel(channelName).addChannelListener(new ChannelListener() {
                @Override
                public void onChannelConnect(ConnectionEvent connectionEvent) {
                    latch.countDown();
                }

                @Override
                public void onChannelDisconnect(ConnectionEvent connectionEvent) {
                    latch.countDown();
                }
            });

            desktopConnection.getChannel(channelName).connectAsync(Channel.RTC_PROTOCOL).thenAccept(client -> {
                client.disconnect();
            });
        });

        latch.await(10, TimeUnit.SECONDS);

        assertEquals(0, latch.getCount());
    }

    /**
     * test middle with both provider and client created by adapter
     * @throws Exception
     */
    @Test
    public void middlewareAction() throws Exception {
        final String channelName = "middlewateActionTest";
        final String actionName = "increment";
        final int initValue = 10;
        final int middlewareIncrement = 2;
        final AtomicInteger resultValue = new AtomicInteger(-1);

        CountDownLatch latch = new CountDownLatch(1);
        desktopConnection.getChannel(channelName).createAsync(AllProtocols).thenAccept(provider -> {
            provider.setBeforeAction(new Middleware() {
                @Override
                public Object invoke(String action, Object payload, JSONObject senderId) {
                    if (actionName.equals(action)) {
                        int value = ((JSONObject)payload).getInt("value");
                        ((JSONObject)payload).put("value", value + middlewareIncrement);
                    }
                    return payload;
                }
            });
            provider.register(actionName, new ChannelAction() {
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
                    int currentValue = ((JSONObject)payload).getInt("value");
                    return ((JSONObject) payload).put("value", currentValue + 1);
                }
            });
            desktopConnection.getChannel(channelName).connectAsync(Channel.RTC_PROTOCOL).thenAccept(client -> {
                JSONObject payload = new JSONObject();
                payload.put("value", initValue);
                client.dispatchAsync(actionName, payload).thenAccept(ack -> {
                    if (ack.isSuccessful()) {
                        resultValue.set(ack.getJsonObject().getJSONObject("data").getJSONObject("result")
                                .getInt("value"));
                        latch.countDown();
                    }
                });
            });
        });

        latch.await(10, TimeUnit.SECONDS);

        assertEquals(0, latch.getCount());
        assertEquals(initValue + 3, resultValue.get());
    }

}
