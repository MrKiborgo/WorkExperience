package com.company.jmeter.setup;

import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Utility that mirrors the original group_init.groovy pacing logic. Call at the
 * very top of each loop (JSR-223 PreProcessor or Sampler) to enforce pacing.
 *
 * Example usage inside a JSR-223 PreProcessor:
 * <pre>
 *     com.company.jmeter.setup.GroupInitUtil.applyPacing(log, vars, ctx, Parameters)
 * </pre>
 */
public final class GroupInitUtil {

    private GroupInitUtil() {}

    /**
     * Apply pacing and store bookkeeping vars.
     *
     * @param log  SLF4J logger from JMeter
     * @param vars Thread-local JMeter variables
     * @param ctx  JMeterContext (to get thread info)
     * @param pacingParam Raw Parameters string from the JSR-223 element – may be
     *                    fixed seconds or "min,max" range
     */
    public static void applyPacing(Logger log, JMeterVariables vars, JMeterContext ctx, String pacingParam) {
        // Optional debug switch to skip the wait
        boolean debug = "on".equals(vars.get("debugSwitch"));

        if (!debug) {
            String prevStart   = vars.get("start_time");
            String prevPacingS = vars.get("pacing");
            if (prevStart != null && prevPacingS != null) {
                long prevStartMillis  = Long.parseLong(prevStart);
                double pacingSeconds  = Double.parseDouble(prevPacingS);
                long pacingMillisPrev = (long) (pacingSeconds * 1000);

                long now      = System.currentTimeMillis();
                long elapsed  = now - prevStartMillis;
                long remaining = pacingMillisPrev - elapsed;

                if (remaining > 0) {
                    if (log != null) {
                        log.info("Pacing – sleeping {} ms (elapsed {} ms)", remaining, elapsed);
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(remaining);
                    } catch (InterruptedException ie) {
                        if (log != null) {
                            log.warn("Pacing sleep interrupted", ie);
                        }
                        Thread.currentThread().interrupt();
                    }
                } else {
                    if (log != null) {
                        log.info("Pacing – no wait needed (elapsed {} ms)", elapsed);
                    }
                }
            }
        } else {
            if (log != null) {
                log.info("Debug mode – pacing wait skipped");
            }
        }

        /* --------- calculate pacing for *this* iteration ---------- */
        if (pacingParam != null && !pacingParam.isBlank()) {
            int seconds;
            if (pacingParam.contains(",")) {
                String[] split = pacingParam.split(",", 2);
                int min = Integer.parseInt(split[0].trim());
                int max = Integer.parseInt(split[1].trim());
                if (min > max) { int tmp = min; min = max; max = tmp; }
                seconds = min + new Random().nextInt(max - min + 1);
            } else {
                seconds = Integer.parseInt(pacingParam.trim());
            }
            vars.put("pacing", String.valueOf(seconds));
        }

        /* ------- store start time & debug message for this loop ----- */
        long startMillis = System.currentTimeMillis();
        vars.put("start_time", String.valueOf(startMillis));

        int threadNum    = ctx.getThreadNum();
        String tgName    = ctx.getThreadGroup().getName();
        int iterationNum = vars.getIteration();
        String hostname  = vars.get("hostname");

        vars.put("debug_msg", String.format("H:%s:G%s:T%d:I%d", hostname, tgName, threadNum, iterationNum));
        if (log != null) {
            log.debug("Start time recorded: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
    }
} 