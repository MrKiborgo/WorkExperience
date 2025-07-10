package com.company;

import com.company.jmeter.setup.GlobalSetupUtil;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.log.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class DCU {
    private static Logger log; 
    private static Properties props;
    private static JMeterVariables vars;

    public static DCU init(Logger l, Properties p, JMeterVariables v) {
        log = l; 
        props = p; 
        vars = v;
        return new DCU();
    }

    public String getStsHost() { return props.getProperty("V_STS_HOST"); }

    public void globalSetup() {
        // Run the comprehensive global setup using our new GlobalSetupUtil
        if (log != null && props != null) {
            try {
                // Create a stub vars map if vars is null (for GlobalSetupListener context)
                Map<String, String> varsMap = new HashMap<>();
                if (vars != null) {
                    // Copy existing variables if available
                    for (Map.Entry<String, Object> entry : vars.entrySet()) {
                        varsMap.put(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
                
                log.info("DCU: Running global setup...");
                GlobalSetupUtil.run(log, props, varsMap);
                
                // Copy back any new variables if vars is available
                if (vars != null) {
                    for (Map.Entry<String, String> entry : varsMap.entrySet()) {
                        vars.put(entry.getKey(), entry.getValue());
                    }
                }
                
                log.info("DCU: Global setup completed");
            } catch (Exception e) {
                log.error("DCU: Error during global setup", e);
            }
        }
    }

    public void checkHost() {
        props.putIfAbsent("V_STS_HOST", "http://sts.default:8080");
    }
}
