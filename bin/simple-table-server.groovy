print("Startup jsr223 groovy script running");

import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.util.JMeterException;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.reflect.ClassTools;
import org.apache.log.Logger;

Logger log = LoggingManager.getLoggerForClass();

// Get all properties from system properties first, fallback to JMeter properties
def getProperty = { String key, def defaultValue ->
    System.getProperty(key, JMeterUtils.getPropDefault(key, defaultValue))
}

// Configure all properties at runtime
def config = [
    port: Integer.parseInt(getProperty("jmeterPlugin.sts.port", "9191")),
    datasetDirectory: getProperty("jmeterPlugin.sts.datasetDirectory", JMeterUtils.getJMeterBinDir()),
    addTimestamp: Boolean.parseBoolean(getProperty("jmeterPlugin.sts.addTimestamp", "true")),
    loadAndRunOnStartup: Boolean.parseBoolean(getProperty("jmeterPlugin.sts.loadAndRunOnStartup", "true")),
    initFileAtStartup: getProperty("jmeterPlugin.sts.initFileAtStartup", ""),
    initFileAtStartupRegex: Boolean.parseBoolean(getProperty("jmeterPlugin.sts.initFileAtStartupRegex", "false"))
]

// Log actual configuration
log.info("Using configuration:")
config.each { k,v -> log.info("${k}: ${v}") }

if (config.port > 0 && config.loadAndRunOnStartup) {
    log.info("Starting Simple Table server (" + config.port + ")");
    try {
        Object instance = ClassTools
                .construct("org.jmeterplugins.protocol.http.control.HttpSimpleTableControl");
        ClassTools.invoke(instance, "startHttpSimpleTable");
        msg = "Simple Table Server is running on port : " + config.port;
        log.info(msg);
        print(msg);
        
        if (config.initFileAtStartup) {
            log.info("INITFILE at STS startup")
            print("INITFILE at STS startup")
            log.info("jmeterPlugin.sts.initFileAtStartup=" + config.initFileAtStartup);
            log.info("jmeterPlugin.sts.initFileAtStartupRegex=" + config.initFileAtStartupRegex);
        }
        
        if (config.initFileAtStartup && !config.initFileAtStartupRegex) {
            def tabFileName = org.apache.commons.lang3.StringUtils.splitPreserveAllTokens(config.initFileAtStartup,',');
            for (int i = 0; i < tabFileName.length; i++) {
                String fileName = tabFileName[i].trim();
                log.info("INITFILE : i = " + i + ", fileName = " + fileName);
                String response = new URL("http://localhost:" + config.port +"/sts/INITFILE?FILENAME=" + fileName).text;
                log.info("INITFILE?FILENAME=" + fileName + ", response=" + response);
                print("INITFILE?FILENAME=" + fileName + ", response=" + response);
            }
        }
     } catch (JMeterException e) {
        log.warn("Could not start Simple Table server", e);
    }
}
else {
    msg = "jmeterPlugin.sts.port == 0 OR jmeterPlugin.sts.loadAndRunOnStartup != true => Simple Table Server is NOT running";
    log.info(msg);
    print(msg);
}

print("Startup jsr223 groovy script completed");

