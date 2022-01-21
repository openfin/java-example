package com.openfin.desktop;

import com.openfin.desktop.snapshot.SnapshotSourceProvider;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class SnapshotTest implements SnapshotSourceProvider {

    private static Logger logger = LoggerFactory.getLogger(SnapshotTest.class.getName());

    private static final String DESKTOP_UUID = SnapshotTest.class.getName();
    private static DesktopConnection desktopConnection;
    private static OpenFinRuntime runtime;
    private static final JSONObject SNAPSHOT_CONTENT = new JSONObject("{width: 123}");

    private JSONObject randomSnapshot;

    @BeforeClass
    public static void setup() throws Exception {
        logger.debug("starting");
        desktopConnection = TestUtils.setupConnection(DESKTOP_UUID);
        if (desktopConnection != null) {
            runtime = new OpenFinRuntime(desktopConnection);
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        TestUtils.teardownDesktopConnection(desktopConnection);
    }

    @Test
    public void initProviderThenCreateClient() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        final String appUuid = "initProviderThenCreateClient";
        desktopConnection.getSnapshotSource().initSnapshotSourceProviderAsync(appUuid, this).thenAccept(provider -> {
            logger.debug("Snapshot provider created");
            latch.countDown();
        });
        desktopConnection.getSnapshotSource().createSnapshotSourceClientAsync(appUuid).thenAccept(client -> {
            logger.debug("Snapshot client created");
            latch.countDown();
        });

        latch.await(5, TimeUnit.SECONDS);

        assertEquals("initProviderThenCreateClient timeout", latch.getCount(), 0);
    }

    @Test
    public void initProviderThenCreateClientThenGetSnapshot() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        final String appUuid = "initProviderThenCreateClientThenGetSnapshot";
        desktopConnection.getSnapshotSource().initSnapshotSourceProviderAsync(appUuid, this).thenAccept(provider -> {
            logger.debug("Snapshot provider created");
                latch.countDown();
        });

        desktopConnection.getSnapshotSource().createSnapshotSourceClientAsync(appUuid).thenAccept(client -> {
            client.getSnapshotAsync().thenAccept(snapshot -> {
                if (SNAPSHOT_CONTENT.toString().equals(snapshot.toString())) {
                    latch.countDown();
                }
            });
        });

        latch.await(5, TimeUnit.SECONDS);

        assertEquals("initProviderThenCreateClientThenGetSnapshot timeout", latch.getCount(), 0);
    }

    @Test
    public void initProviderThenCreateClientThenApplySnapshot() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        final JSONObject random = new JSONObject(String.format("{value: %f}", Math.random()));
        final String appUuid = "initProviderThenCreateClientThenApplySnapshot";
        desktopConnection.getSnapshotSource().initSnapshotSourceProviderAsync(appUuid, this).thenAccept(provider -> {
            latch.countDown();
        });

        desktopConnection.getSnapshotSource().createSnapshotSourceClientAsync(appUuid).thenAccept(client -> {
            client.applySnapshotAsync(random).thenAccept(ack -> {
                client.getSnapshotAsync().thenAccept(snapshot -> {
                    if (random.toString().equals(snapshot.toString())) {
                        latch.countDown();
                    }
                });

            });
        });

        latch.await(5, TimeUnit.SECONDS);

        assertEquals("initProviderThenCreateClientThenGetSnapshot timeout", latch.getCount(), 0);
    }

    @Override
    public JSONObject getSnapshot() {
        if (this.randomSnapshot != null) {
            return this.randomSnapshot;
        } else {
            return SNAPSHOT_CONTENT;
        }
    }

    @Override
    public void applySnapshot(JSONObject snapshot) {
        this.randomSnapshot = snapshot;
    }
}
