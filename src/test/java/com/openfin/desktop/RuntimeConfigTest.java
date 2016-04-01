package com.openfin.desktop;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.assertEquals;

/**
 * Created by richard on 4/1/2016.
 */
public class RuntimeConfigTest {
    private static Logger logger = LoggerFactory.getLogger(RuntimeConfigTest.class.getName());

    private static final String DESKTOP_UUID = RuntimeConfigTest.class.getName();
    private static DesktopConnection desktopConnection;
    private static RuntimeConfiguration configuration;
    private static String appUUID = UUID.randomUUID().toString();

    @BeforeClass
    public static void setup() throws Exception {
        logger.debug("starting");
        configuration = getRuntimeConfiguration();
        desktopConnection = TestUtils.setupConnection(DESKTOP_UUID, configuration);
    }

    private static RuntimeConfiguration getRuntimeConfiguration() {
        RuntimeConfiguration configuration = new RuntimeConfiguration();
        configuration.setRuntimeVersion(TestUtils.getRuntimeVersion());
        configuration.setDevToolsPort(9090);
        configuration.setAdditionalRuntimeArguments("--v=1");

        JSONObject startupApp = new JSONObject();
        startupApp.put("name", "TVBWebTest");
        startupApp.put("uuid", appUUID);
        startupApp.put("url", TestUtils.openfin_app_url);
        startupApp.put("applicationIcon", TestUtils.icon_url);
        startupApp.put("autoShow", true);
        startupApp.put("defaultTop", 100);
        startupApp.put("defaultLeft", 100);
        startupApp.put("defaultWidth", 200);
        startupApp.put("defaultHeight", 200);
        startupApp.put("delay_connection", true);
        startupApp.put("frame", true);
        startupApp.put("saveWindowState", false);
        configuration.setStartupApp(startupApp);

        return configuration;
    }

    @AfterClass
    public static void teardown() throws Exception {
        TestUtils.teardownDesktopConnection(desktopConnection);
    }

    @Test
    public void launchFromConfig() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> atomicReference = new AtomicReference<>();
        atomicReference.set(true);
        // check for startup app
        Thread waitThread = new Thread() {
            public void run() {
                while (atomicReference.get()) {
                    try {
                        if (isWindowCreated(appUUID)) {
                            latch.countDown();
                            atomicReference.set(false);
                        } else {
                            Thread.sleep(1000);
                        }
                    } catch (Exception ex) {
                        logger.error("Error waiting for checking window list", ex);
                    }
                }
            }
        };
        waitThread.start();

        latch.await(10, TimeUnit.SECONDS);
        atomicReference.set(false);
        assertEquals(latch.getCount(), 0);
    }

    private boolean isWindowCreated(String uuid) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> atomicReference = new AtomicReference<>();
        atomicReference.set(false);
        OpenFinRuntime runtime = new OpenFinRuntime(desktopConnection);
        runtime.getAllWindows(new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    JSONArray data = (JSONArray) ack.getData();
                    logger.debug(String.format("All windows info %s", data.toString()));
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject window = data.getJSONObject(i);
                        if (window.has("uuid") && window.has("mainWindow") && window.getString("uuid").equals(uuid)) {
                            atomicReference.set(true);
                        }
                    }
                    latch.countDown();
                }
            }
            @Override
            public void onError(Ack ack) {
                logger.error("Connection failed: %s", ack.getReason());
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        return atomicReference.get();
    }
}

