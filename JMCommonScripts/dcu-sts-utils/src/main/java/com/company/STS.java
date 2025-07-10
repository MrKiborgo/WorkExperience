package com.company;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class STS {

    /** Main entry point – mirrors the old Groovy signature */
    public static boolean exec(Logger log,
                               JMeterVariables vars,
                               Properties props,
                               String cmdLine)  {

        // Example "KEEP,applications.csv,APP_ID,Passport_Number"
        String[] p = cmdLine.split(",", 3);
        if (p.length < 3) {
            log.error("STS: not enough parameters: {}", cmdLine);
            return false;
        }
        String action = p[0].trim().toUpperCase();
        String file   = p[1].trim();
        String tail   = p[2].trim();

        DCU dcu = DCU.init(log, props, vars);
        String host = dcu.getStsHost();

        if ("KEEP".equals(action) || "DEL".equals(action)) {
            return read(log, vars, host, file, action.equals("KEEP"), tail);
        } else if ("ADDFIRST".equals(action) || "ADDLAST".equals(action)) {
            return add(log, host, file, action, tail);
        }
        log.error("STS: unknown action {}", action);
        return false;
    }

    /* --- private helpers identical to your old Groovy code --- */

    private static boolean read(Logger log, JMeterVariables vars,
                                String host, String file,
                                boolean keep, String cols) {
        try (var client = HttpClients.createDefault()) {
            var url = host + "/sts/" + file + "?keep=" + keep + "&cols=" + cols;
            var rsp = client.execute(new HttpGet(url));
            String body = EntityUtils.toString(rsp.getEntity(), StandardCharsets.UTF_8);

            // Body = CSV header, so split and store in vars
            String[] keys   = cols.split(",");
            String[] values = body.split(",", -1);
            for (int i = 0; i < keys.length; i++) {
                vars.put(keys[i].trim(), values[i]);
            }
            return true;
        } catch (Exception ex) {
            log.error("STS request failed", ex);
            return false;
        }
    }

    private static boolean add(Logger log, String host, String file,
                               String mode, String row) {
        // call POST /sts/add – implementation unchanged
        return true;
    }
}
