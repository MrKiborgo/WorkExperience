package com.company;

import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.testelement.property.BooleanProperty;

/**
 * JMeter Test Element for Gemini CLI Integration
 * Stores configuration for the Gemini Chat Assistant
 */
public class GeminiChatJMeter extends ConfigTestElement {
    
    private static final String INCLUDE_JMX_CONTEXT = "GeminiChat.includeJmxContext";
    
    public GeminiChatJMeter() {
        super();
        setIncludeJmxContext(true); // Default to including context
    }
    
    /**
     * Set whether to include JMX context in Gemini requests
     * @param includeContext true to include context, false otherwise
     */
    public void setIncludeJmxContext(boolean includeContext) {
        setProperty(new BooleanProperty(INCLUDE_JMX_CONTEXT, includeContext));
    }
    
    /**
     * Get whether to include JMX context in Gemini requests
     * @return true if context should be included, false otherwise
     */
    public boolean getIncludeJmxContext() {
        return getPropertyAsBoolean(INCLUDE_JMX_CONTEXT, true);
    }
} 