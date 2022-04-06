package com.openfin.desktop.demo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import com.openfin.desktop.channel.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openfin.desktop.Ack;
import com.openfin.desktop.AckListener;
import com.openfin.desktop.AsyncCallback;
import com.openfin.desktop.DesktopConnection;
import com.openfin.desktop.DesktopException;
import com.openfin.desktop.DesktopStateListener;
import com.openfin.desktop.RuntimeConfiguration;

public class ChannelExample implements DesktopStateListener {

    private static Logger logger = LoggerFactory.getLogger(ChannelExample.class.getName());
    private static CountDownLatch latch = new CountDownLatch(1);
    private static String CHANNEL_NAME="ChannelExample";

    private DesktopConnection desktopConnection;

    private static String channelType;  // client or provider, if not set, both

    public ChannelExample() {
        try {
            StringBuilder sb = new StringBuilder("ChannelExample");
            if (channelType != null) {
                sb.append(channelType);
            }
            desktopConnection = new DesktopConnection(sb.toString());
            String desktopVersion = java.lang.System.getProperty("com.openfin.demo.runtime.version", "stable");
            RuntimeConfiguration configuration = new RuntimeConfiguration();
            configuration.setRuntimeVersion(desktopVersion);
            desktopConnection.connect(configuration, this, 60);
        }
        catch (Exception ex) {
            logger.error("Error launching Runtime", ex);
        }
    }

    /**
     * Create a provider that supports "getValue", "increment" and "incrementBy n" actions
     */
    public void createChannelProvider() {

        // Create the channel provider.
        desktopConnection.getChannel(CHANNEL_NAME).createAsync().thenAccept(provider -> {
            provider.addProviderListener(new ChannelProviderListener() {

                // Create the onChannelConnect event handler.
                @Override
                public void onClientConnect(ChannelClientConnectEvent connectionEvent) throws Exception {

                    // Add a line to the log file to identify the UUID of the caller.
                    logger.info(String.format("provider receives client connect event from %s ", connectionEvent.getUuid()));

                    // Extract the JSON payload.
                    JSONObject payload = (JSONObject) connectionEvent.getPayload();

                    // If the "name" element of the payload says the client is invalid, reject the request.
                    if (payload != null) {
                        String name = payload.optString("name");
                        if ("Invalid Client".equals(name)) {
                            throw new Exception("request rejected");
                        }
                    }
                }

                // Create the onChannelDisconnect event handler.
                @Override
                public void onClientDisconnect(ChannelClientConnectEvent connectionEvent) {

                    // Add a line to the log file identifying the UUID of the caller.
                    logger.info(String.format("provider receives channel disconnect event from %s ", connectionEvent.getUuid()));
                }
            });

            // The provider was created. Now to register the actions.
            // ------------------------------------------------------

            // This variable is used as the "value" element for the getValue, increment, and incrementBy actions.
            AtomicInteger localInteger = new AtomicInteger(0);

            // Register the "getValue" action.
            // This action will return the value of the localInteger variable.
            provider.register("getValue", new ChannelAction() {

                // This is the logic for the "getValue" action.
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {

                    // Write a string to the logfile that shows the requested action and payload.
                    logger.info(String.format("provider processing action %s, payload=%s", action, payload.toString()));

                    // Create a JSON object to return to the channel client.
                    JSONObject obj = new JSONObject();

                    // Set the "value" JSON element to the value of the localInteger variable.
                    obj.put("value", localInteger.get());

                    // Return the JSON object to the channel client.
                    return obj;
                }
            });

            // Register the "increment" action.
            // This action will increment the value of the localInteger variable by one.
            provider.register("increment", new ChannelAction() {

                // This is the logic for the "increment" action.
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {

                    // Write a string to the logfile that identifies the action and payload.
                    logger.info(String.format("provider processing action %s, payload=%s", action, payload.toString()));

                    // Create a JSON object to return to the channel client.
                    JSONObject obj = new JSONObject();

                    // Increment localInteger and set the "value" JSON element to the new value of localInteger.
                    obj.put("value", localInteger.incrementAndGet());
                    provider.publish("event", obj, null);

                    // Return the JSON object to the channel client.
                    return obj;
                }
            });

            // Register the "incrementBy" action.
            // This action will increment the value of the localInteger variable by a specified amount.
            provider.register("incrementBy", new ChannelAction() {

                // This is the logic for the "incrementBy" action.
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {

                    // Write a string to the logfile that identifies the action and payload.
                    logger.info(String.format("provider processing action %s, payload=%s", action, payload.toString()));

                    // Extract the increment amount (delta) from the payload JSON object.
                    int delta = ((JSONObject)payload).getInt("delta");

                    // Create a new JSON object to return to the channel client.
                    JSONObject obj = new JSONObject();

                    // Increase localInteger by the delta amount and set the "value" JSON element to the new value of localInteger.
                    obj.put("value", localInteger.addAndGet(delta));

                    // Return the new JSON object to the channel client.
                    return obj;
                }
            });
        });
    }

    /**
     * Create a channel client that invokes "getValue", "increment" and "incrementBy n" actions
     */
    public void createChannelClient() {
        JSONObject payload = new JSONObject();
        payload.put("name", "java example");
        desktopConnection.getChannel(CHANNEL_NAME).connectAsync(false, payload).thenAccept(client -> {
            client.addChannelListener(new ChannelListener() {
                @Override
                public void onChannelConnect(ConnectionEvent connectionEvent) {
                }
                @Override
                public void onChannelDisconnect(ConnectionEvent connectionEvent) {
                    logger.info("channel disconnected {}", connectionEvent.getChannelId());
                }
            });
            client.register("event", new ChannelAction() {
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
                    logger.info("channel event {}", action);
                    return null;
                }
            });

            client.dispatch("getValue", null, new AckListener() {
                @Override
                public void onSuccess(Ack ack) {
                    logger.info("current value={}", ack.getJsonObject().getJSONObject("data").getJSONObject("result").getInt("value"));

                    //got current value, do increment
                    client.dispatch("increment", null, new AckListener() {
                        @Override
                        public void onSuccess(Ack ack) {
                            logger.info("after invoking increment, value={}", ack.getJsonObject().getJSONObject("data").getJSONObject("result").getInt("value"));

                            //let's do increatmentBy 10
                            JSONObject payload = new JSONObject();
                            payload.put("delta", 10);
                            client.dispatch("incrementBy", payload, new AckListener() {
                                @Override
                                public void onSuccess(Ack ack) {
                                    logger.info("after invoking incrementBy, value={}", ack.getJsonObject().getJSONObject("data").getJSONObject("result").getInt("value"));

                                    try {
                                        desktopConnection.disconnect();
                                    } catch (DesktopException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onError(Ack ack) {
                                }
                            });
                        }

                        @Override
                        public void onError(Ack ack) {
                        }
                    });
                }

                @Override
                public void onError(Ack ack) {
                }
            });
        });
    }

    @Override
    public void onReady() {
        if ("provider".equals(channelType) || channelType == null) {
            createChannelProvider();
        }
        if ("client".equals(channelType) || channelType == null) {
            createChannelClient();
        }
    }

    @Override
    public void onClose(String error) {
        logger.info("onClose, value={}", error);
        latch.countDown();
    }

    @Override
    public void onError(String reason) {
        logger.info("onError, value={}", reason);
    }

    @Override
    public void onMessage(String message) {

    }

    @Override
    public void onOutgoingMessage(String message) {

    }

    public static void main(String[] args) {
        if (args.length > 0) {
            channelType = args[0];
        }
        try {
            new ChannelExample();
            latch.await();
        }
        catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        java.lang.System.exit(0);
    }
}
