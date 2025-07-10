import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId

/*
 * Group-Init.groovy – single-entry pacing and per-thread setup script
 * -------------------------------------------------------------------
 * This script now replaces the former pacing.groovy and group_end.groovy.
 * It executes at the *start* of every iteration inside a Thread Group and:
 *   1. Waits (if needed) so that the previous loop plus think-time meets the
 *      pacing target.
 *   2. Captures the new start-time for this iteration.
 *   3. Reads the pacing specification from the JSR 223 sampler's "Parameters"
 *      field.  You may enter:
 *         • A single integer – e.g. 75  → exactly 75 seconds per iteration.
 *         • A range "min,max" – e.g. 60,90 → randomly chooses a value inside
 *           the inclusive range every loop.
 *      The selected value is stored into the thread-local variable ${pacing}
 *      for logging and re-use.
 *   4. On the *first* iteration for each virtual user, performs once-only
 *      environment checks (e.g. DCU.checkHost()).
 *
 * UK spelling is used in all comments as per project guidelines.
 */

// -------------------------------------------------------------------------
// Detect iteration boundaries to prevent final iteration hanging
// -------------------------------------------------------------------------
def iterationNumCurrent = vars.getIteration()
def threadGroup = ctx.getThreadGroup()
def maxLoops = threadGroup.getLoopController().getLoops()
def isFirstIteration = (iterationNumCurrent == 1)
def isFinalIteration = false

// Determine if this is the final iteration
if (maxLoops > 0) {  // Not infinite loops (-1)
    isFinalIteration = (iterationNumCurrent >= maxLoops)
    log.debug(">>>>> Iteration ${iterationNumCurrent} of ${maxLoops} (Final: ${isFinalIteration})")
} else {
    log.debug(">>>>> Iteration ${iterationNumCurrent} of infinite loops")
}

// -------------------------------------------------------------------------
// Handle pacing wait from previous iteration (logic migrated from group_end)
// -------------------------------------------------------------------------
// Skip pacing wait on first iteration OR if this is the final iteration
def debugSwitch = vars.get("debugSwitch")
if (debugSwitch != "on") {
    def prevStartTimeStr = vars.get("start_time")
    def prevPacingStr = vars.get("pacing")
    
    // Apply pacing wait logic ONLY if:
    // 1. Not the first iteration (no previous timing exists)
    // 2. Previous timing data exists
    // 3. Not the final iteration (prevents hanging after completion)
    if (!isFirstIteration && prevStartTimeStr && prevPacingStr && !isFinalIteration) {
        def prevStartTimeMillis = prevStartTimeStr as Long
        def prevPacingSeconds = prevPacingStr as Double
        def pacingMillisPrev = (prevPacingSeconds * 1000) as Long

        def currentMillis = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        def elapsedMillis = currentMillis - prevStartTimeMillis
        def remainingMillis = pacingMillisPrev - elapsedMillis

        def threadName = ctx.getThreadGroup().getName()
        def threadNum = ctx.getThreadNum()
        def debugMsg = "TG:${threadName}:T${threadNum}:I${iterationNumCurrent}"
        debugMsg += String.format(" Elapsed:%.1fs", elapsedMillis/1000.0)
        debugMsg += String.format(" Pacing:%.1fs", pacingMillisPrev/1000.0)

        if (remainingMillis > 0) {
            debugMsg += String.format(" Waiting:%.1fs", remainingMillis/1000.0)
            log.info(">>>>> ${debugMsg}")
            try {
                Thread.sleep(remainingMillis)
            } catch (InterruptedException e) {
                log.warn(">>>>> Sleep interrupted in thread ${threadName}-${threadNum} - test stopping")
                // Properly handle interruption by restoring interrupt status
                Thread.currentThread().interrupt()
                return  // Exit early if interrupted
            }
        } else {
            debugMsg += " No wait needed"
            log.info(">>>>> ${debugMsg}")
        }
    } else {
        if (isFirstIteration) {
            log.debug(">>>>> First iteration – no pacing wait applied")
        } else if (isFinalIteration) {
            log.info(">>>>> Final iteration detected – skipping pacing wait to prevent hanging")
        } else {
            log.debug(">>>>> Previous timing data not available – no pacing wait applied")
        }
    }
} else {
    log.info(">>>>> Debug mode ON - skipping pacing wait")
}

// -------------------------------------------------------------------------
// Set start time for this iteration (moved after pacing wait)
// -------------------------------------------------------------------------
def startTime = LocalDateTime.now()
vars.put("start_time", startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli().toString())
log.debug(">>>>> Set start time: ${startTime.format(DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'))}")

// -------------------------------------------------------------------------
// Calculate pacing for *this* iteration (logic migrated from pacing.groovy)
// Skip pacing calculation on final iteration since it won't be used
// -------------------------------------------------------------------------
if (!isFinalIteration) {
    // "Parameters" comes directly from the JSR 223 sampler UI.
    // – Single value  → fixed pacing.
    // – "min,max"     → random integer in the inclusive range.
    // Any parsing error will be logged and the raw input preserved.
    def pacingInput = Parameters?.trim()
    if (pacingInput) {
        def pacingValue
        if (pacingInput.contains(',')) {
            def (minStr, maxStr) = pacingInput.split(',').collect { it.trim() }
            try {
                def min = Integer.parseInt(minStr)
                def max = Integer.parseInt(maxStr)
                if (min > max) (min, max) = [max, min]
                pacingValue = min + new Random().nextInt(max - min + 1)
                log.debug(">>>>> Pacing range ${min}-${max}, selected: ${pacingValue}")
            } catch (NumberFormatException e) {
                log.error("Invalid number format in pacing range: ${pacingInput}")
                pacingValue = pacingInput
            }
        } else {
            pacingValue = pacingInput
            log.debug(">>>>> Single pacing value: ${pacingValue}")
        }
        vars.put("pacing", pacingValue.toString())
    }
} else {
    log.info(">>>>> Final iteration – skipping pacing calculation")
    // Clear pacing variable to prevent confusion
    vars.remove("pacing")
}

// Store the current Thread Group name for reference
vars.put("thread_group_name", ctx.getThreadGroup().getName())

// Environment detection is now handled globally in global_setup.groovy

// Gather thread and host info for logging/debugging
def threadNum = ctx.getThreadNum()
def currentThreadGroup = ctx.getThreadGroup().getName()
def hostnameVal = props.get("hostname")

// Store a compact debug message (useful in View Results Tree)
vars.put("debug_msg", "H:${hostnameVal}:G${currentThreadGroup}:T${threadNum}:I${iterationNumCurrent}")
if (isFinalIteration) {
    vars.put("debug_msg", vars.get("debug_msg") + ":FINAL")
}

log.info("Group Setup: Complete for HOSTNAME:${hostnameVal}, Thread ${threadNum} in ${currentThreadGroup} (Iteration ${iterationNumCurrent}${isFinalIteration ? ' - FINAL' : ''})") 