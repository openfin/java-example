/**
 * Example for Channel API in Java adapter
 *
 * This example can run as a channel Provider or channel Client.  In both cases,  it needs a OpenFin app to demonstrate how channel APIs can be used for communication
 *
 * As a channel provider:
 *
 *  1. start this example with 'provider' as an argument
 *  2. start an OpenFin app and run the following javascript, which creates channel client and sends actions to this example to process
 *
 *	    const client = await fin.InterApplicationBus.Channel.connect('ChannelExample', { protocols: ['classic'] });  // replace 'classic' with 'rtc' for webrtc based channels
 * 	    const value = await client.dispatch('getValue');  // 'getValue' is processed by a channel provider created by Java example
 * 	    console.log(value);
 *
 * As a channel client:
 *
 *  1. start an OpenFin app and run the following javascript, which creates channel provider and waits for connection from Java example
 *
 *      const providerBus = await fin.InterApplicationBus.Channel.create('ChannelExample', { protocols: ['rtc', 'classic'] });
 *      provider.onConnection((identity, payload) => {
 *              console.log('Client connection request identity: ', JSON.stringify(identity));
 *              console.log('Client connection request payload: ', JSON.stringify(payload));
 *              });
 *      provider.onDisconnection(evt => {
 *              console.log('Client disconnected', `uuid: ${evt.uuid}, name: ${evt.name}`);
 *      });
 *
 *      let value = 123;
 *      provider.register('getValue', (payload, identity) => {
 *          console.log('Action dispatched by client: ', JSON.stringify(identity));
 *          console.log('Payload sent in dispatch: ', JSON.stringify(payload));
 *          return {
 *              value: value
 *          };
 *      });
 *      providerBus.register('increment', (payload, identity) => {
 *          console.log('Action dispatched by client: ', JSON.stringify(identity));
 *          console.log('Payload sent in dispatch: ', JSON.stringify(payload));
 * 	        value = value + payload.value;
 *          return {
 *            value: value
 *          };
 *      });
 *
 *  2. start this example with 'client' as an argument
 *
 *  Channel API support Channel.CLASSIC_PROTOCOL and  Channel.RTC_PROTOCOL.  Version 24.96.67.4+ of Runtime is required for Channel.RTC_PROTOCOL.
 *
 */
package com.openfin.desktop.demo;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import com.openfin.desktop.channel.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openfin.desktop.DesktopConnection;
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
        desktopConnection.getChannel(CHANNEL_NAME).addChannelListener(new ChannelListener() {
            @Override
            public void onChannelConnect(ConnectionEvent connectionEvent) {
                logger.info(String.format("provider receives channel connect event from %s ", connectionEvent.getUuid()));
            }
            @Override
            public void onChannelDisconnect(ConnectionEvent connectionEvent) {
                logger.info(String.format("provider receives channel disconnect event from %s ", connectionEvent.getUuid()));
            }
        });
        desktopConnection.getChannel(CHANNEL_NAME).createAsync(Arrays.asList(Channel.RTC_PROTOCOL, Channel.CLASSIC_PROTOCOL)).thenAccept(provider -> {
            AtomicInteger x = new AtomicInteger(0);
            provider.register("getValue", new ChannelAction() {
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
                    logger.info(String.format("provider processing action %s", action));
                    JSONObject obj = new JSONObject();
                    obj.put("value", x.get());
                    return obj;
                }
            });
            provider.register("increment", new ChannelAction() {
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
                    logger.info(String.format("provider processing action %s, payload=%s", action, payload.toString()));
                    JSONObject obj = new JSONObject();
                    obj.put("value", x.incrementAndGet());
                    return obj;
                }
            });
            provider.register("incrementBy", new ChannelAction() {
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
                    logger.info(String.format("provider processing action %s, payload=%s", action, payload.toString()));
                    int delta = ((JSONObject)payload).getInt("delta");
                    JSONObject obj = new JSONObject();
                    obj.put("value", x.addAndGet(delta));
                    return obj;
                }
            });
        });
    }

    /**
     * Create a channel client that invokes "getValue", "increment" and "incrementBy n" actions
     */
    public void createChannelClient() {
        desktopConnection.getChannel(CHANNEL_NAME).connectAsync(Channel.RTC_PROTOCOL).thenAccept(client -> {
            client.register("event", new ChannelAction() {
                @Override
                public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
                    logger.info("channel event {}", action);
                    return null;
                }
            });
            client.dispatchAsync("getValue", null).thenAccept(ack -> {
                if (ack.isSuccessful()) {
                    logger.info("current value={}", ack.getJsonObject().getJSONObject("data").getJSONObject("result").getInt("value"));
                    //let's do increment by 10
                    JSONObject payload = new JSONObject();
                    payload.put("value", 10);
                    client.dispatchAsync("increment", payload).thenAccept(incAck -> {
                        if (incAck.isSuccessful()) {
                            logger.info("after invoking incrementBy, value={}", incAck.getJsonObject().getJSONObject("data").getJSONObject("result").getInt("value"));
                        }
                    });
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