package com.openfin.desktop.demo;

import com.openfin.desktop.Application;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by richard on 2/28/15.
 */
public class DockingDemo extends JPanel implements ActionListener {
    private JFrame jFrame;
    private JButton dock_btn, undock_btn, alert_btn;
    private Application app;
    private final String name;
    private Rectangle origBounds;
    public DockingDemo(final String name) {
        setLayout(new BorderLayout());
        this.name = name;
        dock_btn = new JButton("Dock Me");
        dock_btn.setActionCommand("dock");
        dock_btn.addActionListener(this);
        undock_btn = new JButton("UnDock Me");
        undock_btn.setActionCommand("undock");
        undock_btn.addActionListener(this);
        alert_btn = new JButton("Alert");
        alert_btn.setActionCommand("alert");
        alert_btn.addActionListener(this);


        JPanel content = new JPanel();
        content.add(new JLabel());
        content.add(dock_btn);
        content.add(undock_btn);
        content.add(alert_btn);

        add(content);
        setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.red));
    }

    public void setFrame(JFrame frame) {
        this.jFrame = frame;
    }

    public JFrame getFrame() {
        return jFrame;
    }

    public void setApplication(Application app) {
        this.app = app;
    }

    public void actionPerformed(ActionEvent e) {
//        if ("dock".equals(e.getActionCommand())) {
//            if (this.app != null) {
//                origBounds = jFrame.getBounds();
//                this.app.dockWindow(getContentHwnd(), name);
//            }
//        }
//        else if ("undock".equals(e.getActionCommand())) {
//            if (this.app != null) {
//                this.app.unDockWindow(getContentHwnd(), name, (int)origBounds.getX(), (int)origBounds.getY());
//            }
//        }
//        else if ("alert".equals(e.getActionCommand())) {
//            JOptionPane.showMessageDialog(jFrame,
//                    "No More Docking.");
//        }
    }

    public void showHwnd() {
        long parentHwnd = getWindowId(jFrame);
        System.out.println("frameHwnd:" + parentHwnd + " " + getContentHwnd());
    }

    public String getContentHwnd() {
        long parentHwnd = getWindowId(jFrame);
        //return parentHwnd;
        return Long.toHexString(parentHwnd);
    }

    protected long getWindowId(Container frame) {

        try {
            // The reflection code below does the same as this
            // long handle = frame.getPeer() != null ? ((WComponentPeer) frame.getPeer()).getHWnd() : 0;

            Object wComponentPeer = invokeMethod(frame, "getPeer");

            return (Long) invokeMethod(wComponentPeer, "getHWnd");

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    protected Object invokeMethod(Object o, String methodName) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {

        Class c = o.getClass();
        for (Method m : c.getMethods()) {
            if (m.getName().equals(methodName)) {
                Object ret = m.invoke(o);
                return ret;
            }
        }
        throw new RuntimeException("Could not find method named '"+methodName+"' on class " + c);
    }

    public static DockingDemo createAndShowGUI(JFrame other, final String name) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    //UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }
        //Create and set up the window.
        JFrame jFrame = new JFrame("Java Docking Demo: " + name);
        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        //Create and set up the content pane.
        DockingDemo newContentPane = new DockingDemo(name);
        //newContentPane.setOpaque(true); //content panes must be opaque
        jFrame.setContentPane(newContentPane);
        //jFrame.addWindowListener(newContentPane);

        //Display the window.
        jFrame.pack();
        jFrame.setSize(300, 250);
        jFrame.setLocationRelativeTo(other);
        jFrame.setResizable(true);
        jFrame.setVisible(true);
        newContentPane.setFrame(jFrame);
        newContentPane.showHwnd();
        return  newContentPane;
    }

    public static void main(String[] args) {
        System.setProperty("sun.awt.noerasebackground", "true");
        //System.setProperty("sun.awt.erasebackgroundonresize", "true");
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(null, "Test");
            }
        });
    }
}
