package fr.ax_dev.universejobs.config;

import fr.ax_dev.universejobs.UniverseJobs;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages plugin configuration loading and saving.
 */
public class ConfigManager {
    
    private final UniverseJobs plugin;
    private ProgressBarConfig progressBarConfig;
    
    /**
     * Create a new ConfigManager.
     * 
     * @param plugin The plugin instance
     */
    public ConfigManager(UniverseJobs plugin) {
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
        
        // Load progress bar configuration
        this.progressBarConfig = new ProgressBarConfig(config.getConfigurationSection("progress-bar"));
        
        // Configuration loaded successfully
    }
    
    /**
     * Validate the configuration and set defaults if needed.
     * 
     * @param config The configuration to validate
     */
    private void validateConfig(FileConfiguration config) {
        // No longer add default values automatically
        // The config.yml in resources already contains all necessary values
        // This prevents the config from being polluted with old/duplicate values
    }
    
    /**
     * Reload the configuration.
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        validateConfig(config);
        
        // Reload progress bar configuration
        this.progressBarConfig = new ProgressBarConfig(config.getConfigurationSection("progress-bar"));
        
        // Reload level up actions
        plugin.getLevelUpActionManager().reload(); // Reload batched reward manager
        plugin.getBatchedRewardManager().reloadConfig();
        
        // Reload block protection settings
        plugin.getProtectionManager().reloadConfig();
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
        return plugin.getConfig().getBoolean("debug", false);
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
    
    /**
     * Get the progress bar configuration.
     * 
     * @return The progress bar configuration
     */
    public ProgressBarConfig getProgressBarConfig() {
        return progressBarConfig;
    }
    
    /**
     * Get the boost calculation mode.
     * 
     * @return The boost calculation mode
     */
    public BoostCalculationMode getBoostCalculationMode() {
        String modeStr = plugin.getConfig().getString("jobs.boost-calculation-mode", "MULTIPLICATIVE");
        try {
            return BoostCalculationMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid boost calculation mode: " + modeStr + ", using MULTIPLICATIVE");
            return BoostCalculationMode.MULTIPLICATIVE;
        }
    }
    
    /**
     * Get the configured job status text.
     * 
     * @param hasJob Whether the player has the job or not
     * @return The formatted status text
     */
    public String getJobStatus(boolean hasJob) {
        String key = hasJob ? "placeholders.job_status.joined" : "placeholders.job_status.not_joined";
        String defaultValue = hasJob ? "<#abffb3>✓ Active" : "<#ff6b6b>○ Not Joined";
        return plugin.getConfig().getString(key, defaultValue);
    }
    
    /**
     * Check if job leave penalty is enabled.
     *
     * @return true if penalty is enabled
     */
    public boolean isLeavePenaltyEnabled() {
        return plugin.getConfig().getBoolean("jobs.leave-penalty.enabled", false);
    }

    /**
     * Get the leave penalty type.
     *
     * @return "level" or "xp"
     */
    public String getLeavePenaltyType() {
        return plugin.getConfig().getString("jobs.leave-penalty.type", "level");
    }

    /**
     * Get the leave penalty percentage.
     *
     * @return The penalty percentage (0.0 to 1.0)
     */
    public double getLeavePenaltyPercentage() {
        return plugin.getConfig().getDouble("jobs.leave-penalty.percentage", 0.1);
    }

    /**
     * Enum for boost calculation modes.
     */
    public enum BoostCalculationMode {
        ADDITIVE,       // Mode 1: 2.5x + 2.5x = 5.0x
        MULTIPLICATIVE, // Mode 2: 2.5x * 2.5x = 6.25x
        HIGHEST        // Mode 3: Only use the highest multiplier
    }
}