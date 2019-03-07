package com.openfin.desktop.demo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.json.JSONObject;

import com.openfin.desktop.Ack;
import com.openfin.desktop.AckListener;
import com.openfin.desktop.Application;
import com.openfin.desktop.ApplicationOptions;
import com.openfin.desktop.AsyncCallback;
import com.openfin.desktop.DesktopConnection;
import com.openfin.desktop.DesktopException;
import com.openfin.desktop.DesktopIOException;
import com.openfin.desktop.DesktopStateListener;
import com.openfin.desktop.RuntimeConfiguration;
import com.openfin.desktop.WindowOptions;
import com.openfin.desktop.channel.ChannelClient;
import com.openfin.desktop.win32.ExternalWindowObserver;

public class LayoutServiceDemo implements DesktopStateListener {

	private final static String appUuid = "layoutServiceDemo";

	private DesktopConnection desktopConnection;
	private CountDownLatch latch = new CountDownLatch(1);
	private JFrame mainWindow;
	private JButton btnCreateOpenfinWindow;
	private JButton btnCreateJavaWindow;
	private Application application;
	private ChannelClient channelClient;

	LayoutServiceDemo() {
		try {
			this.createMainWindow();
			this.launchOpenfin();
		}
		catch (DesktopException | DesktopIOException | IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	void createMainWindow() {
		this.mainWindow = new JFrame("Layout Service Demo");
		this.mainWindow.setResizable(false);
		this.mainWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.mainWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				try {
					application.close(true, new AckListener() {

						@Override
						public void onSuccess(Ack ack) {
							if (channelClient != null) {
								desktopConnection.getChannel().disconnect(channelClient, new AckListener() {
									@Override
									public void onSuccess(Ack ack) {
									}

									@Override
									public void onError(Ack ack) {
									}
								});
							}
							mainWindow.dispose();
							try {
								desktopConnection.disconnect();
							}
							catch (DesktopException e) {
								e.printStackTrace();
							}
						}

						@Override
						public void onError(Ack ack) {
						}
					});

				}
				catch (DesktopException de) {
					de.printStackTrace();
				}
			}
		});

		this.btnCreateOpenfinWindow = new JButton("Create Openfin Window");
		this.btnCreateOpenfinWindow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createOpenfinWindow();
			}
		});
		this.btnCreateJavaWindow = new JButton("Create Java Window");
		this.btnCreateJavaWindow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					createJavaWindow(UUID.randomUUID().toString());
				}
				catch (DesktopException e1) {
					e1.printStackTrace();
				}
			}
		});
		this.btnCreateOpenfinWindow.setEnabled(false);
		this.btnCreateJavaWindow.setEnabled(false);
		JPanel contentPnl = new JPanel(new BorderLayout(10, 10));
		contentPnl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
		pnl.add(btnCreateOpenfinWindow);
		pnl.add(btnCreateJavaWindow);

		contentPnl.add(new JLabel("Undock Openfin windows with global hotkey (CTRL+SHIFT+U or CMD+SHIFT+U)"),
				BorderLayout.NORTH);
		contentPnl.add(pnl, BorderLayout.CENTER);

		this.mainWindow.getContentPane().add(contentPnl);

		this.mainWindow.pack();
		this.mainWindow.setLocationRelativeTo(null);
		this.mainWindow.setVisible(true);
	}

	void launchOpenfin() throws DesktopException, DesktopIOException, IOException, InterruptedException {
		RuntimeConfiguration config = new RuntimeConfiguration();
		config.setRuntimeVersion("stable");
		config.setAdditionalRuntimeArguments("--v=1");
		config.addService("layouts", "https://cdn.openfin.co/services/openfin/layouts/app.json");
		this.desktopConnection = new DesktopConnection("LayoutServiceDemo");
		this.desktopConnection.connect(config, this, 60);
		latch.await();
	}

	void createApplication(String name, String uuid, String url, AckListener listener) {
		ApplicationOptions appOpt = new ApplicationOptions(name, uuid, url);
		WindowOptions mainWindowOptions = new WindowOptions();
		mainWindowOptions.setAutoShow(false);
		appOpt.setMainWindowOptions(mainWindowOptions);

		this.application = new Application(appOpt, this.desktopConnection, new AckListener() {
			@Override
			public void onSuccess(Ack ack) {
				application.run(listener);
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						btnCreateOpenfinWindow.setEnabled(true);
						btnCreateJavaWindow.setEnabled(true);
					}
				});
			}

			@Override
			public void onError(Ack ack) {
			}
		});
	}

	void createJavaWindow(String windowName) throws DesktopException {
		final JButton btnUndock = new JButton("undock");

		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.setPreferredSize(new Dimension(640, 480));
		JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
		pnl.add(btnUndock);
		f.getContentPane().add(pnl);
		f.pack();
		f.setLocationRelativeTo(null);
		f.setVisible(true);

		new ExternalWindowObserver(this.desktopConnection.getPort(), appUuid, windowName, f, new AckListener() {
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
							}
						});
			}

			@Override
			public void onError(Ack ack) {
				System.out.println(windowName + ": unable to register external window, " + ack.getReason());
			}
		});
	}

	void createOpenfinWindow() {
		try {
			WindowOptions winOpts = new WindowOptions();
			winOpts.setAutoShow(true);
			winOpts.setDefaultHeight(480);
			winOpts.setDefaultWidth(640);
			winOpts.setName(UUID.randomUUID().toString());
			winOpts.setUrl("https://www.google.com");
			application.createChildWindow(winOpts, new AckListener() {
				@Override
				public void onSuccess(Ack ack) {
				}

				@Override
				public void onError(Ack ack) {
					System.out.println("unable to create openfin window: " + ack.getReason());
				}
			});
		}
		catch (DesktopException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onReady() {
		createApplication(appUuid, appUuid, "about:blank", new AckListener() {
			@Override
			public void onSuccess(Ack ack) {
			}

			@Override
			public void onError(Ack ack) {
				System.out.println("error creating applicaton: " + ack.getReason());
			}

		});
	}

	@Override
	public void onClose(String error) {
		latch.countDown();
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
		new LayoutServiceDemo();
	}
}
