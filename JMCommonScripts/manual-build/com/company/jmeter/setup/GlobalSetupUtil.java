package com.company.jmeter.setup;

import org.apache.jmeter.services.FileServer;
import org.apache.log.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.*;

/**
 * Java counterpart to the former global_setup.groovy.  Invoke once at the start
 * of a test run (e.g. in a JSR-223 Sampler within a JMeter set-up thread group):
 *
 * <pre>
 *     com.company.jmeter.setup.GlobalSetupUtil.run(log, JMeterUtils.getJMeterProperties(), vars);
 * </pre>
 *
 * The routine detects the JMX directory, processes template.* properties, sets
 * V_STS_HOST and Git repository information, so subsequent test elements can
 * rely on them without importing Groovy scripts.
 */
public final class GlobalSetupUtil {

    private GlobalSetupUtil() {}

    public static void run(Logger log, Properties props, Map<String, String> vars) {
        // Property-based feature configuration (defaults to true for backward compatibility)
        boolean templateProcessing = isPropertyEnabled(props, "global.setup.template.processing", true);
        boolean directorySetup = isPropertyEnabled(props, "global.setup.directory.setup", true);
        boolean hostnameDetection = isPropertyEnabled(props, "global.setup.hostname.detection", true);
        boolean environmentDetection = isPropertyEnabled(props, "global.setup.environment.detection", true);
        boolean gitDetection = isPropertyEnabled(props, "global.setup.git.detection", true);
        boolean verboseLogging = isPropertyEnabled(props, "global.setup.logging.verbose", false);

        if (verboseLogging) {
            log.info("GlobalSetupUtil starting with features: template=" + templateProcessing + 
                    ", directory=" + directorySetup + ", hostname=" + hostnameDetection + 
                    ", environment=" + environmentDetection + ", git=" + gitDetection);
        }

        // Execute enabled features
        if (templateProcessing) {
            processTemplateProperties(log, props, vars);
        }

        String jmxDir = null;
        if (directorySetup) {
            jmxDir = determineJmxDir(log, props);
            saveVarsAsProps(log, props, vars, jmxDir, List.of("C_SCRIPTS", "P_SCRIPTS", "DATA"));
        }

        if (hostnameDetection) {
            detectHostname(log, props, vars);
        }

        if (environmentDetection) {
            checkHost(log, props, vars);
        }

        if (gitDetection) {
            // Use determined jmxDir or fall back to getting it now
            if (jmxDir == null) {
                jmxDir = FileServer.getFileServer().getBaseDir();
                if (jmxDir.endsWith("/")) jmxDir = jmxDir.substring(0, jmxDir.length() - 1);
                if (jmxDir.endsWith("/.")) jmxDir = jmxDir.substring(0, jmxDir.length() - 2);
            }
            detectGitRepository(log, props, jmxDir);
        }

        log.info("GlobalSetupUtil completed successfully");
    }

    /**
     * Check if a property is enabled (defaults to specified default if not set)
     */
    private static boolean isPropertyEnabled(Properties props, String propertyName, boolean defaultValue) {
        String value = props.getProperty(propertyName, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(value.trim());
    }

    /* ---------------------------------------------------- */
    private static void processTemplateProperties(Logger log, Properties props, Map<String, String> vars) {
        int count = 0;
        for (String name : props.stringPropertyNames()) {
            if (name.startsWith("template.")) {
                String value = props.getProperty(name, "").trim();
                String varName = name.substring("template.".length());
                vars.put(varName, value);
                props.setProperty(varName, value);
                log.info("Processed template property " + name + " -> " + value);
                count++;
            }
        }
        log.info("Template property processing complete (" + count + " entries)");
    }

    /* ---------------------------------------------------- */
    private static String determineJmxDir(Logger log, Properties props) {
        String baseDir = FileServer.getFileServer().getBaseDir();
        if (baseDir.endsWith("/")) baseDir = baseDir.substring(0, baseDir.length() - 1);
        if (baseDir.endsWith("/.")) baseDir = baseDir.substring(0, baseDir.length() - 2);
        props.setProperty("JMX_DIR", baseDir);
        log.info("JMX_DIR set to " + baseDir);
        return baseDir;
    }

    /* ---------------------------------------------------- */
    private static void saveVarsAsProps(Logger log, Properties props, Map<String, String> vars,
                                        String baseDir, List<String> names) {
        String cleanDir = baseDir.replaceAll("[\\/]$", "");
        for (String n : names) {
            String v = vars.get(n);
            if (v != null) {
                String path = cleanDir + v.trim();
                props.setProperty(n, path);
                log.info("Set property " + n + " = " + path);
            }
        }
    }

    /* ---------------------------------------------------- */
    private static void detectHostname(Logger log, Properties props, Map<String, String> vars) {
        try {
            // Use JMeter's efficient hostname detection (like ${__machineName()})
            String host = System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getHostName();
            // Actually, let's just use the simple hostname
            host = InetAddress.getLocalHost().getHostName();
            
            props.setProperty("hostname", host);
            vars.put("hostname", host);
            log.info("Hostname detected instantly: " + host);
            
            if (host.startsWith("jm-gen-")) {
                int idx = host.indexOf('-', 7);
                if (idx > 7) {
                    String testId = host.substring(7, idx);
                    vars.put("testname", testId);
                    log.info("Extracted test identifier " + testId);
                }
            }
        } catch (Exception e) {
            log.error("Hostname detection failed, using fallback", e);
            // Fallback to system property
            String fallbackHost = System.getProperty("HOSTNAME", System.getProperty("COMPUTERNAME", "localhost"));
            props.setProperty("hostname", fallbackHost);
            vars.put("hostname", fallbackHost);
            log.info("Using fallback hostname: " + fallbackHost);
        }
    }

    /* ---------------------------------------------------- */
    private static void checkHost(Logger log, Properties props, Map<String, String> vars) {
        String host = props.getProperty("hostname", "");
        boolean runningOnCluster = host.startsWith("jm-gen") || host.startsWith("jm-nofrills");
        
        if (runningOnCluster) {
            // Running on cluster nodes (jm-gen-*, jm-nofrills-*) → use internal service URL
            String localHost = props.getProperty("V_STS_LOCAL_HOST", "");
            if (!localHost.isBlank()) {
                props.setProperty("V_STS_HOST", localHost);
                vars.put("V_STS_HOST", localHost);
                log.info("Cluster run (host=" + host + ") → V_STS_HOST set to internal URL: " + localHost);
            }
        } else {
            // Running on external client → use external URL (keep original V_STS_HOST)
            String externalHost = props.getProperty("V_STS_HOST", "");
            if (!externalHost.isBlank()) {
                // V_STS_HOST is already set from template processing, just log it
                log.info("External client run (host=" + host + ") → V_STS_HOST using external URL: " + externalHost);
            } else {
                // Fallback if no V_STS_HOST is set
                String fallback = props.getProperty("V_STS_HOST_DEFAULT", "http://sts.default:8080");
                props.setProperty("V_STS_HOST", fallback);
                vars.put("V_STS_HOST", fallback);
                log.info("External client run (host=" + host + ") → V_STS_HOST set to fallback: " + fallback);
            }
        }
        
        props.setProperty("IS_LOCAL_ENVIRONMENT", String.valueOf(!runningOnCluster));
    }

    /* ---------------------------------------------------- */
    private static void detectGitRepository(Logger log, Properties props, String startDir) {
        Path ptr = Paths.get(startDir).toAbsolutePath();
        while (ptr != null) {
            Path cfg = ptr.resolve(".git").resolve("config");
            if (Files.exists(cfg)) {
                try {
                    List<String> lines = Files.readAllLines(cfg);
                    String url = lines.stream()
                                      .filter(l -> l.contains("url ="))
                                      .map(l -> l.split("=", 2)[1].trim())
                                      .findFirst()
                                      .orElse("Not Found");
                    String name = url.matches(".*[/:]([^/]+)\\.git$")
                                 ? url.replaceAll(".*[/:]([^/]+)\\.git$", "$1")
                                 : ptr.getFileName().toString();
                    props.setProperty("GIT_REPO_PATH", ptr.toString());
                    props.setProperty("GIT_REPO_URL", url);
                    props.setProperty("GIT_REPO_NAME", name);
                    log.info("Git repository detected: " + name + " (url: " + url + ")");
                } catch (IOException ex) {
                    log.warn("Failed to read git config", ex);
                }
                return;
            }
            ptr = ptr.getParent();
        }
        log.info("No Git repository found starting from " + startDir);
    }
} 