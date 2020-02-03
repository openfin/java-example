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
import com.sun.jna.platform.win32.WinDef;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.System;

public class LayoutFrame extends JFrame {
	private static String LayoutServiceChannelName = "of-layouts-service-v1";
	private ExternalWindowObserver externalWindowObserver;
	private JLabel labelName;
	private JButton btnUndock;
	private String windowName;
	private ChannelClient channelClient;
	private String appUuid;
	private boolean frameless;

	public LayoutFrame(DesktopConnection desktopConnection, String appUuid, String windowName) throws DesktopException {
		this(desktopConnection, appUuid, windowName, false);
	}
	
	public LayoutFrame(DesktopConnection desktopConnection, String appUuid, String windowName, boolean frameless) throws DesktopException {
		super();
		this.setTitle(windowName);
		System.out.println(windowName + " being created ");
		this.appUuid = appUuid;
		this.windowName = windowName;
		this.frameless = frameless;
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setPreferredSize(new Dimension(640, 480));
		JPanel pnl = new JPanel();
		pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
		this.labelName = new JLabel(windowName);
		pnl.add(labelName);
		this.btnUndock = new JButton("undock");
		this.btnUndock.setEnabled(false);
		pnl.add(btnUndock);
		this.getContentPane().add(pnl);

		if (frameless) {
			this.setUndecorated(true);
			JPanel titleBar = new JPanel(new BorderLayout());
			titleBar.setBackground(Color.DARK_GRAY);
			MouseAdapter myListener = new MouseAdapter() {
				int pressedAtX, pressedAtY;
				@Override
				public void mousePressed(MouseEvent e) {
					pressedAtX = e.getX();
					pressedAtY = e.getY();
					System.out.println("mouse pressed at x=" + pressedAtX + ", y=" + pressedAtY);
					LayoutFrame.this.externalWindowObserver.enterSizeMove();
				}
				@Override
				public void mouseDragged(MouseEvent e) {
					int distanceX = e.getX() - pressedAtX;
					int distanceY = e.getY() - pressedAtY;
					System.out.println("dragged x=" + distanceX + ", y=" + distanceY);
					Point frameLocation = LayoutFrame.this.getLocation();
					Dimension dimension = LayoutFrame.this.getSize();
					WindowBounds bounds = new WindowBounds(frameLocation.x + distanceX, frameLocation.y + distanceY,
							dimension.width, dimension.height);
					Point point = new Point(e.getX(), e.getY());
					if (!LayoutFrame.this.externalWindowObserver.onMoving(bounds, point)) {
						LayoutFrame.this.setLocation(frameLocation.x + distanceX, frameLocation.y + distanceY);
					}
				}
				@Override
				public void mouseReleased(MouseEvent e) {
					LayoutFrame.this.externalWindowObserver.exitSizeMove();
				}
			};
			titleBar.addMouseListener(myListener);
			titleBar.addMouseMotionListener(myListener);
			
			JButton btnClose = new JButton("X");
			btnClose.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					LayoutFrame.this.dispose();
				}});
			titleBar.add(btnClose, BorderLayout.EAST);
			
			this.getContentPane().add(titleBar, BorderLayout.NORTH);
			
		}
		
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);

		this.externalWindowObserver = new ExternalWindowObserver(desktopConnection.getPort(), appUuid, windowName, this,
				new AckListener() {
					@Override
					public void onSuccess(Ack ack) {
						ExternalWindowObserver observer = (ExternalWindowObserver) ack.getSource();
						observer.getDesktopConnection().getChannel(LayoutServiceChannelName).connect(
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
											public JSONObject invoke(String action, JSONObject payload, JSONObject senderIdentity) {
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
//		this.externalWindowObserver.setUserGesture(!this.frameless);
		try {
//			if (this.frameless) {
//				WindowOptions options = new WindowOptions();
//				options.setFrame(false);
//				this.externalWindowObserver.setWindowOptions(options);
//			}
			this.externalWindowObserver.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

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
							setHasFrame(LayoutFrame.this, true);
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
				}
				catch (Exception e1) {
					e1.printStackTrace();
				}
			}

			public void windowClosed(WindowEvent e) {
				super.windowClosed(e);
				System.out.println(windowName + " closed ");
			}
		});
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
					boolean enabled = !(tabs != null && tabs.length() > 0);
					LayoutFrame.this.btnUndock.setEnabled(enabled);
					setHasFrame(LayoutFrame.this, false);
				} else {
					LayoutFrame.this.btnUndock.setEnabled(true);
					setHasFrame(LayoutFrame.this, true);
				}
			}

			@Override
			public void onError(Ack ack) {
				System.out.printf("channel GETTABS error " + ack.getReason());
			}
		});
	}

	private void setHasFrame(JFrame frame, boolean hasFrame) {
		if (!this.frameless) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					System.out.println(windowName + " hasFrame=" + hasFrame);
					WinDef.HWND hWnd = new WinDef.HWND();
					hWnd.setPointer(Native.getComponentPointer(frame));
					LayoutFrame.this.externalWindowObserver.setHasFrame(hWnd, hasFrame);
					frame.setResizable(hasFrame);
					frame.invalidate();
					frame.validate();
					frame.repaint();
					SwingUtilities.updateComponentTreeUI(frame);
				}
			});
		}
	}

	public String getWindowName() {
		return windowName;
	}

	public void cleanup() {
		try {
			System.out.println(windowName + " cleaning up ");
			this.externalWindowObserver.dispose();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		final JFrame frame = new JFrame();
		frame.setPreferredSize(new Dimension(640, 480));
		JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));

		frame.setUndecorated(true);
		JPanel titleBar = new JPanel(new BorderLayout());
		titleBar.setBackground(Color.DARK_GRAY);
		MouseAdapter myListener = new MouseAdapter() {
			int pressedAtX, pressedAtY;
			@Override
			public void mousePressed(MouseEvent e) {
				pressedAtX = e.getX();
				pressedAtY = e.getY();
				System.out.println("mouse pressed at x=" + pressedAtX + ", y=" + pressedAtY);
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				int distanceX = e.getX() - pressedAtX;
				int distanceY = e.getY() - pressedAtY;
				System.out.println("dragged x=" + distanceX + ", y=" + distanceY);
				Point frameLocation = frame.getLocation();
				frame.setLocation(frameLocation.x + distanceX, frameLocation.y + distanceY);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				pressedAtX = e.getX();
				pressedAtY = e.getY();
				System.out.println("mouse released at x=" + pressedAtX + ", y=" + pressedAtY);
			}
		};
		titleBar.addMouseListener(myListener);
		titleBar.addMouseMotionListener(myListener);

		JButton btnClose = new JButton("X");
		btnClose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.dispose();
			}});
		titleBar.add(btnClose, BorderLayout.EAST);
		frame.getContentPane().add(titleBar, BorderLayout.NORTH);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

}
