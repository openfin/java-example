package com.openfin.desktop.demo;

import com.openfin.desktop.ApplicationOptions;
import com.openfin.desktop.WindowOptions;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.UUID;

/**
 * Created by richard on 2/28/15.
 */
public class AppCreateDialog extends JDialog {
    protected JTextField uuidText, nameText, urlText, versionText, applicationIconText,
            defaultLeftText, defaultTopText, defaultWidthText, defaultHeightText, windowStateText,
            applicationIconTopText, applicationIconLeftText;

    protected JLabel uuidLabel, nameLabel, urlLabel, versionLabel, applicationIconLabel,
            defaultLeftLabel, defaultTopLabel, defaultWidthLabel, defaultHeightLabel, windowStateLabel,
            applicationIconTopLabel, applicationIconLeftLabel;

    protected JCheckBox resizeCheck, frameCheck, alwaysOnBottomCheck,
            showTaskbarIconCheck, autoShowCheck, draggableCheck;

    protected JButton createButton;
    protected boolean createClicked = false;

    public AppCreateDialog() {
        super();
        setTitle("Create Application");
        setModal(true);
        setLayout(new BorderLayout());

        this.add(layoutDescriptionPanel(), BorderLayout.CENTER);
        this.add(layoutActionButtonPanel(), BorderLayout.SOUTH);

        setSize(500, 600);
    }

    private JPanel layoutDescriptionPanel() {

        uuidLabel = new JLabel("UUID: ");
        uuidLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        uuidText = new JTextField(UUID.randomUUID().toString());
        uuidText.setColumns(50);

        nameLabel = new JLabel("Name: ");
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        nameText = new JTextField("FX Live v1.4.0.0b");
        nameText.setColumns(50);

        urlLabel = new JLabel("URL: ");
        urlLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        urlText = new JTextField("https://fxlive.openf.in/fxlive/1.4.0.0b/");
        urlText.setColumns(50);

        versionLabel = new JLabel("Version: ");
        versionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        versionText = new JTextField("1.1.0.0b");
        versionText.setColumns(50);

        applicationIconLabel = new JLabel("Icon URL:" );
        applicationIconLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        applicationIconText = new JTextField("https://fxlive.openf.in/fxlive/1.1.0.0b/img/FXlive_icon.png");
        applicationIconText.setColumns(50);

        defaultLeftLabel = new JLabel("Left: ");
        defaultLeftLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        defaultLeftText = new JTextField("10");
        defaultLeftText.setColumns(4);

        defaultTopLabel = new JLabel("Top: ");
        defaultTopLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        defaultTopText = new JTextField("10");
        defaultTopText.setColumns(4);

        applicationIconLeftLabel = new JLabel("Icon Left: ");
        applicationIconLeftLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        applicationIconLeftText = new JTextField("100");
        applicationIconLeftText.setColumns(4);

        applicationIconTopLabel = new JLabel("Icon Top: ");
        applicationIconTopLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        applicationIconTopText = new JTextField("100");
        applicationIconTopText.setColumns(4);

        defaultWidthLabel = new JLabel("Width: ");
        defaultWidthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        defaultWidthText = new JTextField("858");
        defaultWidthText.setColumns(4);

        defaultHeightLabel = new JLabel("Height: ");
        defaultHeightLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        defaultHeightText = new JTextField("545");
        defaultHeightText.setColumns(4);

        windowStateLabel = new JLabel("Window State: ");
        windowStateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        windowStateText = new JTextField("normal");
        windowStateText.setColumns(10);

        resizeCheck = new JCheckBox("resize", true);
        frameCheck = new JCheckBox("frame", true);
        alwaysOnBottomCheck = new JCheckBox("alwaysOnBottom", false);
        showTaskbarIconCheck = new JCheckBox("showTaskbarIcon", true);
        autoShowCheck = new JCheckBox("autoShow", true);
        draggableCheck = new JCheckBox("draggable", false);

        JPanel fieldPanel = new JPanel();

        fieldPanel.setLayout(new javax.swing.BoxLayout(fieldPanel, javax.swing.BoxLayout.Y_AXIS));

        double size[][] =
                {{0.25, 0.75},
                        {30,30,30,30,30,30,30,30,30,30,30,30}};

        TableLayout twoColLayout = new TableLayout(size);
        JPanel twoColPanel = new JPanel(twoColLayout);

        twoColPanel.add(uuidLabel, "0, 0");
        twoColPanel.add(uuidText, "1, 0");
        twoColPanel.add(nameLabel , "0, 1");
        twoColPanel.add(nameText , "1, 1");
        twoColPanel.add(urlLabel , "0, 2");
        twoColPanel.add(urlText , "1, 2");
        twoColPanel.add(versionLabel , "0, 3");
        twoColPanel.add(versionText , "1, 3");
        twoColPanel.add(applicationIconLabel , "0, 4");
        twoColPanel.add(applicationIconText , "1, 4");
        twoColPanel.add(applicationIconLeftLabel , "0, 5");
        twoColPanel.add(applicationIconLeftText , "1, 5");
        twoColPanel.add(applicationIconTopLabel , "0, 6");
        twoColPanel.add(applicationIconTopText , "1, 6");
        twoColPanel.add(defaultLeftLabel , "0, 7");
        twoColPanel.add(defaultLeftText , "1, 7");
        twoColPanel.add(defaultTopLabel , "0, 8");
        twoColPanel.add(defaultTopText , "1, 8");
        twoColPanel.add(defaultWidthLabel , "0, 9");
        twoColPanel.add(defaultWidthText , "1, 9");
        twoColPanel.add(defaultHeightLabel , "0, 10");
        twoColPanel.add(defaultHeightText , "1, 10");
        twoColPanel.add(windowStateLabel , "0, 11");
        twoColPanel.add(windowStateText , "1, 11");

        fieldPanel.add(twoColPanel);
        fieldPanel.add(resizeCheck);
        fieldPanel.add(frameCheck);
        fieldPanel.add(alwaysOnBottomCheck);
        fieldPanel.add(showTaskbarIconCheck);
        fieldPanel.add(autoShowCheck);
        fieldPanel.add(draggableCheck);
        return fieldPanel;
    }

    public ApplicationOptions getApplicatonOptions() {
        ApplicationOptions options = null;
        if (createClicked) {
            String uuid = uuidText.getText();
            String name = nameText.getText();
            String url = urlText.getText();
            String version = versionText.getText();
            String applicationIcon = applicationIconText.getText();
            int applicationIconTop = Integer.parseInt(applicationIconTopText.getText());
            int applicationIconLeft = Integer.parseInt(applicationIconLeftText.getText());
            int defaultLeft = Integer.parseInt(defaultLeftText.getText());
            int defaultTop = Integer.parseInt(defaultTopText.getText());
            int defaultWidth = Integer.parseInt(defaultWidthText.getText());
            int defaultHeight = Integer.parseInt(defaultHeightText.getText());
            String state = windowStateText.getText();
            boolean resize = resizeCheck.isSelected();
            boolean frame = frameCheck.isSelected();
            boolean alwaysOnBottom = alwaysOnBottomCheck.isSelected();
            boolean showTaskbarIcon = showTaskbarIconCheck.isSelected();
            boolean autoShow = autoShowCheck.isSelected();
            boolean draggable = draggableCheck.isSelected();

            options = new ApplicationOptions(name, uuid, url);
            options.setVersion(version);
            options.setApplicationIcon(applicationIcon);

            WindowOptions mainWindowOptions = new WindowOptions();

            mainWindowOptions.setAutoShow(autoShow);
            mainWindowOptions.setDefaultHeight(defaultHeight);
            mainWindowOptions.setDefaultLeft(defaultLeft);
            mainWindowOptions.setDefaultTop(defaultTop);
            mainWindowOptions.setDefaultWidth(defaultWidth);
            mainWindowOptions.setState(state);
            mainWindowOptions.setResizable(resize);
            mainWindowOptions.setFrame(frame);
            mainWindowOptions.setShowTaskbarIcon(showTaskbarIcon);

            options.setMainWindowOptions(mainWindowOptions);
        }
        return options;
    }

    public void show(Component parent) {
        this.createClicked = false;
        uuidText.setText("OpenFinDesktopDemo");
        this.setLocationRelativeTo(parent);
        setVisible(true);
    }

    private JPanel layoutActionButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        createButton = new JButton("Create");
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createClicked = true;
                AppCreateDialog.this.setVisible(false);
            }
        });
        buttonPanel.add(createButton);

        return buttonPanel;
    }

}
