package fr.ax_dev.universejobs.menu.impl;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.action.JobAction;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.menu.BaseMenu;
import fr.ax_dev.universejobs.menu.config.MenuItemConfig;
import fr.ax_dev.universejobs.menu.config.SingleMenuConfig;
import fr.ax_dev.universejobs.menu.config.SimpleConfigurationSection;
import fr.ax_dev.universejobs.reward.Reward;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Menu showing job actions and their rewards.
 */
public class JobActionsMenu extends BaseMenu {
    
    private final Job job;
    private final List<ActionInfo> actionInfos;
    private final List<Reward> jobRewards;
    
    public JobActionsMenu(UniverseJobs plugin, org.bukkit.entity.Player player, String jobId, SingleMenuConfig config) {
        super(plugin, player, config);
        
        this.job = plugin.getJobManager().getJob(jobId);
        this.actionInfos = new ArrayList<>();
        this.jobRewards = new ArrayList<>();
        
        if (this.job == null) {
            MessageUtils.sendMessage(player, "&cJob not found: " + jobId);
            close();
            return;
        }
        
        loadJobActions();
        loadJobRewards();
    }
    
    /**
     * Load all actions for this job.
     */
    private void loadJobActions() {
        for (ActionType actionType : job.getActionTypes()) {
            List<JobAction> actions = job.getActions(actionType);
            for (JobAction action : actions) {
                actionInfos.add(new ActionInfo(actionType, action));
            }
        }
    }
    
    /**
     * Load all rewards for this job.
     */
    private void loadJobRewards() {
        if (plugin.getRewardManager() != null) {
            jobRewards.addAll(plugin.getRewardManager().getJobRewards(job.getId()));
        }
    }
    
    @Override
    protected void populateInventory() {
        if (job == null) return;
        
        // Clear inventory first
        inventory.clear();
        
        // Add job header
        addJobHeader();
        
        // Add action and reward items
        addActionAndRewardItems();
        
        // Add navigation items
        addNavigationItems();
        
        // Add static items
        addStaticItems();
        
        // Fill empty slots
        addFillItems();
    }
    
    /**
     * Add job header information.
     */
    private void addJobHeader() {
        ItemStack headerItem = createJobHeaderItem();
        inventory.setItem(4, headerItem); // Top center
    }
    
    /**
     * Create job header item.
     */
    private ItemStack createJobHeaderItem() {
        Material iconMaterial;
        try {
            iconMaterial = Material.valueOf(job.getIcon().toUpperCase());
        } catch (IllegalArgumentException e) {
            iconMaterial = Material.STONE;
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("&7" + job.getDescription());
        lore.add("");
        lore.add("&7Total Actions: &e" + actionInfos.size());
        lore.add("&7Total Rewards: &e" + jobRewards.size());
        
        Map<String, Object> configMap = createItemConfigMap(
            job.getIcon(), "&6&l" + job.getName() + " - Actions & Rewards", lore, true
        );
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, getJobPlaceholders());
    }
    
    /**
     * Add action and reward items to the menu.
     */
    private void addActionAndRewardItems() {
        List<Integer> contentSlots = config.getContentSlots();
        
        // Combine actions and rewards into a single list for pagination
        List<DisplayItem> displayItems = new ArrayList<>();
        
        // Add actions
        for (ActionInfo actionInfo : actionInfos) {
            displayItems.add(new DisplayItem("action", actionInfo));
        }
        
        // Add rewards
        for (Reward reward : jobRewards) {
            displayItems.add(new DisplayItem("reward", reward));
        }
        
        // Apply pagination
        int startIndex = currentPage * config.getItemsPerPage();
        int endIndex = Math.min(startIndex + config.getItemsPerPage(), displayItems.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            DisplayItem displayItem = displayItems.get(i);
            int slotIndex = i - startIndex;
            
            if (slotIndex >= contentSlots.size()) break;
            
            int slot = contentSlots.get(slotIndex);
            ItemStack item;
            
            if (displayItem.type.equals("action")) {
                item = createActionItem((ActionInfo) displayItem.data);
            } else {
                item = createRewardItem((Reward) displayItem.data);
            }
            
            inventory.setItem(slot, item);
        }
    }
    
    /**
     * Create an action item.
     */
    private ItemStack createActionItem(ActionInfo actionInfo) {
        ActionType actionType = actionInfo.actionType;
        JobAction action = actionInfo.action;
        
        // Get appropriate material based on action type
        String material = getActionMaterial(actionType);
        
        List<String> lore = new ArrayList<>();
        lore.add("&7Type: &e" + actionType.name().toLowerCase().replace("_", " "));
        
        // Add target information
        if (action.getTarget() != null && !action.getTarget().isEmpty()) {
            lore.add("&7Target: &e" + action.getTarget());
        }
        
        // Add XP reward
        if (action.getXp() > 0) {
            lore.add("&7XP Reward: &b+" + action.getXp());
        }
        
        // Add money reward
        if (action.getMoney() > 0) {
            lore.add("&7Money Reward: &a$" + action.getMoney());
        }
        
        // Add conditions if any
        if (action.hasRequirements()) {
            lore.add("");
            lore.add("&6Conditions:");
            lore.add("&8- Various conditions apply");
        }
        
        lore.add("");
        lore.add("&8Action from job: &7" + job.getName());
        
        Map<String, Object> configMap = createItemConfigMap(
            material, "&e&l" + formatActionName(actionType), lore, false
        );
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, getJobPlaceholders());
    }
    
    /**
     * Create a reward item.
     */
    private ItemStack createRewardItem(Reward reward) {
        List<String> lore = new ArrayList<>();
        lore.add("&7" + reward.getDescription());
        lore.add("");
        lore.add("&7Required Level: &e" + reward.getRequiredLevel());
        
        if (reward.isRepeatable()) {
            lore.add("&7Repeatable: &aYes");
            if (reward.getCooldownHours() > 0) {
                lore.add("&7Cooldown: &e" + reward.getCooldownHours() + "h");
            }
        } else {
            lore.add("&7Repeatable: &cNo");
        }
        
        // Add reward items info
        if (reward.hasEconomyReward()) {
            lore.add("&7Money Reward: &a$" + reward.getEconomyReward());
        }
        
        if (reward.hasCommands()) {
            lore.add("&7Special Rewards: &eYes");
        }
        
        if (!reward.getItems().isEmpty()) {
            lore.add("");
            lore.add("&6Item Rewards:");
            int itemsShown = 0;
            for (Reward.RewardItem item : reward.getItems()) {
                if (itemsShown >= 3) {
                    lore.add("&8... and " + (reward.getItems().size() - 3) + " more");
                    break;
                }
                
                String itemName = item.getDisplayName() != null ? 
                    item.getDisplayName() : item.getMaterial();
                lore.add("&8- &f" + item.getAmount() + "x " + itemName);
                itemsShown++;
            }
        }
        
        lore.add("");
        lore.add("&e▶ Click to view reward details");
        
        Map<String, Object> configMap = createItemConfigMap(
            "CHEST", "&6&l" + reward.getName(), lore, false
        );
        configMap.put("action", "view_reward");
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, getJobPlaceholders());
    }
    
    /**
     * Add navigation items.
     */
    private void addNavigationItems() {
        Map<String, MenuItemConfig> navItems = config.getNavigationItems();
        
        // Previous page
        if (navItems.containsKey("previous") && currentPage > 0) {
            MenuItemConfig prevConfig = navItems.get("previous");
            ItemStack prevItem = createMenuItem(prevConfig, getNavigationPlaceholders());
            
            for (int slot : prevConfig.getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, prevItem);
                }
            }
        }
        
        // Next page
        if (navItems.containsKey("next") && hasNextPage()) {
            MenuItemConfig nextConfig = navItems.get("next");
            ItemStack nextItem = createMenuItem(nextConfig, getNavigationPlaceholders());
            
            for (int slot : nextConfig.getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, nextItem);
                }
            }
        }
        
        // Back button
        if (navItems.containsKey("back")) {
            MenuItemConfig backConfig = navItems.get("back");
            ItemStack backItem = createMenuItem(backConfig, getNavigationPlaceholders());
            
            for (int slot : backConfig.getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, backItem);
                }
            }
        }
        
        // Close button
        if (navItems.containsKey("close")) {
            MenuItemConfig closeConfig = navItems.get("close");
            ItemStack closeItem = createMenuItem(closeConfig, getNavigationPlaceholders());
            
            for (int slot : closeConfig.getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, closeItem);
                }
            }
        }
    }
    
    /**
     * Add static items from configuration.
     */
    private void addStaticItems() {
        for (Map.Entry<String, MenuItemConfig> entry : config.getStaticItems().entrySet()) {
            MenuItemConfig itemConfig = entry.getValue();
            if (!itemConfig.isEnabled()) continue;
            
            ItemStack item = createMenuItem(itemConfig, getJobPlaceholders());
            
            for (int slot : itemConfig.getSlots()) {
                if (slot >= 0 && slot < inventory.getSize() && inventory.getItem(slot) == null) {
                    inventory.setItem(slot, item);
                }
            }
        }
    }
    
    @Override
    public void handleClick(int slot, InventoryClickEvent event) {
        // Handle navigation clicks first
        if (handleNavigationClick(slot)) {
            return;
        }
        
        // Handle content clicks
        List<Integer> contentSlots = config.getContentSlots();
        int slotIndex = contentSlots.indexOf(slot);
        
        if (slotIndex >= 0) {
            List<DisplayItem> displayItems = getAllDisplayItems();
            int itemIndex = currentPage * config.getItemsPerPage() + slotIndex;
            
            if (itemIndex < displayItems.size()) {
                DisplayItem displayItem = displayItems.get(itemIndex);
                
                if (displayItem.type.equals("reward")) {
                    // Open rewards GUI for this specific reward
                    Reward reward = (Reward) displayItem.data;
                    plugin.getRewardGuiManager().openRewardsGui(player, job.getId());
                }
            }
        }
    }
    
    @Override
    protected boolean hasNextPage() {
        List<DisplayItem> displayItems = getAllDisplayItems();
        return (currentPage + 1) * config.getItemsPerPage() < displayItems.size();
    }
    
    @Override
    protected void handleBackButton() {
        plugin.getMenuManager().openJobMenu(player, job.getId());
    }
    
    /**
     * Get all display items for pagination calculation.
     */
    private List<DisplayItem> getAllDisplayItems() {
        List<DisplayItem> displayItems = new ArrayList<>();
        
        for (ActionInfo actionInfo : actionInfos) {
            displayItems.add(new DisplayItem("action", actionInfo));
        }
        
        for (Reward reward : jobRewards) {
            displayItems.add(new DisplayItem("reward", reward));
        }
        
        return displayItems;
    }
    
    /**
     * Get navigation placeholders.
     */
    private Map<String, String> getNavigationPlaceholders() {
        List<DisplayItem> displayItems = getAllDisplayItems();
        int totalPages = (int) Math.ceil((double) displayItems.size() / config.getItemsPerPage());
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("current_page", String.valueOf(currentPage + 1));
        placeholders.put("total_pages", String.valueOf(totalPages));
        placeholders.put("total_items", String.valueOf(displayItems.size()));
        placeholders.put("total_actions", String.valueOf(actionInfos.size()));
        placeholders.put("total_rewards", String.valueOf(jobRewards.size()));
        return placeholders;
    }
    
    /**
     * Get job-specific placeholders.
     */
    private Map<String, String> getJobPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("job_id", job.getId());
        placeholders.put("job_name", job.getName());
        placeholders.put("job_description", job.getDescription());
        return placeholders;
    }
    
    
    /**
     * Format action name for display.
     */
    private String formatActionName(ActionType actionType) {
        return actionType.name().toLowerCase().replace("_", " ");
    }
    
    /**
     * Get appropriate material for action type display.
     */
    private String getActionMaterial(ActionType actionType) {
        return switch (actionType) {
            case KILL -> "DIAMOND_SWORD";
            case PLACE -> "GRASS_BLOCK";
            case BREAK -> "IRON_PICKAXE";
            case HARVEST -> "WHEAT";
            case BLOCK_INTERACT -> "OAK_BUTTON";
            case ENTITY_INTERACT -> "LEAD";
            case BREED -> "WHEAT_SEEDS";
            case FISH -> "FISHING_ROD";
            case CRAFT -> "CRAFTING_TABLE";
            case SMELT -> "FURNACE";
            case ENCHANT -> "ENCHANTING_TABLE";
            case TRADE -> "EMERALD";
            case TAME -> "BONE";
            case SHEAR -> "SHEARS";
            case MILK -> "MILK_BUCKET";
            case EAT -> "COOKED_BEEF";
            case POTION -> "POTION";
            case CUSTOM -> "COMMAND_BLOCK";
            default -> "STONE";
        };
    }
    
    /**
     * Helper method to create item config map.
     */
    private Map<String, Object> createItemConfigMap(String material, String displayName, List<String> lore, boolean glow) {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("enabled", true);
        configMap.put("material", material);
        configMap.put("amount", 1);
        configMap.put("display-name", displayName);
        configMap.put("lore", lore);
        configMap.put("custom-model-data", 0);
        configMap.put("glow", glow);
        configMap.put("hide-attributes", false);
        configMap.put("hide-enchants", glow);
        configMap.put("slots", new ArrayList<Integer>());
        configMap.put("action", "none");
        configMap.put("action-value", "");
        return configMap;
    }
    
    /**
     * Container for action information.
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
     * Container for display items.
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