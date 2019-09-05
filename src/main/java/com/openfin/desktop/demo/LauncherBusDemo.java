/**
 * Demo for launching OpenFin app and send messages via Inter application bus
 *
 * javascript side is in release/busdemo.html, which needs to be hosted in localhost:8888
 */

package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.Window;
import com.openfin.desktop.channel.*;
import com.openfin.desktop.channel.NotificationListener;
import com.openfin.desktop.channel.NotificationOptions;
import com.openfin.desktop.win32.ExternalWindowObserver;
import com.sun.jna.Native;
import info.clearthought.layout.TableLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.System;
import java.util.UUID;

public class LauncherBusDemo extends JFrame {
    private final static Logger logger = LoggerFactory.getLogger(LauncherBusDemo.class.getName());

    private DesktopConnection desktopConnection;
    private InterApplicationBus interApplicationBus;
    private NotificationClient notificationClient;
    private JButton btnOFApp1, btnOFApp2;
    private JButton btnTabOFApp1;
    private JButton btnNotification, btnToggleNotification;   // button to create notifications
    private JButton btnUndock;   // button to undock this Java window
    private JButton btnOFSendApp1, btnOFSendApp2;  // send messages to OpenFin app via Inter App Bus
    private JButton btnGenerateWorkSpace, btnRestoreWorkSpace;
    private static String appStartupUuid = "LaunchManifestDemo";  // App UUID for startup app in startup manifest
    private static String javaWindowName = appStartupUuid + "-Java-Window";  // name of this Java window registered with Runtime
    private final String app1Uuid = "Layout Client1";       // defined in layoutclient1.json
    private final String app2Uuid = "Layout Client2";       // defined in layoutclient2.json
    private final String appUrl = "http://localhost:8888/busdemo.html";
    com.openfin.desktop.Application app1, app2;  // OpenFin apps

    private final String embedUuid = "Embed Client";
    Application embeddedApp;   // OpenFin app to be embedded in Java canvas

    private LayoutClient layoutClient;                      // client for Layout service
    private JSONObject lastSavedWorkspace;
    private ExternalWindowObserver externalWindowObserver;  // required for Layout service to control Java window
    protected java.awt.Canvas embedCanvas;                  // required for embedding OpenFin window

    public LauncherBusDemo() {
        btnOFApp1 = new JButton();
        btnOFApp1.setEnabled(false);
        btnOFApp1.setText("Launch OpenFin app1");
        btnOFApp1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                launchAppFromManifest("http://localhost:8888/layoutclient1.json");
            }
        });

        btnTabOFApp1 = new JButton();
        btnTabOFApp1.setEnabled(false);
        btnTabOFApp1.setText("Tab to OpenFin app1");
        btnTabOFApp1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // uuid and name from http://localhost:8888/layoutclient1.json
                tabToWindow("Layout Client1", "Layout Client1");
            }
        });

        btnOFApp2 = new JButton();
        btnOFApp2.setEnabled(false);
        btnOFApp2.setText("Launch OpenFin App2");
        btnOFApp2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                launchAppFromManifest("http://localhost:8888/layoutclient2.json");
            }
        });

        btnOFSendApp1 = new JButton();
        btnOFSendApp1.setEnabled(false);
        btnOFSendApp1.setText("Send messages OpenFin app1");
        btnOFSendApp1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                sendOFApp(app1Uuid);
            }
        });

        btnOFSendApp2 = new JButton();
        btnOFSendApp2.setEnabled(false);
        btnOFSendApp2.setText("Send messages OpenFin app2");
        btnOFSendApp1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                sendOFApp(app2Uuid);
            }
        });

        btnNotification = new JButton();
        btnNotification.setText("Notification");
        btnNotification.setEnabled(false);
        btnNotification.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                createNotification();
            }
        });

        btnToggleNotification = new JButton();
        btnToggleNotification.setText("Toggle Notification CENTER");
        btnToggleNotification.setEnabled(false);
        btnToggleNotification.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleNotificationCenter();
            }
        });

        btnGenerateWorkSpace = new JButton();
        btnGenerateWorkSpace.setText("Generate WorkSpace");
        btnGenerateWorkSpace.setEnabled(false);
        btnGenerateWorkSpace.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                generateWorkSpace();
            }
        });

        btnRestoreWorkSpace = new JButton();
        btnRestoreWorkSpace.setText("Restore WorkSpace");
        btnRestoreWorkSpace.setEnabled(false);
        btnRestoreWorkSpace.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                restoreWorkSpace();
            }
        });

        btnUndock = new JButton();
        btnUndock.setText("Undock");
        btnUndock.setEnabled(false);

        JPanel topPanel = new JPanel();
        double size[][] = {{10, 190}, {25, 5, 25, 5, 25, 5, 25, 5, 25, 5, 25, 5, 25, 5, 25, 5, 25, 5, 25, 5}};
        topPanel.setLayout(new TableLayout(size));

        topPanel.add(btnOFApp1, "1,0,1,0");
        topPanel.add(btnTabOFApp1, "1,2,1,2");
        topPanel.add(btnOFApp2, "1,4,1,4");
        topPanel.add(btnOFSendApp1, "1,6,1,6");
        topPanel.add(btnOFSendApp2, "1,8,1,8");
        topPanel.add(btnNotification, "1,10,1,10");
        topPanel.add(btnToggleNotification, "1,12,1,12");
        topPanel.add(btnGenerateWorkSpace, "1,14,1,14");
        topPanel.add(btnRestoreWorkSpace, "1,16,1,16");
        topPanel.add(btnUndock, "1,18,1,18");

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(layoutEmbedPanel(), BorderLayout.CENTER);

        launchRuntime();
    }

    protected JPanel layoutEmbedPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(2), "HTML5 app"));
        embedCanvas = new java.awt.Canvas();
        panel.add(embedCanvas, BorderLayout.CENTER);
        embedCanvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                super.componentResized(event);
                Dimension newSize = event.getComponent().getSize();
                try {
//                    if (startupHtml5app != null) {
//                        startupHtml5app.getWindow().embedComponentSizeChange((int)newSize.getWidth(), (int)newSize.getHeight());
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return panel;
    }


    private void launchRuntime() {
        if (desktopConnection == null) {
            RuntimeConfiguration cfg = new RuntimeConfiguration();
            cfg.setRuntimeVersion("stable");
            cfg.setSecurityRealm("java-test");
            cfg.setDevToolsPort(9099);
            cfg.setAdditionalRuntimeArguments("--v=1 --enable-mesh ");   // --v=1  => enable verbose logging by Runtime
                                                                         // --enable-mesh  => enable multi-Runtime for the security realm
            // Add Layout Service to the manifest
            JSONArray serviceConfig = new JSONArray();
            // add Layout service to app manifest
            JSONObject layout = new JSONObject();
            layout.put("name", "layouts");
            JSONObject scfg = new JSONObject();
            JSONObject sfeatures = new JSONObject();
            sfeatures.put("dock", true);
            sfeatures.put("tab", true);
            scfg.put("features", sfeatures);
            layout.put("config", scfg);
//            layout.put("manifestUrl", "https://cdn.openfin.co/services/openfin/layouts/1.0.0/app.json");
            serviceConfig.put(0, layout);

            JSONObject notification = new JSONObject();
            notification.put("name", "notifications");
            serviceConfig.put(1, notification);

            cfg.addConfigurationItem("services", serviceConfig);

            JSONObject startupApp = new JSONObject();
            startupApp.put("uuid", appStartupUuid);
            startupApp.put("name", appStartupUuid);
            startupApp.put("url", "about:blank");
            startupApp.put("autoShow", false);
            cfg.setStartupApp(startupApp);

            try {
                desktopConnection = new DesktopConnection("Java app");
                desktopConnection.connect(cfg, new DesktopStateListener() {
                    @Override
                    public void onReady() {
                        logger.info("Connected to OpenFin Runtime");
                        interApplicationBus = new InterApplicationBus(desktopConnection);
                        btnOFApp1.setEnabled(true);
                        btnTabOFApp1.setEnabled(true);
                        btnOFApp2.setEnabled(true);
                        configAppEventListener();
                        createEmbddedApp();
                        createLayoutClient();
                        createNotificationClient();
                    }

                    @Override
                    public void onClose(String error) {
                    }

                    @Override
                    public void onError(String reason) {
                    }

                    @Override
                    public void onMessage(String message) {
                    }

                    @Override
                    public void onOutgoingMessage(String message) {
                    }
                }, 60);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void configAppEventListener() {
        try {
            // set up listener for "started" and "closed" event for layoutclient1.json
            app1 = com.openfin.desktop.Application.wrap(app1Uuid, this.desktopConnection);
            app1.addEventListener("started", new EventListener() {
                @Override
                public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                    btnOFApp1.setEnabled(false);
                    btnOFSendApp1.setEnabled(true);
                }
            }, null);
            app1.addEventListener("closed", new EventListener() {
                @Override
                public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                    btnOFApp1.setEnabled(true);
                    btnOFSendApp1.setEnabled(false);
                }
            }, null);

            // set up listener for "started and "closed" event for layoutclient2.json
            app2 = com.openfin.desktop.Application.wrap(app2Uuid, this.desktopConnection);
            app2.addEventListener("started", new EventListener() {
                @Override
                public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                    btnOFApp2.setEnabled(false);
                    btnOFSendApp2.setEnabled(true);
                }
            }, null);
            app2.addEventListener("closed", new EventListener() {
                @Override
                public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                    btnOFApp2.setEnabled(true);
                    btnOFSendApp2.setEnabled(false);
                }
            }, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Create externalWindowObserver for this Java frame so Runtime & Layout Service can keep track of location & size
     */
    private void createExternalWindowObserver() {
        if (this.externalWindowObserver != null) {
            // only needs to happen once
            return;
        }
        try {
            // ExternalWindowObserver forwards window events to Runtime & Layout Service.  Currently ExternalWindowObserver requires UUID of an
            // existing OpenFin app.  So here we are using UUID of the startup app in manifest.
            this.externalWindowObserver = new ExternalWindowObserver(desktopConnection.getPort(), appStartupUuid, javaWindowName, this,
                    new AckListener() {
                        @Override
                        public void onSuccess(Ack ack) {
                            btnUndock.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(java.awt.event.ActionEvent e) {
                                    LauncherBusDemo.this.layoutClient.undockWindow(appStartupUuid, javaWindowName, null);
                                }
                            });
                        }
                        @Override
                        public void onError(Ack ack) {
                            System.out.println(javaWindowName + ": unable to register external window, " + ack.getReason());
                        }
                    });
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // when this Java window is docked or undocked by layout service, "group-changed" is fired.
        // calling getGroup to determine if btnUndock should enabled.
        Window w = Window.wrap(appStartupUuid, javaWindowName, desktopConnection);
        w.addEventListener("group-changed", new EventListener() {
            @Override
            public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                w.getGroup(new AsyncCallback<java.util.List<Window>>() {
                    @Override
                    public void onSuccess(java.util.List<Window> result) {
                        if (result.size() > 0) {
                            btnUndock.setEnabled(true);
                        } else {
                            btnUndock.setEnabled(false);
                        }
                    }
                }, null);
            }
        }, null);

        try {
            this.externalWindowObserver.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchAppFromManifest(String manifest) {
        try {
            com.openfin.desktop.Application.createFromManifest(manifest,
                    new AsyncCallback<com.openfin.desktop.Application>() {
                        @Override
                        public void onSuccess(com.openfin.desktop.Application app) {
                            try {
                                app.run();
                                createExternalWindowObserver();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }, new AckListener() {
                        @Override
                        public void onSuccess(Ack ack) {
                        }

                        @Override
                        public void onError(Ack ack) {
                            logger.info("error creating app: {}", ack.getReason());
                        }
                    }, desktopConnection);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendOFApp(String desticationAppUuid) {
        JSONObject msg = new JSONObject();
        msg.put("ticker", "AAPL");
        msg.put("price", Math.random() * 100);
        try {
            interApplicationBus.send(desticationAppUuid, "messageFromJavaTopic", msg);
        } catch (DesktopException e) {
            e.printStackTrace();
        }
    }

    void createEmbddedApp() {
        ApplicationOptions appOpt = new ApplicationOptions(embedUuid, embedUuid, "http://localhost:8888/busdemo.html");
        WindowOptions mainWindowOptions = new WindowOptions();
        mainWindowOptions.setAutoShow(true);
        mainWindowOptions.setFrame(false);   // remove frame for embedded app
        mainWindowOptions.setResizable(true);
        mainWindowOptions.setContextMenu(true);
        appOpt.setMainWindowOptions(mainWindowOptions);

        this.embeddedApp = new Application(appOpt, this.desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                try {
                    embeddedApp.run(new AckListener() {
                        @Override
                        public void onSuccess(Ack ack) {
                            embedOpenFinApp();
                        }
                        @Override
                        public void onError(Ack ack) {
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            @Override
            public void onError(Ack ack) {
            }
        });
    }
    private void embedOpenFinApp() {
        try {
            Window html5Wnd = embeddedApp.getWindow();
            long parentHWndId = Native.getComponentID(this.embedCanvas);
            System.out.println("Canvas HWND " + Long.toHexString(parentHWndId));
            html5Wnd.embedInto(parentHWndId, this.embedCanvas.getWidth(), this.embedCanvas.getHeight(), new AckListener() {
                @Override
                public void onSuccess(Ack ack) {
                    if (ack.isSuccessful()) {
                    } else {
                        logger.error("embedding failed: " + ack.getJsonObject().toString());
                    }
                }
                @Override
                public void onError(Ack ack) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void createLayoutClient() {
        this.layoutClient = new LayoutClient(this.desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                btnGenerateWorkSpace.setEnabled(true);
                btnRestoreWorkSpace.setEnabled(true);
                createExternalWindowObserver();
            }
            @Override
            public void onError(Ack ack) {
            }
        });
    }
    private void createNotificationClient() {
        this.notificationClient = new NotificationClient(this.desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                btnNotification.setEnabled(true);
                btnToggleNotification.setEnabled(true);
                LauncherBusDemo.this.notificationClient.addNotificationListener(new NotificationListener() {
                    @Override
                    public void onClick(NotificationOptions options) {
                        logger.info(String.format("Notification clicked %s", options.getId()));
                    }
                    @Override
                    public void onButtonClick(NotificationOptions options) {
                        logger.info(String.format("Notification button clicked %s button index %d", options.getId(),
                                    options.getButtonIndex()));
                    }
                    @Override
                    public void onClose(NotificationOptions options) {
                        logger.info(String.format("Notification closed %s", options.getId()));
                    }
                });
            }
            @Override
            public void onError(Ack ack) {

            }
        });
    }
    private void createNotification() {
        NotificationOptions options = new NotificationOptions();
        options.setId(UUID.randomUUID().toString());
        options.setBody("Hello From Java app");
        options.setTitle("Java Demo");
        options.setIcon("https://openfin.co/favicon.ico");
        options.addButton(null, "button1");
        options.addButton(null, "button2");
        this.notificationClient.create(options, null);
    }
    private void toggleNotificationCenter() {
        this.notificationClient.toggleNotificationCenter(null);
    }

    private void generateWorkSpace() {
        this.layoutClient.generateWorkspace(new AsyncCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                LauncherBusDemo.this.lastSavedWorkspace = result;
                logger.info(String.format("Current workspace %s", result.toString()));
            }
        },
        new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
            }
            @Override
            public void onError(Ack ack) {
                logger.error(String.format("Error generating workspace %s", ack.getReason()));
            }
        });
    }

    private void restoreWorkSpace() {
        if (this.lastSavedWorkspace != null) {
            this.layoutClient.retoreWorkspace(this.lastSavedWorkspace, new AckListener() {
                @Override
                public void onSuccess(Ack ack) {
                }

                @Override
                public void onError(Ack ack) {
                    logger.error(String.format("Error restoring workspace %s", ack.getReason()));
                }
            });
        }
    }

    /**
     * Tab this Java window to an OpenFin windowq
     * @param appUuid
     * @param windowName
     */
    public void tabToWindow(String appUuid, String windowName) {
        WindowIdentity target = new WindowIdentity();
        target.setUuid(appUuid);
        target.setName(windowName);
        WindowIdentity me = new WindowIdentity();
        // this Java window is registered as appStartupUuid/javaWindowName, as in createExternalWindowObserver
        me.setUuid(appStartupUuid);
        me.setName(javaWindowName);
        this.layoutClient.tabWindows(target, me, null);
    }

    public void cleanup() {
        try {
            if (this.externalWindowObserver != null) {
                this.externalWindowObserver.dispose();
            }
            if (this.desktopConnection != null) {
                // shut down startup app
                Application app = Application.wrap(appStartupUuid, desktopConnection);
                app.close(true, null);

                Application app2 = Application.wrap(embedUuid, desktopConnection);
                app2.close(true, null);

                this.desktopConnection.disconnect();
                Thread.sleep(1000);
                java.lang.System.exit(0);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        this.externalWindowObserver = null;
    }

    private static void createAndShowGUI(final String startupUuid) {
        //Create and set up the window.
        LauncherBusDemo newContentPane = new LauncherBusDemo();
//        newContentPane.setOpaque(true); //content panes must be opaque
//        jFrame.setContentPane(newContentPane);
//        jFrame.addWindowListener(newContentPane);
        //Display the window.
        newContentPane.pack();
        newContentPane.setSize(700, 700);
        newContentPane.setLocationRelativeTo(null);
        newContentPane.setResizable(true);
        newContentPane.setVisible(true);

        newContentPane.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                try {
                    newContentPane.cleanup();
                }
                catch (Exception de) {
                    de.printStackTrace();
                }
            }
        });
    }


    public static void main(String args[]) {

        System.out.println(String.format("Install info on your machine %s", OpenFinRuntime.getInstallInfo().toString()));
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI("OpenFin Embed Example");
            }
        });
    }



}
