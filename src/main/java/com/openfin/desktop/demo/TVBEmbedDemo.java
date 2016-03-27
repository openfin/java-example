package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.win32.WinMessageHelper;
import com.sun.jna.Native;
import info.clearthought.layout.TableLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.System;
import java.util.*;

/**
 * Created by wche on 3/26/2016.
 */
public class TVBEmbedDemo extends JPanel implements ActionListener, WindowListener {

    private final static Logger logger = LoggerFactory.getLogger(TVBEmbedDemo.class.getName());

    private static JFrame jFrame;
    private String appUuid = "JavaEmbedding";
    private String startupUuid = "TVBWebLocal-yeotwn3hvf6wh142";
    protected DesktopConnection desktopConnection;
    private JButton launch, close;
    private java.awt.Canvas embedCanvas;
    private Long previousPrarentHwndId;
    private java.util.Timer timer = new java.util.Timer();

    // bounds of HTML5 app
    private static int appHeigth = 1080;
    private static int appWidth =  1920;

    public TVBEmbedDemo() {
        try {
            this.desktopConnection = new DesktopConnection(appUuid);
        } catch (DesktopException desktopError) {
            desktopError.printStackTrace();
        }
        setLayout(new BorderLayout());
        add(layoutCenterPanel(), BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setMainButtonsEnabled(false);
    }

    private JPanel layoutCenterPanel() {
        JPanel panel = new JPanel();
        double size[][] = {{TableLayout.FILL}, {40, TableLayout.FILL}};
        panel.setLayout(new TableLayout(size));
        panel.add(layoutActionButtonPanel(), "0,0,0,0");
        panel.add(layoutEmbedPanel(), "0, 1, 0, 1");
        return panel;
    }

    private JPanel layoutActionButtonPanel() {
        JPanel buttonPanel = new JPanel();

        JPanel topPanel = new JPanel();
        double size[][] = {{10, 190, 20, 190, 10}, {25}};
        topPanel.setLayout(new TableLayout(size));

        launch = new JButton("Launch OpenFin");
        launch.setActionCommand("start");
        close = new JButton("Shutdown OpenFin");
        close.setActionCommand("close");
        topPanel.add(launch, "1,0,1,0");
        topPanel.add(close, "3,0,3,0");

        close.addActionListener(this);
        launch.addActionListener(this);

        buttonPanel.add(topPanel, "0,0");
        return buttonPanel;
    }

    protected JPanel layoutEmbedPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(2), "HTML5 app"));

        embedCanvas = new java.awt.Canvas();
        panel.add(embedCanvas, BorderLayout.CENTER);


        return panel;
    }

    private void setMainButtonsEnabled(boolean enabled) {
        launch.setEnabled(!enabled);
        close.setEnabled(enabled);

        if (enabled) {
            this.desktopConnection.getInterApplicationBus().addSubscribeListener(new SubscriptionListener() {
                public void subscribed(String uuid, String topic) {
                    System.out.println("subscribed " + uuid + " on topic " + topic);
                }

                public void unsubscribed(String uuid, String topic) {
                    System.out.println("unsubscribed " + uuid + " on topic " + topic);

                }
            });
        }
    }

    private static void createAndShowGUI() {
        //Create and set up the window.
        jFrame = new JFrame("Java Embedding Demo");
        jFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        //Create and set up the content pane.
        TVBEmbedDemo newContentPane = new TVBEmbedDemo();
        newContentPane.setOpaque(true); //content panes must be opaque
        jFrame.setContentPane(newContentPane);
        jFrame.addWindowListener(newContentPane);
        //Display the window.
        jFrame.pack();
        jFrame.setSize(appWidth+80, appHeigth+160);
        jFrame.setLocationRelativeTo(null);
        jFrame.setResizable(true);
        jFrame.setVisible(true);
    }

    private void closeDesktop() {
        if (desktopConnection != null && desktopConnection.isConnected()) {
            try {
                if (startupHtml5app != null) {
                    startupHtml5app.close();
                }
                desktopConnection.disconnect();
//                OpenFinRuntime runtime = new OpenFinRuntime(desktopConnection);
//                runtime.exit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //closeWebSocket();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                jFrame.dispose();
            }
        });
        try {
            Thread.sleep(1000);
            java.lang.System.exit(0);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        closeDesktop();
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        if ("start".equals(e.getActionCommand())) {
            runStartAction();
        } else if ("close".equals(e.getActionCommand())) {
            closeDesktop();
        } else if ("embed-window".equals(e.getActionCommand())) {
            embedStartupApp();
        }
    }

    private void runStartAction() {
        try {
            DesktopStateListener listener = new DesktopStateListener() {
                @Override
                public void onReady() {
                    setMainButtonsEnabled(true);

                    long embedWaitTime = 3000L;
                    String s = java.lang.System.getProperty("com.openfin.demo.embedWaitTime");
                    if (s != null) {
                        embedWaitTime = Long.parseLong(s);
                    }
                    System.out.println(String.format("Embed wait time %d", embedWaitTime));
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            embedStartupApp();
                        }
                    }, embedWaitTime);
                }

                @Override
                public void onClose() {
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
            };

            RuntimeConfiguration configuration = new RuntimeConfiguration();
//            configuration.setRuntimeVersion("6.49.8.14");

            String runtimeVersion = java.lang.System.getProperty("com.openfin.demo.version");
            if (runtimeVersion == null) {
                runtimeVersion = "stable";
            }
            configuration.setRuntimeVersion(runtimeVersion);
            configuration.setDevToolsPort(9090);
            configuration.setAdditionalRuntimeArguments("--v=1 --noerrdialogs --disable-web-security --enable-aggressive-domstorage-flushing --load-extension=%LOCALAPPDATA%/OpenFin/apps/1578057152/assets/gktvbext-8266cea06bade1e88033/1.0.0");

            JSONObject asset = new JSONObject();
            asset.put("src", "https://test.web.tradervoicebox.com/openfin/gkt-vb-chrome-extension-8266cea06bade1e88033.zip");
            asset.put("alias", "gktvbext-8266cea06bade1e88033");
            asset.put("version", "1.0.0");
            configuration.addAppAsset(asset);

            JSONObject startupApp = new JSONObject();
            startupApp.put("name", "TVBWebTest");
            startupApp.put("uuid", startupUuid);
            startupApp.put("url", "https://test.web.tradervoicebox.com");
            startupApp.put("applicationIcon", "https://test.web.tradervoicebox.com/img/logo-large.jpg");
            startupApp.put("autoShow", false);
            startupApp.put("defaultWidth", appWidth);
            startupApp.put("defaultHeight", appHeigth);

            JSONArray permissions = new JSONArray();
            permissions.put("audioCapture");
            permissions.put("videoCapture");
            permissions.put("midi");
            startupApp.put("permissions",permissions);

            startupApp.put("delay_connection", true);
            startupApp.put("frame", false);
            startupApp.put("saveWindowState", false);
            configuration.setStartupApp(startupApp);

//            ApplicationOptions options = new ApplicationOptions("TVBWebTest", startupUuid, "https://test.web.tradervoicebox.com");
//            options.setApplicationIcon("https://test.web.tradervoicebox.com/img/logo-large.jpg");
//            WindowOptions mainWindowOptions = new WindowOptions();
//            mainWindowOptions.setAutoShow(true);
//            mainWindowOptions.setDefaultHeight(appHeigth);
//            mainWindowOptions.setDefaultWidth(appWidth);
//            mainWindowOptions.setSaveWindowState(false);  // set to false so all windows start at same initial positions for each run
//            mainWindowOptions.setFrame(false);
//            mainWindowOptions.toJsonObject().put("permissions","[audioCapture,videoCapture,midi]");
//            mainWindowOptions.toJsonObject().put("delay_connection", true);
//            options.setMainWindowOptions(mainWindowOptions);


            desktopConnection.connect(configuration, listener, 60);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchHtmlApp() {
        try {
            ApplicationOptions options = new ApplicationOptions("TVBWebTest", startupUuid, "https://test.web.tradervoicebox.com");
            options.setApplicationIcon("https://test.web.tradervoicebox.com/img/logo-large.jpg");
            WindowOptions mainWindowOptions = new WindowOptions();
            mainWindowOptions.setAutoShow(false);
            mainWindowOptions.setDefaultHeight(appHeigth);
            mainWindowOptions.setDefaultWidth(appWidth);
            mainWindowOptions.setSaveWindowState(false);  // set to false so all windows start at same initial positions for each run
            mainWindowOptions.setFrame(false);
            mainWindowOptions.toJsonObject().put("permissions","[audioCapture,videoCapture,midi]");
            mainWindowOptions.toJsonObject().put("delay_connection", true);
            options.setMainWindowOptions(mainWindowOptions);

            DemoUtils.runApplication(options, this.desktopConnection,  new AckListener() {
                @Override
                public void onSuccess(Ack ack) {
                    startupHtml5app = (Application) ack.getSource();
//                    embedStartupApp();
                }
                @Override
                public void onError(Ack ack) {
                    logger.error(String.format("Error launching %s %s", options.getUUID(), ack.getReason()));
                }
            });
        } catch (Exception e) {
            logger.error("Error launching app", e);
        }
    }

    private boolean trayIconAdded = false;
    private Application startupHtml5app;

    private void embedStartupApp() {
        try {
            if (startupHtml5app == null) {
                startupHtml5app = Application.wrap(this.startupUuid, this.desktopConnection);
            }

            com.openfin.desktop.Window html5Wnd = com.openfin.desktop.Window.wrap(startupUuid, startupUuid, desktopConnection);
            long parentHWndId = Native.getComponentID(this.embedCanvas);
            System.out.println("Canvas HWND " + Long.toHexString(parentHWndId));
            WinMessageHelper.embedInto(parentHWndId, html5Wnd, appWidth, appHeigth, 0, 0, new AckListener() {
                @Override
                public void onSuccess(Ack ack) {
                    if (ack.isSuccessful()) {
                        previousPrarentHwndId = ack.getJsonObject().getLong("hWndPreviousParent");
                        try {
                            html5Wnd.show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        java.lang.System.out.println("embedding failed: " + ack.getJsonObject().toString());
                    }
                }
                @Override
                public void onError(Ack ack) {
                    java.lang.System.out.println("embedding failed: " + ack.getJsonObject().toString());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseStartupApp() {
        com.openfin.desktop.Window html5Wnd = com.openfin.desktop.Window.wrap(startupUuid, startupUuid, desktopConnection);
        if (this.previousPrarentHwndId != null) {
            WinMessageHelper.embedInto(this.previousPrarentHwndId, html5Wnd, 395, 525, 0, 0, new AckListener() {
                @Override
                public void onSuccess(Ack ack) {
                    java.lang.System.out.println("embedding result: " + ack.getJsonObject().toString());
                }
                @Override
                public void onError(Ack ack) {
                    java.lang.System.out.println("embedding failed: " + ack.getJsonObject().toString());
                }
            });
            this.previousPrarentHwndId = null;
        }
    }

    /**
     * To start OpenFin Desktop and Connect, pass full path of OpenFin with*
     *    -DOpenFinOption=--config=\"RemoteConfigUrl\"
     *
     * Set UUID of startup HTML5 app to dock to
     *    -DStartupUUID="550e8400-e29b-41d4-a716-4466333333000"
     *
     * @param args
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

}
