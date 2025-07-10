import com.company.STSConfigGui;
import com.company.STSConfigElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Demo application to showcase the STS Configuration GUI
 */
public class STSGuiDemo {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }
    
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("STS Configuration GUI - Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        
        // Create the STS GUI
        STSConfigGui stsGui = new STSConfigGui();
        
        // Create a panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton exampleKEEPButton = new JButton("Example: KEEP");
        exampleKEEPButton.addActionListener(e -> {
            STSConfigElement example = new STSConfigElement();
            example.setAction("KEEP");
            example.setFilename("applications.csv");
            example.setVariables("APP_ID,Passport_Number,Name");
            stsGui.setConfigElement(example);
        });
        
        JButton exampleADDButton = new JButton("Example: ADDFIRST");
        exampleADDButton.addActionListener(e -> {
            STSConfigElement example = new STSConfigElement();
            example.setAction("ADDFIRST");
            example.setFilename("users.csv");
            example.setValues("12345,A1234567,John Doe");
            stsGui.setConfigElement(example);
        });
        
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> stsGui.clearGui());
        
        JButton showFunctionButton = new JButton("Show Generated Function");
        showFunctionButton.addActionListener(e -> {
            STSConfigElement config = stsGui.getConfigElement();
            String function = config.generateFunction();
            JOptionPane.showMessageDialog(frame, 
                "Generated Function:\n\n" + function,
                "STS Function Output",
                JOptionPane.INFORMATION_MESSAGE);
        });
        
        buttonPanel.add(exampleKEEPButton);
        buttonPanel.add(exampleADDButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(showFunctionButton);
        
        // Layout
        frame.setLayout(new BorderLayout());
        frame.add(stsGui, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add instructions
        JTextArea instructions = new JTextArea(3, 50);
        instructions.setText(
            "🎯 STS Configuration GUI Demo\n" +
            "• Use the dropdown to select an action (KEEP, DEL, ADDFIRST, ADDLAST)\n" +
            "• Enter a filename and parameters - watch the live preview update!\n" +
            "• Try the example buttons to see different configurations"
        );
        instructions.setEditable(false);
        instructions.setBackground(frame.getBackground());
        instructions.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.add(instructions, BorderLayout.NORTH);
        
        frame.setVisible(true);
        
        System.out.println("STS GUI Demo started!");
        System.out.println("This demonstrates the user-friendly interface for STS operations.");
        System.out.println("No more manual function syntax required!");
    }
} 