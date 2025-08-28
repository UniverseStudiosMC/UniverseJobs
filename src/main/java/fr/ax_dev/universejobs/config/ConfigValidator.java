package fr.ax_dev.universejobs.config;

import fr.ax_dev.universejobs.UniverseJobs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;

/**
 * Validates and auto-generates missing required configuration values.
 * Ensures all necessary configuration keys exist with proper default values.
 */
public class ConfigValidator {
    
    private final UniverseJobs plugin;
    private final Map<String, ConfigTemplate> configTemplates;
    
    public ConfigValidator(UniverseJobs plugin) {
        this.plugin = plugin;
        this.configTemplates = new HashMap<>();
        initializeTemplates();
    }
    
    /**
     * Initialize configuration templates with required keys and default values.
     */
    private void initializeTemplates() {
        // Main config.yml template
        ConfigTemplate mainConfig = new ConfigTemplate("config.yml");
        mainConfig.addRequired("debug", false, "Enable debug mode for troubleshooting");
        mainConfig.addRequired("language.locale", "en_US", "Language locale (en_US, fr_FR)");
        mainConfig.addRequired("settings.save-interval", 300, "Auto-save interval in seconds");
        mainConfig.addRequired("create-example-jobs", true, "Create example job files on first run");
        
        // Database settings
        mainConfig.addRequired("database.enabled", false, "Use database instead of files");
        mainConfig.addRequired("database.host", "localhost", "Database host");
        mainConfig.addRequired("database.port", "3306", "Database port");
        mainConfig.addRequired("database.prefix", "UniverseJobs_", "Table prefix");
        mainConfig.addRequired("database.username", "UniverseJobs", "Database username");
        mainConfig.addRequired("database.password", "your_password_here", "Database password");
        mainConfig.addRequired("database.pool.min-connections", 2, "Minimum connection pool size");
        mainConfig.addRequired("database.pool.max-connections", 10, "Maximum connection pool size");
        mainConfig.addRequired("database.pool.connection-timeout-ms", 30000, "Connection timeout in milliseconds");
        mainConfig.addRequired("database.pool.validation-interval-ms", 300000, "Connection validation interval");
        
        // Job settings
        mainConfig.addRequired("jobs.all-jobs-by-default", false, "Automatically assign all jobs to players");
        mainConfig.addRequired("jobs.jobs-by-default", Arrays.asList(), "List of default jobs players cannot leave");
        mainConfig.addRequired("jobs.max-jobs-per-player", 3, "Maximum jobs per player (ignored if all-jobs-by-default is true)");
        
        configTemplates.put("config.yml", mainConfig);
        
        // Menu configuration template
        ConfigTemplate menuConfig = new ConfigTemplate("menus/main-menu.yml");
        menuConfig.addRequired("title", "&6&lJobs Menu", "Menu title");
        menuConfig.addRequired("size", 54, "Menu size (multiple of 9, max 54)");
        menuConfig.addRequired("pagination.enabled", true, "Enable pagination");
        menuConfig.addRequired("pagination.items-per-page", 28, "Items per page");
        
        // Fill item configuration
        menuConfig.addRequired("fill-item.enabled", true, "Enable fill items");
        menuConfig.addRequired("fill-item.material", "GRAY_STAINED_GLASS_PANE", "Fill item material");
        menuConfig.addRequired("fill-item.display-name", " ", "Fill item display name");
        menuConfig.addRequired("fill-item.lore", Arrays.asList(), "Fill item lore");
        menuConfig.addRequired("fill-item.amount", 1, "Fill item amount");
        menuConfig.addRequired("fill-item.custom-model-data", 0, "Fill item custom model data");
        menuConfig.addRequired("fill-item.glow", false, "Fill item glow effect");
        menuConfig.addRequired("fill-item.hide-attributes", true, "Hide fill item attributes");
        menuConfig.addRequired("fill-item.hide-enchants", true, "Hide fill item enchants");
        
        // Content and navigation slots
        menuConfig.addRequired("fill-slots", Arrays.asList(), "Slots to fill with fill items");
        menuConfig.addRequired("content-slots", Arrays.asList(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43), "Slots for job items");
        menuConfig.addRequired("job-slots", new HashMap<>(), "Specific job slot assignments");
        
        // Navigation slots
        menuConfig.addRequired("navigation-slots.previous", Arrays.asList(45), "Previous page button slots");
        menuConfig.addRequired("navigation-slots.next", Arrays.asList(53), "Next page button slots");
        menuConfig.addRequired("navigation-slots.close", Arrays.asList(49), "Close menu button slots");
        menuConfig.addRequired("navigation-slots.info", Arrays.asList(50), "Info button slots");
        
        configTemplates.put("menus/main-menu.yml", menuConfig);
    }
    
    /**
     * Validate a configuration file and auto-generate missing values.
     * 
     * @param configPath The path to the configuration file (relative to plugin folder)
     * @return true if validation was successful or changes were made
     */
    public boolean validateAndUpdate(String configPath) {
        ConfigTemplate template = configTemplates.get(configPath);
        if (template == null) {
            plugin.getLogger().info("No validation template found for " + configPath + " - skipping auto-generation");
            return true;
        }
        
        try {
            File configFile = new File(plugin.getDataFolder(), configPath);
            FileConfiguration config;
            boolean configExists = configFile.exists();
            
            if (configExists) {
                config = YamlConfiguration.loadConfiguration(configFile);
            } else {
                config = new YamlConfiguration();
                plugin.getLogger().info("Configuration file " + configPath + " does not exist - creating with defaults");
            }
            
            // Check and add missing configurations
            boolean hasChanges = false;
            List<String> addedKeys = new ArrayList<>();
            
            for (Map.Entry<String, RequiredConfig> entry : template.getRequiredConfigs().entrySet()) {
                String key = entry.getKey();
                RequiredConfig requiredConfig = entry.getValue();
                
                if (!config.isSet(key)) {
                    config.set(key, requiredConfig.getDefaultValue());
                    addedKeys.add(key);
                    hasChanges = true;
                }
            }
            
            // Save if changes were made
            if (hasChanges) {
                // Create parent directories if they don't exist
                if (configFile.getParentFile() != null) {
                    configFile.getParentFile().mkdirs();
                }
                
                // Add header comments
                String header = generateHeader(template, addedKeys);
                config.options().header(header);
                config.options().copyDefaults(true);
                
                config.save(configFile);
                
                if (addedKeys.isEmpty()) {
                    plugin.getLogger().info("Created new configuration file: " + configPath);
                } else {
                    plugin.getLogger().info("Auto-generated " + addedKeys.size() + " missing configuration keys in " + configPath + ": " + String.join(", ", addedKeys));
                }
            }
            
            return true;
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to validate/update configuration file: " + configPath, e);
            return false;
        }
    }
    
    /**
     * Generate a header comment for the configuration file.
     */
    private String generateHeader(ConfigTemplate template, List<String> addedKeys) {
        StringBuilder header = new StringBuilder();
        header.append("UniverseJobs Configuration File: ").append(template.getFileName()).append("\n");
        header.append("Auto-generated on: ").append(new Date()).append("\n");
        
        if (!addedKeys.isEmpty()) {
            header.append("Auto-generated keys: ").append(String.join(", ", addedKeys)).append("\n");
        }
        
        header.append("\nConfiguration keys and their purposes:\n");
        for (Map.Entry<String, RequiredConfig> entry : template.getRequiredConfigs().entrySet()) {
            String key = entry.getKey();
            String description = entry.getValue().getDescription();
            header.append("- ").append(key).append(": ").append(description).append("\n");
        }
        
        return header.toString();
    }
    
    /**
     * Validate all known configuration files.
     */
    public void validateAllConfigurations() {
        plugin.getLogger().info("Starting configuration validation and auto-generation...");
        
        int validated = 0;
        int failed = 0;
        
        for (String configPath : configTemplates.keySet()) {
            if (validateAndUpdate(configPath)) {
                validated++;
            } else {
                failed++;
            }
        }
        
        plugin.getLogger().info("Configuration validation complete: " + validated + " validated, " + failed + " failed");
    }
    
    /**
     * Get the list of all template file paths.
     */
    public Set<String> getTemplateFiles() {
        return configTemplates.keySet();
    }
    
    /**
     * Configuration template containing required keys and defaults.
     */
    private static class ConfigTemplate {
        private final String fileName;
        private final Map<String, RequiredConfig> requiredConfigs;
        
        public ConfigTemplate(String fileName) {
            this.fileName = fileName;
            this.requiredConfigs = new LinkedHashMap<>();
        }
        
        public void addRequired(String key, Object defaultValue, String description) {
            requiredConfigs.put(key, new RequiredConfig(defaultValue, description));
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public Map<String, RequiredConfig> getRequiredConfigs() {
            return requiredConfigs;
        }
    }
    
    /**
     * Required configuration entry with default value and description.
     */
    private static class RequiredConfig {
        private final Object defaultValue;
        private final String description;
        
        public RequiredConfig(Object defaultValue, String description) {
            this.defaultValue = defaultValue;
            this.description = description;
        }
        
        public Object getDefaultValue() {
            return defaultValue;
        }
        
        public String getDescription() {
            return description;
        }
    }
}