#!/bin/bash

# Build script for Gemini Chat JMeter Plugin
# This script compiles the Java source files and creates a JAR plugin for JMeter

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"
JMETER_LIB="../../lib"
JMETER_LIB_EXT="../../lib/ext"

echo "🚀 Building Gemini Chat JMeter Plugin..."

# Clean build directory
echo "📁 Cleaning build directory..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/classes"

# Find JMeter JAR files for classpath
echo "🔍 Finding JMeter JAR files..."
CLASSPATH=""
for jar in "$JMETER_LIB"/*.jar "$JMETER_LIB_EXT"/*.jar; do
    if [ -f "$jar" ]; then
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

# Remove leading colon
CLASSPATH="${CLASSPATH#:}"

if [ -z "$CLASSPATH" ]; then
    echo "❌ Error: Could not find JMeter JAR files. Make sure you're running this from the correct directory."
    exit 1
fi

echo "✅ Found JMeter libraries"

# Compile Java source files
echo "🔧 Compiling Java source files..."
javac -cp "$CLASSPATH" \
      -d "$BUILD_DIR/classes" \
      com/company/GeminiChatJMeter.java \
      com/company/GeminiChatJMeterGui.java

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi

echo "✅ Compilation successful"

# Create JAR file
echo "📦 Creating JAR file..."
cd "$BUILD_DIR/classes"
jar cf "../gemini-chat-jmeter-plugin.jar" com/company/*.class

if [ $? -ne 0 ]; then
    echo "❌ JAR creation failed!"
    exit 1
fi

echo "✅ JAR created: $BUILD_DIR/gemini-chat-jmeter-plugin.jar"

# Copy to JMeter lib/ext directory
echo "📂 Installing plugin to JMeter..."
cp "$BUILD_DIR/gemini-chat-jmeter-plugin.jar" "$JMETER_LIB_EXT/gemini-chat-jmeter-plugin.jar"

if [ $? -ne 0 ]; then
    echo "❌ Failed to copy plugin to JMeter lib/ext directory"
    exit 1
fi

echo "✅ Plugin installed successfully!"
echo ""
echo "🎉 Gemini Chat JMeter Plugin has been built and installed!"
echo ""
echo "📋 Next steps:"
echo "   1. Restart JMeter GUI"
echo "   2. Look for 'Gemini AI Assistant' under Config Elements in the Add menu"
echo "   3. Make sure you have already set up Gemini CLI with: gemini"
echo ""
echo "💡 Plugin file created at: $JMETER_LIB_EXT/gemini-chat-jmeter-plugin.jar" 