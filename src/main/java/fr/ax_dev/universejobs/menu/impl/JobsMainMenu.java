package fr.ax_dev.universejobs.menu.impl;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.menu.BaseMenu;
import fr.ax_dev.universejobs.menu.config.JobItemFormat;
import fr.ax_dev.universejobs.menu.config.MenuItemConfig;
import fr.ax_dev.universejobs.menu.config.SingleMenuConfig;
import fr.ax_dev.universejobs.menu.config.SimpleConfigurationSection;
import fr.ax_dev.universejobs.menu.utils.MenuItemUtils;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main jobs menu showing all available jobs.
 * Players can click on jobs to open individual job menus.
 */
public class JobsMainMenu extends BaseMenu {
    
    private final List<Job> availableJobs;
    private final PlayerJobData playerData;
    
    public JobsMainMenu(UniverseJobs plugin, org.bukkit.entity.Player player, SingleMenuConfig config) {
        super(plugin, player, config);
        
        this.playerData = plugin.getJobManager().getPlayerData(player.getUniqueId());
        this.availableJobs = plugin.getJobManager().getJobs().values().stream()
            .filter(job -> job.isEnabled())
            .filter(job -> job.getPermission() == null || player.hasPermission(job.getPermission()))
            .sorted(Comparator.comparing(Job::getName))
            .collect(Collectors.toList());
        
        // Now populate the inventory after all fields are initialized
        populateInventory();
    }
    
    @Override
    protected void populateInventory() {
        // Clear inventory first
        inventory.clear();
        
        // Add static items first
        addStaticItems();
        
        // Add job items
        addJobItems();
        
        // Add navigation items
        addNavigationItems();
        
        // Fill empty slots
        addFillItems();
    }
    
    /**
     * Add static items defined in configuration.
     */
    private void addStaticItems() {
        Map<String, String> placeholders = getGeneralPlaceholders();
        MenuItemUtils.addStaticItems(inventory, config.getStaticItems(), placeholders,
            config -> createMenuItem(config, placeholders));
    }
    
    /**
     * Add job items to the menu.
     */
    private void addJobItems() {
        List<Integer> contentSlots = config.getContentSlots();
        int startIndex = currentPage * config.getItemsPerPage();
        int endIndex = Math.min(startIndex + config.getItemsPerPage(), availableJobs.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Job job = availableJobs.get(i);
            int slotIndex = i - startIndex;
            
            if (slotIndex >= contentSlots.size()) break;
            
            int slot = contentSlots.get(slotIndex);
            ItemStack jobItem = createJobItem(job);
            inventory.setItem(slot, jobItem);
        }
    }
    
    /**
     * Create an ItemStack representing a job using the configured format.
     */
    private ItemStack createJobItem(Job job) {
        JobItemFormat format = config.getJobItemFormat();
        // Get job icon material if configured to use job icon
        Material iconMaterial = Material.STONE;
        int customModelData = 0;
        
        if (format.isUseJobIcon()) {
            try {
                iconMaterial = Material.valueOf(job.getIconMaterial().toUpperCase());
            } catch (IllegalArgumentException e) {
                iconMaterial = Material.STONE;
            }
            customModelData = job.getCustomModelData();
        }
        
        ItemStack item = new ItemStack(iconMaterial, format.getAmount());
        
        // Apply custom model data if set
        if (customModelData > 0) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                item.setItemMeta(meta);
            }
        }
        
        // Get player's job data for this job
        boolean hasJob = playerData.hasJob(job.getId());
        int playerLevel = hasJob ? playerData.getLevel(job.getId()) : 0;
        long playerXp = hasJob ? (long) playerData.getXp(job.getId()) : 0;
        
        // Create comprehensive placeholders for this job
        Map<String, String> placeholders = createJobPlaceholders(job, hasJob, playerLevel, playerXp);
        
        // Use formatted display name and lore from config
        String displayName = format.getDisplayName();
        List<String> lore = hasJob ? format.getLore() : format.getLoreWithoutJob();
        boolean shouldGlow = hasJob ? format.isGlowWhenJoined() : format.isGlowWhenNotJoined();
        
        // Create menu item config with format settings
        Map<String, Object> jobConfigMap = new HashMap<>();
        jobConfigMap.put("material", iconMaterial.name());
        jobConfigMap.put("display-name", displayName);
        jobConfigMap.put("lore", lore);
        jobConfigMap.put("amount", format.getAmount());
        jobConfigMap.put("glow", shouldGlow);
        jobConfigMap.put("hide-attributes", format.isHideAttributes());
        jobConfigMap.put("hide-enchants", format.isHideEnchants());
        
        // Apply custom model data if set
        if (customModelData > 0) {
            jobConfigMap.put("custom-model-data", customModelData);
        }
        
        jobConfigMap.put("action", "open_job");
        jobConfigMap.put("action-value", job.getId());
        
        MenuItemConfig jobItemConfig = new MenuItemConfig(new SimpleConfigurationSection(jobConfigMap));
        return createMenuItem(jobItemConfig, placeholders);
    }
    
    /**
     * Add navigation items to the menu.
     */
    private void addNavigationItems() {
        Map<String, MenuItemConfig> navItems = config.getNavigationItems();
        MenuItemUtils.addNavigationItems(inventory, navItems, currentPage, hasNextPage(),
            getNavigationPlaceholders(), config -> createMenuItem(config, getNavigationPlaceholders()));
    }
    
    /**
     * Get placeholders for navigation items.
     */
    private Map<String, String> getNavigationPlaceholders() {
        Map<String, String> placeholders = MenuItemUtils.createNavigationPlaceholders(
            currentPage, availableJobs.size(), config.getItemsPerPage());
        placeholders.put("total_jobs", String.valueOf(availableJobs.size()));
        placeholders.put("player_jobs", String.valueOf(playerData.getJobs().size()));
        return placeholders;
    }
    
    /**
     * Get general placeholders for static items.
     */
    private Map<String, String> getGeneralPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("total_jobs", String.valueOf(availableJobs.size()));
        placeholders.put("player_jobs", String.valueOf(playerData.getJobs().size()));
        return placeholders;
    }
    
    @Override
    public void handleClick(int slot, InventoryClickEvent event) {
        // Handle navigation clicks first
        if (handleNavigationClick(slot)) {
            return;
        }
        
        // Handle job clicks
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        // Find which job was clicked
        List<Integer> contentSlots = config.getContentSlots();
        int slotIndex = contentSlots.indexOf(slot);
        
        if (slotIndex >= 0) {
            int jobIndex = currentPage * config.getItemsPerPage() + slotIndex;
            if (jobIndex < availableJobs.size()) {
                Job clickedJob = availableJobs.get(jobIndex);
                
                // Check click type for different actions
                if (event.getClick().isShiftClick()) {
                    // Shift+Click for quick join/leave
                    handleQuickJoinLeave(clickedJob);
                } else {
                    // Regular click opens the job menu
                    plugin.getMenuManager().openJobMenu(player, clickedJob.getId());
                }
            }
        }
    }
    
    /**
     * Handle quick join/leave action from main menu.
     */
    private void handleQuickJoinLeave(Job job) {
        boolean currentlyHasJob = playerData.hasJob(job.getId());
        
        if (currentlyHasJob) {
            // Quick leave
            if (plugin.getJobManager().leaveJob(player, job.getId())) {
                MessageUtils.sendMessage(player, "&cYou have left the job: &e" + job.getName());
                refresh(); // Refresh to update status
            } else {
                MessageUtils.sendMessage(player, "&cFailed to leave the job.");
            }
        } else {
            // Quick join - check limitations first
            if (!canJoinJob(job)) {
                return; // Error message already sent
            }
            
            if (plugin.getJobManager().joinJob(player, job.getId())) {
                MessageUtils.sendMessage(player, "&aYou have joined the job: &e" + job.getName());
                refresh(); // Refresh to update status
            } else {
                MessageUtils.sendMessage(player, "&cFailed to join the job.");
            }
        }
    }
    
    /**
     * Check if player can join a job.
     */
    private boolean canJoinJob(Job job) {
        // Check permission
        if (job.getPermission() != null && !player.hasPermission(job.getPermission())) {
            MessageUtils.sendMessage(player, "&cYou don't have permission to join this job.");
            return false;
        }
        
        // Check max jobs limit
        int maxJobs = getMaxJobsForPlayer(player);
        int currentJobs = playerData.getJobs().size();
        if (currentJobs >= maxJobs) {
            MessageUtils.sendMessage(player, "&cYou have reached the maximum number of jobs (" + maxJobs + ").");
            return false;
        }
        
        return true;
    }
    
    /**
     * Create comprehensive placeholders for a job.
     */
    private Map<String, String> createJobPlaceholders(Job job, boolean hasJob, int playerLevel, long playerXp) {
        Map<String, String> placeholders = new HashMap<>();
        
        // Basic job information
        placeholders.put("job_id", job.getId());
        placeholders.put("job_name", job.getName());
        placeholders.put("job_description", job.getDescription());
        placeholders.put("job_max_level", String.valueOf(job.getMaxLevel()));
        placeholders.put("job_permission", job.getPermission() != null ? job.getPermission() : "none");
        
        // Player status
        placeholders.put("player_level", String.valueOf(playerLevel));
        placeholders.put("player_xp", String.valueOf(playerXp));
        placeholders.put("has_job", hasJob ? "Yes" : "No");
        placeholders.put("job_status", hasJob ? "&aJoined" : "&7Not Joined");
        
        // XP and progress calculations
        if (hasJob && job.getXpCurve() != null && playerLevel < job.getMaxLevel()) {
            long currentLevelXp = (long) job.getXpCurve().getXpForLevel(playerLevel);
            long nextLevelXp = (long) job.getXpCurve().getXpForLevel(playerLevel + 1);
            long xpToNext = nextLevelXp - playerXp;
            long xpProgress = playerXp - currentLevelXp;
            long xpRequired = nextLevelXp - currentLevelXp;
            
            placeholders.put("xp_to_next", String.valueOf(Math.max(0, xpToNext)));
            placeholders.put("next_level_xp", String.valueOf(nextLevelXp));
            placeholders.put("current_level_xp", String.valueOf(currentLevelXp));
            
            // Calculate progress percentage
            double progressPercent = xpRequired > 0 ? (double) xpProgress / xpRequired * 100 : 0;
            placeholders.put("progress_percent", String.format("%.1f", progressPercent));
            
            // Create progress bar
            placeholders.put("progress_bar", createProgressBar(progressPercent));
        } else {
            placeholders.put("xp_to_next", "0");
            placeholders.put("next_level_xp", "0");
            placeholders.put("current_level_xp", "0");
            placeholders.put("progress_percent", playerLevel >= job.getMaxLevel() ? "100.0" : "0.0");
            placeholders.put("progress_bar", playerLevel >= job.getMaxLevel() ? 
                createProgressBar(100) : createProgressBar(0));
        }
        
        return placeholders;
    }
    
    /**
     * Create a visual progress bar.
     */
    private String createProgressBar(double percent) {
        int totalBars = 20;
        int filledBars = (int) (percent / 100.0 * totalBars);
        StringBuilder bar = new StringBuilder();
        
        bar.append("&a");
        for (int i = 0; i < filledBars; i++) {
            bar.append("▰");
        }
        
        bar.append("&7");
        for (int i = filledBars; i < totalBars; i++) {
            bar.append("▱");
        }
        
        return bar.toString();
    }
    
    /**
     * Get the maximum number of jobs a player can have.
     */
    private int getMaxJobsForPlayer(org.bukkit.entity.Player player) {
        int maxJobs = 1;
        
        for (org.bukkit.permissions.PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
            String permission = permInfo.getPermission();
            
            if (permission.startsWith("universejobs.max_join.") && permInfo.getValue()) {
                try {
                    String numberPart = permission.substring("universejobs.max_join.".length());
                    int permissionValue = Integer.parseInt(numberPart);
                    
                    if (permissionValue > maxJobs) {
                        maxJobs = permissionValue;
                    }
                } catch (NumberFormatException e) {
                    // Invalid number in permission, ignore it
                }
            }
        }
        
        return maxJobs;
    }
    
    @Override
    protected boolean hasNextPage() {
        return (currentPage + 1) * config.getItemsPerPage() < availableJobs.size();
    }
    
    /**
     * Get total number of pages.
     */
    private int getTotalPages() {
        return (int) Math.ceil((double) availableJobs.size() / config.getItemsPerPage());
    }
    
}