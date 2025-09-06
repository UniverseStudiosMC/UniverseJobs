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
    private final List<String> commands;
    private final String displaynamebonus;
    private final List<Integer> hideWhenNoMoney;
    private final List<Integer> hideWhenNoXp;
    
    public ActionItemFormat(ConfigurationSection config) {
        this.loreBonus = config.getStringList("lore_bonus");
        this.amount = config.getInt("amount", 1);
        this.glow = config.getBoolean("glow", false);
        this.hideAttributes = config.getBoolean("hide-attributes", true);
        this.hideEnchants = config.getBoolean("hide-enchants", true);
        this.commands = config.getStringList("commands");
        this.displaynamebonus = config.getString("display_name_bonus", "");
        
        // Parse hide_line configuration
        this.hideWhenNoMoney = new ArrayList<>();
        this.hideWhenNoXp = new ArrayList<>();
        
        ConfigurationSection hideLineSection = config.getConfigurationSection("hide_line");
        if (hideLineSection != null) {
            // Support both single value and list
            if (hideLineSection.contains("when_no_money")) {
                if (hideLineSection.isInt("when_no_money")) {
                    this.hideWhenNoMoney.add(hideLineSection.getInt("when_no_money"));
                } else if (hideLineSection.isList("when_no_money")) {
                    this.hideWhenNoMoney.addAll(hideLineSection.getIntegerList("when_no_money"));
                }
            }
            
            if (hideLineSection.contains("when_no_xp")) {
                if (hideLineSection.isInt("when_no_xp")) {
                    this.hideWhenNoXp.add(hideLineSection.getInt("when_no_xp"));
                } else if (hideLineSection.isList("when_no_xp")) {
                    this.hideWhenNoXp.addAll(hideLineSection.getIntegerList("when_no_xp"));
                }
            }
        }
    }
    
    public ActionItemFormat() {
        this.loreBonus = getDefaultLoreBonus();
        this.amount = 1;
        this.glow = false;
        this.hideAttributes = true;
        this.hideEnchants = true;
        this.commands = new ArrayList<>();
        this.displaynamebonus = "";
        this.hideWhenNoMoney = new ArrayList<>();
        this.hideWhenNoXp = new ArrayList<>();
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
    
    public List<String> getCommands() {
        return commands != null ? new ArrayList<>(commands) : new ArrayList<>();
    }
    
    public String getDisplayNameEnabled() {
        return displaynamebonus != null ? displaynamebonus : "";
    }
    
    public List<Integer> getHideWhenNoMoney() {
        return new ArrayList<>(hideWhenNoMoney);
    }
    
    public List<Integer> getHideWhenNoXp() {
        return new ArrayList<>(hideWhenNoXp);
    }
}