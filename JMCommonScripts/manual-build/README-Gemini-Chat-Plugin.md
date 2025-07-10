# Gemini Chat JMeter Plugin

🤖 **Integrate Google Gemini AI directly into your JMeter GUI for intelligent performance testing assistance!**

## Overview

This plugin brings the power of Google's Gemini AI directly into JMeter's interface, providing real-time assistance with:
- **Test Plan Development** - Get AI suggestions for structuring your performance tests
- **Script Optimisation** - Improve your JMeter scripts with AI recommendations  
- **Troubleshooting** - Diagnose performance issues with AI assistance
- **Best Practices** - Learn JMeter best practices through interactive chat
- **Configuration Help** - Get guidance on JMeter settings and configurations

## Prerequisites

Before installing this plugin, ensure you have:

1. ✅ **JMeter** (this installation)
2. ✅ **Gemini CLI** installed and configured (already done!)
3. ✅ **Java Development Kit** (for building the plugin)

### Verifying Gemini CLI

Test that Gemini CLI is working:
```bash
gemini --version
```

If not authenticated, run:
```bash
gemini
```
And follow the authentication steps.

## Installation

### Step 1: Build the Plugin

From the JMeter root directory, run:

```bash
cd JMCommonScripts/manual-build
./build-gemini-plugin.sh
```

This will:
- Compile the Java source files
- Create a JAR plugin file
- Install it to JMeter's `lib/ext/` directory

### Step 2: Restart JMeter

Close and restart JMeter GUI to load the new plugin.

### Step 3: Add the Plugin to Your Test Plan

1. **Right-click** on your Test Plan or Thread Group
2. Go to **Add > Config Element > Gemini AI Assistant**
3. The Gemini Chat panel will appear

## Features

### 🎯 **Smart Chat Interface**
- Clean, intuitive chat UI integrated into JMeter
- Real-time conversation with Gemini AI
- Syntax highlighting for user and AI messages
- Auto-scrolling chat history

### 🔍 **Context Awareness**  
- **JMX Context Toggle**: Include current test plan information in your queries
- Gemini understands your current JMeter setup
- More relevant and specific assistance

### ⚡ **Performance Features**
- Asynchronous communication (doesn't block JMeter GUI)
- Status indicators showing connection state
- Error handling and recovery
- Thread-safe implementation

### 💬 **Chat Controls**
- **Send Button**: Send your message to Gemini
- **Clear Chat**: Reset the conversation history
- **Enter Key**: Quick send (Shift+Enter for multiline)
- **Context Toggle**: Include/exclude JMX information

## Usage Examples

### Getting Started
Simply type your questions in the input field:

**Example 1: Test Plan Structure**
```
"How should I structure a JMeter test plan for testing a REST API with 1000 concurrent users?"
```

**Example 2: Performance Troubleshooting**  
```
"My JMeter test is showing high response times. What should I check?"
```

**Example 3: Configuration Help**
```
"What's the best way to configure connection timeouts for HTTP requests?"
```

### Advanced Usage

**With JMX Context Enabled:**
When you enable "Include JMX Context", Gemini receives information about your current test plan, making responses more specific and relevant.

**Example:**
```
"Analyse my current test plan and suggest optimisations"
```

## Technical Details

### Architecture
- **Frontend**: Java Swing GUI integrated into JMeter's interface
- **Backend**: Executes Gemini CLI via `ProcessBuilder`
- **Threading**: Uses `ExecutorService` for non-blocking operations
- **Integration**: Extends JMeter's `AbstractConfigGui` framework

### File Structure
```
JMCommonScripts/manual-build/
├── com/company/
│   ├── GeminiChatJMeter.java        # Test element (data model)
│   └── GeminiChatJMeterGui.java     # GUI component
├── build-gemini-plugin.sh           # Build script
└── README-Gemini-Chat-Plugin.md     # This documentation
```

### Plugin Location
After installation: `lib/ext/gemini-chat-jmeter-plugin.jar`

## Troubleshooting

### Plugin Not Appearing in Menu
1. **Check Installation**: Verify JAR file exists in `lib/ext/`
2. **Restart JMeter**: Plugins require a restart to load
3. **Check Logs**: Look in `jmeter.log` for any errors

### Gemini CLI Issues
1. **Command Not Found**: Ensure `gemini` is in your PATH
2. **Authentication**: Run `gemini` standalone to check auth status
3. **Version Check**: Run `gemini --version` to verify installation

### Connection Problems
1. **Network**: Check internet connectivity
2. **Firewall**: Ensure Gemini CLI can make outbound connections
3. **Rate Limits**: Be aware of Gemini's usage limits

### Build Issues
1. **Java Version**: Ensure compatible JDK version
2. **Classpath**: Verify JMeter JAR files are accessible
3. **Permissions**: Check write permissions for `lib/ext/` directory

## Advanced Configuration

### Custom Context
You can modify `getJmxContext()` method in `GeminiChatJMeterGui.java` to include more specific information about your test plan.

### Styling
Modify the colour constants in the GUI class to match your preferences:
- `CHAT_BG`: Chat background colour
- `USER_MSG_COLOR`: User message colour  
- `AI_MSG_COLOR`: AI response colour

## Best Practices

### Effective Prompting
1. **Be Specific**: Include details about your testing scenario
2. **Provide Context**: Use the JMX context feature for better assistance
3. **Ask Follow-ups**: Build on previous responses for deeper insights

### Performance Considerations
1. **Rate Limits**: Be mindful of Gemini's request limits
2. **Large Responses**: Very long AI responses may take time to display
3. **Multiple Instances**: Avoid opening multiple chat panels simultaneously

## Future Enhancements

Potential improvements for future versions:
- **File Upload**: Direct sharing of JMX files with Gemini
- **Test Results Analysis**: Integration with JMeter result files
- **Custom Templates**: Pre-built prompts for common scenarios
- **Export Chat**: Save conversations for later reference
- **Plugin Management**: Settings panel for configuration

## Support

### Getting Help
1. **Documentation**: This README and inline help text
2. **JMeter Community**: General JMeter support forums
3. **Gemini CLI**: Official Google documentation

### Contributing
This plugin is part of your local JMeter setup. To modify:
1. Edit the Java source files
2. Run the build script
3. Restart JMeter to test changes

### Version Information
- **Plugin Version**: 1.0.0
- **JMeter Compatibility**: Tested with JMeter 5.x
- **Gemini CLI**: Requires version 0.1.7+

---

**Happy Performance Testing with AI! 🚀** 