package com.company;

import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.gui.util.HorizontalPanel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import org.apache.jmeter.gui.GUIMenuSortOrder;
import org.apache.jmeter.gui.util.MenuFactory;

/**
 * JMeter GUI for Pacing Configuration
 * Provides a clean interface for configuring thread pacing instead of using JSR223 Parameters
 */
public class PacingConfigJMeterGui extends AbstractConfigGui {
    
    private static final long serialVersionUID = 1L;
    
    private JTextField minPacingField;
    private JTextField maxPacingField;
    private JCheckBox enabledCheckBox;
    private JTextArea previewArea;
    
    public PacingConfigJMeterGui() {
        super();
        init();
    }
    
    @Override
    public String getStaticLabel() {
        return "-> Pacing";
    }
    
    /**
     * Load the pacing icon for the tree view (reuse STS icon for now)
     */
    public ImageIcon getIcon() {
        try {
            // Use the same icon structure as STS for now
            InputStream iconStream = getClass().getResourceAsStream("/org/apache/jmeter/images/tree/24x24/sts.png");
            if (iconStream != null) {
                try {
                    byte[] iconBytes = iconStream.readAllBytes();
                    if (iconBytes.length > 0) {
                        ImageIcon icon = new ImageIcon(iconBytes);
                        if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                            return icon;
                        }
                    }
                } finally {
                    iconStream.close();
                }
            }
        } catch (Exception e) {
            // Silently fall back to default icon
        }
        return null; // Return null to use default icon
    }
    
    @Override
    public TestElement createTestElement() {
        PacingConfigJMeter element = new PacingConfigJMeter();
        modifyTestElement(element);
        return element;
    }
    
    @Override
    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
        if (element instanceof PacingConfigJMeter) {
            PacingConfigJMeter pacingElement = (PacingConfigJMeter) element;
            pacingElement.setMinPacing(minPacingField.getText().trim());
            pacingElement.setMaxPacing(maxPacingField.getText().trim());
            pacingElement.setPacingEnabled(enabledCheckBox.isSelected());
        }
    }
    
    @Override
    public void configure(TestElement element) {
        super.configure(element);
        if (element instanceof PacingConfigJMeter) {
            PacingConfigJMeter pacingElement = (PacingConfigJMeter) element;
            minPacingField.setText(pacingElement.getMinPacing());
            maxPacingField.setText(pacingElement.getMaxPacing());
            enabledCheckBox.setSelected(pacingElement.isPacingEnabled());
            updatePreview();
        }
    }
    
    @Override
    public void clearGui() {
        super.clearGui();
        minPacingField.setText("");
        maxPacingField.setText("");
        enabledCheckBox.setSelected(true);
        updatePreview();
    }
    
    @Override
    public String getLabelResource() {
        return "pacing_config";
    }
    
    /**
     * Define the menu categories this component appears under
     * Uses CONFIG_ELEMENTS but with sort order 1 to appear first in the menu
     */
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
        
        // Configuration panel in the center
        mainPanel.add(createConfigPanel(), BorderLayout.CENTER);
        
        // Preview panel at the bottom
        mainPanel.add(createPreviewPanel(), BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Initial preview update
        updatePreview();
    }
    
    private JPanel createHelpPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Help"));
        
        JTextArea helpArea = new JTextArea(4, 50);
        helpArea.setEditable(false);
        helpArea.setBackground(getBackground());
        helpArea.setFont(helpArea.getFont().deriveFont(Font.ITALIC));
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        helpArea.setText(
            "Configure pacing (think time) between iterations. " +
            "Min Pacing (required): Fixed pacing if Max is empty, or minimum for random range. " +
            "Max Pacing (optional): If specified, pacing will be randomly selected between Min and Max (inclusive). " +
            "This replaces the need for JSR223 sampler Parameters and group_init.groovy pacing logic."
        );
        
        panel.add(helpArea, BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createConfigPanel() {
        JPanel panel = new VerticalPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Pacing Configuration"));
        
        // Enabled checkbox
        enabledCheckBox = new JCheckBox("Enable Pacing", true);
        enabledCheckBox.addActionListener(e -> {
            boolean enabled = enabledCheckBox.isSelected();
            minPacingField.setEnabled(enabled);
            maxPacingField.setEnabled(enabled);
            updatePreview();
        });
        panel.add(enabledCheckBox);
        
        // Min pacing field
        JPanel minPanel = new HorizontalPanel();
        minPanel.add(new JLabel("Min Pacing (seconds): "));
        minPacingField = new JTextField(10);
        minPacingField.setToolTipText("Minimum pacing in seconds (required). If Max is empty, this will be the fixed pacing.");
        minPacingField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updatePreview(); }
            public void removeUpdate(DocumentEvent e) { updatePreview(); }
            public void changedUpdate(DocumentEvent e) { updatePreview(); }
        });
        minPanel.add(minPacingField);
        panel.add(minPanel);
        
        // Max pacing field
        JPanel maxPanel = new HorizontalPanel();
        maxPanel.add(new JLabel("Max Pacing (seconds): "));
        maxPacingField = new JTextField(10);
        maxPacingField.setToolTipText("Maximum pacing in seconds (optional). If specified, pacing will be random between Min and Max.");
        maxPacingField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updatePreview(); }
            public void removeUpdate(DocumentEvent e) { updatePreview(); }
            public void changedUpdate(DocumentEvent e) { updatePreview(); }
        });
        maxPanel.add(maxPacingField);
        panel.add(maxPanel);
        
        return panel;
    }
    
    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuration Preview"));
        
        previewArea = new JTextArea(3, 50);
        previewArea.setEditable(false);
        previewArea.setBackground(getBackground());
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(previewArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(500, 80));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    /**
     * Update the preview text based on current configuration
     */
    private void updatePreview() {
        if (previewArea == null) return;
        
        StringBuilder preview = new StringBuilder();
        
        if (!enabledCheckBox.isSelected()) {
            preview.append("Pacing is DISABLED - no think time will be applied between iterations");
        } else {
            String minStr = minPacingField.getText().trim();
            String maxStr = maxPacingField.getText().trim();
            
            if (minStr.isEmpty()) {
                preview.append("⚠ Min Pacing is required when pacing is enabled");
            } else {
                try {
                    int min = Integer.parseInt(minStr);
                    if (min <= 0) {
                        preview.append("⚠ Min Pacing must be greater than 0");
                    } else if (maxStr.isEmpty()) {
                        preview.append("Fixed pacing: exactly ").append(min).append(" seconds between each iteration");
                    } else {
                        try {
                            int max = Integer.parseInt(maxStr);
                            if (max <= 0) {
                                preview.append("⚠ Max Pacing must be greater than 0");
                            } else if (max < min) {
                                preview.append("⚠ Max Pacing must be >= Min Pacing");
                            } else if (max == min) {
                                preview.append("Fixed pacing: exactly ").append(min).append(" seconds between each iteration");
                            } else {
                                preview.append("Random pacing: between ").append(min).append(" and ").append(max)
                                       .append(" seconds (inclusive) - randomly selected each iteration");
                            }
                        } catch (NumberFormatException e) {
                            preview.append("⚠ Max Pacing must be a valid integer");
                        }
                    }
                } catch (NumberFormatException e) {
                    preview.append("⚠ Min Pacing must be a valid integer");
                }
            }
        }
        
        preview.append("\n\nThis element replaces the pacing functionality from group_init.groovy and JSR223 Parameters.");
        
        previewArea.setText(preview.toString());
    }
} 