package fr.ax_dev.universejobs.menu.config;

import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration for job item formatting in menus.
 * Allows customization of how job items are displayed.
 */
public class JobItemFormat {
    
    private final boolean useJobIcon;
    private final String displayName;
    private final List<String> lore;
    private final List<String> loreWithoutJob;
    private final boolean glowWhenJoined;
    private final boolean glowWhenNotJoined;
    private final int amount;
    private final boolean hideAttributes;
    private final boolean hideEnchants;
    
    public JobItemFormat(ConfigurationSection config) {
        this.useJobIcon = config.getBoolean("use-job-icon", true);
        this.displayName = config.getString("display-name", "&e&l{job_name}");
        this.lore = config.getStringList("lore");
        this.loreWithoutJob = config.getStringList("lore-without-job");
        this.glowWhenJoined = config.getBoolean("glow-when-joined", true);
        this.glowWhenNotJoined = config.getBoolean("glow-when-not-joined", false);
        this.amount = config.getInt("amount", 1);
        this.hideAttributes = config.getBoolean("hide-attributes", true);
        this.hideEnchants = config.getBoolean("hide-enchants", true);
        
        // Set default lore if empty
        if (this.lore.isEmpty()) {
            this.lore.addAll(getDefaultLore());
        }
        
        if (this.loreWithoutJob.isEmpty()) {
            this.loreWithoutJob.addAll(getDefaultLoreWithoutJob());
        }
    }
    
    /**
     * Private constructor for default format.
     */
    private JobItemFormat(boolean useJobIcon, String displayName, List<String> lore, 
                         List<String> loreWithoutJob, boolean glowWhenJoined, 
                         boolean glowWhenNotJoined, int amount, 
                         boolean hideAttributes, boolean hideEnchants) {
        this.useJobIcon = useJobIcon;
        this.displayName = displayName;
        this.lore = lore;
        this.loreWithoutJob = loreWithoutJob;
        this.glowWhenJoined = glowWhenJoined;
        this.glowWhenNotJoined = glowWhenNotJoined;
        this.amount = amount;
        this.hideAttributes = hideAttributes;
        this.hideEnchants = hideEnchants;
    }
    
    /**
     * Get default job item format.
     */
    public static JobItemFormat getDefault() {
        return new JobItemFormat(
            true,  // useJobIcon
            "&e&l{job_name}",  // displayName
            getDefaultLore(),  // lore
            getDefaultLoreWithoutJob(),  // loreWithoutJob
            true,  // glowWhenJoined
            false,  // glowWhenNotJoined
            1,  // amount
            true,  // hideAttributes
            true  // hideEnchants
        );
    }
    
    private static List<String> getDefaultLore() {
        return Arrays.asList(
            "&7{job_description}",
            "",
            "&7Max Level: &e{job_max_level}",
            "",
            "&7Your Status:",
            "&8├ &7Level: &a{player_level}&7/&e{job_max_level}",
            "&8├ &7XP: &b{player_xp}",
            "&8├ &7Progress: &e{progress_percent}%",
            "&8└ &7Status: {job_status}",
            "",
            "&7Progress to Next Level:",
            "&8└ {progress_bar}",
            "",
            "&e▶ Left-Click to open job menu",
            "&a▶ Shift-Left-Click to join/leave"
        );
    }
    
    private static List<String> getDefaultLoreWithoutJob() {
        return Arrays.asList(
            "&7{job_description}",
            "",
            "&7Max Level: &e{job_max_level}",
            "",
            "&cYou don't have this job",
            "",
            "&e▶ Left-Click to open job menu",
            "&a▶ Shift-Left-Click to join"
        );
    }
    
    // Getters
    public boolean isUseJobIcon() {
        return useJobIcon;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public List<String> getLore() {
        return new ArrayList<>(lore);
    }
    
    public List<String> getLoreWithoutJob() {
        return new ArrayList<>(loreWithoutJob);
    }
    
    public boolean isGlowWhenJoined() {
        return glowWhenJoined;
    }
    
    public boolean isGlowWhenNotJoined() {
        return glowWhenNotJoined;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public boolean isHideAttributes() {
        return hideAttributes;
    }
    
    public boolean isHideEnchants() {
        return hideEnchants;
    }
}