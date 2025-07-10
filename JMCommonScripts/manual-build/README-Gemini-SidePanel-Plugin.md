# Gemini AI Side Panel for JMeter 🤖

**Transform your JMeter experience with AI-powered assistance directly in your GUI!**

## 🎯 Overview

This plugin integrates Google Gemini AI as a **toggleable side panel** directly into JMeter's main interface - similar to Cursor's AI assistant. No more switching between applications or adding config elements to test plans!

### ✨ **Key Features**

- **🔄 Toggle On/Off** - Show/hide the AI panel with `Ctrl+Shift+G` or via the Tools menu
- **📏 Collapsible** - Minimize to a thin strip when you need more screen space
- **🧠 Context-Aware** - Automatically includes your current test plan context
- **⚡ Real-time Chat** - Direct conversation with Gemini AI
- **🎨 Modern UI** - Clean, professional interface that matches JMeter's look
- **🔧 Always Available** - Not tied to specific test elements or configurations

## 🚀 **Installation**

### Prerequisites
1. ✅ **JMeter** installed and working
2. ✅ **Gemini CLI** installed and configured (`gemini --version` should work)
3. ✅ **Java Development Kit** (for building from source)

### Quick Install
```bash
# Navigate to the manual-build directory
cd JMCommonScripts/manual-build

# Build and install the plugin
./build-gemini-sidepanel.sh
```

### Manual Installation
If you have the JAR file:
```bash
# Copy the plugin to JMeter's extension directory
cp gemini-sidepanel-jmeter-plugin.jar /path/to/jmeter/lib/ext/

# Restart JMeter
```

## 🎮 **Usage**

### 1. **Start JMeter GUI**
```bash
./jmeter.sh
# or 
./jmeter.bat
```

### 2. **Open the AI Panel**
**Method 1:** Use keyboard shortcut `Ctrl+Shift+G`
**Method 2:** Menu: `Tools` → `🤖 Toggle Gemini AI Panel`

### 3. **Start Chatting!**
- Type your question in the input field
- Press `Send` or `Ctrl+Enter` to send
- Check "Include JMX" to share current test plan context
- Use "Clear" to start a fresh conversation

### 4. **Panel Controls**
- **Collapse/Expand**: Click the `◀`/`▶` button in the header
- **Toggle Visibility**: Use `Ctrl+Shift+G` or the menu item
- **Resize**: Drag the panel border to resize

## 💡 **What Can You Ask?**

### **JMeter Help**
- "How do I create a load test for a web application?"
- "What's the best way to parameterize my test data?"
- "How can I add assertions to verify responses?"

### **Performance Testing Strategy**
- "What load testing approach should I use for an e-commerce site?"
- "How do I interpret these response time results?"
- "What's the difference between load, stress, and spike testing?"

### **Troubleshooting**
- "My test is failing with connection timeouts, what should I check?"
- "How do I debug slow response times?"
- "Why are my concurrent users not ramping up correctly?"

### **Script Optimization**
- "How can I make my test plan more efficient?"
- "What's the best practice for thread group configuration?"
- "Should I use CSV Data Set Config or User Parameters?"

## 🛠️ **Technical Details**

### **Architecture**
```
JMeter GUI
├── Main Frame
│   ├── Menu Bar (+ Tools → Gemini AI)
│   └── Content Pane
│       ├── Test Plan Tree (LEFT)
│       ├── Configuration Panels (CENTER)
│       └── Gemini AI Panel (RIGHT) ← NEW!
```

### **Components**
- **`GeminiSidePanel.java`** - Main UI component with chat interface
- **`GeminiSidePanelPlugin.java`** - Plugin integration with JMeter GUI
- **`GeminiPluginLoader.java`** - Automatic plugin initialization

### **How It Works**
1. Plugin loads when JMeter GUI starts
2. Adds menu item to Tools menu
3. Creates side panel component (hidden initially)
4. User toggles panel visibility
5. Panel communicates with Gemini CLI via ProcessBuilder
6. Responses displayed in real-time chat interface

## 🔧 **Configuration**

### **Gemini CLI Setup**
Make sure Gemini CLI is properly configured:
```bash
# Test Gemini CLI
gemini --version

# First time setup (if needed)
gemini
```

### **Plugin Settings**
- **Panel Width**: Default 400px (auto-resizable)
- **Collapsed Width**: 30px
- **Keyboard Shortcut**: `Ctrl+Shift+G` (fixed)
- **Context Inclusion**: Toggle via checkbox

## 🚨 **Troubleshooting**

### **Panel Doesn't Appear**
1. **Check JMeter Logs**: Look for plugin initialization messages
2. **Verify Installation**: Ensure JAR is in `lib/ext/` directory
3. **Restart JMeter**: Plugin loads at startup
4. **Check Menu**: Look for Tools → Toggle Gemini AI Panel

### **Gemini Not Responding**
1. **Test CLI**: Run `gemini --version` in terminal
2. **Check PATH**: Ensure `gemini` command is available
3. **Authentication**: Run `gemini` standalone to check auth status
4. **Permissions**: Ensure JMeter can execute external processes

### **UI Issues**
1. **Panel Too Small**: Drag the border to resize
2. **Text Overlap**: Try collapsing and expanding the panel
3. **Font Issues**: Check your Java Look & Feel settings

## 📊 **Screenshots**

### Side Panel Open
```
┌─────────────────────────────────────────────┐
│ JMeter - Test Plan                          │
├─────────────────────┬───────────────────────┤
│ Test Plan Tree      │ 🤖 Gemini AI         │
│ ├─ Thread Group     │ ┌─────────────────────┤
│ ├─ HTTP Request     │ │ Chat Area           │
│ ├─ Listeners        │ │ You: How do I...    │
│ └─ ...              │ │ Gemini: To do...    │
├─────────────────────┤ │                     │
│ Configuration Panel │ ├─────────────────────┤
│                     │ │ [Input Field]       │
│                     │ │ [Send] [Clear]      │
└─────────────────────┴─┴─────────────────────┘
```

### Side Panel Collapsed
```
┌─────────────────────────────────────────────┐
│ JMeter - Test Plan                          │
├─────────────────────────────────────────────┤
│ Test Plan Tree      │ Configuration Panel  ▶│
│ ├─ Thread Group     │                       │
│ ├─ HTTP Request     │                       │
│ ├─ Listeners        │                       │
│ └─ ...              │                       │
└─────────────────────┴─────────────────────────┘
```

## 🔮 **Future Enhancements**

- **🎨 Theme Integration**: Match JMeter's Look & Feel
- **💾 Chat History**: Persist conversations across sessions
- **📎 File Attachments**: Share JMX files with Gemini
- **🔍 Smart Suggestions**: Context-aware quick actions
- **📱 Responsive Design**: Better mobile/small screen support

## 🤝 **Contributing**

### Building from Source
```bash
# Clone/download the source files
# Ensure you have JMeter and Java installed
cd JMCommonScripts/manual-build
./build-gemini-sidepanel.sh
```

### Development Setup
- **IDE**: Any Java IDE (IntelliJ, Eclipse, VS Code)
- **JMeter Dev**: Include JMeter JARs in classpath
- **Testing**: Load plugin in JMeter GUI for testing

## 📝 **License**

This plugin is provided as-is for educational and development purposes. Please ensure compliance with:
- JMeter's Apache License
- Google Gemini API Terms of Service
- Your organization's AI usage policies

## 🎉 **Enjoy Your AI-Powered Performance Testing!**

Happy testing! 🚀 