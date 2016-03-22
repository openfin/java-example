package com.openfin.desktop.demo;

import com.openfin.desktop.DockingManager;
import com.openfin.desktop.*;
import info.clearthought.layout.TableLayout;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.System;
import java.util.Arrays;

/**
 * Example of snap&dock Java window with OpenFin html5 window
 *
 * Move windows close to each other so they snap, and release mouse to dock 2 windows.
 *
 * This example use snap and dock library at https://github.com/openfin/java-snap-and-dock.
 *
 * Steps to implement snap&dock in this example
 *
 * 1. Launch OpenFin Runtime, as in startOpenFinRuntime
 * 2. Create an instance of DockingManager
 * 2. Launch a HTML5 app and register with DockingManager as in launchHTMLApps
 * 3. Register Java window as in DockingManager registerJavaWindow
 *
 * All windows can receive notification when it is docked by subscribing to 'window-docked' topic.  It can also request to be undocked
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

    // For a Java window to be dockable, it must be registered with Runtime as a child window of a HTML5 app.  We are going to create
    // a hidden HTML5 app, with javaParentAppUuid as UUID, as the parent app of all Java windows
    protected String javaParentAppUuid = "Java Parent App";

    protected String appUuid = "JavaDocking";  // UUID for desktopConnection
    protected String javaWindowName = "Java Dock Window";
    protected String openfin_app_url = "https://cdn.openfin.co/examples/junit/SimpleDockingExample.html";  // source is in release/SimpleDockingExample.html

    protected DesktopConnection desktopConnection;
    protected DockingManager dockingManager;
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

        undockButton = new JButton("Undock");
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
            msg.put("applicationUuid", javaParentAppUuid);
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
                    onRuntimeReady();
                }

                @Override
                public void onClose() {
                    updateMessagePanel("Connection closed");
                }
                @Override
                public void onError(String reason) {
                    updateMessagePanel("Connection failed: " + reason);
                }
                @Override
                public void onMessage(String message) {
                }
                @Override
                public void onOutgoingMessage(String message) {
                }
            };
            desktopConnection.setAdditionalRuntimeArguments(" --v=1");  // enable additional logging from Runtime
            desktopConnection.connectToVersion("5.44.10.26", listener, 60); // 5.44.10.26 has fix for cross-app docking, which is required for windowsInShameGroupMoveTogether

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onRuntimeReady() {
        try {
            updateMessagePanel("Connection authorized.");
            setMainButtonsEnabled(true);
            this.dockingManager = new DockingManager(this.desktopConnection, this.javaParentAppUuid);
            this.desktopConnection.getInterApplicationBus().subscribe("*", "window-docked", (uuid, topic, data) -> {
                JSONObject event = (JSONObject) data;
                String appUuid = event.getString("applicationUuid");
                String windowName = event.getString("windowName");
                updateMessagePanel(String.format("Window docked %s %s", appUuid, windowName));
                if (javaParentAppUuid.equals(appUuid) && javaWindowName.equals(windowName)) {
                    updateUndockButton(true);
                }
            });
            this.desktopConnection.getInterApplicationBus().subscribe("*", "window-undocked", (uuid, topic, data) -> {
                JSONObject event = (JSONObject) data;
                String appUuid = event.getString("applicationUuid");
                String windowName = event.getString("windowName");
                updateMessagePanel(String.format("Window undocked %s %s", appUuid, windowName));
                if (javaParentAppUuid.equals(appUuid) && javaWindowName.equals(windowName)) {
                    updateUndockButton(false);
                }
            });
            registerJavaWindow();
            launchHTMLApps();
            launchAnotherJavaApp();
        } catch (Exception e) {
            logger.error("Error creating DockingManager", e);
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
    private void registerJavaWindow() {
        try {
            // Java window needs to be assigned a HTML5 app as parent app in order for Runtime to control it.
            // now we are creating a HTML5 app aith autoShow = false so it is hidden.
            ApplicationOptions options = new ApplicationOptions(javaParentAppUuid, javaParentAppUuid, openfin_app_url);
            options.setApplicationIcon("http://openfin.github.io/snap-and-dock/openfin.ico");
            WindowOptions mainWindowOptions = new WindowOptions();
            mainWindowOptions.setAutoShow(false);
            mainWindowOptions.setDefaultHeight(50);
            mainWindowOptions.setDefaultLeft(50);
            mainWindowOptions.setDefaultTop(50);
            mainWindowOptions.setDefaultWidth(50);
            mainWindowOptions.setShowTaskbarIcon(false);
            mainWindowOptions.setSaveWindowState(false);  // set to false so all windows start at same initial positions for each run
            options.setMainWindowOptions(mainWindowOptions);
            launchHTMLApp(options, new AckListener() {
                @Override
                public void onSuccess(Ack ack) {
                    Application app = (Application) ack.getSource();
                    logger.debug("Registering java window");
                    dockingManager.registerJavaWindow(javaWindowName, jFrame,
                        new AckListener() {
                            @Override
                            public void onSuccess(Ack ack) {
                                if (ack.isSuccessful()) {
                                    undockButton.setEnabled(false);
                                }
                            }
                            @Override
                            public void onError(Ack ack) {
                                logger.error("Error registering Java window " + ack.getReason());
                            }
                        });
                }
                @Override
                public void onError(Ack ack) {
                    logger.error("Error launching " + javaParentAppUuid);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * Launching HTML5 apps and register docking manager
     *
     */
    private void launchHTMLApps() {
        // launch 5 instances of same example app
        int width = 300, height=200;
        int gap = 50;  // gap between windows at initial positions
        for (int i = 0; i < 2; i++) {
            try {
                String uuid = String.format("docking-html-%d", i);
                ApplicationOptions options = new ApplicationOptions(uuid, uuid, openfin_app_url);
                options.setApplicationIcon("http://openfin.github.io/snap-and-dock/openfin.ico");
                WindowOptions mainWindowOptions = new WindowOptions();
                mainWindowOptions.setAutoShow(true);
                mainWindowOptions.setDefaultHeight(height);
                mainWindowOptions.setDefaultLeft(10 + i * (width + gap));
                mainWindowOptions.setDefaultTop(50);
                mainWindowOptions.setDefaultWidth(width);
                mainWindowOptions.setShowTaskbarIcon(true);
                mainWindowOptions.setSaveWindowState(false);  // set to false so all windows start at same initial positions for each run
                options.setMainWindowOptions(mainWindowOptions);
                launchHTMLApp(options, new AckListener() {
                    @Override
                    public void onSuccess(Ack ack) {
                        logger.debug(String.format("Successful launching %s", options.getUUID()));
                        Application app = (Application) ack.getSource();
                        dockingManager.registerWindow(app.getWindow());
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
    }

    private void launchHTMLApp(ApplicationOptions options, AckListener ackListener) throws Exception {
        logger.debug(String.format("Launching %s", options.getUUID()));
        DemoUtils.runApplication(options, this.desktopConnection, ackListener);
    }


    private void launchAnotherJavaApp() {
        Thread launchThread = new Thread() {
            public void run() {
                logger.debug(String.format("Launching docking.bat %s", DockingDemo2.class.getName()));
                String[] command = {"docking.bat", DockingDemo2.class.getName()};
                ProcessBuilder probuilder = new ProcessBuilder( command );
                //You can set up your work directory
                probuilder.directory(new File("."));
                try {
//                    Process process = probuilder.start();
//                    //Read out dir output
//                    InputStream is = process.getInputStream();
//                    InputStreamReader isr = new InputStreamReader(is);
//                    BufferedReader br = new BufferedReader(isr);
//                    String line;
//                    while ((line = br.readLine()) != null) {
//                        logger.debug(String.format("Java App output: %s", line));
//                    }
//                    int exitValue = process.waitFor();
//                    logger.debug(String.format("Java app exit code %d", exitValue));

                    Process process = Runtime.getRuntime().exec("cmd");
                    OutputStream stdin = process.getOutputStream();
                    InputStream stderr = process.getErrorStream();
                    InputStream stdout = process.getInputStream();
                    String line = "start docking.bat " + DockingDemo2.class.getName() + "\n";
                    stdin.write(line.getBytes());
                    stdin.flush();
                    stdin.close();
                    // clean up if any output in stdout
                    BufferedReader brCleanUp =
                            new BufferedReader(new InputStreamReader(stdout));
                    while ((line = brCleanUp.readLine()) != null) {
                        logger.debug("[Stdout] " + line);
                    }
                    brCleanUp.close();
                    // clean up if any output in stderr
                    brCleanUp =
                            new BufferedReader(new InputStreamReader(stderr));
                    while ((line = brCleanUp.readLine()) != null) {
                        logger.info("[Stderr] " + line);
                    }
                    brCleanUp.close();
                } catch (Exception ex) {
                    logger.error("Error launch java app", ex);
                }
            }
        };
        launchThread.start();
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
        jFrame.setSize(470, 300);
        jFrame.setLocation(100, 400);
//        jFrame.setLocationRelativeTo(null);
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
