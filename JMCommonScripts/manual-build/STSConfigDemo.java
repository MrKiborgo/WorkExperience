import com.company.STSConfigElement;

/**
 * Command-line demo of STS Configuration functionality
 */
public class STSConfigDemo {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("🎯 STS Configuration Demo");
        System.out.println("=".repeat(60));
        
        // Demo 1: KEEP operation
        System.out.println("\n📋 Demo 1: KEEP Operation");
        STSConfigElement keepConfig = new STSConfigElement();
        keepConfig.setAction("KEEP");
        keepConfig.setFilename("applications.csv");
        keepConfig.setVariables("APP_ID,Passport_Number,Name");
        
        demonstrateConfig(keepConfig, "Reading first row and storing in variables");
        
        // Demo 2: DEL operation
        System.out.println("\n📋 Demo 2: DEL Operation");
        STSConfigElement delConfig = new STSConfigElement();
        delConfig.setAction("DEL");
        delConfig.setFilename("processed_applications.csv");
        delConfig.setVariables("ID,Status");
        
        demonstrateConfig(delConfig, "Reading and removing first row");
        
        // Demo 3: ADDFIRST operation
        System.out.println("\n📋 Demo 3: ADDFIRST Operation");
        STSConfigElement addFirstConfig = new STSConfigElement();
        addFirstConfig.setAction("ADDFIRST");
        addFirstConfig.setFilename("new_users.csv");
        addFirstConfig.setValues("12345,A1234567,John Doe,Engineer");
        
        demonstrateConfig(addFirstConfig, "Adding new row at beginning of file");
        
        // Demo 4: ADDLAST operation
        System.out.println("\n📋 Demo 4: ADDLAST Operation");
        STSConfigElement addLastConfig = new STSConfigElement();
        addLastConfig.setAction("ADDLAST");
        addLastConfig.setFilename("audit_log.csv");
        addLastConfig.setValues("2025-01-28,STS_OPERATION,SUCCESS");
        
        demonstrateConfig(addLastConfig, "Adding new row at end of file");
        
        // Demo 5: Validation examples
        System.out.println("\n❌ Demo 5: Validation Examples");
        
        // Invalid: missing filename
        STSConfigElement invalidConfig1 = new STSConfigElement();
        invalidConfig1.setAction("KEEP");
        invalidConfig1.setVariables("var1,var2");
        demonstrateConfig(invalidConfig1, "Missing filename - should fail validation");
        
        // Invalid: missing variables for KEEP
        STSConfigElement invalidConfig2 = new STSConfigElement();
        invalidConfig2.setAction("DEL");
        invalidConfig2.setFilename("test.csv");
        demonstrateConfig(invalidConfig2, "Missing variables for DEL operation");
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ Demo complete! The STS GUI eliminates manual function syntax.");
        System.out.println("   Users can now configure STS operations visually with validation!");
        System.out.println("=".repeat(60));
    }
    
    private static void demonstrateConfig(STSConfigElement config, String description) {
        System.out.println("📝 " + description);
        System.out.println("   Action: " + config.getAction());
        System.out.println("   Filename: " + config.getFilename());
        
        if ("KEEP".equals(config.getAction()) || "DEL".equals(config.getAction())) {
            System.out.println("   Variables: " + config.getVariables());
        } else {
            System.out.println("   Values: " + config.getValues());
        }
        
        // Generate function
        String function = config.generateFunction();
        System.out.println("   📋 Generated: " + function);
        
        // Validate
        STSConfigElement.ValidationResult validation = config.validate();
        if (validation.isValid()) {
            System.out.println("   ✅ " + validation.getMessage());
        } else {
            System.out.println("   ❌ " + validation.getMessage());
        }
        
        System.out.println();
    }
} 