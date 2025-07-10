import java.nio.file.Paths
import java.net.InetAddress
import org.apache.jmeter.services.FileServer

// ===============================================================
// IMPORTS FUNCTIONALITY (moved from imports.groovy)
// This must run FIRST to set up template properties and directories
// ===============================================================

def processTemplateProperties() {
    log.info("Starting template properties processing...")
    
    def templatePropsProcessed = 0
    
    // Defensive check for props availability
    if (props == null) {
        log.error("Props object not available")
        return
    }
    
    // Iterate through all JMeter properties looking for ones starting with "template."
    props.stringPropertyNames().each { propName ->
        if (propName.startsWith("template.")) {
            // Strip the "template." prefix to create the variable name
            def varName = propName.substring("template.".length())
            def propValue = props.get(propName)
            
            // Trim any leading/trailing spaces from the property value
            if (propValue != null) {
                propValue = propValue.trim()
            }
            
            // Set the variable with the stripped name (for backward compatibility)
            vars.put(varName, propValue)
            
            // ALSO set as a property without template prefix for reliable global access
            props.put(varName, propValue)
            
            log.info("Processed template property: ${propName} -> Variable: ${varName} = ${propValue}")
            log.info("Also set global property: ${varName} = ${propValue}")
            templatePropsProcessed++
        }
    }
    
    log.info("Template properties processing complete. Processed ${templatePropsProcessed} properties.")
}

// Helper function to save variables as properties
def saveVarsAsProps(baseDir, varNames) {
    log.info(">>>>> Saving variables as properties...")
    
    // Ensure baseDir is clean
    def cleanBaseDir = baseDir.trim()
    if (cleanBaseDir.endsWith("/") || cleanBaseDir.endsWith("\\")) {
        cleanBaseDir = cleanBaseDir.substring(0, cleanBaseDir.length() - 1)
    }
    
    varNames.each { varName ->
        def varValue = vars.get(varName)
        if (varValue != null) {
            // Trim the variable value and ensure clean path construction
            varValue = varValue.trim()
            
            // Create property name (same as variable name)
            def propName = varName
            
            // Create property value (baseDir + variable value)
            def propValue = cleanBaseDir + varValue
            
            // Save to properties
            props.put(propName, propValue)
            log.info(">>>>> Set ${propName} property to: ${propValue}")
        } else {
            log.warn(">>>>> Variable ${varName} not found, skipping property creation")
        }
    }
    
    return true
}

// STEP 1: Process template properties FIRST so variables are available for directory setup
processTemplateProperties()

// STEP 2: Set up JMX directory and related properties
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

// STEP 3: Save common variables as properties with full paths
def varsToSave = ["C_SCRIPTS", "P_SCRIPTS", "DATA"]
saveVarsAsProps(jmxDir, varsToSave)

// ===============================================================
// GLOBAL SETUP FUNCTIONALITY (existing functionality)
// ===============================================================

// Verify JMX_DIR is now available (it should be from above)
if (!props.get("JMX_DIR")) {
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

// ---------------------------------------------------------------
// Environment detection (migrated from DCU.checkHost)
// ---------------------------------------------------------------

def checkHostGlobal = {
    def hostnameVal = props.get("hostname")
    log.info(">>>>>>>>>>>>>>>>>>>>>>> HOSTNAME: ${hostnameVal}")

    def isLocal = "NOT_FOUND"

    if (hostnameVal == null) {
        log.warn(">>>>> Cannot check local host: hostname not set")
        return isLocal
    }

    // Detect remote execution based on hostname pattern
    if (hostnameVal.startsWith("jm-gen") || hostnameVal.startsWith("jm-nofrills")) {
        isLocal = false
        log.info(">>>>> Detected remote execution environment (${hostnameVal})")

        // Get the local host value from properties (not variables) for reliability
        def localHost = props.get("V_STS_LOCAL_HOST")
        if (localHost != null && !localHost.isEmpty()) {
            // Set as property
            props.put("V_STS_HOST", localHost)
            // Also set as variable for backward compatibility
            vars.put("V_STS_HOST", localHost)
            log.info(">>>>> Set V_STS_HOST to remote value: ${localHost}")
        } else {
            log.warn(">>>>> V_STS_LOCAL_HOST property is not set or empty, cannot set V_STS_HOST")
        }
    } else {
        log.info(">>>>> Running in local environment (${hostnameVal})")
        isLocal = true

        // For local environment, apply default STS host if defined
        def defaultStsHost = props.get("V_STS_HOST_DEFAULT")
        if (defaultStsHost != null && !defaultStsHost.isEmpty()) {
            props.put("V_STS_HOST", defaultStsHost)
            vars.put("V_STS_HOST", defaultStsHost)
            log.info(">>>>> Set V_STS_HOST to default local value: ${defaultStsHost}")
        }
    }

    // Store the environment flag
    props.put("IS_LOCAL_ENVIRONMENT", isLocal.toString())
    return isLocal
}

// Execute host/environment check once during global setup
checkHostGlobal()

// Clean up scriptsPath for proper path construction
def cleanScriptsPath = scriptsPath.trim()
// Remove leading slash if present (since we're appending to jmxDir)
if (cleanScriptsPath.startsWith("/")) {
    cleanScriptsPath = cleanScriptsPath.substring(1)
}
// Remove trailing slash if present
if (cleanScriptsPath.endsWith("/")) {
    cleanScriptsPath = cleanScriptsPath.substring(0, cleanScriptsPath.length() - 1)
}
// Replace forward slashes with system file separator
cleanScriptsPath = cleanScriptsPath.replace("/", File.separator)

// ---------------------------------------------------------------
// Git repository detection (migrated from DCU.groovy)
// ---------------------------------------------------------------

// Helper closure to walk up directories until a .git folder is found
def findGitRepo = { startPath ->
    def currentDir = Paths.get(startPath).toAbsolutePath()
    while (currentDir != null) {
        def gitConfigPath = currentDir.resolve(".git").resolve("config")
        if (java.nio.file.Files.exists(gitConfigPath)) {
            return currentDir.toString()
        }
        currentDir = currentDir.getParent()
    }
    return null
}

// Main detection routine
def detectGitRepository = {
    log.info("🔍 Checking Git repository from test plan directory: ${jmxDir}")

    def gitRepoPath = findGitRepo(jmxDir)

    if (gitRepoPath) {
        def gitConfigPath = Paths.get(gitRepoPath, ".git", "config")
        def configLines = java.nio.file.Files.readAllLines(gitConfigPath)
        def repoUrl = configLines.find { it.contains("url =") }?.split("=")[1]?.trim()

        // Extract repository name from the path or URL
        def repoName = ""
        if (repoUrl) {
            def matcher = (repoUrl =~ /.*[\\/:]([^\\/]+)(\\.git)?$/)
            if (matcher.matches()) {
                repoName = matcher[0][1]
            }
        }

        if (!repoName) {
            repoName = Paths.get(gitRepoPath).getFileName().toString()
        }

        // Store Git repository info as properties for global access
        props.put("GIT_REPO_PATH", gitRepoPath)
        props.put("GIT_REPO_URL", repoUrl ?: "Not Found")
        props.put("GIT_REPO_NAME", repoName)

        log.info("✅ Git Repository Found: ${gitRepoPath}")
        log.info("🔗 Git Remote URL: ${repoUrl}")
        log.info("📂 Git Repository Name: ${repoName}")
    } else {
        log.warn("⚠️ No Git repository found for the test plan.")
    }
}

// Execute detection once during global setup
detectGitRepository()

log.info("Global Setup: Complete")