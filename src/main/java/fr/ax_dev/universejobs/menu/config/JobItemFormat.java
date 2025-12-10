package fr.ax_dev.universejobs.menu.config;

import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for job item formatting in menus.
 * Allows customization of how job items are displayed.
 * Now extends MenuItemConfig for consistency.
 */
public class JobItemFormat extends MenuItemConfig {
    
    private final boolean useJobIcon;
    private final List<String> loreWithoutJob;
    private final boolean elseGlow;
    
    public JobItemFormat(ConfigurationSection config) {
        super(createJobItemConfigSection(config));
        
        this.useJobIcon = config.getBoolean("use-job-icon", true);
        
        // Handle "else" section for when job is not joined
        ConfigurationSection elseSection = config.getConfigurationSection("else");
        if (elseSection != null) {
            List<String> elseLore = elseSection.getStringList("lore");
            this.loreWithoutJob = elseLore.isEmpty() ? getDefaultLoreWithoutJob() : elseLore;
            this.elseGlow = elseSection.getBoolean("glow", true);
        } else {
            // Fallback to old format
            List<String> loreWithoutJobRaw = config.getStringList("lore-without-job");
            this.loreWithoutJob = loreWithoutJobRaw.isEmpty() ? getDefaultLoreWithoutJob() : loreWithoutJobRaw;
            this.elseGlow = config.getBoolean("glow-when-not-joined", true);
        }
    }
    
    /**
     * Private constructor for default format.
     */
    private JobItemFormat(boolean useJobIcon, String displayName, List<String> lore, 
                         List<String> loreWithoutJob, boolean glowWhenJoined, 
                         boolean glowWhenNotJoined, int amount, 
                         boolean hideAttributes, boolean hideEnchants) {
        super(createDefaultConfigSection(displayName, lore, glowWhenJoined, glowWhenNotJoined, amount, hideAttributes, hideEnchants));
        
        this.useJobIcon = useJobIcon;
        this.loreWithoutJob = loreWithoutJob;
        this.elseGlow = glowWhenNotJoined;
    }
    
    /**
     * Create a ConfigurationSection for job item format.
     */
    private static ConfigurationSection createJobItemConfigSection(ConfigurationSection config) {
        Map<String, Object> configMap = new HashMap<>();
        
        // Copy standard MenuItemConfig properties
        configMap.put("enabled", config.getBoolean("enabled", true));
        configMap.put("material", config.getString("material", "STONE"));
        configMap.put("amount", config.getInt("amount", 1));
        configMap.put("display-name", config.getString("display-name", "<bold>{job_name}</bold>"));
        
        List<String> lore = config.getStringList("lore");
        if (lore.isEmpty()) {
            lore = getDefaultLore();
        }
        configMap.put("lore", lore);

        if (config.contains("custom-model-data")) {
            configMap.put("custom-model-data", config.get("custom-model-data"));
        }

        ConfigurationSection modelDataSection = config.getConfigurationSection("model_data_component");
        if (modelDataSection != null) {
            configMap.put("model_data_component", modelDataSection.getValues(false));
        }
        configMap.put("glow", config.getBoolean("glow-when-joined", false));
        configMap.put("hide-attributes", config.getBoolean("hide-attributes", true));
        configMap.put("hide-enchants", config.getBoolean("hide-enchants", true));
        
        return new SimpleConfigurationSection(configMap);
    }
    
    /**
     * Create default configuration section.
     */
    private static ConfigurationSection createDefaultConfigSection(String displayName, List<String> lore, 
                                                                  boolean glowWhenJoined, boolean glowWhenNotJoined, 
                                                                  int amount, boolean hideAttributes, boolean hideEnchants) {
        Map<String, Object> configMap = new HashMap<>();
        
        configMap.put("enabled", true);
        configMap.put("material", "STONE");
        configMap.put("amount", amount);
        configMap.put("display-name", displayName);
        configMap.put("lore", lore);
        configMap.put("glow", glowWhenJoined);
        configMap.put("hide-attributes", hideAttributes);
        configMap.put("hide-enchants", hideEnchants);
        
        return new SimpleConfigurationSection(configMap);
    }
    
    /**
     * Get default job item format.
     */
    public static JobItemFormat getDefault() {
        return new JobItemFormat(
            true,  // useJobIcon
            "<bold>{job_name}</bold>",  // displayName
            getDefaultLore(),  // lore
            getDefaultLoreWithoutJob(),  // loreWithoutJob
            false,  // glowWhenJoined
            true,  // glowWhenNotJoined
            1,  // amount
            true,  // hideAttributes
            true  // hideEnchants
        );
    }
    
    private static List<String> getDefaultLore() {
        return Arrays.asList(
            "<gray>{job_description}",
            "",
            "<gray>Your Status:",
            "<gray>├ <gray>Level: <#abffb3>{player_level}<gray>/<#abffb3>{job_max_level} <gray>(<#FFD700>{progress_percent}%<gray>)",
            "<gray>├ <gray>XP: <#62de6e>{player_xp}",
            "<gray>└ {progress_bar}",
            "",
            "<gray>◆ <gray>Status: {job_status}",
            "",
            "<#FFD700>▶ Left-Click to open job menu",
            "<#abffb3>▶ Right-Click to leave"
        );
    }
    
    private static List<String> getDefaultLoreWithoutJob() {
        return Arrays.asList(
            "<gray>{job_description}",
            "",
            "<gray>Your Status:",
            "<gray>├ <gray>Level: <#abffb3>{player_level}<gray>/<#abffb3>{job_max_level} <gray>(<#FFD700>{progress_percent}%<gray>)",
            "<gray>├ <gray>XP: <#62de6e>{player_xp}",
            "<gray>└ {progress_bar}",
            "",
            "<gray>◆ <gray>Status: {job_status}",
            "",
            "<#FFD700>▶ Left-Click to open job menu",
            "<#abffb3>▶ Right-Click to join"
        );
    }
    
    // Specific getters for JobItemFormat
    public boolean isUseJobIcon() {
        return useJobIcon;
    }
    
    public List<String> getLoreWithoutJob() {
        return new ArrayList<>(loreWithoutJob);
    }
    
    // Convenience methods for glow based on job status
    public boolean shouldGlow(boolean hasJob) {
        return hasJob ? isGlow() : elseGlow;
    }
}