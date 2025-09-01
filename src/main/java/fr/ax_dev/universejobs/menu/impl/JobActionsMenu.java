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
        Material material = MaterialUtils.getMaterialForTarget(groupedInfo.target, firstAction.actionType);
        String materialName = material.name();
        
        // Create display name showing the target
        String displayName = groupedInfo.target;
        if (firstAction.action.getDisplayName() != null && !firstAction.action.getDisplayName().isEmpty()) {
            displayName = firstAction.action.getDisplayName();
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
            lore.addAll(firstAction.getLore());
        }
        
        // Add action types and their rewards
        lore.add("");
        lore.add("<gray>Available Actions:");
        
        for (ActionInfo actionInfo : groupedInfo.actions) {
            JobAction action = actionInfo.action;
            String actionTypeStr = actionInfo.actionType.name().toLowerCase();
            actionTypeStr = actionTypeStr.substring(0, 1).toUpperCase() + actionTypeStr.substring(1);
            
            lore.add("<gray>• <#FFD700>" + actionTypeStr + "<gray>: <#abffb3>+" + action.getXp() + " XP<gray>, <#FFD700>$" + action.getMoney());
        }
        
        // Add combined requirements (if any exist)
        addCombinedRequirements(lore, groupedInfo);
        
        return lore;
    }
    
    /**
     * Add combined requirements from all actions for the same target.
     */
    private void addCombinedRequirements(List<String> lore, GroupedActionInfo groupedInfo) {
        Set<String> allRequirements = new HashSet<>();
        
        for (ActionInfo actionInfo : groupedInfo.actions) {
            JobAction action = actionInfo.action;
            
            // Collect all unique requirements
            if (action.getEnchantLevel() != null && !action.getEnchantLevel().isEmpty()) {
                allRequirements.add("Enchant Level: " + action.getEnchantLevel());
            }
            
            if (action.hasPotionTypeRequirements()) {
                allRequirements.add("Potion Types: " + String.join(", ", action.getPotionTypes()));
            }
            
            if (action.hasProfessionRequirements()) {
                allRequirements.add("Professions: " + String.join(", ", action.getProfessions()));
            }
            
            if (action.hasColorRequirements()) {
                allRequirements.add("Colors: " + String.join(", ", action.getColors()));
            }
            
            if (action.hasNbtRequirements()) {
                allRequirements.add("Item Types: " + String.join(", ", action.getNbtTags()));
            }
            
            if (!action.getInteractType().equals("RIGHT_CLICK")) {
                allRequirements.add("Interact: " + action.getInteractType().replace("_", " "));
            }
        }
        
        // Add requirements section if any exist
        if (!allRequirements.isEmpty()) {
            lore.add("");
            lore.add("<gray>Requirements:");
            List<String> sortedRequirements = new ArrayList<>(allRequirements);
            sortedRequirements.sort(String::compareTo);
            
            for (int i = 0; i < sortedRequirements.size(); i++) {
                String prefix = i == sortedRequirements.size() - 1 ? "└" : "├";
                lore.add("<gray>" + prefix + " <gray>" + sortedRequirements.get(i).replace(": ", ": <#FFD700>"));
            }
        }
    }
    
    /**
     * Build action lore using centralized approach with proper message system.
     */
    private List<String> buildActionLore(JobAction action) {
        List<String> lore = new ArrayList<>();
        
        // Add custom lore from action config
        if (action.getLore() != null && !action.getLore().isEmpty()) {
            lore.addAll(action.getLore());
        }
        
        // Add special requirements
        addSpecialRequirements(lore, action);
        
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
     * Add special requirements to the lore based on action type and conditions.
     */
    private void addSpecialRequirements(List<String> lore, JobAction action) {
        boolean hasRequirements = false;
        
        // Enchant level requirement
        if (action.getEnchantLevel() != null && !action.getEnchantLevel().isEmpty()) {
            if (!hasRequirements) {
                lore.add("");
                lore.add("<gray>Requirements:");
                hasRequirements = true;
            }
            lore.add("<gray>├ <gray>Enchant Level: <#FFD700>" + action.getEnchantLevel());
        }
        
        // Potion type requirement
        if (action.hasPotionTypeRequirements()) {
            if (!hasRequirements) {
                lore.add("");
                lore.add("<gray>Requirements:");
                hasRequirements = true;
            }
            if (action.getPotionTypes().size() == 1) {
                lore.add("<gray>├ <gray>Potion Type: <#FFD700>" + action.getPotionTypes().get(0));
            } else {
                lore.add("<gray>├ <gray>Potion Types: <#FFD700>" + String.join("<gray>, <#FFD700>", action.getPotionTypes()));
            }
        }
        
        // Profession requirement
        if (action.hasProfessionRequirements()) {
            if (!hasRequirements) {
                lore.add("");
                lore.add("<gray>Requirements:");
                hasRequirements = true;
            }
            if (action.getProfessions().size() == 1) {
                lore.add("<gray>├ <gray>Profession: <#FFD700>" + action.getProfessions().get(0));
            } else {
                lore.add("<gray>├ <gray>Professions: <#FFD700>" + String.join("<gray>, <#FFD700>", action.getProfessions()));
            }
        }
        
        // Color requirement
        if (action.hasColorRequirements()) {
            if (!hasRequirements) {
                lore.add("");
                lore.add("<gray>Requirements:");
                hasRequirements = true;
            }
            if (action.getColors().size() == 1) {
                lore.add("<gray>├ <gray>Color: <#FFD700>" + action.getColors().get(0));
            } else {
                lore.add("<gray>├ <gray>Colors: <#FFD700>" + String.join("<gray>, <#FFD700>", action.getColors()));
            }
        }
        
        // NBT requirement
        if (action.hasNbtRequirements()) {
            if (!hasRequirements) {
                lore.add("");
                lore.add("<gray>Requirements:");
                hasRequirements = true;
            }
            if (action.getNbtTags().size() == 1) {
                lore.add("<gray>├ <gray>Item Type: <#FFD700>" + action.getNbtTags().get(0));
            } else {
                lore.add("<gray>├ <gray>Item Types: <#FFD700>" + String.join("<gray>, <#FFD700>", action.getNbtTags()));
            }
        }
        
        // Interact type requirement
        if (!action.getInteractType().equals("RIGHT_CLICK")) {
            if (!hasRequirements) {
                lore.add("");
                lore.add("<gray>Requirements:");
                hasRequirements = true;
            }
            lore.add("<gray>├ <gray>Interact: <#FFD700>" + action.getInteractType().replace("_", " "));
        }
        
        // Close the requirements section
        if (hasRequirements) {
            // Replace the last ├ with └ for better formatting
            if (!lore.isEmpty()) {
                String lastLine = lore.get(lore.size() - 1);
                if (lastLine.contains("├")) {
                    lore.set(lore.size() - 1, lastLine.replace("├", "└"));
                }
            }
        }
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