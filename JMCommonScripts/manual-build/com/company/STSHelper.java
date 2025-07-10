package com.company;

import org.apache.jmeter.threads.JMeterVariables;
import org.apache.log.Logger;
import org.apache.jorphan.logging.LoggingManager;

import java.util.Properties;

/**
 * Simple helper class to make STS calls more concise in JSR223 samplers
 * 
 * Usage in JSR223 samplers:
 *   import static com.company.STSHelper.sts
 *   sts("KEEP,applications.csv,APP_ID,Passport_Number")
 * 
 * Or even shorter:
 *   import static com.company.STSHelper.*
 *   sts("KEEP,applications.csv,APP_ID,Passport_Number")
 */
public final class STSHelper {
    
    /**
     * Shortcut method for STS operations
     * Automatically uses the current JMeter context (log, vars, props)
     */
    public static boolean sts(String command) {
        // Get JMeter context objects
        Logger log = LoggingManager.getLoggerForClass();
        JMeterVariables vars = org.apache.jmeter.threads.JMeterContextService.getContext().getVariables();
        Properties props = org.apache.jmeter.threads.JMeterContextService.getContext().getProperties();
        
        // Call the main STS function
        return STS.exec(log, vars, props, command);
    }
    
    /**
     * Alternative method names for different preferences
     */
    public static boolean STS(String command) {
        return sts(command);
    }
    
    public static boolean exec(String command) {
        return sts(command);
    }
    
    /**
     * Overloaded version that allows custom logging
     */
    public static boolean sts(Logger customLog, String command) {
        JMeterVariables vars = org.apache.jmeter.threads.JMeterContextService.getContext().getVariables();
        Properties props = org.apache.jmeter.threads.JMeterContextService.getContext().getProperties();
        
        return STS.exec(customLog, vars, props, command);
    }
} 