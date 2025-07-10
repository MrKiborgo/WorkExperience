#!/bin/bash

# Focused Gemini Chat Plugin Build Script
echo "Building ONLY Gemini Chat JMeter Plugin..."

# Clean and create build directories
rm -rf build-gemini
mkdir -p build-gemini/classes/com/company
mkdir -p build-gemini/META-INF/services

# Copy only the Gemini Chat related files
echo "Copying Gemini Chat specific files..."
cp com/company/GeminiChatJMeter.java build-gemini/classes/com/company/
cp com/company/GeminiChatJMeterGui.java build-gemini/classes/com/company/

# Copy service discovery files
cp META-INF/services/org.apache.jmeter.gui.JMeterGUIComponent build-gemini/META-INF/services/
cp META-INF/MANIFEST.MF build-gemini/META-INF/

# Compile only Gemini Chat files
echo "Compiling Gemini Chat files..."
javac -cp "../../../lib/ext/ApacheJMeter_core.jar:../../../lib/ext/ApacheJMeter_components.jar" \
      build-gemini/classes/com/company/GeminiChatJMeter.java \
      build-gemini/classes/com/company/GeminiChatJMeterGui.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    
    # Create JAR with service discovery
    echo "Creating JAR with service discovery..."
    cd build-gemini
    jar -cvfm ../../lib/ext/gemini-chat-jmeter-plugin.jar META-INF/MANIFEST.MF \
        -C classes com/company/GeminiChatJMeter.class \
        -C classes com/company/GeminiChatJMeterGui.class \
        -C classes com/company/GeminiChatJMeterGui\$1.class \
        -C . META-INF/services/
    cd ..
    
    echo "✅ Gemini Chat Plugin JAR created successfully!"
    echo "📦 Location: ../../lib/ext/gemini-chat-jmeter-plugin.jar"
    
    # List contents of JAR
    echo "📋 JAR Contents:"
    jar -tf ../../lib/ext/gemini-chat-jmeter-plugin.jar
    
else
    echo "❌ Compilation failed!"
    exit 1
fi

# Clean up build directory
rm -rf build-gemini
echo "🧹 Build directory cleaned up"
echo "🔄 Please restart JMeter to load the new plugin" 