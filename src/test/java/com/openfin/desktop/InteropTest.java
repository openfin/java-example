package com.openfin.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.openfin.desktop.interop.Intent;
import com.openfin.desktop.interop.InteropClient;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openfin.desktop.interop.Context;
import com.openfin.desktop.interop.ContextGroupInfo;

public class InteropTest {
	private static Logger logger = LoggerFactory.getLogger(InteropTest.class.getName());

	private static final String DESKTOP_UUID = InteropTest.class.getName();
	private static final String BROKER_NANE = "AdapterInteropTest";  // created by javascript side
	private static DesktopConnection desktopConnection;

	@BeforeClass
	public static void setup() throws Exception {
		logger.debug("starting");
		RuntimeConfiguration cfg = new RuntimeConfiguration();
		cfg.setManifestLocation("https://testing-assets.openfin.co/adapters/interop/app.json");
//		cfg.setManifestLocation("http://localhost:5555/app.json");
		desktopConnection = TestUtils.setupConnection(DESKTOP_UUID, cfg);
	}

	@AfterClass
	public static void teardown() throws Exception {
		OpenFinRuntime runtime = new OpenFinRuntime(desktopConnection);
		runtime.exit();
	}

	@Test
	public void clientGetContextGroupInfo() throws Exception {
		CompletionStage<ContextGroupInfo[]> getContextFuture = desktopConnection.getInterop().connect(BROKER_NANE).thenCompose(client->{
			return client.getContextGroups();
		});
		
		ContextGroupInfo[] contextGroupInfo = getContextFuture.toCompletableFuture().get(10, TimeUnit.SECONDS);
		assertNotNull(contextGroupInfo);
		assertTrue(contextGroupInfo.length > 0);
		assertNotNull(contextGroupInfo[0].getDisplayMetadata().getColor());
	}

	@Test
	public void clientGetInfoForContextGroup() throws Exception {
		CompletionStage<ContextGroupInfo> getContextFuture = desktopConnection.getInterop().connect(BROKER_NANE).thenCompose(client->{
			return client.getInfoForContextGroup("red");
		});
		
		ContextGroupInfo contextGroupInfo = getContextFuture.toCompletableFuture().get(10, TimeUnit.SECONDS);
		assertNotNull(contextGroupInfo);
		assertNotNull(contextGroupInfo.getDisplayMetadata().getColor());
	}
	
	@Test
	public void clientGetAllClientsInContextGroup() throws Exception {
		CompletionStage<ClientIdentity[]> getContextFuture = desktopConnection.getInterop().connect(BROKER_NANE).thenCompose(client->{
			return client.joinContextGroup("red").thenCompose(v->{
				return client.getAllClientsInContextGroup("red");
			});
		});
		
		ClientIdentity[] clientIdentity = getContextFuture.toCompletableFuture().get(10, TimeUnit.SECONDS);
		assertNotNull(clientIdentity);
		assertTrue(clientIdentity.length > 0);
		assertNotNull(clientIdentity[0].getEndpointId());
	}

	@Test
	public void clientJoinThenRemoveFromContextGroup() throws Exception {
		AtomicInteger clientCntAfterJoin = new AtomicInteger(0);
		AtomicInteger clientCntAfterRemove = new AtomicInteger(0);
		CompletionStage<?> testFuture = desktopConnection.getInterop().connect(BROKER_NANE).thenCompose(client->{
			return client.joinContextGroup("green").thenCompose(v->{
				return client.getAllClientsInContextGroup("green");
			}).thenAccept(clients->{
				logger.info(String.format("clientJoinThenRemoveFromContextGroup after join %d", clients.length));
				clientCntAfterJoin.set(clients.length);
			}).thenCompose(v->{
				return client.removeFromContextGroup();
			}).thenCompose(v->{
				return client.getAllClientsInContextGroup("green");
			}).thenAccept(clients->{
				logger.info(String.format("clientJoinThenRemoveFromContextGroup after remove %d", clients.length));
				clientCntAfterRemove.set(clients.length);
			});
		});
		
		testFuture.toCompletableFuture().get(10, TimeUnit.SECONDS);
		assertEquals(clientCntAfterJoin.get(), clientCntAfterRemove.incrementAndGet());
	}

	@Test
	public void clientSetContext() throws Exception {
		final Context context = getRandomContext();
		CompletionStage<Void> setContextFuture = desktopConnection.getInterop().connect(BROKER_NANE).thenCompose(client->{
			return client.getContextGroups().thenCompose(groups->{
				return client.joinContextGroup("red").thenCompose(v->{
					return client.setContext(context);
				});
			});
		});
		
		setContextFuture.toCompletableFuture().get(10, TimeUnit.SECONDS);
	}

	@Test
	public void clientAddContextListener() throws Exception {
		final Context context = getRandomContext();

		CompletableFuture<Context> listenerInvokedFuture = new CompletableFuture<>();
		
		desktopConnection.getInterop().connect(BROKER_NANE).thenCompose(client->{
			return client.addContextListener(ctx->{
				String ticker = ctx.getId().optString("ticker", "");
				StringBuilder sb = new StringBuilder(context.getId().getString("ticker"));
				if (ticker.equals(sb.append("1").toString())) {
					listenerInvokedFuture.complete(ctx);
				}
			}).thenApply(v->{
				return client;
			});
		}).thenCompose(client->{
			return client.joinContextGroup("red").thenCompose(v->{
				return client.setContext(context);
			});
		});
		
		Context ctx = listenerInvokedFuture.toCompletableFuture().get(10, TimeUnit.SECONDS);
		assertNotNull(ctx);
	}

	@Test
	public void clientFireIntent() throws Exception {
		final Context context = getRandomContext();
		Intent intent = new Intent();
		intent.setName("JavaIntent");
		intent.setContext(context);
		CompletionStage<Void> fireIntentFuture = desktopConnection.getInterop().connect(BROKER_NANE).thenCompose(client->{
			return client.fireIntent(intent);
		});

		fireIntentFuture.toCompletableFuture().get(10, TimeUnit.SECONDS);
	}

	@Test
	public void clientFireAndRegisterIntentListener() throws Exception {
		final Context context = getRandomContext();

		CompletableFuture<Intent> listenerInvokedFuture = new CompletableFuture<>();
		desktopConnection.getInterop().connect(BROKER_NANE).thenCompose(client->{
			return client.registerIntentListener("JavaIntent", intentReceived->{
				String ticker = intentReceived.getContext().getId().optString("ticker", "");
				StringBuilder sb = new StringBuilder(context.getId().getString("ticker"));
				if (ticker.equals(sb.append("1").toString())) {
					listenerInvokedFuture.complete(intentReceived);
				}
			}).thenCompose(v -> {
				Intent intent = new Intent();
				intent.setName("JsTestIntent");
				intent.setContext(context);
				return client.fireIntent(intent);
			});
		});

		Intent intent = listenerInvokedFuture.toCompletableFuture().get(60, TimeUnit.SECONDS);
		assertNotNull(intent);
	}

	private Context getRandomContext() {
		Long randomTicker = Math.round(Math.random() * 100);
		final Context context = new Context();
		JSONObject id = new JSONObject();
		id.put("ticker", String.format("%d", randomTicker));
		context.setId(id);
		context.setType("java");
		context.setName("java");
		return context;
	}
}
