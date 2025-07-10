package com.company;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.MainFrame;
import org.apache.jmeter.services.FileServer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gemini AI Side Panel for JMeter
 * A dockable side panel that provides AI assistance directly in JMeter's main interface
 */
public class GeminiSidePanel extends JPanel {
    
    private static final String GEMINI_COMMAND = "gemini";
    private static final Color CHAT_BG = new Color(248, 249, 250);
    private static final Color USER_MSG_COLOR = new Color(0, 102, 204);
    private static final Color AI_MSG_COLOR = new Color(34, 139, 34);
    private static final Color PANEL_BG = new Color(245, 245, 245);
    
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton clearButton;
    private JCheckBox includeJmxCheckbox;
    private JLabel statusLabel;
    private ExecutorService executorService;
    private volatile boolean isGeminiReady = false;
    private boolean isCollapsed = false;
    
    // Panel management
    private static GeminiSidePanel instance;
    private JButton toggleButton;
    private int expandedWidth = 400;
    private int collapsedWidth = 30;
    
    public GeminiSidePanel() {
        executorService = Executors.newSingleThreadExecutor();
        initializeComponents();
        initializeGemini();
        instance = this;
    }
    
    public static GeminiSidePanel getInstance() {
        if (instance == null) {
            instance = new GeminiSidePanel();
        }
        return instance;
    }
    
    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(PANEL_BG);
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setPreferredSize(new Dimension(expandedWidth, 600));
        
        // Header with toggle and title
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Main content (chat area)
        add(createMainPanel(), BorderLayout.CENTER);
        
        // Input area
        add(createInputPanel(), BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(230, 230, 230));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        
        // Title
        JLabel titleLabel = new JLabel("🤖 Gemini AI");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        header.add(titleLabel, BorderLayout.CENTER);
        
        // Toggle button for collapse/expand
        toggleButton = new JButton("◀");
        toggleButton.setPreferredSize(new Dimension(25, 25));
        toggleButton.setToolTipText("Collapse/Expand Panel");
        toggleButton.addActionListener(e -> togglePanel());
        header.add(toggleButton, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Help text
        JTextArea helpText = new JTextArea(2, 30);
        helpText.setText("Ask me about JMeter, performance testing, load testing strategies, or script optimization!");
        helpText.setEditable(false);
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        helpText.setBackground(new Color(240, 248, 255));
        helpText.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        helpText.setFont(helpText.getFont().deriveFont(Font.ITALIC, 11f));
        mainPanel.add(helpText, BorderLayout.NORTH);
        
        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(CHAT_BG);
        chatArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setMargin(new Insets(10, 10, 10, 10));
        
        // Auto-scroll
        DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
        
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        statusPanel.setBackground(PANEL_BG);
        statusLabel = new JLabel("Initializing...");
        statusLabel.setFont(statusLabel.getFont().deriveFont(10f));
        statusPanel.add(statusLabel);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        
        // Input field
        inputField = new JTextField();
        inputField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown()) {
                    sendMessage();
                }
            }
        });
        
        // Controls
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        controlsPanel.setBackground(PANEL_BG);
        
        includeJmxCheckbox = new JCheckBox("Include JMX", true);
        includeJmxCheckbox.setFont(includeJmxCheckbox.getFont().deriveFont(10f));
        includeJmxCheckbox.setBackground(PANEL_BG);
        includeJmxCheckbox.setToolTipText("Include current test plan context");
        
        clearButton = new JButton("Clear");
        clearButton.setFont(clearButton.getFont().deriveFont(10f));
        clearButton.addActionListener(e -> clearChat());
        
        sendButton = new JButton("Send");
        sendButton.setFont(sendButton.getFont().deriveFont(10f));
        sendButton.addActionListener(e -> sendMessage());
        sendButton.setEnabled(false);
        
        controlsPanel.add(includeJmxCheckbox);
        controlsPanel.add(clearButton);
        controlsPanel.add(sendButton);
        
        inputPanel.add(new JLabel("Message (Ctrl+Enter to send):"), BorderLayout.NORTH);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(controlsPanel, BorderLayout.SOUTH);
        
        return inputPanel;
    }
    
    private void togglePanel() {
        isCollapsed = !isCollapsed;
        
        if (isCollapsed) {
            setPreferredSize(new Dimension(collapsedWidth, getHeight()));
            toggleButton.setText("▶");
            toggleButton.setToolTipText("Expand Panel");
            // Hide all components except header
            Component[] components = getComponents();
            for (Component comp : components) {
                if (comp != getComponent(0)) { // Keep header visible
                    comp.setVisible(false);
                }
            }
        } else {
            setPreferredSize(new Dimension(expandedWidth, getHeight()));
            toggleButton.setText("◀");
            toggleButton.setToolTipText("Collapse Panel");
            // Show all components
            Component[] components = getComponents();
            for (Component comp : components) {
                comp.setVisible(true);
            }
        }
        
        revalidate();
        repaint();
        
        // Notify parent container to update layout
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }
    
    private void initializeGemini() {
        executorService.submit(() -> {
            try {
                updateStatus("Checking Gemini CLI...", Color.ORANGE);
                
                ProcessBuilder checkBuilder = new ProcessBuilder(GEMINI_COMMAND, "--version");
                Process checkProcess = checkBuilder.start();
                int exitCode = checkProcess.waitFor();
                
                if (exitCode != 0) {
                    updateStatus("Gemini CLI not found", Color.RED);
                    SwingUtilities.invokeLater(() -> {
                        appendToChatArea("❌ Gemini CLI not found. Please ensure it's installed and in your PATH.\n", Color.RED);
                    });
                    return;
                }
                
                updateStatus("Ready!", Color.GREEN);
                SwingUtilities.invokeLater(() -> {
                    sendButton.setEnabled(true);
                    inputField.setEnabled(true);
                    appendToChatArea("🤖 Gemini AI Assistant ready!\n\nI can help you with:\n• JMeter test plan design\n• Performance testing strategies\n• Script optimization\n• Troubleshooting\n\nWhat can I help you with today?\n\n", AI_MSG_COLOR);
                });
                isGeminiReady = true;
                
            } catch (Exception e) {
                updateStatus("Error: " + e.getMessage(), Color.RED);
                SwingUtilities.invokeLater(() -> {
                    appendToChatArea("❌ Error initializing Gemini: " + e.getMessage() + "\n", Color.RED);
                });
            }
        });
    }
    
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty() || !isGeminiReady) {
            return;
        }
        
        appendToChatArea("👤 You: " + message + "\n", USER_MSG_COLOR);
        inputField.setText("");
        sendButton.setEnabled(false);
        
        // Prepare context if requested
        String contextualMessage = message;
        if (includeJmxCheckbox.isSelected()) {
            String jmxContext = getJmxContext();
            if (!jmxContext.isEmpty()) {
                contextualMessage = "JMeter Context:\n" + jmxContext + "\n\nUser Question: " + message;
            }
        }
        
        final String finalMessage = contextualMessage;
        executorService.submit(() -> {
            try {
                updateStatus("Asking Gemini...", Color.ORANGE);
                
                ProcessBuilder builder = new ProcessBuilder(GEMINI_COMMAND, "-p", finalMessage);
                builder.redirectErrorStream(true);
                Process process = builder.start();
                
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    SwingUtilities.invokeLater(() -> {
                        appendToChatArea("🤖 Gemini: " + response.toString() + "\n", AI_MSG_COLOR);
                        updateStatus("Ready!", Color.GREEN);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        appendToChatArea("❌ Error getting response from Gemini\n", Color.RED);
                        updateStatus("Error", Color.RED);
                    });
                }
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendToChatArea("❌ Error: " + e.getMessage() + "\n", Color.RED);
                    updateStatus("Error", Color.RED);
                });
            } finally {
                SwingUtilities.invokeLater(() -> sendButton.setEnabled(true));
            }
        });
    }
    
    private void clearChat() {
        chatArea.setText("");
        appendToChatArea("Chat cleared. How can I help you?\n\n", Color.GRAY);
    }
    
    private String getJmxContext() {
        StringBuilder context = new StringBuilder();
        
        try {
            GuiPackage guiPackage = GuiPackage.getInstance();
            if (guiPackage != null) {
                // Get current test plan name
                String testPlanName = guiPackage.getTestPlanFile();
                if (testPlanName != null) {
                    context.append("Current Test Plan: ").append(testPlanName).append("\n");
                }
                
                // Get base directory
                String baseDir = FileServer.getFileServer().getBaseDir();
                if (baseDir != null && !baseDir.isEmpty()) {
                    context.append("Base Directory: ").append(baseDir).append("\n");
                }
                
                context.append("JMeter GUI Mode: Active\n");
                context.append("Available for: Performance testing assistance\n");
            }
            
        } catch (Exception e) {
            context.append("JMeter context not available: ").append(e.getMessage()).append("\n");
        }
        
        return context.toString();
    }
    
    private void appendToChatArea(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(text);
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
    
    private void updateStatus(String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
            statusLabel.setForeground(color);
        });
    }
    
    /**
     * Cleanup method to properly shutdown resources
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
} 