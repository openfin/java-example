package com.openfin.desktop;

import static org.junit.Assert.assertEquals;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.openfin.desktop.channel.*;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit tests for com.openfin.desktop.Channel class
 *
 *
 * Created by wche on 1/27/16.
 *
 */
public class ChannelTest {
	private static Logger logger = LoggerFactory.getLogger(ChannelTest.class.getName());

	private static final String DESKTOP_UUID = ChannelTest.class.getName();
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
	public void createChannelProvider() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		desktopConnection.getChannel("createChannelProviderTest").createAsync().thenAccept(provider -> {
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
		final String channelName = "createChannelClientTest";
		desktopConnection.getChannel(channelName).createAsync().thenAccept(provider -> {
			desktopConnection.getChannel(channelName).connectAsync().thenAccept(client -> {
				latch.countDown();
			});
		});
		latch.await(10, TimeUnit.SECONDS);
		assertEquals(0, latch.getCount());
	}
	
	@Test
	public void multipleChannelClients() throws Exception {
		CountDownLatch latch1 = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(1);
		final String channelName = "createMultipleChannelClientTest";
		final String clientActionName = "clientAction";
		final String providerActionName = "providerAction";
		desktopConnection.getChannel(channelName).createAsync().thenAccept(provider -> {
			provider.register(providerActionName, new ChannelAction() {
				@Override
				public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
					return null;
				}
			});
			//first channel client
			desktopConnection.getChannel(channelName).connectAsync().thenAccept(client -> {
				client.dispatch(providerActionName, new JSONObject(), null);
				client.register(clientActionName, new ChannelAction() {
					@Override
					public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
						latch1.countDown();
						return null;
					}
				});
			});
			//second channel client
			desktopConnection.getChannel(channelName).connectAsync().thenAccept(client -> {
				client.register(clientActionName, new ChannelAction() {
					@Override
					public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
						latch2.countDown();
						return null;
					}
				});
				provider.publish(clientActionName, new JSONObject(), null);
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
		desktopConnection.getChannel(channelName).createAsync().thenAccept(provider -> {
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
	
	@Test
	public void invokeProviderAction() throws Exception {
		final String channelName = "invokeProviderActionTest";
		final String actionName = "increment";
		final int initValue = 10;
		final AtomicInteger resultValue = new AtomicInteger(-1);

		CountDownLatch latch = new CountDownLatch(1);
		desktopConnection.getChannel(channelName).createAsync().thenAccept(provider -> {
			provider.register(actionName, new ChannelAction() {
				@Override
				public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
					int currentValue = ((JSONObject)payload).getInt("value");
					return ((JSONObject)payload).put("value", currentValue + 1);
				}
			});
			desktopConnection.getChannel(channelName).connectAsync().thenAccept(client -> {
				JSONObject payload = new JSONObject();
				payload.put("value", initValue);
				client.dispatch(actionName, payload, new AckListener() {
					@Override
					public void onSuccess(Ack ack) {
						resultValue.set(ack.getJsonObject().getJSONObject("data").getJSONObject("result")
								.getInt("value"));
						latch.countDown();
					}
					@Override
					public void onError(Ack ack) {
					}
				});
			});
		});



		latch.await(10, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
		assertEquals(initValue + 1, resultValue.get());
	}
	
	@Test
	public void invokeClientAction() throws Exception {
		final String channelName = "invokeClientActionTest";
		final String providerActionName = "invokeClientAction";
		final String clientActionName = "clientAction";

		CountDownLatch latch = new CountDownLatch(1);
		desktopConnection.getChannel(channelName).createAsync().thenAccept(provider -> {
			provider.register(providerActionName, new ChannelAction() {
				@Override
				public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
					provider.dispatch(senderIdentity, clientActionName, new JSONObject(), null);
					return null;
				}
			});

			desktopConnection.getChannel(channelName).connectAsync().thenAccept(client -> {
				client.register(clientActionName, new ChannelAction() {
					@Override
					public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
						latch.countDown();
						return null;
					}
				});
				client.dispatch(providerActionName, new JSONObject(), null);
			});
		});

		latch.await(10, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
	}

	@Test
	public void publishToClient() throws Exception {
		final String channelName = "publishToClientTest";
		final String actionName = "message";
		final String actionMessage = "actionMessage";

		CountDownLatch latch = new CountDownLatch(1);
		desktopConnection.getChannel(channelName).createAsync().thenAccept(provider -> {
					desktopConnection.getChannel(channelName).connectAsync().thenAccept(client -> {
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
						provider.publish(actionName, payload, null);
					});
				});
		latch.await(10, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
	}

	@Test
	public void connectionListener() throws Exception {
		final String channelName = "connectionListenerTest";
		CountDownLatch latch = new CountDownLatch(3);

		desktopConnection.getChannel(channelName).createAsync().thenAccept(provider -> {
			provider.addProviderListener(new ChannelProviderListener() {
				@Override
				public void onClientConnect(ChannelClientConnectEvent connectionEvent) throws Exception {
					latch.countDown();
				}
				@Override
				public void onClientDisconnect(ChannelClientConnectEvent connectionEvent) {
					latch.countDown();
				}
			});
			desktopConnection.getChannel(channelName).connectAsync().thenAccept(client -> {
				client.addChannelListener(new ChannelListener() {
					@Override
					public void onChannelConnect(ConnectionEvent connectionEvent) {
					}
					@Override
					public void onChannelDisconnect(ConnectionEvent connectionEvent) {
						latch.countDown();
					}
				});
				client.disconnect();
			});
		});

		latch.await(10, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
	}

	@Test
	public void middlewareAction() throws Exception {
		final String channelName = "middlewateActionTest";
		final String actionName = "increment";
		final int initValue = 10;
		final int middlewareIncrement = 2;
		final AtomicInteger resultValue = new AtomicInteger(-1);

		CountDownLatch latch = new CountDownLatch(1);
		desktopConnection.getChannel(channelName).createAsync().thenAccept(provider -> {
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
			desktopConnection.getChannel(channelName).connectAsync().thenAccept(client -> {
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

	@Test
	public void rejectConnection() throws Exception {
		final String channelName = "rejectConnectionTest";
		final String payloadValue = "reject me";
		final String rejectReason = "not allowed";

		CountDownLatch latch = new CountDownLatch(1);

		desktopConnection.getChannel(channelName).createAsync().thenAccept(provider -> {
			provider.addProviderListener(new ChannelProviderListener() {
				@Override
				public void onClientConnect(ChannelClientConnectEvent connectionEvent) throws Exception {
					String payload = (String) connectionEvent.getPayload();
					if (payloadValue.equals(payload)) {
						throw new Exception(rejectReason);
					}
				}
				@Override
				public void onClientDisconnect(ChannelClientConnectEvent connectionEvent) {
				}
			});

			desktopConnection.getChannel(channelName).connectAsync(payloadValue).thenAccept(client -> {
				if (Objects.isNull(client)) {
					latch.countDown();
				}
			}).exceptionally(ex -> {
				if (ex.getMessage().contains(rejectReason)) {
					latch.countDown();
				}
				return null;
			});
		});

		latch.await(10, TimeUnit.SECONDS);
		assertEquals(0, latch.getCount());
	}


}
