#!/bin/bash

# Build script for Gemini AI Side Panel JMeter Plugin
# This script compiles the Java source files and creates a JAR plugin for JMeter

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build-sidepanel"
JMETER_LIB="../../lib"
JMETER_LIB_EXT="../../lib/ext"

echo "🚀 Building Gemini AI Side Panel JMeter Plugin..."

# Clean build directory
echo "📁 Cleaning build directory..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/classes"
mkdir -p "$BUILD_DIR/classes/META-INF/services"

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

# Create plugin loader hook - this will be auto-loaded by JMeter's classloader
echo "📋 Creating plugin initialization..."

# Create a plugin properties file
cat > "$BUILD_DIR/classes/gemini-sidepanel-plugin.properties" << EOF
plugin.name=Gemini AI Side Panel
plugin.version=1.0.0
plugin.description=Integrates Google Gemini AI directly into JMeter GUI as a side panel
plugin.author=JMeter Community
plugin.loader=com.company.GeminiPluginLoader
EOF

# Compile Java source files
echo "🔧 Compiling Java source files..."
javac -cp "$CLASSPATH" \
      -d "$BUILD_DIR/classes" \
      com/company/GeminiSidePanel.java \
      com/company/GeminiSidePanelPlugin.java \
      com/company/GeminiPluginLoader.java \
      com/company/GeminiPluginInitializer.java

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi

echo "✅ Compilation successful"

# Create JAR file
echo "📦 Creating JAR file..."
cd "$BUILD_DIR/classes"

# Create manifest file
cat > MANIFEST.MF << EOF
Manifest-Version: 1.0
Implementation-Title: Gemini AI Side Panel for JMeter
Implementation-Version: 1.0.0
Implementation-Vendor: JMeter Community
Plugin-Class: com.company.GeminiPluginLoader
Main-Class: com.company.GeminiPluginLoader

EOF

jar cfm "../gemini-sidepanel-jmeter-plugin.jar" MANIFEST.MF \
    com/company/*.class \
    META-INF/ \
    *.properties

if [ $? -ne 0 ]; then
    echo "❌ JAR creation failed!"
    exit 1
fi

echo "✅ JAR created: $BUILD_DIR/gemini-sidepanel-jmeter-plugin.jar"

# Remove old plugin if exists
if [ -f "$JMETER_LIB_EXT/gemini-chat-jmeter-plugin.jar" ]; then
    echo "🗑️ Removing old config element plugin..."
    rm "$JMETER_LIB_EXT/gemini-chat-jmeter-plugin.jar"
fi

# Copy to JMeter lib/ext directory
echo "📂 Installing plugin to JMeter..."
cp "$BUILD_DIR/gemini-sidepanel-jmeter-plugin.jar" "$JMETER_LIB_EXT/gemini-sidepanel-jmeter-plugin.jar"

if [ $? -ne 0 ]; then
    echo "❌ Failed to copy plugin to JMeter lib/ext directory"
    exit 1
fi

echo "✅ Plugin installed successfully!"
echo ""
echo "🎉 Gemini AI Side Panel JMeter Plugin has been built and installed!"
echo ""
echo "📋 Next steps:"
echo "   1. 🔄 Restart JMeter GUI"
echo "   2. 🔍 Look for 'Tools' menu → '🤖 Toggle Gemini AI Panel'"
echo "   3. ⌨️  Or use the keyboard shortcut: Ctrl+Shift+G"
echo "   4. 🔧 Ensure Gemini CLI is set up with: gemini"
echo ""
echo "💡 Features:"
echo "   • Side panel (like Cursor AI) that can be toggled on/off"
echo "   • Collapsible/expandable panel"
echo "   • Always accessible (not tied to test elements)"
echo "   • Context-aware JMeter assistance"
echo ""
echo "💡 Plugin file created at: $JMETER_LIB_EXT/gemini-sidepanel-jmeter-plugin.jar" 