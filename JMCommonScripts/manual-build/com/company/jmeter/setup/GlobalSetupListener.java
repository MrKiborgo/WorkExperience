package com.company.jmeter.setup;

import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Auto-invoked listener that executes GlobalSetupUtil.run() exactly once at the
 * very beginning of the test (before any Thread Groups are started).  To enable
 * it without modifying the test plan, add the following line to
 * <JMETER_HOME>/bin/user.properties (or jmeter.properties):
 *
 *     global.setup.listener=com.company.jmeter.setup.GlobalSetupListener
 *
 * Then add the same property key to JVM system properties when launching
 * JMeter in non-GUI/CI mode, e.g.  -Dglobal.setup.listener=... if needed.
 *
 * When JMeter boots it reads the property, instantiates the class via
 * Class.forName(..).newInstance(), and keeps it as an engine-level listener.
 */
public class GlobalSetupListener implements TestStateListener {

    private static final Logger LOG = LoggingManager.getLoggerForClass();
    private static volatile boolean done = false;

    @Override public void testStarted() { testStarted("local"); }

    @Override public void testStarted(String host) {
        if (done) return;
        done = true;
        LOG.info("GlobalSetupListener triggered (host=" + host + ")");

        Properties props = JMeterUtils.getJMeterProperties();
        Map<String,String> stubVars = new HashMap<>();   // no thread context yet
        GlobalSetupUtil.run(LOG, props, stubVars);
    }

    @Override public void testEnded() { /* no-op */ }
    @Override public void testEnded(String host) { /* no-op */ }
} 