package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.System;
import com.openfin.desktop.Window;
import com.openfin.desktop.animation.AnimationTransitions;
import com.openfin.desktop.animation.OpacityTransition;
import com.openfin.desktop.animation.PositionTransition;
import com.openfin.desktop.notifications.*;
import com.openfin.desktop.notifications.NotificationOptions;
import com.openfin.desktop.notifications.events.NotificationActionEvent;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.HashMap;

import jdk.nashorn.internal.scripts.JO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * GUI example that allows for instantiating and controlling
 *
 * Supported system properties by this demo:
 *
 *  1. to specify security realm for OpenFin Runtime
 *      -Dcom.openfin.demo.security.realm=SampleRealm
 *
 *  2. to specify version of OpenFin Runtime
 *      -Dcom.openfin.demo.version=5.44.8.56
 *
 *  3. to specify RDM URL
 *      -Dcom.openfin.demo.rdmURL=rdm_url
 *
 *  4. to specify URL of RVM&Runtime URL
 *      -Dcom.openfin.demo.assetsURL=assets_url
 *
 *  4. to connect to a port of a running instance of OpenFin Runtime (this is a deprecated use of OpenFin API
 *      -Dcom.openfin.demo.port=9000
 *
 *
 * Created by wche on 2/28/15.
 *
 */
public class OpenFinDesktopDemo extends JPanel implements ActionListener, WindowListener {
    private final static Logger logger = LoggerFactory.getLogger(OpenFinDesktopDemo.class.getName());


    private static JFrame jFrame;

    protected JButton launch;
    protected JButton close;

    protected JButton minimizeButton, maximizeButton, restoreButton;
    protected JButton largerButton, smallerButton, upButton, downButton, rightButton, leftButton;

    protected JButton createApplication;
    protected JButton createNotification;
    private Notifications notifications;

    protected JList activeApplications;
    protected java.util.List<ApplicationOptions> appOptionsList;
    protected java.util.Map<String, Application> applicationList;
    protected Application selectedApplication;

    protected JList apiListBox;
    protected int currentApiTestCaseIndex = -1;

    InterApplicationBus bus;

    protected DesktopConnection desktopConnection;
    protected RuntimeConfiguration runtimeConfiguration;
    protected int desktopPort = -1; // if set, assuming Runtime is already running on the port
    protected System openfinSystem;
    protected AppCreateDialog appCreateDialog;
    protected LoadAppsDialog loadAppsDialog;

    protected JTextArea status;

    private JLabel uuidLabel, nameLabel, versionLabel, urlLabel, resizeLabel, autoShowLabel, frameLabel;


    public OpenFinDesktopDemo() {
        this.appCreateDialog = new AppCreateDialog();
        this.loadAppsDialog = new LoadAppsDialog();

        setLayout(new BorderLayout());

        add(layoutCenterPanel(), BorderLayout.CENTER);
        add(layoutLeftPanel(), BorderLayout.WEST);
        setMainButtonsEnabled(false);
        setAppButtonsEnabled(false);
    }

    private void initDesktopConnection() throws DesktopException {
        this.runtimeConfiguration = new RuntimeConfiguration();
        String connectionUuid = "OpenFinDesktopDemoJava";
        if (java.lang.System.getProperty("com.openfin.demo.connectionUuid") != null) {
            connectionUuid = java.lang.System.getProperty("com.openfin.demo.connectionUuid");
        }
        if (java.lang.System.getProperty("com.openfin.demo.port") != null) {
            this.desktopPort = Integer.parseInt(java.lang.System.getProperty("com.openfin.demo.port"));
        }
        if (this.desktopPort > 0) {
            this.desktopConnection = new DesktopConnection("OpenFinDesktopDemoJava", "localhost", this.desktopPort);
        } else {
            this.desktopConnection = new DesktopConnection(connectionUuid);
        }
        String securityRealm = null;
        if (java.lang.System.getProperty("com.openfin.demo.security.realm") != null) {
            securityRealm = java.lang.System.getProperty("com.openfin.demo.security.realm");
        }
        if (securityRealm != null) {
            this.runtimeConfiguration.setSecurityRealm(securityRealm);
        }
        String desktopVersion = java.lang.System.getProperty("com.openfin.demo.version");
        if (desktopVersion == null) {
            desktopVersion = "stable";
        }
        this.runtimeConfiguration.setLocalManifestFileName("OpenFinJavaDemo");
        this.runtimeConfiguration.setRuntimeVersion(desktopVersion);

        String fallBackVersion = java.lang.System.getProperty("com.openfin.demo.fallBackVersion");
        if (fallBackVersion != null) {
            this.runtimeConfiguration.setRuntimeFallbackVersion(fallBackVersion);
        }
        String rvmArgs = java.lang.System.getProperty("com.openfin.demo.rvm.arguments");
        if (rvmArgs != null) {
            updateMessagePanel("Additional RVM arguments: " + rvmArgs);
            this.runtimeConfiguration.setAdditionalRvmArguments(rvmArgs);
        }
        this.runtimeConfiguration.setAdditionalRuntimeArguments("--v=1  ");  // enable additional logging
        this.runtimeConfiguration.setDevToolsPort(9090);

        JSONObject startupApp = new JSONObject();
        startupApp.put("uuid", "DEMO-JAVA");
        startupApp.put("name", "DEMO-JAVA");
        startupApp.put("url", "https://cdn.openfin.co/process-manager/index.html");
        startupApp.put("autoShow", true);

        String licenseKey = java.lang.System.getProperty("com.openfin.demo.licenseKey");
        if (licenseKey != null) {
            this.runtimeConfiguration.setLicenseKey(licenseKey);
        }
        JSONObject myconfig = new JSONObject();
//        this.runtimeConfiguration.setManifestLocation("https://cdn.openfin.co/process-manager/app.json");
//        myconfig.put("key1", "value1");
//        myconfig.put("PI", 3.14);
        this.runtimeConfiguration.addConfigurationItem("myconfig", myconfig);
    }

    private void addCustomConfig(JSONObject startupApp) {
        JSONObject defaultDomainSettings = new JSONObject();
        JSONArray rules = new JSONArray();
        JSONObject rule1 = new JSONObject();
        JSONArray matchList = new JSONArray();
        matchList.put("*/*.example.com");
        rule1.put("match", matchList);
        JSONObject options = new JSONObject();
        JSONObject downloadSettings = new JSONObject();
        JSONArray downloadSettingsRules = new JSONArray();
        JSONObject downloadSettingsRule1 = new JSONObject();
        JSONArray matchList1 = new JSONArray();
        matchList1.put("*://*/*.png");
        matchList1.put("*://*/*.jpg");
        downloadSettingsRule1.put("match", matchList1);
        downloadSettingsRule1.put("behavior", "no-prompt");
        downloadSettingsRules.put(downloadSettingsRule1);
        downloadSettings.put("rules", downloadSettingsRules);
        options.put("downloadSettings", downloadSettings);
        rule1.put("options", options);
        rules.put(rule1);
        defaultDomainSettings.put("rules", rules);
        startupApp.put("defaultDomainSettings", defaultDomainSettings);
        this.runtimeConfiguration.setStartupApp(startupApp);
    }

    private JPanel layoutLeftPanel() {
        JPanel panel = new JPanel();
        double size[][] = {{TableLayout.FILL}, {160, 30, TableLayout.FILL}};
        panel.setLayout(new TableLayout(size));

        panel.add(layoutActionButtonPanel(), "0,0,0,0");

        JLabel label = new JLabel(" Active Applications");
        panel.add(label, "0,1,0,1");

        this.appOptionsList = new ArrayList<ApplicationOptions>();
        this.applicationList = new HashMap<String, Application>();
        this.activeApplications = new JList(new DefaultListModel());
        this.activeApplications.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        this.activeApplications.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        this.activeApplications.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                int selected = activeApplications.getSelectedIndex();
                if (selected >= 0) {
                    descApplication(appOptionsList.get(selected));
                }
            }
        });
        panel.add(this.activeApplications, "0,2,0,2");

        return panel;
    }

    private JPanel layoutActionButtonPanel() {
        JPanel buttonPanel = new JPanel();

        JPanel topPanel = new JPanel();
        double size[][] = {{0.5, 0.5}, {30}};
        topPanel.setLayout(new TableLayout(size));
        topPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(2), "Desktop"));

        launch = new JButton("Start");
        launch.setActionCommand("start");
        close = new JButton("Close");
        close.setActionCommand("close");
        topPanel.add(launch, "0,0,0,0");
        topPanel.add(close, "1,0,1,0");

        size = new double[][]{{TableLayout.FILL}, {60, 30, 30, 30}};
        buttonPanel.setLayout(new TableLayout(size));

        createApplication = new JButton("Create Application");
        createApplication.setActionCommand("create-application");

        createNotification = new JButton("Create Notification");
        createNotification.setActionCommand("create-notification");

        close.addActionListener(this);
        launch.addActionListener(this);
        createApplication.addActionListener(this);
        createNotification.addActionListener(this);

        buttonPanel.add(topPanel, "0,0");
        buttonPanel.add(createApplication, "0,1");
        buttonPanel.add(createNotification, "0,2");
        return buttonPanel;
    }

    private JPanel layoutCenterPanel() {
        JPanel panel = new JPanel();
        double size[][] = {{TableLayout.FILL}, {150, 150, TableLayout.FILL}};
        panel.setLayout(new TableLayout(size));

        panel.add(layoutAppDescriptionPanel(), "0,0,0,0");

        panel.add(layoutWindowControlPanel(), "0,1,0,1");
        panel.add(layoutStatusPanel(), "0,2,0,2");

        return panel;
    }

    private JPanel layoutAppDescriptionPanel() {
        JPanel panel = new JPanel();
        double size[][] = {{TableLayout.FILL, 100}, {20, 20, 20, 20, 20, 20, 20}};
        panel.setLayout(new TableLayout(size));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(2), "Application Settings"));

        panel.add(uuidLabel = new JLabel(), "0, 0, 0, 0");
        panel.add(nameLabel = new JLabel(), "0, 1, 0, 1");
        panel.add(versionLabel = new JLabel(), "0, 2, 0, 2");
        panel.add(urlLabel = new JLabel(), "0, 3, 0, 3");

        panel.add(resizeLabel = new JLabel(), "1, 1, 1, 1");
        panel.add(frameLabel = new JLabel(), "1, 2, 1, 2");
        panel.add(autoShowLabel = new JLabel(), "1, 3, 1, 3");

        return panel;
    }

    private JPanel layoutWindowControlPanel() {
        JPanel panel = new JPanel();
        double size[][] = {{50, 50, 50, 10, 70, 70, 70, 10, 70, 70, TableLayout.FILL}, {0.33, 0.33, 0.33}};
        panel.setLayout(new TableLayout(size));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createBevelBorder(2), "Window Control"));

        JButton button;
        panel.add(button = new JButton(new ImageIcon(getClass().getResource("/img/minimize.png"))), "4, 0");
        button.setActionCommand("minimize");
        button.addActionListener(this);
        minimizeButton = button;

        panel.add(button = new JButton(new ImageIcon(getClass().getResource("/img/maximize.png"))), "5, 0");
        button.setActionCommand("maximize");
        button.addActionListener(this);
        maximizeButton = button;

        panel.add(button = new JButton(new ImageIcon(getClass().getResource("/img/restore.png"))), "6, 0");
        button.setActionCommand("restore");
        button.addActionListener(this);
        restoreButton = button;

        Font f = new Font("Verdana", Font.BOLD, 24);
        panel.add(button = new JButton("+"), "8, 0");
        button.setActionCommand("resize+");
        button.addActionListener(this);
        button.setFont(f);
        largerButton = button;

        panel.add(button = new JButton("-"), "9, 0");
        button.setActionCommand("resize-");
        button.addActionListener(this);
        button.setFont(f);
        smallerButton = button;

        panel.add(button = new JButton("\u2191"), "1, 0");
        button.setActionCommand("move up");
        button.addActionListener(this);
        upButton = button;

        panel.add(button = new JButton("\u2192"), "2, 1");
        button.setActionCommand("move right");
        button.addActionListener(this);
        rightButton = button;

        panel.add(button = new JButton("\u2193"), "1, 2");
        button.setActionCommand("move down");
        button.addActionListener(this);
        downButton = button;

        panel.add(button = new JButton("\u2190"), "0,1");
        button.setActionCommand("move left");
        button.addActionListener(this);
        leftButton = button;

        this.apiListBox = new JList(new DefaultListModel());
        this.apiListBox.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        this.apiListBox.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        ((DefaultListModel) this.apiListBox.getModel()).addElement("Opacity");
        ((DefaultListModel) this.apiListBox.getModel()).addElement("Rounded Corners");
        ((DefaultListModel) this.apiListBox.getModel()).addElement("Opacity Animation");
        ((DefaultListModel) this.apiListBox.getModel()).addElement("Position Animation");
        this.apiListBox.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                int selected = apiListBox.getSelectedIndex();
                if (selected != currentApiTestCaseIndex) {
                    currentApiTestCaseIndex = selected;
                    switch (selected) {
                        case 0:
                            testOpacity();
                            break;
                        case 1:
                            testRoundedCorners();
                            break;
                        case 2:
                            testOpacityAnimation();
                            break;
                        case 3:
                            testPositionAnimation();
                            break;
                    }
                }
            }
        });
        panel.add(this.apiListBox, "4,1,9,2");

        return panel;
    }


    protected JScrollPane layoutStatusPanel() {
        //Simple status console
        status = new JTextArea(100, 40);
        status.setEditable(false);
        status.setAutoscrolls(true);
        status.setLineWrap(true);
        status.setMinimumSize(new Dimension(40, 100));
        status.setPreferredSize(new Dimension(40, 100));
        JScrollPane statusPane = new JScrollPane(status);
        statusPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        statusPane.setPreferredSize(new Dimension(500, 650));
        return statusPane;
    }

    private void closeWebSocket() {
        if (desktopConnection != null && desktopConnection.isConnected()) {
            try {
                desktopConnection.disconnect();
            } catch (DesktopException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        closeDesktop();
        try {
            Thread.sleep(1000);
            java.lang.System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        java.lang.System.out.println(msg);
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

    private void resetUI() {
        setMainButtonsEnabled(false);
        setAppButtonsEnabled(false);
        ((DefaultListModel) this.activeApplications.getModel()).clear();
        this.applicationList.clear();
        this.appOptionsList.clear();
    }

    private void closeDesktop() {
        if (desktopConnection != null && desktopConnection.isConnected()) {
            try {
                desktopConnection.disconnect();
//                this.desktopConnection.disconnect();
//                Application app = Application.wrap(this.startupUUID, this.desktopConnection);
//                app.close();
                resetUI();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
//                jFrame.dispose();
            }
        });
        try {
            Thread.sleep(1000);
//            java.lang.System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    private void createAdminApplication() throws DesktopException {
        updateMessagePanel("Creating InterAppBus");
        bus = desktopConnection.getInterApplicationBus();
        openfinSystem = new System(desktopConnection);

        this.notifications = new Notifications(desktopConnection);
        this.notifications.addEventListener(Notifications.EVENT_TYPE_ACTION, ne ->{
            NotificationActionEvent actionEvent = (NotificationActionEvent) ne;
            NotificationActionResult actionResult = actionEvent.getResult();
            java.lang.System.out.println("actionResult: notificationId: " + actionEvent.getNotificationOptions().getId() + ", user clicked on btn: " + actionResult.getString("btn"));
        });

        updateMessagePanel("Connected to Desktop");
        setMainButtonsEnabled(true);

        openfinSystem.addEventListener("desktop-icon-clicked", new EventListener() {
            @Override
            public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                updateMessagePanel("desktop-icon-clicked");
            }
        }, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
            }
            @Override
            public void onError(Ack ack) {
                java.lang.System.out.println("error with desktop-icon-clicked " + ack.getJsonObject().toString());
            }
        });

        try {
            bus.subscribe("*", "demo-topic", new BusListener() {
                @Override
                public void onMessageReceived(String sourceUuid, String topic, Object payload) {
                    java.lang.System.out.println(String.format("Message from %s: %s ", sourceUuid, payload.toString()));
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init() {
        runStartAction();
    }

    private void runStartAction() {
        try {
            initDesktopConnection();
        } catch (DesktopException desktopError) {
            desktopError.printStackTrace();
        }

        final DesktopStateListener listener = new DesktopStateListener() {
            @Override
            public void onReady() {
                try {
                    updateMessagePanel("Connection authorized.");
                    createAdminApplication();
                } catch (DesktopException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(String error) {
                updateMessagePanel(String.format("Connection closed %s", error));
                resetUI();
            }

            @Override
            public void onError(String reason) {
                updateMessagePanel("Connection failed: " + reason);

                if (!desktopConnection.isConnected()) {

                }
            }

            @Override
            public void onMessage(String message) {
//                updateMessagePanel("-->FROM DESKTOP-" + message);
            }

            @Override
            public void onOutgoingMessage(String message) {
//                updateMessagePanel("<--TO DESKTOP-" + message);
            }
        };

        try {
            if (this.desktopPort > 0) {
                this.runtimeConfiguration.setRuntimePort(this.desktopPort);
                updateMessagePanel("Connecting to Runtime already running at port " + this.desktopPort);
                this.runtimeConfiguration.setMaxMessageSize(1024*1024);
                desktopConnection.connect(this.runtimeConfiguration, listener, 20);
            } else {
                updateMessagePanel("Connecting to version " + this.runtimeConfiguration.getRuntimeVersion());
                desktopConnection.connect(this.runtimeConfiguration, listener, 20);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent e) {
        try {
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
            } else if ("create-notification".equals(e.getActionCommand())) {
                createNotification();
                bus.send("*", "test", "Hello from Java");
            } else if ("minimize".equals(e.getActionCommand())) {
                if (this.selectedApplication != null) {
                    this.selectedApplication.getWindow().minimize();
                }
            } else if ("maximize".equals(e.getActionCommand())) {
                if (this.selectedApplication != null) {
                    this.selectedApplication.getWindow().maximize();
                }
            } else if ("restore".equals(e.getActionCommand())) {
                if (this.selectedApplication != null) {
                    this.selectedApplication.getWindow().restore();
                }
            } else if ("resize+".equals(e.getActionCommand())) {
                if (this.selectedApplication != null) {
                    this.selectedApplication.getWindow().resizeBy(10, 10, "top-left");
                }
            } else if ("resize-".equals(e.getActionCommand())) {
                if (this.selectedApplication != null) {
                    this.selectedApplication.getWindow().resizeBy(-10, -10, "top-left");
                }
            } else if ("move left".equals(e.getActionCommand())) {
                if (this.selectedApplication != null) {
                    this.selectedApplication.getWindow().moveBy(-10, 0, null);
                }
            } else if ("move right".equals(e.getActionCommand())) {
                if (this.selectedApplication != null) {
                    this.selectedApplication.getWindow().moveBy(10, 0, null);
                }
            } else if ("move up".equals(e.getActionCommand())) {
                if (this.selectedApplication != null) {
                    this.selectedApplication.getWindow().moveBy(0, -10, null);
                }
            } else if ("move down".equals(e.getActionCommand())) {
                if (this.selectedApplication != null) {
                    this.selectedApplication.getWindow().moveBy(0, 10, null);


                    this.selectedApplication.getWindow().getNativeId(new AckListener() {
                        @Override
                        public void onSuccess(Ack ack) {
                            java.lang.System.out.println(ack.getJsonObject().toString());
                        }

                        @Override
                        public void onError(Ack ack) {
                            java.lang.System.out.println("error with getNativeId");

                        }
                    });
                }
            }
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void createNotification() throws Exception {
        NotificationOptions opt = new NotificationOptions("Notification from Java", "Write once, run everywhere", "Category");
        opt.setSticky(NotificationOptions.STICKY_STICKY);
        opt.setIndicator(new NotificationIndicator(NotificationIndicator.TYPE_SUCCESS));
        ButtonOptions bo1 = new ButtonOptions("Button 1");
        bo1.setOnClick(new NotificationActionResult(new JSONObject().put("btn", "btn1")));
        ButtonOptions bo2 = new ButtonOptions("Button 2");
        bo2.setOnClick(new NotificationActionResult(new JSONObject().put("btn", "btn2")));
        bo2.setCta(true);
        opt.setButtons(bo1, bo2);

        this.notifications.create(opt);
    }

    private void testOpacity() {
        this.selectedApplication.getWindow().getOptions(new AsyncCallback<WindowOptions>() {
            @Override
            public void onSuccess(WindowOptions result) {
                java.lang.System.out.println("getOptions: " + result.getJson().toString());
                double opacity = result.getOpacity() > 0.5 ? 0.5 : 1;
                WindowOptions options = new WindowOptions();
                options.setOpacity(opacity);
                selectedApplication.getWindow().updateOptions(options, null);
            }
        }, null);
    }

    private void testRoundedCorners() {
        ApplicationOptions opts = appOptionsList.get(0);
        OpenFinRuntime runtime = new OpenFinRuntime(this.desktopConnection);
        try {
            runtime.showDeveloperTools(opts.getUUID(), opts.getUUID(), null);

            Window w = Window.wrap(opts.getUUID(), opts.getUUID(), this.desktopConnection);
            WindowOptions wopts = new WindowOptions();
            wopts.setAutoShow(true);
            wopts.setContextMenu(true);
            w.updateOptions(wopts, null);

        } catch (DesktopException e) {
            e.printStackTrace();
        }
    }

    private void testOpacityAnimation() {
        this.selectedApplication.getWindow().getOptions(new AsyncCallback<WindowOptions>() {
            @Override
            public void onSuccess(WindowOptions result) {
                double opacity = result.getOpacity() > 0.5 ? 0.5 : 1;
                OpacityTransition ot = new OpacityTransition();
                ot.setDuration(3000);
                ot.setOpacity(opacity);
                AnimationTransitions at = new AnimationTransitions();
                at.setOpacity(ot);
                selectedApplication.getWindow().animate(at, null, null);
            }
        }, null);
    }

    private void testPositionAnimation() {
        this.selectedApplication.getWindow().getBounds(new AsyncCallback<WindowBounds>() {
            @Override
            public void onSuccess(WindowBounds result) {
                int top = result.getTop();
                top = top > 100? 100 : 400;
                PositionTransition pt = new PositionTransition(top, top, 3000);
                AnimationTransitions at = new AnimationTransitions();
                at.setPosition(pt);
                selectedApplication.getWindow().animate(at, null, null);
            }
        }, null);
    }

    private void setMainButtonsEnabled(boolean enabled) {
        java.lang.System.out.println("setMainButtonsEnabled " + enabled);
        launch.setEnabled(!enabled);

        close.setEnabled(enabled);

        createApplication.setEnabled(enabled);
        createNotification.setEnabled(enabled);

    }

    private void setAppButtonsEnabled(boolean enabled) {
        largerButton.setEnabled(enabled);
        smallerButton.setEnabled(enabled);
        upButton.setEnabled(enabled);
        downButton.setEnabled(enabled);
        rightButton.setEnabled(enabled);
        leftButton.setEnabled(enabled);

    }

    private void createApplication(final ApplicationOptions options) {
        options.getMainWindowOptions().setContextMenu(true);
        Application app = new Application(options, desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                Application application = (Application) ack.getSource();
                try {
                    application.run(new AckListener() {
                        public void onSuccess(Ack ack) {
                            java.lang.System.out.println("Intalling minimized event listener");
                            Application application = (Application) ack.getSource();
                            application.getWindow().addEventListener("minimized", new EventListener() {
                                public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                                    java.lang.System.out.println("eventReceived " + actionEvent.getType());
                                }
                            }, null);
                        }
                        public void onError(Ack ack) {
                        }
                    });

                    // add a window event listener
                    java.lang.System.out.println("Adding app-loaded event");
                    Window w = Window.wrap(options.getUUID(), "child0", desktopConnection);
                    w.addEventListener("app-loaded", new EventListener() {
                        public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                            java.lang.System.out.println("Received " + actionEvent.getType());
                        }
                    }, new AckListener() {
                        public void onSuccess(Ack ack) {
                            java.lang.System.out.println("app-loaded added");
                        }
                        public void onError(Ack ack) {
                            java.lang.System.out.println("failed to app-loaded");
                        }
                    });

                    // add a window event listener
                    java.lang.System.out.println("Adding app-connected event");
                    w.addEventListener("app-connected", new EventListener() {
                        public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                            java.lang.System.out.println("Received " + actionEvent.getType());
                        }
                    }, new AckListener() {
                        public void onSuccess(Ack ack) {
                            java.lang.System.out.println("app-connected added");
                        }
                        public void onError(Ack ack) {
                            java.lang.System.out.println("failed to app-connected");
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
                addApplication(options);
            }

            @Override
            public void onError(Ack ack) {
                java.lang.System.out.println(String.format("Error creating application: %s", ack.getReason()));
            }
        });
        this.applicationList.put(options.getUUID(), app);
    }

    private void addApplication(ApplicationOptions options) {
        setAppButtonsEnabled(true);
        ((DefaultListModel) this.activeApplications.getModel()).addElement(" " + options.getName());
        this.appOptionsList.add(options);
        this.activeApplications.setSelectedIndex(this.appOptionsList.size() - 1);
    }

    private void descApplication(ApplicationOptions options) {
        java.lang.System.out.println("descApplication " + options.getName());
        this.uuidLabel.setText(" UUID: " + options.getUUID());
        this.nameLabel.setText(" Name: " + options.getName());
        this.versionLabel.setText(" Version: " + (options.getVersion() != null ? options.getVersion() : " " ));
        this.urlLabel.setText(" URL: " + (options.getURL() != null ? options.getURL() : " "));

        WindowOptions mainWindowOptions = options.getMainWindowOptions();
        this.resizeLabel.setText(" resize: " + getBooleanString(mainWindowOptions.getResizable()));
        this.frameLabel.setText(" frame: " + getBooleanString(mainWindowOptions.getFrame()));
        this.autoShowLabel.setText(" autoShow: " + getBooleanString(mainWindowOptions.getAutoShow()));

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

    private static void createAndShowGUI() {

        //Create and set up the window.
        jFrame = new JFrame("Java OpenFin Demo");
//        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        OpenFinDesktopDemo newContentPane = new OpenFinDesktopDemo();
        newContentPane.setOpaque(true); //content panes must be opaque
        jFrame.setContentPane(newContentPane);
        jFrame.addWindowListener(newContentPane);

        //Display the window.
        jFrame.pack();
        jFrame.setSize(700, 500);
        jFrame.setLocationRelativeTo(null);
        jFrame.setResizable(true);
        jFrame.setVisible(true);
        //newContentPane.init();
    }

    /**
     * To start OpenFin Desktop and Connect, pass full path of OpenFin with*
     *    -DOpenFinOption=--config=\"RemoteConfigUrl\"
     *
     * Set UUID of startup to control it
     *    -DStartupUUID="550e8400-e29b-41d4-a716-4466333333000"
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {


        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
