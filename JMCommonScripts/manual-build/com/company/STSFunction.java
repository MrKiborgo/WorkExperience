package com.company;

import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.functions.AbstractFunction;
import org.apache.jmeter.functions.InvalidVariableException;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.log.Logger;
import org.apache.jorphan.logging.LoggingManager;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * JMeter custom function for STS operations
 * 
 * Usage in any JMeter test element:
 *   ${__STS(KEEP,applications.csv,APP_ID,Passport_Number)}
 *   ${__STS(DEL,applications.csv,APP_ID,Passport_Number)}
 *   ${__STS(ADDFIRST,applications.csv,value1,value2,value3)}
 * 
 * The function returns "true" on success, "false" on failure
 * Variables are automatically populated in JMeter context
 */
public class STSFunction extends AbstractFunction {
    
    private static final Logger log = LoggingManager.getLoggerForClass();
    
    private static final String KEY = "__STS";
    private static final List<String> DESC = new LinkedList<>();
    
    static {
        DESC.add("Action (KEEP, DEL, ADDFIRST, ADDLAST)");
        DESC.add("Filename (e.g., applications.csv)");
        DESC.add("Variable1 (or Value1 for ADD operations)");
        DESC.add("Variable2 (or Value2 for ADD operations) - Optional");
        DESC.add("Variable3 (or Value3 for ADD operations) - Optional");
        DESC.add("Variable4 (or Value4 for ADD operations) - Optional");
        DESC.add("Variable5 (or Value5 for ADD operations) - Optional");
    }
    
    private Object[] parameters;
    
    @Override
    public String execute(SampleResult result, Sampler sampler) throws InvalidVariableException {
        // Reconstruct the command from multiple parameters
        StringBuilder commandBuilder = new StringBuilder();
        
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                commandBuilder.append(",");
            }
            String param = ((CompoundVariable) parameters[i]).execute().trim();
            commandBuilder.append(param);
        }
        
        String command = commandBuilder.toString();
        
        if (command.isEmpty()) {
            log.error("STS Function: Command parameters are required");
            return "false";
        }
        
        try {
            // Get JMeter context
            JMeterVariables vars = getVariables();
            Properties props = org.apache.jmeter.threads.JMeterContextService.getContext().getProperties(); 
            
            // Execute STS operation
            boolean success = STS.exec(log, vars, props, command);
            
            log.debug("STS Function: Command '" + command + "' executed with result: " + success);
            return success ? "true" : "false";
            
        } catch (Exception e) {
            log.error("STS Function: Error executing command '" + command + "': " + e.getMessage(), e);
            return "false";
        }
    }
    
    @Override
    public void setParameters(Collection<CompoundVariable> parameters) throws InvalidVariableException {
        checkParameterCount(parameters, 2, 7); // Min 2 (action + filename), Max 7 (action + filename + 5 variables)
        this.parameters = parameters.toArray();
    }
    
    @Override
    public String getReferenceKey() {
        return KEY;
    }
    
    @Override
    public List<String> getArgumentDesc() {
        return DESC;
    }
} 