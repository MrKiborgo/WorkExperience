#!/bin/bash

# Enhanced Gemini Chat Plugin Build Script with Service Discovery
echo "Building Gemini Chat JMeter Plugin with Service Discovery..."

# Clean and create build directories
rm -rf build
mkdir -p build/classes
mkdir -p build/META-INF/services

# Copy source files to build directory
cp -r com build/classes/

# Copy service discovery files
cp -r META-INF build/

# Compile Java files
echo "Compiling Java files..."
find build/classes -name "*.java" -type f > build/java_files.txt

if [ -s build/java_files.txt ]; then
    javac -cp "../../../lib/ext/ApacheJMeter_core.jar:../../../lib/ext/ApacheJMeter_components.jar" \
          @build/java_files.txt
    
    if [ $? -eq 0 ]; then
        echo "Compilation successful!"
        
        # Create JAR with service discovery
        echo "Creating JAR with service discovery..."
        cd build
        jar -cvf ../../lib/ext/gemini-chat-jmeter-plugin.jar \
            -C . com/ \
            -C . META-INF/
        cd ..
        
        echo "✅ Gemini Chat Plugin JAR created successfully with service discovery!"
        echo "📦 Location: ../../lib/ext/gemini-chat-jmeter-plugin.jar"
        
        # List contents of JAR
        echo "📋 JAR Contents:"
        jar -tf ../../lib/ext/gemini-chat-jmeter-plugin.jar
        
    else
        echo "❌ Compilation failed!"
        exit 1
    fi
else
    echo "❌ No Java files found to compile!"
    exit 1
fi

# Clean up build directory
rm -rf build
echo "🧹 Build directory cleaned up"
echo "🔄 Please restart JMeter to load the new plugin" 