# Pacing Configuration Element

## Overview
The **Pacing Configuration** element provides a clean GUI-based way to configure thread pacing (think time between iterations) in JMeter, replacing the need for JSR223 sampler Parameters and portions of the `group_init.groovy` script.

## Features
- **Fixed Pacing**: Set a single value for consistent think time
- **Random Pacing**: Set min/max range for variable think time
- **Real-time Preview**: See exactly what your configuration will do
- **Easy Setup**: No more manual parameter entry in JSR223 samplers
- **Debug Support**: Respects `debugSwitch=on` to skip pacing waits

## Usage

### 1. Add Pacing Configuration Element
1. Right-click Thread Group
2. Choose **Add → Config Element → Pacing Configuration**
3. Configure your pacing settings:
   - **Min Pacing**: Required field (seconds)
   - **Max Pacing**: Optional field (seconds)
   - **Enable Pacing**: Checkbox to enable/disable

### 2. Configuration Options

#### Fixed Pacing
- **Min Pacing**: `60` (seconds)
- **Max Pacing**: Leave empty
- **Result**: Exactly 60 seconds between each iteration

#### Random Pacing
- **Min Pacing**: `45` (seconds)  
- **Max Pacing**: `75` (seconds)
- **Result**: Random value between 45-75 seconds (inclusive) for each iteration

#### Disable Pacing
- **Enable Pacing**: Unchecked
- **Result**: No think time applied (iterations run back-to-back)

### 3. Preview Panel
The configuration preview shows exactly what will happen:
- `Fixed pacing: exactly 60 seconds between each iteration`
- `Random pacing: between 45 and 75 seconds (inclusive) - randomly selected each iteration`
- `⚠ Min Pacing is required when pacing is enabled`

## Replacement of group_init.groovy

### What You Can Remove
The Pacing Configuration element **replaces** these sections from `group_init.groovy`:

```groovy
// ❌ DELETE: This pacing calculation logic is no longer needed
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
```

```groovy
// ❌ DELETE: This pacing wait logic is also handled by the element
def debugSwitch = vars.get("debugSwitch")
if (debugSwitch != "on") {
    def prevStartTimeStr = vars.get("start_time")
    def prevPacingStr = vars.get("pacing")
    if (prevStartTimeStr && prevPacingStr) {
        // ... entire pacing wait block can be removed
    }
}
```

### What You Can Keep
These parts of `group_init.groovy` are still useful:
- Environment detection logic
- Thread group name setting
- Debug message creation
- First iteration detection
- Any other custom setup logic

## Variables Set
The Pacing Configuration element sets these JMeter variables:
- `start_time`: Timestamp when iteration started (milliseconds)
- `pacing`: Selected pacing value for this iteration (seconds)

## Migration Steps

### Step 1: Add Pacing Configuration
1. Add the **Pacing Configuration** element to your Thread Group
2. Configure your min/max pacing values
3. Test that pacing works as expected

### Step 2: Simplify group_init.groovy
1. Remove the pacing calculation code (shown above)
2. Remove the pacing wait logic (shown above)  
3. Keep any other setup logic you need

### Step 3: Remove JSR223 Parameters
1. Clear the **Parameters** field in any JSR223 samplers that were used for pacing
2. The JSR223 sampler can now focus purely on its intended logic

## Benefits

### Before (JSR223 Parameters)
```
Parameters: 60,90
```
- Manual parameter entry
- No validation
- Hard to see what's configured
- Mixed with sampler logic

### After (Pacing Configuration)
```
[GUI Element]
Min Pacing: 60
Max Pacing: 90
Enable Pacing: ✓

Preview: Random pacing: between 60 and 90 seconds (inclusive) - randomly selected each iteration
```
- Clean GUI interface
- Real-time validation
- Clear preview of behaviour
- Separate from sampler logic
- Reusable across thread groups

## Debug Support
When `debugSwitch=on` is set in JMeter variables, the Pacing Configuration element will:
- Skip all pacing waits (same as original behaviour)
- Log: `>>>>> Debug mode ON - skipping pacing wait`
- Still set `start_time` and `pacing` variables for consistency

## Backwards Compatibility
The Pacing Configuration element can be used alongside existing `group_init.groovy` scripts during migration. The element will take precedence for pacing logic, but won't interfere with other functionality in the Groovy script. 