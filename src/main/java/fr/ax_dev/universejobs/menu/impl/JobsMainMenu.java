package fr.ax_dev.universejobs.menu.impl;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.config.LanguageManager;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.menu.BaseMenu;
import fr.ax_dev.universejobs.menu.config.JobItemFormat;
import fr.ax_dev.universejobs.menu.config.MenuItemConfig;
import fr.ax_dev.universejobs.menu.config.SingleMenuConfig;
import fr.ax_dev.universejobs.menu.config.SimpleConfigurationSection;
import fr.ax_dev.universejobs.menu.config.JobSlotManager;
import fr.ax_dev.universejobs.menu.utils.MenuItemUtils;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main jobs menu showing all available jobs.
 * Players can click on jobs to open individual job menus or quick join/leave.
 * Implements InventoryHolder for better integration and uses centralized approach.
 */
public class JobsMainMenu extends BaseMenu {
    
    private static final int DEFAULT_PROGRESS_BARS = 20;
    private static final char PROGRESS_FILLED = '▰';
    private static final char PROGRESS_EMPTY = '▱';
    private static final String PROGRESS_FILLED_COLOR = "&a";
    private static final String PROGRESS_EMPTY_COLOR = "&7";
    
    private final List<Job> availableJobs;
    private final PlayerJobData playerData;
    private final Map<String, String> cachedPlaceholders;
    private final LanguageManager languageManager;

    // Performance optimization: precomputed items
    private Map<String, ItemStack> precomputedItems;
    
    public JobsMainMenu(UniverseJobs plugin, org.bukkit.entity.Player player, SingleMenuConfig config, JobSlotManager jobSlotManager) {
        super(plugin, player, config);
        
        this.playerData = plugin.getJobManager().getPlayerData(player.getUniqueId());
        this.languageManager = plugin.getLanguageManager();
        
        // Load available jobs efficiently using streams with proper filtering
        this.availableJobs = loadAvailableJobsForPlayer();
        
        // Pre-calculate and cache common placeholders for performance
        this.cachedPlaceholders = createGeneralPlaceholders();
        
        // Initialize menu after all fields are set
        initialize();
    }
    
    /**
     * Load available jobs for player using efficient filtering and sorting.
     */
    private List<Job> loadAvailableJobsForPlayer() {
        return plugin.getJobManager().getJobs().values().stream()
            .filter(Job::isEnabled)
            .filter(job -> job.getPermission() == null || player.hasPermission(job.getPermission()))
            .sorted(Comparator.comparing(Job::getName))
            .collect(Collectors.toList());
    }
    
    @Override
    protected void populateInventory() {
        inventory.clear();
        
        // Use centralized approach for menu population
        populateStaticItems();
        populateJobItems();
        populateNavigationItems();
        
        addFillItems();
    }
    
    /**
     * Populate static items using centralized approach.
     */
    private void populateStaticItems() {
        MenuItemUtils.addStaticItems(inventory, config.getStaticItems(), cachedPlaceholders,
            config -> createMenuItem(config, cachedPlaceholders));
    }
    
    /**
     * Set precomputed items for ultra-fast menu loading.
     */
    public void setPrecomputedItems(Map<String, ItemStack> items) {
        this.precomputedItems = items;
    }

    /**
     * Populate job items using centralized slot management.
     */
    private void populateJobItems() {

        List<Integer> contentSlots = config.getContentSlots();
        Map<String, Integer> configuredJobSlots = config.getJobSlots();
        Set<Integer> usedSlots = new HashSet<>();


        // First, place jobs with specific slot configurations
        for (Job job : availableJobs) {

            if (configuredJobSlots.containsKey(job.getId())) {
                int slot = configuredJobSlots.get(job.getId());

                if (contentSlots.contains(slot) && !usedSlots.contains(slot)) {
                    try {
                        ItemStack jobItem = getJobItemOptimized(job);
                        if (jobItem != null) {
                            inventory.setItem(slot, jobItem);
                            usedSlots.add(slot);
                            logDebugPlacement(job, slot);
                        } else {
                            plugin.getLogger().warning("[DEBUG] Failed to create item for job " + job.getId() + " - item was null");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to create job item for " + job.getId() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                }
            } else {
            }
        }


        // Then, place remaining jobs in available content slots with pagination
        List<Job> remainingJobs = availableJobs.stream()
            .filter(job -> !configuredJobSlots.containsKey(job.getId()))
            .collect(Collectors.toList());


        List<Integer> availableContentSlots = contentSlots.stream()
            .filter(slot -> !usedSlots.contains(slot))
            .collect(Collectors.toList());


        int itemsPerPage = availableContentSlots.size();
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, remainingJobs.size());


        for (int i = startIndex; i < endIndex; i++) {
            Job job = remainingJobs.get(i);
            int slotIndex = i - startIndex;


            if (slotIndex < availableContentSlots.size()) {
                int slot = availableContentSlots.get(slotIndex);

                try {
                    ItemStack jobItem = getJobItemOptimized(job);
                    if (jobItem != null) {
                        inventory.setItem(slot, jobItem);
                        logDebugPlacement(job, slot);
                    } else {
                        plugin.getLogger().warning("[DEBUG] Failed to create item for remaining job " + job.getId() + " - item was null");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to create job item for " + job.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                plugin.getLogger().warning("[DEBUG] Slot index " + slotIndex + " >= available slots size " + availableContentSlots.size());
            }
        }

    }
    
    /**
     * Log debug placement if debugging is enabled.
     */
    private void logDebugPlacement(Job job, int slot) {
        if (plugin.getConfigCache() != null && plugin.getConfigCache().isDebugEnabled()) {
            plugin.getLogger().info("Placed job '" + job.getId() + "' in slot " + slot);
        }
    }
    
    /**
     * Get job item with performance optimization.
     */
    private ItemStack getJobItemOptimized(Job job) {
        // Use cache-optimized creation
        return createJobItemOptimized(job);
    }

    /**
     * Create optimized job item using proper API and error handling.
     */
    private ItemStack createJobItemOptimized(Job job) {

        JobItemFormat format = config.getJobItemFormat();

        // Get material with proper error handling
        Material iconMaterial = getJobMaterial(job, format);

        if (iconMaterial == null) {
            plugin.getLogger().warning("[DEBUG] Icon material is null for job " + job.getId() + ", returning null");
            return null;
        }

        // Get player job status efficiently
        boolean hasJob = playerData.hasJob(job.getId());
        // Always get saved stats, even if player left the job
        int playerLevel = playerData.getLevel(job.getId());
        long playerXp = (long) playerData.getXp(job.getId());


        // Create comprehensive placeholders for this specific job
        Map<String, String> jobPlaceholders = createJobPlaceholdersOptimized(job, hasJob, playerLevel, playerXp);

        // Build item using centralized configuration approach
        Map<String, Object> jobConfigMap = createJobConfigMap(job, format, iconMaterial, hasJob);

        MenuItemConfig jobItemConfig = new MenuItemConfig(new SimpleConfigurationSection(jobConfigMap));

        ItemStack result = createMenuItem(jobItemConfig, jobPlaceholders);

        if (result != null) {
            result = addJobNBT(result, job.getId());
        }

        return result;
    }
    
    /**
     * Get job material with proper error handling and no fallbacks.
     */
    private Material getJobMaterial(Job job, JobItemFormat format) {

        if (format.isUseJobIcon()) {
            String iconMaterial = job.getIconMaterial();

            if (iconMaterial == null || iconMaterial.isEmpty()) {
                plugin.getLogger().severe("No material configured for job " + job.getId());
                return null;
            }

            Material material = fr.ax_dev.universejobs.utils.EnumUtils.parseMaterial(iconMaterial, null);

            if (material == null) {
                plugin.getLogger().severe("Invalid material for job " + job.getId() + ": " + iconMaterial);
            }
            return material;
        } else {
            // Default material when format doesn't specify one
            return Material.PAPER;
        }
    }
    
    /**
     * Create job configuration map efficiently.
     */
    private Map<String, Object> createJobConfigMap(Job job, JobItemFormat format, Material iconMaterial, boolean hasJob) {
        Map<String, Object> jobConfigMap = new HashMap<>();

        jobConfigMap.put("enabled", true); // This was missing!
        jobConfigMap.put("material", iconMaterial.name());
        jobConfigMap.put("display-name", format.getDisplayName());
        jobConfigMap.put("lore", hasJob ? format.getLore() : format.getLoreWithoutJob());
        jobConfigMap.put("amount", format.getAmount());
        jobConfigMap.put("glow", format.shouldGlow(hasJob));
        jobConfigMap.put("hide-attributes", format.isHideAttributes());
        jobConfigMap.put("hide-enchants", format.isHideEnchants());
        
        // Apply custom model data if set
        if (job.getCustomModelData() > 0) {
            jobConfigMap.put("custom-model-data", job.getCustomModelData());
        }
        
        jobConfigMap.put("action", "open_job");
        jobConfigMap.put("action-value", job.getId());
        
        return jobConfigMap;
    }
    
    /**
     * Populate navigation items using centralized approach.
     */
    private void populateNavigationItems() {
        Map<String, MenuItemConfig> navItems = config.getNavigationItems();
        
        for (Map.Entry<String, MenuItemConfig> entry : navItems.entrySet()) {
            MenuItemConfig navConfig = entry.getValue();
            if (navConfig.isEnabled()) {
                ItemStack navItem = createMenuItem(navConfig, getNavigationPlaceholders());
                placeItemInSlots(navItem, navConfig.getSlots());
            }
        }
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
    
    @Override
    public void handleClick(int slot, InventoryClickEvent event) {
        // Handle navigation clicks first
        if (handleNavigationClickWithSound(slot)) {
            return;
        }
        
        // Handle job clicks with proper action detection
        handleJobClick(slot, event);
    }
    
    /**
     * Handle job click with proper action detection using click types.
     */
    private void handleJobClick(int slot, InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        Job clickedJob = findJobFromItem(event.getCurrentItem());
        if (clickedJob == null) return;


        // Handle different click types
        if (event.getClick() == ClickType.RIGHT) {
            handleQuickJobToggle(clickedJob);
        } else {
            plugin.getMenuManager().openJobMenu(player, clickedJob.getId());
        }
    }
    
    /**
     * Find job from clicked slot efficiently.
     */
    private Job findJobFromItem(ItemStack item) {
        if (item == null) return null;

        String jobId = getJobIdFromNBT(item);

        if (jobId != null) {
            Job job = plugin.getJobManager().getJob(jobId);
            return job;
        }

        return null;
    }
    
    /**
     * Add job NBT data to item.
     */
    private ItemStack addJobNBT(ItemStack item, String jobId) {
        if (item == null || jobId == null) return item;

        return fr.ax_dev.universejobs.utils.NBTItemUtils.setStringNBT(item, "universe_job_id", jobId);
    }

    /**
     * Get job ID from item NBT.
     */
    private String getJobIdFromNBT(ItemStack item) {
        if (item == null) return null;

        return fr.ax_dev.universejobs.utils.NBTItemUtils.getStringNBT(item, "universe_job_id");
    }

    /**
     * Handle quick job toggle (join/leave) with proper validation and error messages.
     */
    private void handleQuickJobToggle(Job job) {
        boolean currentlyHasJob = playerData.hasJob(job.getId());
        
        if (currentlyHasJob) {
            handleQuickLeaveJob(job);
        } else {
            handleQuickJoinJob(job);
        }
    }
    
    /**
     * Handle quick job leave with proper error handling.
     */
    private void handleQuickLeaveJob(Job job) {
        if (plugin.getJobManager().leaveJob(player, job.getId())) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.leave.success", "job", job.getName()));
            refresh();
        } else {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.leave.failed", "job", job.getName()));
        }
    }
    
    /**
     * Handle quick job join with proper validation and error handling.
     */
    private void handleQuickJoinJob(Job job) {
        if (!validateJobJoinRequirements(job)) {
            return;
        }
        
        if (plugin.getJobManager().joinJob(player, job.getId())) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.join.success", "job", job.getName()));
            refresh();
        } else {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.join.failed", "job", job.getName()));
        }
    }
    
    /**
     * Validate job join requirements with proper error messages.
     */
    private boolean validateJobJoinRequirements(Job job) {
        // Permission check
        if (job.getPermission() != null && !player.hasPermission(job.getPermission())) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.join.no-permission", "job", job.getName()));
            return false;
        }
        
        // Max jobs limit check using centralized approach
        int maxJobs = plugin.getConfigManager().getMaxJobsPerPlayer();
        int currentJobs = playerData.getJobs().size();
        if (currentJobs >= maxJobs) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.join.max-jobs-reached", "max", String.valueOf(maxJobs)));
            return false;
        }
        
        return true;
    }
    
    /**
     * Get navigation placeholders efficiently.
     */
    private Map<String, String> getNavigationPlaceholders() {
        int itemsPerPage = config.getContentSlots().size();
        Map<String, String> placeholders = MenuItemUtils.createNavigationPlaceholders(
            currentPage, availableJobs.size(), itemsPerPage);
        placeholders.put("total_jobs", String.valueOf(availableJobs.size()));
        placeholders.put("player_jobs", String.valueOf(playerData.getJobs().size()));
        placeholders.put("{max_jobs}", String.valueOf(plugin.getConfigManager().getMaxJobsPerPlayer()));
        return placeholders;
    }
    
    /**
     * Create general placeholders using centralized approach.
     */
    private Map<String, String> createGeneralPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("total_jobs", String.valueOf(availableJobs.size()));
        placeholders.put("player_jobs", String.valueOf(playerData.getJobs().size()));
        placeholders.put("{max_jobs}", String.valueOf(plugin.getConfigManager().getMaxJobsPerPlayer()));
        return placeholders;
    }
    
    /**
     * Create optimized job-specific placeholders using centralized calculations.
     */
    private Map<String, String> createJobPlaceholdersOptimized(Job job, boolean hasJob, int playerLevel, long playerXp) {
        Map<String, String> placeholders = new HashMap<>(cachedPlaceholders);
        
        // Add job-specific placeholders efficiently
        addJobBasicPlaceholders(placeholders, job);
        addPlayerStatusPlaceholders(placeholders, hasJob, playerLevel, playerXp);
        addProgressPlaceholders(placeholders, job, hasJob, playerLevel, playerXp);
        
        return placeholders;
    }
    
    /**
     * Add basic job placeholders efficiently.
     */
    private void addJobBasicPlaceholders(Map<String, String> placeholders, Job job) {
        placeholders.put("job_id", job.getId());
        placeholders.put("job_name", job.getName());
        placeholders.put("job_description", job.getDescription());
        placeholders.put("job_description_lines", String.join("\n", job.getDescriptionLines()));
        placeholders.put("job_max_level", String.valueOf(playerData.getMaxLevel(job.getId())));
        placeholders.put("job_permission", job.getPermission() != null ? job.getPermission() : "none");
    }
    
    /**
     * Add player status placeholders efficiently.
     */
    private void addPlayerStatusPlaceholders(Map<String, String> placeholders, boolean hasJob, int playerLevel, long playerXp) {
        placeholders.put("player_level", String.valueOf(playerLevel));
        placeholders.put("player_xp", String.valueOf(playerXp));
        placeholders.put("has_job", hasJob ? "Yes" : "No");
        placeholders.put("job_status", plugin.getConfigManager().getJobStatus(hasJob));
        placeholders.put("{max_jobs}", String.valueOf(plugin.getConfigManager().getMaxJobsPerPlayer()));
    }
    
    /**
     * Add progress placeholders with efficient calculations.
     */
    private void addProgressPlaceholders(Map<String, String> placeholders, Job job, boolean hasJob, int playerLevel, long playerXp) {
        // Calculate progress based on saved stats, regardless of current job status
        int effectiveMaxLevel = playerData.getMaxLevel(job.getId());
        if (job.getXpCurve() != null && playerLevel > 0 && playerLevel < effectiveMaxLevel) {
            calculateAndAddProgressValues(placeholders, job, playerLevel, playerXp);
        } else {
            addDefaultProgressValues(placeholders, job, playerLevel);
        }
    }
    
    /**
     * Calculate and add progress values efficiently.
     */
    private void calculateAndAddProgressValues(Map<String, String> placeholders, Job job, int playerLevel, long playerXp) {
        long currentLevelXp = (long) job.getXpCurve().getXpForLevel(playerLevel);
        long nextLevelXp = (long) job.getXpCurve().getXpForLevel(playerLevel + 1);
        long xpToNext = Math.max(0, nextLevelXp - playerXp);
        long xpProgress = playerXp - currentLevelXp;
        long xpRequired = nextLevelXp - currentLevelXp;

        placeholders.put("xp_to_next", String.valueOf(xpToNext));
        placeholders.put("next_level_xp", String.valueOf(xpRequired));
        placeholders.put("current_level_xp", String.valueOf(currentLevelXp));
        placeholders.put("current_xp", String.valueOf(xpProgress));

        double progressPercent = xpRequired > 0 ? (double) xpProgress / xpRequired * 100 : 0;
        placeholders.put("progress_percent", String.format("%.1f", progressPercent));
        placeholders.put("progress_bar", createProgressBarOptimized(progressPercent));
    }
    
    /**
     * Add default progress values for players without job or at max level.
     */
    private void addDefaultProgressValues(Map<String, String> placeholders, Job job, int playerLevel) {
        placeholders.put("xp_to_next", "0");
        placeholders.put("next_level_xp", "0");
        placeholders.put("current_level_xp", "0");
        
        boolean isMaxLevel = playerLevel >= playerData.getMaxLevel(job.getId());
        placeholders.put("progress_percent", isMaxLevel ? "100.0" : "0.0");
        placeholders.put("progress_bar", createProgressBarOptimized(isMaxLevel ? 100 : 0));
    }
    
    /**
     * Create optimized progress bar with efficient string building.
     */
    private String createProgressBarOptimized(double percent) {
        var progressBarConfig = plugin.getAccessor().getConfigManager().getProgressBarConfig();
        if (progressBarConfig != null) {
            return progressBarConfig.generateProgressBar(percent, 100, false);
        }
        
        // Fallback to legacy progress bar if config is not available
        int filledBars = (int) Math.round(percent / 100.0 * DEFAULT_PROGRESS_BARS);
        
        StringBuilder bar = new StringBuilder(64); // Pre-allocate capacity
        
        bar.append(PROGRESS_FILLED_COLOR);
        for (int i = 0; i < filledBars; i++) {
            bar.append(PROGRESS_FILLED);
        }
        
        bar.append(PROGRESS_EMPTY_COLOR);
        for (int i = filledBars; i < DEFAULT_PROGRESS_BARS; i++) {
            bar.append(PROGRESS_EMPTY);
        }
        
        return bar.toString();
    }
    
    @Override
    protected boolean hasNextPage() {
        Map<String, Integer> configuredJobSlots = config.getJobSlots();
        List<Job> remainingJobs = availableJobs.stream()
            .filter(job -> !configuredJobSlots.containsKey(job.getId()))
            .collect(Collectors.toList());
        
        List<Integer> contentSlots = config.getContentSlots();
        Set<Integer> usedSlots = new HashSet<>(configuredJobSlots.values());
        int availableSlots = (int) contentSlots.stream()
            .filter(slot -> !usedSlots.contains(slot))
            .count();
        
        return (currentPage + 1) * availableSlots < remainingJobs.size();
    }
}