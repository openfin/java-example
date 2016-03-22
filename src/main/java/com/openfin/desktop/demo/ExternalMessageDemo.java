package com.openfin.desktop.demo;

import com.openfin.desktop.DesktopConnection;
import com.openfin.desktop.DesktopStateListener;
import com.openfin.desktop.ExternalMessageListener;
import com.openfin.desktop.ExternalMessageResultHandler;
import org.json.JSONObject;

/**
 * Created by richard on 2/28/15.
 */
public class ExternalMessageDemo {
    DesktopConnection controller;

    ExternalMessageListener listener = new ExternalMessageListener() {
        @Override
        public void process(ExternalMessageResultHandler resultHandler, JSONObject payload) {
            System.out.println("Receiving " + payload.toString());
            resultHandler.send(true, "Worked");
        }
    };

    public void test() throws Exception{
        controller = new DesktopConnection("ExternalWPFMainWindowConnectionExample2", "localhost", 9696);
        controller.addExternalMessageHandler(listener, this);

        controller.connect(new DesktopStateListener() {
            @Override
            public void onReady() {
                System.out.println("Desktop onReady");
            }

            @Override
            public void onClose() {
                System.out.println("Connection closed");
            }

            @Override
            public void onError(String reason) {
                System.out.println("Desktop onError " + reason);
            }

            @Override
            public void onMessage(String message) {
                System.out.println("Desktop onMessage " + message);
            }

            @Override
            public void onOutgoingMessage(String message) {
                System.out.println("Desktop onOutgoingMessage " + message);
            }
        });

        synchronized (this) {
            this.wait(300*1000);
        }
    }

    public static void main(String[] args) throws Exception{

        ExternalMessageDemo demo = new ExternalMessageDemo();
        demo.test();

    }



}
