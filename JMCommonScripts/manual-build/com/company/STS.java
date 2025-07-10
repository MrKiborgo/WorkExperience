package com.company;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.log.Logger;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Properties;

public final class STS {

    /** Main entry point – mirrors the old Groovy signature */
    public static boolean exec(Logger log,
                               JMeterVariables vars,
                               Properties props,
                               String cmdLine)  {

        // Parse parameters
        String[] params = cmdLine.split(",");
        if (params.length < 3) {
            log.error("STS: not enough parameters: " + cmdLine);
            return false;
        }
        
        String action = params[0].trim().toUpperCase();
        String originalFilename = params[1].trim();
        
        // Format filename according to convention
        String filename = formatFilename(originalFilename, props, log);
        
        DCU dcu = DCU.init(log, props, vars);
        String host = dcu.getStsHost();
        if (host == null) {
            log.error("STS: V_STS_HOST not available. Cannot connect to Simple Table Server.");
            return false;
        }

        if ("KEEP".equals(action) || "DEL".equals(action)) {
            // For read operations, remaining parameters are variable names
            String[] varNames = new String[params.length - 2];
            System.arraycopy(params, 2, varNames, 0, params.length - 2);
            return read(log, vars, props, host, filename, "KEEP".equals(action), varNames);
        } else if ("ADDFIRST".equals(action) || "ADDLAST".equals(action)) {
            // For add operations, remaining parameters are values to add
            String[] values = new String[params.length - 2];
            System.arraycopy(params, 2, values, 0, params.length - 2);
            String addMode = "ADDFIRST".equals(action) ? "FIRST" : "LAST";
            return add(log, props, host, filename, addMode, values);
        }
        log.error("STS: unknown action " + action);
        return false;
    }

    /* --- private helpers implementing the STS API --- */

    private static boolean read(Logger log, JMeterVariables vars, Properties props,
                                String host, String filename, boolean keep, String[] varNames) {
        try {
            // Determine protocol based on sts.use.https property
            boolean useHttps = Boolean.parseBoolean(props.getProperty("sts.use.https", "false"));
            String protocol = useHttps ? "https" : "http";
            
            // Build the STS URL
            String keepValue = keep ? "TRUE" : "FALSE";
            String stsUrl = protocol + "://" + host + "/sts/READ?READ_MODE=FIRST&KEEP=" + keepValue + "&FILENAME=" + filename;
            
            log.debug("==== STS READ REQUEST ====");
            log.debug("URL: " + stsUrl);
            log.debug("Method: GET");
            log.debug("READ_MODE: FIRST");
            log.debug("KEEP: " + keepValue);
            log.debug("FILENAME: " + filename);
            log.debug("=========================");
            
            // Create HTTP client and execute request
            var httpClient = createTrustAllHttpClient();
            var httpGet = new HttpGet(stsUrl);
            
            var response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            String statusLine = response.getStatusLine().toString();
            
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log.debug("==== STS READ RESPONSE ====");
            log.debug("Status: " + statusLine);
            log.debug("Status Code: " + statusCode);
            log.debug("Response Body: " + responseBody);
            log.debug("==========================");
            
            if (statusCode != 200) {
                log.error("STS request failed with status code: " + statusCode);
                // Set error message in all variables
                String errorMsg = filename + "-HTTP_ERROR_" + statusCode;
                for (String varName : varNames) {
                    vars.put(varName, errorMsg);
                    log.info("Set error value for variable " + varName + ": " + errorMsg);
                }
                return false;
            }
            
            // Check for empty file response
            if (responseBody.contains("<title>KO</title>") && responseBody.contains("Error : No more line !")) {
                log.warn("No more data available in file: " + filename);
                String emptyFileMsg = filename + "-EMPTY_FILE_ERROR";
                for (String varName : varNames) {
                    vars.put(varName, emptyFileMsg);
                    log.info("Set empty file error value for variable " + varName + ": " + emptyFileMsg);
                }
                vars.put("STS_FILE_EMPTY", "true");
                vars.put("STS_FILE_EMPTY_NAME", filename);
                return false;
            }
            
            // Extract data from HTML response
            String bodyPattern = "<body>(.*?)</body>";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(bodyPattern, java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher matcher = pattern.matcher(responseBody);
            
            if (!matcher.find()) {
                log.error("Could not extract data from STS response");
                String parseErrorMsg = filename + "-PARSE_ERROR";
                for (String varName : varNames) {
                    vars.put(varName, parseErrorMsg);
                    log.info("Set parse error value for variable " + varName + ": " + parseErrorMsg);
                }
                return false;
            }
            
            String csvData = matcher.group(1).trim();
            log.info("Extracted CSV data: " + csvData);
            
            // Split CSV data into columns
            String[] columns = csvData.split(",");
            
            // Store each column in the corresponding variable
            for (int i = 0; i < varNames.length && i < columns.length; i++) {
                vars.put(varNames[i], columns[i].trim());
                log.debug("Stored column " + (i+1) + " in variable " + varNames[i] + ": " + columns[i].trim());
            }
            
            // Clear the empty file flag if it exists
            vars.put("STS_FILE_EMPTY", "false");
            
            return true;
        } catch (Exception e) {
            log.error("Error reading from STS: " + e.getMessage(), e);
            
            // Set exception error message in all variables
            String exceptionMsg = filename + "-EXCEPTION_ERROR: " + e.getMessage().substring(0, Math.min(50, e.getMessage().length()));
            for (String varName : varNames) {
                vars.put(varName, exceptionMsg);
                log.info("Set exception error value for variable " + varName + ": " + exceptionMsg);
            }
            
            vars.put("STS_ERROR", "true");
            vars.put("STS_ERROR_MESSAGE", e.getMessage());
            
            return false;
        }
    }

    private static boolean add(Logger log, Properties props, String host, String filename,
                               String addMode, String[] values) {
        try {
            // Create the CSV line from values
            String line = String.join(",", values);
            
            // Determine protocol based on sts.use.https property
            boolean useHttps = Boolean.parseBoolean(props.getProperty("sts.use.https", "false"));
            String protocol = useHttps ? "https" : "http";
            
            // Build the STS URL
            String stsUrl = protocol + "://" + host + "/sts/ADD";
            
            log.debug("==== STS ADD REQUEST ====");
            log.debug("URL: " + stsUrl);
            log.debug("Method: POST");
            log.debug("ADD_MODE: " + addMode);
            log.debug("FILENAME: " + filename);
            log.debug("LINE: " + line);
            log.debug("========================");
            
            // Create HTTP client and POST request
            var httpClient = createTrustAllHttpClient();
            var httpPost = new HttpPost(stsUrl);
            
            // Add form parameters
            var params = new ArrayList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("ADD_MODE", addMode));
            params.add(new BasicNameValuePair("FILENAME", filename));
            params.add(new BasicNameValuePair("LINE", line));
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            
            // Execute the request
            log.info("Executing POST request to STS...");
            var response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            String statusLine = response.getStatusLine().toString();
            
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            log.debug("==== STS ADD RESPONSE ====");
            log.debug("Status: " + statusLine);
            log.debug("Status Code: " + statusCode);
            log.debug("Response Body: " + responseBody);
            log.debug("=========================");
            
            if (statusCode != 200) {
                log.error("STS add request failed with status code: " + statusCode);
                return false;
            }
            
            // Check if response contains success message
            if (responseBody.contains("<title>OK</title>")) {
                log.info("STS add operation successful");
                return true;
            } else {
                log.error("STS add request did not return OK");
                return false;
            }
        } catch (Exception e) {
            log.error("Error adding to STS: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Format the filename according to the naming convention:
     * - Should start with repository name in uppercase (without jm_ prefix and .git extension)
     * - If it doesn't, add the prefix
     * - If it has the prefix but in wrong case, correct it
     */
    private static String formatFilename(String filename, Properties props, Logger log) {
        try {
            // Get repository name from properties
            String repoName = props.getProperty("GIT_REPO_NAME");
            if (repoName == null) {
                log.warn("GIT_REPO_NAME property not set. Cannot format filename according to convention.");
                return filename;
            }
            
            // Process repository name:
            // 1. Remove .git extension if present
            if (repoName.toLowerCase().endsWith(".git")) {
                repoName = repoName.substring(0, repoName.length() - 4);
            }
            
            // 2. Remove jm_ or JM_ prefix if present
            if (repoName.toLowerCase().startsWith("jm_")) {
                repoName = repoName.substring(3);
            }
            
            // 3. Convert to uppercase
            String prefix = repoName.toUpperCase();
            
            log.debug("Repository prefix for filenames: " + prefix);
            
            // Check if filename already has the prefix (case insensitive)
            String prefixPattern = "(?i)^" + prefix + "_";
            if (filename.matches(prefixPattern + ".*")) {
                // Filename has the prefix but might be in wrong case
                // Replace with correct case
                String correctedFilename = filename.replaceFirst(prefixPattern, prefix + "_");
                
                if (!correctedFilename.equals(filename)) {
                    log.info("Corrected filename case: " + filename + " -> " + correctedFilename);
                }
                
                return correctedFilename;
            } else {
                // Filename doesn't have the prefix, add it
                String newFilename = prefix + "_" + filename;
                log.info("Added repository prefix to filename: " + filename + " -> " + newFilename);
                return newFilename;
            }
        } catch (Exception e) {
            log.error("Error formatting filename: " + e.getMessage());
            // Return original filename if any error occurs
            return filename;
        }
    }

    /**
     * Create an HTTP client that ignores SSL certificate validation
     */
    private static org.apache.http.client.HttpClient createTrustAllHttpClient() {
        try {
            // Create a simple SSLContext that trusts all certificates
            var sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null, new TrustStrategy() {
                    @Override
                    public boolean isTrusted(X509Certificate[] chain, String authType) {
                        return true;
                    }
                })
                .build();
                
            // Create and return an HttpClient with the custom SSLContext
            return HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
        } catch (Exception e) {
            // Fallback to default client if SSL setup fails
            return HttpClients.createDefault();
        }
    }
}
