package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import com.openfin.desktop.fdc3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.System;
import java.util.UUID;

public class FDC3Example implements DesktopStateListener {

    private final static String javaConnectUuid = "JavaFDC3Demo";

    private DesktopConnection desktopConnection;
    private JFrame mainWindow;
    private JButton btnLaunchRed;
    private JButton btnTickerToRed;
    private JButton btnFindIntent;
    private JButton btnFindContextIntent;
    private JButton btnBroadcast;
    private JButton btnIntentListener;

    private FDC3Client fdc3Client;

    private JSONArray serviceConfig = new JSONArray();

    FDC3Example() {
        try {
            this.createMainWindow();
            this.launchOpenfin();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    void createMainWindow() {
        this.mainWindow = new JFrame("FDC3 Demo");
        this.mainWindow.setResizable(false);
        this.mainWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.mainWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                try {
//                    application.close();
                    Thread.sleep(1000);
                    java.lang.System.exit(0);
                }
                catch (Exception de) {
                    de.printStackTrace();
                }
            }
        });

        JPanel contentPnl = new JPanel(new BorderLayout(10, 10));
        contentPnl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));

        this.btnLaunchRed = new JButton("Start Charts: Red");
        this.btnLaunchRed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startReds();
            }
        });
        this.btnLaunchRed.setEnabled(false);
        pnl.add(btnLaunchRed);

        this.btnTickerToRed = new JButton("Send ticker to Red");
        this.btnTickerToRed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendTickerToReds();
            }
        });
        this.btnTickerToRed.setEnabled(false);
        pnl.add(btnTickerToRed);

        this.btnFindIntent = new JButton("Find intent of ViewChart");
        this.btnFindIntent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findIntent();
            }
        });
        this.btnFindIntent.setEnabled(false);
        pnl.add(btnFindIntent);

        this.btnFindContextIntent = new JButton("Find intent of fdc3.instrument context");
        this.btnFindContextIntent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findContextIntent();
            }
        });
        this.btnFindContextIntent.setEnabled(false);
        pnl.add(btnFindContextIntent);

        this.btnBroadcast = new JButton("Broadcast to default channel");
        this.btnBroadcast.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                broadcast();
            }
        });
        this.btnBroadcast.setEnabled(false);
        pnl.add(btnBroadcast);

        this.btnIntentListener = new JButton("Register an intent listener");
        this.btnIntentListener.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addIntentListener();
            }
        });
        this.btnIntentListener.setEnabled(false);
        pnl.add(btnIntentListener);

        contentPnl.add(pnl, BorderLayout.CENTER);

        this.mainWindow.getContentPane().add(contentPnl);
        this.mainWindow.setLocationRelativeTo(null);
        this.mainWindow.setSize(400, 400);
        this.mainWindow.setVisible(true);
    }

    void launchOpenfin() throws DesktopException, DesktopIOException, IOException, InterruptedException {
        RuntimeConfiguration config = new RuntimeConfiguration();
//        config.setRuntimeVersion("stable");
        config.setRuntimeVersion("10.66.41.18");
        config.setAdditionalRuntimeArguments("--v=1 ");
//        serviceConfig = new JSONArray();
//        JSONObject layout = new JSONObject();
//        layout.put("name", "fdc3");
//        serviceConfig.put(0, layout);
//        config.addConfigurationItem("services", serviceConfig);

//        JSONObject startupApp = new JSONObject();
//        startupApp.put("uuid", appUuid);
//        startupApp.put("name", appUuid);
//        startupApp.put("url", "about:blank");
//        startupApp.put("autoShow", false);
//        config.setStartupApp(startupApp);

        this.desktopConnection = new DesktopConnection(javaConnectUuid);
        this.desktopConnection.connect(config, this, 60);
    }

    @Override
    public void onReady() {
        this.fdc3Client = new FDC3Client(this.desktopConnection);
        this.fdc3Client.connect("JavaFDC3Demo", new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                btnLaunchRed.setEnabled(true);
                btnTickerToRed.setEnabled(true);
                btnFindIntent.setEnabled(true);
                btnFindContextIntent.setEnabled(true);
                btnBroadcast.setEnabled(true);
                btnIntentListener.setEnabled(true);
                addContextListener();
                System.out.println(String.format("Connected to FDC3 service"));
            }
            @Override
            public void onError(Ack ack) {
                System.out.println(String.format("Failed t0 Connect to FDC3 service"));
            }
        });
    }

    private void startReds() {
        fdc3Client.open("charts-red", null, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                System.out.printf(String.format("FDC3 started %b", ack.isSuccessful()));
            }
            @Override
            public void onError(Ack ack) {
                System.out.printf(String.format("FDC3 started failed %s", ack.getReason()));
            }
        });
    }

    private void sendTickerToReds() {
        Context context = new Context("fdc3.instrument", "IBM");
        JSONObject id = new JSONObject();
        id.put("ticker", "ibm");
        context.setId(id);
        fdc3Client.raiseIntent("fdc3.ViewChart", context, "charts-red", new AsyncCallback<IntentResolution>() {
            @Override
            public void onSuccess(IntentResolution result) {
            }
        });
    }

    private void findIntent() {
        fdc3Client.findIntent("fdc3.ViewChart", null, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    System.out.printf(String.format("Intent found: %s", ack.getJsonObject().getJSONObject("data").getJSONObject("result")));
                }
            }
            @Override
            public void onError(Ack ack) {
            }
        });
    }

    private void findContextIntent() {
        Context context = new Context("fdc3.instrument", "IBM");

        fdc3Client.findIntentsByContext(context, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                System.out.printf(String.format("Intent found: %s", ack.getJsonObject().getJSONObject("data").getJSONArray("result")));
            }
            @Override
            public void onError(Ack ack) {
            }
        });
    }

    private void broadcast() {
        Context context = new Context("fdc3.security", "IBM");
        JSONObject id = new JSONObject();
        id.put("ticker", "ibm");
        context.setId(id);

        fdc3Client.broadcast(context, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
//                System.out.printf(String.format("Intent found: %s", ack.getJsonObject().getJSONObject("data").getJSONArray("result")));
            }
            @Override
            public void onError(Ack ack) {
            }
        });
    }

    private void addIntentListener() {
        fdc3Client.addIntentListener("fdc3.ViewChart", new IntentListener() {
            @Override
            public JSONObject onIntent(Context context) {
                System.out.println(String.format("Received Intent: %s", context.toString()));
                context.put("comment",  "Java rules");
                return context;
            }
        },null);
    }

    private void addContextListener() {
        fdc3Client.addContextListener(new ContextListener() {
            @Override
            public void onContext(Context result) {
                System.out.println(String.format("Received context: %s", result.toString()));
            }
        });
    }

    @Override
    public void onClose(String error) {

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

    public static void main(String[] args) {
        new FDC3Example();
    }

}
