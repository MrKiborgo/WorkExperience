package com.company;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;

/**
 * STS Configuration GUI - User interface for STS operations
 * 
 * This GUI provides a user-friendly interface for configuring STS operations
 * without requiring manual function syntax knowledge.
 */
public class STSConfigGui extends JPanel implements ActionListener, KeyListener {
    
    // GUI Components
    private JComboBox<String> actionCombo;
    private JTextField filenameField;
    private JTextArea variablesArea;
    private JTextArea valuesArea;
    private JLabel variablesLabel;
    private JLabel valuesLabel;
    private JPanel dynamicPanel;
    private JTextArea previewArea;
    private JLabel statusLabel;
    private JButton copyButton;
    private JButton testButton;
    
    // Action options
    private static final String[] ACTIONS = {"KEEP", "DEL", "ADDFIRST", "ADDLAST"};
    
    // Configuration model
    private STSConfigElement configElement;
    
    public STSConfigGui() {
        super();
        this.configElement = new STSConfigElement();
        init();
    }
    
    public STSConfigElement getConfigElement() {
        return configElement;
    }
    
    public void setConfigElement(STSConfigElement element) {
        this.configElement = element;
        updateFromConfig();
    }
    
    private void updateFromConfig() {
        if (configElement != null) {
            actionCombo.setSelectedItem(configElement.getAction());
            filenameField.setText(configElement.getFilename());
            variablesArea.setText(configElement.getVariables());
            valuesArea.setText(configElement.getValues());
            
            updateDynamicFields();
            updatePreview();
        }
    }
    
    private void updateConfigFromGui() {
        if (configElement != null) {
            configElement.setAction((String) actionCombo.getSelectedItem());
            configElement.setFilename(filenameField.getText());
            configElement.setVariables(variablesArea.getText());
            configElement.setValues(valuesArea.getText());
            
            // Generate and store the function
            String function = configElement.generateFunction();
            configElement.setGeneratedFunction(function);
        }
    }
    
    public void clearGui() {
        actionCombo.setSelectedIndex(0); // KEEP
        filenameField.setText("");
        variablesArea.setText("");
        valuesArea.setText("");
        configElement = new STSConfigElement();
        updateDynamicFields();
        updatePreview();
    }
    
    /**
     * Initialize the GUI components
     */
    private void init() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("STS Configuration"));
        add(createMainPanel(), BorderLayout.CENTER);
    }
    
    /**
     * Create the main panel with all components
     */
    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        // Add title (optional, remove if not needed)
        // mainPanel.add(createTitleComponent());
        
        // Add configuration section
        mainPanel.add(createConfigurationPanel());
        
        // Add dynamic fields section
        dynamicPanel = createDynamicPanel();
        mainPanel.add(dynamicPanel);
        
        // Add preview section
        mainPanel.add(createPreviewPanel());
        
        // Add action buttons
        mainPanel.add(createButtonPanel());
        
        return mainPanel;
    }
    
    /**
     * Create the configuration panel
     */
    private JPanel createConfigurationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Action selection
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Action:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        actionCombo = new JComboBox<>(ACTIONS);
        actionCombo.addActionListener(this);
        panel.add(actionCombo, gbc);
        
        // Filename field
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Filename:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        filenameField = new JTextField(20);
        filenameField.addKeyListener(this);
        panel.add(filenameField, gbc);
        
        return panel;
    }
    
    /**
     * Create the dynamic panel that changes based on action
     */
    private JPanel createDynamicPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Parameters"));
        
        // Variables section (for KEEP/DEL)
        JPanel variablesPanel = new JPanel(new BorderLayout());
        variablesLabel = new JLabel("Variable Names (comma-separated):");
        variablesArea = new JTextArea(3, 30);
        variablesArea.addKeyListener(this);
        variablesArea.setLineWrap(true);
        variablesArea.setWrapStyleWord(true);
        variablesPanel.add(variablesLabel, BorderLayout.NORTH);
        variablesPanel.add(new JScrollPane(variablesArea), BorderLayout.CENTER);
        
        // Values section (for ADDFIRST/ADDLAST)
        JPanel valuesPanel = new JPanel(new BorderLayout());
        valuesLabel = new JLabel("Values (comma-separated):");
        valuesArea = new JTextArea(3, 30);
        valuesArea.addKeyListener(this);
        valuesArea.setLineWrap(true);
        valuesArea.setWrapStyleWord(true);
        valuesPanel.add(valuesLabel, BorderLayout.NORTH);
        valuesPanel.add(new JScrollPane(valuesArea), BorderLayout.CENTER);
        
        // Create card layout to switch between panels
        JPanel cardPanel = new JPanel(new CardLayout());
        cardPanel.add(variablesPanel, "VARIABLES");
        cardPanel.add(valuesPanel, "VALUES");
        
        panel.add(cardPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create the preview panel
     */
    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Generated Function"));
        
        previewArea = new JTextArea(2, 50);
        previewArea.setEditable(false);
        previewArea.setBackground(panel.getBackground());
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        
        panel.add(new JScrollPane(previewArea), BorderLayout.CENTER);
        
        // Status label
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC));
        panel.add(statusLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Create the button panel
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        
        copyButton = new JButton("Copy Function");
        copyButton.addActionListener(this);
        copyButton.setToolTipText("Copy the generated function to clipboard");
        
        testButton = new JButton("Test Connection");
        testButton.addActionListener(this);
        testButton.setToolTipText("Test STS connection with current configuration");
        
        panel.add(copyButton);
        panel.add(testButton);
        
        return panel;
    }
    
    /**
     * Update the dynamic fields based on selected action
     */
    private void updateDynamicFields() {
        String action = (String) actionCombo.getSelectedItem();
        CardLayout cardLayout = (CardLayout) ((JPanel) dynamicPanel.getComponent(0)).getLayout();
        
        if ("KEEP".equals(action) || "DEL".equals(action)) {
            cardLayout.show((JPanel) dynamicPanel.getComponent(0), "VARIABLES");
            variablesLabel.setText("Variable Names (comma-separated) - for " + action + ":");
        } else {
            cardLayout.show((JPanel) dynamicPanel.getComponent(0), "VALUES");
            valuesLabel.setText("Values (comma-separated) - for " + action + ":");
        }
        
        updatePreview();
    }
    
    /**
     * Update the preview with the current configuration
     */
    private void updatePreview() {
        // Update the config element from GUI
        updateConfigFromGui();
        
        String function = configElement.generateFunction();
        previewArea.setText(function);
        
        // Update status
        STSConfigElement.ValidationResult validation = configElement.validate();
        if (validation.isValid()) {
            statusLabel.setText("✅ " + validation.getMessage());
            statusLabel.setForeground(new Color(0, 120, 0));
        } else {
            statusLabel.setText("❌ " + validation.getMessage());
            statusLabel.setForeground(Color.RED);
        }
        
        // Enable/disable buttons based on validation
        copyButton.setEnabled(validation.isValid());
        testButton.setEnabled(validation.isValid());
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == actionCombo) {
            updateDynamicFields();
        } else if (e.getSource() == copyButton) {
            copyFunctionToClipboard();
        } else if (e.getSource() == testButton) {
            testSTSConnection();
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // Update preview as user types
        SwingUtilities.invokeLater(this::updatePreview);
    }
    
    @Override
    public void keyPressed(KeyEvent e) {}
    
    @Override
    public void keyReleased(KeyEvent e) {}
    
    /**
     * Copy the generated function to clipboard
     */
    private void copyFunctionToClipboard() {
        String function = previewArea.getText();
        if (!function.isEmpty()) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(function), null);
            
            // Show temporary confirmation
            String originalText = statusLabel.getText();
            statusLabel.setText("📋 Function copied to clipboard!");
            
            // Reset after 2 seconds
            Timer timer = new Timer(2000, e -> statusLabel.setText(originalText));
            timer.setRepeats(false);
            timer.start();
        }
    }
    
    /**
     * Test STS connection with current configuration
     */
    private void testSTSConnection() {
        statusLabel.setText("🔄 Testing STS connection...");
        statusLabel.setForeground(Color.BLUE);
        
        // This would normally run in background thread, but for simplicity:
        SwingUtilities.invokeLater(() -> {
            try {
                updateConfigFromGui();
                
                // For now, just validate configuration
                STSConfigElement.ValidationResult result = configElement.validate();
                if (result.isValid()) {
                    statusLabel.setText("✅ Configuration is valid for STS testing");
                    statusLabel.setForeground(new Color(0, 120, 0));
                } else {
                    statusLabel.setText("❌ " + result.getMessage());
                    statusLabel.setForeground(Color.RED);
                }
            } catch (Exception ex) {
                statusLabel.setText("❌ Test failed: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
            }
        });
    }
} 