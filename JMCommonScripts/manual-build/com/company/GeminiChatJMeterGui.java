package com.company;

import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.gui.util.HorizontalPanel;
import org.apache.jmeter.services.FileServer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.jmeter.gui.util.MenuFactory;

/**
 * JMeter GUI for Gemini CLI Integration
 * Provides a chat interface to interact with Gemini AI directly within JMeter
 */
public class GeminiChatJMeterGui extends AbstractConfigGui {
    
    private static final String GEMINI_COMMAND = "gemini";
    private static final Color CHAT_BG = new Color(248, 249, 250);
    private static final Color USER_MSG_COLOR = new Color(0, 102, 204);
    private static final Color AI_MSG_COLOR = new Color(34, 139, 34);
    
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton clearButton;
    private JCheckBox includeJmxCheckbox;
    private JLabel statusLabel;
    private ExecutorService executorService;
    private volatile Process geminiProcess;
    private volatile PrintWriter geminiInput;
    private volatile boolean isGeminiReady = false;
    
    public GeminiChatJMeterGui() {
        super();
        executorService = Executors.newSingleThreadExecutor();
        init();
        initializeGemini();
    }
    
    @Override
    public String getStaticLabel() {
        return "Gemini AI Assistant";
    }
    
    @Override
    public String getLabelResource() {
        return "gemini_chat";
    }
    
    @Override
    public TestElement createTestElement() {
        GeminiChatJMeter element = new GeminiChatJMeter();
        configureTestElement(element);
        return element;
    }
    
    @Override
    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
        if (element instanceof GeminiChatJMeter) {
            GeminiChatJMeter geminiElement = (GeminiChatJMeter) element;
            geminiElement.setIncludeJmxContext(includeJmxCheckbox.isSelected());
        }
    }
    
    @Override
    public void configure(TestElement element) {
        super.configure(element);
        if (element instanceof GeminiChatJMeter) {
            GeminiChatJMeter geminiElement = (GeminiChatJMeter) element;
            includeJmxCheckbox.setSelected(geminiElement.getIncludeJmxContext());
        }
    }
    
    @Override
    public void clearGui() {
        super.clearGui();
        chatArea.setText("");
        inputField.setText("");
        includeJmxCheckbox.setSelected(true);
        updateStatus("Cleared", Color.BLUE);
    }
    
    @Override
    public Collection<String> getMenuCategories() {
        return Collections.singletonList(MenuFactory.CONFIG_ELEMENTS);
    }
    
    private void init() {
        setLayout(new BorderLayout());
        setBorder(makeBorder());
        add(makeTitlePanel(), BorderLayout.NORTH);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Help panel at the top
        mainPanel.add(createHelpPanel(), BorderLayout.NORTH);
        
        // Chat panel in the center
        mainPanel.add(createChatPanel(), BorderLayout.CENTER);
        
        // Input panel at the bottom
        mainPanel.add(createInputPanel(), BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createHelpPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("AI Assistant Help"));
        
        JTextArea helpArea = new JTextArea(2, 50);
        helpArea.setEditable(false);
        helpArea.setBackground(getBackground());
        helpArea.setFont(helpArea.getFont().deriveFont(Font.ITALIC));
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        helpArea.setText(
            "Chat with Gemini AI to get help with JMeter test plans, performance testing strategies, " +
            "script optimization, and troubleshooting. Enable 'Include JMX Context' to share your current test plan with Gemini."
        );
        
        panel.add(helpArea, BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Chat with Gemini AI"));
        
        // Chat display area
        chatArea = new JTextArea(20, 80);
        chatArea.setEditable(false);
        chatArea.setBackground(CHAT_BG);
        chatArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        
        // Auto-scroll to bottom
        DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setPreferredSize(new Dimension(700, 400));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Starting Gemini CLI...");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC));
        statusPanel.add(new JLabel("Status: "));
        statusPanel.add(statusLabel);
        
        panel.add(statusPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Send Message"));
        
        // Input field
        inputField = new JTextField();
        inputField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    sendMessage();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isShiftDown()) {
                    // Allow multiline with Shift+Enter
                    inputField.setText(inputField.getText() + "\n");
                }
            }
        });
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        sendButton.setEnabled(false);
        
        clearButton = new JButton("Clear Chat");
        clearButton.addActionListener(e -> {
            chatArea.setText("");
            appendToChatArea("=== Chat Cleared ===\n", Color.GRAY);
        });
        
        includeJmxCheckbox = new JCheckBox("Include JMX Context", true);
        includeJmxCheckbox.setToolTipText("Include current JMX file information in the context sent to Gemini");
        
        buttonPanel.add(includeJmxCheckbox);
        buttonPanel.add(clearButton);
        buttonPanel.add(sendButton);
        
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void initializeGemini() {
        executorService.submit(() -> {
            try {
                updateStatus("Checking Gemini CLI availability...", Color.ORANGE);
                
                // Check if gemini command is available
                ProcessBuilder checkBuilder = new ProcessBuilder(GEMINI_COMMAND, "--version");
                Process checkProcess = checkBuilder.start();
                int exitCode = checkProcess.waitFor();
                
                if (exitCode != 0) {
                    updateStatus("Gemini CLI not found. Please install gemini-cli first.", Color.RED);
                    return;
                }
                
                updateStatus("Gemini CLI ready!", Color.GREEN);
                SwingUtilities.invokeLater(() -> {
                    sendButton.setEnabled(true);
                    inputField.setEnabled(true);
                    appendToChatArea("🤖 Gemini AI Assistant ready! How can I help you with JMeter today?\n\n", AI_MSG_COLOR);
                });
                isGeminiReady = true;
                
            } catch (Exception e) {
                updateStatus("Error initializing Gemini: " + e.getMessage(), Color.RED);
                SwingUtilities.invokeLater(() -> {
                    appendToChatArea("❌ Failed to initialize Gemini CLI: " + e.getMessage() + "\n", Color.RED);
                });
            }
        });
    }
    
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty() || !isGeminiReady) {
            return;
        }
        
        // Display user message
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
        
        // Send to Gemini
        final String finalMessage = contextualMessage;
        executorService.submit(() -> {
            try {
                updateStatus("Sending to Gemini...", Color.ORANGE);
                
                ProcessBuilder builder = new ProcessBuilder(GEMINI_COMMAND, "-p", finalMessage);
                builder.redirectErrorStream(true);
                Process process = builder.start();
                
                // Read response
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
                        updateStatus("Response received", Color.GREEN);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        appendToChatArea("❌ Error: Failed to get response from Gemini\n", Color.RED);
                        updateStatus("Error getting response", Color.RED);
                    });
                }
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendToChatArea("❌ Error communicating with Gemini: " + e.getMessage() + "\n", Color.RED);
                    updateStatus("Communication error", Color.RED);
                });
            } finally {
                SwingUtilities.invokeLater(() -> sendButton.setEnabled(true));
            }
        });
    }
    
    private String getJmxContext() {
        StringBuilder context = new StringBuilder();
        
        try {
            // Get current JMX file path
            String jmxFile = FileServer.getFileServer().getBaseDir();
            if (jmxFile != null && !jmxFile.isEmpty()) {
                context.append("Current JMX Base Directory: ").append(jmxFile).append("\n");
            }
            
            // Try to get test plan name if available
            // This would require access to GuiPackage which might not be available
            // in this context, so we'll keep it simple for now
            context.append("Working with JMeter Test Plan\n");
            context.append("Available for: Performance testing, Load testing, Script optimization, Troubleshooting\n");
            
        } catch (Exception e) {
            context.append("Could not retrieve JMX context: ").append(e.getMessage()).append("\n");
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
    
    @Override
    protected void finalize() throws Throwable {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (geminiProcess != null) {
            geminiProcess.destroy();
        }
        super.finalize();
    }
} 