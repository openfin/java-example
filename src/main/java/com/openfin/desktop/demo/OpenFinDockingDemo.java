package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.OpenFinRuntime;
import com.openfin.desktop.win32.ExternalWindowObserver;
import info.clearthought.layout.TableLayout;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.System;

/**
 * Example of snap&dock Java window with OpenFin html5 window
 *
 * Move windows close to each other so they snap, and release mouse to dock 2 windows.
 *
 * This example use snap and dock js library at https://github.com/openfin/snap-and-dock.
 *
 * Steps to implement snap&dock in this example
 *
 * 1. Launch OpenFin Runtime, as in startOpenFinRuntime
 * 2. Launch a HTML5 app that include DockingManager from snap&dock library, as in launchDockingManager
 * 3. Once Docking Manger is ready, register Java window with OpenFin Runtime, as in registerJavaWindowWithRuntime
 * 4. Register Java window with Docking Manager, as in registerJavaWindowWithDockingManager
 *
 * Java window can receive notification when it is docked by subscribing to 'window-docked' topic.  It can also request to be undocked
 * by sending a message to Docking Manager.   Please refer to document of Snap&Dock library for more into
 *
 * Created by wche on 2/28/15.
 *
 */
public class OpenFinDockingDemo extends JPanel implements ActionListener, WindowListener {
    private final static Logger logger = LoggerFactory.getLogger(OpenFinDockingDemo.class.getName());


    private static JFrame jFrame;

    protected JButton launch;
    protected JButton close;

    protected JButton undockButton;

    protected ExternalWindowObserver externalWindowObserver;
    protected String javaWindowName = "Java Dock Window";
    protected String appUuid = "JavaDocking";
    protected String dockingManagerUuid = "DockingManager";  // Example HTML5 app for using snap and dock manager
    protected String dockingManagerURL = "http://openfin.github.io/snap-and-dock/index.html";  // exmple HTML5 app that includes Docking Manager

    protected DesktopConnection desktopConnection;

    protected JTextField dockStatus;  // show Ready to dock message
    protected JTextArea status;

    public OpenFinDockingDemo() {
        try {
            this.desktopConnection = new DesktopConnection(appUuid);
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
        double size[][] = {{410}, {120, 30, TableLayout.FILL}};
        panel.setLayout(new TableLayout(size));
        panel.add(layoutActionButtonPanel(), "0,0,0,0");
        panel.add(layoutDockStatus(), "0,1,0,1");
        panel.add(layoutStatusPanel(), "0, 2, 0, 2");
        return panel;
    }

    private JTextField layoutDockStatus() {
        this.dockStatus = new JTextField();
        this.dockStatus.setForeground(Color.RED);
        return this.dockStatus;
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

        undockButton = new JButton("Undock from HTML5 app");
        undockButton.setActionCommand("undock-window");
        undockButton.setEnabled(false);
        topPanel.add(undockButton, "1,2,1,2");

        close.addActionListener(this);
        launch.addActionListener(this);
        undockButton.addActionListener(this);

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
        if (desktopConnection != null && desktopConnection.isConnected()) {
            try {
                externalWindowObserver.dispose();
                Thread.sleep(2000);
                new OpenFinRuntime(desktopConnection).exit();
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
            System.exit(0);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void undockFromOtherWindows() {
        try {
            // send message to Docking Manager to undock me
            JSONObject msg = new JSONObject();
            msg.put("windowName", javaWindowName);
            desktopConnection.getInterApplicationBus().publish("undock-window", msg);

        } catch (Exception e) {
            e.printStackTrace();
        }
        undockButton.setEnabled(false);
    }

    /**
     *
     * Start OpenFin Runtime
     *
     */
    private void startOpenFinRuntime() {
        try {
            DesktopStateListener listener = new DesktopStateListener() {
                @Override
                public void onReady() {
                    updateMessagePanel("Connection authorized.");
                    setMainButtonsEnabled(true);
                    launchDockingManager();
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
            desktopConnection.setAdditionalRuntimeArguments(" --v=1");  // enable additional logging from Runtime
            desktopConnection.connectToVersion("stable", listener, 10000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        if ("start".equals(e.getActionCommand())) {
            startOpenFinRuntime();
        } else if ("close".equals(e.getActionCommand())) {
            closeDesktop();
        } else if ("undock-window".equals(e.getActionCommand())) {
            undockFromOtherWindows();
        }
    }


    private void setMainButtonsEnabled(boolean enabled) {
        launch.setEnabled(!enabled);
        close.setEnabled(enabled);
    }

    private void setAppButtonsEnabled(boolean enabled) {
    }

    /**
     * Register Java window with OpenFin Runtime
     *
     */
    private void registerJavaWindowWithRuntime() {
        try {
            this.externalWindowObserver = new ExternalWindowObserver(desktopConnection.getPort(), dockingManagerUuid, javaWindowName, jFrame,
                    new AckListener() {
                        @Override
                        public void onSuccess(Ack ack) {
                            if (ack.isSuccessful()) {
                                undockButton.setEnabled(false);
                                registerJavaWindowWithDockingManager();
                            }
                        }
                        @Override
                        public void onError(Ack ack) {
                            logger.error("Error starting " + dockingManagerUuid, ack.getReason());
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerJavaWindowWithDockingManager() {
        try {
            // get notified when being docked
            desktopConnection.getInterApplicationBus().subscribe("*", "window-docked", (sourceUuid, topic, payload) -> {
                JSONObject msg = (JSONObject) payload;
                if (msg.has("windowName") && msg.getString("windowName").equals(javaWindowName)) {
                    updateUndockButton(true);
                }
            }, null);

            // get notified when being undocked
            desktopConnection.getInterApplicationBus().subscribe("*", "window-undocked", (sourceUuid, topic, payload) -> {
                JSONObject msg = (JSONObject) payload;
                if (msg.has("windowName") && msg.getString("windowName").equals(javaWindowName)) {
                    updateUndockButton(false);
                }
            });

            JSONObject msg = new JSONObject();
            msg.put("applicationUuid", dockingManagerUuid);
            msg.put("windowName", javaWindowName);
            desktopConnection.getInterApplicationBus().publish("register-docking-window", msg);

        } catch (Exception ex) {
            logger.error("Error registeting with docking manager", ex);
        }

    }

    /**
     *
     * Launching a HTML5 app with docking manager
     *
     */
    private void launchDockingManager() {

        try {
            // Listen to status update from Docking Manager
            desktopConnection.getInterApplicationBus().subscribe(dockingManagerUuid, "status-update", new BusListener() {
                @Override
                public void onMessageReceived(String sourceUuid, String topic, Object payload) {
                    JSONObject msg = (JSONObject) payload;
                    if (msg.has("status") && msg.getString("status").equals("ready")) {
                        // Docking Manager is ready
                        registerJavaWindowWithRuntime();
                    }
                }
            }, null);
        } catch (Exception ex) {
            logger.error("Error subscribing ", ex);
        }

        ApplicationOptions options = new ApplicationOptions(dockingManagerUuid, dockingManagerUuid, dockingManagerURL);

        options.setApplicationIcon("http://openfin.github.io/snap-and-dock/openfin.ico");
        WindowOptions mainWindowOptions = new WindowOptions();
        mainWindowOptions.setAutoShow(true);
        mainWindowOptions.setDefaultHeight(150);
        mainWindowOptions.setDefaultLeft(10);
        mainWindowOptions.setDefaultTop(50);
        mainWindowOptions.setDefaultWidth(250);
        mainWindowOptions.setShowTaskbarIcon(true);
        options.setMainWindowOptions(mainWindowOptions);

        Application app = new Application(options, desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                Application application = (Application) ack.getSource();
                try {
                    application.run(new AckListener() {
                        public void onSuccess(Ack ack) {
                        }
                        public void onError(Ack ack) {
                            logger.error(String.format("Error running %s Reason %s", dockingManagerUuid, ack.getReason()));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onError(Ack ack) {
            }
        });
    }

    private void updateUndockButton(boolean enabled) {
        this.undockButton.setEnabled(enabled);
        if (enabled) {
            this.dockStatus.setText("Docked. Click Unlock button to undock");
        } else {
            this.dockStatus.setText("");
        }
    }


    private static void createAndShowGUI() {
        //Create and set up the window.
        jFrame = new JFrame("Java Docking Demo");
        jFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        //Create and set up the content pane.
        OpenFinDockingDemo newContentPane = new OpenFinDockingDemo();
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
