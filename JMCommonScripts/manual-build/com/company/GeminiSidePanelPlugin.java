package com.company;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.HashSet;

/**
 * JMeter Plugin to integrate Gemini AI Side Panel
 * This plugin adds a toggleable AI assistant panel to JMeter's main interface
 */
public class GeminiSidePanelPlugin {
    
    private static final String MENU_ITEM_TEXT = "Toggle Gemini AI Panel";
    private static final String MENU_CATEGORY = "Tools";
    
    private static GeminiSidePanel sidePanel;
    private static boolean isPanelVisible = false;
    private static JFrame mainFrame;
    
    /**
     * Initialize the plugin when JMeter GUI starts
     */
    public static void installPlugin() {
        System.out.println("🔧 GeminiSidePanelPlugin.installPlugin() called");
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("🔧 Attempting to get GuiPackage instance...");
                GuiPackage guiPackage = GuiPackage.getInstance();
                if (guiPackage != null) {
                    System.out.println("✅ GuiPackage found, getting main frame...");
                    mainFrame = guiPackage.getMainFrame();
                    if (mainFrame != null) {
                        System.out.println("✅ Main frame found: " + mainFrame.getClass().getName());
                        addMenuItems();
                        System.out.println("✅ Gemini AI Side Panel Plugin installed successfully!");
                    } else {
                        System.err.println("❌ Main frame is null");
                    }
                } else {
                    System.err.println("❌ GuiPackage is null");
                }
            } catch (Exception e) {
                System.err.println("❌ Failed to install Gemini AI Side Panel Plugin: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Add menu items to JMeter's menu bar
     */
    private static void addMenuItems() {
        JMenuBar menuBar = mainFrame.getJMenuBar();
        if (menuBar == null) {
            return;
        }
        
        // Find or create Tools menu
        JMenu toolsMenu = findOrCreateMenu(menuBar, MENU_CATEGORY);
        
        // Add separator if the menu already has items
        if (toolsMenu.getMenuComponentCount() > 0) {
            toolsMenu.addSeparator();
        }
        
        // Create menu item to toggle the panel
        JMenuItem toggleMenuItem = new JMenuItem("🤖 " + MENU_ITEM_TEXT);
        toggleMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleGeminiPanel();
            }
        });
        
        toolsMenu.add(toggleMenuItem);
        
        // Add keyboard shortcut (Ctrl+Shift+G)
        toggleMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl shift G"));
        toggleMenuItem.setToolTipText("Toggle Gemini AI Assistant Panel (Ctrl+Shift+G)");
    }
    
    /**
     * Find existing menu or create new one
     */
    private static JMenu findOrCreateMenu(JMenuBar menuBar, String menuName) {
        // Check if menu already exists
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu != null && menuName.equals(menu.getText())) {
                return menu;
            }
        }
        
        // Create new menu
        JMenu newMenu = new JMenu(menuName);
        menuBar.add(newMenu);
        return newMenu;
    }
    
    /**
     * Toggle the visibility of the Gemini AI panel
     */
    private static void toggleGeminiPanel() {
        System.out.println("🔄 toggleGeminiPanel() called - current state: " + (isPanelVisible ? "visible" : "hidden"));
        if (isPanelVisible) {
            hideGeminiPanel();
        } else {
            showGeminiPanel();
        }
    }
    
    /**
     * Show the Gemini AI side panel
     */
    private static void showGeminiPanel() {
        if (sidePanel == null) {
            sidePanel = GeminiSidePanel.getInstance();
        }
        
        Container contentPane = mainFrame.getContentPane();
        if (contentPane instanceof JPanel) {
            JPanel mainPanel = (JPanel) contentPane;
            
            // Check if panel is already added
            Component[] components = mainPanel.getComponents();
            for (Component comp : components) {
                if (comp == sidePanel) {
                    sidePanel.setVisible(true);
                    isPanelVisible = true;
                    return;
                }
            }
            
            // Add panel to the right side
            mainPanel.add(sidePanel, BorderLayout.EAST);
            mainPanel.revalidate();
            mainPanel.repaint();
            isPanelVisible = true;
            
            System.out.println("Gemini AI Panel shown");
        }
    }
    
    /**
     * Hide the Gemini AI side panel
     */
    private static void hideGeminiPanel() {
        if (sidePanel != null) {
            sidePanel.setVisible(false);
            isPanelVisible = false;
            
            Container contentPane = mainFrame.getContentPane();
            if (contentPane instanceof JPanel) {
                JPanel mainPanel = (JPanel) contentPane;
                mainPanel.revalidate();
                mainPanel.repaint();
            }
            
            System.out.println("Gemini AI Panel hidden");
        }
    }
    
    /**
     * Check if the panel is currently visible
     */
    public static boolean isPanelVisible() {
        return isPanelVisible;
    }
    
    /**
     * Get the side panel instance
     */
    public static GeminiSidePanel getSidePanel() {
        return sidePanel;
    }
} 