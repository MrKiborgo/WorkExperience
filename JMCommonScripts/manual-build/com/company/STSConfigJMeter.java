package com.company;

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * JMeter-compatible STS Sampler
 * This integrates with JMeter's test plan and GUI system
 * Executes at the position where it's placed in the test plan tree!
 */
public class STSConfigJMeter extends AbstractSampler {
    
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggingManager.getLoggerForClass();
    private static volatile boolean globalSetupDone = false;
    
    // Property names for JMeter persistence
    public static final String STS_ACTION = "STSConfig.action";
    public static final String STS_FILENAME = "STSConfig.filename";
    public static final String STS_VARIABLES = "STSConfig.variables";
    public static final String STS_VALUES = "STSConfig.values";
    public static final String STS_OPERATIONS = "STSConfig.operations";
    
    public STSConfigJMeter() {
        super();
        setAction("KEEP");
        setFilename("");
        setVariables("");
        setValues("");
    }
    
    // Action property
    public void setAction(String action) {
        setProperty(new StringProperty(STS_ACTION, action));
    }
    
    public String getAction() {
        return getPropertyAsString(STS_ACTION, "KEEP");
    }
    
    // Filename property
    public void setFilename(String filename) {
        setProperty(new StringProperty(STS_FILENAME, filename));
    }
    
    public String getFilename() {
        return getPropertyAsString(STS_FILENAME, "");
    }
    
    // Variables property (for KEEP/DEL)
    public void setVariables(String variables) {
        setProperty(new StringProperty(STS_VARIABLES, variables));
    }
    
    public String getVariables() {
        return getPropertyAsString(STS_VARIABLES, "");
    }
    
        // Values property (for ADDFIRST/ADDLAST)
    public void setValues(String values) {
        setProperty(new StringProperty(STS_VALUES, values));
    }

    public String getValues() {
        return getPropertyAsString(STS_VALUES, "");
    }
    
    // Operations property (for table-based configuration)
    public void setOperations(String operations) {
        setProperty(new StringProperty(STS_OPERATIONS, operations));
    }

    public String getOperations() {
        return getPropertyAsString(STS_OPERATIONS, "");
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
        return function.toString();
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
     * AbstractSampler implementation - executes STS operations at this position in the test plan
     */
    @Override
    public SampleResult sample(Entry e) {
        log.debug("STSConfigJMeter.sample() called - executing at tree position");
        
        SampleResult result = new SampleResult();
        result.setSampleLabel(getName().isEmpty() ? "STS Configuration" : getName());
        result.sampleStart(); // Start timing
        
        try {
            executeSTSOperations();
            
            // Mark as successful
            result.setSuccessful(true);
            result.setResponseMessage("STS operations completed successfully");
            result.setResponseCode("200");
            result.setResponseData("STS operations executed", "UTF-8");
            
        } catch (Exception ex) {
            // Mark as failed
            result.setSuccessful(false);
            result.setResponseMessage("STS operations failed: " + ex.getMessage());
            result.setResponseCode("500");
            result.setResponseData("Error: " + ex.getMessage(), "UTF-8");
            log.error("STS operations failed", ex);
        }
        
        result.sampleEnd(); // End timing
        return result;
    }
    
    /**
     * Execute all STS operations from the table and set JMeter variables
     */
    private void executeSTSOperations() {
        try {
            // Ensure global setup has been executed
            ensureGlobalSetup();
            
            // Get current JMeter context
            JMeterVariables vars = JMeterContextService.getContext().getVariables();
            
            // Get operations from table (new format) or fallback to legacy single operation
            String operations = getOperations().trim();
            if (operations.isEmpty()) {
                // Fallback to legacy single operation format
                executeLegacySTS();
                return;
            }
            
            // Parse and execute multiple operations
            String[] operationList = operations.split("\\|");
            int successCount = 0;
            int totalCount = 0;
            StringBuilder lastCommand = new StringBuilder();
            
            for (String operation : operationList) {
                operation = operation.trim();
                if (operation.isEmpty()) continue;
                
                String[] parts;
                // Check if this is new semicolon format or legacy comma format
                if (operation.contains(";")) {
                    // New format: use semicolon as separator (avoids conflict with commas in Variables/Values)
                    parts = operation.split(";", 4);
                } else {
                    // Legacy format: use comma as separator (for backward compatibility)
                    parts = operation.split(",", 4);
                }
                
                if (parts.length < 3) continue;
                
                String filename = parts[0].trim();
                String action = parts[1].trim();
                String variablesValues = parts[2].trim(); // Can contain commas in new format
                // parts[3] would be comment - not used for execution
                
                if (filename.isEmpty() || variablesValues.isEmpty()) continue;
                
                // Resolve JMeter variables in the values/variables string before sending to STS
                String resolvedVariablesValues = resolveJMeterVariables(variablesValues, vars);
                
                // Build the STS command for this operation
                StringBuilder command = new StringBuilder();
                command.append(action).append(",").append(filename).append(",").append(resolvedVariablesValues);
                lastCommand = new StringBuilder(command);
                
                // Execute this STS operation
                boolean success = STS.exec(
                    log, // Logger
                    vars,
                    JMeterContextService.getContext().getProperties(),
                    command.toString()
                );
                
                totalCount++;
                if (success) {
                    successCount++;
                }
                
                // Log each operation
            }
            
            // Set overall result in JMeter variables
            boolean overallSuccess = (successCount == totalCount && totalCount > 0);
            vars.put("STS_SUCCESS", overallSuccess ? "true" : "false");
            vars.put("STS_OPERATIONS_TOTAL", String.valueOf(totalCount));
            vars.put("STS_OPERATIONS_SUCCESS", String.valueOf(successCount));
            vars.put("STS_OPERATIONS_FAILED", String.valueOf(totalCount - successCount));
            vars.put("STS_STATUS", overallSuccess ? "ALL_SUCCESS" : "SOME_FAILED");
            vars.put("STS_LAST_COMMAND", lastCommand.toString());
            
            // Log overall success
                
        } catch (Exception e) {
            // Set error variables
            JMeterVariables vars = JMeterContextService.getContext().getVariables();
            vars.put("STS_SUCCESS", "false");
            vars.put("STS_STATUS", "ERROR");
            vars.put("STS_ERROR", e.getMessage());
            
            // Log error
        }
    }
    
    /**
     * Ensure global setup has been executed (once per JMeter session)
     */
    private void ensureGlobalSetup() {
        if (!globalSetupDone) {
            synchronized (STSConfigJMeter.class) {
                if (!globalSetupDone) {
                    try {
                        log.info("STSConfigJMeter: Triggering global setup...");
                        com.company.jmeter.setup.GlobalSetupUtil.run(
                            log, 
                            JMeterContextService.getContext().getProperties(),
                            new java.util.HashMap<>()
                        );
                        globalSetupDone = true;
                        log.info("STSConfigJMeter: Global setup completed");
                    } catch (Exception e) {
                        log.error("STSConfigJMeter: Error during global setup", e);
                    }
                }
            }
        }
    }
    
    /**
     * Execute legacy single STS operation (backward compatibility)
     */
    private void executeLegacySTS() {
        try {
            // Ensure global setup has been executed
            ensureGlobalSetup();
            
            // Get current JMeter context
            JMeterVariables vars = JMeterContextService.getContext().getVariables();
            
            // Build the STS command from legacy single operation fields
            String action = getAction();
            String filename = getFilename().trim();
            
            if (filename.isEmpty()) {
                return; // Nothing to do
            }
            
            StringBuilder command = new StringBuilder();
            command.append(action).append(",").append(filename);
            
            if ("KEEP".equals(action) || "DEL".equals(action)) {
                String variables = getVariables().trim();
                if (!variables.isEmpty()) {
                    String[] varArray = variables.split(",");
                    for (String var : varArray) {
                        command.append(",").append(var.trim());
                    }
                }
            } else if ("ADDFIRST".equals(action) || "ADDLAST".equals(action)) {
                String values = getValues().trim();
                if (!values.isEmpty()) {
                    // Resolve JMeter variables in the values before sending to STS
                    String resolvedValues = resolveJMeterVariables(values, vars);
                    String[] valueArray = resolvedValues.split(",");
                    for (String value : valueArray) {
                        command.append(",").append(value.trim());
                    }
                }
            }
            
            // Execute STS function
            boolean success = STS.exec(
                log, // Logger
                vars,
                JMeterContextService.getContext().getProperties(),
                command.toString()
            );
            
            // Set result in JMeter variables (using fixed STS_ prefix for status variables)
            vars.put("STS_SUCCESS", success ? "true" : "false");
            vars.put("STS_ACTION", action);
            vars.put("STS_FILENAME", filename);
            vars.put("STS_STATUS", success ? "SUCCESS" : "FAILED");
            
            // Log success
                
        } catch (Exception e) {
            // Set error variables
            JMeterVariables vars = JMeterContextService.getContext().getVariables();
            vars.put("STS_SUCCESS", "false");
            vars.put("STS_STATUS", "ERROR");
            vars.put("STS_ERROR", e.getMessage());
            
            // Log error
        }
    }
    
    /**
     * Static method to execute STS operations on-demand from scripts
     * Call this from your Groovy script AFTER setting variables:
     * 
     * Example Groovy usage:
     *   vars.put("WriteThis", "actual_value_1")
     *   vars.put("WriteThat", "actual_value_2") 
     *   com.company.STSConfigJMeter.executeNow()
     */
    public static void executeNow() {
        try {
            // Get the current thread's context
            org.apache.jmeter.threads.JMeterContext context = JMeterContextService.getContext();
            JMeterVariables vars = context.getVariables();
            
            // Find the STSConfigJMeter element in the current test plan
            org.apache.jmeter.testelement.TestElement currentElement = context.getCurrentSampler();
            if (currentElement == null) {
                currentElement = context.getPreviousSampler();
            }
            
            // Look for STS config in the thread group or test plan
            STSConfigJMeter stsConfig = findSTSConfig(context);
            if (stsConfig != null) {
                log.info("DEBUG: STSConfigJMeter.executeNow() called from script - variables should be available now");
                stsConfig.executeSTSOperations();
            } else {
                log.warn("STSConfigJMeter.executeNow(): No STS Configuration found in test plan");
            }
            
        } catch (Exception e) {
            log.error("Error in STSConfigJMeter.executeNow()", e);
        }
    }
    
    /**
     * Find the STS configuration element in the test plan (simplified version)
     */
    private static STSConfigJMeter findSTSConfig(org.apache.jmeter.threads.JMeterContext context) {
        // For now, just log that executeNow() was called - the automatic sampler approach should work
        log.info("STSConfigJMeter.executeNow() called - but now that STS is a Sampler, it should run automatically in tree order");
        return null;
    }

    /**
     * Resolve JMeter variables (like ${varName}) in a string to their actual values
     */
    private String resolveJMeterVariables(String input, JMeterVariables vars) {
        if (input == null || vars == null) {
            return input;
        }
        
        // Debug: Log all available variables (only at DEBUG level)
        if (log.isDebugEnabled()) {
            if (vars.entrySet().size() > 0) {
                log.debug("Available JMeter variables (" + vars.entrySet().size() + " total):");
                for (java.util.Map.Entry<String, Object> entry : vars.entrySet()) {
                    log.debug("  " + entry.getKey() + " = " + entry.getValue());
                }
            } else {
                log.debug("No JMeter variables available in context!");
            }
        }
        
        String result = input;
        
        // Simple regex-based variable resolution for ${variableName} patterns
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String variableValue = vars.get(variableName);
            
            if (variableValue != null) {
                // Replace this specific variable reference with its value
                String varRef = "${" + variableName + "}";
                result = result.replace(varRef, variableValue);
                log.debug("Resolved variable " + varRef + " → " + variableValue);
            } else {
                log.warn("Variable " + variableName + " not found, keeping ${" + variableName + "} as-is");
            }
        }
        
        return result;
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