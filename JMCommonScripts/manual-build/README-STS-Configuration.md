# STS (Simple Table Server) - Consolidated Module

## Overview

The STS module is now a single, self-contained Java class that handles:
- Automatic hostname detection
- Environment-based STS URL configuration  
- Multiple property name fallbacks (including template.* properties)
- Robust error handling and logging

## Hostname-Based Environment Detection

### Remote Environment (jm-gen* or jm-nofrills*)
If the hostname starts with `jm-gen` or `jm-nofrills`:
- **Detected as**: Remote execution environment
- **STS Host Sources** (tried in order):
  1. `V_STS_LOCAL_HOST`
  2. `template.V_STS_LOCAL_HOST` ✅ **Supports user.properties templates!**
  3. `STS_LOCAL_HOST`

### Local Environment (anything else)
If the hostname doesn't match remote patterns:
- **Detected as**: Local development environment  
- **STS Host Sources** (tried in order):
  1. `V_STS_HOST_DEFAULT`
  2. `template.V_STS_HOST_DEFAULT`
  3. `STS_HOST_DEFAULT`
- **Fallback**: `http://localhost:8080`

## Configuration Options

### Option 1: Template Properties in user.properties (Recommended)
Your existing `bin/user.properties` template setup will work automatically:

```properties
template.V_STS_HOST=simple-table-server-ho-hmpo-ew2tlg1nl1-i-team-nft-i-docker-tlg1.np.ebsa.homeoffice.gov.uk
template.V_STS_LOCAL_HOST=simple-table-server-ho-hmpo-ew2tlg1nl1-i-team-nft-i-docker-tlg1.service.np.ebsa.local
```

### Option 2: Direct Properties
```properties
V_STS_LOCAL_HOST=https://your-remote-sts-server.com:8080
V_STS_HOST_DEFAULT=http://localhost:8080
```

### Option 3: Manual Override
```properties
V_STS_HOST=https://your-sts-server.com:8080
```

## Usage in JMeter Scripts

```groovy
import com.company.STS

// The STS.exec() method will automatically:
// 1. Check if V_STS_HOST is already configured
// 2. If not, detect the current hostname
// 3. Determine if running locally or remotely
// 4. Try multiple property names (including template.* from user.properties)
// 5. Set the appropriate STS server URL
// 6. Execute the STS operation

STS.exec(log, vars, props, "KEEP,applications.csv,APP_ID,Passport_Number")
```

## Property Fallback Chain

The module tries properties in this order based on environment:

### Remote Environment (jm-gen*/jm-nofrills*)
1. `V_STS_LOCAL_HOST`
2. `template.V_STS_LOCAL_HOST` ← **Your user.properties template**
3. `STS_LOCAL_HOST`
4. Falls back to general fallback chain

### Local Environment
1. `V_STS_HOST_DEFAULT`
2. `template.V_STS_HOST_DEFAULT`
3. `STS_HOST_DEFAULT`
4. `http://localhost:8080` (hardcoded fallback)

### General Fallback Chain
1. `template.V_STS_HOST` ← **Your user.properties template**
2. `V_STS_HOST`
3. `STS_HOST`
4. `template.V_STS_LOCAL_HOST`
5. `V_STS_LOCAL_HOST`
6. `http://localhost:8080` (final fallback)

## Debug Information

The implementation provides comprehensive logging:

```
STS: V_STS_HOST not set, detecting environment...
STS: Hostname detected and stored: jm-gen-mytest-01
STS: >>>>>>>>>>>>>>>>>>>>>>>>>> HOSTNAME: jm-gen-mytest-01
STS: >>>>> Detected remote execution environment (jm-gen-mytest-01)
STS: Found property template.V_STS_LOCAL_HOST = simple-table-server-ho-hmpo-ew2tlg1nl1-i-team-nft-i-docker-tlg1.service.np.ebsa.local
STS: >>>>> Set V_STS_HOST to remote value: simple-table-server-ho-hmpo-ew2tlg1nl1-i-team-nft-i-docker-tlg1.service.np.ebsa.local
STS: Executing GET request: https://simple-table-server.../sts/READ?READ_MODE=FIRST&KEEP=true&FILENAME=applications.csv
```

## Advantages of Consolidated Approach

✅ **Single Module**: No DCU dependency, everything in one place  
✅ **Template Support**: Automatically handles `template.*` properties from user.properties  
✅ **Multiple Fallbacks**: Tries many property name variations  
✅ **Self-Contained**: Works independently of JMeter property loading issues  
✅ **Better Logging**: Detailed debug information for troubleshooting  
✅ **Robust**: Handles missing properties gracefully with fallbacks  

## Troubleshooting

### "Target host is not specified" Error
This error should no longer occur due to multiple fallbacks, but if it does:

1. **Check Logs**: Look for hostname detection and property resolution messages
2. **Verify Properties**: Ensure your `user.properties` contains the template properties
3. **Manual Override**: Set `V_STS_HOST` directly as a temporary workaround

### Property Loading Issues
If template properties aren't loading from user.properties:
- The module will log which properties it's looking for
- It will try non-template versions as fallbacks
- It will use localhost as final fallback for local environments

## Your Current Configuration

Based on your `user.properties`, this should work automatically:
- Remote environments will use: `simple-table-server-ho-hmpo-ew2tlg1nl1-i-team-nft-i-docker-tlg1.service.np.ebsa.local`
- Local environments will use: `http://localhost:8080` (fallback)

## Compatibility
- Compiled with Java 11 for compatibility with Java 11, 17, and 21
- Works in Alpine containers running Java 17
- Backwards compatible with existing Groovy-based configurations 

## Filename Formatting & Repository Integration

The STS implementation automatically formats filenames according to repository naming conventions while giving users complete flexibility in how they name their local CSV files.

### User-Friendly Design Philosophy
- **Name Files However You Want**: Users can use simple names like `users.csv`, `applications.csv`, or `data.csv` locally
- **Automatic Server-Side Prefixing**: STS requests automatically add repository prefixes for proper namespace separation
- **No Duplication**: Smart logic prevents double-prefixing (e.g., `MYPROJECT_users.csv` won't become `MYPROJECT_MYPROJECT_users.csv`)
- **Pipeline Consistency**: Ensures all STS server files follow naming conventions regardless of local file names

### Automatic Filename Processing
- **Repository Detection**: Automatically detects Git repository information from the JMeter test plan directory
- **Smart Prefix Addition**: Adds repository prefix only when needed (e.g., `users.csv` → `MYPROJECT_users.csv`)  
- **Case Correction**: Ensures prefix uses correct uppercase format
- **Namespace Separation**: Prevents filename conflicts across different repositories/pipelines

### How It Works
1. **JMX Directory Detection**: Uses `FileServer.getFileServer().getBaseDir()` to get actual test plan directory (not JMeter installation directory)
2. **Git Repository Scanning**: Searches up the directory tree from JMX test plan location to find `.git` folder
3. **Repository Name Extraction**: Reads `.git/config` to get repository URL/name
4. **Name Processing**: 
   - Removes `.git` extension if present
   - Removes `jm_` prefix if present  
   - Converts to uppercase
5. **Smart Filename Formatting**:
   - If filename already has correct prefix → no changes
   - If filename has prefix but wrong case → corrects case only
   - If filename missing prefix → adds repository prefix
   - **Never duplicates prefixes** - applies transformation only once

### Examples
For repository `jm_myproject.git`:
- `applications.csv` → `MYPROJECT_applications.csv` (prefix added)
- `myproject_users.csv` → `MYPROJECT_users.csv` (case corrected)
- `MYPROJECT_data.csv` → `MYPROJECT_data.csv` (no change needed)
- `other_data.csv` → `MYPROJECT_other_data.csv` (prefix added to any filename)

### Properties Used
- `FileServer.getBaseDir()`: JMeter's actual test plan directory (primary source)
- `JMX_DIR`: Starting directory for Git repository search (fallback)
- `GIT_REPO_NAME`: Discovered repository name (set automatically)
- `GIT_REPO_PATH`: Full path to Git repository root
- `GIT_REPO_URL`: Remote repository URL

### Directory Detection Logic
The implementation uses a robust fallback chain to find the correct starting directory:
1. **Primary**: `FileServer.getFileServer().getBaseDir()` - JMeter's actual test plan directory
2. **Fallback 1**: `JMX_DIR` property (if set by global setup scripts)
3. **Fallback 2**: `System.getProperty("user.dir")` - current working directory (last resort)

This ensures Git repository detection works from the actual test plan location, not the JMeter installation directory.

This ensures that STS CSV files maintain proper namespace separation and follow established pipeline conventions across different environments and repositories.

## GUI Configuration Elements

The JAR includes three comprehensive GUI configuration elements:

### STS Configuration Element
A table-based GUI element similar to User Defined Variables:

#### How to Use
1. **Add to Test Plan**: Right-click test plan → Add → Config Element → "STS Configuration"
2. **Configure Multiple Operations**: Use the table to define multiple STS operations in a single element
3. **Table Columns**:
   - **CSV Filename**: Dropdown to select from `/data/` folder or type manually
   - **Action**: Dropdown with KEEP, DEL, ADDFIRST, ADDLAST options
   - **Variables/Values**: Context-sensitive field (variables for KEEP/DEL, values for ADD operations)
   - **Comment**: Optional comments for documentation
4. **Table Management**: Use Add/Delete buttons to manage operations, Refresh Files to update dropdown

### Key Benefits
- **Multiple Operations**: Configure multiple STS operations in a single element instead of multiple config elements
- **Table Interface**: Familiar JMeter table interface similar to User Defined Variables
- **Efficient Workflow**: One STS Configuration element per thread group is sufficient
- **Better Organization**: All STS operations visible in one place with comments

### Table Interface Example
```
| CSV Filename      | Action   | Variables/Values           | Comment                    |
|------------------|----------|----------------------------|----------------------------|
| applications.csv | KEEP     | APP_ID,Passport_Number     | Get user application data  |
| users.csv        | DEL      | USER_ID,Name,Email         | Consume user record        |
| audit.csv        | ADDLAST  | John,Smith,2024-01-15      | Add audit log entry        |
```

### Execution Flow
1. **Multiple Operations**: All table rows execute sequentially on each iteration
2. **Auto Prefixing**: `applications.csv` → `MYPROJECT_applications.csv` (automatic)
3. **Status Variables**: `STS_OPERATIONS_TOTAL`, `STS_OPERATIONS_SUCCESS`, `STS_OPERATIONS_FAILED`
4. **Backward Compatibility**: Works with existing single-operation test plans

### Benefits
- **Simplified Test Plans**: One STS Configuration element handles all operations for a thread group
- **Table Interface**: Familiar JMeter table pattern (like User Defined Variables)
- **Batch Operations**: Configure multiple STS operations together with comments
- **Visual Organization**: All STS operations visible at a glance in organized table format
- **Auto-Discovery**: Dropdown automatically finds CSV files in your `/data/` directory
- **Custom Icon**: Easy identification in JMeter tree with STS-specific icon

### Pacing Configuration Element
A clean GUI-based replacement for JSR223 sampler Parameters and group_init.groovy pacing logic:

#### How to Use
1. **Add to Test Plan**: Right-click Thread Group → Add → Config Element → "Pacing Configuration"
2. **Configure Pacing**: Set min/max pacing values with real-time preview
3. **Replace group_init.groovy**: Remove pacing logic from Groovy scripts

#### Features
- **Fixed Pacing**: Single value for consistent think time (e.g., exactly 60 seconds)
- **Random Pacing**: Min/max range for variable think time (e.g., 45-75 seconds)
- **Real-time Preview**: See exactly what your configuration will do
- **Debug Support**: Respects `debugSwitch=on` to skip pacing waits
- **Easy Migration**: Drop-in replacement for JSR223 Parameters pacing

#### Configuration Options
- **Min Pacing**: Required field (seconds) - fixed value if Max empty
- **Max Pacing**: Optional field (seconds) - creates random range if specified
- **Enable Pacing**: Checkbox to temporarily disable without losing configuration

#### Benefits Over JSR223 Parameters
- ✅ **GUI Validation**: Real-time error checking and configuration preview
- ✅ **Separation of Concerns**: Pacing logic separate from sampler business logic
- ✅ **Reusability**: Easy to copy/paste pacing configuration between thread groups
- ✅ **Visibility**: Clear view of pacing configuration in test plan
- ✅ **Migration Path**: Can gradually replace group_init.groovy pacing logic

See `README-Pacing-Configuration.md` for detailed migration instructions.

### Global Setup Configuration Element
A comprehensive replacement for global_setup.groovy script functionality:

#### How to Use
1. **Add to Test Plan**: Right-click Test Plan → Add → Config Element → "Global Setup Configuration"
2. **Configure Features**: Enable/disable individual setup features as needed
3. **Monitor Status**: View real-time detection results in status panel
4. **Replace Scripts**: Remove global_setup.groovy and related JSR223 samplers

#### Features
- **Template Processing**: Converts `template.*` properties to regular properties/variables
- **Directory Setup**: Establishes JMX_DIR and converts relative paths to absolute paths  
- **Hostname Detection**: Auto-detects hostname and extracts test names from patterns
- **Environment Detection**: Auto-configures STS hosts for local vs remote environments
- **Git Detection**: Finds repository information for filename prefixing and context
- **Status Display**: Real-time visual feedback with color-coded indicators

#### Configuration Options
- **Template Processing**: Enable/disable processing of `template.*` properties
- **Directory Setup**: Enable/disable JMX_DIR and path resolution
- **Hostname Detection**: Enable/disable hostname detection and test name extraction
- **Environment Detection**: Enable/disable automatic STS host configuration
- **Git Detection**: Enable/disable Git repository discovery

#### Benefits Over global_setup.groovy
- ✅ **Visual Feedback**: See exactly what was detected with status panel
- ✅ **Selective Control**: Enable/disable individual features as needed
- ✅ **Easy Debugging**: All detection results visible in GUI immediately
- ✅ **Reliable Execution**: Runs automatically at test start
- ✅ **Integration Ready**: Works seamlessly with STS and Pacing elements

#### Status Panel
Real-time display showing:
- **Hostname**: Detected system hostname
- **Test Name**: Extracted from jm-gen-* hostname patterns  
- **Environment**: Local/Remote determination
- **Git Repository**: Found repository name and path
- **JMX Directory**: Test plan directory location
- **Template Props**: Count of processed template properties

See `README-Global-Setup-Configuration.md` for detailed migration instructions. 