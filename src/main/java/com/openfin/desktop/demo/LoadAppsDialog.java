package com.openfin.desktop.demo;

import info.clearthought.layout.TableLayout;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by richard on 2/28/15.
 */
public class LoadAppsDialog extends JDialog {
    protected JTextField username;
    protected JPasswordField password;

    protected JLabel usernameLabel, passwordLabel;

    protected JButton retrieveApps;
    protected boolean retrieveClicked = false;

    public LoadAppsDialog() {
        super();
        setTitle("Load Apps");
        setModal(true);
        setLayout(new BorderLayout());

        this.add(layoutDescriptionPanel(), BorderLayout.CENTER);
        this.add(layoutActionButtonPanel(), BorderLayout.SOUTH);

        setSize(300, 140);
    }

    private JPanel layoutDescriptionPanel() {

        usernameLabel = new JLabel("Username: ");
        usernameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        username = new JTextField();
        username.setColumns(50);

        passwordLabel = new JLabel("password: ");
        passwordLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        password = new JPasswordField();
        password.setColumns(50);

        double size[][] =
                {{0.25, 0.75},
                        {30,30}};

        TableLayout twoColLayout = new TableLayout(size);
        JPanel twoColPanel = new JPanel(twoColLayout);

        twoColPanel.add(usernameLabel, "0, 0");
        twoColPanel.add(username, "1, 0");
        twoColPanel.add(passwordLabel , "0, 1");
        twoColPanel.add(password , "1, 1");

        return twoColPanel;
    }

    public JSONObject getCredentials() {
        JSONObject credentials = new JSONObject();
        if (retrieveClicked) {
            String email = username.getText();
            char[] pwd = password.getPassword();

            try {
                credentials.put("email", email);
                credentials.put("password", new String(pwd));
            } catch (JSONException e) {
            }
        }
        return credentials;
    }

    public void show(Component parent) {
        this.retrieveClicked = false;
        this.setLocationRelativeTo(parent);
        setVisible(true);
    }

    private JPanel layoutActionButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        retrieveApps = new JButton("Load Apps");
        retrieveApps.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                retrieveClicked = true;
                LoadAppsDialog.this.setVisible(false);
            }
        });
        buttonPanel.add(retrieveApps);

        return buttonPanel;
    }


}
