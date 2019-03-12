package com.openfin.desktop.demo;

/**
 * Example of Java window that can be managed by OpenFin layout service for snap&dock
 */

import com.openfin.desktop.*;
import com.openfin.desktop.Window;
import com.openfin.desktop.channel.ChannelAction;
import com.openfin.desktop.channel.ChannelClient;
import com.openfin.desktop.win32.ExternalWindowObserver;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.System;
import java.util.Iterator;

public class LayoutFrame extends JFrame {
    private ExternalWindowObserver externalWindowObserver;
    private JButton btnUndock;
    private String windowName;

    public LayoutFrame(DesktopConnection desktopConnection, String appUuid, String windowName) throws DesktopException {
        super();
        System.out.println(windowName + " being created ");
        this.windowName = windowName;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setPreferredSize(new Dimension(640, 480));
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
        this.btnUndock = new JButton("undock");
        this.btnUndock.setEnabled(false);
        pnl.add(btnUndock);
        this.getContentPane().add(pnl);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);

        this.externalWindowObserver = new ExternalWindowObserver(desktopConnection.getPort(), appUuid, windowName,
                        this, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                ExternalWindowObserver observer = (ExternalWindowObserver) ack.getSource();
                observer.getDesktopConnection().getChannel().connect("of-layouts-service-v1",
                        new AsyncCallback<ChannelClient>() {
                            @Override
                            public void onSuccess(ChannelClient client) {
                                btnUndock.addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        JSONObject payload = new JSONObject();
                                        payload.put("uuid", appUuid);
                                        payload.put("name", windowName);
                                        client.dispatch("UNDOCK-WINDOW", payload, null);
                                    }
                                });

                                client.register("event", new ChannelAction() {
                                    @Override
                                    public JSONObject invoke(String action, JSONObject payload) {
                                        System.out.printf("channel event " + action);
                                        return null;
                                    }
                                });
                            }
                        });
            }
            @Override
            public void onError(Ack ack) {
                System.out.println(windowName + ": unable to register external window, " + ack.getReason());
            }
        });

        Window w = Window.wrap(appUuid, windowName, desktopConnection);
        w.addEventListener("group-changed", new EventListener() {
            @Override
            public void eventReceived(com.openfin.desktop.ActionEvent actionEvent) {
                JSONObject eventObj = actionEvent.getEventObject();
                w.getGroup(new AsyncCallback<java.util.List<Window>>() {
                    @Override
                    public void onSuccess(java.util.List<Window> result) {
                        if (result.size() > 0) {
                            boolean tabbed = false;
                            for (Iterator<Window> iter = result.iterator(); iter.hasNext();) {
                                Window w = iter.next();
                                if ("layouts-service".equals(w.getUuid())) {
                                    tabbed = true;
                                    break;
                                }
                            }
                            LayoutFrame.this.btnUndock.setEnabled(!tabbed);
                        } else {
                            LayoutFrame.this.btnUndock.setEnabled(false);
                        }
                    }
                }, null);
            }
        }, null);

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    LayoutFrame.this.cleanup();
                    LayoutFrame.this.dispose();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                System.out.println(windowName + " closed ");
            }
        });
    }

    public String getWindowName() {
        return windowName;
    }

    public void cleanup() {
        try {
            System.out.println(windowName + " cleaning up ");
            this.externalWindowObserver.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
