# Configuration Auto-Generation System

UniverseJobs now features an intelligent auto-generation system that automatically creates missing required configuration values. This is extremely useful for plugin updates and ensures your configurations are always complete.

## How It Works

### Automatic Validation
- **On Plugin Start**: All configurations are validated and missing keys are auto-generated
- **On Config Reload**: Missing configurations are detected and added automatically
- **Manual Command**: Use `/jobs admin validateconfig` to manually trigger validation

### Supported Configuration Files

1. **config.yml** - Main plugin configuration
2. **menus/main-menu.yml** - Main jobs menu configuration
3. More files can be easily added to the system

### Features

#### ✅ **Smart Detection**
- Detects missing required configuration keys
- Preserves existing values (never overwrites)
- Only adds what's missing

#### ✅ **Detailed Logging**
- Console logs show exactly what was generated
- Timestamp information for tracking changes
- Clear error reporting if issues occur

#### ✅ **Future-Proof Updates**
- When you update the plugin, new config options are automatically added
- No manual configuration file editing required
- Backwards compatibility maintained

#### ✅ **Descriptive Headers**
- Auto-generated configurations include helpful comments
- Each key is documented with its purpose
- Easy to understand what each setting does

## Example Usage

### Scenario 1: Missing Database Configuration
If your `config.yml` is missing database settings, the system will automatically add:

```yaml
# Auto-generated database configuration
database:
  enabled: false
  host: "localhost"
  port: "3306"
  prefix: "UniverseJobs_"
  username: "UniverseJobs"
  password: "your_password_here"
  pool:
    min-connections: 2
    max-connections: 10
    connection-timeout-ms: 30000
    validation-interval-ms: 300000
```

### Scenario 2: Plugin Update with New Features
When the plugin is updated with new features requiring configuration:

1. **Before Update**: Your config works but is missing new keys
2. **After Update**: System detects missing keys and adds them automatically
3. **Result**: New features work immediately with sensible defaults

### Scenario 3: Corrupted/Incomplete Menu Configuration
If your menu configuration is incomplete:

```yaml
# What you have
title: "&6Jobs Menu"

# What gets auto-generated
title: "&6Jobs Menu"
size: 54
pagination:
  enabled: true
  items-per-page: 28
fill-item:
  enabled: true
  material: "GRAY_STAINED_GLASS_PANE"
  # ... and all other required menu settings
```

## Console Output Examples

### First Run (New Installation)
```
[UniverseJobs] Configuration file config.yml does not exist - creating with defaults
[UniverseJobs] Created new configuration file: config.yml
[UniverseJobs] Configuration validation and auto-generation complete
```

### Update Scenario (Missing Keys)
```
[UniverseJobs] Auto-generated 5 missing configuration keys in config.yml: database.pool.min-connections, database.pool.max-connections, jobs.max-jobs-per-player, settings.new-feature-enabled, debug.extended-logging
[UniverseJobs] Configuration validation and auto-generation complete
```

### Manual Validation
```
> /jobs admin validateconfig
[UniverseJobs] Starting configuration validation and auto-generation...
[UniverseJobs] Auto-generated 2 missing configuration keys in menus/main-menu.yml: navigation-slots.info, job-item-format.amount
[UniverseJobs] Configuration validation complete: 2 validated, 0 failed
✅ Configuration validation complete! Check console for details.
```

## Benefits for Server Administrators

### 🎯 **Zero Maintenance**
- No need to manually update configuration files
- Plugin updates won't break due to missing configs
- Always have the latest configuration options available

### 🛡️ **Error Prevention**  
- Prevents plugin crashes from missing required settings
- Ensures all features have proper default values
- Reduces support tickets related to configuration issues

### 📈 **Easy Updates**
- New plugin versions automatically configure themselves
- Test new features immediately with working defaults
- Gradual customization as you learn new features

### 🔧 **Development Friendly**
- Easy to add new configuration options
- Automatic documentation generation
- Consistent configuration structure

## Technical Details

### Configuration Templates
Each supported configuration file has a template defining:
- **Required Keys**: Essential settings that must exist
- **Default Values**: Sensible defaults for each key
- **Descriptions**: Documentation for each setting's purpose

### Validation Process
1. **Load Template**: Get the configuration template for the file
2. **Check Existing**: Read current configuration file (if exists)
3. **Compare**: Identify missing required keys
4. **Generate**: Add missing keys with default values
5. **Save**: Write updated configuration with header comments
6. **Log**: Report what was generated

### Adding New Templates
Developers can easily add new configuration files to the auto-generation system by modifying the `ConfigValidator` class:

```java
// Add new template
ConfigTemplate newConfig = new ConfigTemplate("path/to/config.yml");
newConfig.addRequired("setting.key", defaultValue, "Description");
configTemplates.put("path/to/config.yml", newConfig);
```

## Commands

### `/jobs admin validateconfig`
- **Permission**: `universejobs.admin.validateconfig`
- **Description**: Manually validate and auto-generate missing configurations
- **Usage**: Run this command after manually editing config files or when troubleshooting

## Conclusion

The Configuration Auto-Generation System ensures your UniverseJobs installation is always properly configured and ready for new features. It eliminates the common issues associated with plugin updates and provides a smooth experience for server administrators.

**No more broken configs, no more manual updates, no more missing settings!** 🎉