/*
 * DEPLOYMENT LOCATION: This script must be placed in the JMeter bin directory
 * Path: <JMETER_HOME>/bin/jmeter-bin/scripts/global_setup.groovy
 * 
 * This is the bootstrap script that runs before all other scripts and sets up
 * the environment for portable test plans. It processes template properties
 * and establishes directory paths for all other project scripts.
 */

import java.net.InetAddress

// ===== TEMPLATE PROPERTIES PROCESSING (moved to run first) =====
// Function to process template properties and convert them to variables
def processTemplateProperties() {
    log.info("Starting template properties processing...")
    
    def templatePropsProcessed = 0
    
    // Iterate through all JMeter properties looking for ones starting with "template."
    props.stringPropertyNames().each { propName ->
        if (propName.startsWith("template.")) {
            // Strip the "template." prefix to create the variable name
            def varName = propName.substring("template.".length())
            def propValue = props.get(propName)
            
            // Set the variable with the stripped name
            vars.put(varName, propValue)
            
            log.info("Processed template property: ${propName} -> Variable: ${varName} = ${propValue}")
            templatePropsProcessed++
        }
    }
    
    log.info("Template properties processing complete. Processed ${templatePropsProcessed} properties.")
}

// Process template properties FIRST so variables are available for directory setup
processTemplateProperties()

// ===== DIRECTORY SETUP (now uses variables from template processing) =====
// Get base directory using a more compatible approach
def jmxDir = null

// Try to get from JMeter system properties first
jmxDir = System.getProperty("jmeter.save.saveservice.base_prefix")
if (!jmxDir) {
    // Fallback to user.dir which should be the directory where JMeter was started
    jmxDir = System.getProperty("user.dir")
}

log.info(">>>>> Got JMX directory: ${jmxDir}")

// Clean up the directory path - remove trailing slash if present
if (jmxDir && jmxDir.endsWith("/")) {
    jmxDir = jmxDir.substring(0, jmxDir.length() - 1)
}
// Additional check to remove trailing /. if present
if (jmxDir && jmxDir.endsWith("/.")) {
    jmxDir = jmxDir.substring(0, jmxDir.length() - 2)
}

// Set JMX_DIR property
props.put("JMX_DIR", jmxDir)
log.info(">>>>> Set JMX_DIR property to: ${jmxDir}")

// Helper function to save variables as properties
def saveVarsAsProps(baseDir, varNames) {
    log.info(">>>>> Saving variables as properties...")
    
    varNames.each { varName ->
        def varValue = vars.get(varName)
        if (varValue != null) {
            // Create property name (same as variable name)
            def propName = varName
            
            // Create property value (baseDir + variable value)
            def propValue = baseDir + varValue
            
            // Save to properties
            props.put(propName, propValue)
            log.info(">>>>> Set ${propName} property to: ${propValue}")
        } else {
            log.warn(">>>>> Variable ${varName} not found, skipping property creation")
        }
    }
    
    return true
}

// Use the helper function to save common variables (now these variables exist from template processing)
def varsToSave = ["C_SCRIPTS", "P_SCRIPTS", "DATA"]
saveVarsAsProps(jmxDir, varsToSave)

// ===== EXISTING GLOBAL SETUP LOGIC =====
// Note: JMX_DIR is now already set from directory setup above
if (!jmxDir) {
    log.warn("JMX_DIR property not set. Using current directory as fallback.")
    jmxDir = System.getProperty("user.dir")
    props.put("JMX_DIR", jmxDir)
}
log.info("Using JMX Directory: ${jmxDir}")

// Get the scripts directory path from C_SCRIPTS variable
def scriptsPath = vars.get("C_SCRIPTS") ?: "/scripts/common/"
log.info("Using scripts path from C_SCRIPTS: ${scriptsPath}")

// Directly detect hostname
def hostname = null
try {
    hostname = InetAddress.getLocalHost().getHostName()
    props.put("hostname", hostname)
    log.info("Hostname detected and stored: ${hostname}")
    
    // Extract test name from hostname if it starts with "jm-gen-"
    if (hostname && hostname.startsWith("jm-gen-")) {
        // Extract the part between "jm-gen-" and the next "-"
        def matcher = hostname =~ /jm-gen-([^-]+)-/
        if (matcher.find()) {
            def testIdentifier = matcher.group(1)
            // Store in variables without capitalization
            vars.put("testname", testIdentifier)
            log.info("Extracted test name from hostname: ${testIdentifier}")
        } else {
            log.warn("Hostname starts with jm-gen- but couldn't extract test name: ${hostname}")
        }
    }
} catch (Exception e) {
    log.error("Failed to detect hostname: ${e.message}")
}

// Import and initialize DCU
def dcuPath = new File(jmxDir + scriptsPath + "DCU.groovy").getPath()
log.info("Loading DCU from: ${dcuPath}")
def dcu = evaluate(new File(dcuPath).text)
dcu.init(log, props, vars)

// Run the global setup
dcu.globalSetup()

log.info("Global Setup: Complete")
