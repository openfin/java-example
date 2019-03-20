package com.openfin.desktop.demo;

/**
 * Example of Java window that can be managed by OpenFin layout service for snap&dock
 */

import com.openfin.desktop.*;
import com.openfin.desktop.Window;
import com.openfin.desktop.channel.ChannelAction;
import com.openfin.desktop.channel.ChannelClient;
import com.openfin.desktop.win32.ExternalWindowObserver;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.json.JSONArray;
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
    private ChannelClient channelClient;
    private String appUuid;

    public LayoutFrame(DesktopConnection desktopConnection, String appUuid, String windowName) throws DesktopException {
        super();
        System.out.println(windowName + " being created ");
        this.appUuid = appUuid;
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
                                LayoutFrame.this.channelClient = client;
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
                            checkTabbing();
                        } else {
                            LayoutFrame.this.btnUndock.setEnabled(false);
//                            setHasFrame(LayoutFrame.this, true);
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

//        this.externalWindowObserver.setUserGesture(false);
    }

    private void checkTabbing() {
        JSONObject payload = new JSONObject();
        payload.put("uuid", appUuid);
        payload.put("name", windowName);
        channelClient.dispatch("GETTABS", payload, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                System.out.printf("channel GETTABS ");
                JSONObject data = (JSONObject) ack.getData();
                Object result = data.get("result");
                if (result != null && result instanceof JSONArray) {
                    JSONArray tabs = (JSONArray) result;
                    LayoutFrame.this.btnUndock.setEnabled(!(tabs != null && tabs.length() > 0));
//                    setHasFrame(LayoutFrame.this, false);
                } else {
                    LayoutFrame.this.btnUndock.setEnabled(true);
//                    setHasFrame(LayoutFrame.this, true);

                }
            }
            @Override
            public void onError(Ack ack) {
                System.out.printf("channel GETTABS error " + ack.getReason());
            }
        });
    }


    private void setHasFrame(JFrame frame, boolean hasFrame) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println(windowName + " hasFrame=" + hasFrame);

                Dimension size = frame.getSize();
                WinDef.HWND hWnd = new WinDef.HWND();
                hWnd.setPointer(Native.getComponentPointer(frame));

                int style = User32.INSTANCE.GetWindowLong(hWnd, User32.GWL_STYLE);

                if (hasFrame) {
                    frame.setResizable(true);
//					style = style & ~User32.WS_CHILD;
                    style = style | User32.WS_CAPTION | User32.WS_BORDER | User32.WS_THICKFRAME;
                } else {
                    frame.setResizable(false);
                    style = style &~ User32.WS_CAPTION &~ User32.WS_BORDER &~ User32.WS_THICKFRAME;
//					style = style | User32.WS_CHILD;
                }
                User32.INSTANCE.SetWindowLong(hWnd, User32.GWL_STYLE, style);
                User32.INSTANCE.RedrawWindow(hWnd, null, null, new WinDef.DWORD((User32.RDW_FRAME | User32.RDW_INVALIDATE)));
                frame.setSize(size.width, size.height + 1);
                frame.invalidate();
                frame.repaint();
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
