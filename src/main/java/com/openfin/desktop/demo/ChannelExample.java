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

    public ChannelExample() {
        try {
            desktopConnection = new DesktopConnection("ChannelExample");
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
        desktopConnection.getChannel().addChannelListener(new ChannelListener() {
            @Override
            public void onChannelConnect(ConnectionEvent connectionEvent) {
                logger.info(String.format("provider receives channel connect event from %s ", connectionEvent.getUuid()));
            }
            @Override
            public void onChannelDisconnect(ConnectionEvent connectionEvent) {
                logger.info(String.format("provider receives channel disconnect event from %s ", connectionEvent.getUuid()));
            }
        });
        desktopConnection.getChannel().create(CHANNEL_NAME, new AsyncCallback<ChannelProvider>() {
            @Override
            public void onSuccess(ChannelProvider provider) {
                //provider created, register actions.
                AtomicInteger x = new AtomicInteger(0);

                provider.register("getValue", new ChannelAction() {
                    @Override
                    public JSONObject invoke(String action, JSONObject payload) {
                        logger.info(String.format("provider processing action %s, payload=%s", action, payload.toString()));
                        JSONObject obj = new JSONObject();
                        obj.put("value", x.get());
                        return obj;
                    }
                });
                provider.register("increment", new ChannelAction() {
                    @Override
                    public JSONObject invoke(String action, JSONObject payload) {
                        logger.info(String.format("provider processing action %s, payload=%s", action, payload.toString()));
                        JSONObject obj = new JSONObject();
                        obj.put("value", x.incrementAndGet());
                        return obj;
                    }
                });
                provider.register("incrementBy", new ChannelAction() {
                    @Override
                    public JSONObject invoke(String action, JSONObject payload) {
                        logger.info(String.format("provider processing action %s, payload=%s", action, payload.toString()));
                        int delta = payload.getInt("delta");
                        JSONObject obj = new JSONObject();
                        obj.put("value", x.addAndGet(delta));
                        return obj;
                    }
                });
            }
        });
    }

    /**
     * Create a channel client that invokes "getValue", "increment" and "incrementBy n" actions
     */
    public void createChannelClient() {
        desktopConnection.getChannel().connect(CHANNEL_NAME, new AsyncCallback<ChannelClient>() {
            @Override
            public void onSuccess(ChannelClient client) {
                //connected to provider, invoke actions provided by the provider.
                //get current value
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
                                        }
                                        catch (DesktopException e) {
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
            }

        });
    }

    @Override
    public void onReady() {
        createChannelProvider();
        createChannelClient();
    }

    @Override
    public void onClose(String error) {
        latch.countDown();
    }

    @Override
    public void onError(String reason) {

    }

    @Override
    public void onMessage(String message) {

    }

    @Override
    public void onOutgoingMessage(String message) {

    }

    public static void main(String[] args) {

        try {
            new ChannelExample();
            latch.await();
        }
        catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}