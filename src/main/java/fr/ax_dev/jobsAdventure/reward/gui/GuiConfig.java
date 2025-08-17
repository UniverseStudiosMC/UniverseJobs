package fr.ax_dev.jobsAdventure.reward.gui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;

import java.util.*;

/**
 * Configuration class for customizable reward GUIs.
 * Supports full customization of slots, items, navigation, etc.
 */
public class GuiConfig {
    
    private final String title;
    private final int size;
    private final Map<String, GuiItem> items;
    private final List<Integer> rewardSlots;
    private final NavigationConfig navigation;
    private final Map<String, Object> fillItems;
    
    /**
     * Create a new GuiConfig from configuration.
     * 
     * @param config The configuration section
     */
    public GuiConfig(ConfigurationSection config) {
        this.title = config.getString("title", "&6Rewards");
        this.size = config.getInt("size", 54);
        this.items = new HashMap<>();
        this.rewardSlots = new ArrayList<>();
        this.navigation = new NavigationConfig(config.getConfigurationSection("navigation"));
        this.fillItems = new HashMap<>();
        
        loadItems(config);
        loadRewardSlots(config);
        loadFillItems(config);
    }
    
    /**
     * Load custom items from configuration.
     */
    private void loadItems(ConfigurationSection config) {
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
                if (itemConfig != null) {
                    GuiItem item = new GuiItem(itemConfig);
                    items.put(key, item);
                }
            }
        }
    }
    
    /**
     * Load reward slots from configuration.
     */
    private void loadRewardSlots(ConfigurationSection config) {
        List<Integer> slots = config.getIntegerList("reward-slots");
        if (!slots.isEmpty()) {
            rewardSlots.addAll(slots);
        } else {
            // Default: rows 1-4 (slots 9-44), excluding navigation slots
            for (int i = 9; i < 45; i++) {
                rewardSlots.add(i);
            }
        }
    }
    
    /**
     * Load fill items configuration.
     */
    private void loadFillItems(ConfigurationSection config) {
        ConfigurationSection fillSection = config.getConfigurationSection("fill-items");
        if (fillSection == null) {
            // Fallback to old "fill" section name for compatibility
            fillSection = config.getConfigurationSection("fill");
        }
        
        if (fillSection != null) {
            fillItems.put("enabled", fillSection.getBoolean("enabled", false));
            fillItems.put("material", fillSection.getString("material", "GRAY_STAINED_GLASS_PANE"));
            fillItems.put("name", fillSection.getString("name", " "));
            fillItems.put("slots", fillSection.getIntegerList("slots"));
        }
    }
    
    // Getters
    public String getTitle() { return title; }
    public int getSize() { return size; }
    public Map<String, GuiItem> getItems() { return items; }
    public List<Integer> getRewardSlots() { return rewardSlots; }
    public NavigationConfig getNavigation() { return navigation; }
    public Map<String, Object> getFillItems() { return fillItems; }
    
    /**
     * Represents a custom GUI item.
     */
    public static class GuiItem {
        private final String materialName;
        private final int amount;
        private final String displayName;
        private final List<String> lore;
        private final int customModelData;
        private final Map<Enchantment, Integer> enchantments;
        private final List<Integer> slots;
        private final String action;
        private final boolean glowing;
        
        public GuiItem(ConfigurationSection config) {
            this.materialName = config.getString("material", "BARRIER");
            this.amount = config.getInt("amount", 1);
            this.displayName = config.getString("display-name", config.getString("name", ""));
            this.lore = config.getStringList("lore");
            this.customModelData = config.getInt("custom-model-data", -1);
            this.enchantments = loadEnchantments(config);
            this.slots = config.getIntegerList("slots");
            this.action = config.getString("action", "");
            this.glowing = config.getBoolean("glowing", false);
        }
        
        private Map<Enchantment, Integer> loadEnchantments(ConfigurationSection config) {
            Map<Enchantment, Integer> enchants = new HashMap<>();
            ConfigurationSection enchSection = config.getConfigurationSection("enchantments");
            if (enchSection != null) {
                for (String enchName : enchSection.getKeys(false)) {
                    try {
                        Enchantment enchant = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchName.toLowerCase()));
                        if (enchant != null) {
                            enchants.put(enchant, enchSection.getInt(enchName));
                        }
                    } catch (Exception e) {
                        // Invalid enchantment, skip
                    }
                }
            }
            return enchants;
        }
        
        // Getters
        public String getMaterialName() { return materialName; }
        public int getAmount() { return amount; }
        public String getDisplayName() { return displayName; }
        public List<String> getLore() { return lore; }
        public int getCustomModelData() { return customModelData; }
        public Map<Enchantment, Integer> getEnchantments() { return enchantments; }
        public List<Integer> getSlots() { return slots; }
        public String getAction() { return action; }
        public boolean isGlowing() { return glowing; }
    }
    
    /**
     * Navigation configuration for GUI.
     */
    public static class NavigationConfig {
        private final GuiItem previousPage;
        private final GuiItem nextPage;
        private final GuiItem close;
        private final GuiItem refresh;
        private final GuiItem info;
        
        public NavigationConfig(ConfigurationSection config) {
            if (config != null) {
                this.previousPage = config.contains("previous-page") ? 
                    new GuiItem(config.getConfigurationSection("previous-page")) : null;
                this.nextPage = config.contains("next-page") ? 
                    new GuiItem(config.getConfigurationSection("next-page")) : null;
                this.close = config.contains("close") ? 
                    new GuiItem(config.getConfigurationSection("close")) : null;
                this.refresh = config.contains("refresh") ? 
                    new GuiItem(config.getConfigurationSection("refresh")) : null;
                this.info = config.contains("info") ? 
                    new GuiItem(config.getConfigurationSection("info")) : null;
            } else {
                // Default navigation items
                this.previousPage = null;
                this.nextPage = null;
                this.close = null;
                this.refresh = null;
                this.info = null;
            }
        }
        
        // Getters
        public GuiItem getPreviousPage() { return previousPage; }
        public GuiItem getNextPage() { return nextPage; }
        public GuiItem getClose() { return close; }
        public GuiItem getRefresh() { return refresh; }
        public GuiItem getInfo() { return info; }
    }
}