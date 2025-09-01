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
    private final Map<String, List<ActionInfo>> groupedActions;
    private final Map<String, String> cachedPlaceholders;
    public JobActionsMenu(UniverseJobs plugin, org.bukkit.entity.Player player, String jobId, SingleMenuConfig config) {
        super(plugin, player, config);
        
        this.job = plugin.getJobManager().getJob(jobId);
        if (this.job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }
        
        plugin.getLanguageManager();
        this.groupedActions = new HashMap<>();
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
     * Load all actions for this job efficiently and group them by target.
     */
    private void loadJobActionsEfficiently() {
        for (ActionType actionType : job.getActionTypes()) {
            for (JobAction action : job.getActions(actionType)) {
                String target = action.getTarget();
                groupedActions.computeIfAbsent(target, k -> new ArrayList<>())
                    .add(new ActionInfo(actionType, action));
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
     * Create display items for grouped actions.
     */
    private List<DisplayItem> createDisplayItems() {
        List<DisplayItem> displayItems = new ArrayList<>(groupedActions.size());
        
        // Add grouped actions efficiently
        groupedActions.forEach((target, actions) -> 
            displayItems.add(new DisplayItem(ACTION_TYPE, new GroupedActionInfo(target, actions))));
        
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
                item = createGroupedActionItem((GroupedActionInfo) displayItem.data);
            }
            
            if (item != null) {
                inventory.setItem(contentSlots.get(slotIndex), item);
            }
        }
    }
    
    /**
     * Create grouped action item combining multiple actions for the same target.
     */
    private ItemStack createGroupedActionItem(GroupedActionInfo groupedInfo) {
        List<String> lore = buildGroupedActionLore(groupedInfo);
        
        // Use the first action's material or default to appropriate material
        ActionInfo firstAction = groupedInfo.actions.get(0);
        Material material = MaterialUtils.getSourceMaterialForTarget(groupedInfo.target, firstAction.actionType);
        String materialName = material.name();
        
        // Create display name showing the target
        String displayName = groupedInfo.target;
        if (firstAction.action.getDisplayName() != null && !firstAction.action.getDisplayName().isEmpty()) {
            displayName = firstAction.action.getDisplayName();
            // Remove default Minecraft formatting if present
            if (!displayName.startsWith("<") && !displayName.startsWith("&")) {
                displayName = "<!italic><white>" + displayName;
            }
        } else {
            // Apply default formatting to target name
            displayName = "<!italic><white>" + displayName;
        }
        
        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
            materialName, 
            displayName, 
            lore, 
            false
        );
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, cachedPlaceholders);
    }
    
    /**
     * Build grouped action lore combining multiple actions for the same target.
     */
    private List<String> buildGroupedActionLore(GroupedActionInfo groupedInfo) {
        List<String> lore = new ArrayList<>();
        
        // Add custom lore from first action config
        JobAction firstAction = groupedInfo.actions.get(0).action;
        if (firstAction.getLore() != null && !firstAction.getLore().isEmpty()) {
            for (String loreLine : firstAction.getLore()) {
                // Apply default formatting if no formatting is present and line is not empty
                if (!loreLine.trim().isEmpty() && !loreLine.startsWith("<") && !loreLine.startsWith("&")) {
                    lore.add("<!italic><white>" + loreLine);
                } else {
                    lore.add(loreLine);
                }
            }
        }
        
        // Add each action's information using the YAML format
        for (ActionInfo actionInfo : groupedActions.get(groupedInfo.target)) {
            JobAction action = actionInfo.action;
            String actionTypeStr = actionInfo.actionType.name().toLowerCase();
            actionTypeStr = actionTypeStr.substring(0, 1).toUpperCase() + actionTypeStr.substring(1);
            
            // Use the action-item-format from YAML config
            ActionItemFormat format = config.getActionItemFormat();
            if (format != null && format.getLoreBonus() != null) {
                List<String> formatLore = new ArrayList<>(format.getLoreBonus());
                
                // Replace placeholders for this specific action
                for (int i = 0; i < formatLore.size(); i++) {
                    String line = formatLore.get(i);
                    line = line.replace("{action_type}", actionTypeStr);
                    line = line.replace("{action_target}", action.getTarget());
                    line = line.replace("{action_xp}", String.valueOf(action.getXp()));
                    line = line.replace("{action_money}", String.valueOf(action.getMoney()));
                    
                    // Build requirements string
                    String requirements = buildRequirementsString(action);
                    line = line.replace("{action_requirements}", requirements);
                    
                    // Apply default formatting if no formatting is present and line is not empty
                    if (!line.trim().isEmpty() && !line.startsWith("<") && !line.startsWith("&")) {
                        line = "<!italic><white>" + line;
                    }
                    
                    formatLore.set(i, line);
                }
                
                lore.addAll(formatLore);
            }
        }
        
        return lore;
    }
    
    /**
     * Build requirements string for an action.
     */
    private String buildRequirementsString(JobAction action) {
        List<String> requirements = new ArrayList<>();
        
        if (action.getEnchantLevel() != null && !action.getEnchantLevel().isEmpty()) {
            requirements.add("Enchant Level: " + action.getEnchantLevel());
        }
        
        if (action.hasPotionTypeRequirements()) {
            requirements.add("Potion Types: " + String.join(", ", action.getPotionTypes()));
        }
        
        if (action.hasProfessionRequirements()) {
            requirements.add("Professions: " + String.join(", ", action.getProfessions()));
        }
        
        if (action.hasColorRequirements()) {
            requirements.add("Colors: " + String.join(", ", action.getColors()));
        }
        
        if (action.hasNbtRequirements()) {
            requirements.add("Item Types: " + String.join(", ", action.getNbtTags()));
        }
        
        if (!action.getInteractType().equals("RIGHT_CLICK")) {
            requirements.add("Interact: " + action.getInteractType().replace("_", " "));
        }
        
        return requirements.isEmpty() ? "None" : String.join(", ", requirements);
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
        return (currentPage + 1) * config.getItemsPerPage() < groupedActions.size();
    }
    
    /**
     * Create job-specific placeholders for better performance.
     */
    private Map<String, String> createJobPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{job_id}", job.getId());
        placeholders.put("{job_name}", job.getDisplayName());
        placeholders.put("{job_description}", job.getDescription());
        int totalActions = groupedActions.values().stream().mapToInt(List::size).sum();
        placeholders.put("{total_actions}", String.valueOf(totalActions));
        return placeholders;
    }
    
    /**
     * Create navigation placeholders efficiently.
     */
    private Map<String, String> createNavigationPlaceholders() {
        int totalActions = groupedActions.values().stream().mapToInt(List::size).sum();
        Map<String, String> placeholders = MenuItemUtils.createNavigationPlaceholders(
            currentPage, groupedActions.size(), config.getItemsPerPage());
        placeholders.put("total_actions", String.valueOf(totalActions));
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
     * Grouped action information for same target.
     */
    private static class GroupedActionInfo {
        final String target;
        final List<ActionInfo> actions;
        
        GroupedActionInfo(String target, List<ActionInfo> actions) {
            this.target = target;
            this.actions = actions;
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