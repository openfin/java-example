package com.openfin.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openfin.desktop.channel.ChannelAction;
import com.openfin.desktop.channel.ChannelClient;
import com.openfin.desktop.channel.ChannelListener;
import com.openfin.desktop.channel.ChannelProvider;
import com.openfin.desktop.channel.ConnectionEvent;
import com.openfin.desktop.channel.Middleware;

/**
 * JUnit tests for com.openfin.desktop.channel.Channel class
 */
public class ChannelTest {
	private static Logger logger = LoggerFactory.getLogger(ChannelTest.class.getName());

	private static final String DESKTOP_UUID = ChannelTest.class.getName();
	private static DesktopConnection desktopConnection;

	@BeforeClass
	public static void setup() throws Exception {
		desktopConnection = TestUtils.setupConnection(DESKTOP_UUID);
	}

	@AfterClass
	public static void teardown() throws Exception {
		TestUtils.teardownDesktopConnection(desktopConnection);
	}

	@Test
	public void createChannelProvider() throws Exception {
		ChannelProvider provider = desktopConnection.getChannel().create("createChannelProviderTest").get();
		assertNotNull(provider);
	}

	@Test
	public void createChannelClient() throws Exception {
		final String channelName = "createChannelClientTest";
		
		ChannelProvider provider = desktopConnection.getChannel().create(channelName).get();
		
		ChannelClient client = desktopConnection.getChannel().connect(channelName).get();

		assertNotNull(client);
	}

	@Test
	public void registerAction() throws Exception {
		final String channelName = "registerActionTest";
		AtomicBoolean registered = new AtomicBoolean(false);
		
		desktopConnection.getChannel().create(channelName).thenAccept(provider->{
			registered.set(provider.register("currentTime", new ChannelAction() {
				@Override
				public JSONObject invoke(String action, JSONObject payload) {
					return payload.put("currentTime", java.lang.System.currentTimeMillis());
				}
			}));
		}).get();
		
		assertEquals(true, registered.get());
	}
	
	@Test
	public void invokeProviderAction() throws Exception {
		final String channelName = "invokeProviderActionTest";
		final String actionName = "increment";
		final int initValue = 10;

		CompletableFuture<Ack> ackFuture = desktopConnection.getChannel().create(channelName).thenAccept(provider -> {
			provider.register(actionName, new ChannelAction() {
				@Override
				public JSONObject invoke(String action, JSONObject payload) {
					int currentValue = payload.getInt("value");
					return payload.put("value", currentValue + 1);
				}
			});
		}).thenApply(v -> {
			try {
				ChannelClient client = desktopConnection.getChannel().connect(channelName).get();
				JSONObject payload = new JSONObject();
				payload.put("value", initValue);
				return client.dispatch(actionName, payload).get();
			}
			catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}

		});

		Ack ack = ackFuture.get();
		int resultValue = ack.getJsonObject().getJSONObject("data").getJSONObject("result").getInt("value");
				
		assertEquals(initValue + 1, resultValue);
	}

	@Test
	public void publishToClient() throws Exception {
		final String channelName = "publishToClientTest";
		final String actionName = "message";
		final String actionMessage = "actionMessage";

		CountDownLatch latch = new CountDownLatch(1);
		desktopConnection.getChannel().create(channelName).thenAccept(provider->{
			desktopConnection.getChannel().addChannelListener(new ChannelListener() {
				@Override
				public void onChannelConnect(ConnectionEvent connectionEvent) {
					// once the channel is connected, invoke publish method
					JSONObject payload = new JSONObject();
					payload.put("message", actionMessage);
					provider.publish(actionName, payload);
				}

				@Override
				public void onChannelDisconnect(ConnectionEvent connectionEvent) {

				}
			});

			desktopConnection.getChannel().connect(channelName).thenAccept(client -> {
				client.register(actionName, new ChannelAction() {
					@Override
					public JSONObject invoke(String action, JSONObject payload) {
						if (actionName.equals(action) && actionMessage.equals(payload.getString("message"))) {
							latch.countDown();
						}
						return null;
					}
				});
			});
		});

		latch.await(10, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
	}

	@Test
	public void connectionListener() throws Exception {
		final String channelName = "connectionListenerTest";
		CountDownLatch latch = new CountDownLatch(2);

		desktopConnection.getChannel().create(channelName).thenAccept(provider -> {
			desktopConnection.getChannel().addChannelListener(new ChannelListener() {
				@Override
				public void onChannelConnect(ConnectionEvent connectionEvent) {
					latch.countDown();
				}

				@Override
				public void onChannelDisconnect(ConnectionEvent connectionEvent) {
					latch.countDown();
				}
			});

		}).get();

		desktopConnection.getChannel().connect(channelName).thenAccept(client->{
			desktopConnection.getChannel().disconnect(client);
		}).get();

		
		latch.await(10, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
	}

	@Test
	public void middlewareAction() throws Exception {
		final String channelName = "middlewateActionTest";
		final String actionName = "increment";
		final int initValue = 10;
		final int middlewareIncrement = 2;

		CompletableFuture<Ack> ackFuture = desktopConnection.getChannel().create(channelName).thenAccept(provider -> {
			provider.setBeforeAction(new Middleware() {

				@Override
				public JSONObject invoke(String action, JSONObject payload, JSONObject senderId) {
					if (actionName.equals(action)) {
						int value = payload.getInt("value");
						payload.put("value", value + middlewareIncrement);
					}
					return payload;
				}
			});

			provider.register(actionName, new ChannelAction() {
				@Override
				public JSONObject invoke(String action, JSONObject payload) {
					int currentValue = payload.getInt("value");
					return payload.put("value", currentValue + 1);
				}
			});
		}).thenApply((v) -> {
			try {
				return desktopConnection.getChannel().connect(channelName).thenApply(client -> {
					JSONObject payload = new JSONObject();
					payload.put("value", initValue);
					try {
						return client.dispatch(actionName, payload).get();
					}
					catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException(e);
					}
				}).get();
			}
			catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		});

		Ack ack = ackFuture.get();

		int resultValue = ack.getJsonObject().getJSONObject("data").getJSONObject("result").getInt("value");

		assertEquals(initValue + 3, resultValue);
	}

}
