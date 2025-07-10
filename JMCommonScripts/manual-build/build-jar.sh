#!/bin/bash

# Simple script to build the STS JAR manually without Gradle
# This ensures compatibility with Java 17 in Alpine containers

set -e

echo "Building dcu-sts-utils.jar manually..."

# Clean previous build
rm -f dcu-sts-utils.jar
find com/ -name "*.class" -delete

# Compile Java sources with Java 11 for compatibility
echo "Compiling Java sources with Java 11..."
find com/company -name "*.java" -exec /usr/lib/jvm/java-11-openjdk-amd64/bin/javac -cp "../../lib/ext/*:../../lib/*" {} +

# Create JAR including the icon resources in JMeter standard structure
echo "Creating JAR file..."
jar cf dcu-sts-utils.jar com/ org/ sts.png

# Copy to JMeter lib/ext
echo "Copying to JMeter lib/ext..."
cp dcu-sts-utils.jar ../../lib/ext/

# Verify
echo "Verifying JAR contents:"
jar -tf dcu-sts-utils.jar

echo "Build complete! JAR created with classes compiled for Java 11:"
/usr/lib/jvm/java-11-openjdk-amd64/bin/java -version 2>&1 | head -1

echo ""
echo "This JAR should work on Java 11, 17, and 21."
echo ""
echo "Usage in JMeter:"
echo "  1. Function: \${__STS(KEEP,filename.csv,var1,var2)}"
echo "  2. Script: import com.company.STS"
echo "  3. GUI: Add -> Config Element -> STS Configuration"
echo "  4. GUI: Add -> Config Element -> Pacing Configuration"
echo "  5. Automatic: Global Setup runs automatically (property-driven)" 