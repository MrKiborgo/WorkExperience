/**
 * STS (Simple Table Server) utility script for JMeter
 * 
 * Usage:
 * Enter values in the "Parameters" field of the JSR223 element:
 *   KEEP,applications.csv,APP_ID,Passport_Number,Name
 *   or
 *   DEL,applications.csv,APP_ID,Passport_Number,Name
 *   or
 *   ADDFIRST,applications.csv,value1,value2,value3
 *   or
 *   ADDLAST,applications.csv,value1,value2,value3
 * 
 * Format for READ operations:
 *   action,filename,var1,var2,...,varN
 * 
 * Format for ADD operations:
 *   ADDFIRST/ADDLAST,filename,value1,value2,...,valueN
 * 
 * Where:
 *   - action: KEEP, DEL, ADDFIRST, or ADDLAST
 *   - filename: The name of the CSV file in STS
 *   - For KEEP/DEL: var1,var2,...,varN are variable names to store the CSV columns
 *   - For ADDFIRST/ADDLAST: value1,value2,...,valueN are values to add as a new row
 * 
 * The script will:
 *   - For KEEP/DEL: Read the first row and store in variables
 *   - For ADDFIRST: Add a new row to the beginning of the file
 *   - For ADDLAST: Add a new row to the end of the file
 */

import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.message.BasicNameValuePair
import java.nio.charset.StandardCharsets
import java.net.URLEncoder
import org.apache.http.ssl.SSLContextBuilder
import org.apache.http.ssl.TrustStrategy
import org.apache.http.conn.ssl.NoopHostnameVerifier
import java.security.cert.X509Certificate

// Load DCU module for STS host configuration
def jmxDir = props.get("JMX_DIR")
if (jmxDir) {
    jmxDir = jmxDir.trim()
}

// Use variable first (relative path), fallback to property (absolute path) if needed
def scriptsPath = vars.get("C_SCRIPTS") ?: props.get("C_SCRIPTS") ?: "/scripts/common/"
scriptsPath = scriptsPath.trim() // Remove any leading/trailing spaces

// If we got an absolute path from properties, extract just the relative part
if (scriptsPath.startsWith(jmxDir)) {
    scriptsPath = scriptsPath.substring(jmxDir.length())
    // Remove leading slash if present
    if (scriptsPath.startsWith("/") || scriptsPath.startsWith("\\")) {
        scriptsPath = scriptsPath.substring(1)
    }
}

// Clean up scriptsPath to ensure proper path construction
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

def dcuPath = new File(jmxDir, cleanScriptsPath + File.separator + "DCU.groovy").getAbsolutePath()
def dcu = evaluate(new File(dcuPath).text)
dcu.init(log, props, vars)

// Get and parse the input parameters
def inputParams = Parameters.trim()
if (!inputParams) {
    log.error("No parameters provided. Format: action,filename,var1/value1,var2/value2,...,varN/valueN")
    return
}

def paramsList = inputParams.split(',').collect { it.trim() }
if (paramsList.size() < 3) {
    log.error("Insufficient parameters. Need at least action, filename and one variable/value.")
    return
}

// Extract action, filename and remaining parameters
def action = paramsList[0].toUpperCase()
def originalFilename = paramsList[1]
def remainingParams = paramsList[2..-1]

// Format the filename according to convention
def filename = formatFilename(originalFilename)

// Replace the entire DCU loading and path handling block with a local helper for STS host retrieval
def getStsHost() {
    // Prefer the property value for consistency across JMeter components
    def stsHost = props.get("V_STS_HOST")
    if (stsHost && stsHost.trim()) {
        log.info(">>>>> Got STS host from properties: ${stsHost}")
        return stsHost
    }
    // Fallback to variables for backward compatibility
    stsHost = vars.get("V_STS_HOST")
    if (stsHost && stsHost.trim()) {
        log.warn(">>>>> Got STS host from variables (fallback): ${stsHost}")
        // Cache in properties for future look-ups
        props.put("V_STS_HOST", stsHost)
        return stsHost
    }
    log.error(">>>>> V_STS_HOST not found in properties or variables")
    return null
}

// Get the STS host using local helper (properties-first approach)
def stsHost = getStsHost()
if (!stsHost) {
    log.error("V_STS_HOST not available from local helper. Cannot connect to Simple Table Server.")
    return
}

// Handle different actions
switch (action) {
    case "KEEP":
    case "DEL":
        // Read operation
        def keepValue = (action == "KEEP") ? "TRUE" : "FALSE"
        log.info("STS Read: Action=${action}, Original File=${originalFilename}, Formatted File=${filename}, Variables=${remainingParams}")
        def success = STS_read(stsHost, filename, keepValue, remainingParams)
        
        if (success) {
            log.info("STS read successful (${action} mode)")
        } else {
            log.error("Failed to read from STS")
        }
        break
        
    case "ADDFIRST":
        // Add operation at the beginning
        log.info("STS Add: Action=${action}, Original File=${originalFilename}, Formatted File=${filename}, Values=${remainingParams}")
        def success = STS_add(stsHost, filename, "FIRST", remainingParams)
        
        if (success) {
            log.info("STS add successful (ADDFIRST mode)")
        } else {
            log.error("Failed to add to STS")
        }
        break
        
    case "ADDLAST":
        // Add operation at the end
        log.info("STS Add: Action=${action}, Original File=${originalFilename}, Formatted File=${filename}, Values=${remainingParams}")
        def success = STS_add(stsHost, filename, "LAST", remainingParams)
        
        if (success) {
            log.info("STS add successful (ADDLAST mode)")
        } else {
            log.error("Failed to add to STS")
        }
        break
        
    default:
        log.error("Invalid action: ${action}. Must be KEEP, DEL, ADDFIRST, or ADDLAST.")
}

// Function to read from STS and keep/delete the row
def STS_read(String stsHost, String filename, String keepValue, List<String> varNames) {
    try {
        // Determine protocol based on sts.use.https property
        def useHttps = Boolean.parseBoolean(props.get("sts.use.https") ?: "false")
        def protocol = useHttps ? "https" : "http"
        
        // Build the STS URL without encoding the filename
        def stsUrl = "${protocol}://${stsHost}/sts/READ?READ_MODE=FIRST&KEEP=${keepValue}&FILENAME=${filename}"
        
        // Log the full request details
        log.info("==== STS READ REQUEST ====")
        log.info("URL: ${stsUrl}")
        log.info("Method: GET")
        log.info("READ_MODE: FIRST")
        log.info("KEEP: ${keepValue}")
        log.info("FILENAME: ${filename}")
        log.info("=========================")
        
        // Create HTTP client and execute request
        def httpClient = createTrustAllHttpClient()
        def httpGet = new HttpGet(stsUrl)
        
        // Execute the request
        log.info("Executing GET request to STS...")
        def response = httpClient.execute(httpGet)
        def statusCode = response.getStatusLine().getStatusCode()
        def statusLine = response.getStatusLine().toString()
        
        // Extract response body
        def responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
        
        // Log the full response
        log.info("==== STS READ RESPONSE ====")
        log.info("Status: ${statusLine}")
        log.info("Status Code: ${statusCode}")
        log.info("Response Body: ${responseBody}")
        log.info("==========================")
        
        if (statusCode != 200) {
            log.error("STS request failed with status code: ${statusCode}")
            
            // Set error message in all variables
            def errorMsg = "${filename}-HTTP_ERROR_${statusCode}"
            varNames.each { varName ->
                vars.put(varName, errorMsg)
                log.info("Set error value for variable ${varName}: ${errorMsg}")
            }
            
            return false
        }
        
        // Check for empty file response - exact match for the known empty response
        if (responseBody.contains("<title>KO</title>") && responseBody.contains("Error : No more line !")) {
            log.warn("No more data available in file: ${filename}")
            
            // Create a descriptive error message that includes the filename
            def emptyFileMsg = "${filename}-EMPTY_FILE_ERROR"
            
            // Set the error message in all requested variables
            varNames.each { varName ->
                vars.put(varName, emptyFileMsg)
                log.info("Set empty file error value for variable ${varName}: ${emptyFileMsg}")
            }
            
            // Set a special flag variable to indicate empty file
            vars.put("STS_FILE_EMPTY", "true")
            vars.put("STS_FILE_EMPTY_NAME", filename)
            log.info("Set STS_FILE_EMPTY=true to indicate empty file condition")
            
            return false
        }
        
        // Extract data from HTML response
        def bodyMatcher = responseBody =~ /<body>(.*?)<\/body>/
        if (!bodyMatcher.find()) {
            log.error("Could not extract data from STS response")
            
            // Set parse error message in all variables
            def parseErrorMsg = "${filename}-PARSE_ERROR"
            varNames.each { varName ->
                vars.put(varName, parseErrorMsg)
                log.info("Set parse error value for variable ${varName}: ${parseErrorMsg}")
            }
            
            return false
        }
        
        def csvData = bodyMatcher.group(1).trim()
        log.info("Extracted CSV data: ${csvData}")
        
        // Split CSV data into columns
        def columns = csvData.split(",")
        
        // Store each column in the corresponding variable
        for (int i = 0; i < varNames.size() && i < columns.length; i++) {
            vars.put(varNames[i], columns[i].trim())
            log.info("Stored column ${i+1} in variable ${varNames[i]}: ${columns[i].trim()}")
        }
        
        // Clear the empty file flag if it exists
        vars.put("STS_FILE_EMPTY", "false")
        
        return true
    } catch (Exception e) {
        log.error("Error reading from STS: ${e.message}")
        e.printStackTrace()
        
        // Set exception error message in all variables
        def exceptionMsg = "${filename}-EXCEPTION_ERROR: ${e.message.take(50)}"
        varNames.each { varName ->
            vars.put(varName, exceptionMsg)
            log.info("Set exception error value for variable ${varName}: ${exceptionMsg}")
        }
        
        // Set error flag
        vars.put("STS_ERROR", "true")
        vars.put("STS_ERROR_MESSAGE", e.message)
        
        return false
    }
}

// Function to add a row to STS
def STS_add(String stsHost, String filename, String addMode, List<String> values) {
    try {
        // Create the CSV line from values
        def line = values.join(",")
        
        // Determine protocol based on sts.use.https property
        def useHttps = Boolean.parseBoolean(props.get("sts.use.https") ?: "false")
        def protocol = useHttps ? "https" : "http"
        
        // Build the STS URL
        def stsUrl = "${protocol}://${stsHost}/sts/ADD"
        
        // Log the full request details
        log.info("==== STS ADD REQUEST ====")
        log.info("URL: ${stsUrl}")
        log.info("Method: POST")
        log.info("ADD_MODE: ${addMode}")
        log.info("FILENAME: ${filename}")
        log.info("LINE: ${line}")
        log.info("========================")
        
        // Create HTTP client and POST request
        def httpClient = createTrustAllHttpClient()
        def httpPost = new HttpPost(stsUrl)
        
        // Add form parameters without encoding
        def params = new ArrayList<BasicNameValuePair>()
        params.add(new BasicNameValuePair("ADD_MODE", addMode))
        params.add(new BasicNameValuePair("FILENAME", filename))
        params.add(new BasicNameValuePair("LINE", line))
        httpPost.setEntity(new UrlEncodedFormEntity(params))
        
        // Execute the request
        log.info("Executing POST request to STS...")
        def response = httpClient.execute(httpPost)
        def statusCode = response.getStatusLine().getStatusCode()
        def statusLine = response.getStatusLine().toString()
        
        // Extract response body
        def responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
        
        // Log the full response
        log.info("==== STS ADD RESPONSE ====")
        log.info("Status: ${statusLine}")
        log.info("Status Code: ${statusCode}")
        log.info("Response Body: ${responseBody}")
        log.info("=========================")
        
        if (statusCode != 200) {
            log.error("STS add request failed with status code: ${statusCode}")
            return false
        }
        
        // Check if response contains success message
        if (responseBody.contains("<title>OK</title>")) {
            log.info("STS add operation successful")
            return true
        } else {
            log.error("STS add request did not return OK")
            return false
        }
    } catch (Exception e) {
        log.error("Error adding to STS: ${e.message}")
        e.printStackTrace()
        return false
    }
}

/**
 * Format the filename according to the naming convention:
 * - Should start with repository name in uppercase (without jm_ prefix and .git extension)
 * - If it doesn't, add the prefix
 * - If it has the prefix but in wrong case, correct it
 * 
 * @param filename The original filename
 * @return The formatted filename
 */
def formatFilename(String filename) {
    try {
        // Get repository name from properties
        def repoName = props.get("GIT_REPO_NAME")
        if (!repoName) {
            log.warn("GIT_REPO_NAME property not set. Cannot format filename according to convention.")
            return filename
        }
        
        // Process repository name:
        // 1. Remove .git extension if present
        if (repoName.toLowerCase().endsWith(".git")) {
            repoName = repoName.substring(0, repoName.length() - 4)
        }
        
        // 2. Remove jm_ or JM_ prefix if present
        if (repoName.toLowerCase().startsWith("jm_")) {
            repoName = repoName.substring(3)
        }
        
        // 3. Convert to uppercase
        def prefix = repoName.toUpperCase()
        
        log.info("Repository prefix for filenames: ${prefix}")
        
        // Check if filename already has the prefix (case insensitive)
        def prefixPattern = "(?i)^${prefix}_"
        if (filename =~ prefixPattern) {
            // Filename has the prefix but might be in wrong case
            // Replace with correct case
            def correctedFilename = filename.replaceFirst(prefixPattern, "${prefix}_")
            
            if (correctedFilename != filename) {
                log.info("Corrected filename case: ${filename} -> ${correctedFilename}")
            }
            
            return correctedFilename
        } else {
            // Filename doesn't have the prefix, add it
            def newFilename = "${prefix}_${filename}"
            log.info("Added repository prefix to filename: ${filename} -> ${newFilename}")
            return newFilename
        }
    } catch (Exception e) {
        log.error("Error formatting filename: ${e.message}")
        // Return original filename if any error occurs
        return filename
    }
}

// Function to create an HTTP client that ignores SSL certificate validation
def createTrustAllHttpClient() {
    // Create a simple SSLContext that trusts all certificates
    def sslContext = SSLContextBuilder.create()
        .loadTrustMaterial(null, new TrustStrategy() {
            @Override
            boolean isTrusted(X509Certificate[] chain, String authType) {
                return true
            }
        })
        .build()
        
    // Create and return an HttpClient with the custom SSLContext
    return HttpClients.custom()
        .setSSLContext(sslContext)
        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
        .build()
} 