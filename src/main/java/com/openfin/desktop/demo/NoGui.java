package com.openfin.desktop.demo;

import com.openfin.desktop.*;

import java.io.IOException;
import java.lang.System;
import java.util.UUID;

/**
 * Example for launching OpenFin Runtime without Swing
 *
 * Created by wche on 9/4/2016.
 */
public class NoGui {
    private static boolean connected = false;

    public static void main(String[] args) {

        try {
            final DesktopConnection desktopConnection = new DesktopConnection(UUID.randomUUID().toString());
            DesktopStateListener listener = new DesktopStateListener() {
                @Override
                public void onReady() {
                    try {
                        connected = true;
                        ApplicationOptions applicationOptions = new ApplicationOptions("HelloOpenFin", UUID.randomUUID().toString(), "http://demoappdirectory.openf.in/desktop/config/apps/OpenFin/HelloOpenFin/index.html");
                        WindowOptions windowOptions = new WindowOptions();
                        windowOptions.setAutoShow(true);
                        applicationOptions.setMainWindowOptions(windowOptions);
                        Application application = new Application(applicationOptions, desktopConnection, new AckListener() {
                            @Override
                            public void onSuccess(Ack ack) {
                                System.out.println("Main.onSuccess");
                            }

                            @Override
                            public void onError(Ack ack) {
                                System.out.println("Main.onError");
                            }
                        });

//                        application.run(new AckListener() {
//                            @Override
//                            public void onSuccess(Ack ack) {
//                                System.out.println("Main.onSuccess");
//                            }
//
//                            @Override
//                            public void onError(Ack ack) {
//                                System.out.println("Main.onError");
//                            }
//                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose() {
                    System.out.println("Main.onClose");
                }

                @Override
                public void onError(String reason) {
                    System.out.println("Main.onError");
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("Main.onMessage");
                }

                @Override
                public void onOutgoingMessage(String message) {
                    System.out.println("Main.onOutgoingMessage");
                }
            };
            desktopConnection.connectToVersion("6.49.12.17", listener, 5000);

            try {
                // keep Runtime running for 10 seconds
                Thread.sleep(20000);
                desktopConnection.exit();
                // Give Runtime some time to exit
                Thread.sleep(3000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Before exit");

        } catch (DesktopException e) {
            e.printStackTrace();
        } catch (DesktopIOException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
