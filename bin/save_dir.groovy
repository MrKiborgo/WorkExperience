import org.apache.jmeter.services.FileServer

// Get base directory from FileServer (works in both GUI and non-GUI modes)
def jmxDir = FileServer.getFileServer().getBaseDir()
log.info(">>>>> Got JMX directory: ${jmxDir}")

// Clean up the directory path - remove trailing slash if present
if (jmxDir.endsWith("/")) {
    jmxDir = jmxDir.substring(0, jmxDir.length() - 1)
}
// Additional check to remove trailing /. if present
if (jmxDir.endsWith("/.")) {
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

// Use the helper function to save common variables
def varsToSave = ["C_SCRIPTS", "P_SCRIPTS", "DATA"]
saveVarsAsProps(jmxDir, varsToSave)

// Return the JMX directory for potential use in the calling script
return jmxDir

