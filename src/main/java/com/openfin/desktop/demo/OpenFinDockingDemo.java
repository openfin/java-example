package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.System;
import com.openfin.desktop.Window;
import com.openfin.desktop.win32.ExternalWindowObserver;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Example of docking Java window with OpenFin html5 window
 *
 * clicking dock button to dock this app to the HTML5 app.
 *
 * Created by wche on 2/28/15.
 *
 */
public class OpenFinDockingDemo extends JPanel implements ActionListener, WindowListener {


    private static JFrame jFrame;

    protected JButton launch;
    protected JButton close;

    protected JButton dock, undock;

    protected ExternalWindowObserver externalWindowObserver;
    protected String javaWindowName = "Java Dock Window";
    protected String appUuid = "JavaDocking";
    protected String startupUuid = "OpenFinHelloWorld";
    protected String desktopOption;

    protected DesktopConnection controller;

    protected JTextArea status;

    public OpenFinDockingDemo(final String desktopOption, final String startupUuid) {
        this.startupUuid = startupUuid;
        this.desktopOption = desktopOption;
        try {
            this.controller = new DesktopConnection(appUuid, "localhost", 9696);
        } catch (DesktopException desktopError) {
            desktopError.printStackTrace();
        }
        setLayout(new BorderLayout());
        add(layoutCenterPanel(), BorderLayout.CENTER);
        add(layoutLeftPanel(), BorderLayout.WEST);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        setMainButtonsEnabled(false);
        setAppButtonsEnabled(false);
    }

    private JPanel layoutLeftPanel() {
        JPanel panel = new JPanel();
        double size[][] = {{410}, {120, TableLayout.FILL}};
        panel.setLayout(new TableLayout(size));
        panel.add(layoutActionButtonPanel(), "0,0,0,0");
        panel.add(layoutStatusPanel(), "0, 1, 0, 1");
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

        dock = new JButton("Dock to HTML5 app");
        dock.setActionCommand("dock-window");
        dock.setEnabled(false);
        topPanel.add(dock, "1,2,1,2");

        undock = new JButton("Undock from HTML5 app");
        undock.setActionCommand("undock-window");
        undock.setEnabled(false);
        topPanel.add(undock, "3,2,3,2");

        close.addActionListener(this);
        launch.addActionListener(this);
        dock.addActionListener(this);
        undock.addActionListener(this);

        buttonPanel.add(topPanel, "0,0");
        return buttonPanel;
    }

    private JPanel layoutCenterPanel() {
        JPanel panel = new JPanel();
        double size[][] = {{TableLayout.FILL}, {150, 150, TableLayout.FILL}};
        panel.setLayout(new TableLayout(size));

        return panel;
    }

    protected JPanel layoutStatusPanel() {
        //Simple status console
        status = new JTextArea();
        status.setEditable(false);
        status.setAutoscrolls(true);
        status.setLineWrap(true);
        JScrollPane statusPane = new JScrollPane(status);
        statusPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        statusPane.getViewport().setOpaque(false);
        statusPane.setOpaque(false);
        statusPane.setBorder(BorderFactory.createEmptyBorder(5,15,15,15));



        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(2), "Status"));
        panel.add(statusPane, BorderLayout.CENTER);

        return panel;
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

    private void updateMessagePanel(final String msg) {
        if (SwingUtilities.isEventDispatchThread()) {
            String t = "";
            if (status.getText().length() > 0) {

                t = status.getText();
            }
            StringBuilder b = new StringBuilder();
            b.append(msg).append("\n").append(t);
            status.setText(b.toString());
            status.setCaretPosition(0);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateMessagePanel(msg);
                }
            });
        }
    }


    private void closeDesktop() {
        if (controller != null && controller.isConnected()) {
            try {
                new System(controller).exit();
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

    private void dockToStartupApp() {
        try {
            Window w = Window.wrap(startupUuid, javaWindowName, controller);
            w.joinGroup(Window.wrap(startupUuid, startupUuid, controller));
        } catch (Exception e) {
            e.printStackTrace();
        }
        dock.setEnabled(false);
        undock.setEnabled(true);
    }

    private void undockFromStartupApp() {
        try {
            Window w = Window.wrap(startupUuid, javaWindowName, controller);
            w.leaveGroup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        dock.setEnabled(true);
        undock.setEnabled(false);
    }

    private void registerExternalWindow() {
        try {
            externalWindowObserver = new ExternalWindowObserver(controller.getPort(), startupUuid, javaWindowName, jFrame);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private void runStartAction() {
        try {
            DesktopStateListener listener = new DesktopStateListener() {
                @Override
                public void onReady() {
                    updateMessagePanel("Connection authorized.");
                    setMainButtonsEnabled(true);
                    registerExternalWindow();
                }

                @Override
                public void onError(String reason) {
                    updateMessagePanel("Connection failed: " + reason);
                }

                @Override
                public void onMessage(String message) {
                    updateMessagePanel("-->FROM DESKTOP-" + message);
                }

                @Override
                public void onOutgoingMessage(String message) {
                    updateMessagePanel("<--TO DESKTOP-" + message);
                }

            };
            controller.launchAndConnect(null, desktopOption, listener, 10000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        if ("start".equals(e.getActionCommand())) {
            runStartAction();
        } else if ("close".equals(e.getActionCommand())) {
            closeDesktop();
        } else if ("dock-window".equals(e.getActionCommand())) {
            dockToStartupApp();
        } else if ("undock-window".equals(e.getActionCommand())) {
            undockFromStartupApp();
        }
    }


    private void setMainButtonsEnabled(boolean enabled) {
        launch.setEnabled(!enabled);
        dock.setEnabled(enabled);
        close.setEnabled(enabled);
    }

    private void setAppButtonsEnabled(boolean enabled) {
    }


    private static void createAndShowGUI(final String desktopOption, final String startupUuid) {
        //Create and set up the window.
        jFrame = new JFrame("Java Docking Demo");
        jFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        //Create and set up the content pane.
        OpenFinDockingDemo newContentPane = new OpenFinDockingDemo(desktopOption, startupUuid);
        newContentPane.setOpaque(true); //content panes must be opaque
        jFrame.setContentPane(newContentPane);
        jFrame.addWindowListener(newContentPane);

        //Display the window.
        jFrame.pack();
        jFrame.setSize(470, 500);
        jFrame.setLocationRelativeTo(null);
        jFrame.setResizable(false);
        jFrame.setVisible(true);
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
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(desktop_option, startupUUID);
            }
        });
    }
}
