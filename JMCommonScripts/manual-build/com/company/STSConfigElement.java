package com.company;

import org.apache.jmeter.threads.JMeterVariables;
import org.apache.log.Logger;
import org.apache.jorphan.logging.LoggingManager;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

/**
 * STS Configuration Element - Backend model for STS GUI
 * 
 * This class stores the configuration for STS operations and
 * can generate the appropriate STS function calls.
 */
public class STSConfigElement {
    
    private static final Logger log = LoggingManager.getLoggerForClass();
    
    // Configuration fields
    private String action = "KEEP";
    private String filename = "";
    private String variables = "";
    private String values = "";
    private String generatedFunction = "";
    
    // Default constructor
    public STSConfigElement() {
        // Set default values
        this.action = "KEEP";
        this.filename = "";
        this.variables = "";
        this.values = "";
    }
    
    // Getters and setters for action
    public String getAction() {
        return action != null ? action : "KEEP";
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    // Getters and setters for filename
    public String getFilename() {
        return filename != null ? filename : "";
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    // Getters and setters for variables (comma-separated)
    public String getVariables() {
        return variables != null ? variables : "";
    }
    
    public void setVariables(String variables) {
        this.variables = variables;
    }
    
    // Getters and setters for values (comma-separated)
    public String getValues() {
        return values != null ? values : "";
    }
    
    public void setValues(String values) {
        this.values = values;
    }
    
    // Generated function for preview
    public String getGeneratedFunction() {
        return generatedFunction != null ? generatedFunction : "";
    }
    
    public void setGeneratedFunction(String function) {
        this.generatedFunction = function;
    }
    
    /**
     * Generate the STS function call based on current configuration
     */
    public String generateFunction() {
        String action = getAction();
        String filename = getFilename().trim();
        
        if (filename.isEmpty()) {
            return "${__STS(/* Please enter a filename */)}";
        }
        
        StringBuilder function = new StringBuilder("${__STS(");
        function.append(action).append(",").append(filename);
        
        if ("KEEP".equals(action) || "DEL".equals(action)) {
            // Read operations use variables
            String variables = getVariables().trim();
            if (!variables.isEmpty()) {
                String[] varArray = variables.split(",");
                for (String var : varArray) {
                    function.append(",").append(var.trim());
                }
            }
        } else if ("ADDFIRST".equals(action) || "ADDLAST".equals(action)) {
            // Add operations use values
            String values = getValues().trim();
            if (!values.isEmpty()) {
                String[] valueArray = values.split(",");
                for (String value : valueArray) {
                    function.append(",").append(value.trim());
                }
            }
        }
        
        function.append(")}");
        String result = function.toString();
        setGeneratedFunction(result);
        return result;
    }
    
    /**
     * Execute the STS operation directly (for testing/validation)
     */
    public boolean executeSTS(JMeterVariables vars, Properties props) {
        String action = getAction();
        String filename = getFilename();
        
        if ("KEEP".equals(action) || "DEL".equals(action)) {
            String variables = getVariables();
            if (!variables.isEmpty()) {
                boolean keep = "KEEP".equals(action);
                return STS.exec(log, vars, props, 
                               action + "," + filename + "," + variables);
            }
        } else if ("ADDFIRST".equals(action) || "ADDLAST".equals(action)) {
            String values = getValues();
            if (!values.isEmpty()) {
                return STS.exec(log, vars, props,
                               action + "," + filename + "," + values);
            }
        }
        
        return false;
    }
    
    /**
     * Validate the current configuration
     */
    public ValidationResult validate() {
        String action = getAction();
        String filename = getFilename().trim();
        
        if (filename.isEmpty()) {
            return new ValidationResult(false, "Filename is required");
        }
        
        if ("KEEP".equals(action) || "DEL".equals(action)) {
            String variables = getVariables().trim();
            if (variables.isEmpty()) {
                return new ValidationResult(false, "At least one variable name is required for " + action + " operations");
            }
        } else if ("ADDFIRST".equals(action) || "ADDLAST".equals(action)) {
            String values = getValues().trim();
            if (values.isEmpty()) {
                return new ValidationResult(false, "At least one value is required for " + action + " operations");
            }
        }
        
        return new ValidationResult(true, "Configuration is valid");
    }
    
    /**
     * Simple validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
} 