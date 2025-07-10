package com.company;

import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.config.ConfigTestElement;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * Gemini Plugin Initializer - Hidden GUI Component
 * This component is automatically loaded by JMeter's class scanner
 * and initializes the Gemini Side Panel when loaded
 */
public class GeminiPluginInitializer extends AbstractConfigGui {
    
    private static boolean initialized = false;
    
    public GeminiPluginInitializer() {
        super();
        initializeGeminiPlugin();
        setVisible(false); // Hide this component - it's just for initialization
    }
    
    private void initializeGeminiPlugin() {
        if (!initialized) {
            SwingUtilities.invokeLater(() -> {
                try {
                    // Small delay to ensure JMeter GUI is fully loaded
                    Thread.sleep(2000);
                    
                    System.out.println("🤖 Initializing Gemini AI Side Panel Plugin...");
                    GeminiSidePanelPlugin.installPlugin();
                    initialized = true;
                    System.out.println("✅ Gemini AI Side Panel Plugin initialized successfully!");
                } catch (Exception e) {
                    System.err.println("❌ Failed to initialize Gemini AI Side Panel Plugin: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
    
    @Override
    public String getLabelResource() {
        return "gemini_plugin_initializer"; // This won't be shown in UI
    }
    
    @Override
    public String getStaticLabel() {
        return "Gemini Plugin Initializer"; // This won't be shown in UI
    }
    
    @Override
    public TestElement createTestElement() {
        ConfigTestElement element = new ConfigTestElement();
        element.setName("Gemini Plugin Initializer");
        return element;
    }
    
    @Override
    public void modifyTestElement(TestElement element) {
        // Nothing to modify
    }
    
    @Override
    public void configure(TestElement element) {
        // Nothing to configure
    }
    
    @Override
    public void clearGui() {
        // Nothing to clear
    }
    
    /**
     * Hide this component from JMeter's add menu
     */
    @Override
    public Collection<String> getMenuCategories() {
        return null; // Don't show in any menu
    }
} 