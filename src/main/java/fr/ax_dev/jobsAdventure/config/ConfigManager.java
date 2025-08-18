package fr.ax_dev.jobsAdventure.config;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages plugin configuration loading and saving.
 */
public class ConfigManager {
    
    private final JobsAdventure plugin;
    
    /**
     * Create a new ConfigManager.
     * 
     * @param plugin The plugin instance
     */
    public ConfigManager(JobsAdventure plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load the main configuration file.
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        
        FileConfiguration config = plugin.getConfig();
        
        // Validate configuration
        validateConfig(config);
        
        // Configuration loaded successfully
    }
    
    /**
     * Validate the configuration and set defaults if needed.
     * 
     * @param config The configuration to validate
     */
    private void validateConfig(FileConfiguration config) {
        boolean changed = false;
        
        // Messages section
        if (!config.contains("messages.level-up")) {
            config.set("messages.level-up", "&aCongratulations! You reached level {level} in {job}!");
            changed = true;
        }
        
        if (!config.contains("messages.xp-gain")) {
            config.set("messages.xp-gain", "&e+{xp} XP ({job})");
            changed = true;
        }
        
        if (!config.contains("messages.show-xp-gain")) {
            config.set("messages.show-xp-gain", true);
            changed = true;
        }
        
        if (!config.contains("messages.job-joined")) {
            config.set("messages.job-joined", "&aYou joined the {job} job!");
            changed = true;
        }
        
        if (!config.contains("messages.job-left")) {
            config.set("messages.job-left", "&cYou left the {job} job!");
            changed = true;
        }
        
        if (!config.contains("messages.already-have-job")) {
            config.set("messages.already-have-job", "&cYou already have the {job} job!");
            changed = true;
        }
        
        if (!config.contains("messages.dont-have-job")) {
            config.set("messages.dont-have-job", "&cYou don't have the {job} job!");
            changed = true;
        }
        
        if (!config.contains("messages.job-not-found")) {
            config.set("messages.job-not-found", "&cJob '{job}' not found!");
            changed = true;
        }
        
        if (!config.contains("messages.no-permission")) {
            config.set("messages.no-permission", "&cYou don't have permission to join the {job} job!");
            changed = true;
        }
        
        // Sounds section
        if (!config.contains("sounds.level-up")) {
            config.set("sounds.level-up", "ENTITY_PLAYER_LEVELUP");
            changed = true;
        }
        
        if (!config.contains("sounds.xp-gain")) {
            config.set("sounds.xp-gain", "ENTITY_EXPERIENCE_ORB_PICKUP");
            changed = true;
        }
        
        // Settings section
        if (!config.contains("settings.save-interval")) {
            config.set("settings.save-interval", 300); // 5 minutes
            changed = true;
        }
        
        if (!config.contains("settings.max-jobs-per-player")) {
            config.set("settings.max-jobs-per-player", 3);
            changed = true;
        }
        
        if (!config.contains("settings.debug")) {
            config.set("settings.debug", false);
            changed = true;
        }
        
        // Level up commands
        if (!config.contains("level-up-commands")) {
            config.set("level-up-commands", java.util.Arrays.asList(
                "broadcast {player} reached level {level} in {job}!",
                "give {player} diamond 1"
            ));
            changed = true;
        }
        
        // Database settings (for future use)
        if (!config.contains("database.enabled")) {
            config.set("database.enabled", false);
            changed = true;
        }
        
        if (!config.contains("database.type")) {
            config.set("database.type", "mysql");
            changed = true;
        }
        
        if (!config.contains("database.host")) {
            config.set("database.host", "localhost");
            changed = true;
        }
        
        if (!config.contains("database.port")) {
            config.set("database.port", 3306);
            changed = true;
        }
        
        if (!config.contains("database.database")) {
            config.set("database.database", "jobsadventure");
            changed = true;
        }
        
        if (!config.contains("database.username")) {
            config.set("database.username", "username");
            changed = true;
        }
        
        if (!config.contains("database.password")) {
            config.set("database.password", "password");
            changed = true;
        }
        
        if (changed) {
            plugin.saveConfig();
            // Configuration updated with new default values
        }
    }
    
    /**
     * Reload the configuration.
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        validateConfig(plugin.getConfig());
    }
    
    /**
     * Get a message from the configuration with color codes translated.
     * Supports both MiniMessage format and legacy color codes.
     * 
     * @param key The message key
     * @param defaultValue The default value if not found
     * @return The formatted message
     */
    public String getMessage(String key, String defaultValue) {
        String message = plugin.getConfig().getString("messages." + key, defaultValue);
        // The message will be processed by MessageUtils when sent to players
        return message;
    }
    
    /**
     * Get a message from the configuration with placeholder replacement.
     * Supports both MiniMessage format and legacy color codes.
     * 
     * @param key The message key
     * @param defaultValue The default value if not found
     * @param placeholders Placeholder replacements (key, value, key, value, ...)
     * @return The formatted message
     */
    public String getMessage(String key, String defaultValue, String... placeholders) {
        String message = getMessage(key, defaultValue);
        
        // Build placeholder map
        Map<String, String> placeholderMap = new HashMap<>();
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            placeholderMap.put(placeholders[i], placeholders[i + 1]);
        }
        
        // Replace placeholders
        for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return message;
    }
    
    /**
     * Check if debug mode is enabled.
     * 
     * @return true if debug mode is enabled
     */
    public boolean isDebugEnabled() {
        return plugin.getConfig().getBoolean("settings.debug", false);
    }
    
    /**
     * Get the maximum number of jobs a player can have.
     * Note: This is ignored when all-jobs-by-default is enabled.
     * 
     * @return The max job count
     */
    public int getMaxJobsPerPlayer() {
        // If all jobs by default is enabled, return a high number
        if (isAllJobsByDefault()) {
            return Integer.MAX_VALUE;
        }
        return plugin.getConfig().getInt("jobs.max-jobs-per-player", 3);
    }
    
    /**
     * Check if all jobs should be assigned by default.
     * 
     * @return true if all jobs should be assigned by default
     */
    public boolean isAllJobsByDefault() {
        return plugin.getConfig().getBoolean("jobs.all-jobs-by-default", false);
    }
    
    /**
     * Get the list of job IDs that players automatically join and cannot leave.
     * 
     * @return List of default job IDs
     */
    public List<String> getJobsByDefault() {
        return plugin.getConfig().getStringList("jobs.jobs-by-default");
    }
    
    /**
     * Check if a job is a default job that players cannot leave.
     * 
     * @param jobId The job ID to check
     * @return true if the job is a default job
     */
    public boolean isDefaultJob(String jobId) {
        if (isAllJobsByDefault()) {
            return true; // All jobs are default jobs when this is enabled
        }
        return getJobsByDefault().contains(jobId);
    }
    
    /**
     * Get the data save interval in seconds.
     * 
     * @return The save interval
     */
    public int getSaveInterval() {
        return plugin.getConfig().getInt("settings.save-interval", 300);
    }
}