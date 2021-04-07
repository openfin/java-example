package com.openfin.desktop.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.openfin.desktop.FinEventListener;
import com.openfin.desktop.FinLauncher;
import com.openfin.desktop.FinLayoutObject;
import com.openfin.desktop.FinPlatform;
import com.openfin.desktop.FinPlatformObject;
import com.openfin.desktop.FinRuntime;
import com.openfin.desktop.FinRuntimeConnectionListener;
import com.openfin.desktop.FinViewObject;
import com.openfin.desktop.FinWindowObject;
import com.openfin.desktop.FinLayoutObject.PresetLayout;
import com.openfin.desktop.bean.ApplySnapshotOptions;
import com.openfin.desktop.bean.DefaultWindowOptions;
import com.openfin.desktop.bean.FinBeanUtils;
import com.openfin.desktop.bean.Identity;
import com.openfin.desktop.bean.LayoutConfig;
import com.openfin.desktop.bean.LayoutItem;
import com.openfin.desktop.bean.PlatformOptions;
import com.openfin.desktop.bean.RuntimeConfig;
import com.openfin.desktop.bean.Snapshot;
import com.openfin.desktop.bean.ViewOptions;
import com.openfin.desktop.bean.WindowOptions;

public class PlatformApiDemo {

	private JFrame frame;
	private JPanel glassPane;
	private DefaultTreeModel platformTreeModel;
	private DefaultMutableTreeNode rootNode;
	private JTree runtimeTree;
	private boolean windowClosing;
	private FinRuntime fin;

	PlatformApiDemo() {
		this.createGui();
		this.launchOpenFin();
	}

	JPanel createGlassPane() {
		JPanel p = new JPanel(new BorderLayout());
		p.add(new JLabel("Loading, please wait......", JLabel.CENTER), BorderLayout.CENTER);
		return p;
	}

	JPanel createRuntimeTreePanel() {
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createTitledBorder("Running Platforms"));
		this.rootNode = new DefaultMutableTreeNode("OpenFin Runtime");
		this.platformTreeModel = new DefaultTreeModel(this.rootNode);
		this.runtimeTree = new JTree(this.platformTreeModel);
		this.runtimeTree.setShowsRootHandles(true);
		JLabel renderer = new JLabel();
		renderer.setOpaque(true);
		this.runtimeTree.setCellRenderer((t, value, selected, expanded, leaf, row, hasFocus) -> {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			Object nodeValue = node.getUserObject();
			if (nodeValue instanceof FinPlatformObject) {
				FinPlatformObject platform = (FinPlatformObject) nodeValue;
				renderer.setText(platform.getIdentity().getUuid());
			}
			else if (nodeValue instanceof FinWindowObject) {
				FinWindowObject window = (FinWindowObject) nodeValue;
				renderer.setText(window.getIdentity().getName());
			}
			else if (nodeValue instanceof FinViewObject) {
				FinViewObject view = (FinViewObject) nodeValue;
				renderer.setText(view.getIdentity().getName());
			}
			else {
				renderer.setText(value.toString());
			}
			renderer.setBackground(selected ? Color.LIGHT_GRAY : Color.WHITE);
			return renderer;
		});
		p.add(new JScrollPane(this.runtimeTree), BorderLayout.CENTER);
		return p;
	}

	@SuppressWarnings("unchecked")
	<T> T getSelectedNode(Class<T> clazz) {
		T n = null;
		TreePath selectionPath = this.runtimeTree.getSelectionPath();
		if (selectionPath != null) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
			if (clazz.isInstance(node.getUserObject())) {
				n = (T) node.getUserObject();
			}
		}
		return n;
	}

	JPanel createPlatformStartFromManifestPanel() {
		JTextField tfUrl = new JTextField("https://openfin.github.io/platform-api-project-seed/public.json");
		JButton btnStart = new JButton("Start");
		ActionListener al = ae -> {
			this.platformStartFromManifest(tfUrl.getText());
		};
		tfUrl.addActionListener(al);
		btnStart.addActionListener(al);
		JPanel p = new JPanel(new BorderLayout(5, 5));
		p.setBorder(BorderFactory.createTitledBorder("startFromManifest"));
		p.add(new JLabel("Manifest URL"), BorderLayout.WEST);
		p.add(tfUrl, BorderLayout.CENTER);
		p.add(btnStart, BorderLayout.EAST);
		return p;
	}

	JPanel createPlatformStartPanel() {
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints gbConst = new GridBagConstraints();
		gbConst.insets = new Insets(5, 5, 5, 5);
		gbConst.fill = GridBagConstraints.BOTH;
		gbConst.gridx = 0;
		gbConst.gridy = 0;
		p.setBorder(BorderFactory.createTitledBorder("start"));
		p.add(new JLabel("UUID"), gbConst);
		gbConst.gridy++;
		p.add(new JLabel("Default Window Width"), gbConst);
		gbConst.gridy++;
		p.add(new JLabel("Default Window Height"), gbConst);
		gbConst.gridy++;
		p.add(new JLabel("Default Window Centered"), gbConst);
		gbConst.gridy++;
		p.add(new JLabel("Default View URL"), gbConst);
		gbConst.gridy++;
		p.add(new JLabel("Create View"), gbConst);
		gbConst.weightx = 1;
		gbConst.gridwidth = 2;
		gbConst.gridx = 1;
		gbConst.gridy = 0;
		JTextField tfUuid = new JTextField(UUID.randomUUID().toString());
		p.add(tfUuid, gbConst);
		gbConst.gridy++;
		JTextField tfWinWidth = new JTextField("");
		p.add(tfWinWidth, gbConst);
		gbConst.gridy++;
		JTextField tfWinHeight = new JTextField("");
		p.add(tfWinHeight, gbConst);
		gbConst.gridy++;
		JCheckBox cbWinCenter = new JCheckBox("", false);
		p.add(cbWinCenter, gbConst);
		gbConst.gridy++;
		JTextField tfUrl = new JTextField("");
		p.add(tfUrl, gbConst);
		gbConst.gridy++;
		JCheckBox cbCreateView = new JCheckBox("", false);
		p.add(cbCreateView, gbConst);
		gbConst.gridwidth = 1;
		gbConst.gridy++;
		p.add(new JLabel(""), gbConst); // filler
		gbConst.gridx = 2;
		gbConst.weightx = 0;
		JButton btnStart = new JButton("Start");
		btnStart.addActionListener(e -> {
			platformStart(tfUuid.getText(), tfWinWidth.getText(), tfWinHeight.getText(), cbWinCenter.isSelected(),
					tfUrl.getText(), cbCreateView.isSelected());
		});
		p.add(btnStart, gbConst);
		return p;
	}

	JPanel createPlatformPanel() {
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.add(createPlatformStartFromManifestPanel());
		p.add(createPlatformStartPanel());
		p.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, Short.MAX_VALUE),
				new Dimension(0, Short.MAX_VALUE)));
		return p;
	}

	JPanel createWindowPanel() {
		JPanel pnl = new JPanel(new BorderLayout());
		JPanel pnlWinOpts = new JPanel(new GridBagLayout());
		pnlWinOpts.setBorder(BorderFactory.createTitledBorder("Window Options"));
		GridBagConstraints gbConst = new GridBagConstraints();
		gbConst.insets = new Insets(5, 5, 5, 5);
		gbConst.fill = GridBagConstraints.BOTH;
		gbConst.gridx = 0;
		gbConst.gridy = 0;
		JCheckBox cbInitLayout = new JCheckBox("Platform Window");
		pnlWinOpts.add(cbInitLayout, gbConst);
		gbConst.gridy++;
		pnlWinOpts.add(new JLabel("Name"), gbConst);
		gbConst.gridy++;
		pnlWinOpts.add(new JLabel("URL"), gbConst);
		gbConst.gridy++;
		pnlWinOpts.add(new JLabel("Default Window Width"), gbConst);
		gbConst.gridy++;
		pnlWinOpts.add(new JLabel("Default Window Height"), gbConst);
		gbConst.gridy++;
		pnlWinOpts.add(new JLabel("Default Window Centered"), gbConst);
		gbConst.weightx = 1;
		gbConst.gridwidth = 2;
		gbConst.gridx = 1;
		gbConst.gridy = 1;
		JTextField tfName = new JTextField("windowName");
		pnlWinOpts.add(tfName, gbConst);
		gbConst.gridy++;
		JTextField tfUrl = new JTextField("https://openfin.co");
		pnlWinOpts.add(tfUrl, gbConst);
		gbConst.gridy++;
		JTextField tfWinWidth = new JTextField("");
		pnlWinOpts.add(tfWinWidth, gbConst);
		gbConst.gridy++;
		JTextField tfWinHeight = new JTextField("");
		pnlWinOpts.add(tfWinHeight, gbConst);
		gbConst.gridy++;
		JCheckBox cbWinCenter = new JCheckBox("", false);
		pnlWinOpts.add(cbWinCenter, gbConst);
		gbConst.gridwidth = 1;
		gbConst.gridy++;
		gbConst.weighty = 1;
		pnlWinOpts.add(new JLabel(""), gbConst); // filler

		JButton btnCreate = new JButton("Create");
		btnCreate.setEnabled(false);
		btnCreate.addActionListener(e -> {
			FinPlatformObject platform = this.getSelectedNode(FinPlatformObject.class);
			if (platform != null) {
				this.platformCreateWindow(platform, tfName.getText(), tfUrl.getText(), cbInitLayout.isSelected(),
						tfWinWidth.getText(), tfWinHeight.getText(), cbWinCenter.isSelected());
			}
			else {
				// show popup warning?
			}
		});

		JPanel pnlTop = new JPanel(new BorderLayout(5, 5));
		pnlTop.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
		pnlTop.add(new JLabel("Selected Platform"), BorderLayout.WEST);
		JTextField tfSelectedPlatformUuid = new JTextField("N/A");
		tfSelectedPlatformUuid.setEditable(false);
		pnlTop.add(tfSelectedPlatformUuid, BorderLayout.CENTER);

		this.runtimeTree.addTreeSelectionListener(e -> {
			FinPlatformObject p = this.getSelectedNode(FinPlatformObject.class);
			btnCreate.setEnabled(p != null);
			tfSelectedPlatformUuid.setText(p == null ? "N/A" : p.getIdentity().getUuid());
		});

		JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		pnlBottom.add(btnCreate);
		pnl.add(pnlTop, BorderLayout.NORTH);
		pnl.add(pnlWinOpts, BorderLayout.CENTER);
		pnl.add(pnlBottom, BorderLayout.SOUTH);
		return pnl;
	}

	JPanel createViewPanel() {
		JPanel pnl = new JPanel(new BorderLayout());
		JPanel pnlWinOpts = new JPanel(new GridBagLayout());
		pnlWinOpts.setBorder(BorderFactory.createTitledBorder("View Options"));
		GridBagConstraints gbConst = new GridBagConstraints();
		gbConst.insets = new Insets(5, 5, 5, 5);
		gbConst.fill = GridBagConstraints.BOTH;
		gbConst.gridx = 0;
		gbConst.gridy = 0;
		pnlWinOpts.add(new JLabel("Name"), gbConst);
		gbConst.gridy++;
		pnlWinOpts.add(new JLabel("URL"), gbConst);
		gbConst.weightx = 1;
		gbConst.gridwidth = 2;
		gbConst.gridx = 1;
		gbConst.gridy = 0;
		JTextField tfName = new JTextField("");
		pnlWinOpts.add(tfName, gbConst);
		gbConst.gridy++;
		JTextField tfUrl = new JTextField("https://openfin.co");
		pnlWinOpts.add(tfUrl, gbConst);
		gbConst.gridwidth = 1;
		gbConst.gridy++;
		gbConst.weighty = 1;
		pnlWinOpts.add(new JLabel(""), gbConst); // filler

		JButton btnCreate = new JButton("Create");
		btnCreate.setEnabled(false);
		btnCreate.addActionListener(e -> {
			ViewOptions viewOpts = new ViewOptions();
			String name = tfName.getText();
			String url = tfUrl.getText();
			if (name != null && !name.trim().isEmpty()) {
				viewOpts.setName(name.trim());
			}
			if (url != null && !url.trim().isEmpty()) {
				viewOpts.setUrl(url.trim());
			}
			
			JsonObject customData = Json.createObjectBuilder().add("flashCnt", (int)(Math.random() * 10)).build();
			viewOpts.setCustomData(customData);
			
			FinPlatformObject p = this.getSelectedNode(FinPlatformObject.class);
			FinWindowObject w = this.getSelectedNode(FinWindowObject.class);
			if (p != null) {
				this.platformCreateView(p, viewOpts, null);

			}
			else if (w != null) {
				p = (FinPlatformObject) ((DefaultMutableTreeNode) this.runtimeTree.getSelectionPath().getParentPath()
						.getLastPathComponent()).getUserObject();
				this.platformCreateView(p, viewOpts, w.getIdentity());
			}
		});
		JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		pnlBottom.add(btnCreate);

		JPanel pnlTop = new JPanel(new BorderLayout(5, 5));
		pnlTop.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
		pnlTop.add(new JLabel("Selected"), BorderLayout.WEST);
		JTextField tfSelectedWinIdentity = new JTextField("N/A");
		tfSelectedWinIdentity.setEditable(false);
		pnlTop.add(tfSelectedWinIdentity, BorderLayout.CENTER);

		this.runtimeTree.addTreeSelectionListener(e -> {
			FinPlatformObject p = this.getSelectedNode(FinPlatformObject.class);
			FinWindowObject w = this.getSelectedNode(FinWindowObject.class);
			btnCreate.setEnabled(p != null || w != null);
			if (p != null) {
				tfSelectedWinIdentity.setText("Platform: " + p.getIdentity().getUuid());

			}
			else if (w != null) {
				tfSelectedWinIdentity.setText("Window: " + w.getIdentity().getName());
			}
			else {
				tfSelectedWinIdentity.setText("N/A");
			}
		});

		pnl.add(pnlTop, BorderLayout.NORTH);
		pnl.add(pnlWinOpts, BorderLayout.CENTER);
		pnl.add(pnlBottom, BorderLayout.SOUTH);
		return pnl;
	}

	JPanel createSnapshotPanel() {
		JPanel pnlSnapshot = new JPanel(new BorderLayout(5, 5));
		JPanel pnlCenter = new JPanel();
		pnlCenter.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));

		JPanel pnlSnapshots = new JPanel(new BorderLayout(10, 10));
		pnlSnapshots.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Snapshots"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		DefaultListModel<Object[]> snapshotListModel = new DefaultListModel<>();

		JList<Object[]> lstSnapshots = new JList<>(snapshotListModel);
		DefaultListCellRenderer renderer = new DefaultListCellRenderer();
		lstSnapshots.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
			JLabel lbl = (JLabel) renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			lbl.setText(value[0].toString());
			return renderer;
		});

		pnlSnapshots.add(new JScrollPane(lstSnapshots), BorderLayout.CENTER);

		JPanel pnlButtons = new JPanel(new GridLayout(10, 1, 5, 5));
		JButton btnGet = new JButton("Get");
		btnGet.setEnabled(false);
		btnGet.addActionListener(ae -> {
			FinPlatformObject p = this.getSelectedNode(FinPlatformObject.class);
			if (p != null) {
				p.getSnapshot().thenAccept(snapshot -> {
					System.out.println("snapshot: " + snapshot.toString());
					SwingUtilities.invokeLater(() -> {
						String name = JOptionPane.showInputDialog(pnlSnapshots, "Snapshot Name",
								"snapshot-" + (snapshotListModel.getSize() + 1));
						if (name != null) {
							Object[] snapshotData = {name, snapshot};
							snapshotListModel.addElement(snapshotData);
						}
					});
				});
			}
		});

		JButton btnApply = new JButton("Apply");
		btnApply.setEnabled(false);
		btnApply.addActionListener(e -> {
			int rv = JOptionPane.showConfirmDialog(pnlSnapshots, "Close Existing Windows?", "Apply Snapshot",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (rv == JOptionPane.YES_OPTION || rv == JOptionPane.NO_OPTION) {
				FinPlatformObject p = this.getSelectedNode(FinPlatformObject.class);
				ApplySnapshotOptions snapshotOpts = new ApplySnapshotOptions();
				snapshotOpts.setCloseExistingWindows(rv == JOptionPane.YES_OPTION);
				Object[] snapshotData = lstSnapshots.getSelectedValue();
				p.applySnapshot((Snapshot)snapshotData[1], snapshotOpts);
			}
		});

		lstSnapshots.addListSelectionListener(e -> {
			btnApply.setEnabled(lstSnapshots.getSelectedIndex() != -1);
		});

		pnlButtons.add(btnGet);
		pnlButtons.add(btnApply);

		pnlSnapshots.add(pnlButtons, BorderLayout.EAST);

		pnlCenter.add(pnlSnapshots);
		pnlCenter.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, Short.MAX_VALUE),
				new Dimension(0, Short.MAX_VALUE)));

		JPanel pnlTop = new JPanel(new BorderLayout(5, 5));
		pnlTop.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
		pnlTop.add(new JLabel("Selected Platform"), BorderLayout.WEST);
		JTextField tfSelectedPlatformUuid = new JTextField("N/A");
		tfSelectedPlatformUuid.setEditable(false);
		pnlTop.add(tfSelectedPlatformUuid, BorderLayout.CENTER);

		this.runtimeTree.addTreeSelectionListener(e -> {
			FinPlatformObject p = this.getSelectedNode(FinPlatformObject.class);
			btnGet.setEnabled(p != null);
			btnApply.setEnabled(p != null && lstSnapshots.getSelectedIndex() != -1);
			tfSelectedPlatformUuid.setText(p == null ? "N/A" : p.getIdentity().getUuid());
		});

		pnlSnapshot.add(pnlTop, BorderLayout.NORTH);
		pnlSnapshot.add(pnlCenter, BorderLayout.CENTER);
		return pnlSnapshot;
	}

	JPanel createLayoutPanel() {
		JPanel pnlLayout = new JPanel(new BorderLayout(5, 5));
		JPanel pnlCenter = new JPanel();
		pnlCenter.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));
		JPanel pnlPreset = new JPanel();
		pnlPreset.setLayout(new BoxLayout(pnlPreset, BoxLayout.X_AXIS));
		pnlPreset.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Apply Preset"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		JComboBox<FinLayoutObject.PresetLayout> cbPreset = new JComboBox<>(new PresetLayout[] { PresetLayout.COLUMNS, PresetLayout.ROWS, PresetLayout.GRID, PresetLayout.TABS });
		cbPreset.setPreferredSize(new Dimension(Short.MAX_VALUE, cbPreset.getPreferredSize().height));
		JButton btnApply = new JButton("Apply");
		btnApply.setEnabled(false);
		btnApply.addActionListener(e -> {
			FinWindowObject win = this.getSelectedNode(FinWindowObject.class);
			if (win != null) {
				fin.Layout.wrap(win.getIdentity()).thenAccept(layout->{
					layout.applyPreset((PresetLayout) cbPreset.getSelectedItem());
				});
			}
			else {
			}
		});
		pnlPreset.add(cbPreset);
		pnlPreset.add(Box.createHorizontalStrut(5));
		pnlPreset.add(btnApply);

		JPanel pnlLayouts = new JPanel(new BorderLayout(10, 10));
		pnlLayouts.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Layouts"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		DefaultListModel<Object[]> layoutListModel = new DefaultListModel<>();

		JList<Object[]> lstLayouts = new JList<>(layoutListModel);
		DefaultListCellRenderer renderer = new DefaultListCellRenderer();
		lstLayouts.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
			JLabel lbl = (JLabel) renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			lbl.setText(value[0].toString());
			return renderer;
		});

		pnlLayouts.add(new JScrollPane(lstLayouts), BorderLayout.CENTER);

		JPanel pnlButtons = new JPanel(new GridLayout(10, 1, 5, 5));
		JButton btnGet = new JButton("Get");
		btnGet.setEnabled(false);
		btnGet.addActionListener(ae -> {
			FinWindowObject win = this.getSelectedNode(FinWindowObject.class);
			if (win != null) {
				fin.Layout.wrap(win.getIdentity()).thenAccept(layout->{
					layout.getConfig().thenAccept(l -> {
						SwingUtilities.invokeLater(() -> {
							String name = JOptionPane.showInputDialog(pnlLayouts, "Layout Name",
									"layout-" + (layoutListModel.getSize() + 1));
							if (name != null) {
								Object[] layoutData = new Object[] {name, l};
								layoutListModel.addElement(layoutData);
							}
						});
					});
				});
			}
		});

		JButton btnReplace = new JButton("Replace");
		btnReplace.setEnabled(false);
		btnReplace.addActionListener(e -> {
			FinWindowObject win = this.getSelectedNode(FinWindowObject.class);
			fin.Layout.wrap(win.getIdentity()).thenAccept(layout->{
				layout.replace((LayoutConfig) ((Object[])lstLayouts.getSelectedValue())[1]);
			});
		});

		lstLayouts.addListSelectionListener(e -> {
			btnReplace.setEnabled(lstLayouts.getSelectedIndex() != -1);
		});

		pnlButtons.add(btnGet);
		pnlButtons.add(btnReplace);

		pnlLayouts.add(pnlButtons, BorderLayout.EAST);

		pnlCenter.add(pnlPreset);
		pnlCenter.add(Box.createVerticalStrut(10));
		pnlCenter.add(pnlLayouts);
		pnlCenter.add(new Box.Filler(new Dimension(0, 0), new Dimension(0, Short.MAX_VALUE),
				new Dimension(0, Short.MAX_VALUE)));

		JPanel pnlTop = new JPanel(new BorderLayout(5, 5));
		pnlTop.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
		pnlTop.add(new JLabel("Selected Window"), BorderLayout.WEST);
		JTextField tfSelectedWindow = new JTextField("N/A");
		tfSelectedWindow.setEditable(false);
		pnlTop.add(tfSelectedWindow, BorderLayout.CENTER);

		this.runtimeTree.addTreeSelectionListener(e -> {
			FinWindowObject win = this.getSelectedNode(FinWindowObject.class);
			btnApply.setEnabled(win != null);
			btnGet.setEnabled(win != null);
			lstLayouts.setEnabled(win != null);
			btnReplace.setEnabled(win != null && lstLayouts.getSelectedIndex() != -1);
			tfSelectedWindow.setText(win == null ? "N/A" : win.getIdentity().toString());
		});

		pnlLayout.add(pnlTop, BorderLayout.NORTH);
		pnlLayout.add(pnlCenter, BorderLayout.CENTER);

		return pnlLayout;
	}

	JPanel createContentPane() {
		JPanel pnlLeft = this.createRuntimeTreePanel();
		JPanel pnlRight = new JPanel(new BorderLayout());
		pnlRight.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Platform", createPlatformPanel());
		tabs.addTab("Window", createWindowPanel());
		tabs.addTab("View", createViewPanel());
		tabs.addTab("Snapshot", createSnapshotPanel());
		tabs.addTab("Layout", createLayoutPanel());

		pnlRight.add(tabs, BorderLayout.CENTER);
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlLeft, pnlRight);
		splitPane.setDividerLocation(350);
		JPanel pnl = new JPanel(new BorderLayout());
		pnl.add(splitPane, BorderLayout.CENTER);

		return pnl;
	}

	void createGui() {
		this.frame = new JFrame("OpenFin Platform API Demo");
		this.frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				windowClosing = true;
				PlatformApiDemo.this.frame.setVisible(false);
				int cnt = rootNode.getChildCount();
				if (cnt == 0) {
					fin.disconnect();
				}
				else {
					ArrayList<CompletableFuture<?>> quitFutures = new ArrayList<>();
					for (int i = 0; i < cnt; i++) {
						DefaultMutableTreeNode pNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
						FinPlatformObject p = (FinPlatformObject) pNode.getUserObject();
						quitFutures.add(p.quit().toCompletableFuture());
					}
					try {
						CompletableFuture.allOf(quitFutures.toArray(new CompletableFuture<?>[cnt])).get(10,
								TimeUnit.SECONDS);
					}
					catch (InterruptedException | ExecutionException | TimeoutException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

		this.glassPane = this.createGlassPane();
		this.frame.setGlassPane(glassPane);
		this.glassPane.setVisible(true);
		this.frame.setContentPane(this.createContentPane());
		this.frame.setPreferredSize(new Dimension(850, 600));
		this.frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.frame.pack();
		this.frame.setLocationRelativeTo(null);
		this.frame.setVisible(true);
	}

	void deleteViewNode(DefaultMutableTreeNode viewNode) {
		SwingUtilities.invokeLater(() -> {
			this.platformTreeModel.removeNodeFromParent(viewNode);
		});
	}

	void addViewNode(DefaultMutableTreeNode winNode, Identity viewIdentity) {
		fin.View.wrap(viewIdentity).thenAccept(view->{
			view.getOptions().thenAccept(viewOpts->{
				JsonValue customData = (JsonValue) viewOpts.getCustomData();
				if (customData != null) {
					System.out.println("customData: " + customData);
					int flashCnt = customData.asJsonObject().getInt("flashCnt", 0);
					if (flashCnt > 0) {
						System.out.println("view attached, need to flash " + flashCnt + " times.");
					}
				}
			});
			
			FinWindowObject window = (FinWindowObject) winNode.getUserObject();
			DefaultMutableTreeNode viewNode = new DefaultMutableTreeNode(view);
			window.addEventListener("view-detached", e -> {
				JsonObject vId = e.getJsonObject("viewIdentity");
				if (Objects.equals(viewIdentity.getUuid(), vId.getString("uuid"))
						&& Objects.equals(viewIdentity.getName(), vId.getString("name"))) {
					this.deleteViewNode(viewNode);
				}
			});
			SwingUtilities.invokeLater(() -> {
				this.platformTreeModel.insertNodeInto(viewNode, winNode, winNode.getChildCount());
				this.runtimeTree.expandPath(new TreePath(winNode.getPath()));
			});
		});
	}

	void deleteWindowNode(DefaultMutableTreeNode winNode) {
		SwingUtilities.invokeLater(() -> {
			this.platformTreeModel.removeNodeFromParent(winNode);
		});
	}

	void addWindowNode(DefaultMutableTreeNode platformNode, Identity winIdentity) {
		FinPlatformObject platform = (FinPlatformObject) platformNode.getUserObject();
		fin.Window.wrap(winIdentity).thenAccept(window->{
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(window);
			platform.addEventListener("window-closed", e -> {
				System.out.println("window-closed: " + e);
				String uuid = e.getString("uuid");
				String name = e.getString("name");
				if (Objects.equals(winIdentity.getUuid(), uuid) && Objects.equals(winIdentity.getName(), name)) {
					deleteWindowNode(node);
				}
			});
			window.addEventListener("view-attached", e -> {
				System.out.println("view-attached: " + e);
				addViewNode(node, FinBeanUtils.fromJsonObject(e.getJsonObject("viewIdentity"), Identity.class));
			});
			SwingUtilities.invokeLater(() -> {
				this.platformTreeModel.insertNodeInto(node, platformNode, platformNode.getChildCount());
				this.runtimeTree.expandPath(new TreePath(platformNode.getPath()));
			});
		});
	}

	void deletePlatformNode(DefaultMutableTreeNode platformNode) {
		SwingUtilities.invokeLater(() -> {
			this.platformTreeModel.removeNodeFromParent(platformNode);
			if (windowClosing && rootNode.getChildCount() == 0) {
				fin.disconnect();
			}
		});
	}

	void addPlatformNode(String uuid) {
		SwingUtilities.invokeLater(() -> {
			fin.Platform.wrap(uuid).thenAccept(platform->{
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(platform);
				FinEventListener listener = new FinEventListener() { 
					@Override
					public void onEvent(JsonObject e) {
						System.out.println("application-closed: " + e);
						String eUuid = e.getString("uuid");
						if (Objects.equals(uuid, eUuid)) {
							deletePlatformNode(node);
							fin.System.removeEventListener("application-closed", this);
						}
					}
				};
				fin.System.addEventListener("application-closed", listener);
				platform.addEventListener("window-created", e -> {
					System.out.println("window-created: " + e);
					String eUuid = e.getString("uuid");
					String eName = e.getString("name");
					addWindowNode(node, new Identity(eUuid, eName));
				});
				this.platformTreeModel.insertNodeInto(node, this.rootNode, this.rootNode.getChildCount());
				this.runtimeTree.expandRow(0);
			});
		});
	}

	void launchOpenFin() {
		RuntimeConfig config = new RuntimeConfig();
		config.getRuntime().setVersion("stable");
		config.getRuntime().setArguments("--v=1");
		
		FinLauncher.newLauncherBuilder().connectionListener(new FinRuntimeConnectionListener() {
			@Override
			public void onClose(String reason) {
				System.exit(0);
			}
		}).runtimeConfig(config).build().launch().thenAccept(fin -> {
			this.fin = fin;
			fin.System.addEventListener("application-platform-api-ready", e -> {
				System.out.println("application-platform-api-ready: " + e);
				String uuid = e.getString("uuid");
				addPlatformNode(uuid);
			});

			fin.System.addEventListener("window-created", e -> {
				System.out.println("system::window-created: " + e);
				String uuid = e.getString("uuid");
				String name = e.getString("name");
			});

			SwingUtilities.invokeLater(() -> {
				this.glassPane.setVisible(false);
			});
		});	}

	void platformStartFromManifest(String manifest) {
		fin.Platform.startFromManifest(manifest).exceptionally(e -> {
			e.printStackTrace();
			return null;
		});
	}

	void platformStart(String uuid, String width, String height, boolean center, String url, boolean createView) {
		PlatformOptions opts = new PlatformOptions(uuid);
		DefaultWindowOptions winOpts = new DefaultWindowOptions();
		if (!width.isEmpty()) {
			winOpts.setDefaultWidth(Integer.parseInt(width));
		}
		if (!height.isEmpty()) {
			winOpts.setDefaultHeight(Integer.parseInt(height));
		}
		if (center) {
			winOpts.setDefaultCentered(center);
		}
		opts.setDefaultWindowOptions(winOpts);
		if (!url.isEmpty()) {
			ViewOptions defViewOpts = new ViewOptions();
			defViewOpts.setUrl(url);
			opts.setDefaultViewOptions(defViewOpts);
		}
		fin.Platform.start(opts).thenAccept(p -> {
			System.out.println("processing thread: " + Thread.currentThread().getName());
			if (createView) {
				ViewOptions viewOpts = new ViewOptions();
				this.platformCreateView(p, viewOpts, null);
			}
		}).exceptionally(e -> {
			e.printStackTrace();
			return null;
		});
	}

	void platformCreateWindow(FinPlatformObject platform, String winName, String url, boolean initLayout, String width,
			String height, boolean center) {
		WindowOptions winOpts = new WindowOptions();
		if (!width.isEmpty()) {
			winOpts.setDefaultWidth(Integer.parseInt(width));
		}
		if (!height.isEmpty()) {
			winOpts.setDefaultHeight(Integer.parseInt(height));
		}
		if (center) {
			winOpts.setDefaultCentered(center);
		}

		winOpts.setName(winName);
		if (initLayout) {
			ViewOptions viewOpts = new ViewOptions();
			viewOpts.setUrl(url);
			//config->stack->component
			//1. component
			LayoutItem itemComponent = new LayoutItem(LayoutItem.TYPE_COMPONENT);
			itemComponent.setComponentState(viewOpts);
			//2. stack
			LayoutItem itemStack = new LayoutItem(LayoutItem.TYPE_STACK);
			itemStack.add(itemComponent);
			//3. config
			LayoutConfig layoutConfig = new LayoutConfig();
			layoutConfig.add(itemStack);
			winOpts.setLayout(layoutConfig);
		}
		else {
			winOpts.setUrl(url);
		}

		platform.createWindow(winOpts);
	}

	void platformCreateView(FinPlatformObject platform, ViewOptions viewOpts, Identity targtWindow) {
		platform.createView(viewOpts, targtWindow).thenAccept(view ->{
			System.out.println("view created");
		}).exceptionally(e->{
			e.printStackTrace();
			return null;
		});
	}

	void platformSaveSnapshot(FinPlatformObject platform, File path) {
		platform.getSnapshot().thenAccept(snapshot -> {
			try {
				Files.write(path.toPath(), FinBeanUtils.toJsonString(snapshot).getBytes(), StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	void platformApplySnapshot(FinPlatformObject platform, File path, boolean closeExistingWindows) {
		ApplySnapshotOptions opts = null;
		if (closeExistingWindows) {
			opts = new ApplySnapshotOptions();
			opts.setCloseExistingWindows(true);
		}
		platform.applySnapshot(path.getAbsolutePath(), opts);
	}

	public static void main(String[] args) {
		new PlatformApiDemo();
	}
}
