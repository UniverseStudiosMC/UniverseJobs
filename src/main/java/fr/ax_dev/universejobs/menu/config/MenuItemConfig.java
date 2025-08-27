package fr.ax_dev.universejobs.menu.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Configuration class for a single menu item.
 */
public class MenuItemConfig {
    
    private final boolean enabled;
    private final String material;
    private final int amount;
    private final String displayName;
    private final List<String> lore;
    private final int customModelData;
    private final Map<String, Integer> enchantments;
    private final boolean glow;
    private final boolean hideAttributes;
    private final boolean hideEnchants;
    private final List<Integer> slots;
    private final String action;
    private final String actionValue;
    
    public MenuItemConfig(ConfigurationSection config) {
        this.enabled = config.getBoolean("enabled", true);
        this.material = config.getString("material", "STONE");
        this.amount = config.getInt("amount", 1);
        this.displayName = config.getString("display-name", "");
        this.lore = config.getStringList("lore");
        this.customModelData = config.getInt("custom-model-data", 0);
        this.glow = config.getBoolean("glow", false);
        this.hideAttributes = config.getBoolean("hide-attributes", false);
        this.hideEnchants = config.getBoolean("hide-enchants", false);
        this.slots = config.getIntegerList("slots");
        this.action = config.getString("action", "none");
        this.actionValue = config.getString("action-value", "");
        
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
    public List<Integer> getSlots() { return new ArrayList<>(slots); }
    public String getAction() { return action; }
    public String getActionValue() { return actionValue; }
    
}