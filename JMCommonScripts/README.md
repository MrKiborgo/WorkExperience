# JMeter Common Scripts

This repository contains Groovy scripts for JMeter test plans, providing reusable functionality for test setup, data management, pacing, and integration with external services.

## Scripts

### `global_setup.groovy`
**Purpose:** Initialises the test environment and sets up essential properties and directories.

**Key Functions:**
- Processes template properties from configuration
- Sets up JMX directory and related file paths
- Saves common variables as properties with full paths
- Detects hostname and extracts test names from hostname patterns
- Imports and initialises the DCU module

**Usage:** Should be run once at the start of any test plan as a Setup Thread Group.

### `DCU.groovy` (Dynamic Configuration Utility)
**Purpose:** A module that provides common functionality for hostname detection, Git repository discovery, and configuration management.

**Key Functions:**
- **Global Setup:** Detects hostname and Git repository information
- **Host Detection:** Determines if running locally or in remote environment
- **STS Host Management:** Manages Simple Table Server host configuration
- **Git Repository Discovery:** Finds and extracts Git repository information
- **Counter Implementation:** Flexible counter with thread-local or global policies
- **Property Management:** Utilities for setting and getting properties

**Usage:** Imported by other scripts. Call `dcu.init(log, props, vars)` to initialise.

### `STS.groovy` (Simple Table Server)
**Purpose:** Utility for reading from and writing to CSV files managed by a Simple Table Server.

**Supported Operations:**
- **KEEP:** Read first row from CSV file and keep it (stores columns in variables)
- **DEL:** Read first row from CSV file and delete it (stores columns in variables)
- **ADDFIRST:** Add a new row to the beginning of a CSV file
- **ADDLAST:** Add a new row to the end of a CSV file

**Usage Example:**
```
KEEP,applications.csv,APP_ID,Passport_Number,Name
DEL,applications.csv,APP_ID,Passport_Number,Name
ADDFIRST,applications.csv,value1,value2,value3
ADDLAST,applications.csv,value1,value2,value3
```

**Features:**
- Automatic filename formatting based on Git repository name
- SSL certificate validation bypassing for testing environments
- Comprehensive error handling with descriptive error messages
- Empty file detection and handling

### `group_init.groovy`
**Purpose:** Initialises thread group execution with timing and environment setup.

**Functions:**
- Records start time for pacing calculations
- Sets up thread group name and debugging information
- Initialises DCU module and checks host environment
- Creates debug messages with hostname, thread group, thread number, and iteration

**Usage:** Place at the beginning of each Thread Group.

### `group_end.groovy`
**Purpose:** Handles pacing at the end of thread group iterations.

**Functions:**
- Calculates elapsed time since thread group start
- Applies pacing delays to maintain consistent iteration timing
- Respects debug mode to skip pacing when testing
- Provides detailed logging of timing information

**Usage:** Place at the end of each Thread Group that requires pacing.

### `pacing.groovy`
**Purpose:** Sets pacing values for thread group execution timing.

**Supported Formats:**
- **Single Value:** `75` (sets pacing to exactly 75 seconds)
- **Range:** `60,80` (sets random pacing between 60-80 seconds inclusive)

**Features:**
- Auto-correction if min/max values are reversed
- Random value generation within specified ranges
- Stores result in `${pacing}` variable for use by `group_end.groovy`

**Usage:** Enter value in Parameters field of JSR223 element.

### `example_sts.groovy`
**Purpose:** Demonstrates how to use the STS module programmatically.

**Example Usage:**
- Shows how to import and initialise the STS module
- Demonstrates reading from STS and accessing stored variables
- Provides error handling patterns

## Typical Usage Pattern

1. **Test Plan Setup:**
   - Add Setup Thread Group with `global_setup.groovy`

2. **Thread Groups:**
   - Start each Thread Group with `group_init.groovy`
   - Use `STS.groovy` for data management as needed
   - Use `pacing.groovy` to set iteration timing
   - End each Thread Group with `group_end.groovy`

3. **Data Management:**
   - Use STS operations for reading/writing shared test data
   - Leverage automatic filename formatting and error handling

## Requirements

- Apache JMeter 5.x or higher
- Simple Table Server (for STS operations)
- Git repository (for automatic configuration)

## Notes

- Scripts use UK spelling in comments and documentation
- Designed to be modular and reusable
- Comprehensive logging for debugging
- Descriptive error handling for troubleshooting 