package fr.ax_dev.universejobs.menu.config;

import org.bukkit.configuration.ConfigurationSection;
import fr.ax_dev.universejobs.UniverseJobs;

import java.util.*;

/**
 * Configuration class for a single menu item.
 */
public class MenuItemConfig {
    
    private final boolean enabled;
    private final String material;
    private final int amount;
    private final String displayName;
    private List<String> lore;
    private final int customModelData;
    private final Map<String, Integer> enchantments;
    private final boolean glow;
    private final boolean hideAttributes;
    private final boolean hideEnchants;
    private final boolean hideToolTip;
    private final List<Integer> slots;
    private final String action;
    private final String actionValue;
    private final String skullOwner;
    private final String playerHead;
    private final String sound;
    private final List<String> commands;
    
    
    // Else configuration for toggle-job
    private final String elseMaterial;
    private final String elseDisplayName;
    private final List<String> elseLore;
    private final int elseCustomModelData;
    
    public MenuItemConfig(ConfigurationSection config) {
        // Get global defaults
        Map<String, Object> defaults = getGlobalDefaults();
        
        this.enabled = config.getBoolean("enabled", (Boolean) defaults.getOrDefault("enabled", true));
        this.material = config.getString("material", (String) defaults.getOrDefault("material", "STONE"));
        this.amount = config.getInt("amount", (Integer) defaults.getOrDefault("amount", 1));
        this.displayName = config.getString("display-name", (String) defaults.getOrDefault("display-name", ""));
        
        // Handle lore - merge defaults with config
        List<String> defaultLore = (List<String>) defaults.getOrDefault("lore", new ArrayList<>());
        this.lore = config.getStringList("lore");
        if (this.lore.isEmpty() && !defaultLore.isEmpty()) {
            this.lore = new ArrayList<>(defaultLore);
        }
        
        this.customModelData = config.getInt("custom-model-data", (Integer) defaults.getOrDefault("custom-model-data", 0));
        this.glow = config.getBoolean("glow", (Boolean) defaults.getOrDefault("glow", false));
        this.hideAttributes = config.getBoolean("hide-attributes", (Boolean) defaults.getOrDefault("hide-attributes", false));
        this.hideEnchants = config.getBoolean("hide-enchants", (Boolean) defaults.getOrDefault("hide-enchants", false));
        this.hideToolTip = config.getBoolean("hideToolTip", false);
        this.slots = config.getIntegerList("slots");
        this.action = config.getString("action", (String) defaults.getOrDefault("action", "none"));
        this.actionValue = config.getString("action-value", "");
        this.skullOwner = config.getString("skull-owner", "");
        this.playerHead = config.getString("player-head", "");
        this.sound = config.getString("sound", (String) defaults.getOrDefault("sound", ""));
        this.commands = config.getStringList("commands");
        
        
        // Load else configuration for toggle-job
        ConfigurationSection elseSection = config.getConfigurationSection("else");
        if (elseSection != null) {
            this.elseMaterial = elseSection.getString("material", "");
            this.elseDisplayName = elseSection.getString("display-name", "");
            this.elseLore = elseSection.getStringList("lore");
            this.elseCustomModelData = elseSection.getInt("custom-model-data", 0);
        } else {
            this.elseMaterial = "";
            this.elseDisplayName = "";
            this.elseLore = new ArrayList<>();
            this.elseCustomModelData = 0;
        }
        
        // Load enchantments
        this.enchantments = new HashMap<>();
        ConfigurationSection enchantSection = config.getConfigurationSection("enchantments");
        if (enchantSection != null) {
            for (String enchantKey : enchantSection.getKeys(false)) {
                int level = enchantSection.getInt(enchantKey, 1);
                this.enchantments.put(enchantKey, level);
            }
        }
    }
    
    /**
     * Create a default item configuration.
     */
    public static MenuItemConfig defaultItem(String material, String displayName, String... lore) {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("enabled", true);
        configMap.put("material", material);
        configMap.put("amount", 1);
        configMap.put("display-name", displayName);
        configMap.put("lore", Arrays.asList(lore));
        configMap.put("custom-model-data", 0);
        configMap.put("glow", false);
        configMap.put("hide-attributes", false);
        configMap.put("hide-enchants", false);
        configMap.put("hideToolTip", false);
        configMap.put("slots", new ArrayList<Integer>());
        configMap.put("action", "none");
        configMap.put("action-value", "");
        
        return new MenuItemConfig(new SimpleConfigurationSection(configMap));
    }
    
    /**
     * Create a navigation item configuration.
     */
    public static MenuItemConfig navigationItem(String material, String displayName, String action, List<Integer> slots, String... lore) {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("enabled", true);
        configMap.put("material", material);
        configMap.put("amount", 1);
        configMap.put("display-name", displayName);
        configMap.put("lore", Arrays.asList(lore));
        configMap.put("custom-model-data", 0);
        configMap.put("glow", false);
        configMap.put("hide-attributes", false);
        configMap.put("hide-enchants", false);
        configMap.put("hideToolTip", false);
        configMap.put("slots", slots);
        configMap.put("action", action);
        configMap.put("action-value", "");
        
        return new MenuItemConfig(new SimpleConfigurationSection(configMap));
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public String getMaterial() { return material; }
    public int getAmount() { return amount; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return new ArrayList<>(lore); }
    public int getCustomModelData() { return customModelData; }
    public Map<String, Integer> getEnchantments() { return new HashMap<>(enchantments); }
    public boolean isGlow() { return glow; }
    public boolean isHideAttributes() { return hideAttributes; }
    public boolean isHideEnchants() { return hideEnchants; }
    public boolean isHideToolTip() { return hideToolTip; }
    public List<Integer> getSlots() { return new ArrayList<>(slots); }
    public String getAction() { return action; }
    public String getActionValue() { return actionValue; }
    public String getSkullOwner() { return skullOwner; }
    public String getPlayerHead() { return playerHead; }
    public String getSound() { return sound; }
    public List<String> getCommands() { return commands; }
    
    
    // Else configuration getters
    public String getElseMaterial() { return elseMaterial; }
    public String getElseDisplayName() { return elseDisplayName; }
    public List<String> getElseLore() { return new ArrayList<>(elseLore); }
    public int getElseCustomModelData() { return elseCustomModelData; }
    
    public boolean hasElseConfiguration() {
        return elseMaterial != null && !elseMaterial.isEmpty();
    }
    
    /**
     * Get global default settings from config.yml
     */
    private static Map<String, Object> getGlobalDefaults() {
        try {
            UniverseJobs plugin = UniverseJobs.getInstance();
            if (plugin != null) {
                ConfigurationSection defaultsSection = plugin.getConfig().getConfigurationSection("gui-default-settings");
                if (defaultsSection != null) {
                    Map<String, Object> defaults = new HashMap<>();
                    defaults.put("enabled", defaultsSection.getBoolean("enabled", true));
                    defaults.put("amount", defaultsSection.getInt("amount", 1));
                    defaults.put("display-name", defaultsSection.getString("display-name", ""));
                    defaults.put("material", defaultsSection.getString("material", "STONE"));
                    defaults.put("lore", defaultsSection.getStringList("lore"));
                    defaults.put("glow", defaultsSection.getBoolean("glow", false));
                    defaults.put("hide-attributes", defaultsSection.getBoolean("hide-attributes", false));
                    defaults.put("hide-enchants", defaultsSection.getBoolean("hide-enchants", false));
                    defaults.put("sound", defaultsSection.getString("sound", ""));
                    defaults.put("custom-model-data", defaultsSection.getInt("custom-model-data", 0));
                    defaults.put("action", defaultsSection.getString("action", "none"));
                    return defaults;
                }
            }
        } catch (Exception e) {
            // Silently fall back to hardcoded defaults if there's any issue
        }
        
        // Fallback defaults
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("enabled", true);
        defaults.put("amount", 1);
        defaults.put("display-name", "");
        defaults.put("material", "STONE");
        defaults.put("lore", new ArrayList<>());
        defaults.put("glow", false);
        defaults.put("hide-attributes", false);
        defaults.put("hide-enchants", false);
        defaults.put("sound", "");
        defaults.put("custom-model-data", 0);
        defaults.put("action", "none");
        return defaults;
    }
    
}