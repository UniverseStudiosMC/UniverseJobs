package fr.ax_dev.universejobs.reward.gui;

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
    private final Map<Integer, List<Integer>> rewardSlotsByPage;
    private final List<Integer> rewardSlots;
    private final NavigationConfig navigation;
    private final Map<String, Object> fillItems;
    private final RewardItemConfig rewardItemConfig;
    
    /**
     * Create a new GuiConfig from configuration.
     * 
     * @param config The configuration section
     */
    public GuiConfig(ConfigurationSection config) {
        this.title = config.getString("title", "&6Rewards");
        this.size = config.getInt("size", 54);
        this.items = new HashMap<>();
        this.rewardSlotsByPage = new HashMap<>();
        this.rewardSlots = new ArrayList<>();
        this.navigation = new NavigationConfig(config.getConfigurationSection("navigation"));
        this.fillItems = new HashMap<>();
        this.rewardItemConfig = new RewardItemConfig(config.getConfigurationSection("reward-item"));
        
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

        // Load page-specific reward slots
        ConfigurationSection pageSection = config.getConfigurationSection("reward-slots-by-page");
        if (pageSection != null) {
            for (String pageKey : pageSection.getKeys(false)) {
                try {
                    int pageNum = Integer.parseInt(pageKey);
                    List<Integer> pageSlots = pageSection.getIntegerList(pageKey);
                    if (!pageSlots.isEmpty()) {
                        rewardSlotsByPage.put(pageNum, pageSlots);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid page numbers
                }
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

    /**
     * Get reward slots for a specific page.
     * Falls back to default slots if page-specific slots not found.
     */
    public List<Integer> getRewardSlots(int page) {
        return rewardSlotsByPage.getOrDefault(page, rewardSlots);
    }
    public NavigationConfig getNavigation() { return navigation; }
    public Map<String, Object> getFillItems() { return fillItems; }
    public RewardItemConfig getRewardItemConfig() { return rewardItemConfig; }
    
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
    
    public static class RewardItemConfig {
        private final Map<String, String> materials;
        private final List<String> loreFormat;
        private final String nameFormat;
        private final Map<String, String> statusIndicators;
        private final String clickInstruction;
        private final String timeFormat;
        private final List<String> infoButtonLore;
        private final Map<String, String> texts;
        
        public RewardItemConfig(ConfigurationSection config) {
            this.materials = new HashMap<>();
            this.statusIndicators = new HashMap<>();
            
            if (config != null) {
                ConfigurationSection materialsSection = config.getConfigurationSection("materials");
                if (materialsSection != null) {
                    this.materials.put("retrievable", materialsSection.getString("retrievable", "LIME_SHULKER_BOX"));
                    this.materials.put("blocked", materialsSection.getString("blocked", "RED_SHULKER_BOX"));
                    this.materials.put("retrieved", materialsSection.getString("retrieved", "GRAY_SHULKER_BOX"));
                } else {
                    this.materials.put("retrievable", "LIME_SHULKER_BOX");
                    this.materials.put("blocked", "RED_SHULKER_BOX");
                    this.materials.put("retrieved", "GRAY_SHULKER_BOX");
                }
                
                ConfigurationSection statusSection = config.getConfigurationSection("status-indicators");
                if (statusSection != null) {
                    this.statusIndicators.put("retrievable", statusSection.getString("retrievable", "&a✓"));
                    this.statusIndicators.put("blocked", statusSection.getString("blocked", "&c✗"));
                    this.statusIndicators.put("retrieved", statusSection.getString("retrieved", "&7✓"));
                } else {
                    this.statusIndicators.put("retrievable", "&a✓");
                    this.statusIndicators.put("blocked", "&c✗");
                    this.statusIndicators.put("retrieved", "&7✓");
                }
                
                this.loreFormat = config.getStringList("lore-format");
                this.nameFormat = config.getString("name-format", "{status} {name}");
                this.clickInstruction = config.getString("click-instruction", "&a▶ Click to claim!");
                this.timeFormat = config.getString("time-format", "{hours}h");
                this.infoButtonLore = config.getStringList("info-button-lore");
                
                // Load custom texts
                this.texts = new HashMap<>();
                ConfigurationSection textsSection = config.getConfigurationSection("texts");
                if (textsSection != null) {
                    this.texts.put("repeatable_yes", textsSection.getString("repeatable-yes", "&7Repeatable: &aYes"));
                    this.texts.put("repeatable_no", textsSection.getString("repeatable-no", "&7Repeatable: &cNo"));
                    this.texts.put("cooldown_prefix", textsSection.getString("cooldown-prefix", "&7Cooldown: &e"));
                    this.texts.put("rewards_title", textsSection.getString("rewards-title", "&6Rewards:"));
                    this.texts.put("more_items", textsSection.getString("more-items", "&7... and {count} more"));
                    this.texts.put("item_format", textsSection.getString("item-format", "&7- &f{amount}x {name}"));
                    this.texts.put("special_rewards", textsSection.getString("special-rewards", "Special rewards"));
                } else {
                    this.texts.put("repeatable_yes", "&7Repeatable: &aYes");
                    this.texts.put("repeatable_no", "&7Repeatable: &cNo");
                    this.texts.put("cooldown_prefix", "&7Cooldown: &e");
                    this.texts.put("rewards_title", "&6Rewards:");
                    this.texts.put("more_items", "&7... and {count} more");
                    this.texts.put("item_format", "&7- &f{amount}x {name}");
                    this.texts.put("special_rewards", "Special rewards");
                }
            } else {
                this.materials.put("retrievable", "LIME_SHULKER_BOX");
                this.materials.put("blocked", "RED_SHULKER_BOX");
                this.materials.put("retrieved", "GRAY_SHULKER_BOX");
                this.statusIndicators.put("retrievable", "&a✓");
                this.statusIndicators.put("blocked", "&c✗");
                this.statusIndicators.put("retrieved", "&7✓");
                this.loreFormat = Arrays.asList(
                    "{description}",
                    "",
                    "&7Required Level: &e{level}",
                    "&7Status: {status_description}",
                    "{repeatable_info}",
                    "{cooldown}",
                    "{reward_items}",
                    "{economy_reward}",
                    "{commands}",
                    "{click_instruction}"
                );
                this.nameFormat = "{status} {name}";
                this.clickInstruction = "&a▶ Click to claim!";
                this.timeFormat = "{hours}h";
                this.infoButtonLore = new ArrayList<>();
                
                // Default texts
                this.texts = new HashMap<>();
                this.texts.put("repeatable_yes", "&7Repeatable: &aYes");
                this.texts.put("repeatable_no", "&7Repeatable: &cNo");
                this.texts.put("cooldown_prefix", "&7Cooldown: &e");
                this.texts.put("rewards_title", "&6Rewards:");
                this.texts.put("more_items", "&7... and {count} more");
                this.texts.put("item_format", "&7- &f{amount}x {name}");
                this.texts.put("special_rewards", "Special rewards");
            }
        }
        
        public String getMaterial(String status) { return materials.get(status); }
        public String getStatusIndicator(String status) { return statusIndicators.get(status); }
        public List<String> getLoreFormat() { return loreFormat; }
        public String getNameFormat() { return nameFormat; }
        public String getClickInstruction() { return clickInstruction; }
        public String getTimeFormat() { return timeFormat; }
        public List<String> getInfoButtonLore() { return infoButtonLore; }
        public String getText(String key) { return texts.getOrDefault(key, ""); }
    }
    
    public static class DefaultGuiConfig {
        private final int size;
        private final int rewardsPerPage;
        private final String titleFormat;
        private final Map<String, Object> navigationSlots;
        private final Map<String, String> navigationMaterials;
        private final Map<String, String> navigationNames;
        private final Map<String, List<String>> navigationLore;
        private final String fillerMaterial;
        private final String fillerName;
        private final RewardItemConfig rewardItemConfig;
        
        public DefaultGuiConfig(ConfigurationSection config) {
            if (config != null) {
                this.size = config.getInt("size", 54);
                this.rewardsPerPage = config.getInt("rewards-per-page", 45);
                this.titleFormat = config.getString("title-format", "&6{job} Rewards");
                this.fillerMaterial = config.getString("filler.material", "GRAY_STAINED_GLASS_PANE");
                this.fillerName = config.getString("filler.name", " ");
                
                this.navigationSlots = new HashMap<>();
                ConfigurationSection navSection = config.getConfigurationSection("navigation-slots");
                if (navSection != null) {
                    this.navigationSlots.put("previous", navSection.getInt("previous", 45));
                    this.navigationSlots.put("close", navSection.getInt("close", 48));
                    this.navigationSlots.put("info", navSection.getInt("info", 49));
                    this.navigationSlots.put("refresh", navSection.getInt("refresh", 50));
                    this.navigationSlots.put("next", navSection.getInt("next", 53));
                } else {
                    this.navigationSlots.put("previous", 45);
                    this.navigationSlots.put("close", 48);
                    this.navigationSlots.put("info", 49);
                    this.navigationSlots.put("refresh", 50);
                    this.navigationSlots.put("next", 53);
                }
                
                // Load navigation materials
                this.navigationMaterials = new HashMap<>();
                ConfigurationSection matSection = config.getConfigurationSection("navigation-materials");
                if (matSection != null) {
                    this.navigationMaterials.put("previous", matSection.getString("previous", "ARROW"));
                    this.navigationMaterials.put("next", matSection.getString("next", "ARROW"));
                    this.navigationMaterials.put("close", matSection.getString("close", "BARRIER"));
                    this.navigationMaterials.put("refresh", matSection.getString("refresh", "EMERALD"));
                    this.navigationMaterials.put("info", matSection.getString("info", "BOOK"));
                } else {
                    this.navigationMaterials.put("previous", "ARROW");
                    this.navigationMaterials.put("next", "ARROW");
                    this.navigationMaterials.put("close", "BARRIER");
                    this.navigationMaterials.put("refresh", "EMERALD");
                    this.navigationMaterials.put("info", "BOOK");
                }
                
                // Load navigation names
                this.navigationNames = new HashMap<>();
                ConfigurationSection nameSection = config.getConfigurationSection("navigation-names");
                if (nameSection != null) {
                    this.navigationNames.put("previous", nameSection.getString("previous", "&aPrevious Page"));
                    this.navigationNames.put("next", nameSection.getString("next", "&aNext Page"));
                    this.navigationNames.put("close", nameSection.getString("close", "&cClose"));
                    this.navigationNames.put("refresh", nameSection.getString("refresh", "&aRefresh"));
                    this.navigationNames.put("info", nameSection.getString("info", "&6Page {current_page}/{total_pages}"));
                } else {
                    this.navigationNames.put("previous", "&aPrevious Page");
                    this.navigationNames.put("next", "&aNext Page");
                    this.navigationNames.put("close", "&cClose");
                    this.navigationNames.put("refresh", "&aRefresh");
                    this.navigationNames.put("info", "&6Page {current_page}/{total_pages}");
                }
                
                // Load navigation lore
                this.navigationLore = new HashMap<>();
                ConfigurationSection loreSection = config.getConfigurationSection("navigation-lore");
                if (loreSection != null) {
                    this.navigationLore.put("previous", loreSection.getStringList("previous"));
                    this.navigationLore.put("next", loreSection.getStringList("next"));
                    this.navigationLore.put("close", loreSection.getStringList("close"));
                    this.navigationLore.put("refresh", loreSection.getStringList("refresh"));
                    this.navigationLore.put("info", loreSection.getStringList("info"));
                } else {
                    this.navigationLore.put("previous", Arrays.asList("&6Click to go to page {target_page}"));
                    this.navigationLore.put("next", Arrays.asList("&6Click to go to page {target_page}"));
                    this.navigationLore.put("close", Arrays.asList("&6Click to close this menu"));
                    this.navigationLore.put("refresh", Arrays.asList("&6Click to refresh rewards"));
                    this.navigationLore.put("info", Arrays.asList("&7Showing rewards for {job}"));
                }
                
                this.rewardItemConfig = new RewardItemConfig(config.getConfigurationSection("reward-item"));
            } else {
                this.size = 54;
                this.rewardsPerPage = 45;
                this.titleFormat = "&6{job} Rewards";
                this.fillerMaterial = "GRAY_STAINED_GLASS_PANE";
                this.fillerName = " ";
                
                this.navigationSlots = new HashMap<>();
                this.navigationSlots.put("previous", 45);
                this.navigationSlots.put("close", 48);
                this.navigationSlots.put("info", 49);
                this.navigationSlots.put("refresh", 50);
                this.navigationSlots.put("next", 53);
                
                this.navigationMaterials = new HashMap<>();
                this.navigationMaterials.put("previous", "ARROW");
                this.navigationMaterials.put("next", "ARROW");
                this.navigationMaterials.put("close", "BARRIER");
                this.navigationMaterials.put("refresh", "EMERALD");
                this.navigationMaterials.put("info", "BOOK");
                
                this.navigationNames = new HashMap<>();
                this.navigationNames.put("previous", "&aPrevious Page");
                this.navigationNames.put("next", "&aNext Page");
                this.navigationNames.put("close", "&cClose");
                this.navigationNames.put("refresh", "&aRefresh");
                this.navigationNames.put("info", "&6Page {current_page}/{total_pages}");
                
                this.navigationLore = new HashMap<>();
                this.navigationLore.put("previous", Arrays.asList("&6Click to go to page {target_page}"));
                this.navigationLore.put("next", Arrays.asList("&6Click to go to page {target_page}"));
                this.navigationLore.put("close", Arrays.asList("&6Click to close this menu"));
                this.navigationLore.put("refresh", Arrays.asList("&6Click to refresh rewards"));
                this.navigationLore.put("info", Arrays.asList("&7Showing rewards for {job}"));
                
                this.rewardItemConfig = new RewardItemConfig(null);
            }
        }
        
        public int getSize() { return size; }
        public int getRewardsPerPage() { return rewardsPerPage; }
        public String getTitleFormat() { return titleFormat; }
        public int getNavigationSlot(String type) { return (Integer) navigationSlots.getOrDefault(type, -1); }
        public String getNavigationMaterial(String type) { return navigationMaterials.getOrDefault(type, "STONE"); }
        public String getNavigationName(String type) { return navigationNames.getOrDefault(type, ""); }
        public List<String> getNavigationLore(String type) { return navigationLore.getOrDefault(type, new ArrayList<>()); }
        public String getFillerMaterial() { return fillerMaterial; }
        public String getFillerName() { return fillerName; }
        public RewardItemConfig getRewardItemConfig() { return rewardItemConfig; }
    }
}