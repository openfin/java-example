package com.openfin.desktop;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.junit.Test;

/**
 * Created by wche on 9/15/2016.
 */
@SuppressWarnings("unchecked")
public class TestExample {

    private final String uuidForJavaApp = "OpenFinPOCExampleJava";
    private final String uuidForHtml5   = "OpenFinPOCExample";
    private DesktopConnection desktopConnection;

    @Test
    public void testOpenFin() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        desktopConnection = new DesktopConnection(this.uuidForJavaApp);
        final DesktopStateListener desktopStateListener = new DesktopStateListener(){
            @Override
            public void onReady() {
                // Java adapter is connected to Runtime.  we can create HTML5 app now
                java.lang.System.out.printf("onReady");
                latch.countDown();
                publishMessages();
            }
            @Override
            public void onClose() {
            }

            @Override
            public void onError(String reason) {
                java.lang.System.out.printf(reason);
            }
            @Override
            public void onMessage(String message) {
            }
            @Override
            public void onOutgoingMessage(String message) {
            }
        };

        final RuntimeConfiguration runtimeConfig = new RuntimeConfiguration();
        this.populateAppJsonConfigFile(runtimeConfig);
        desktopConnection.connect(runtimeConfig, desktopStateListener, 60);

        latch.await(30, TimeUnit.SECONDS);
        assertEquals("onReady timed out", latch.getCount(), 0);
        Thread.sleep(10000);  // keep Runtime running for 10 seconds
        desktopConnection.exit();
        Thread.sleep(5000);  // Give Runtime few seconds to exit
    }

    private void publishMessages() {

        desktopConnection.isConnected();

        final JSONObject jsonMessage = new JSONObject();
        final String topic = "Test Topic";
        jsonMessage.put(topic, "Hello World!");

        try {
            desktopConnection.getInterApplicationBus().publish(topic, jsonMessage);
        } catch (DesktopException e) {
            e.printStackTrace();
        }

    }

    private void populateAppJsonConfigFile(RuntimeConfiguration runtimeConfig) {
        final LinkedHashMap startupAppMap = new LinkedHashMap<>();
        startupAppMap.put("name", "OpenFin-Desktop");
        startupAppMap.put("description", "OpenFin POC");
        startupAppMap.put("url", "https://cdn.openfin.co/examples/junit/SimpleOpenFinApp.html");
        startupAppMap.put("uuid", this.uuidForHtml5);
        startupAppMap.put("autoShow", true);
        startupAppMap.put("defaultWidth", 1280);
        startupAppMap.put("minWidth", 1280);
        startupAppMap.put("defaultHeight", 920);
        startupAppMap.put("minHeight", 920);
        startupAppMap.put("resizable", false);
        startupAppMap.put("maximizable", true);

        final LinkedHashMap runtimeMap = new LinkedHashMap<>();
        runtimeMap.put("arguments", "");
        runtimeMap.put("forceLatest", true);

        /*  skip creating shortcut
            final LinkedHashMap shortcutMap = new LinkedHashMap<>();
            shortcutMap.put("company", "TPI");
            shortcutMap.put("description", "OpenFin POC");
            shortcutMap.put("name", "OpenFin-Desktop");
            runtimeConfig.setShortCut(new JSONObject(shortcutMap));
        */

        runtimeConfig.setDevToolsPort(9090);
        runtimeConfig.setStartupApp(new JSONObject(startupAppMap));

        runtimeConfig.setRuntimeVersion("stable");
    }

}
