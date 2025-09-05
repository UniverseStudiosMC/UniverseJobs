package fr.ax_dev.universejobs.menu.config;

import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    
    public ActionItemFormat() {
        this.loreBonus = getDefaultLoreBonus();
        this.amount = 1;
        this.glow = false;
        this.hideAttributes = true;
        this.hideEnchants = true;
    }
    
    private static List<String> getDefaultLoreBonus() {
        return Arrays.asList(
            "",
            "<gray>Rewards:",
            "<gray>├ <gray>XP: <#abffb3>+{action_xp}",
            "<gray>└ <gray>Money: <#FFD700>${action_money}",
            "",
            "<gray>Requirements: {action_requirements}"
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