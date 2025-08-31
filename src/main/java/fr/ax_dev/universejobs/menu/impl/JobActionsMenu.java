package fr.ax_dev.universejobs.menu.impl;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.action.JobAction;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.menu.BaseMenu;
import fr.ax_dev.universejobs.menu.config.ActionItemFormat;
import fr.ax_dev.universejobs.menu.config.MenuItemConfig;
import fr.ax_dev.universejobs.menu.config.SingleMenuConfig;
import fr.ax_dev.universejobs.menu.config.SimpleConfigurationSection;
import fr.ax_dev.universejobs.menu.utils.MaterialUtils;
import fr.ax_dev.universejobs.menu.utils.MenuItemUtils;
import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Menu showing job actions only.
 * Implements InventoryHolder for better integration and uses centralized approach.
 */
public class JobActionsMenu extends BaseMenu implements InventoryHolder {
    
    private static final int HEADER_SLOT = 4;
    private static final String ACTION_TYPE = "action";
    
    private final Job job;
    private final List<ActionInfo> actionInfos;
    private final Map<String, String> cachedPlaceholders;
    public JobActionsMenu(UniverseJobs plugin, org.bukkit.entity.Player player, String jobId, SingleMenuConfig config) {
        super(plugin, player, config);
        
        this.job = plugin.getJobManager().getJob(jobId);
        if (this.job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }
        
        plugin.getLanguageManager();
        this.actionInfos = new ArrayList<>();
        this.cachedPlaceholders = new HashMap<>();
        
        // Load data efficiently using centralized approach
        loadJobData();
        
        // Pre-calculate and cache placeholders for performance
        this.cachedPlaceholders.putAll(createJobPlaceholders());
        
        // Initialize menu after all fields are set
        initialize();
    }
    
    @Override
    protected void createInventory() {
        String title = config.getTitle().replace("{job_name}", job.getDisplayName());
        title = processPlaceholders(title);
        Component titleComponent = fr.ax_dev.universejobs.utils.MessageUtils.parseMessage(title);
        this.inventory = org.bukkit.Bukkit.createInventory(this, config.getSize(), titleComponent);
    }
    
    /**
     * Load job data efficiently using centralized approach.
     */
    private void loadJobData() {
        loadJobActionsEfficiently();
    }
    
    /**
     * Load all actions for this job efficiently.
     */
    private void loadJobActionsEfficiently() {
        for (ActionType actionType : job.getActionTypes()) {
            for (JobAction action : job.getActions(actionType)) {
                actionInfos.add(new ActionInfo(actionType, action));
            }
        }
    }
    
    @Override
    protected void populateInventory() {
        addStaticItems();
        addFillItems();
        
        List<DisplayItem> displayItems = createDisplayItems();
        addPaginatedItems(displayItems);
        addNavigationItems();
    }
    
    /**
     * Add static header item.
     */
    private void addStaticItems() {
        if (config.getStaticItems().containsKey("info-header")) {
            MenuItemConfig headerConfig = config.getStaticItems().get("info-header");
            ItemStack headerItem = createMenuItem(headerConfig, cachedPlaceholders);
            inventory.setItem(HEADER_SLOT, headerItem);
        }
    }
    
    /**
     * Create display items for actions only.
     */
    private List<DisplayItem> createDisplayItems() {
        List<DisplayItem> displayItems = new ArrayList<>(actionInfos.size());
        
        // Add actions efficiently
        actionInfos.forEach(actionInfo -> 
            displayItems.add(new DisplayItem(ACTION_TYPE, actionInfo)));
        
        return displayItems;
    }
    
    /**
     * Add paginated items to inventory using optimal slot allocation.
     */
    private void addPaginatedItems(List<DisplayItem> displayItems) {
        List<Integer> contentSlots = config.getContentSlots();
        int itemsPerPage = config.getItemsPerPage();
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, displayItems.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex >= contentSlots.size()) break;
            
            DisplayItem displayItem = displayItems.get(i);
            ItemStack item = null;
            
            if (ACTION_TYPE.equals(displayItem.type)) {
                item = createActionItemOptimized((ActionInfo) displayItem.data);
            }
            
            if (item != null) {
                inventory.setItem(contentSlots.get(slotIndex), item);
            }
        }
    }
    
    /**
     * Create optimized action item using proper error handling.
     */
    private ItemStack createActionItemOptimized(ActionInfo actionInfo) {
        JobAction action = actionInfo.action;
        List<String> lore = buildActionLore(action);
        
        // Use action target as material or default to appropriate material
        Material material = MaterialUtils.getMaterialForTarget(action.getTarget(), actionInfo.actionType);
        String materialName = material.name();
        
        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
            materialName, 
            action.getDisplayName() != null ? action.getDisplayName() : actionInfo.actionType + ": " + action.getTarget(), 
            lore, 
            false
        );
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, cachedPlaceholders);
    }
    
    /**
     * Build action lore using centralized approach with proper message system.
     */
    private List<String> buildActionLore(JobAction action) {
        List<String> lore = new ArrayList<>();
        
        // Add custom lore from action config
        if (action.getLore() != null && !action.getLore().isEmpty()) {
            lore.addAll(action.getLore());
        } else {
            lore.add("Perform this action to earn rewards");
        }
        
        // Add XP and money rewards
        lore.add("");
        lore.add("<gray>Rewards:");
        lore.add("<gray>├ <gray>XP: <#abffb3>+" + action.getXp());
        if (action.getMoney() > 0) {
            lore.add("<gray>└ <gray>Money: <#FFD700>$" + action.getMoney());
        } else {
            lore.add("<gray>└ <gray>Money: <#FFD700>$0");
        }
        
        // Add format bonus lore from config
        ActionItemFormat format = config.getActionItemFormat();
        if (format != null && format.getLoreBonus() != null) {
            // Skip format bonus as we already added our own formatting
        }
        
        return lore;
    }
    
    /**
     * Add navigation items using configuration and proper placeholder replacement.
     */
    private void addNavigationItems() {
        Map<String, String> navPlaceholders = createNavigationPlaceholders();
        
        config.getNavigationItems().forEach((key, itemConfig) -> {
            if (itemConfig.isEnabled()) {
                ItemStack navItem = createMenuItem(itemConfig, navPlaceholders);
                for (int slot : itemConfig.getSlots()) {
                    inventory.setItem(slot, navItem);
                }
            }
        });
    }
    
    @Override
    public void handleClick(int slot, InventoryClickEvent event) {
        event.setCancelled(true);
        
        // Handle navigation items
        if (handleNavigationClick(slot)) {
            return;
        }
        
        // Handle content items - actions don't need special click behavior
        // They're just informational
    }
    
    @Override
    protected boolean hasNextPage() {
        return (currentPage + 1) * config.getItemsPerPage() < actionInfos.size();
    }
    
    /**
     * Create job-specific placeholders for better performance.
     */
    private Map<String, String> createJobPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{job_id}", job.getId());
        placeholders.put("{job_name}", job.getDisplayName());
        placeholders.put("{job_description}", job.getDescription());
        placeholders.put("{total_actions}", String.valueOf(actionInfos.size()));
        return placeholders;
    }
    
    /**
     * Create navigation placeholders efficiently.
     */
    private Map<String, String> createNavigationPlaceholders() {
        Map<String, String> placeholders = MenuItemUtils.createNavigationPlaceholders(
            currentPage, actionInfos.size(), config.getItemsPerPage());
        placeholders.put("total_actions", String.valueOf(actionInfos.size()));
        placeholders.put("job_name", job.getDisplayName());
        return placeholders;
    }
    
    /**
     * Action information container for better organization.
     */
    private static class ActionInfo {
        final ActionType actionType;
        final JobAction action;
        
        ActionInfo(ActionType actionType, JobAction action) {
            this.actionType = actionType;
            this.action = action;
        }
    }
    
    /**
     * Display item container for unified handling.
     */
    private static class DisplayItem {
        final String type;
        final Object data;
        
        DisplayItem(String type, Object data) {
            this.type = type;
            this.data = data;
        }
    }
}