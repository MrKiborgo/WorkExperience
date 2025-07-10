# STS Usage Examples - Multiple Ways to Call STS

Now you have **3 different ways** to use STS functionality, from most concise to most explicit:

## 🚀 Option 1: JMeter Custom Function (Shortest - `${__STS()}`)

Use directly in **any JMeter test element** (HTTP Request, If Controller, etc.):

```
${__STS(KEEP,applications.csv,APP_ID,Passport_Number)}
${__STS(DEL,applications.csv,APP_ID,Passport_Number)}
${__STS(ADDFIRST,applications.csv,value1,value2,value3)}
```

**Benefits:**
- ✅ **Shortest syntax** - exactly what you asked for!
- ✅ **No JSR223 sampler needed** - use in any test element
- ✅ **Returns true/false** for success checking
- ✅ **Variables automatically populated**

**Example in HTTP Request:**
- **Request URL**: `https://api.example.com/users/${APP_ID}`
- **Pre-Processor**: Add `${__STS(KEEP,applications.csv,APP_ID,Passport_Number)}` 

## 📝 Option 2: STSHelper in JSR223 Samplers (Short)

Use in JSR223 samplers with static import:

```groovy
import static com.company.STSHelper.*

// Ultra-short syntax
sts("KEEP,applications.csv,APP_ID,Passport_Number")
sts("DEL,applications.csv,APP_ID,Passport_Number") 
sts("ADDFIRST,applications.csv,value1,value2,value3")

// Alternative method names
STS("KEEP,applications.csv,APP_ID,Passport_Number")  // Uppercase
exec("KEEP,applications.csv,APP_ID,Passport_Number") // Alternative
```

**Benefits:**
- ✅ **Very short** - no need to pass log, vars, props
- ✅ **Auto-context** - automatically gets JMeter context
- ✅ **Multiple aliases** - sts(), STS(), exec()

## 🔧 Option 3: Direct STS Class (Most Explicit)

Use the original STS class directly:

```groovy
import com.company.STS

STS.exec(log, vars, props, "KEEP,applications.csv,APP_ID,Passport_Number")
```

**Benefits:**
- ✅ **Full control** - pass custom log, vars, props if needed
- ✅ **Explicit** - clear what's happening
- ✅ **Original method** - same as before

## 💡 Which Option to Choose?

### Use **Option 1** (`${__STS()}`) when:
- You want the **shortest possible syntax** ⭐
- Using in HTTP Requests, If Controllers, etc.
- Don't need JSR223 samplers
- Want `$__STS()` syntax as you requested

### Use **Option 2** (`sts()`) when:
- Writing JSR223 samplers
- Want short syntax but need scripting logic
- Need to check return values in Groovy

### Use **Option 3** (`STS.exec()`) when:
- Need maximum control
- Passing custom logging or context
- Debugging or complex scenarios

## 📋 Complete Examples

### Example 1: JMeter Custom Function in HTTP Request
```
Method: GET
URL: https://api.example.com/users/${APP_ID}
Pre-Processor Action: ${__STS(KEEP,applications.csv,APP_ID,Passport_Number)}
```
Result: `APP_ID` and `Passport_Number` variables populated automatically

### Example 2: STSHelper in JSR223 Sampler
```groovy
import static com.company.STSHelper.*

// Get user data
if (sts("KEEP,applications.csv,APP_ID,Passport_Number")) {
    log.info("Got user: APP_ID=${vars.get('APP_ID')}, Passport=${vars.get('Passport_Number')}")
} else {
    log.error("Failed to get user data")
}
```

### Example 3: Multiple STS calls in sequence
```groovy
import static com.company.STSHelper.*

// Get user data
sts("KEEP,users.csv,USER_ID,EMAIL")

// Add audit log entry  
sts("ADDFIRST,audit.csv,${USER_ID},LOGIN,${__time()}")

// Remove used token
sts("DEL,tokens.csv,TOKEN_ID,USER_ID")
```

## ✨ Your Requested `$__STS()` Syntax

You asked for `$__STS()` - this is now available as **Option 1**:

```
${__STS(KEEP,applications.csv,APP_ID,Passport_Number)}
```

This is the **shortest possible syntax** and works in any JMeter test element! 🎉 