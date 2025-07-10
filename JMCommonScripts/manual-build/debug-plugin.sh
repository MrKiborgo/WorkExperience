#!/bin/bash

# Debug Gemini Plugin with Full Classpath
echo "🔍 Debugging Gemini Chat Plugin with Full Classpath..."

# Build full classpath from all JMeter libraries
JMETER_HOME="../../../"
CLASSPATH="${JMETER_HOME}/lib/ext/ApacheJMeter_core.jar"
CLASSPATH="${CLASSPATH}:${JMETER_HOME}/lib/ext/ApacheJMeter_components.jar"
CLASSPATH="${CLASSPATH}:${JMETER_HOME}/lib/jorphan.jar"
CLASSPATH="${CLASSPATH}:${JMETER_HOME}/lib/logkit-2.0.jar"

# Add all JMeter libs to classpath
for jar in ${JMETER_HOME}/lib/*.jar; do
    CLASSPATH="${CLASSPATH}:${jar}"
done

echo "📁 Using classpath: ${CLASSPATH}"

# Clean and create build directories
rm -rf debug-build
mkdir -p debug-build/classes/com/company
mkdir -p debug-build/META-INF/services

# Copy files
echo "📂 Copying source files..."
cp com/company/GeminiChatJMeter.java debug-build/classes/com/company/
cp com/company/GeminiChatJMeterGui.java debug-build/classes/com/company/
cp META-INF/services/org.apache.jmeter.gui.JMeterGUIComponent debug-build/META-INF/services/ 2>/dev/null || echo "Service file not found, will create manually"
cp META-INF/MANIFEST.MF debug-build/META-INF/ 2>/dev/null || echo "Manifest not found, will create manually"

# Create service file if it doesn't exist
echo "com.company.GeminiChatJMeterGui" > debug-build/META-INF/services/org.apache.jmeter.gui.JMeterGUIComponent

# Compile with full classpath
echo "🔨 Compiling with full classpath..."
javac -cp "${CLASSPATH}" \
      debug-build/classes/com/company/GeminiChatJMeter.java \
      debug-build/classes/com/company/GeminiChatJMeterGui.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    
    # Create JAR
    echo "📦 Creating JAR..."
    cd debug-build
    jar -cvf ../../lib/ext/gemini-chat-jmeter-plugin.jar \
        -C classes com/company/GeminiChatJMeter.class \
        -C classes com/company/GeminiChatJMeterGui.class \
        -C classes com/company/GeminiChatJMeterGui\$1.class \
        META-INF/services/org.apache.jmeter.gui.JMeterGUIComponent
    cd ..
    
    echo "✅ JAR created successfully!"
    echo "📋 JAR Contents:"
    jar -tf ../../lib/ext/gemini-chat-jmeter-plugin.jar
    
    echo ""
    echo "🎯 NEXT STEPS FOR DEBUGGING:"
    echo "1. Restart JMeter completely"
    echo "2. Look for 'Gemini AI Assistant' in: Add → Config Element"
    echo "3. Check jmeter.log for any loading errors"
    echo "4. Try running: grep -i gemini jmeter.log"
    
else
    echo "❌ Compilation still failed. Let's check what classes are available:"
    echo "📝 Checking available classes in ApacheJMeter_core.jar:"
    jar -tf ${JMETER_HOME}/lib/ext/ApacheJMeter_core.jar | grep -E "(AbstractConfigGui|ConfigTestElement)" | head -5
fi

# Don't clean up for debugging
echo "🔍 Debug files kept in debug-build/ for inspection" 