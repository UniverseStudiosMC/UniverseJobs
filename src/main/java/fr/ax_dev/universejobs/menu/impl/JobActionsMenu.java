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
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Menu showing job actions only.
 * Implements InventoryHolder for better integration and uses centralized approach.
 */
public class JobActionsMenu extends BaseMenu {
    
    private static final int HEADER_SLOT = 4;
    private static final String ACTION_TYPE = "action";
    
    private final Job job;
    private final Map<String, List<ActionInfo>> groupedActions;
    private final Map<String, String> cachedPlaceholders;
    private final Map<Integer, GroupedActionInfo> slotToActionMap;
    public JobActionsMenu(UniverseJobs plugin, org.bukkit.entity.Player player, String jobId, SingleMenuConfig config) {
        super(plugin, player, config);
        
        this.job = plugin.getJobManager().getJob(jobId);
        if (this.job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }
        
        plugin.getLanguageManager();
        this.groupedActions = new HashMap<>();
        this.cachedPlaceholders = new HashMap<>();
        this.slotToActionMap = new HashMap<>();
        
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
        // Update cached placeholders to ensure they're current
        Map<String, String> currentPlaceholders = createJobPlaceholders();
        
        for (Map.Entry<String, MenuItemConfig> entry : config.getStaticItems().entrySet()) {
            MenuItemConfig itemConfig = entry.getValue();
            if (itemConfig.isEnabled()) {
                ItemStack staticItem = createMenuItem(itemConfig, currentPlaceholders);
                for (int slot : itemConfig.getSlots()) {
                    inventory.setItem(slot, staticItem);
                }
            }
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
        int itemsPerPage = contentSlots.size();
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, displayItems.size());
        
        slotToActionMap.clear();
        
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex >= contentSlots.size()) break;
            
            DisplayItem displayItem = displayItems.get(i);
            ItemStack item = null;
            
            if (ACTION_TYPE.equals(displayItem.type)) {
                GroupedActionInfo actionInfo = (GroupedActionInfo) displayItem.data;
                item = createGroupedActionItem(actionInfo);
                int slot = contentSlots.get(slotIndex);
                slotToActionMap.put(slot, actionInfo);
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
        // Use the first action's material or default to appropriate material
        ActionInfo firstAction = groupedInfo.actions.get(0);
        Material material = MaterialUtils.getSourceMaterialForTarget(groupedInfo.target, firstAction.actionType);
        String materialName = material.name();
        
        // Get display name from config
        String displayName = "";
        ActionItemFormat format = config.getActionItemFormat();
        boolean useFirstLoreAsDisplayName = false;
        
        if (format != null && format.getDisplayNameEnabled() != null && !format.getDisplayNameEnabled().isEmpty()) {
            // Use configured display name with placeholders
            displayName = format.getDisplayNameEnabled()
                .replace("{action_target}", groupedInfo.target)
                .replace("{action_display_name}", firstAction.action.getDisplayName() != null ? firstAction.action.getDisplayName() : groupedInfo.target)
                .replace("{action_type}", firstAction.actionType.name())
                .replace("{action_name}", firstAction.action.getName());
            
            // Apply default formatting if no formatting is present
            if (!displayName.startsWith("<") && !displayName.startsWith("&")) {
                displayName = "<!italic><white>" + displayName;
            }
        } else {
            // If display_name_bonus is empty, use first line of lore_bonus as display name
            useFirstLoreAsDisplayName = true;
        }
        
        // Build lore with special handling for display name
        List<String> lore = buildGroupedActionLore(groupedInfo, useFirstLoreAsDisplayName);
        
        // If we need to use first lore line as display name, extract it
        if (useFirstLoreAsDisplayName && !lore.isEmpty()) {
            displayName = lore.get(0);
            lore.remove(0); // Remove first line since it becomes the display name
            
            // Apply default formatting if no formatting is present
            if (!displayName.startsWith("<") && !displayName.startsWith("&")) {
                displayName = "<!italic><white>" + displayName;
            }
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
    private List<String> buildGroupedActionLore(GroupedActionInfo groupedInfo, boolean includeFirstLine) {
        List<String> lore = new ArrayList<>();
        
        // Add each action's information using the YAML format
        for (ActionInfo actionInfo : groupedActions.get(groupedInfo.target)) {
            JobAction action = actionInfo.action;
            String actionTypeStr = actionInfo.actionType.name().toLowerCase();
            actionTypeStr = actionTypeStr.substring(0, 1).toUpperCase() + actionTypeStr.substring(1);
            
            // Use the action-item-format from YAML config
            ActionItemFormat format = config.getActionItemFormat();
            if (format != null && format.getLoreBonus() != null) {
                List<String> formatLore = new ArrayList<>(format.getLoreBonus());
                
                // Apply hide_line logic FIRST on raw lines before any processing
                List<String> filteredLore = applyHideLineLogicRaw(formatLore, action, format);
                List<String> processedLore = new ArrayList<>();
                
                // Replace placeholders for this specific action
                for (String line : filteredLore) {
                    // Replace placeholders using configurable formats
                    line = replacePlaceholder(line, "action_type", actionTypeStr);
                    line = replacePlaceholder(line, "action_target", action.getTarget());
                    line = replacePlaceholder(line, "action_name", action.getName());
                    
                    // Display name placeholder
                    String actionDisplayName = action.getDisplayName() != null && !action.getDisplayName().isEmpty() 
                        ? action.getDisplayName() 
                        : action.getTarget();
                    line = replacePlaceholder(line, "action_display_name", actionDisplayName);
                    
                    // Handle {action_lore} placeholder - should expand to multiple lines
                    if (line.contains("{action_lore}")) {
                        if (action.getLore() != null && !action.getLore().isEmpty()) {
                            // Replace this line with all lore lines using configurable format
                            for (String loreLine : action.getLore()) {
                                String loreFormat = plugin.getConfig().getString("placeholders.action_lore", "<gray>{value}");
                                String processedLine = loreFormat.replace("{value}", loreLine);
                                processedLore.add(processedLine);
                            }
                            continue; // Skip adding the original line since we replaced it
                        } else {
                            // If no lore, skip this line entirely
                            continue;
                        }
                    }
                    
                    // Base values (without boost)
                    line = replacePlaceholder(line, "action_xp_base", String.valueOf(action.getXp()));
                    line = replacePlaceholder(line, "action_money_base", String.valueOf(action.getMoney()));
                    
                    // Calculate boosted values
                    double xpMultiplier = plugin.getBonusManager().getTotalMultiplier(player.getUniqueId(), job.getId());
                    double moneyMultiplier = plugin.getMoneyBonusManager().getTotalMultiplier(player.getUniqueId(), job.getId());
                    
                    // Values with boost
                    double boostedXp = action.getXp() * xpMultiplier;
                    double boostedMoney = action.getMoney() * moneyMultiplier;
                    line = replacePlaceholder(line, "action_xp", String.format("%.1f", boostedXp));
                    line = replacePlaceholder(line, "action_money", String.format("%.2f", boostedMoney));
                    
                    // Handle cooldown placeholder if exists
                    if (line.contains("{action_cooldown}")) {
                        // For now, using a placeholder value - you can implement actual cooldown logic
                        line = replacePlaceholder(line, "action_cooldown", "0");
                    }
                    
                    // Multiplier placeholders (only show if boost is active)
                    String xpMultiplierText = "";
                    String moneyMultiplierText = "";
                    
                    if (xpMultiplier > 1.0) {
                        String xpMultiplierFormat = plugin.getConfig().getString("placeholders.action_xp_multiplier", "<gray>(<white>x{value}<gray>)");
                        xpMultiplierText = xpMultiplierFormat.replace("{value}", String.format("%.1f", xpMultiplier));
                    }
                    
                    if (moneyMultiplier > 1.0) {
                        String moneyMultiplierFormat = plugin.getConfig().getString("placeholders.action_money_multiplier", "<gray>(<white>x{value}<gray>)");
                        moneyMultiplierText = moneyMultiplierFormat.replace("{value}", String.format("%.1f", moneyMultiplier));
                    }
                    
                    line = line.replace("{action_xp_multiplier}", xpMultiplierText);
                    line = line.replace("{action_money_multiplier}", moneyMultiplierText);
                    
                    // Apply default formatting if no formatting is present and line is not empty
                    if (!line.trim().isEmpty() && !line.startsWith("<") && !line.startsWith("&")) {
                        line = "<!italic><white>" + line;
                    }
                    processedLore.add(line);
                }
                
                lore.addAll(processedLore);
            }
        }
        
        return lore;
    }
    
    /**
     * Apply hide_line logic on raw lore lines BEFORE any placeholder processing.
     */
    private List<String> applyHideLineLogicRaw(List<String> rawLoreLines, JobAction action, ActionItemFormat format) {
        List<String> result = new ArrayList<>();
        boolean hasNoMoney = action.getMoney() <= 0;
        boolean hasNoXp = action.getXp() <= 0;
        
        List<Integer> hideWhenNoMoney = format.getHideWhenNoMoney();
        List<Integer> hideWhenNoXp = format.getHideWhenNoXp();
        
        for (int i = 0; i < rawLoreLines.size(); i++) {
            int lineNumber = i + 1; // Lines are 1-indexed in config
            
            boolean shouldHide = false;
            
            // Check if this line should be hidden when no money
            if (hasNoMoney && hideWhenNoMoney.contains(lineNumber)) {
                shouldHide = true;
            }
            
            // Check if this line should be hidden when no XP
            if (hasNoXp && hideWhenNoXp.contains(lineNumber)) {
                shouldHide = true;
            }
            
            if (!shouldHide) {
                result.add(rawLoreLines.get(i));
            }
        }
        
        return result;
    }
    
    /**
     * Replace a placeholder using configurable format from config.yml.
     * Supports specific configurations per action type for action_type placeholder.
     */
    private String replacePlaceholder(String text, String placeholder, String value) {
        if (!text.contains("{" + placeholder + "}")) {
            return text;
        }
        
        String format;
        
        // Special handling for action_type placeholder with type-specific configurations
        if ("action_type".equals(placeholder)) {
            // Try to get specific format for this action type
            String specificFormat = plugin.getConfig().getString("placeholders.action_type." + value.toUpperCase());
            if (specificFormat != null) {
                format = specificFormat;
            } else {
                // Fall back to default format
                format = plugin.getConfig().getString("placeholders.action_type.default", "<white>{value}");
            }
        } else {
            // For other placeholders, use standard format
            format = plugin.getConfig().getString("placeholders." + placeholder, "<white>{value}");
        }
        
        String formatted = format.replace("{value}", value);
        return text.replace("{" + placeholder + "}", formatted);
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
        if (handleNavigationClickWithSound(slot)) {
            return;
        }
        
        // Handle action item clicks
        GroupedActionInfo actionInfo = slotToActionMap.get(slot);
        if (actionInfo != null) {
            executeActionCommands(actionInfo);
        }
    }
    
    /**
     * Execute commands associated with an action when clicked.
     */
    private void executeActionCommands(GroupedActionInfo actionInfo) {
        // Get commands from action item format config
        ActionItemFormat format = config.getActionItemFormat();
        if (format == null) return;
        
        List<String> commands = format.getCommands();
        if (commands == null || commands.isEmpty()) return;
        
        // Execute each command for the first action (primary action)
        ActionInfo firstAction = actionInfo.actions.get(0);
        for (String command : commands) {
            String processedCommand = command
                .replace("{player}", player.getName())
                .replace("{job_id}", job.getId())
                .replace("{job_name}", job.getDisplayName())
                .replace("{action_type}", firstAction.actionType.name())
                .replace("{action_target}", firstAction.action.getTarget())
                .replace("{action_name}", firstAction.action.getName())
                .replace("{action_display_name}", firstAction.action.getDisplayName() != null ? firstAction.action.getDisplayName() : firstAction.action.getTarget());
            
            // Execute as console or player based on prefix
            if (processedCommand.startsWith("[console]")) {
                String consoleCmd = processedCommand.substring(9).trim();
                plugin.getFoliaManager().runNextTick(() -> 
                    org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), consoleCmd)
                );
            } else if (processedCommand.startsWith("[player]")) {
                String playerCmd = processedCommand.substring(8).trim();
                plugin.getFoliaManager().runNextTick(() -> 
                    player.performCommand(playerCmd)
                );
            } else if (processedCommand.startsWith("[close]")) {
                close();
            } else {
                // Default to player command
                plugin.getFoliaManager().runNextTick(() -> 
                    player.performCommand(processedCommand)
                );
            }
        }
    }
    
    @Override
    protected boolean hasNextPage() {
        int itemsPerPage = config.getContentSlots().size();
        return (currentPage + 1) * itemsPerPage < groupedActions.size();
    }
    
    @Override
    protected void handleBackButton() {
        // Override to go back to job menu instead of closing
        try {
            plugin.getMenuManager().openJobMenu(player, job.getId());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to open job menu when going back: " + e.getMessage());
            // Fallback to closing the menu
            close();
        }
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
        int itemsPerPage = config.getContentSlots().size();
        Map<String, String> placeholders = MenuItemUtils.createNavigationPlaceholders(
            currentPage, groupedActions.size(), itemsPerPage);
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