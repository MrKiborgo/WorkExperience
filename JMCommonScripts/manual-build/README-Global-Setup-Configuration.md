# Global Setup Configuration

> **⚠️ DEPRECATED GUI ELEMENT**  
> The GUI-based Global Setup Configuration element is **deprecated** in favour of the automatic property-driven approach. The automatic system provides the same functionality without requiring GUI elements in test plans.
>
> **✅ RECOMMENDED: Use Automatic Global Setup**  
> See the [Automatic Global Setup](#automatic-global-setup-recommended) section below for the property-driven approach that runs automatically without any GUI elements.

## Overview
~~The **Global Setup Configuration** element provides a comprehensive GUI-based replacement for the `global_setup.groovy` script~~. 

The **Global Setup** system handles essential test plan initialisation tasks including template property processing, directory setup, hostname detection, environment configuration, and Git repository discovery.

There are two approaches available:
1. **[Automatic Global Setup](#automatic-global-setup-recommended)** ✅ **RECOMMENDED** - Property-driven, no GUI elements required
2. **[GUI Global Setup Configuration Element](#gui-global-setup-configuration-element-deprecated)** ❌ **DEPRECATED** - Requires GUI elements in each test plan

---

## Automatic Global Setup (RECOMMENDED)

### Overview
The **Automatic Global Setup** system provides all global setup functionality automatically without requiring any GUI elements in test plans. It runs via a `TestStateListener` that is automatically registered through JMeter's property system.

### ✅ **Benefits of Automatic Approach**
- **🎯 Zero Test Plan Modifications**: Works with existing test plans without changes
- **🔧 Centralised Management**: All configuration in one place (JMeter installation)
- **⚡ Consistent Execution**: Runs automatically on every test start
- **🛠️ Property-Driven**: Easy to configure per environment via properties
- **📦 Version Control Friendly**: Update once, affects all test plans
- **🚀 CI/CD Ready**: No GUI elements to manage in automated environments

### Quick Start
The system is already configured and ready to use! It's automatically enabled via:
```properties
# Already configured in jmeter.properties
global.setup.listener=com.company.jmeter.setup.GlobalSetupListener
```

### Features (All Automatic)
- **Template Property Processing**: Converts `template.*` properties to regular properties
- **Directory Path Setup**: Sets `JMX_DIR` and converts relative paths to absolute
- **Hostname Detection**: Detects hostname and extracts test name from patterns
- **Environment Configuration**: Auto-configures Local/Remote STS hosts
- **Git Repository Discovery**: Finds and sets Git repository information

### Configuration
All features are **enabled by default** and controlled via properties. Add these to your `user.properties` to customise:

```properties
# Core Features (all default to true)
global.setup.template.processing=true
global.setup.directory.setup=true
global.setup.hostname.detection=true
global.setup.environment.detection=true
global.setup.git.detection=true

# Logging
global.setup.logging.verbose=false

# Environment-specific STS configuration
V_STS_LOCAL_HOST=your-sts-server.company.com
V_STS_HOST_DEFAULT=localhost:8080

# STS Protocol Configuration (HTTP vs HTTPS)
sts.use.https=false

# Template directories
template.C_SCRIPTS=/scripts/common/
template.P_SCRIPTS=/scripts/project/
template.DATA=/data/
```

### Properties Set Automatically
The system automatically sets these properties/variables on every test:

| **Category** | **Properties/Variables Set** |
|-------------|------------------------------|
| **Template Processing** | `template.PROPERTY_NAME` → `PROPERTY_NAME` |
| **Directory Setup** | `JMX_DIR`, `C_SCRIPTS`, `P_SCRIPTS`, `DATA` |
| **Hostname Detection** | `hostname`, `testname` (if jm-gen pattern) |
| **Environment Detection** | `IS_LOCAL_ENVIRONMENT`, `V_STS_HOST` |
| **Git Detection** | `GIT_REPO_PATH`, `GIT_REPO_NAME`, `GIT_REPO_URL` |

### Advanced Configuration
See `auto-global-setup.properties` for complete configuration options and examples.

### Troubleshooting
Enable verbose logging to see what's happening:
```properties
global.setup.logging.verbose=true
```

Check JMeter logs for messages like:
```
INFO  [AutoGlobalSetup] Starting Automatic Global Setup...
INFO  [AutoGlobalSetup] Template processing: 5 properties processed
INFO  [AutoGlobalSetup] Directory setup: JMX_DIR = /path/to/test
INFO  [AutoGlobalSetup] Hostname detection: hostname=myhost, testName=automation
INFO  [AutoGlobalSetup] Environment detection: Local development (myhost)
INFO  [AutoGlobalSetup] Git detection: myproject (/path/to/repo)
INFO  [AutoGlobalSetup] GlobalSetupUtil completed successfully
```

#### SSL Certificate Issues
If you see SSL certificate errors (PKIX path building failed), the STS system defaults to HTTP to avoid these issues:
```properties
# Default - uses HTTP (no SSL issues)
sts.use.https=false

# Only enable HTTPS if you have valid SSL certificates
sts.use.https=true
```

---

## GUI Global Setup Configuration Element (DEPRECATED)

> **⚠️ This approach is deprecated.** Use the [Automatic Global Setup](#automatic-global-setup-recommended) instead.

## Features
- **Template Property Processing**: Converts `template.*` properties to regular properties and variables
- **Directory Path Setup**: Establishes JMX_DIR and converts relative paths to absolute paths
- **Hostname Detection**: Automatic hostname detection with test name extraction
- **Environment Configuration**: Auto-configures STS hosts based on hostname patterns
- **Git Repository Discovery**: Finds and extracts Git repository information
- **Real-time Status Display**: Shows detected values with visual indicators
- **Selective Enabling**: Individual control over each feature

## Usage

### 1. Add Global Setup Configuration Element
1. Right-click **Test Plan** (recommended) or **Thread Group**
2. Choose **Add → Config Element → Global Setup Configuration**
3. Configure which features to enable/disable
4. Run test to see detection results in status panel

### 2. Configuration Options

#### Template Processing
- **Purpose**: Processes `template.*` properties from `user.properties`
- **Example**: `template.V_STS_HOST=server.com` → creates `V_STS_HOST=server.com`
- **Benefit**: Enables template-based configuration management

#### Directory Setup
- **Purpose**: Establishes JMX_DIR and converts relative paths to absolute
- **Variables**: `C_SCRIPTS`, `P_SCRIPTS`, `DATA`
- **Example**: `DATA="/data/"` → `DATA="/path/to/test/data/"`
- **Benefit**: Consistent path resolution across scripts

#### Hostname Detection
- **Purpose**: Detects hostname and extracts test name from patterns
- **Pattern**: `jm-gen-{testname}-{worker}` → extracts `testname`
- **Example**: `jm-gen-automation-worker-01` → `testname="automation"`
- **Benefit**: Automatic test metadata population

#### Environment Detection
- **Purpose**: Auto-configures STS hosts based on hostname
- **Remote Pattern**: `jm-gen*`, `jm-nofrills*` → uses `V_STS_LOCAL_HOST`
- **Local Pattern**: Other hostnames → uses `V_STS_HOST_DEFAULT`
- **Benefit**: Seamless local/remote environment switching

#### Git Detection
- **Purpose**: Finds Git repository information for context
- **Discovery**: Walks up directory tree from JMX file location
- **Properties Set**: `GIT_REPO_PATH`, `GIT_REPO_NAME`, `GIT_REPO_URL`
- **Benefit**: Repository-aware configuration and filename prefixing

### 3. Status Panel
The status panel provides real-time feedback:

| **Field** | **Shows** | **Colors** |
|-----------|-----------|------------|
| **Hostname** | Detected system hostname | Green=detected, Gray=not detected |
| **Test Name** | Extracted from hostname pattern | Green=extracted, Gray=not extracted |
| **Environment** | Local/Remote with hostname | Green=determined, Gray=unknown |
| **Git Repository** | Repository name and path | Green=found, Gray=not found |
| **JMX Directory** | Test plan directory | Green=set, Gray=not set |
| **Template Props** | Count of processed properties | Green=>0, Gray=0 |

## Replacement of global_setup.groovy

### What You Can Remove
The Global Setup Configuration element **completely replaces** `global_setup.groovy`:

```groovy
// ❌ DELETE: All of this functionality is now in the GUI element
processTemplateProperties()
saveVarsAsProps(jmxDir, varsToSave)
checkHostGlobal()
detectGitRepository()
// ... entire global_setup.groovy can be removed
```

### Migration Steps

#### Step 1: Add Global Setup Configuration
1. Add the **Global Setup Configuration** element to your Test Plan
2. Enable all features initially (default)
3. Run a test to verify detection works correctly

#### Step 2: Verify Detection Results
1. Check the **Status Panel** shows correct values:
   - Hostname should show your machine name
   - Environment should be "Local" or "Remote" appropriately
   - Git Repository should show your project name
   - Template Props should show count > 0 if you use templates

#### Step 3: Remove global_setup.groovy
1. Remove `global_setup.groovy` from your test plan
2. Remove any JSR223 samplers that called it
3. Test that everything still works correctly

## Properties Set
The Global Setup Configuration element sets these properties and variables:

### Template Processing
- **From**: `template.PROPERTY_NAME` → **To**: `PROPERTY_NAME` (both property and variable)

### Directory Setup
- **Properties**: `JMX_DIR`, `C_SCRIPTS`, `P_SCRIPTS`, `DATA` (as absolute paths)

### Hostname Detection
- **Properties**: `hostname`
- **Variables**: `testname` (if extracted from jm-gen-* pattern)

### Environment Detection
- **Properties**: `IS_LOCAL_ENVIRONMENT`, `V_STS_HOST` (if configured)
- **Variables**: `V_STS_HOST` (for backward compatibility)

### Git Detection
- **Properties**: `GIT_REPO_PATH`, `GIT_REPO_NAME`, `GIT_REPO_URL`

## Integration with Existing Elements

### STS Configuration
The Global Setup element works perfectly with STS Configuration:
- **Environment Detection**: Auto-sets `V_STS_HOST` for STS to use
- **Git Detection**: Provides repository info for filename prefixing
- **Template Processing**: Ensures STS template properties are available

### Pacing Configuration
No direct integration, but Global Setup ensures:
- **Directory Setup**: Scripts can find their paths reliably
- **Environment Variables**: Available for any custom pacing logic

## Benefits

### Before (global_setup.groovy)
```groovy
// Hidden script execution
// No visibility into what's happening
// Manual debugging through log files
// Mixed with business logic
// Hard to selectively disable features
```

### After (Global Setup Configuration)
```
[GUI Element]
✓ Template Processing (23 properties processed)
✓ Directory Setup 
✓ Hostname Detection (jm-gen-automation-01)
✓ Environment Detection (Remote execution)
✓ Git Detection (myproject repository found)

Status Panel:
Hostname: jm-gen-automation-01
Test Name: automation
Environment: Remote execution (jm-gen-automation-01)
Git Repository: myproject (/path/to/repo)
JMX Directory: /path/to/test/plan
Template Props: 23
```

- ✅ **Full Visibility**: See exactly what was detected
- ✅ **Selective Control**: Enable/disable individual features
- ✅ **Visual Feedback**: Color-coded status indicators
- ✅ **Easy Debugging**: All information visible in GUI
- ✅ **Reliable Execution**: Runs at test start automatically

## Debug Information
When issues occur, the status panel immediately shows:
- **Red Text**: Errors (e.g., "Detection failed: Permission denied")
- **Gray Text**: Not found/detected (e.g., "Not found", "Not extracted")
- **Green Text**: Successfully detected (e.g., "automation", "Local development")

## Advanced Usage

### Selective Feature Control
You can disable features you don't need:
- **Large Test Plans**: Disable Git detection if it's slow
- **Simple Tests**: Disable template processing if you don't use templates
- **Local Only**: Disable environment detection if always running locally

### Custom Configuration
The element respects existing properties:
- **Pre-set V_STS_HOST**: Won't override if already configured
- **Manual JMX_DIR**: Uses existing value if already set
- **Existing Variables**: Template processing won't override existing variables

### Multiple Elements
You can have multiple Global Setup elements:
- **Different Thread Groups**: Each can have different feature sets
- **Conditional Setup**: Enable different features based on test conditions
- **Staged Processing**: Break complex setup into multiple elements

## Troubleshooting

### Template Properties Not Processing
- **Check**: Enable Template Processing checkbox is checked
- **Verify**: `user.properties` contains `template.*` properties
- **Status**: Template Props count should be > 0

### Hostname Not Detected
- **Check**: Enable Hostname Detection checkbox is checked
- **Status**: Should show your machine hostname, not "Not detected"
- **Fallback**: Can manually set `hostname` property if needed

### Git Repository Not Found
- **Check**: Test plan is saved in a Git repository directory
- **Verify**: `.git` folder exists in current or parent directories
- **Status**: Should show repository name, not "Not found"

### Environment Detection Issues
- **Check**: Hostname detection worked first (dependency)
- **Verify**: `V_STS_LOCAL_HOST` and `V_STS_HOST_DEFAULT` properties exist
- **Status**: Should show "Local" or "Remote", not "Not determined"

## Performance
The Global Setup Configuration element runs once at test start and has minimal performance impact:
- **Template Processing**: O(n) where n = number of properties
- **Directory Setup**: O(1) simple path operations
- **Hostname Detection**: O(1) single system call
- **Environment Detection**: O(1) string comparison
- **Git Detection**: O(d) where d = directory depth to repository root

Typical execution time: < 100ms for most test plans. 