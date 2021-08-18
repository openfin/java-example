package com.openfin.desktop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openfin.desktop.snapshot.SnapshotSourceClient;
import com.openfin.desktop.snapshot.SnapshotSourceProvider;

public class SnapshotSourceTest {

	private static Logger logger = LoggerFactory.getLogger(SnapshotSourceTest.class.getName());

	private static final String DESKTOP_UUID = SnapshotSourceTest.class.getName();

	private DesktopConnection desktopConnection;

	@Before
	public void initDesktopConnection() throws Exception {
		this.desktopConnection = TestUtils.setupConnection(DESKTOP_UUID);
	}

	@Test
	public void initProviderThenCreateClient() throws Exception {
		String appUuid = "initProviderThenCreateClient";
		
		SnapshotSourceClient snapshotSourceClient = this.desktopConnection.getSnapshotSource().initSnapshotSource(appUuid, new SnapshotSourceProvider() {
			@Override
			public JSONObject getSnapshot() {
				return null;
			}

			@Override
			public void applySnapshot(JSONObject snapshot) {
				
			}
			
		}).thenCompose(snapshotSourceProvider-> {
			return this.desktopConnection.getSnapshotSource().createSnapshotSourceClient(appUuid);
		}).toCompletableFuture().get(10, TimeUnit.SECONDS);
		
		assertNotNull(snapshotSourceClient);
	}

	@Test
	public void initProviderThenCreateClientThenGetSnapshot() throws Exception {
		String appUuid = "initProviderThenCreateClientThenGetSnapshot";
		String snapshotPropName = "whatever";
		String snapshotPropValue = "hohoho";
		
		SnapshotSourceClient snapshotSourceClient = this.desktopConnection.getSnapshotSource().initSnapshotSource(appUuid, new SnapshotSourceProvider() {
			@Override
			public JSONObject getSnapshot() {
				JSONObject snapshot = new JSONObject();
				snapshot.put(snapshotPropName, snapshotPropValue);
				return snapshot;
			}

			@Override
			public void applySnapshot(JSONObject snapshot) {
				
			}
			
		}).thenCompose(snapshotSourceProvider-> {
			return this.desktopConnection.getSnapshotSource().createSnapshotSourceClient(appUuid);
		}).toCompletableFuture().get(10, TimeUnit.SECONDS);
		
		assertNotNull(snapshotSourceClient);
		
		JSONObject snapshot = snapshotSourceClient.getSnapshot().toCompletableFuture().get(10, TimeUnit.SECONDS);
		
		assertEquals(snapshot.get(snapshotPropName), snapshotPropValue);
	}

	@Test
	public void initProviderThenCreateClientThenApplySnapshot() throws Exception {
		String appUuid = "initProviderThenCreateClientThenApplySnapshot";
		String snapshotPropName = "whatever";
		String snapshotPropValue = "hohoho";
		
		CountDownLatch latch = new CountDownLatch(1);
		
		SnapshotSourceClient snapshotSourceClient = this.desktopConnection.getSnapshotSource().initSnapshotSource(appUuid, new SnapshotSourceProvider() {
			@Override
			public JSONObject getSnapshot() {
				return null;
			}

			@Override
			public void applySnapshot(JSONObject snapshot) {
				if (snapshotPropValue.equals(snapshot.getString(snapshotPropName))) {
					latch.countDown();
				}
			}
			
		}).thenCompose(snapshotSourceProvider-> {
			return this.desktopConnection.getSnapshotSource().createSnapshotSourceClient(appUuid);
		}).toCompletableFuture().get(10, TimeUnit.SECONDS);
		
		assertNotNull(snapshotSourceClient);

		JSONObject snapshot = new JSONObject();
		snapshot.put(snapshotPropName, snapshotPropValue);

		snapshotSourceClient.applySnapshot(snapshot).toCompletableFuture().get(10, TimeUnit.SECONDS);
		
		latch.await(10, TimeUnit.SECONDS);
		
		assertEquals(0, latch.getCount());
	}
}
