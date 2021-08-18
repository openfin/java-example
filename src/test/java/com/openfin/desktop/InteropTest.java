package com.openfin.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openfin.desktop.interop.Context;
import com.openfin.desktop.interop.ContextGroupInfo;

public class InteropTest {
	private static Logger logger = LoggerFactory.getLogger(InteropTest.class.getName());

	private static final String DESKTOP_UUID = InteropTest.class.getName();
	private static DesktopConnection desktopConnection;

	@BeforeClass
	public static void setup() throws Exception {
		logger.debug("starting");
		desktopConnection = TestUtils.setupConnection(DESKTOP_UUID);
	}
	
	@Test
	public void clientGetContextGroupInfo() throws Exception {
		CompletionStage<ContextGroupInfo[]> getContextFuture = desktopConnection.getInterop().connect("InteropTest").thenCompose(client->{
			return client.getContextGroups();
		});
		
		ContextGroupInfo[] contextGroupInfo = getContextFuture.toCompletableFuture().get(10, TimeUnit.SECONDS);
		assertNotNull(contextGroupInfo);
		assertTrue(contextGroupInfo.length > 0);
		assertNotNull(contextGroupInfo[0].getDisplayMetadata().getColor());
	}

	@Test
	public void clientGetInfoForContextGroup() throws Exception {
		CompletionStage<ContextGroupInfo> getContextFuture = desktopConnection.getInterop().connect("InteropTest").thenCompose(client->{
			return client.getInfoForContextGroup("red");
		});
		
		ContextGroupInfo contextGroupInfo = getContextFuture.toCompletableFuture().get(10, TimeUnit.SECONDS);
		assertNotNull(contextGroupInfo);
		assertNotNull(contextGroupInfo.getDisplayMetadata().getColor());
	}
	
	@Test
	public void clientGetAllClientsInContextGroup() throws Exception {
		CompletionStage<ClientIdentity[]> getContextFuture = desktopConnection.getInterop().connect("InteropTest").thenCompose(client->{
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
		CompletionStage<?> testFuture = desktopConnection.getInterop().connect("InteropTest").thenCompose(client->{
			return client.joinContextGroup("red").thenCompose(v->{
				return client.getAllClientsInContextGroup("red");
			}).thenAccept(clients->{
				clientCntAfterJoin.set(clients.length);
			}).thenCompose(v->{
				return client.removeFromContextGroup();
			}).thenCompose(v->{
				return client.getAllClientsInContextGroup("red");
			}).thenAccept(clients->{
				clientCntAfterRemove.set(clients.length);
			});
		});
		
		testFuture.toCompletableFuture().get(10, TimeUnit.SECONDS);
		assertEquals(clientCntAfterJoin.get(), clientCntAfterRemove.incrementAndGet());
	}

	@Test
	public void clientSetContext() throws Exception {
		Context context = new Context();
		context.setId("MyId");
		context.setName("MyName");
		context.setType("MyType");
		CompletionStage<Void> setContextFuture = desktopConnection.getInterop().connect("InteropTest").thenCompose(client->{
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
		Context context = new Context();
		context.setId("MyId");
		context.setName("MyName");
		context.setType("MyType");
		
		CompletableFuture<Context> listenerInvokedFuture = new CompletableFuture<>();
		
		desktopConnection.getInterop().connect("InteropTest").thenCompose(client->{
			return client.addContextListener(ctx->{
				listenerInvokedFuture.complete(ctx);
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
}
