package fr.ax_dev.universejobs.menu.config;

import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for action item formatting in menus.
 * Allows customization of how action items are displayed.
 */
public class ActionItemFormat {
    
    private final List<String> loreBonus;
    private final int amount;
    private final boolean glow;
    private final boolean hideAttributes;
    private final boolean hideEnchants;
    
    public ActionItemFormat(ConfigurationSection config) {
        this.loreBonus = config.getStringList("lore_bonus");
        this.amount = config.getInt("amount", 1);
        this.glow = config.getBoolean("glow", false);
        this.hideAttributes = config.getBoolean("hide-attributes", true);
        this.hideEnchants = config.getBoolean("hide-enchants", true);
    }
    
    /**
     * Private constructor for default format.
     */
    private ActionItemFormat(List<String> loreBonus, int amount, boolean glow, 
                           boolean hideAttributes, boolean hideEnchants) {
        this.loreBonus = loreBonus;
        this.amount = amount;
        this.glow = glow;
        this.hideAttributes = hideAttributes;
        this.hideEnchants = hideEnchants;
    }
    
    /**
     * Get default action item format.
     */
    public static ActionItemFormat getDefault() {
        return new ActionItemFormat(
            getDefaultLoreBonus(),  // loreBonus
            1,  // amount
            false,  // glow
            true,  // hideAttributes
            true  // hideEnchants
        );
    }
    
    private static List<String> getDefaultLoreBonus() {
        return Arrays.asList(
            "",
            "&7Rewards:",
            "&8├ &7XP: &a+{action_xp}",
            "&8└ &7Money: &6${action_money}",
            "",
            "&7Requirements: {action_requirements}"
        );
    }
    
    
    // Getters
    public List<String> getLoreBonus() {
        return new ArrayList<>(loreBonus);
    }
    
    public int getAmount() {
        return amount;
    }
    
    public boolean isGlow() {
        return glow;
    }
    
    public boolean isHideAttributes() {
        return hideAttributes;
    }
    
    public boolean isHideEnchants() {
        return hideEnchants;
    }
}