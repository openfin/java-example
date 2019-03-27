package com.openfin.desktop.demo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.System;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.openfin.desktop.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class LayoutServiceDemo implements DesktopStateListener {

	private final static String appUuid = "layoutServiceDemo-" + UUID.randomUUID();
	private final static String javaConnectUuid = "layoutServiceDemoJava-" + UUID.randomUUID();

	private DesktopConnection desktopConnection;
	private CountDownLatch latch = new CountDownLatch(1);
	private JFrame mainWindow;
	private JButton btnCreateOpenfinWindow;
	private JButton btnCreateJavaWindow;
	private Application application;

	private JSONArray serviceConfig = new JSONArray();
	private Map<String, LayoutFrame> childFrames = new HashMap();
	private Map<String, FxLayoutFrame> childFxFrames = new HashMap();
	private WindowAdapter childFrameCleanListener;
	private JButton btnCreateFramelessJavaWindow;
	private JButton btnCreateJavaFxWindow;

	LayoutServiceDemo() {
		try {
			this.createMainWindow();
			this.launchOpenfin();

			this.childFrameCleanListener = new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					super.windowClosed(e);
					LayoutFrame frame = (LayoutFrame) e.getWindow();
					childFrames.remove(frame.getWindowName());
				}
			};
		}
		catch (Exception e) {
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
					childFrames.values().forEach(frame -> {
						frame.cleanup();
					});
					childFxFrames.values().forEach(frame -> {
						frame.cleanup();
					});
					application.close();
					Thread.sleep(1000);
					OpenFinRuntime runtime = new OpenFinRuntime(desktopConnection);
					runtime.exit();
					Thread.sleep(1000);
		            java.lang.System.exit(0);
				}
				catch (Exception de) {
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
					createJavaWindow();
				}
				catch (DesktopException e1) {
					e1.printStackTrace();
				}
			}
		});
		this.btnCreateFramelessJavaWindow = new JButton("Create Frameless Java Window");
		this.btnCreateFramelessJavaWindow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					createFramelessJavaWindow();
				}
				catch (DesktopException e1) {
					e1.printStackTrace();
				}
			}
		});
		this.btnCreateJavaFxWindow = new JButton("Create JavaFX Window");
		this.btnCreateJavaFxWindow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createJavaFxWindow();
			}
		});
		
		this.btnCreateOpenfinWindow.setEnabled(false);
		this.btnCreateJavaWindow.setEnabled(false);
		this.btnCreateFramelessJavaWindow.setEnabled(false);
		this.btnCreateJavaFxWindow.setEnabled(false);
		JPanel contentPnl = new JPanel(new BorderLayout(10, 10));
		contentPnl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
		pnl.add(btnCreateOpenfinWindow);
		pnl.add(btnCreateJavaWindow);
		if (System.getProperty("com.openfin.demo.layout.frameless") != null) {
			// This example is experimental and not available yet
			pnl.add(btnCreateFramelessJavaWindow);
		}
		pnl.add(btnCreateJavaFxWindow);

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
		String rvm = System.getProperty("com.openfin.demo.layout.rvm");
		if (rvm != null) {
			config.setLaunchRVMPath(rvm);
		}
		config.setRuntimeVersion("9.61.38.40");
		config.setAdditionalRuntimeArguments("--v=1 --remote-debugging-port=9090 ");
		serviceConfig = new JSONArray();
		JSONObject layout = new JSONObject();
		layout.put("name", "layouts");
		JSONObject scfg = new JSONObject();
		JSONObject sfeatures = new JSONObject();
		sfeatures.put("dock", true);
		sfeatures.put("tab", true);
		scfg.put("features", sfeatures);
		layout.put("config", scfg);
		serviceConfig.put(0, layout);
		config.addConfigurationItem("services", serviceConfig);

		JSONObject startupApp = new JSONObject();
		startupApp.put("uuid", appUuid);
		startupApp.put("name", appUuid);
		startupApp.put("url", "about:blank");
		startupApp.put("autoShow", false);
		config.setStartupApp(startupApp);

		this.desktopConnection = new DesktopConnection(javaConnectUuid);
		this.desktopConnection.connect(config, this, 60);
		latch.await();
	}

	void createApplication(String name, String uuid, String url, AckListener listener) {
		ApplicationOptions appOpt = new ApplicationOptions(name, uuid, url);
		WindowOptions mainWindowOptions = new WindowOptions();
		mainWindowOptions.setAutoShow(false);
		appOpt.setMainWindowOptions(mainWindowOptions);
		appOpt.put("services", serviceConfig);

		this.application = new Application(appOpt, this.desktopConnection, new AckListener() {
			@Override
			public void onSuccess(Ack ack) {
				application.run(listener);
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						btnCreateOpenfinWindow.setEnabled(true);
						btnCreateJavaWindow.setEnabled(true);
						btnCreateFramelessJavaWindow.setEnabled(true);
						btnCreateJavaFxWindow.setEnabled(true);
					}
				});
			}

			@Override
			public void onError(Ack ack) {
			}
		});
	}

	void createJavaWindow() throws DesktopException {
		String windowName = "Java-" + UUID.randomUUID().toString();
		LayoutFrame frame = new LayoutFrame(this.desktopConnection, appUuid, windowName);
		this.childFrames.put(windowName, frame);
		frame.addWindowListener(this.childFrameCleanListener);
	}
	
	void createFramelessJavaWindow() throws DesktopException {
		String windowName = "Java-" + UUID.randomUUID().toString();
		LayoutFrame frame = new LayoutFrame(this.desktopConnection, appUuid, windowName, true);
		this.childFrames.put(windowName, frame);
		frame.addWindowListener(this.childFrameCleanListener);
	}

	void createJavaFxWindow() {
		String windowName = "JavaFX-" + UUID.randomUUID().toString();
		FxLayoutFrame frame = new FxLayoutFrame(this.desktopConnection, appUuid, windowName);
		this.childFxFrames.put(windowName, frame);
	}

	void createOpenfinWindow() {
		try {
			WindowOptions winOpts = new WindowOptions();
			winOpts.setAutoShow(true);
			winOpts.setDefaultHeight(480);
			winOpts.setDefaultWidth(640);
            winOpts.setResizable(true);
            winOpts.setFrame(true);
			winOpts.setName(UUID.randomUUID().toString());
			String url = java.lang.System.getProperty("com.openfin.demo.layout.url");
			winOpts.setUrl(url == null ? "https://openfin.co" : url);
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
		this.application = Application.wrap(appUuid, this.desktopConnection);
		btnCreateOpenfinWindow.setEnabled(true);
		btnCreateJavaWindow.setEnabled(true);
		btnCreateFramelessJavaWindow.setEnabled(true);
		btnCreateJavaFxWindow.setEnabled(true);
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
