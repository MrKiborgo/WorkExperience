package com.company;

import org.apache.jmeter.gui.GuiPackage;

import javax.swing.*;

/**
 * Gemini Plugin Loader
 * Automatically installs the Gemini AI Side Panel when JMeter GUI starts
 */
public class GeminiPluginLoader {
    
    // Static block to initialize when class is loaded
    static {
        installGeminiPlugin();
    }
    
    /**
     * Install the Gemini plugin when JMeter GUI is ready
     */
    private static void installGeminiPlugin() {
        // Use a timer to check when JMeter GUI is ready
        Timer timer = new Timer(1000, null);
        timer.addActionListener(e -> {
            try {
                GuiPackage guiPackage = GuiPackage.getInstance();
                if (guiPackage != null && guiPackage.getMainFrame() != null) {
                    // GUI is ready, install the plugin
                    GeminiSidePanelPlugin.installPlugin();
                    timer.stop(); // Stop checking
                }
            } catch (Exception ex) {
                // GUI not ready yet, keep checking
            }
        });
        timer.start();
        
        System.out.println("Gemini Plugin Loader initialized - waiting for JMeter GUI...");
    }
    
    /**
     * Manual installation method for debugging
     */
    public static void manualInstall() {
        GeminiSidePanelPlugin.installPlugin();
    }
} 