package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.ActionEvent;
import com.openfin.desktop.Window;
import com.sun.jna.Native;
import info.clearthought.layout.TableLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.System;

/**
 * Java example to embed HTML5 window in Swing window
 *
 * Created by wche on 3/13/15.
 *
 */
public class WindowEmbedDemo extends JPanel implements ActionListener, WindowListener {
    private final static Logger logger = LoggerFactory.getLogger(WindowEmbedDemo.class.getName());

    private static JFrame jFrame;
    protected String appUuid = "JavaEmbedding";
    protected String startupUuid = "OpenFinHelloWorld";
    protected DesktopConnection desktopConnection;

    protected String openfin_app_url = "https://cdn.openfin.co/examples/junit/SimpleDockingExample.html";  // source is in release/SimpleDockingExample.html

    protected JButton launch, close;
    protected java.awt.Canvas embedCanvas;
    protected Long previousPrarentHwndId;

    public WindowEmbedDemo(final String startupUuid) {
        this.startupUuid = startupUuid;
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
        double size[][] = {{TableLayout.FILL}, {120, TableLayout.FILL}};
        panel.setLayout(new TableLayout(size));
        panel.add(layoutActionButtonPanel(), "0,0,0,0");
        panel.add(layoutEmbedPanel(), "0, 1, 0, 1");
        return panel;
    }

    private JPanel layoutActionButtonPanel() {
        JPanel buttonPanel = new JPanel();

        JPanel topPanel = new JPanel();
        double size[][] = {{10, 190, 20, 190, 10}, {25, 10, 25, 10}};
        topPanel.setLayout(new TableLayout(size));
        topPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(2), "Desktop"));

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

        panel.add(embedCanvas, BorderLayout.CENTER);
        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                super.componentResized(event);
                Dimension newSize = event.getComponent().getSize();
                try {
                    if (startupHtml5app != null) {
                        startupHtml5app.getWindow().embedComponentSizeChange((int)newSize.getWidth(), (int)newSize.getHeight());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


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

    private static void createAndShowGUI(final String startupUuid) {
        //Create and set up the window.
        jFrame = new JFrame("Java Embedding Demo");
//        jFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        //Create and set up the content pane.
        WindowEmbedDemo newContentPane = new WindowEmbedDemo(startupUuid);
        newContentPane.setOpaque(true); //content panes must be opaque
        jFrame.setContentPane(newContentPane);
        jFrame.addWindowListener(newContentPane);
        //Display the window.
        jFrame.pack();
        jFrame.setSize(800, 800);
        jFrame.setLocationRelativeTo(null);
        jFrame.setResizable(true);
        jFrame.setVisible(true);
    }

    private void closeDesktop() {
        if (desktopConnection != null && desktopConnection.isConnected()) {
            try {
//                new com.openfin.desktop.System(desktopConnection).exit();
                System.out.println("disconnecting ");
                desktopConnection.disconnect();
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
                    launchHtmlApp();
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
            configuration.setAdditionalRuntimeArguments(" --v=1 ");  // enable additional logging from Runtime
            String desktopVersion = java.lang.System.getProperty("com.openfin.demo.version");
            if (desktopVersion == null) {
                desktopVersion = "stable";
            }
            configuration.setRuntimeVersion(desktopVersion);
            desktopConnection.connect(configuration, listener, 60);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchHtmlApp() {
            // launch 5 instances of same example app
            int width = 500, height=500;
            try {
                String url = java.lang.System.getProperty("com.openfin.demo.embed.URL");
                if (url != null) {
                    openfin_app_url = url;
                }
                ApplicationOptions options = new ApplicationOptions(startupUuid, startupUuid, openfin_app_url);
                options.setApplicationIcon("http://openfin.github.io/snap-and-dock/openfin.ico");
                WindowOptions mainWindowOptions = new WindowOptions();
                mainWindowOptions.setAutoShow(false);
                mainWindowOptions.setDefaultHeight(height);
                mainWindowOptions.setDefaultLeft(10);
                mainWindowOptions.setDefaultTop(50);
                mainWindowOptions.setDefaultWidth(width);
                mainWindowOptions.setShowTaskbarIcon(true);
                mainWindowOptions.setSaveWindowState(false);  // set to false so all windows start at same initial positions for each run
                mainWindowOptions.setFrame(false);
                mainWindowOptions.setContextMenu(true);
                mainWindowOptions.setResizeRegionSize(0);        // need this to turn off resize region for embedded (child) window
                options.setMainWindowOptions(mainWindowOptions);
                DemoUtils.runApplication(options, this.desktopConnection,  new AckListener() {
                    @Override
                    public void onSuccess(Ack ack) {
                        Application app = (Application) ack.getSource();
                        try {
                            Thread.sleep(1000);
                            embedStartupApp();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
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
            if (!trayIconAdded) {
                startupHtml5app.setTrayIcon("http://icons.iconarchive.com/icons/marcus-roberto/google-play/512/Google-Search-icon.png", new EventListener() {
                    public void eventReceived(ActionEvent actionEvent) {
                        java.lang.System.out.println("Tray icon clicked");
                    }
                }, null);
                trayIconAdded = true;
            } else {
                startupHtml5app.removeTrayIcon(null);
                trayIconAdded = false;
            }

            Window html5Wnd = startupHtml5app.getWindow();
            long parentHWndId = Native.getComponentID(this.embedCanvas);
            System.out.println("Canvas HWND " + Long.toHexString(parentHWndId));
            html5Wnd.embedInto(parentHWndId, this.embedCanvas.getWidth(), this.embedCanvas.getHeight(), new AckListener() {
                @Override
                public void onSuccess(Ack ack) {
                    if (ack.isSuccessful()) {
                        previousPrarentHwndId = ack.getJsonObject().getLong("hWndPreviousParent");
                    } else {
                        java.lang.System.out.println("embedding failed: " + ack.getJsonObject().toString());
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

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        java.lang.System.out.println("starting: ");
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI("OpenFin Embed Example");
            }
        });
    }
}
