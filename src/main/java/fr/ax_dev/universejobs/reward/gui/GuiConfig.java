package fr.ax_dev.universejobs.reward.gui;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.item.ModelDataComponentConfig;
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

        // Get global defaults for GuiItems
        ConfigurationSection globalDefaults = null;
        try {
            UniverseJobs plugin = UniverseJobs.getInstance();
            if (plugin != null) {
                globalDefaults = plugin.getConfig().getConfigurationSection("gui-default-settings");
            }
        } catch (Exception e) {
            // Ignore if plugin not available
        }

        this.navigation = new NavigationConfig(config.getConfigurationSection("navigation"), globalDefaults);
        this.fillItems = new HashMap<>();
        this.rewardItemConfig = new RewardItemConfig(config.getConfigurationSection("reward-item"));

        loadItems(config, globalDefaults);
        loadRewardSlots(config);
        loadFillItems(config);
    }
    
    /**
     * Load custom items from configuration.
     */
    private void loadItems(ConfigurationSection config, ConfigurationSection globalDefaults) {
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
                if (itemConfig != null) {
                    GuiItem item = new GuiItem(itemConfig, globalDefaults);
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
        private final ModelDataComponentConfig modelData;
        private final Map<Enchantment, Integer> enchantments;
        private final List<Integer> slots;
        private final String action;
        private final boolean glowing;
        private final String playerHead;
        private final boolean hideAttributes;
        private final boolean hideEnchants;

        public GuiItem(ConfigurationSection config) {
            this(config, null);
        }

        public GuiItem(ConfigurationSection config, ConfigurationSection globalDefaults) {
            // Get global defaults
            ConfigurationSection defaultsSection = resolveDefaultsSection(globalDefaults);
            Map<String, Object> defaults = getGlobalDefaults(defaultsSection);

            this.materialName = config.getString("material", (String) defaults.getOrDefault("material", "BARRIER"));
            this.amount = config.getInt("amount", (Integer) defaults.getOrDefault("amount", 1));
            this.displayName = config.getString("display-name", config.getString("name", (String) defaults.getOrDefault("display-name", "")));
            this.lore = config.contains("lore") ? config.getStringList("lore") : (List<String>) defaults.getOrDefault("lore", new ArrayList<>());
            ModelDataComponentConfig defaultModelData = defaultsSection != null
                    ? ModelDataComponentConfig.fromSection(defaultsSection)
                    : ModelDataComponentConfig.empty();
            ModelDataComponentConfig resolvedModelData = ModelDataComponentConfig.fromSection(config);
            this.modelData = resolvedModelData.isEmpty() ? defaultModelData : resolvedModelData;
            this.enchantments = loadEnchantments(config);
            this.slots = config.getIntegerList("slots");
            this.action = config.getString("action", (String) defaults.getOrDefault("action", ""));
            this.glowing = config.getBoolean("glowing", (Boolean) defaults.getOrDefault("glow", false));
            this.playerHead = config.getString("player-head", "");
            this.hideAttributes = config.contains("hide-attributes") ? config.getBoolean("hide-attributes") : (Boolean) defaults.getOrDefault("hide-attributes", true);
            this.hideEnchants = config.contains("hide-enchants") ? config.getBoolean("hide-enchants") : (Boolean) defaults.getOrDefault("hide-enchants", true);
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

        private static ConfigurationSection resolveDefaultsSection(ConfigurationSection explicitDefaults) {
            if (explicitDefaults != null) {
                return explicitDefaults;
            }
            UniverseJobs plugin = UniverseJobs.getInstance();
            if (plugin != null) {
                return plugin.getConfig().getConfigurationSection("gui-default-settings");
            }
            return null;
        }

        private static Map<String, Object> getGlobalDefaults(ConfigurationSection defaultsSection) {
            try {

                if (defaultsSection != null) {
                    Map<String, Object> defaults = new HashMap<>();
                    defaults.put("enabled", defaultsSection.getBoolean("enabled", true));
                    defaults.put("amount", defaultsSection.getInt("amount", 1));
                    defaults.put("display-name", defaultsSection.getString("display-name", ""));
                    defaults.put("material", defaultsSection.getString("material", "GRAY_STAINED_GLASS_PANE"));
                    defaults.put("lore", defaultsSection.getStringList("lore"));
                    defaults.put("glow", defaultsSection.getBoolean("glow", false));
                    defaults.put("hide-attributes", defaultsSection.getBoolean("hide-attributes", true));
                    defaults.put("hide-enchants", defaultsSection.getBoolean("hide-enchants", true));
                    defaults.put("sound", defaultsSection.getString("sound", ""));
                    defaults.put("action", defaultsSection.getString("action", "none"));
                    return defaults;
                }
            } catch (Exception e) {
                // Silently fall back to hardcoded defaults if there's any issue
            }

            // Fallback defaults that match config.yml gui-default-settings
            Map<String, Object> defaults = new HashMap<>();
            defaults.put("enabled", true);
            defaults.put("amount", 1);
            defaults.put("display-name", "");
            defaults.put("material", "GRAY_STAINED_GLASS_PANE");
            defaults.put("lore", new ArrayList<>());
            defaults.put("glow", false);
            defaults.put("hide-attributes", true);
            defaults.put("hide-enchants", true);
            defaults.put("sound", "");
            defaults.put("action", "none");
            return defaults;
        }

        // Getters
        public String getMaterialName() { return materialName; }
        public int getAmount() { return amount; }
        public String getDisplayName() { return displayName; }
        public List<String> getLore() { return lore; }
        public ModelDataComponentConfig getModelData() { return modelData; }
        public Map<Enchantment, Integer> getEnchantments() { return enchantments; }
        public List<Integer> getSlots() { return slots; }
        public String getAction() { return action; }
        public boolean isGlowing() { return glowing; }
        public String getPlayerHead() { return playerHead; }
        public boolean isHideAttributes() { return hideAttributes; }
        public boolean isHideEnchants() { return hideEnchants; }
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
        private final GuiItem back;

        public NavigationConfig(ConfigurationSection config) {
            this(config, null);
        }

        public NavigationConfig(ConfigurationSection config, ConfigurationSection globalDefaults) {
            if (config != null) {
                this.previousPage = config.contains("previous-page") ?
                    new GuiItem(config.getConfigurationSection("previous-page"), globalDefaults) : null;
                this.nextPage = config.contains("next-page") ?
                    new GuiItem(config.getConfigurationSection("next-page"), globalDefaults) : null;
                this.close = config.contains("close") ?
                    new GuiItem(config.getConfigurationSection("close"), globalDefaults) : null;
                this.refresh = config.contains("refresh") ?
                    new GuiItem(config.getConfigurationSection("refresh"), globalDefaults) : null;
                this.info = config.contains("info") ?
                    new GuiItem(config.getConfigurationSection("info"), globalDefaults) : null;
                this.back = config.contains("back") ?
                    new GuiItem(config.getConfigurationSection("back"), globalDefaults) : null;
            } else {
                // Default navigation items
                this.previousPage = null;
                this.nextPage = null;
                this.close = null;
                this.refresh = null;
                this.info = null;
                this.back = null;
            }
        }
        
        // Getters
        public GuiItem getPreviousPage() { return previousPage; }
        public GuiItem getNextPage() { return nextPage; }
        public GuiItem getClose() { return close; }
        public GuiItem getRefresh() { return refresh; }
        public GuiItem getInfo() { return info; }
        public GuiItem getBack() { return back; }
    }
    
    public static class RewardItemConfig {
        private final Map<String, String> materials;
        private final Map<String, String> displayNames;
        private final Map<String, List<String>> loreTemplates;
        private final Map<String, String> statusIndicators;
        private final String clickInstruction;
        private final String timeFormat;
        private final List<String> infoButtonLore;
        private final Map<String, String> texts;

        public RewardItemConfig(ConfigurationSection config) {
            this.materials = new HashMap<>();
            this.displayNames = new HashMap<>();
            this.loreTemplates = new HashMap<>();
            this.statusIndicators = new HashMap<>();

            if (config != null) {
                // Load materials
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

                // Load display names
                ConfigurationSection displayNamesSection = config.getConfigurationSection("display-names");
                if (displayNamesSection != null) {
                    this.displayNames.put("retrievable", displayNamesSection.getString("retrievable", "<#32CD32><bold>✓ {reward_name}</bold>"));
                    this.displayNames.put("blocked", displayNamesSection.getString("blocked", "<#FF6B6B><bold>✗ {reward_name}</bold>"));
                    this.displayNames.put("retrieved", displayNamesSection.getString("retrieved", "<#808080><bold>✓ {reward_name}</bold>"));
                } else {
                    this.displayNames.put("retrievable", "<#32CD32><bold>✓ {reward_name}</bold>");
                    this.displayNames.put("blocked", "<#FF6B6B><bold>✗ {reward_name}</bold>");
                    this.displayNames.put("retrieved", "<#808080><bold>✓ {reward_name}</bold>");
                }

                // Load lore templates
                ConfigurationSection loreSection = config.getConfigurationSection("lore");
                if (loreSection != null) {
                    this.loreTemplates.put("retrievable", loreSection.getStringList("retrievable"));
                    this.loreTemplates.put("blocked", loreSection.getStringList("blocked"));
                    this.loreTemplates.put("retrieved", loreSection.getStringList("retrieved"));
                } else {
                    this.loreTemplates.put("retrievable", Arrays.asList("<#32CD32>Status: Ready to claim!", "<gray>Level required: <#FFD700>{level}", "", "<#abffb3>Click to claim reward!"));
                    this.loreTemplates.put("blocked", Arrays.asList("<#FF6B6B>Status: Requirements not met", "<gray>Level required: <#FFD700>{level}", "<gray>Your level: <#FF6B6B>{player_level}", "", "<gray>Level up to unlock this reward!"));
                    this.loreTemplates.put("retrieved", Arrays.asList("<#808080>Status: Already claimed", "<gray>Level required: <#FFD700>{level}", "<gray>Claimed on: <#808080>{claim_date}"));
                }

                // Extract status indicators from display names (fallback)
                this.statusIndicators.put("retrievable", "✓");
                this.statusIndicators.put("blocked", "✗");
                this.statusIndicators.put("retrieved", "✓");

                this.clickInstruction = "&a▶ Click to claim!";
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
                // Default configuration
                this.materials.put("retrievable", "LIME_SHULKER_BOX");
                this.materials.put("blocked", "RED_SHULKER_BOX");
                this.materials.put("retrieved", "GRAY_SHULKER_BOX");

                this.displayNames.put("retrievable", "<#32CD32><bold>✓ {reward_name}</bold>");
                this.displayNames.put("blocked", "<#FF6B6B><bold>✗ {reward_name}</bold>");
                this.displayNames.put("retrieved", "<#808080><bold>✓ {reward_name}</bold>");

                this.loreTemplates.put("retrievable", Arrays.asList("<#32CD32>Status: Ready to claim!", "<gray>Level required: <#FFD700>{level}", "", "<#abffb3>Click to claim reward!"));
                this.loreTemplates.put("blocked", Arrays.asList("<#FF6B6B>Status: Requirements not met", "<gray>Level required: <#FFD700>{level}", "<gray>Your level: <#FF6B6B>{player_level}", "", "<gray>Level up to unlock this reward!"));
                this.loreTemplates.put("retrieved", Arrays.asList("<#808080>Status: Already claimed", "<gray>Level required: <#FFD700>{level}", "<gray>Claimed on: <#808080>{claim_date}"));

                this.statusIndicators.put("retrievable", "✓");
                this.statusIndicators.put("blocked", "✗");
                this.statusIndicators.put("retrieved", "✓");

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
        public String getDisplayName(String status) { return displayNames.get(status); }
        public List<String> getLoreTemplate(String status) { return loreTemplates.getOrDefault(status, new ArrayList<>()); }
        public String getStatusIndicator(String status) { return statusIndicators.get(status); }
        public String getClickInstruction() { return clickInstruction; }
        public String getTimeFormat() { return timeFormat; }
        public List<String> getInfoButtonLore() { return infoButtonLore; }
        public String getText(String key) { return texts.getOrDefault(key, ""); }

        // Legacy compatibility methods
        public List<String> getLoreFormat() { return loreTemplates.getOrDefault("retrievable", new ArrayList<>()); }
        public String getNameFormat() { return displayNames.getOrDefault("retrievable", "{reward_name}"); }
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