package com.openfin.desktop.demo;

import com.openfin.desktop.*;

import java.lang.System;
import java.util.UUID;

/**
 * Created by wche on 2/28/15.
 */
public class LaunchVersionDemo {
    private DesktopConnection controller;
    private String appURL;

    public LaunchVersionDemo(final String desktopVersion, final String appUrl) throws Exception {

        this.controller = new DesktopConnection("LaunchVersionDemo");
        this.controller.setLogLevel(true);

        this.appURL = appUrl;

        final DesktopStateListener listener = new DesktopStateListener() {
            @Override
            public void onReady() {
                System.out.println("Connected to OpenFin Runtime");
                InterApplicationBus bus = controller.getInterApplicationBus();
                try {
                    launchHTML5App();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            @Override
            public void onError(String reason) {
                System.out.println("Connection failed: " + reason);
            }

            @Override
            public void onMessage(String message) {
            }

            @Override
            public void onOutgoingMessage(String message) {
            }
        };

        controller.connectToVersion(desktopVersion, listener, 100000);
    }

    /**
     *
     * Starts the HTML5 app
     *
     */
    private void launchHTML5App() {

        ApplicationOptions applicationOptions = new ApplicationOptions("Hello Open Fin", UUID.randomUUID().toString(),
                this.appURL != null ? this.appURL : "http://demoappdirectory.openf.in/desktop/config/apps/OpenFin/HelloOpenFin/index.html");
        WindowOptions windowOptions = new WindowOptions();
        windowOptions.setAutoShow(true);
        windowOptions.setFrame(false);
        windowOptions.setResizable(false);
        windowOptions.setDefaultHeight(525);
        windowOptions.setDefaultWidth(395);
        windowOptions.setDefaultTop(50);
        windowOptions.setDefaultLeft(10);
        applicationOptions.setMainWindowOptions(windowOptions);
        Application app = new Application(applicationOptions, controller, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    Application application = (Application) ack.getSource();
                    try {
                        application.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onError(Ack ack) {

            }
        });
    }

    /**
     *
     * Closes down OpenFin Runtime
     *
     */
    public void closeDesktop() {
        if (controller != null && controller.isConnected()) {
            try {
                new com.openfin.desktop.System(controller).exit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This program accepts the following arguments
     *
     *    -DOpenFinRelease=SomeVersion -DOpenFinApp=AppUrl
     *
     * OpenFinApp should be the URL of an HTML5 app that will be started after OpenFin Runtime is running.
     *
     * If OpenFinApp is missing, URL for Hello OpenFin app is used.
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String versionStr = java.lang.System.getProperty("OpenFinRelease");
        String openFinApp = java.lang.System.getProperty("OpenFinApp");
        LaunchVersionDemo demo = new LaunchVersionDemo(versionStr, openFinApp);


        Thread.sleep(30*1000);

        demo.closeDesktop();
    }
}
