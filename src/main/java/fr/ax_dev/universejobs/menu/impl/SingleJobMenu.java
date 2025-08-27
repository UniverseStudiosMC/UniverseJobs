package fr.ax_dev.universejobs.menu.impl;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.menu.BaseMenu;
import fr.ax_dev.universejobs.menu.config.MenuItemConfig;
import fr.ax_dev.universejobs.menu.config.SingleMenuConfig;
import fr.ax_dev.universejobs.menu.config.SimpleConfigurationSection;
import fr.ax_dev.universejobs.menu.utils.MenuItemUtils;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Menu for an individual job showing job information and action buttons.
 */
public class SingleJobMenu extends BaseMenu {
    
    private final Job job;
    private final PlayerJobData playerData;
    private final boolean hasJob;
    
    public SingleJobMenu(UniverseJobs plugin, org.bukkit.entity.Player player, String jobId, SingleMenuConfig config) {
        super(plugin, player, config);
        
        this.job = plugin.getJobManager().getJob(jobId);
        this.playerData = plugin.getJobManager().getPlayerData(player.getUniqueId());
        this.hasJob = playerData.hasJob(jobId);
        
        if (this.job == null) {
            MessageUtils.sendMessage(player, "&cJob not found: " + jobId);
            close();
        }
    }
    
    @Override
    protected void populateInventory() {
        if (job == null) return;
        
        // Clear inventory first
        inventory.clear();
        
        // Add job information display
        addJobInformation();
        
        // Add action buttons
        addActionButtons();
        
        // Add navigation items
        addNavigationItems();
        
        // Add static items
        addStaticItems();
        
        // Fill empty slots
        addFillItems();
    }
    
    /**
     * Add job information display item.
     */
    private void addJobInformation() {
        // Job info item (center top)
        ItemStack jobInfoItem = createJobInfoItem();
        inventory.setItem(13, jobInfoItem); // Center of top row
        
        // Player stats item
        ItemStack playerStatsItem = createPlayerStatsItem();
        inventory.setItem(22, playerStatsItem); // Center of middle row
    }
    
    /**
     * Create the main job information item.
     */
    private ItemStack createJobInfoItem() {
        Material iconMaterial;
        try {
            iconMaterial = Material.valueOf(job.getIcon().toUpperCase());
        } catch (IllegalArgumentException e) {
            iconMaterial = Material.STONE;
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("&7" + job.getDescription());
        lore.add("");
        lore.add("&7Max Level: &e" + job.getMaxLevel());
        lore.add("&7XP Type: &e" + job.getXpType());
        
        // Add job lore from configuration
        if (!job.getLore().isEmpty()) {
            lore.add("");
            lore.addAll(job.getLore());
        }
        
        // Add action types info
        if (!job.getActionTypes().isEmpty()) {
            lore.add("");
            lore.add("&6Available Actions:");
            job.getActionTypes().forEach(actionType -> 
                lore.add("&8- &e" + actionType.name().toLowerCase().replace("_", " "))
            );
        }
        
        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
            job.getIcon(), "&e&l" + job.getName(), lore, true
        );
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, MenuItemUtils.createJobPlaceholders(job.getId(), job.getName(), job.getDescription()));
    }
    
    /**
     * Create the player stats item.
     */
    private ItemStack createPlayerStatsItem() {
        List<String> lore = new ArrayList<>();
        
        if (hasJob) {
            int playerLevel = playerData.getLevel(job.getId());
            long playerXp = (long) playerData.getXp(job.getId());
            
            lore.add("&7Your Level: &a" + playerLevel);
            lore.add("&7Your XP: &b" + playerXp);
            
            if (playerLevel < job.getMaxLevel() && job.getXpCurve() != null) {
                long nextLevelXp = (long) job.getXpCurve().getXpForLevel(playerLevel + 1);
                long xpToNext = nextLevelXp - playerXp;
                lore.add("&7XP to Next Level: &e" + Math.max(0, xpToNext));
                
                // Progress bar
                double progress = (double) playerXp / nextLevelXp;
                String progressBar = createProgressBar(progress);
                lore.add("&7Progress: " + progressBar);
            }
            
            lore.add("");
            lore.add("&7Status: &aJoined");
        } else {
            lore.add("&7Your Level: &c0");
            lore.add("&7Your XP: &c0");
            lore.add("");
            lore.add("&7Status: &cNot Joined");
        }
        
        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
            "PLAYER_HEAD", "&6Your Progress", lore, hasJob
        );
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, MenuItemUtils.createJobPlaceholders(job.getId(), job.getName(), job.getDescription()));
    }
    
    /**
     * Add action buttons to the menu.
     */
    private void addActionButtons() {
        // Join/Leave button
        ItemStack joinLeaveButton = createJoinLeaveButton();
        inventory.setItem(29, joinLeaveButton);
        
        // Actions & Rewards button
        ItemStack actionsButton = createActionsButton();
        inventory.setItem(31, actionsButton);
        
        // Rankings button
        ItemStack rankingsButton = createRankingsButton();
        inventory.setItem(33, rankingsButton);
    }
    
    /**
     * Create join/leave button.
     */
    private ItemStack createJoinLeaveButton() {
        String material = hasJob ? "RED_CONCRETE" : "GREEN_CONCRETE";
        String name = hasJob ? "&c&lLeave Job" : "&a&lJoin Job";
        List<String> lore = new ArrayList<>();
        
        if (hasJob) {
            lore.add("&7Click to leave this job");
            lore.add("");
            lore.add("&c&lWARNING:");
            lore.add("&cYou will lose all progress!");
        } else {
            lore.add("&7Click to join this job");
            
            if (job.getPermission() != null && !player.hasPermission(job.getPermission())) {
                lore.add("");
                lore.add("&c&lRequired Permission:");
                lore.add("&c" + job.getPermission());
            }
        }
        
        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(material, name, lore, false);
        configMap.put("action", hasJob ? "leave_job" : "join_job");
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, MenuItemUtils.createJobPlaceholders(job.getId(), job.getName(), job.getDescription()));
    }
    
    /**
     * Create actions & rewards button.
     */
    private ItemStack createActionsButton() {
        List<String> lore = Arrays.asList(
            "&7View all available actions",
            "&7and their rewards for this job",
            "",
            "&e▶ Click to open actions menu"
        );
        
        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
            "DIAMOND_PICKAXE", "&6&lActions & Rewards", lore, false
        );
        configMap.put("action", "open_actions");
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, MenuItemUtils.createJobPlaceholders(job.getId(), job.getName(), job.getDescription()));
    }
    
    /**
     * Create rankings button.
     */
    private ItemStack createRankingsButton() {
        List<String> lore = Arrays.asList(
            "&7View global job rankings",
            "&7and see top players",
            "",
            "&e▶ Click to open rankings"
        );
        
        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
            "GOLD_INGOT", "&6&lGlobal Rankings", lore, false
        );
        configMap.put("action", "open_rankings");
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, MenuItemUtils.createJobPlaceholders(job.getId(), job.getName(), job.getDescription()));
    }
    
    /**
     * Add navigation items.
     */
    private void addNavigationItems() {
        // Back to main menu button
        ItemStack backButton = createBackButton();
        inventory.setItem(39, backButton);
        
        // Close button
        ItemStack closeButton = createCloseButton();
        inventory.setItem(41, closeButton);
    }
    
    /**
     * Create back button.
     */
    private ItemStack createBackButton() {
        List<String> lore = Arrays.asList(
            "&7Go back to the main jobs menu"
        );
        
        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
            "ARROW", "&7&l← Back to Jobs Menu", lore, false
        );
        configMap.put("action", "back_to_main");
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig);
    }
    
    /**
     * Create close button.
     */
    private ItemStack createCloseButton() {
        List<String> lore = Arrays.asList(
            "&7Close this menu"
        );
        
        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
            "BARRIER", "&c&lClose", lore, false
        );
        configMap.put("action", "close");
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig);
    }
    
    /**
     * Add static items from configuration.
     */
    private void addStaticItems() {
        Map<String, String> placeholders = MenuItemUtils.createJobPlaceholders(job.getId(), job.getName(), job.getDescription());
        MenuItemUtils.addStaticItems(inventory, config.getStaticItems(), placeholders,
            config -> createMenuItem(config, placeholders));
    }
    
    @Override
    public void handleClick(int slot, InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        // Handle specific button clicks
        switch (slot) {
            case 29: // Join/Leave button
                handleJoinLeave();
                break;
                
            case 31: // Actions & Rewards button
                plugin.getMenuManager().openJobActionsMenu(player, job.getId());
                break;
                
            case 33: // Rankings button
                plugin.getMenuManager().openGlobalRankingsMenu(player);
                break;
                
            case 39: // Back button
                plugin.getMenuManager().openJobsMainMenu(player);
                break;
                
            case 41: // Close button
                close();
                break;
                
            default:
                // Handle navigation clicks
                handleNavigationClick(slot);
                break;
        }
    }
    
    /**
     * Handle join/leave job action.
     */
    private void handleJoinLeave() {
        if (hasJob) {
            // Leave job
            if (plugin.getJobManager().leaveJob(player.getUniqueId(), job.getId())) {
                MessageUtils.sendMessage(player, "&cYou have left the job: &e" + job.getName());
                refresh(); // Refresh menu to update status
            } else {
                MessageUtils.sendMessage(player, "&cFailed to leave the job.");
            }
        } else {
            // Join job
            if (plugin.getJobManager().joinJob(player.getUniqueId(), job.getId())) {
                MessageUtils.sendMessage(player, "&aYou have joined the job: &e" + job.getName());
                refresh(); // Refresh menu to update status
            } else {
                MessageUtils.sendMessage(player, "&cFailed to join the job.");
            }
        }
    }
    
    @Override
    protected boolean hasNextPage() {
        return false; // Single job menu doesn't have pagination
    }
    
    @Override
    protected void handleBackButton() {
        plugin.getMenuManager().openJobsMainMenu(player);
    }
    
    
    /**
     * Create a progress bar string.
     */
    private String createProgressBar(double progress) {
        int bars = 20;
        int filled = (int) Math.round(progress * bars);
        
        StringBuilder progressBar = new StringBuilder("&a");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                progressBar.append("█");
            } else {
                progressBar.append("&7█");
            }
        }
        progressBar.append(" &f").append(String.format("%.1f", progress * 100)).append("%");
        
        return progressBar.toString();
    }
    
    
}