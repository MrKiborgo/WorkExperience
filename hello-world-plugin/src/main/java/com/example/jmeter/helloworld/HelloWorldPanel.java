package com.example.jmeter.helloworld;

import javax.swing.*;
import java.awt.*;

/**
 * Very small swing panel which simply shows "Hello World" centred.
 */
public class HelloWorldPanel extends JPanel {

    public HelloWorldPanel() {
        super(new BorderLayout());
        JLabel label = new JLabel("Hello World", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize() + 4));
        add(label, BorderLayout.CENTER);
    }
} 