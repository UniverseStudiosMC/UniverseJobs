package fr.ax_dev.universejobs.menu.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Configuration class for a single menu.
 */
public class SingleMenuConfig {
    
    private final String title;
    private final int size;
    private final MenuItemConfig fillItem;
    private final List<Integer> fillSlots;
    private final List<Integer> contentSlots;
    private final Map<String, List<Integer>> navigationSlots;
    private final Map<String, MenuItemConfig> staticItems;
    private final Map<String, MenuItemConfig> navigationItems;
    private final boolean enablePagination;
    private final int itemsPerPage;
    private final JobItemFormat jobItemFormat;
    
    public SingleMenuConfig(ConfigurationSection config) {
        this.title = config.getString("title", "&6Jobs Menu");
        this.size = config.getInt("size", 54);
        this.enablePagination = config.getBoolean("pagination.enabled", true);
        this.itemsPerPage = config.getInt("pagination.items-per-page", 28);
        
        // Load fill item configuration
        ConfigurationSection fillSection = config.getConfigurationSection("fill-item");
        if (fillSection != null && fillSection.getBoolean("enabled", false)) {
            this.fillItem = new MenuItemConfig(fillSection);
        } else {
            this.fillItem = null;
        }
        
        // Load fill slots
        this.fillSlots = config.getIntegerList("fill-slots");
        if (fillSlots.isEmpty()) {
            // Default fill slots for entire menu
            for (int i = 0; i < size; i++) {
                fillSlots.add(i);
            }
        }
        
        // Load content slots
        this.contentSlots = config.getIntegerList("content-slots");
        if (contentSlots.isEmpty()) {
            // Default content slots (center area for most menus)
            generateDefaultContentSlots();
        }
        
        // Load navigation slots
        this.navigationSlots = new HashMap<>();
        ConfigurationSection navSection = config.getConfigurationSection("navigation-slots");
        if (navSection != null) {
            for (String navKey : navSection.getKeys(false)) {
                List<Integer> slots = navSection.getIntegerList(navKey);
                this.navigationSlots.put(navKey, slots);
            }
        } else {
            generateDefaultNavigationSlots();
        }
        
        // Load static items
        this.staticItems = new HashMap<>();
        ConfigurationSection staticSection = config.getConfigurationSection("static-items");
        if (staticSection != null) {
            for (String itemKey : staticSection.getKeys(false)) {
                ConfigurationSection itemSection = staticSection.getConfigurationSection(itemKey);
                if (itemSection != null) {
                    this.staticItems.put(itemKey, new MenuItemConfig(itemSection));
                }
            }
        }
        
        // Load navigation items
        this.navigationItems = new HashMap<>();
        ConfigurationSection navItemsSection = config.getConfigurationSection("navigation-items");
        if (navItemsSection != null) {
            for (String itemKey : navItemsSection.getKeys(false)) {
                ConfigurationSection itemSection = navItemsSection.getConfigurationSection(itemKey);
                if (itemSection != null) {
                    this.navigationItems.put(itemKey, new MenuItemConfig(itemSection));
                }
            }
        } else {
            generateDefaultNavigationItems();
        }
        
        // Load job item format configuration
        ConfigurationSection jobFormatSection = config.getConfigurationSection("job-item-format");
        if (jobFormatSection != null) {
            this.jobItemFormat = new JobItemFormat(jobFormatSection);
        } else {
            this.jobItemFormat = JobItemFormat.getDefault();
        }
    }
    
    /**
     * Generate default content slots for the menu.
     */
    private void generateDefaultContentSlots() {
        // Generate content slots in the middle area (avoiding borders)
        for (int row = 1; row < (size / 9) - 1; row++) {
            for (int col = 1; col < 8; col++) {
                contentSlots.add(row * 9 + col);
            }
        }
    }
    
    /**
     * Generate default navigation slots.
     */
    private void generateDefaultNavigationSlots() {
        int rows = size / 9;
        int lastRow = rows - 1;
        
        // Previous page (bottom left)
        navigationSlots.put("previous", Arrays.asList(lastRow * 9));
        
        // Next page (bottom right)  
        navigationSlots.put("next", Arrays.asList(lastRow * 9 + 8));
        
        // Close button (bottom center)
        navigationSlots.put("close", Arrays.asList(lastRow * 9 + 4));
        
        // Back button (if needed)
        navigationSlots.put("back", Arrays.asList(lastRow * 9 + 3));
        
        // Info button
        navigationSlots.put("info", Arrays.asList(lastRow * 9 + 5));
    }
    
    /**
     * Generate default navigation items.
     */
    private void generateDefaultNavigationItems() {
        navigationItems.put("previous", MenuItemConfig.navigationItem(
            "ARROW", "&e← Previous Page", "previous_page", 
            navigationSlots.getOrDefault("previous", Arrays.asList(45)),
            "&7Click to go to the previous page"
        ));
        
        navigationItems.put("next", MenuItemConfig.navigationItem(
            "ARROW", "&eNext Page →", "next_page",
            navigationSlots.getOrDefault("next", Arrays.asList(53)),
            "&7Click to go to the next page"
        ));
        
        navigationItems.put("close", MenuItemConfig.navigationItem(
            "BARRIER", "&cClose", "close",
            navigationSlots.getOrDefault("close", Arrays.asList(49)),
            "&7Click to close this menu"
        ));
        
        navigationItems.put("back", MenuItemConfig.navigationItem(
            "ARROW", "&7← Back", "back",
            navigationSlots.getOrDefault("back", Arrays.asList(48)),
            "&7Click to go back"
        ));
        
        navigationItems.put("info", MenuItemConfig.navigationItem(
            "BOOK", "&6Info", "none",
            navigationSlots.getOrDefault("info", Arrays.asList(50)),
            "&7Menu Information",
            "&8Page: {current_page}/{total_pages}"
        ));
    }
    
    // Static factory methods for default configurations
    
    /**
     * Get default configuration for the main jobs menu.
     */
    public static SingleMenuConfig getDefaultMainMenu() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("title", "&6&lJobs Menu");
        configMap.put("size", 54);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("enabled", true);
        pagination.put("items-per-page", 28);
        configMap.put("pagination", pagination);
        
        return new SingleMenuConfig(new SimpleConfigurationSection(configMap));
    }
    
    /**
     * Get default configuration for individual job menus.
     */
    public static SingleMenuConfig getDefaultJobMenu() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("title", "&6&l{job_name} Menu");
        configMap.put("size", 45);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("enabled", false);
        pagination.put("items-per-page", 21);
        configMap.put("pagination", pagination);
        
        return new SingleMenuConfig(new SimpleConfigurationSection(configMap));
    }
    
    /**
     * Get default configuration for job actions menu.
     */
    public static SingleMenuConfig getDefaultActionsMenu() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("title", "&6&l{job_name} Actions & Rewards");
        configMap.put("size", 54);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("enabled", true);
        pagination.put("items-per-page", 35);
        configMap.put("pagination", pagination);
        
        return new SingleMenuConfig(new SimpleConfigurationSection(configMap));
    }
    
    /**
     * Get default configuration for rankings menu.
     */
    public static SingleMenuConfig getDefaultRankingsMenu() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("title", "&6&lGlobal Rankings");
        configMap.put("size", 54);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("enabled", true);
        pagination.put("items-per-page", 35);
        configMap.put("pagination", pagination);
        
        return new SingleMenuConfig(new SimpleConfigurationSection(configMap));
    }
    
    // Getters
    public String getTitle() { return title; }
    public int getSize() { return size; }
    public MenuItemConfig getFillItem() { return fillItem; }
    public List<Integer> getFillSlots() { return new ArrayList<>(fillSlots); }
    public List<Integer> getContentSlots() { return new ArrayList<>(contentSlots); }
    public Map<String, List<Integer>> getNavigationSlots() { return new HashMap<>(navigationSlots); }
    public Map<String, MenuItemConfig> getStaticItems() { return new HashMap<>(staticItems); }
    public Map<String, MenuItemConfig> getNavigationItems() { return new HashMap<>(navigationItems); }
    public boolean isEnablePagination() { return enablePagination; }
    public int getItemsPerPage() { return itemsPerPage; }
    public JobItemFormat getJobItemFormat() { return jobItemFormat; }
    
}