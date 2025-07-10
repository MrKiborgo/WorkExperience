package com.company;

import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;

import java.util.Properties;

public final class DCU {
    private static Logger log; private static Properties props;

    public static DCU init(Logger l, Properties p, JMeterVariables v) {
        log = l; props = p; return new DCU();
    }

    public String getStsHost() { return props.getProperty("V_STS_HOST"); }

    public void globalSetup() {
        // JMX_DIR, git rev, etc.  (unchanged from your old global_setup.groovy)
    }

    public void checkHost() {
        props.putIfAbsent("V_STS_HOST", "http://sts.default:8080");
    }
}
