package com.openfin.desktop.demo;

import com.openfin.desktop.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 * Example of running OpenFin from WebStart
 *
 */

public class JNLPExample extends JFrame {

    private JButton startButton = new JButton("Start Hello OpenFin Demo");
    private DesktopConnection desktopConnection;

    final DesktopStateListener listener = new DesktopStateListener() {
        @Override
        public void onReady() {
            java.lang.System.out.println("Connected to OpenFin Runtime");
            InterApplicationBus bus = desktopConnection.getInterApplicationBus();
            try {
                launchHTML5App();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onClose(String error) {
            java.lang.System.out.println("Connection closed");
        }

        @Override
        public void onError(String reason)  {
            java.lang.System.out.println("Connection failed: " + reason);
        }

        @Override
        public void onMessage(String message) {
        }

        @Override
        public void onOutgoingMessage(String message) {
        }
    };


    public JNLPExample() throws DesktopException {
        super("Jave Web Start Example on OpenFin");
        this.setSize(350, 200);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(null);

        startButton.setSize(200, 30);
        startButton.setLocation(80, 50);
        this.getContentPane().add(startButton);

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    RuntimeConfiguration configuration = new RuntimeConfiguration();
                    configuration.setRuntimeVersion("stable");
                    desktopConnection.connect(configuration, listener, 60);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });


        this.desktopConnection = new DesktopConnection("WebStartExample");
    }

    private void launchHTML5App() {

        ApplicationOptions applicationOptions = new ApplicationOptions("Hello OpenFin", "Hello OpenFin",
                "http://demoappdirectory.openf.in/desktop/config/apps/OpenFin/HelloOpenFin/index.html");
        WindowOptions windowOptions = new WindowOptions();
        windowOptions.setAutoShow(true);
        windowOptions.setFrame(false);
        windowOptions.setResizable(false);
        windowOptions.setDefaultHeight(525);
        windowOptions.setDefaultWidth(395);
        windowOptions.setDefaultTop(50);
        windowOptions.setDefaultLeft(10);
        applicationOptions.setMainWindowOptions(windowOptions);
        Application app = new Application(applicationOptions, desktopConnection, new AckListener() {
            @Override
            public void onSuccess(Ack ack) {
                if (ack.isSuccessful()) {
                    Application application = (Application) ack.getSource();
                    try {
                        application.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onError(Ack ack) {

            }
        });
    }

    public static void main(String[] args) throws DesktopException {
        JNLPExample exp = new JNLPExample();
        exp.setVisible(true);
    }


}
