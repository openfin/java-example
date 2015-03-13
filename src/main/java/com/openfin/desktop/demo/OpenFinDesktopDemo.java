package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.System;
import com.openfin.desktop.animation.AnimationTransitions;
import com.openfin.desktop.animation.OpacityTransition;
import com.openfin.desktop.animation.PositionTransition;
import info.clearthought.layout.TableLayout;
import org.json.JSONObject;

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
import java.util.Random;
import java.util.logging.Logger;

/**
 *
 * GUI example that allows for instantiating and controlling
 *
 * Created by wche on 2/28/15.
 *
 */
public class OpenFinDesktopDemo extends JPanel implements ActionListener, WindowListener {
    private final static Logger logger = Logger.getLogger(OpenFinDesktopDemo.class.getName());


    private static JFrame jFrame;

    protected JButton launch;
    protected JButton close;

    protected JButton minimizeButton, maximizeButton, restoreButton;
    protected JButton largerButton, smallerButton, upButton, downButton, rightButton, leftButton;

    protected JButton createApplication;
    protected JButton createRfq;

    protected JList activeApplications;
    protected java.util.List<ApplicationOptions> appOptionsList;
    protected java.util.Map<String, Application> applicationList;
    protected Application selectedApplication;

    protected JList apiListBox;
    protected int currentApiTestCaseIndex = -1;

    InterApplicationBus bus;

    protected DesktopConnection controller;
    protected AppCreateDialog appCreateDialog;
    protected LoadAppsDialog loadAppsDialog;

    protected JTextArea status;
    private final String desktopCommandLine;
    private String startupUUID;

    private JLabel uuidLabel, nameLabel, versionLabel, urlLabel, adminLabel, resizeLabel, autoShowLabel, draggableLabel, frameLabel;


    public OpenFinDesktopDemo(final String desktopCommandLine, String startupUUID) {
        this.startupUUID = startupUUID;
        try {
            this.controller = new DesktopConnection("OpenFinDesktopDemoJava", "localhost", 9696);
        } catch (DesktopException desktopError) {
            desktopError.printStackTrace();
        }
        this.desktopCommandLine = desktopCommandLine;
        this.appCreateDialog = new AppCreateDialog();
        this.loadAppsDialog = new LoadAppsDialog();

        setLayout(new BorderLayout());

        add(layoutCenterPanel(), BorderLayout.CENTER);
        add(layoutLeftPanel(), BorderLayout.WEST);
        setMainButtonsEnabled(false);
        setAppButtonsEnabled(false);
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

        createRfq = new JButton("Create RFQ");
        createRfq.setActionCommand("create-rfq");

        close.addActionListener(this);
        launch.addActionListener(this);
        createApplication.addActionListener(this);
        createRfq.addActionListener(this);

        buttonPanel.add(topPanel, "0,0");
        buttonPanel.add(createApplication, "0,1");
//        buttonPanel.add(createRfq, "0,2");
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

        panel.add(adminLabel = new JLabel(), "1, 0, 1, 0");
        panel.add(resizeLabel = new JLabel(), "1, 1, 1, 1");
        panel.add(frameLabel = new JLabel(), "1, 2, 1, 2");
        panel.add(autoShowLabel = new JLabel(), "1, 3, 1, 3");
        panel.add(draggableLabel = new JLabel(), "1, 4, 1, 4");

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


    private void closeDesktop() {
        if (controller != null && controller.isConnected()) {
            try {
                new System(controller).exit();
//                Application app = Application.wrap(this.startupUUID, this.controller);
//                app.close();
                setMainButtonsEnabled(false);
                setAppButtonsEnabled(false);
                ((DefaultListModel) this.activeApplications.getModel()).clear();
                this.applicationList.clear();
                this.appOptionsList.clear();
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
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    private void createAdminApplication() {
        updateMessagePanel("Creating InterAppBus");
        bus = controller.getInterApplicationBus();
        updateMessagePanel("Connected to Desktop");
        setMainButtonsEnabled(true);

        if (this.startupUUID != null) {
            Application app = Application.wrap(this.startupUUID, this.controller);
            ApplicationOptions options = new ApplicationOptions("Startup App", this.startupUUID, null);
            options.setMainWindowOptions(new WindowOptions());
            this.applicationList.put(options.getUUID(), app);
            addApplication(options);

            app.addEventListener("closed", new EventListener() {
                @Override
                public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                    updateMessagePanel("startup app closed");
                }
            }, null);
        }
    }

    public void init() {
        runStartAction();
    }

    private void runStartAction() {
        final DesktopStateListener listener = new DesktopStateListener() {
            @Override
            public void onReady() {
                updateMessagePanel("Connection authorized.");
                createAdminApplication();
            }
            @Override
            public void onError(String reason) {
                updateMessagePanel("Connection failed: " + reason);

                if (!controller.isConnected()) {

                }
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

        try {
            controller.launchAndConnect(this.desktopCommandLine, listener, 10000);
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
            } else if ("create-rfq".equals(e.getActionCommand())) {
                InterApplicationBus bus = controller.getInterApplicationBus();

                org.json.JSONObject message = getRandomQuote(random.nextInt());

                bus.publish("price", message);

            } else if ("load-apps".equals(e.getActionCommand())) {
                this.loadAppsDialog.show(this);
                JSONObject message = this.loadAppsDialog.getCredentials();
                if (message != null) {
                    retrieveApplications(message);
                }
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

    private void testOpacity() {
        this.selectedApplication.getWindow().getOptions(new AsyncCallback<WindowOptions>() {
            @Override
            public void onSuccess(WindowOptions result) {
                java.lang.System.out.println("getOptions: " + result.toJsonObject().toString());
                double opacity = result.getOpacity() > 0.5 ? 0.5 : 1;
                WindowOptions options = new WindowOptions();
                options.setOpacity(opacity);
                selectedApplication.getWindow().updateOptions(options, null);
            }
        }, null);
    }

    private void testRoundedCorners() {
        this.selectedApplication.getWindow().getOptions(new AsyncCallback<WindowOptions>() {
            @Override
            public void onSuccess(WindowOptions result) {
                java.lang.System.out.println("getOptions: " + result.toJsonObject().toString());
                int width = result.getCornerRoundingWidth() > 0 ? 0 : 10;
                WindowOptions options = new WindowOptions();
                options.setCornerRounding(width, width);
                selectedApplication.getWindow().updateOptions(options, null);
            }
        }, null);
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

    final static public String ISSUER = "issuer";
    final static public String MATURITY = "maturity";
    final static public String COUPON = "coupon";
    final static public String SIZE = "size";
    final static public String CCY = "ccy";
    final static public String ACCOUNT = "account";
    final static public String PLATFORM = "platform";
    final static public String MARKET_DATA = "marketData";
    final static public String TYPE = "type";
    final static public String STATUS = "status";
    final static public String EXPIRATION_TIME = "expirationTime";
    final static public String ID = "id";
    final static public String PRICE = "price";

    private Random random = new Random();
    private static final String[] issuers = {"GS", "JPM", "UBS", "MS", "DB", "BC", "BA"};
    private static final String[] ccys = {"USD", "JPY", "GBP", "EUR", "CHF", "CAD"};
    private static final String[] accounts = {"ABC Capital", "DEF Capital", "GHI Capital"};
    private static final String[] platforms = {"Trade Desk"};
    private static final String[] types = {"ASK", "BID"};

    private JSONObject getRandomQuote(int id) {
        String issuer = issuers[random.nextInt(issuers.length)];
        String year = new Integer(random.nextInt(11)).toString();
        if (year.length() == 1) {
            year = "0" + year;
        }
        String maturity = new Integer(random.nextInt(12) + 1).toString() + "/" + new Integer(random.nextInt(30) + 1).toString() + "/" + year;
        double coupon = Math.abs(1 * random.nextGaussian()) + 5;
        int size = 1000000 * (random.nextInt(10) + 1);
        String ccy = ccys[random.nextInt(ccys.length)];
        String account = accounts[random.nextInt(accounts.length)];
        String platform = platforms[random.nextInt(platforms.length)];
        double marketData = Math.abs(200 * random.nextGaussian() + 1000);
        String type = types[random.nextInt(types.length)];
        int expirationTime = 30;

        JSONObject quote = new JSONObject();

        try {
            quote.put(ISSUER, issuer);
            quote.put(MATURITY, maturity);
            quote.put(COUPON, coupon);
            quote.put(SIZE, size);
            quote.put(CCY, ccy);
            quote.put(ACCOUNT, account);
            quote.put(PLATFORM, platform);
            quote.put(MARKET_DATA, marketData);
            quote.put(TYPE, type);
            quote.put(EXPIRATION_TIME, expirationTime);
            quote.put(ID, id);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return quote;
    }

    private void setMainButtonsEnabled(boolean enabled) {
        launch.setEnabled(!enabled);

        close.setEnabled(enabled);

        createApplication.setEnabled(enabled);
        createRfq.setEnabled(enabled);

    }

    private void setAppButtonsEnabled(boolean enabled) {
        largerButton.setEnabled(enabled);
        smallerButton.setEnabled(enabled);
        upButton.setEnabled(enabled);
        downButton.setEnabled(enabled);
        rightButton.setEnabled(enabled);
        leftButton.setEnabled(enabled);

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

    private static void createAndShowGUI(final String desktopCommandLine, String startupUUID) {

        //Create and set up the window.
        jFrame = new JFrame("Java Login Demo");
        jFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        //Create and set up the content pane.
        OpenFinDesktopDemo newContentPane = new OpenFinDesktopDemo(desktopCommandLine, startupUUID);
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
