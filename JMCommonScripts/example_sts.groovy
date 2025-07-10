import java.nio.file.Paths

// Import STS module
def jmxDir = props.get("JMX_DIR")
if (!jmxDir) {
    log.error("JMX_DIR property not set. Cannot locate STS.groovy")
    return
}

def stsPath = Paths.get(jmxDir, "scripts", "STS.groovy").toString()
log.info("Loading STS from: ${stsPath}")
def sts = evaluate(new File(stsPath).text)
sts.init(log, props, vars)

// Example: Read from STS and store columns in variables
def success = sts.readAndKeep("applications.csv", "APP_ID", "Passport_Number", "Name")

if (success) {
    log.info("STS read successful")
    log.info("Application ID: ${vars.get('APP_ID')}")
    log.info("Passport Number: ${vars.get('Passport_Number')}")
    log.info("Name: ${vars.get('Name')}")
} else {
    log.error("Failed to read from STS")
} 