package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.win32.ExternalWindowObserver;
import info.clearthought.layout.TableLayout;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * This app works with OpenFinDockingDemo as another dockable Java window
 *
 * Created by wche on 3/9/2016.
 */
public class DockingDemo2 extends JPanel implements ActionListener, WindowListener {
    private final static Logger logger = LoggerFactory.getLogger(DockingDemo2.class.getName());

    protected String javaParentAppUuid = "Java Parent App";  // Its value must match OpenFinDockingDemo.  Please see OpenFinDockingDemo for comments

    protected String appUuid = "AnotherJavaDocking";  // UUID for desktopConnection
    protected String javaWindowName = "Another Java Dock Window";

    protected DesktopConnection desktopConnection;
    protected ExternalWindowObserver externalWindowObserver;

    private static JFrame jFrame;
    protected JButton undockButton;
    protected JTextField dockStatus;  // show Ready to dock message

    public DockingDemo2() {
        try {
            this.desktopConnection = new DesktopConnection(appUuid);
            startOpenFinRuntime();
        } catch (DesktopException desktopError) {
            desktopError.printStackTrace();
        }
        setLayout(new BorderLayout());
        add(layoutCenterPanel(), BorderLayout.CENTER);
        add(layoutLeftPanel(), BorderLayout.WEST);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
    }

    private JPanel layoutLeftPanel() {
        JPanel panel = new JPanel();
        double size[][] = {{410}, {120, 30, TableLayout.FILL}};
        panel.setLayout(new TableLayout(size));
        panel.add(layoutActionButtonPanel(), "0,0,0,0");
        panel.add(layoutDockStatus(), "0,1,0,1");
        return panel;
    }

    private JTextField layoutDockStatus() {
        this.dockStatus = new JTextField();
        return this.dockStatus;
    }

    private JPanel layoutActionButtonPanel() {
        JPanel buttonPanel = new JPanel();
        JPanel topPanel = new JPanel();
        double size[][] = {{10, 190, 20, 190, 10}, {25, 10, 25, 10}};
        topPanel.setLayout(new TableLayout(size));
        topPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(2), "Desktop"));
        undockButton = new JButton("Undock");
        undockButton.setActionCommand("undock-window");
        undockButton.setEnabled(false);
        topPanel.add(undockButton, "1,0,1,0");
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

    private void startOpenFinRuntime() {
        try {
            DesktopStateListener listener = new DesktopStateListener() {
                @Override
                public void onReady() {
                    onRuntimeReady();
                }

                @Override
                public void onClose(String error) {
                }
                @Override
                public void onError(String reason) {
                    logger.error("Connection failed: " + reason);
                    java.lang.System.exit(0);
                }
                @Override
                public void onMessage(String message) {
                }
                @Override
                public void onOutgoingMessage(String message) {
                }
            };
            desktopConnection.setAdditionalRuntimeArguments(" --v=1");  // enable additional logging from Runtime

            String desktopVersion = java.lang.System.getProperty("com.openfin.demo.version");
            if (desktopVersion == null) {
                desktopVersion = "stable";
            }
            desktopConnection.connectToVersion(desktopVersion, listener, 60); // 5.44.10.26 has fix for cross-app docking, which is required for windowsInShameGroupMoveTogether

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void onRuntimeReady() {
        try {
            this.desktopConnection.getInterApplicationBus().subscribe("*", "window-docked", (uuid, topic, data) -> {
                JSONObject event = (JSONObject) data;
                String appUuid = event.getString("applicationUuid");
                String windowName = event.getString("windowName");
                if (javaParentAppUuid.equals(appUuid) && javaWindowName.equals(windowName)) {
                    updateUndockButton(true);
                }
            });
            this.desktopConnection.getInterApplicationBus().subscribe("*", "window-undocked", (uuid, topic, data) -> {
                JSONObject event = (JSONObject) data;
                String appUuid = event.getString("applicationUuid");
                String windowName = event.getString("windowName");
                if (javaParentAppUuid.equals(appUuid) && javaWindowName.equals(windowName)) {
                    updateUndockButton(false);
                }
            });
            registerJavaWindow();
            Application javaParentApp = Application.wrap(javaParentAppUuid, desktopConnection);
            javaParentApp.addEventListener("closed", actionEvent -> { shutdown(); }, null);
        } catch (Exception e) {
            logger.error("Error creating DockingManager", e);
        }
    }

    private void registerJavaWindow() {
        try {
            externalWindowObserver = new ExternalWindowObserver(desktopConnection.getPort(), javaParentAppUuid, javaWindowName, jFrame,
                new AckListener() {
                    @Override
                    public void onSuccess(Ack ack) {
                        try {
                            if (ack.isSuccessful()) {
                                JSONObject request = new JSONObject();
                                request.put("applicationUuid", javaParentAppUuid);
                                request.put("windowName", javaWindowName);
                                desktopConnection.getInterApplicationBus().publish("register-docking-window", request);
                            }
                        } catch (Exception ex) {
                            logger.error("Error registering window", ex);
                        }
                    }
                    @Override
                    public void onError(Ack ack) {
                        logger.error("Error registering java window ", ack.getReason());
                    }
                });
        } catch (Exception e) {
            logger.error("Error registering external window", e);
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        if ("undock-window".equals(e.getActionCommand())) {
            undockFromOtherWindows();
        }
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
        jFrame = new JFrame("Another Java Docking Demo");
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        DockingDemo2 newContentPane = new DockingDemo2();
        newContentPane.setOpaque(true); //content panes must be opaque
        jFrame.setContentPane(newContentPane);
        jFrame.addWindowListener(newContentPane);

        //Display the window.
        jFrame.pack();
        jFrame.setSize(470, 300);
        jFrame.setLocation(600, 400);
//        jFrame.setLocationRelativeTo(null);
        jFrame.setResizable(false);
        jFrame.setVisible(true);
    }

    private void shutdown() {
        logger.debug("shutting down");
        if (externalWindowObserver != null) {
            logger.debug("Closing");
            try {
                JSONObject request = new JSONObject();
                request.put("applicationUuid", javaParentAppUuid);
                request.put("windowName", javaWindowName);
                desktopConnection.getInterApplicationBus().publish("unregister-docking-window", request);
                Thread.sleep(2000);  // give time to flush messages to Runtime
                externalWindowObserver.dispose();
                desktopConnection.disconnect();
                Thread.sleep(1000);  // give time to flush messages to Runtime
            } catch (Exception ex) {
                logger.error("Error existing", ex);
            }
        }
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

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        shutdown();
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
}


