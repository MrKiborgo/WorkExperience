// DCU (Dynamic Configuration Utility) module for JMeter scripts
import java.net.InetAddress
import java.nio.file.*

// Module state (will be initialized when imported)
def moduleLog = null
def moduleProps = null
def moduleVars = null

// Initialize the module with JMeter objects
def init(log, props, vars) {
    moduleLog = log
    moduleProps = props
    moduleVars = vars
    
    log.info("DCU module initialized")
    return this // Return the module for chaining
}

/**
 * Global setup function - detects hostname and Git repository information
 * This combines functionality from the original global_setup.groovy
 */
def globalSetup() {
    moduleLog.info("Global Setup: Starting setup")

    // Get and store the hostname
    detectHostname()
    
    // Find Git repository information
    findGitRepository()
    
    return this // Return the module for chaining
}

/**
 * Detect and store the hostname
 */
def detectHostname() {
    try {
        def hostname = InetAddress.getLocalHost().getHostName()
        moduleProps.put("hostname", hostname)
        moduleLog.info("Global Setup: Hostname detected and stored: ${hostname}")
        return hostname
    } catch (Exception e) {
        moduleLog.error("Failed to detect hostname: ${e.message}")
        return null
    }
}

/**
 * Find Git repository information starting from the JMX directory
 */
def findGitRepository() {
    // Retrieve the JMX test plan directory from the saved property
    def jmxDir = moduleProps.get("JMX_DIR")

    if (!jmxDir) {
        moduleLog.warn("⚠️ JMX_DIR property is not set. Cannot determine Git repository location.")
        return
    }

    moduleLog.info("🔍 Checking Git repository from test plan directory: ${jmxDir}")

    // Locate the Git repository starting from the JMX test plan directory
    def gitRepoPath = findGitRepo(jmxDir)

    if (gitRepoPath) {
        def gitConfigPath = Paths.get(gitRepoPath, ".git", "config")
        def configLines = Files.readAllLines(gitConfigPath)
        def repoUrl = configLines.find { it.contains("url =") }?.split("=")[1]?.trim()
        
        // Extract repository name from the path or URL
        def repoName = ""
        if (repoUrl) {
            // Try to extract from URL first
            def urlPattern = ~/.*[\/:]([^\/]+)(\.git)?$/
            def matcher = repoUrl =~ urlPattern
            if (matcher.matches()) {
                repoName = matcher[0][1]
            }
        }
        
        // If we couldn't extract from URL, use the directory name
        if (!repoName) {
            repoName = Paths.get(gitRepoPath).getFileName().toString()
        }

        // Store Git repository info in JMeter properties
        moduleProps.put("GIT_REPO_PATH", gitRepoPath)
        moduleProps.put("GIT_REPO_URL", repoUrl ?: "Not Found")
        moduleProps.put("GIT_REPO_NAME", repoName)

        moduleLog.info("✅ Git Repository Found: ${gitRepoPath}")
        moduleLog.info("🔗 Git Remote URL: ${repoUrl}")
        moduleLog.info("📂 Git Repository Name: ${repoName}")
    } else {
        moduleLog.warn("⚠️ No Git repository found for the test plan.")
    }
}

// Function to find the closest Git repository from a given directory
private def findGitRepo(startPath) {
    def currentDir = Paths.get(startPath).toAbsolutePath()
    while (currentDir != null) {
        def gitConfigPath = currentDir.resolve(".git/config")
        if (Files.exists(gitConfigPath)) {
            return currentDir.toString()
        }
        currentDir = currentDir.getParent()  // Move up to the parent directory
    }
    return null
}

// Function to check if running locally and set STS host property accordingly
def checkHost() {
    def hostname = moduleProps.get("hostname")
    moduleLog.info(">>>>>>>>>>>>>>>>>>>>>>>>>> HOSTNAME: ${hostname}")
    
    // Default to unknown environment
    def isLocal = "NOT_FOUND"
    
    if (hostname == null) {
        moduleLog.warn(">>>>> Cannot check local host: hostname not set")
        return isLocal
    }
    
    // Check if hostname starts with "jm-gen" or "jm-nofrills" (supporting additional remote host pattern)
    if (hostname.startsWith("jm-gen") || hostname.startsWith("jm-nofrills")) {
        isLocal = false
        moduleLog.info(">>>>> Detected remote execution environment (${hostname})")
        
        // Get the local host value from PROPERTIES (not variables) for reliable access
        def localHost = moduleProps.get("V_STS_LOCAL_HOST")
        if (localHost != null && !localHost.isEmpty()) {
            // Set the host PROPERTY (not variable) for reliable global access
            moduleProps.put("V_STS_HOST", localHost)
            moduleLog.info(">>>>> Set V_STS_HOST property to remote value: ${localHost}")
            
            // Also set as variable for backward compatibility
            moduleVars.put("V_STS_HOST", localHost)
            moduleLog.info(">>>>> Also set V_STS_HOST variable for backward compatibility: ${localHost}")
        } else {
            moduleLog.warn(">>>>> V_STS_LOCAL_HOST property is not set or empty, cannot set V_STS_HOST")
        }
    } else {
        moduleLog.info(">>>>> Running in local environment (${hostname})")
        isLocal = true
        
        // For local environment, check if there's a default STS host property
        def defaultStsHost = moduleProps.get("V_STS_HOST_DEFAULT")
        if (defaultStsHost != null && !defaultStsHost.isEmpty()) {
            moduleProps.put("V_STS_HOST", defaultStsHost)
            moduleVars.put("V_STS_HOST", defaultStsHost)
            moduleLog.info(">>>>> Set V_STS_HOST to default local value: ${defaultStsHost}")
        }
    }
    
    // Store the environment type in properties for future reference
    moduleProps.put("IS_LOCAL_ENVIRONMENT", isLocal.toString())
    
    return isLocal
}

/**
 * Get STS host with preference for properties over variables
 * This ensures reliable access across different JMeter contexts
 */
def getStsHost() {
    // First try to get from properties (reliable)
    def stsHost = moduleProps.get("V_STS_HOST")
    if (stsHost != null && !stsHost.isEmpty()) {
        moduleLog.info(">>>>> Got STS host from properties: ${stsHost}")
        return stsHost
    }
    
    // Fallback to variables (for backward compatibility)
    stsHost = moduleVars.get("V_STS_HOST")
    if (stsHost != null && !stsHost.isEmpty()) {
        moduleLog.warn(">>>>> Got STS host from variables (fallback): ${stsHost}")
        // Store in properties for future use
        moduleProps.put("V_STS_HOST", stsHost)
        return stsHost
    }
    
    moduleLog.error(">>>>> V_STS_HOST not found in properties or variables")
    return null
}

// Get repository information
def getRepoInfo() {
    def repoPath = moduleProps.get("GIT_REPO_PATH")
    def repoUrl = moduleProps.get("GIT_REPO_URL")
    def repoName = moduleProps.get("GIT_REPO_NAME")
    
    return [
        path: repoPath,
        url: repoUrl,
        name: repoName
    ]
}

/**
 * Flexible counter implementation
 * @param varName Name of the variable to store the counter value
 * @param startValue Starting value for the counter
 * @param maxValue Maximum value before resetting to start value
 * @param increment Amount to increment by each time
 * @param policy Either "each_iteration" (thread-local) or "each_use" (global)
 * @return The current counter value
 */
def getCounter(String varName, int startValue, int maxValue, int increment, String policy) {
    try {
        // Validate Policy
        if (!["each_iteration", "each_use"].contains(policy)) {
            throw new IllegalArgumentException("Invalid policy. Use 'each_iteration' or 'each_use'.")
        }

        // Get stored value or initialize
        def currentValue
        def counterKey = "_COUNTER_" + varName

        if (policy == "each_iteration") {
            currentValue = moduleVars.get(counterKey) ?: startValue
            currentValue = currentValue.toInteger() + increment
            if (currentValue > maxValue) currentValue = startValue
            moduleVars.put(counterKey, currentValue.toString())
        } else {
            currentValue = moduleProps.get(counterKey) ?: startValue
            currentValue = currentValue.toInteger() + increment
            if (currentValue > maxValue) currentValue = startValue
            moduleProps.put(counterKey, currentValue.toString())
        }

        // Store the final result
        moduleVars.put(varName, currentValue.toString())
        moduleLog.info("Counter '${varName}': ${currentValue} (${policy})")
        
        return currentValue

    } catch (Exception e) {
        moduleLog.error("Error in Counter: " + e.getMessage())
        return startValue
    }
}

/**
 * Set a property value with logging
 * Useful for testing and configuration
 */
def setProp(String propName, String propValue) {
    moduleProps.put(propName, propValue)
    moduleLog.info(">>>>> Set property ${propName} = ${propValue}")
    return this // Return the module for chaining
}

/**
 * Get a property value with logging
 * Returns null if not found
 */
def getProp(String propName) {
    def value = moduleProps.get(propName)
    if (value != null) {
        moduleLog.info(">>>>> Got property ${propName} = ${value}")
    } else {
        moduleLog.warn(">>>>> Property ${propName} not found")
    }
    return value
}

// Return the module itself
return this