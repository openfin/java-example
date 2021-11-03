package com.openfin.desktop;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
 * JUnit tests for com.openfin.desktop.InterApplicationBus class
 *
 * Test cases in this class need to have access to an OpenFin HTML5 app to
 * verify sub/pub workflow. Sources for the app can be found in release
 * directory: PubSubExample.html. It is hosted by OpenFin at
 * https://cdn.openfin.co/examples/junit/PubSubExample.html
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
		desktopConnection.getChannel("createChannelProviderTest").create(new AsyncCallback<ChannelProvider>() {
			@Override
			public void onSuccess(ChannelProvider provider) {
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
		desktopConnection.getChannel(channelName).create(new AsyncCallback<ChannelProvider>() {
			@Override
			public void onSuccess(ChannelProvider provider) {
				desktopConnection.getChannel(channelName).connect(new AsyncCallback<ChannelClient>() {
					@Override
					public void onSuccess(ChannelClient result) {
						latch.countDown();
					}
				});
			}
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
		desktopConnection.getChannel(channelName).create(new AsyncCallback<ChannelProvider>() {
			@Override
			public void onSuccess(ChannelProvider provider) {
				
				provider.register(providerActionName, new ChannelAction() {

					@Override
					public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
						return null;
					}
				});

				//first channel client
				desktopConnection.getChannel(channelName).connect(new AsyncCallback<ChannelClient>() {
					@Override
					public void onSuccess(ChannelClient client) {
						client.dispatch(providerActionName, new JSONObject(), null);
						
						client.register(clientActionName, new ChannelAction() {
							@Override
							public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
								latch1.countDown();
								return null;
							}
						});
					}
				});
				//second channel client
				desktopConnection.getChannel(channelName).connect(new AsyncCallback<ChannelClient>() {
					@Override
					public void onSuccess(ChannelClient client) {
						client.register(clientActionName, new ChannelAction() {
							@Override
							public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
								latch2.countDown();
								return null;
							}
						});
						
						provider.publish(clientActionName, new JSONObject(), null);
					}
				});
			}
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
		desktopConnection.getChannel(channelName).create(new AsyncCallback<ChannelProvider>() {
			@Override
			public void onSuccess(ChannelProvider provider) {
				provider.register("currentTime", new ChannelAction() {
					@Override
					public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
						return ((JSONObject)payload).put("currentTime", java.lang.System.currentTimeMillis());
					}
				});
				latch.countDown();
			}
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
		desktopConnection.getChannel(channelName).create(new AsyncCallback<ChannelProvider>() {
			@Override
			public void onSuccess(ChannelProvider provider) {
				provider.register(providerActionName, new ChannelAction() {
					@Override
					public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
						provider.dispatch(senderIdentity, clientActionName, new JSONObject(), null);
						return null;
					}
				});

				desktopConnection.getChannel(channelName).connect(new AsyncCallback<ChannelClient>() {
					@Override
					public void onSuccess(ChannelClient client) {
						client.register(clientActionName, new ChannelAction() {
							@Override
							public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
								latch.countDown();
								return null;
							}
						});

						client.dispatch(providerActionName, new JSONObject(), null);
					}
				});
			}
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
		desktopConnection.getChannel(channelName).create(new AsyncCallback<ChannelProvider>() {
			@Override
			public void onSuccess(ChannelProvider provider) {
				desktopConnection.getChannel(channelName).connect(new AsyncCallback<ChannelClient>() {
					@Override
					public void onSuccess(ChannelClient client) {

						client.register(actionName, new ChannelAction() {
							@Override
							public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
								if (actionName.equals(action) && actionMessage.equals(((JSONObject)payload).getString("message"))) {
									latch.countDown();
								}
								return null;
							}
						});
						
						JSONObject payload = new JSONObject();
						payload.put("message", actionMessage);
						provider.publish(actionName, payload, null);
					}
				});
			}
		});

		latch.await(10, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
	}

	@Test
	public void connectionListener() throws Exception {
		final String channelName = "connectionListenerTest";
		CountDownLatch latch = new CountDownLatch(2);

		desktopConnection.getChannel(channelName).create(new AsyncCallback<ChannelProvider>() {
			@Override
			public void onSuccess(ChannelProvider provider) {
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

				desktopConnection.getChannel(channelName).connectAsync().thenAccept(client -> {
					client.disconnect();
				});
			}
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
		desktopConnection.getChannel(channelName).create(new AsyncCallback<ChannelProvider>() {
			@Override
			public void onSuccess(ChannelProvider provider) {
				provider.setBeforeAction(new Middleware() {

					@Override
					public Object invoke(String action, Object payload, JSONObject senderId) {
						if (actionName.equals(action)) {
							int value = ((JSONObject)payload).getInt("value");
							((JSONObject)payload).put("value", value + middlewareIncrement);
						}
						return payload;
					}});
				
				provider.register(actionName, new ChannelAction() {
					@Override
					public JSONObject invoke(String action, Object payload, JSONObject senderIdentity) {
						int currentValue = ((JSONObject)payload).getInt("value");
						return ((JSONObject) payload).put("value", currentValue + 1);
					}
				});

				desktopConnection.getChannel(channelName).connect(new AsyncCallback<ChannelClient>() {

					@Override
					public void onSuccess(ChannelClient client) {
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
					}

				});
			}
		});

		latch.await(10, TimeUnit.SECONDS);

		assertEquals(0, latch.getCount());
		assertEquals(initValue + 3, resultValue.get());
	}

}
