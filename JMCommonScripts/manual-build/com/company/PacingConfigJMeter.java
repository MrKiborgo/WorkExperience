package com.company;

import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.Controller;
import org.apache.jmeter.testelement.property.TestElementProperty;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

/**
 * JMeter Test Element for Pacing Configuration
 * Handles both fixed pacing (single value) and random pacing (min-max range)
 * Uses post-iteration pacing approach to eliminate final iteration hanging
 * 
 * NEW APPROACH: Calculate pacing at start, apply wait at end of iteration
 */
public class PacingConfigJMeter extends ConfigTestElement implements LoopIterationListener, TestStateListener {
    
    private static final long serialVersionUID = 1L;
    
    // Property keys for storing configuration
    public static final String PACING_MIN = "PacingConfig.min";
    public static final String PACING_MAX = "PacingConfig.max";
    public static final String PACING_ENABLED = "PacingConfig.enabled";
    
    // Thread-local storage for pacing state
    private static final ThreadLocal<Long> iterationStartTime = new ThreadLocal<>();
    private static final ThreadLocal<Double> currentPacing = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> firstIteration = new ThreadLocal<>();
    private static final Random random = new Random();
    
    public PacingConfigJMeter() {
        super();
        setEnabled(true);
    }
    
    // Getters and setters for configuration properties
    
    public void setMinPacing(String minPacing) {
        setProperty(PACING_MIN, minPacing);
    }
    
    public String getMinPacing() {
        return getPropertyAsString(PACING_MIN, "");
    }
    
    public void setMaxPacing(String maxPacing) {
        setProperty(PACING_MAX, maxPacing);
    }
    
    public String getMaxPacing() {
        return getPropertyAsString(PACING_MAX, "");
    }
    
    public void setPacingEnabled(boolean enabled) {
        setProperty(PACING_ENABLED, enabled);
    }
    
    public boolean isPacingEnabled() {
        return getPropertyAsBoolean(PACING_ENABLED, true);
    }
    
    /**
     * Validate the pacing configuration
     */
    public ValidationResult validate() {
        if (!isPacingEnabled()) {
            return new ValidationResult(true, "Pacing is disabled");
        }
        
        String minStr = getMinPacing().trim();
        if (minStr.isEmpty()) {
            return new ValidationResult(false, "Minimum pacing value is required");
        }
        
        try {
            int min = Integer.parseInt(minStr);
            if (min <= 0) {
                return new ValidationResult(false, "Minimum pacing must be greater than 0");
            }
            
            String maxStr = getMaxPacing().trim();
            if (!maxStr.isEmpty()) {
                int max = Integer.parseInt(maxStr);
                if (max <= 0) {
                    return new ValidationResult(false, "Maximum pacing must be greater than 0");
                }
                if (max < min) {
                    return new ValidationResult(false, "Maximum pacing must be greater than or equal to minimum pacing");
                }
            }
            
        } catch (NumberFormatException e) {
            return new ValidationResult(false, "Pacing values must be valid integers");
        }
        
        return new ValidationResult(true, "Configuration is valid");
    }
    
    /**
     * TestStateListener implementation - cleanup when test starts/ends
     */
    @Override
    public void testStarted() {
        testStarted("");
    }
    
    @Override
    public void testStarted(String host) {
        // Clear any previous state
        iterationStartTime.remove();
        currentPacing.remove();
        firstIteration.remove();
    }
    
    @Override
    public void testEnded() {
        testEnded("");
    }
    
    @Override
    public void testEnded(String host) {
        // Clean up thread-local storage
        iterationStartTime.remove();
        currentPacing.remove();
        firstIteration.remove();
    }
    
    /**
     * NEW APPROACH: LoopIterationListener - executes at the START of each iteration
     * But now we ONLY set up pacing for THIS iteration and handle previous iteration's wait
     */
    @Override
    public void iterationStart(LoopIterationEvent event) {
        if (!isPacingEnabled()) {
            return;
        }
        
        try {
            // Get current JMeter context
            JMeterVariables vars = JMeterContextService.getContext().getVariables();
            int iterationNum = vars.getIteration();
            
            // Check if this is the first iteration for this thread
            Boolean isFirst = firstIteration.get();
            if (isFirst == null) {
                // First time this thread is running
                firstIteration.set(true);
                org.slf4j.LoggerFactory.getLogger(this.getClass())
                    .info(">>>>> First iteration for thread - no pacing wait applied");
            } else {
                // Handle pacing wait from PREVIOUS iteration
                handlePacingWaitFromPreviousIteration(vars, iterationNum);
            }
            
            // Calculate and set pacing for THIS iteration
            double pacingValue = calculatePacing();
            currentPacing.set(pacingValue);
            vars.put("pacing", String.valueOf(pacingValue));
            
            // Set start time for THIS iteration
            long startTimeMillis = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            iterationStartTime.set(startTimeMillis);
            vars.put("start_time", String.valueOf(startTimeMillis));
            
            // Log pacing information
            org.slf4j.LoggerFactory.getLogger(this.getClass())
                .info("Pacing Configuration: Set pacing to {} seconds for iteration {}", 
                      pacingValue, iterationNum);
                      
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(this.getClass())
                .error("Pacing Configuration failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handle pacing wait from previous iteration
     * This runs at the START of iteration N, waiting for iteration N-1's pacing requirement
     * GUI-SAFE VERSION: Avoids blocking operations that could interfere with GUI threads
     */
    private void handlePacingWaitFromPreviousIteration(JMeterVariables vars, int currentIteration) {
        Long prevStartTime = iterationStartTime.get();
        Double prevPacing = currentPacing.get();
        
        if (prevStartTime != null && prevPacing != null) {
            String debugSwitch = vars.get("debugSwitch");
            boolean debugMode = "on".equals(debugSwitch);
            
            if (!debugMode) {
                long pacingMillis = (long)(prevPacing * 1000);
                long currentMillis = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                long elapsedMillis = currentMillis - prevStartTime;
                long remainingMillis = pacingMillis - elapsedMillis;
                
                // Get thread information for logging
                String threadName = JMeterContextService.getContext().getThreadGroup().getName();
                int threadNum = JMeterContextService.getContext().getThreadNum();
                
                String debugMsg = String.format("TG:%s:T%d:I%d Elapsed:%.1fs Pacing:%.1fs", 
                                               threadName, threadNum, currentIteration, 
                                               elapsedMillis/1000.0, pacingMillis/1000.0);
                
                if (remainingMillis > 0) {
                    debugMsg += String.format(" Waiting:%.1fs", remainingMillis/1000.0);
                    org.slf4j.LoggerFactory.getLogger(this.getClass()).info(">>>>> " + debugMsg);
                    
                    // GUI-SAFE SLEEP: Use a non-blocking approach for GUI compatibility
                    try {
                        // Detect if we're running in GUI mode vs non-GUI mode
                        boolean isGuiMode = isRunningInGuiMode();
                        
                        if (isGuiMode) {
                            // GUI mode: Use shorter sleep intervals to avoid GUI freezing
                            guiSafeSleep(remainingMillis);
                        } else {
                            // Non-GUI mode: Use regular sleep
                            Thread.sleep(remainingMillis);
                        }
                    } catch (InterruptedException e) {
                        org.slf4j.LoggerFactory.getLogger(this.getClass())
                            .warn(">>>>> Sleep interrupted in thread {}-{} - test stopping", threadName, threadNum);
                        // Properly handle interruption by restoring interrupt status
                        Thread.currentThread().interrupt();
                        return; // Exit early if interrupted
                    }
                } else {
                    debugMsg += " No wait needed";
                    org.slf4j.LoggerFactory.getLogger(this.getClass()).info(">>>>> " + debugMsg);
                }
            } else {
                org.slf4j.LoggerFactory.getLogger(this.getClass())
                    .info(">>>>> Debug mode ON - skipping pacing wait");
            }
        } else {
            org.slf4j.LoggerFactory.getLogger(this.getClass())
                .debug(">>>>> Previous timing data not available – no pacing wait applied");
        }
    }
    
    /**
     * Detect if JMeter is running in GUI mode
     */
    private boolean isRunningInGuiMode() {
        try {
            // Check if JMeter GUI components are available
            Class.forName("org.apache.jmeter.gui.GuiPackage");
            return org.apache.jmeter.gui.GuiPackage.getInstance() != null;
        } catch (Exception e) {
            // GUI classes not available or not initialized - running in non-GUI mode
            return false;
        }
    }
    
    /**
     * GUI-safe sleep that won't freeze the GUI
     * Sleeps in smaller intervals and checks for interruption more frequently
     */
    private void guiSafeSleep(long totalMillis) throws InterruptedException {
        long sleptMillis = 0;
        final long sleepInterval = 100; // Sleep in 100ms chunks
        
        org.slf4j.LoggerFactory.getLogger(this.getClass())
            .debug(">>>>> GUI-safe sleep: {}ms in {}ms intervals", totalMillis, sleepInterval);
        
        while (sleptMillis < totalMillis) {
            long remainingMillis = totalMillis - sleptMillis;
            long thisInterval = Math.min(sleepInterval, remainingMillis);
            
            Thread.sleep(thisInterval);
            sleptMillis += thisInterval;
            
            // Check for interruption more frequently in GUI mode
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("GUI-safe sleep interrupted");
            }
        }
        
        org.slf4j.LoggerFactory.getLogger(this.getClass())
            .debug(">>>>> GUI-safe sleep completed: {}ms", sleptMillis);
    }
    
    /**
     * Calculate pacing value based on configuration
     * Returns either fixed value or random value within range
     */
    private double calculatePacing() {
        try {
            int min = Integer.parseInt(getMinPacing().trim());
            String maxStr = getMaxPacing().trim();
            
            if (maxStr.isEmpty()) {
                // Fixed pacing
                org.slf4j.LoggerFactory.getLogger(this.getClass())
                    .debug(">>>>> Single pacing value: {}", min);
                return min;
            } else {
                // Random pacing within range
                int max = Integer.parseInt(maxStr);
                if (min > max) {
                    // Swap if needed
                    int temp = min;
                    min = max;
                    max = temp;
                }
                
                int pacingValue = min + random.nextInt(max - min + 1);
                org.slf4j.LoggerFactory.getLogger(this.getClass())
                    .debug(">>>>> Pacing range {}-{}, selected: {}", min, max, pacingValue);
                return pacingValue;
            }
            
        } catch (NumberFormatException e) {
            org.slf4j.LoggerFactory.getLogger(this.getClass())
                .error("Invalid pacing configuration, using default: " + e.getMessage());
            return 60; // Default fallback
        }
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