package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.System;
import info.clearthought.layout.TableLayout;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by richard on 2/28/15.
 */
public class OpenFinDockingDemo extends JPanel implements ActionListener, WindowListener {


    private static JFrame jFrame;

    protected JButton launch;
    protected JButton close;

    protected JButton minimizeButton, maximizeButton, restoreButton;
    protected JButton largerButton, smallerButton, upButton, downButton, rightButton, leftButton;

    protected JButton createDockWindow;

    protected java.util.List<ApplicationOptions> appOptionsList;
    protected java.util.Map<String, Application> applicationList;
    protected Application selectedApplication;

    //    Application admin;
    InterApplicationBus bus;

    protected DesktopConnection controller;
    protected AppCreateDialog appCreateDialog;
    protected LoadAppsDialog loadAppsDialog;

    protected JTextArea status;
    private final String desktop_path;
    private final String desktopCommandLine;
    private String authorizationToken;

    private JLabel uuidLabel, nameLabel, versionLabel, urlLabel, adminLabel, resizeLabel, autoShowLabel, draggableLabel, onBottomLabel, frameLabel, taskIconLabel;
    private DockingDemo dockingOne;
    private DockingDemo dockingTwo;
    private boolean isDocking = false;
    public OpenFinDockingDemo(final String desktop_path, final String desktopCommandLine, final int port) {
        authorizationToken = UUID.randomUUID().toString();  //"e5683b8c";
        try {
            this.controller = new DesktopConnection("JavaDocking", "localhost", port);
        } catch (DesktopException desktopError) {
            desktopError.printStackTrace();
        }
        this.desktop_path = desktop_path;
        this.desktopCommandLine = desktopCommandLine;
        this.appCreateDialog = new AppCreateDialog();
        this.loadAppsDialog = new LoadAppsDialog();
        isDocking = true;

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

        this.appOptionsList = new ArrayList<ApplicationOptions>();
        this.applicationList = new HashMap<String, Application>();

        return panel;
    }

    private JPanel layoutActionButtonPanel() {
        JPanel buttonPanel = new JPanel();

        JPanel topPanel = new JPanel();
        double size[][] = {{100, 90, 20, 90, 100}, {25, 10, 25, 10}};
        topPanel.setLayout(new TableLayout(size));
        topPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(2), "Desktop"));

        launch = new JButton("Start");
        launch.setActionCommand("start");
        close = new JButton("Close");
        close.setActionCommand("close");
        topPanel.add(launch, "1,0,1,0");
        topPanel.add(close, "3,0,3,0");

        createDockWindow = new JButton("Creating Docking Windows");
        createDockWindow.setActionCommand("dock-window");
        createDockWindow.setEnabled(false);
        topPanel.add(createDockWindow, "1,2,3,2");


        close.addActionListener(this);
        launch.addActionListener(this);
        createDockWindow.addActionListener(this);

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

    private void closeWebSocket() {
        if (controller != null && controller.isConnected()) {
            controller.disconnect();
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



    public void init() {
        dockingOne = DockingDemo.createAndShowGUI(jFrame, "Dock1");
        dockingTwo = DockingDemo.createAndShowGUI(jFrame, "Dock2");
        Rectangle bounds = dockingOne.getFrame().getBounds();
        bounds.setLocation((int)bounds.getX() + 450, (int)(bounds.getY() - 125));
        dockingOne.getFrame().setBounds(bounds);
        bounds.setLocation((int)bounds.getX(), (int)(bounds.getY() + 250));
        dockingTwo.getFrame().setBounds(bounds);
        dockingOne.setApplication(dockingApp);
        dockingTwo.setApplication(dockingApp);
    }

    Application dockingApp;

    private void createDockingApplication() {
        ApplicationOptions options = new ApplicationOptions("Docking Demo", "dockingapp", "https://developer.openf.in/docking/1.2.0.0b/docking.html");
        dockingApp = new Application(options, controller, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                try {
                    ((Application) ack.getSource()).run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onError(Ack ack) {
            }
        });


        updateMessagePanel("Creating InterAppBus");
        bus = controller.getInterApplicationBus();

        try {
            bus.subscribe("dockingapp", "utils-ready", new BusListener() {
                @Override
                public void onMessageReceived(String sourceUuid, String topic, Object payload) {
                    updateMessagePanel("Docking Demo ready");
                    init();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runStartAction() {
        try {
            controller.launchAndConnect(desktop_path, desktopCommandLine, new DesktopStateListener() {
                @Override
                public void onReady() {
                    updateMessagePanel("Connection authorized.");
                    //                createAdminApplication();
                    setMainButtonsEnabled(true);
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

            }, 10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        if ("start".equals(e.getActionCommand())) {
            runStartAction();
        } else if ("close".equals(e.getActionCommand())) {
            closeDesktop();
        } else if ("create-application".equals(e.getActionCommand())) {
            this.appCreateDialog.show(this);
            ApplicationOptions options = this.appCreateDialog.getApplicatonOptions();
            if (options != null) {
                createApplication(options);
            }
        } else if ("dock-window".equals(e.getActionCommand())) {
            createDockingApplication();
        } else if ("load-apps".equals(e.getActionCommand())) {
            JSONObject message = this.loadAppsDialog.getCredentials();
            if (message != null) {
                retrieveApplications(message);
            }
        }
    }


    private void setMainButtonsEnabled(boolean enabled) {
        launch.setEnabled(!enabled);
        createDockWindow.setEnabled(enabled);
        close.setEnabled(enabled);
    }

    private void setAppButtonsEnabled(boolean enabled) {
    }

    private void retrieveApplications(JSONObject message) {
        bus = controller.getInterApplicationBus();
        try {
            bus.send("ExternalClientUtils", "get-user-app-settings", message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createApplication(final ApplicationOptions options) {
        Application app = new Application(options, controller, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                Application application = (Application) ack.getSource();
                try {
                    application.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                addApplication(options);
            }
            @Override
            public void onError(Ack ack) {
            }
        });
        this.applicationList.put(options.getUUID(), app);
        this.dockingOne.setApplication(app);
        this.dockingTwo.setApplication(app);
    }

    private void addApplication(ApplicationOptions options) {
        setAppButtonsEnabled(true);
        this.appOptionsList.add(options);
    }

    private void descApplication(ApplicationOptions options) {
//        this.uuidLabel.setText(" UUID: " + options.getUUID());
//        this.nameLabel.setText(" Name: " + options.getName());
//        this.versionLabel.setText(" Version: " + options.getVersion());
//        this.urlLabel.setText(" URL: " + options.getURL());
//        this.adminLabel.setText(" admin: " + getBooleanString(options.getIsAdmin()));
//        this.resizeLabel.setText(" resize: " + getBooleanString(options.getResize()));
//        this.frameLabel.setText(" frame: " + getBooleanString(options.getFrame()));
//        this.autoShowLabel.setText(" autoShow: " + getBooleanString(options.getAutoShow()));
//        this.draggableLabel.setText(" draggable: " + getBooleanString(options.getDraggable()));
//        this.onBottomLabel.setText(" alwaysOnBottom: " + getBooleanString(options.getAlwaysOnBottom()));
//        this.taskIconLabel.setText(" showTaskBarIcon: " + getBooleanString(options.getShowTaskbarIcon()));

        this.selectedApplication = this.applicationList.get(options.getUUID());
        try {
            selectedApplication.getWindow().bringToFront();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getBooleanString(boolean value) {
        return value ? "Y" : "N";
    }

    private static void createAndShowGUI(final String desktop_path, final String desktopCommandLine, int port) {
        //Create and set up the window.
        jFrame = new JFrame("Java Docking Demo");
        jFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        //Create and set up the content pane.
        OpenFinDockingDemo newContentPane = new OpenFinDockingDemo(desktop_path, desktopCommandLine, port);
        newContentPane.setOpaque(true); //content panes must be opaque
        jFrame.setContentPane(newContentPane);
        jFrame.addWindowListener(newContentPane);

        //Display the window.
        jFrame.pack();
        jFrame.setSize(450, 500);
        jFrame.setLocationRelativeTo(null);
        jFrame.setResizable(false);
        jFrame.setVisible(true);

    }

    public static void main(String[] args) {
        if (args.length >= 2) {
            final String desktop_path = args[0];
            final int port = Integer.parseInt(args[1]);
            java.lang.System.out.println("Starting Demo: " + desktop_path + " " + port);
            String desktop_cmd_line = "";
            if (args.length > 2) {
                for (int i = 2; i < args.length; i++) {
                    desktop_cmd_line += (args[i] + " ");
                }
                java.lang.System.out.println("Passing desktop args: " + desktop_cmd_line);
            }
            final String desktopCommandLine = desktop_cmd_line;
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    createAndShowGUI(desktop_path, desktopCommandLine, port);
                }
            });
        } else {
            java.lang.System.err.println("Required args: [path to openfin.exe] [WebSocket port]");
        }
    }
}
