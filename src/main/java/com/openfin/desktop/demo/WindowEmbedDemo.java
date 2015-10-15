package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.Window;
import com.openfin.desktop.win32.WinMessageHelper;
import com.sun.jna.Native;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.System;

/**
 * Java example to embed HTML5 window in Swing window
 *
 * Created by wche on 3/13/15.
 *
 */
public class WindowEmbedDemo extends JPanel implements ActionListener, WindowListener {

    private static JFrame jFrame;
    protected String appUuid = "JavaEmbedding";
    protected String startupUuid = "OpenFinHelloWorld";
    protected String desktopOption;
    protected DesktopConnection controller;

    protected JButton launch, close, embed;
    protected java.awt.Canvas embedCanvas;
    protected Long previousPrarentHwndId;

    public WindowEmbedDemo(final String desktopOption, final String startupUuid) {
        this.startupUuid = startupUuid;
        this.desktopOption = desktopOption;
        try {
            this.controller = new DesktopConnection(appUuid, "localhost", 9696);
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

        embed = new JButton("Embed HTML5 app");
        embed.setActionCommand("embed-window");
        embed.setEnabled(false);
        topPanel.add(embed, "1,2,1,2");

        close.addActionListener(this);
        launch.addActionListener(this);
        embed.addActionListener(this);

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
        embed.setEnabled(enabled);
        close.setEnabled(enabled);

        if (enabled) {
            this.controller.getInterApplicationBus().addSubscribeListener(new SubscriptionListener() {
                public void subscribed(String uuid, String topic) {
                    System.out.println("subscribed " + uuid + " on topic " + topic);
                }

                public void unsubscribed(String uuid, String topic) {
                    System.out.println("unsubscribed " + uuid + " on topic " + topic);

                }
            });
        }
    }

    private static void createAndShowGUI(final String desktopOption, final String startupUuid) {
        //Create and set up the window.
        jFrame = new JFrame("Java Docking Demo");
        jFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        //Create and set up the content pane.
        WindowEmbedDemo newContentPane = new WindowEmbedDemo(desktopOption, startupUuid);
        newContentPane.setOpaque(true); //content panes must be opaque
        jFrame.setContentPane(newContentPane);
        jFrame.addWindowListener(newContentPane);
        //Display the window.
        jFrame.pack();
        jFrame.setSize(600, 800);
        jFrame.setLocationRelativeTo(null);
        jFrame.setResizable(true);
        jFrame.setVisible(true);
    }

    private void closeDesktop() {
        if (controller != null && controller.isConnected()) {
            try {
//                new com.openfin.desktop.System(controller).exit();
                System.out.println("disconnecting ");
                controller.disconnect();
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
        } else if ("release-window".equals(e.getActionCommand())) {
            releaseStartupApp();
        }
    }

    private void runStartAction() {
        try {
            DesktopStateListener listener = new DesktopStateListener() {
                @Override
                public void onReady() {
                    setMainButtonsEnabled(true);
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
//            controller.launchAndConnect(null, desktopOption, listener, 10000);
            controller.connect(listener);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean trayIconAdded = false;
    private Application startupHtml5app;

    private void embedStartupApp() {
        try {
            if (startupHtml5app == null) {
                startupHtml5app = Application.wrap(this.startupUuid, this.controller);
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

            Window html5Wnd = Window.wrap(startupUuid, startupUuid, controller);
            long parentHWndId = Native.getComponentID(this.embedCanvas);
            System.out.println("Canvas HWND " + Long.toHexString(parentHWndId));
            WinMessageHelper.embedInto(parentHWndId, html5Wnd, 395, 525, 0, 0, new AckListener() {
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
                    java.lang.System.out.println("embedding failed: " + ack.getJsonObject().toString());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseStartupApp() {
        Window html5Wnd = Window.wrap(startupUuid, startupUuid, controller);
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
        final String desktop_option = java.lang.System.getProperty("OpenFinOption");
        final String startupUUID;
        if (java.lang.System.getProperty("StartupUUID") != null) {
            startupUUID = java.lang.System.getProperty("StartupUUID");
        } else {
            startupUUID = "OpenFinHelloWorld";
        }
        java.lang.System.out.println("starting: ");
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(desktop_option, startupUUID);
            }
        });
    }
}
