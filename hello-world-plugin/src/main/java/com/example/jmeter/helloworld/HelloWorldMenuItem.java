package com.example.jmeter.helloworld;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.MainFrame;
import org.apache.jmeter.gui.plugin.MenuCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * JMenuItem that toggles a {@link HelloWorldPanel} on the right-hand side of the
 * JMeter UI. It reuses the split-pane logic seen in the Feather Wand plug-in but
 * trims it down to the minimum required.
 */
public class HelloWorldMenuItem extends JMenuItem implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(HelloWorldMenuItem.class);

    private static final String ACTION_CMD = "toggle_hello_panel";

    private static HelloWorldPanel panel;
    private static JSplitPane splitPane;

    public HelloWorldMenuItem(JComponent parent) {
        super("Hello World panel");
        setActionCommand(ACTION_CMD);
        addActionListener(this);
        addToolbarIcon();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (ACTION_CMD.equals(e.getActionCommand())) {
            togglePanel();
        }
    }

    private void togglePanel() {
        GuiPackage gp = GuiPackage.getInstance();
        if (gp == null) {
            log.warn("GuiPackage instance is null – cannot toggle panel");
            return;
        }
        MainFrame mainFrame = gp.getMainFrame();
        Container content = mainFrame.getContentPane();

        if (splitPane != null && splitPane.isShowing()) {
            // Remove panel, restore original centre component
            Component centre = splitPane.getLeftComponent();
            content.remove(splitPane);
            content.add(centre, BorderLayout.CENTER);
            splitPane = null;
            content.revalidate();
            content.repaint();
            log.debug("Hello World panel hidden");
        } else {
            if (panel == null) {
                panel = new HelloWorldPanel();
            }
            // Locate current centre component
            Component centreComp = null;
            for (Component comp : content.getComponents()) {
                if (content.getLayout() instanceof BorderLayout &&
                        BorderLayout.CENTER.equals(((BorderLayout) content.getLayout()).getConstraints(comp))) {
                    centreComp = comp;
                    break;
                }
            }
            if (centreComp == null) {
                log.warn("Could not find the central JMeter component to attach to");
                return;
            }
            content.remove(centreComp);
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centreComp, panel);
            splitPane.setResizeWeight(0.75);
            splitPane.setOneTouchExpandable(true);
            content.add(splitPane, BorderLayout.CENTER);
            content.revalidate();
            log.debug("Hello World panel displayed");
        }
    }

    private void addToolbarIcon() {
        GuiPackage gp = GuiPackage.getInstance();
        if (gp == null) {
            return;
        }
        MainFrame mainFrame = gp.getMainFrame();
        // Find the toolbar in the frame
        Component[] c = mainFrame.getContentPane().getComponents();
        for (Component comp : c) {
            if (comp instanceof JToolBar) {
                JToolBar tb = (JToolBar) comp;
                JButton btn = new JButton("HW"); // simple text button for now
                btn.setToolTipText("Toggle Hello World panel");
                btn.setActionCommand(ACTION_CMD);
                btn.addActionListener(this);
                tb.add(btn);
                break;
            }
        }
    }
} 