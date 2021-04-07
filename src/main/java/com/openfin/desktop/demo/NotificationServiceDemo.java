package com.openfin.desktop.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonValue;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.openfin.desktop.FinLauncher;
import com.openfin.desktop.FinRuntime;
import com.openfin.desktop.FinRuntimeConnectionListener;
import com.openfin.desktop.bean.RuntimeConfig;
import com.openfin.desktop.notifications.ButtonOptions;
import com.openfin.desktop.notifications.NotificationActionEvent;
import com.openfin.desktop.notifications.NotificationEvent;
import com.openfin.desktop.notifications.NotificationIndicator;
import com.openfin.desktop.notifications.NotificationOptions;
import com.openfin.desktop.notifications.FinNotifications;

public class NotificationServiceDemo {

	private JFrame demoWindow;
	private JPanel glassPane;
	private FinRuntime openFinRuntime;
	private FinNotifications notifications;

	public NotificationServiceDemo() {
		this.demoWindow = new JFrame("OpenFin Notification Service Demo");
		this.demoWindow.setContentPane(this.createContentPanel());
		this.demoWindow.setGlassPane(this.createGlassPane());
		this.demoWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.demoWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (openFinRuntime != null) {
					openFinRuntime.disconnect();
				}
				else {
					System.exit(0);
				}
			}
		});

		this.demoWindow.pack();
		this.demoWindow.setLocationRelativeTo(null);
		this.demoWindow.setVisible(true);
		this.glassPane.setVisible(true);
		this.initOpenFin();
	}

	private void initOpenFin() {
		RuntimeConfig config = new RuntimeConfig();
		config.getRuntime().setVersion("stable");
		FinLauncher.newLauncherBuilder().connectionUuid("OpenFin Notification Service Demo")
				.connectionListener(new FinRuntimeConnectionListener() {
					@Override
					public void onOpen(FinRuntime runtime) {
						openFinRuntime = runtime;
						notifications = new FinNotifications(runtime);
						notifications.addEventListener(NotificationEvent.TYPE_ACTION, ne -> {
							NotificationActionEvent actionEvent = (NotificationActionEvent) ne;
							JsonValue actionResult = actionEvent.getResult();
							System.out.println("actionResult: notificationId: " + actionEvent.getNotification().getId()
									+ ", user clicked on btn: " + actionResult.asJsonObject().getString("btn"));
						});

						notifications.getProviderStatus().thenAccept(status -> {
							// it comes to here only when the notification service is ready.
							SwingUtilities.invokeLater(() -> {
								glassPane.setVisible(false);
							});
						});
					}

					@Override
					public void onClose(String reason) {
						System.exit(0);
					}
				}).runtimeConfig(config).build().launch();
	}

	private JPanel createGlassPane() {
		this.glassPane = new JPanel(new BorderLayout());
		JLabel l = new JLabel("Loading, please wait......");
		l.setHorizontalAlignment(JLabel.CENTER);
		this.glassPane.add(l, BorderLayout.CENTER);
		this.glassPane.setBackground(Color.LIGHT_GRAY);
		return this.glassPane;
	}

	private JPanel createToggleNotificationCenterPanel() {
		JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
		pnl.setBorder(BorderFactory.createTitledBorder("Notification Center"));
		JButton btnToggleNotificationCenter = new JButton("Toggle Notification Center");
		btnToggleNotificationCenter.addActionListener(e -> {
			this.notifications.toggleNotificationCenter();
		});
		JButton btnClearAll = new JButton("Clear All");
		btnClearAll.addActionListener(e -> {
			this.notifications.clearAll();
		});
		pnl.add(btnToggleNotificationCenter);
		pnl.add(btnClearAll);
		return pnl;
	}

	private JPanel createCreateNotificationPanel() {
		JPanel pnl = new JPanel(new BorderLayout());
		pnl.setBorder(BorderFactory.createTitledBorder("Notification"));

		JTextField tfTitle = new JTextField("Title");
		JTextField tfBody = new JTextField("Body");
		JTextField tfCategory = new JTextField("Category");
		JTextField tfIcon = new JTextField("https://openfin.co/favicon-32x32.png");
		JTextField tfIndicatorText = new JTextField("");
		JTextField tfExpiresInSecs = new JTextField("10");

		JComboBox<String> cbSticky = new JComboBox<>();
		cbSticky.addItem(NotificationOptions.STICKY_STICKY);
		cbSticky.addItem(NotificationOptions.STICKY_TRANSIENT);

		JComboBox<String> cbIndicator = new JComboBox<>();
		cbIndicator.addItem(NotificationIndicator.TYPE_FAILURE);
		cbIndicator.addItem(NotificationIndicator.TYPE_WARNING);
		cbIndicator.addItem(NotificationIndicator.TYPE_SUCCESS);

		JPanel pnlCenter = new JPanel(new GridBagLayout());
		GridBagConstraints gbConst = new GridBagConstraints();
		gbConst.gridx = 0;
		gbConst.gridy = 0;
		gbConst.weightx = 0;
		gbConst.insets = new Insets(5, 5, 5, 5);
		gbConst.anchor = GridBagConstraints.EAST;
		pnlCenter.add(new JLabel("Title"), gbConst);
		gbConst.gridy++;
		pnlCenter.add(new JLabel("Body"), gbConst);
		gbConst.gridy++;
		pnlCenter.add(new JLabel("Category"), gbConst);
		gbConst.gridy++;
		pnlCenter.add(new JLabel("Icon"), gbConst);
		gbConst.gridy++;
		pnlCenter.add(new JLabel("Sticky"), gbConst);
		gbConst.gridy++;
		pnlCenter.add(new JLabel("Indicator"), gbConst);
		gbConst.gridy++;
		pnlCenter.add(new JLabel("Indicator Text"), gbConst);
		gbConst.gridy++;
		pnlCenter.add(new JLabel("Expires (in seconds)"), gbConst);
		gbConst.gridx = 1;
		gbConst.gridy = 0;
		gbConst.weightx = 0.5;
		gbConst.insets = new Insets(5, 0, 5, 5);
		gbConst.fill = GridBagConstraints.BOTH;
		pnlCenter.add(tfTitle, gbConst);
		gbConst.gridy++;
		pnlCenter.add(tfBody, gbConst);
		gbConst.gridy++;
		pnlCenter.add(tfCategory, gbConst);
		gbConst.gridy++;
		pnlCenter.add(tfIcon, gbConst);
		gbConst.gridy++;
		pnlCenter.add(cbSticky, gbConst);
		gbConst.gridy++;
		pnlCenter.add(cbIndicator, gbConst);
		gbConst.gridy++;
		pnlCenter.add(tfIndicatorText, gbConst);
		gbConst.gridy++;
		pnlCenter.add(tfExpiresInSecs, gbConst);
		gbConst.weighty = 0.5;
		gbConst.gridy++;
		pnlCenter.add(new JLabel(), gbConst);

		JButton btnCreate = new JButton("Create Notification");
		btnCreate.addActionListener(e -> {
			NotificationOptions opt = new NotificationOptions(tfTitle.getText(), tfBody.getText(),
					tfCategory.getText());
			String icon = tfIcon.getText().trim();
			if (!icon.isEmpty()) {
				opt.setIcon(icon);
			}
			opt.setSticky((String) cbSticky.getSelectedItem());
			NotificationIndicator indicatorOpts = new NotificationIndicator((String) cbIndicator.getSelectedItem());
			String indicatorText = tfIndicatorText.getText().trim();
			if (!indicatorText.isEmpty()) {
				indicatorOpts.setText(indicatorText);
			}
			opt.setIndicator(indicatorOpts);

			String expires = tfExpiresInSecs.getText().trim();
			if (!expires.isEmpty()) {
				opt.setExpires(new Date(System.currentTimeMillis() + (1000 * (Integer.parseInt(expires)))));
			}

			ButtonOptions bo1 = new ButtonOptions("Button 1");
			bo1.setOnClick(Json.createObjectBuilder().add("btn", "btn1").build());
			ButtonOptions bo2 = new ButtonOptions("Button 2");
			bo2.setOnClick(Json.createObjectBuilder().add("btn", "btn2").build());
			bo2.setCta(true);
			opt.setButtons(bo1, bo2);

			this.notifications.create(opt);
		});
		JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		pnlBottom.add(btnCreate);
		pnl.add(pnlCenter, BorderLayout.CENTER);
		pnl.add(pnlBottom, BorderLayout.SOUTH);
		return pnl;
	}

	private JPanel createContentPanel() {
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p.setPreferredSize(new Dimension(550, 400));
		p.add(this.createToggleNotificationCenterPanel(), BorderLayout.NORTH);
		p.add(this.createCreateNotificationPanel(), BorderLayout.CENTER);
		return p;
	}

	public static void main(String[] args) {
		new NotificationServiceDemo();
	}

}