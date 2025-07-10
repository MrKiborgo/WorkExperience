package com.company;

import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.services.FileServer;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.gui.util.HorizontalPanel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.Collection;
import java.util.Collections;
import org.apache.jmeter.gui.GUIMenuSortOrder;
import org.apache.jmeter.gui.util.MenuFactory;

/**
 * JMeter GUI for STS Configuration with table-based interface
 * Similar to User Defined Variables element
 */
public class STSConfigJMeterGui extends AbstractConfigGui {
    
    private static final long serialVersionUID = 1L;
    
    private static final String FILENAME_COLUMN = "CSV Filename";
    private static final String ACTION_COLUMN = "Action";
    private static final String VARIABLES_COLUMN = "Variables/Values";
    private static final String COMMENT_COLUMN = "Comment";
    
    private JTable stsTable;
    private STSTableModel tableModel;
    private List<String> availableFiles = new ArrayList<>();
    private long lastRefreshTime = 0;
    private static final long REFRESH_CACHE_MS = 2000; // Cache refresh for 2 seconds
    
    public STSConfigJMeterGui() {
        super();
        // Initialize with specific loading message and trigger initial refresh
        availableFiles.add(""); // Empty option for manual entry
        availableFiles.add("(Loading CSV files from data folder...)");
        init();
        // Actually attempt to load files after GUI is initialized
        SwingUtilities.invokeLater(() -> {
            performInitialRefresh();
        });
    }
    
    @Override
    public String getStaticLabel() {
        return "-> STS";
    }
    
    /**
     * Load the STS icon for the tree view
     */
    public ImageIcon getIcon() {
        try {
            // Use JMeter's standard icon loading approach
            InputStream iconStream = getClass().getResourceAsStream("/org/apache/jmeter/images/tree/24x24/sts.png");
            if (iconStream != null) {
                try {
                    byte[] iconBytes = iconStream.readAllBytes();
                    if (iconBytes.length > 0) {
                        ImageIcon icon = new ImageIcon(iconBytes);
                        // Additional check to ensure icon has valid dimensions
                        if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                            System.out.println("STS: Successfully loaded custom icon (" + 
                                             icon.getIconWidth() + "x" + icon.getIconHeight() + ")");
                            return icon;
                        }
                    }
                } finally {
                    iconStream.close();
                }
            }
            // Try the 19x19 version as fallback
            iconStream = getClass().getResourceAsStream("/org/apache/jmeter/images/tree/19x19/sts.png");
            if (iconStream != null) {
                try {
                    byte[] iconBytes = iconStream.readAllBytes();
                    if (iconBytes.length > 0) {
                        ImageIcon icon = new ImageIcon(iconBytes);
                        if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                            System.out.println("STS: Successfully loaded 19x19 custom icon (" + 
                                             icon.getIconWidth() + "x" + icon.getIconHeight() + ")");
                            return icon;
                        }
                    }
                } finally {
                    iconStream.close();
                }
            }
            // Debug: Log that icon loading failed
            System.out.println("STS: Could not load custom icon from standard JMeter paths, using default");
        } catch (Exception e) {
            // Debug: Log the error
            System.out.println("STS: Error loading custom icon: " + e.getMessage());
        }
        return null; // Return null to use default icon
    }
    
    @Override
    public TestElement createTestElement() {
        STSConfigJMeter element = new STSConfigJMeter();
        modifyTestElement(element);
        return element;
    }
    
    @Override
    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
        if (element instanceof STSConfigJMeter) {
            STSConfigJMeter stsElement = (STSConfigJMeter) element;
            // Store table data as a combined string for now (we'll improve the data model later)
            stsElement.setOperations(tableModel.getOperationsAsString());
        }
    }
    
    @Override
    public void configure(TestElement element) {
        super.configure(element);
        if (element instanceof STSConfigJMeter) {
            STSConfigJMeter stsElement = (STSConfigJMeter) element;
            tableModel.setOperationsFromString(stsElement.getOperations());
        }
    }
    
    @Override
    public void clearGui() {
        super.clearGui();
        tableModel.clearData();
    }
    
    @Override
    public String getLabelResource() {
        return "sts_config";
    }
    
    /**
     * Define the menu categories this component appears under
     * Uses CONFIG_ELEMENTS with sort order to appear first in the menu
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
        
        // Table panel in the center
        mainPanel.add(createTablePanel(), BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Don't refresh file list immediately - do it lazily when needed
        // refreshFileList();
    }
    
    private JPanel createHelpPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Help"));
        
        JTextArea helpArea = new JTextArea(3, 50);
        helpArea.setEditable(false);
        helpArea.setBackground(getBackground());
        helpArea.setFont(helpArea.getFont().deriveFont(Font.ITALIC));
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        helpArea.setText(
            "Configure multiple STS operations in a single element. " +
            "KEEP/DEL: Read data (Variables = column names). " +
            "ADDFIRST/ADDLAST: Add data (Variables/Values = data to add). " +
            "Filenames are automatically prefixed with repository name. " +
            "Click 'Refresh CSV List' if files don't appear in dropdown."
        );
        
        panel.add(helpArea, BorderLayout.CENTER);
        
        // Add refresh button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Refresh CSV List");
        refreshButton.setToolTipText("Refresh the CSV file list from the test plan's data directory");
        refreshButton.addActionListener(e -> {
            refreshFileList();
            java.io.File dataDir = findDataDirectory();
            String message;
            if (dataDir != null) {
                message = "CSV file list refreshed from data folder:\n" + dataDir.getAbsolutePath();
            } else {
                message = "No data folder found.\nCurrent base directory: " + FileServer.getFileServer().getBaseDir();
            }
            JOptionPane.showMessageDialog(this, message, "File List Refreshed", JOptionPane.INFORMATION_MESSAGE);
        });
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("STS Operations"));
        
        // Create table model and table
        tableModel = new STSTableModel();
        stsTable = new JTable(tableModel);
        
        // Configure table
        stsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stsTable.setColumnSelectionAllowed(false);
        stsTable.setRowSelectionAllowed(true);
        
        // Set up custom cell editors
        setupTableColumns();
        
        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(stsTable);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add buttons for table management
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            tableModel.addRow();
            int newRow = tableModel.getRowCount() - 1;
            stsTable.setRowSelectionInterval(newRow, newRow);
        });
        buttonPanel.add(addButton);
        
        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> {
            int selectedRow = stsTable.getSelectedRow();
            if (selectedRow >= 0) {
                tableModel.deleteRow(selectedRow);
            }
        });
        buttonPanel.add(deleteButton);
        
        JButton refreshButton = new JButton("Refresh Files");
        refreshButton.addActionListener(e -> {
            refreshFileList();
            tableModel.fireTableDataChanged(); // Refresh dropdown contents
        });
        buttonPanel.add(refreshButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void setupTableColumns() {
        // Filename column with dropdown
        TableColumn filenameColumn = stsTable.getColumnModel().getColumn(0);
        filenameColumn.setPreferredWidth(200);
        JComboBox<String> filenameEditor = createLazyComboBox();
        filenameColumn.setCellEditor(new DefaultCellEditor(filenameEditor));
        
        // Action column with dropdown
        TableColumn actionColumn = stsTable.getColumnModel().getColumn(1);
        actionColumn.setPreferredWidth(100);
        JComboBox<String> actionEditor = new JComboBox<>(new String[]{"KEEP", "DEL", "ADDFIRST", "ADDLAST"});
        actionColumn.setCellEditor(new DefaultCellEditor(actionEditor));
        
        // Variables/Values column
        TableColumn variablesColumn = stsTable.getColumnModel().getColumn(2);
        variablesColumn.setPreferredWidth(300);
        
        // Comment column
        TableColumn commentColumn = stsTable.getColumnModel().getColumn(3);
        commentColumn.setPreferredWidth(150);
    }
    
    /**
     * Perform initial refresh of file list and update the dropdown
     * This replaces the loading message with actual files or a more specific message
     */
    private void performInitialRefresh() {
        refreshFileListQuietly();
        // Update any existing dropdowns with the new file list
        if (stsTable != null) {
            TableColumn filenameColumn = stsTable.getColumnModel().getColumn(0);
            JComboBox<String> filenameEditor = createLazyComboBox();
            filenameColumn.setCellEditor(new DefaultCellEditor(filenameEditor));
        }
    }
    
    private void refreshFileList() {
        availableFiles.clear();
        availableFiles.add(""); // Empty option for manual entry
        
        try {
            java.io.File dataDir = findDataDirectory();
            if (dataDir != null && dataDir.exists() && dataDir.isDirectory()) {
                java.io.File[] files = dataDir.listFiles((dir, name) -> {
                    String lowerName = name.toLowerCase();
                    return lowerName.endsWith(".csv") || lowerName.endsWith(".txt");
                });
                
                if (files != null && files.length > 0) {
                    java.util.Arrays.sort(files, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
                    for (java.io.File file : files) {
                        availableFiles.add(file.getName());
                    }
                }
            }
        } catch (Exception e) {
            // Silently continue if file scanning fails
        }
        
        // Update filename column editor
        if (stsTable != null) {
            TableColumn filenameColumn = stsTable.getColumnModel().getColumn(0);
            JComboBox<String> filenameEditor = createLazyComboBox();
            filenameColumn.setCellEditor(new DefaultCellEditor(filenameEditor));
        }
    }
    
    /**
     * Create a ComboBox that refreshes its list when opened
     * This ensures we get the correct JMX directory even if the test plan was loaded after GUI init
     * Optimized with caching to prevent lag from frequent refreshes
     */
    private JComboBox<String> createLazyComboBox() {
        // Start with current available files (at least basic options)
        JComboBox<String> combo = new JComboBox<String>(availableFiles.toArray(new String[0])) {
            @Override
            public void setPopupVisible(boolean v) {
                if (v) {
                    // Only refresh if cache has expired (prevents lag from rapid clicking)
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastRefreshTime > REFRESH_CACHE_MS) {
                        // Refresh the file list when dropdown is opened
                        refreshFileListQuietly();
                        lastRefreshTime = currentTime;
                        
                        // Update the model with new files
                        removeAllItems();
                        for (String file : availableFiles) {
                            addItem(file);
                        }
                    }
                }
                super.setPopupVisible(v);
            }
        };
        combo.setEditable(true);
        
        // Add tooltip to explain the auto-refresh feature
        combo.setToolTipText("Automatically refreshes file list when opened. Files are scanned from the 'data' folder relative to your test plan.");
        
        return combo;
    }
    
    /**
     * Refresh file list without updating the GUI (for lazy loading)
     * Enhanced with better unsaved test plan handling
     */
    private void refreshFileListQuietly() {
        List<String> newFiles = new ArrayList<>();
        newFiles.add(""); // Empty option for manual entry
        
        boolean foundFiles = false;
        java.io.File dataDir = null;
        String jmxBaseDir = null;
        
        try {
            jmxBaseDir = FileServer.getFileServer().getBaseDir();
            
            // Check if test plan has been saved
            boolean testPlanSaved = (jmxBaseDir != null && !jmxBaseDir.trim().isEmpty() && 
                                   !jmxBaseDir.equals(".") && !jmxBaseDir.equals(""));
            
            if (!testPlanSaved) {
                // Add helpful message for unsaved test plans
                newFiles.add("⚠ Test plan not saved - save first to detect data folder");
                newFiles.add("📁 Browse for Data Folder...");
                
                // Still try fallback detection in current working directory
                String cwd = System.getProperty("user.dir");
                dataDir = new java.io.File(cwd, "data");
                if (dataDir.exists() && dataDir.isDirectory()) {
                    newFiles.add("ℹ Found data folder in: " + cwd);
                } else {
                    newFiles.add("ℹ No data folder found in: " + cwd);
                }
            } else {
                dataDir = findDataDirectory();
            }
            
            if (dataDir != null && dataDir.exists() && dataDir.isDirectory()) {
                java.io.File[] files = dataDir.listFiles((dir, name) -> {
                    String lowerName = name.toLowerCase();
                    return lowerName.endsWith(".csv") || lowerName.endsWith(".txt");
                });
                
                if (files != null && files.length > 0) {
                    java.util.Arrays.sort(files, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
                    
                    // Limit files to prevent dropdown lag with large directories
                    int maxFiles = Math.min(files.length, 50); // Show max 50 files
                    for (int i = 0; i < maxFiles; i++) {
                        newFiles.add(files[i].getName());
                        foundFiles = true;
                    }
                    
                    // Add indicator if there are more files
                    if (files.length > 50) {
                        newFiles.add("⚠ " + (files.length - 50) + " more files... (showing first 50)");
                    }
                }
            }
        } catch (Exception e) {
            newFiles.add("❌ Error scanning for files: " + e.getMessage());
        }
        
        // If no files found, add helpful message with specific path
        if (!foundFiles && dataDir != null) {
            if (dataDir.exists()) {
                newFiles.add("(No CSV files in: " + dataDir.getName() + ")");
            } else {
                newFiles.add("(Data folder not found: " + dataDir.getName() + ")");
            }
        }
        
        // Add additional help options
        if (!foundFiles) {
            newFiles.add("💡 Help: Create 'data' folder next to .jmx file");
            newFiles.add("🔄 Refresh CSV List");
        }
        
        availableFiles.clear();
        availableFiles.addAll(newFiles);
    }
    
    /**
     * Get current data directory path for display purposes
     */
    private String getCurrentDataDirectory() {
        java.io.File dataDir = findDataDirectory();
        if (dataDir != null) {
            return dataDir.getAbsolutePath();
        }
        return "Not found (JMX base: " + FileServer.getFileServer().getBaseDir() + ")";
    }
    
    /**
     * Find the data directory relative to the current test plan (JMX file location)
     */
    private java.io.File findDataDirectory() {
        try {
            String jmxBaseDir = FileServer.getFileServer().getBaseDir();
            
            // Debug logging to help diagnose path issues
            System.out.println("STS DEBUG: JMX Base Directory from FileServer: " + jmxBaseDir);
            System.out.println("STS DEBUG: Current working directory: " + System.getProperty("user.dir"));
            
            if (jmxBaseDir == null || jmxBaseDir.trim().isEmpty()) {
                System.out.println("STS DEBUG: JMX Base Directory is null/empty - test plan probably not saved yet");
                // Fallback: try current working directory for unsaved test plans
                jmxBaseDir = System.getProperty("user.dir");
                System.out.println("STS DEBUG: Using fallback directory: " + jmxBaseDir);
            }
            
            java.io.File baseDir = new java.io.File(jmxBaseDir);
            System.out.println("STS DEBUG: Resolved base directory: " + baseDir.getAbsolutePath());
            System.out.println("STS DEBUG: Base directory exists: " + baseDir.exists());
            
            java.io.File dataDir = new java.io.File(baseDir, "data");
            System.out.println("STS DEBUG: Looking for data directory at: " + dataDir.getAbsolutePath());
            System.out.println("STS DEBUG: Data directory exists: " + dataDir.exists());
            
            if (dataDir.exists() && dataDir.isDirectory()) {
                System.out.println("STS DEBUG: Found data directory: " + dataDir.getAbsolutePath());
                return dataDir;
            }
            
            java.io.File parentDataDir = new java.io.File(baseDir.getParentFile(), "data");
            System.out.println("STS DEBUG: Trying parent data directory: " + parentDataDir.getAbsolutePath());
            System.out.println("STS DEBUG: Parent data directory exists: " + parentDataDir.exists());
            
            if (parentDataDir.exists() && parentDataDir.isDirectory()) {
                System.out.println("STS DEBUG: Found parent data directory: " + parentDataDir.getAbsolutePath());
                return parentDataDir;
            }
            
            // Fallback paths - all relative to JMX directory, not installation directory
            String[] fallbackPaths = {
                new java.io.File(baseDir, "data").getAbsolutePath(),
                new java.io.File(baseDir, "../data").getAbsolutePath(),
                new java.io.File(baseDir, "../../data").getAbsolutePath()
            };
            
            System.out.println("STS DEBUG: Trying fallback paths:");
            for (String path : fallbackPaths) {
                java.io.File dir = new java.io.File(path);
                System.out.println("STS DEBUG: Checking fallback: " + dir.getAbsolutePath() + " (exists: " + dir.exists() + ")");
                if (dir.exists() && dir.isDirectory()) {
                    System.out.println("STS DEBUG: Found fallback data directory: " + dir.getAbsolutePath());
                    return dir;
                }
            }
            
            System.out.println("STS DEBUG: No data directory found anywhere");
            return null;
        } catch (Exception e) {
            System.out.println("STS DEBUG: Exception in findDataDirectory: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Table model for STS operations
     */
    private class STSTableModel extends AbstractTableModel {
        private final String[] columnNames = {FILENAME_COLUMN, ACTION_COLUMN, VARIABLES_COLUMN, COMMENT_COLUMN};
        private Vector<Vector<String>> data = new Vector<>();
        
        public STSTableModel() {
            // Start with one empty row
            addRow();
        }
        
        @Override
        public int getRowCount() {
            return data.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < data.size() && columnIndex < columnNames.length) {
                Vector<String> row = data.get(rowIndex);
                if (columnIndex < row.size()) {
                    return row.get(columnIndex);
                }
            }
            return "";
        }
        
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (rowIndex < data.size() && columnIndex < columnNames.length) {
                Vector<String> row = data.get(rowIndex);
                while (row.size() <= columnIndex) {
                    row.add("");
                }
                row.set(columnIndex, aValue != null ? aValue.toString() : "");
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }
        
        public void addRow() {
            Vector<String> newRow = new Vector<>();
            newRow.add(""); // Filename
            newRow.add("KEEP"); // Action
            newRow.add(""); // Variables/Values
            newRow.add(""); // Comment
            data.add(newRow);
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }
        
        public void deleteRow(int rowIndex) {
            if (rowIndex >= 0 && rowIndex < data.size()) {
                data.remove(rowIndex);
                fireTableRowsDeleted(rowIndex, rowIndex);
            }
        }
        
        public void clearData() {
            data.clear();
            addRow(); // Always have at least one row
            fireTableDataChanged();
        }
        
        public String getOperationsAsString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.size(); i++) {
                Vector<String> row = data.get(i);
                if (row.size() >= 3 && !row.get(0).trim().isEmpty() && !row.get(2).trim().isEmpty()) {
                    if (sb.length() > 0) sb.append("|");
                    // Use semicolon as separator to avoid conflict with commas in Variables/Values field
                    sb.append(row.get(0)).append(";").append(row.get(1)).append(";").append(row.get(2));
                    if (row.size() > 3 && !row.get(3).trim().isEmpty()) {
                        sb.append(";").append(row.get(3));
                    }
                }
            }
            return sb.toString();
        }
        
        public void setOperationsFromString(String operations) {
            data.clear();
            if (operations != null && !operations.trim().isEmpty()) {
                String[] ops = operations.split("\\|");
                for (String op : ops) {
                    String[] parts;
                    // Check if this is new semicolon format or legacy comma format
                    if (op.contains(";")) {
                        // New format: use semicolon as separator (avoids conflict with commas in Variables/Values)
                        parts = op.split(";", 4);
                    } else {
                        // Legacy format: use comma as separator (for backward compatibility)
                        parts = op.split(",", 4);
                    }
                    
                    if (parts.length >= 3) {
                        Vector<String> row = new Vector<>();
                        row.add(parts[0]); // Filename
                        row.add(parts[1]); // Action
                        row.add(parts[2]); // Variables/Values (can contain commas in new format)
                        row.add(parts.length > 3 ? parts[3] : ""); // Comment
                        data.add(row);
                    }
                }
            }
            if (data.isEmpty()) {
                addRow(); // Always have at least one row
            }
            fireTableDataChanged();
        }
    }
} 