package fr.ax_dev.universejobs.menu.impl;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.config.LanguageManager;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.menu.BaseMenu;
import fr.ax_dev.universejobs.menu.config.MenuItemConfig;
import fr.ax_dev.universejobs.menu.config.SingleMenuConfig;
import fr.ax_dev.universejobs.menu.config.SimpleConfigurationSection;
import fr.ax_dev.universejobs.menu.utils.MenuItemUtils;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.UUID;

/**
 * Menu for an individual job showing job information and action buttons.
 * Implements InventoryHolder for better integration.
 */
public class SingleJobMenu extends BaseMenu {
    
    private static final int DEFAULT_PROGRESS_BARS = 20;
    private final Job job;
    private final PlayerJobData playerData;
    private final Map<String, String> cachedPlaceholders;
    private final LanguageManager languageManager;
    private boolean hasJob;
    
    public SingleJobMenu(UniverseJobs plugin, org.bukkit.entity.Player player, String jobId, SingleMenuConfig config) {
        super(plugin, player, config);

        // Initialize job and check if valid
        this.job = plugin.getJobManager().getJob(jobId);
        if (this.job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }
        
        this.playerData = plugin.getJobManager().getPlayerData(player.getUniqueId());
        this.languageManager = plugin.getLanguageManager();
        this.hasJob = playerData.hasJob(jobId);
        
        // Initialize cachedPlaceholders
        this.cachedPlaceholders = new HashMap<>();
        this.cachedPlaceholders.putAll(createJobPlaceholders());
        
        // Initialize menu after all fields are set
        initialize();
    }
    
    @Override
    protected void createInventory() {
        String title = config.getTitle();

        // Replace custom placeholders first
        for (Map.Entry<String, String> entry : cachedPlaceholders.entrySet()) {
            title = title.replace(entry.getKey(), entry.getValue());
        }

        // Then process PlaceholderAPI and other placeholders
        title = processPlaceholders(title);

        net.kyori.adventure.text.Component titleComponent = MessageUtils.parseMessage(title);

        // Use optimized menu holder for 2025 performance
        fr.ax_dev.universejobs.menu.OptimizedMenuHolder holder = new fr.ax_dev.universejobs.menu.OptimizedMenuHolder(
            player.getUniqueId(),
            getClass().getSimpleName(),
            getMenuId(),
            this
        );

        this.inventory = plugin.getAccessor().getMenuManager().getInventoryFromPool(
            config.getSize(),
            titleComponent,
            holder
        );

        holder.setInventory(this.inventory);
    }

    @Override
    protected String getMenuId() {
        return job != null ? job.getId() : "unknown";
    }
    
    @Override
    protected void populateInventory() {
        if (job == null) return;
        
        inventory.clear();
        
        // Use centralized approach for menu population
        populateJobInformation();
        populateActionButtons();
        populateNavigationItems();
        
        addStaticItems();
        addFillItems();
    }
    
    /**
     * Populate job information display items using configuration-driven approach.
     */
    private void populateJobInformation() {
        Map<String, MenuItemConfig> menuItems = loadMenuItemsFromConfig();
        
        // Job info item - centralized configuration handling
        MenuItemConfig jobInfoConfig = menuItems.get("job-info");
        if (jobInfoConfig != null && jobInfoConfig.isEnabled()) {
            ItemStack jobInfo = createMenuItem(jobInfoConfig, cachedPlaceholders);
            placeItemInSlots(jobInfo, jobInfoConfig.getSlots());
        }
        
        // Player stats item - centralized configuration handling
        MenuItemConfig playerStatsConfig = menuItems.get("player-stats");
        if (playerStatsConfig != null && playerStatsConfig.isEnabled()) {
            ItemStack playerStats = createMenuItem(playerStatsConfig, cachedPlaceholders);
            placeItemInSlots(playerStats, playerStatsConfig.getSlots());
        }
    }
    
    /**
     * Populate action buttons using configuration-driven approach.
     */
    private void populateActionButtons() {
        Map<String, MenuItemConfig> menuItems = loadMenuItemsFromConfig();
        
        // Join/Leave button with dynamic configuration based on job status
        MenuItemConfig joinLeaveConfig = menuItems.get("join-leave-button");
        if (joinLeaveConfig != null && joinLeaveConfig.isEnabled()) {
            ItemStack button = createJoinLeaveButton(joinLeaveConfig);
            placeItemInSlots(button, joinLeaveConfig.getSlots());
        }
        
        // Actions button
        MenuItemConfig actionsConfig = menuItems.get("actions-button");
        if (actionsConfig != null && actionsConfig.isEnabled()) {
            ItemStack button = createMenuItem(actionsConfig, cachedPlaceholders);
            placeItemInSlots(button, actionsConfig.getSlots());
        }
        
        // Rewards button
        MenuItemConfig rewardsConfig = menuItems.get("rewards-button");
        if (rewardsConfig != null && rewardsConfig.isEnabled()) {
            ItemStack button = createMenuItem(rewardsConfig, cachedPlaceholders);
            placeItemInSlots(button, rewardsConfig.getSlots());
        }
        
        // Rankings button
        MenuItemConfig rankingsConfig = menuItems.get("rankings-button");
        if (rankingsConfig != null && rankingsConfig.isEnabled()) {
            ItemStack button = createMenuItem(rankingsConfig, cachedPlaceholders);
            placeItemInSlots(button, rankingsConfig.getSlots());
        }
    }
    
    /**
     * Get the job this menu represents.
     * 
     * @return The job or null if not found
     */
    public Job getJob() {
        return job;
    }
    
    /**
     * Populate navigation items using centralized approach.
     */
    private void populateNavigationItems() {
        Map<String, MenuItemConfig> navItems = config.getNavigationItems();
        
        for (Map.Entry<String, MenuItemConfig> entry : navItems.entrySet()) {
            MenuItemConfig navConfig = entry.getValue();
            if (navConfig.isEnabled()) {
                ItemStack navItem = createMenuItem(navConfig, cachedPlaceholders);
                placeItemInSlots(navItem, navConfig.getSlots());
            }
        }
    }
    
    /**
     * Create join/leave button with dynamic configuration based on job status.
     * Uses proper API instead of manual configuration parsing.
     */
    private ItemStack createJoinLeaveButton(MenuItemConfig baseConfig) {
        if (!hasJob) {
            return createMenuItem(baseConfig, cachedPlaceholders);
        }
        
        // For "leave" state, use else configuration if available, otherwise fallback to has-job alternative
        if (baseConfig.hasElseConfiguration()) {
            Map<String, Object> leaveConfig = new HashMap<>();
            leaveConfig.put("enabled", true);
            leaveConfig.put("material", baseConfig.getElseMaterial());
            leaveConfig.put("display-name", baseConfig.getElseDisplayName());
            leaveConfig.put("lore", baseConfig.getElseLore());
            leaveConfig.put("amount", baseConfig.getAmount());
            leaveConfig.put("glow", baseConfig.isGlow());
            leaveConfig.put("hide-attributes", baseConfig.isHideAttributes());
            leaveConfig.put("hide-enchants", baseConfig.isHideEnchants());
            leaveConfig.put("slots", baseConfig.getSlots());
            leaveConfig.put("action", baseConfig.getAction());

            if (baseConfig.getElseModelData() != null) {
                Map<String, Object> modelDataValues = baseConfig.getElseModelData().toConfigurationValues();
                for (Map.Entry<String, Object> entry : modelDataValues.entrySet()) {
                    leaveConfig.put(entry.getKey(), entry.getValue());
                }
            }
            
            MenuItemConfig leaveItemConfig = new MenuItemConfig(new SimpleConfigurationSection(leaveConfig));
            return createMenuItem(leaveItemConfig, cachedPlaceholders);
        }
        
        return createMenuItem(baseConfig, cachedPlaceholders);
    }
    
    /**
     * Place item in multiple slots efficiently.
     */
    private void placeItemInSlots(ItemStack item, List<Integer> slots) {
        if (item == null || slots == null) return;
        
        for (int slot : slots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item);
            }
        }
    }
    
    /**
     * Add static items from configuration using centralized approach.
     */
    private void addStaticItems() {
        MenuItemUtils.addStaticItems(inventory, config.getStaticItems(), cachedPlaceholders,
            config -> createMenuItem(config, cachedPlaceholders));
    }
    
    @Override
    public void handleClick(int slot, InventoryClickEvent event) {
        event.setCancelled(true);
        
        // Handle navigation items first
        if (handleNavigationClickWithSound(slot)) {
            return;
        }
        
        // Handle menu items based on configured slots
        MenuItemConfig clickedItem = getMenuItemForSlot(slot);
        if (clickedItem != null) {
            // Execute commands if present
            if (clickedItem.getCommands() != null && !clickedItem.getCommands().isEmpty()) {
                executeCommands(clickedItem.getCommands());
            }
            
            // Handle action
            String action = clickedItem.getAction();
            switch (action) {
                case "toggle-job" -> handleJoinLeave();
                case "open-actions" -> plugin.getMenuManager().openJobActionsMenu(player, job.getId());
                case "open-rewards" -> plugin.getMenuManager().openRewardsMenu(player, job.getId());
                case "open-rankings" -> plugin.getMenuManager().openGlobalRankingsMenu(player, job.getId());
                case "back" -> plugin.getMenuManager().openJobsMainMenu(player);
                case "close" -> close();
            }
        }
    }
    
    /**
     * Get menu item config for a specific slot.
     */
    private MenuItemConfig getMenuItemForSlot(int slot) {
        // Check menu items configuration
        for (Map.Entry<String, MenuItemConfig> entry : config.getMenuItems().entrySet()) {
            MenuItemConfig itemConfig = entry.getValue();
            if (itemConfig.getSlots().contains(slot)) {
                return itemConfig;
            }
        }
        
        // Check navigation items
        for (Map.Entry<String, MenuItemConfig> entry : config.getNavigationItems().entrySet()) {
            MenuItemConfig itemConfig = entry.getValue();
            if (itemConfig.getSlots().contains(slot)) {
                return itemConfig;
            }
        }
        
        return null;
    }
    
    /**
     * Handle join/leave job action with proper error handling.
     */
    private void handleJoinLeave() {
        if (hasJob) {
            handleLeaveJob();
        } else {
            handleJoinJob();
        }
    }
    
    /**
     * Handle leaving a job with proper error messages.
     */
    private void handleLeaveJob() {
        if (plugin.getJobManager().leaveJob(player, job.getId())) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.leave.success", "job", job.getName()));
            hasJob = false;
            updatePlaceholdersAndRefresh();
        } else {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.leave.failed", "job", job.getName()));
        }
    }
    
    /**
     * Handle joining a job with proper validation and error messages.
     */
    private void handleJoinJob() {
        if (!validateJobJoinRequirements()) {
            return;
        }
        
        if (plugin.getJobManager().joinJob(player, job.getId())) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.join.success", "job", job.getName()));
            hasJob = true;
            updatePlaceholdersAndRefresh();
        } else {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.join.failed", "job", job.getName()));
        }
    }
    
    /**
     * Validate job join requirements with proper error messages.
     */
    private boolean validateJobJoinRequirements() {
        // Permission check
        if (job.getPermission() != null && !player.hasPermission(job.getPermission())) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.join.no-permission", "job", job.getName()));
            return false;
        }
        
        // Max jobs limit check
        int maxJobs = calculateMaxJobsForPlayer();
        int currentJobs = playerData.getJobs().size();
        if (currentJobs >= maxJobs) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.join.max-jobs-reached", "max", String.valueOf(maxJobs)));
            return false;
        }
        
        return true;
    }
    
    private int calculateMaxJobsForPlayer() {
        int maxJobs = plugin.getConfigManager().getMaxJobsPerPlayer();

        for (org.bukkit.permissions.PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
            String permission = permInfo.getPermission();

            if (permInfo.getValue() && (permission.startsWith("universejobs.max_join.") || permission.startsWith("universejobs.maxjobs."))) {
                try {
                    String numberPart;
                    if (permission.startsWith("universejobs.max_join.")) {
                        numberPart = permission.substring("universejobs.max_join.".length());
                    } else {
                        numberPart = permission.substring("universejobs.maxjobs.".length());
                    }

                    int permissionValue = Integer.parseInt(numberPart);

                    if (permissionValue > maxJobs) {
                        maxJobs = permissionValue;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        return maxJobs;
    }
    
    /**
     * Update cached placeholders and refresh menu efficiently.
     */
    private void updatePlaceholdersAndRefresh() {
        cachedPlaceholders.clear();
        cachedPlaceholders.putAll(createJobPlaceholders());
        refresh();
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
     * Create optimized progress bar with proper formatting.
     */
    private String createProgressBar(double progress) {
        var progressBarConfig = plugin.getAccessor().getConfigManager().getProgressBarConfig();
        if (progressBarConfig != null) {
            return progressBarConfig.generateProgressBar(progress * 100, 100, true);
        }
        
        // Fallback to legacy progress bar if config is not available
        int filled = (int) Math.round(progress * DEFAULT_PROGRESS_BARS);
        
        StringBuilder progressBar = new StringBuilder("&a");
        for (int i = 0; i < DEFAULT_PROGRESS_BARS; i++) {
            progressBar.append(i < filled ? "█" : "&7█");
        }
        progressBar.append(" &f").append(String.format("%.1f", progress * 100)).append("%");
        
        return progressBar.toString();
    }
    
    /**
     * Load menu items from configuration using proper API access.
     */
    private Map<String, MenuItemConfig> loadMenuItemsFromConfig() {
        // Use configuration if available
        Map<String, MenuItemConfig> configItems = config.getMenuItems();
        if (!configItems.isEmpty()) {
            return configItems;
        }
        
        // Fallback to default items
        Map<String, MenuItemConfig> items = new HashMap<>();
        try {
            // Create job-info item
            Map<String, Object> jobInfoData = new HashMap<>();
            jobInfoData.put("enabled", true);
            jobInfoData.put("material", "PAPER");
            jobInfoData.put("slots", Arrays.asList(10));
            items.put("job-info", new MenuItemConfig(new SimpleConfigurationSection(jobInfoData)));
            
            // Create player-stats item
            Map<String, Object> playerStatsData = new HashMap<>();
            playerStatsData.put("enabled", true);
            playerStatsData.put("material", "PLAYER_HEAD");
            playerStatsData.put("slots", Arrays.asList(12));
            items.put("player-stats", new MenuItemConfig(new SimpleConfigurationSection(playerStatsData)));
            
            // Create join-leave-button
            Map<String, Object> joinLeaveData = new HashMap<>();
            joinLeaveData.put("enabled", true);
            joinLeaveData.put("material", "EMERALD_BLOCK");
            joinLeaveData.put("slots", Arrays.asList(14));
            items.put("join-leave-button", new MenuItemConfig(new SimpleConfigurationSection(joinLeaveData)));
            
            // Create actions-button
            Map<String, Object> actionsData = new HashMap<>();
            actionsData.put("enabled", true);
            actionsData.put("material", "DIAMOND_SWORD");
            actionsData.put("slots", Arrays.asList(15));
            items.put("actions-button", new MenuItemConfig(new SimpleConfigurationSection(actionsData)));
            
            // Create rewards-button
            Map<String, Object> rewardsData = new HashMap<>();
            rewardsData.put("enabled", true);
            rewardsData.put("material", "EMERALD");
            rewardsData.put("slots", Arrays.asList(17));
            items.put("rewards-button", new MenuItemConfig(new SimpleConfigurationSection(rewardsData)));
            
            // Create rankings-button
            Map<String, Object> rankingsData = new HashMap<>();
            rankingsData.put("enabled", true);
            rankingsData.put("material", "GOLD_INGOT");
            rankingsData.put("slots", Arrays.asList(18));
            items.put("rankings-button", new MenuItemConfig(new SimpleConfigurationSection(rankingsData)));
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load menu items configuration: " + e.getMessage());
        }
        
        return items;
    }
    
    /**
     * Create optimized placeholders for this job and player.
     * Cached for performance improvement.
     */
    private Map<String, String> createJobPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        
        // Job placeholders - centralized approach
        addJobPlaceholders(placeholders);
        
        // Player placeholders - centralized approach
        addPlayerPlaceholders(placeholders);
        
        // Statistics placeholders - centralized approach
        addStatisticsPlaceholders(placeholders);
        
        return placeholders;
    }
    
    /**
     * Add job-related placeholders efficiently.
     */
    private void addJobPlaceholders(Map<String, String> placeholders) {
        placeholders.put("{job_id}", job.getId());
        placeholders.put("{job_name}", job.getDisplayName());
        placeholders.put("{job_description}", job.getDescription());
        placeholders.put("{job_description_lines}", String.join("\n", job.getDescriptionLines()));
        placeholders.put("{job_max_level}", String.valueOf(playerData.getMaxLevel(job.getId())));
        placeholders.put("{job_permission}", job.getPermission() != null ? job.getPermission() : "none");
    }
    
    /**
     * Add player-related placeholders efficiently.
     */
    private void addPlayerPlaceholders(Map<String, String> placeholders) {
        placeholders.put("{player_name}", player.getName());
        placeholders.put("{job_status}", plugin.getConfigManager().getJobStatus(hasJob));
        placeholders.put("{max_jobs}", String.valueOf(calculateMaxJobsForPlayer()));

        if (hasJob) {
            int playerLevel = playerData.getLevel(job.getId());
            long playerXp = (long) playerData.getXp(job.getId());

            placeholders.put("{player_level}", String.valueOf(playerLevel));
            placeholders.put("{player_xp}", String.valueOf(playerXp));

            calculateAndAddProgressPlaceholders(placeholders, playerLevel, playerXp);
        } else {
            addDefaultProgressPlaceholders(placeholders);
        }
    }
    
    /**
     * Calculate and add progress-related placeholders efficiently.
     */
    private void calculateAndAddProgressPlaceholders(Map<String, String> placeholders, int playerLevel, long playerXp) {
        int effectiveMaxLevel = playerData.getMaxLevel(job.getId());
        if (playerLevel < effectiveMaxLevel && job.getXpCurve() != null) {
            long currentLevelXp = (long) job.getXpCurve().getXpForLevel(playerLevel);
            long nextLevelXp = (long) job.getXpCurve().getXpForLevel(playerLevel + 1);
            long currentXpInLevel = Math.max(0, playerXp - currentLevelXp);
            long xpNeededForNext = nextLevelXp - currentLevelXp;
            long xpToNext = Math.max(0, nextLevelXp - playerXp);
            double progress = xpNeededForNext > 0 ? Math.min(1.0, (double) currentXpInLevel / xpNeededForNext) : 1.0;

            placeholders.put("{xp_to_next}", String.valueOf(xpToNext));
            placeholders.put("{next_level_xp}", String.valueOf(xpNeededForNext));
            placeholders.put("{current_xp}", String.valueOf(currentXpInLevel));
            placeholders.put("{progress_percent}", String.format("%.1f", progress * 100));
            placeholders.put("{progress_bar}", createProgressBar(progress));
        } else {
            addDefaultProgressPlaceholders(placeholders);
            placeholders.put("{progress_percent}", "100.0");
            placeholders.put("{progress_bar}", createProgressBar(1.0));
        }
    }
    
    /**
     * Add default progress placeholders for players without the job.
     */
    private void addDefaultProgressPlaceholders(Map<String, String> placeholders) {
        placeholders.put("{player_level}", "0");
        placeholders.put("{player_xp}", "0");
        placeholders.put("{xp_to_next}", "0");
        placeholders.put("{next_level_xp}", "0");
        placeholders.put("{progress_percent}", "0.0");
        placeholders.put("{progress_bar}", createProgressBar(0));
    }
    
    /**
     * Calculate player rank for the current job.
     */
    private String calculatePlayerRank() {
        List<RankingEntry> rankings = getJobRankings();
        for (int i = 0; i < rankings.size(); i++) {
            if (rankings.get(i).playerUuid.equals(player.getUniqueId())) {
                return "" + (i + 1);
            }
        }
        return "N/A";
    }
    
    /**
     * Get top player for the current job.
     */
    private String getTopPlayerForJob() {
        List<RankingEntry> rankings = getJobRankings();
        if (!rankings.isEmpty()) {
            return rankings.get(0).playerName;
        }
        return "N/A";
    }
    
    /**
     * Get job rankings (cached for performance).
     */
    private List<RankingEntry> getJobRankings() {
        List<RankingEntry> entries = new ArrayList<>();

        // Get all player data using centralized JobManager approach
        Map<UUID, PlayerJobData> allPlayerData = plugin.getJobManager().getAllPlayerData();

        for (Map.Entry<UUID, PlayerJobData> entry : allPlayerData.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerJobData playerData = entry.getValue();

            int level = playerData.getLevel(job.getId());
            double xp = playerData.getXp(job.getId());

            // Only include players with stats for this job
            if (level > 0 || xp > 0) {
                org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerId);
                String playerName = offlinePlayer.getName();

                if (playerName != null) {
                    entries.add(new RankingEntry(playerId, playerName, level, xp));
                }
            }
        }

        // Sort by level (descending), then by XP (descending)
        entries.sort((a, b) -> {
            int levelCompare = Integer.compare(b.level, a.level);
            if (levelCompare != 0) return levelCompare;
            return Double.compare(b.xp, a.xp);
        });

        return entries;
    }
    
    /**
     * Simple ranking entry class.
     */
    private static class RankingEntry {
        final UUID playerUuid;
        final String playerName;
        final int level;
        final double xp;
        
        RankingEntry(UUID playerUuid, String playerName, int level, double xp) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.level = level;
            this.xp = xp;
        }
    }
    
    /**
     * Add statistics-related placeholders efficiently.
     */
    private void addStatisticsPlaceholders(Map<String, String> placeholders) {
        // Calculate total actions efficiently
        int totalActions = job.getActionTypes().stream()
            .mapToInt(type -> job.getActions(type).size())
            .sum();
        placeholders.put("{total_actions}", String.valueOf(totalActions));
        
        // Get total rewards efficiently
        int totalRewards = plugin.getRewardManager() != null ? 
            plugin.getRewardManager().getJobRewards(job.getId()).size() : 0;
        placeholders.put("{total_rewards}", String.valueOf(totalRewards));
        
        // Get player rank for this specific job
        String playerRank = calculatePlayerRank();
        placeholders.put("{player_rank}", playerRank);
        
        // Get top player for this specific job
        String topPlayer = getTopPlayerForJob();
        placeholders.put("{top_player}", topPlayer);
    }
}