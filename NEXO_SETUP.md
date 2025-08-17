# 🔧 Nexo Integration Setup

## 📋 Overview

JobsAdventure supports optional integration with Nexo (custom items plugin). This integration is **optional** and the plugin will build and work perfectly without Nexo.

## 🚀 Quick Start (Without Nexo)

The plugin works out of the box without any additional setup. The build will automatically skip Nexo integration if the library is not present.

## 🔌 Enable Nexo Integration (Optional)

If you want to enable Nexo integration:

### Step 1: Download Nexo
1. Download `nexo-1.10.jar` from the official Nexo source
2. Place it in the `libs/` folder at the project root

### Step 2: Verify Maven Profile
The Maven profile will automatically activate when `libs/nexo-1.10.jar` exists:

```xml
<profile>
    <id>nexo</id>
    <activation>
        <file>
            <exists>libs/nexo-1.10.jar</exists>
        </file>
    </activation>
</profile>
```

### Step 3: Build with Nexo
```bash
mvn clean package
```

The build will automatically include Nexo integration when the JAR file is present.

## 📁 Project Structure

```
JobsAdventure/
├── libs/                    # ← Place nexo-1.10.jar here (optional)
│   └── nexo-1.10.jar       # ← This file enables Nexo integration
├── src/
├── pom.xml
└── README.md
```

## ⚠️ Important Notes

### For Developers
- **Nexo integration is optional** - the plugin compiles without it
- **No errors** if Nexo is not present
- **Automatic detection** of Nexo availability

### For CI/CD (GitHub Actions)
- Builds work **automatically** without Nexo
- No additional setup required
- Clean compilation guaranteed

### For Server Administrators
- Install JobsAdventure **with or without** Nexo
- Plugin will detect Nexo at runtime if present
- Full functionality available regardless

## 🔍 How It Works

### Build Time Detection
```xml
<!-- Only included if libs/nexo-1.10.jar exists -->
<activation>
    <file>
        <exists>libs/nexo-1.10.jar</exists>
    </file>
</activation>
```

### Runtime Detection
```java
// Plugin detects Nexo at runtime
if (getServer().getPluginManager().isPluginEnabled("Nexo")) {
    // Enable Nexo integration
    getServer().getPluginManager().registerEvents(
        new NexoEventListener(this, actionProcessor, protectionManager), 
        this
    );
    getLogger().info("Nexo event listener registered for enhanced custom block support");
}
```

## 🎯 Integration Features

When Nexo is available, JobsAdventure provides:

### Custom Block Support
```yaml
actions:
  BREAK:
    mythril_ore:
      target: "nexo:mythril_ore"
      xp: 75.0
      name: "Mythril Extraction"
```

### Enhanced Job Configuration
- Full support for Nexo custom blocks
- NBT-based protection for Nexo items
- Event integration for all Nexo actions

## 🔧 Troubleshooting

### Build Issues
**Problem**: Maven can't find Nexo dependency
**Solution**: Build without Nexo - it's completely optional

**Problem**: Local build works but CI fails
**Solution**: This is expected behavior - CI builds without Nexo by design

### Runtime Issues
**Problem**: Nexo blocks not recognized
**Solution**: 
1. Ensure Nexo plugin is installed and enabled
2. Check that JobsAdventure detected Nexo in console logs

## 📞 Support

- **Without Nexo**: Full support, no limitations
- **With Nexo**: Enhanced functionality for custom blocks
- **Build Issues**: Always build without Nexo first

---

**💡 Remember: Nexo integration is entirely optional and does not affect core functionality!**