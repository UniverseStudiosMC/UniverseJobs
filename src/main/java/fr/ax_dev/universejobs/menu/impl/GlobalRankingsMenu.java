package fr.ax_dev.universejobs.menu.impl;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.menu.BaseMenu;
import fr.ax_dev.universejobs.menu.config.MenuItemConfig;
import fr.ax_dev.universejobs.menu.config.SingleMenuConfig;
import fr.ax_dev.universejobs.menu.config.SimpleConfigurationSection;
import fr.ax_dev.universejobs.menu.utils.MenuItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Menu showing global job rankings for all jobs.
 */
public class GlobalRankingsMenu extends BaseMenu {
    
    private final Map<String, List<RankingEntry>> jobRankings;
    private final List<String> availableJobs;
    private String selectedJob;
    
    public GlobalRankingsMenu(UniverseJobs plugin, org.bukkit.entity.Player player, SingleMenuConfig config) {
        super(plugin, player, config);
        
        this.jobRankings = new HashMap<>();
        this.availableJobs = plugin.getJobManager().getJobs().values().stream()
            .filter(Job::isEnabled)
            .map(Job::getId)
            .sorted()
            .collect(Collectors.toList());
        
        this.selectedJob = availableJobs.isEmpty() ? null : availableJobs.get(0);
        
        loadRankings();
        
        // Initialize menu after all fields are set
        initialize();
    }
    
    /**
     * Load rankings for all jobs.
     */
    private void loadRankings() {
        for (String jobId : availableJobs) {
            List<RankingEntry> rankings = calculateJobRankings(jobId);
            jobRankings.put(jobId, rankings);
        }
    }
    
    /**
     * Calculate rankings for a specific job.
     */
    private List<RankingEntry> calculateJobRankings(String jobId) {
        List<RankingEntry> rankings = new ArrayList<>();
        
        // Get all player data and calculate rankings
        Map<UUID, PlayerJobData> allPlayerData = plugin.getJobManager().getAllPlayerData();
        
        for (Map.Entry<UUID, PlayerJobData> entry : allPlayerData.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerJobData playerData = entry.getValue();
            
            if (playerData.hasJob(jobId)) {
                int level = playerData.getLevel(jobId);
                long xp = (long) playerData.getXp(jobId);
                
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
                String playerName = offlinePlayer.getName();
                
                if (playerName != null) {
                    rankings.add(new RankingEntry(playerId, playerName, level, xp));
                }
            }
        }
        
        // Sort by level (descending), then by XP (descending)
        rankings.sort((a, b) -> {
            int levelCompare = Integer.compare(b.level, a.level);
            if (levelCompare != 0) return levelCompare;
            return Long.compare(b.xp, a.xp);
        });
        
        // Assign ranks
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).rank = i + 1;
        }
        
        return rankings;
    }
    
    @Override
    protected void populateInventory() {
        // Clear inventory first
        inventory.clear();
        
        // Add header with job selection
        addHeader();
        
        // Add job selection buttons
        addJobSelectionButtons();
        
        // Add ranking entries
        addRankingEntries();
        
        // Add navigation items
        addNavigationItems();
        
        // Add static items
        addStaticItems();
        
        // Fill empty slots
        addFillItems();
    }
    
    /**
     * Add header information.
     */
    private void addHeader() {
        if (selectedJob == null) {
            ItemStack noJobsItem = createNoJobsItem();
            inventory.setItem(4, noJobsItem);
            return;
        }
        
        Job job = plugin.getJobManager().getJob(selectedJob);
        if (job == null) return;
        
        ItemStack headerItem = createHeaderItem(job);
        inventory.setItem(4, headerItem);
    }
    
    /**
     * Create header item for selected job.
     */
    private ItemStack createHeaderItem(Job job) {
        List<RankingEntry> rankings = jobRankings.getOrDefault(selectedJob, new ArrayList<>());
        
        List<String> lore = new ArrayList<>();
        lore.add("&7Global rankings for this job");
        lore.add("");
        lore.add("&7Total Players: &e" + rankings.size());
        
        if (!rankings.isEmpty()) {
            RankingEntry topPlayer = rankings.get(0);
            lore.add("&7Top Player: &a" + topPlayer.playerName);
            lore.add("&7Top Level: &e" + topPlayer.level);
        }
        
        // Find player's rank
        UUID playerUUID = player.getUniqueId();
        Optional<RankingEntry> playerRanking = rankings.stream()
            .filter(entry -> entry.playerId.equals(playerUUID))
            .findFirst();
        
        if (playerRanking.isPresent()) {
            RankingEntry entry = playerRanking.get();
            lore.add("");
            lore.add("&6Your Ranking:");
            lore.add("&7Rank: &e#" + entry.rank);
            lore.add("&7Level: &a" + entry.level);
            lore.add("&7XP: &b" + entry.xp);
        } else {
            lore.add("");
            lore.add("&6Your Ranking:");
            lore.add("&7You don't have this job");
        }
        
        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
            job.getIconMaterial(), "&6&l" + job.getName() + " Rankings", lore, true
        );
        
        // Apply custom model data if set
        if (job.getCustomModelData() > 0) {
            configMap.put("custom-model-data", job.getCustomModelData());
        }
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, MenuItemUtils.createJobPlaceholders(job.getId(), job.getName(), job.getDescription()));
    }
    
    /**
     * Create no jobs available item.
     */
    private ItemStack createNoJobsItem() {
        List<String> lore = Arrays.asList(
            "&7No jobs available to show rankings for"
        );
        
        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
            "BARRIER", "&c&lNo Jobs Available", lore, false
        );
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig);
    }
    
    /**
     * Add job selection buttons using configuration.
     */
    private void addJobSelectionButtons() {
        if (availableJobs.isEmpty()) return;

        org.bukkit.configuration.ConfigurationSection selectionConfig =
            config.getRawConfig().getConfigurationSection("job-selection");

        if (selectionConfig == null) {
            plugin.getLogger().warning("Missing job-selection configuration in rankings-menu.yml");
            return;
        }

        // Get default slots
        org.bukkit.configuration.ConfigurationSection defaultFormat =
            selectionConfig.getConfigurationSection("default-format");

        List<Integer> defaultSlots = new ArrayList<>();
        if (defaultFormat != null && defaultFormat.contains("slots")) {
            // Handle both string and integer lists
            List<?> slotsList = defaultFormat.getList("slots");
            if (slotsList != null) {
                for (Object slot : slotsList) {
                    if (slot instanceof Integer) {
                        defaultSlots.add((Integer) slot);
                    } else if (slot instanceof String) {
                        try {
                            defaultSlots.add(Integer.parseInt((String) slot));
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("Invalid slot number: " + slot);
                        }
                    }
                }
            }
        }

        // Fallback if no slots configured
        if (defaultSlots.isEmpty()) {
            defaultSlots = Arrays.asList(19, 28, 37, 46);
        }

        plugin.getLogger().info("DEBUG: Default slots for job selection: " + defaultSlots);

        org.bukkit.configuration.ConfigurationSection jobsConfig =
            selectionConfig.getConfigurationSection("jobs");

        int slotIndex = 0;
        for (String jobId : availableJobs) {
            Job job = plugin.getJobManager().getJob(jobId);
            if (job == null) continue;

            Integer slot = null;

            // Check for specific job slot configuration
            if (jobsConfig != null && jobsConfig.contains(jobId)) {
                org.bukkit.configuration.ConfigurationSection jobConfig = jobsConfig.getConfigurationSection(jobId);
                if (jobConfig != null && jobConfig.contains("slot")) {
                    slot = jobConfig.getInt("slot");
                    plugin.getLogger().info("DEBUG: Job " + jobId + " has specific slot: " + slot);
                }
            }

            // Use default slots if no specific slot configured
            if (slot == null && slotIndex < defaultSlots.size()) {
                slot = defaultSlots.get(slotIndex);
                plugin.getLogger().info("DEBUG: Job " + jobId + " using default slot index " + slotIndex + ": " + slot);
                slotIndex++;
            }

            if (slot != null && slot >= 0 && slot < inventory.getSize()) {
                ItemStack button = createJobSelectionButton(job, jobId.equals(selectedJob), selectionConfig, jobsConfig);
                if (button != null) {
                    inventory.setItem(slot, button);
                    plugin.getLogger().info("DEBUG: Placed job button for " + jobId + " at slot " + slot);
                } else {
                    plugin.getLogger().warning("DEBUG: Failed to create button for job " + jobId);
                }
            } else {
                plugin.getLogger().warning("DEBUG: Invalid slot for job " + jobId + ": " + slot);
            }
        }
    }

    /**
     * Create job selection button using configuration.
     */
    private ItemStack createJobSelectionButton(Job job, boolean selected,
                                               org.bukkit.configuration.ConfigurationSection selectionConfig,
                                               org.bukkit.configuration.ConfigurationSection jobsConfig) {

        // Get rankings for placeholders
        List<RankingEntry> rankings = jobRankings.getOrDefault(job.getId(), new ArrayList<>());

        // Create placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{job_id}", job.getId());
        placeholders.put("{job_name}", job.getName());
        placeholders.put("{job_material}", job.getIconMaterial() != null ? job.getIconMaterial() : "PAPER");
        placeholders.put("{job_custom-model-data}", String.valueOf(job.getCustomModelData()));
        placeholders.put("{player_count}", String.valueOf(rankings.size()));

        // Add top player info
        if (!rankings.isEmpty()) {
            RankingEntry topPlayer = rankings.get(0);
            placeholders.put("{top_player}", topPlayer.playerName);
            placeholders.put("{top_level}", String.valueOf(topPlayer.level));
        } else {
            placeholders.put("{top_player}", "None");
            placeholders.put("{top_level}", "0");
        }

        // Determine which configuration to use (selected or not_selected)
        org.bukkit.configuration.ConfigurationSection stateConfig = null;
        String stateSuffix = selected ? "selected" : "not_selected";

        // Check for specific job configuration first
        if (jobsConfig != null && jobsConfig.contains(job.getId())) {
            org.bukkit.configuration.ConfigurationSection jobConfig = jobsConfig.getConfigurationSection(job.getId());
            if (jobConfig != null && jobConfig.contains(stateSuffix)) {
                stateConfig = jobConfig.getConfigurationSection(stateSuffix);
            }
        }

        // Use default format if no specific config
        if (stateConfig == null) {
            org.bukkit.configuration.ConfigurationSection defaultFormat =
                selectionConfig.getConfigurationSection("default-format");
            if (defaultFormat != null && defaultFormat.contains(stateSuffix)) {
                stateConfig = defaultFormat.getConfigurationSection(stateSuffix);
            }
        }

        if (stateConfig == null) {
            // Fallback to legacy method
            return createLegacyJobButton(job, selected, rankings.size());
        }

        // Build item configuration
        Map<String, Object> configMap = new HashMap<>();

        // Material
        String material = stateConfig.getString("material", job.getIconMaterial());
        if (material != null && material.equals("{job_material}")) {
            material = job.getIconMaterial() != null ? job.getIconMaterial() : "PAPER";
        }
        configMap.put("material", material);

        // Custom model data
        String customModelStr = stateConfig.getString("customodeldata", "0");
        if (customModelStr.equals("{job_custom-model-data}")) {
            configMap.put("custom-model-data", job.getCustomModelData());
        } else {
            int customModelData = stateConfig.getInt("customodeldata", 0);
            if (customModelData > 0) {
                configMap.put("custom-model-data", customModelData);
            }
        }

        // Display name
        configMap.put("display-name", stateConfig.getString("display-name", job.getName()));

        // Lore
        configMap.put("lore", stateConfig.getStringList("lore"));

        // Item settings
        configMap.put("amount", stateConfig.getInt("amount", 1));
        configMap.put("glow", stateConfig.getBoolean("glow", false));
        configMap.put("hide-attributes", stateConfig.getBoolean("hide-attributes", true));
        configMap.put("hide-enchants", stateConfig.getBoolean("hide-enchants", false));

        configMap.put("action", "select_job");
        configMap.put("action-value", job.getId());

        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, placeholders);
    }

    /**
     * Create legacy job button (fallback).
     */
    private ItemStack createLegacyJobButton(Job job, boolean selected, int playerCount) {
        List<String> lore = Arrays.asList(
            "&7Click to view rankings",
            "",
            "&7Players: &e" + playerCount,
            selected ? "" : "",
            selected ? "&a▶ Currently Selected" : ""
        );

        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
            job.getIconMaterial(), "&e" + job.getName(), lore, selected
        );

        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, new HashMap<>());
    }
    
    /**
     * Add ranking entries to the menu.
     */
    private void addRankingEntries() {
        if (selectedJob == null) return;

        List<RankingEntry> rankings = jobRankings.getOrDefault(selectedJob, new ArrayList<>());
        if (rankings.isEmpty()) return;

        List<Integer> contentSlots = new ArrayList<>(config.getContentSlots());
        Set<Integer> usedSlots = new HashSet<>();

        // First, place entries with specific slots
        for (RankingEntry entry : rankings) {
            ItemStack rankingItem = createRankingItem(entry);
            if (rankingItem == null) continue;

            if (entry.preferredSlot != null && entry.preferredSlot >= 0 && entry.preferredSlot < inventory.getSize()) {
                inventory.setItem(entry.preferredSlot, rankingItem);
                usedSlots.add(entry.preferredSlot);
                contentSlots.remove(entry.preferredSlot);
            }
        }

        // Then place remaining entries in available content slots
        int slotIndex = 0;
        for (RankingEntry entry : rankings) {
            if (entry.preferredSlot != null) continue; // Already placed

            ItemStack rankingItem = createRankingItem(entry);
            if (rankingItem == null) continue;

            if (slotIndex < contentSlots.size()) {
                int slot = contentSlots.get(slotIndex);
                if (!usedSlots.contains(slot)) {
                    inventory.setItem(slot, rankingItem);
                    slotIndex++;
                }
            }
        }
    }
    
    /**
     * Create ranking entry item using configuration.
     */
    private ItemStack createRankingItem(RankingEntry entry) {
        // Get ranking entries configuration
        org.bukkit.configuration.ConfigurationSection entriesConfig =
            config.getRawConfig().getConfigurationSection("ranking-entries");

        if (entriesConfig == null) {
            plugin.getLogger().warning("Missing ranking-entries configuration in rankings-menu.yml");
            return createDefaultRankingItem(entry);
        }

        // Determine which config to use based on rank
        org.bukkit.configuration.ConfigurationSection rankConfig = getRankConfig(entriesConfig, entry.rank);

        if (rankConfig == null || !rankConfig.getBoolean("enabled", true)) {
            return null;
        }

        // Create placeholders for this entry
        Map<String, String> placeholders = createRankingPlaceholders(entry);

        // Build item configuration from config
        Map<String, Object> configMap = new HashMap<>();

        // Material
        String material = rankConfig.getString("material", "PLAYER_HEAD");
        configMap.put("material", material);

        // Handle player head texture
        if (material.equalsIgnoreCase("PLAYER_HEAD")) {
            String headTexture = rankConfig.getString("player-head", "");
            if (!headTexture.isEmpty()) {
                if (headTexture.equals("{player_texture}") || headTexture.equals("{player_name}")) {
                    // Use actual player's head name
                    configMap.put("player-head", entry.playerName);
                } else {
                    // Use custom texture
                    configMap.put("player-head", headTexture);
                }
            }
        }

        // Display name
        configMap.put("display-name", rankConfig.getString("display-name", "#{rank} - {player}"));

        // Base lore
        List<String> lore = new ArrayList<>(rankConfig.getStringList("lore"));


        configMap.put("lore", lore);

        // General item settings
        configMap.put("amount", rankConfig.getInt("amount", 1));
        configMap.put("glow", rankConfig.getBoolean("glow", false) ||
            (entry.playerId.equals(player.getUniqueId()) && entry.rank <= 3));
        configMap.put("hide-attributes", rankConfig.getBoolean("hide-attributes", true));
        configMap.put("hide-enchants", rankConfig.getBoolean("hide-enchants", false));

        int customModelData = rankConfig.getInt("custom-model-data", 0);
        if (customModelData > 0) {
            configMap.put("custom-model-data", customModelData);
        }

        // Check for specific slot
        if (rankConfig.contains("slot")) {
            entry.preferredSlot = rankConfig.getInt("slot");
        }

        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, placeholders);
    }

    /**
     * Get the appropriate rank configuration based on rank number.
     */
    private org.bukkit.configuration.ConfigurationSection getRankConfig(
            org.bukkit.configuration.ConfigurationSection entriesConfig, int rank) {

        // Check for specific rank configuration
        if (rank == 1 && entriesConfig.contains("rank-1")) {
            return entriesConfig.getConfigurationSection("rank-1");
        } else if (rank == 2 && entriesConfig.contains("rank-2")) {
            return entriesConfig.getConfigurationSection("rank-2");
        } else if (rank == 3 && entriesConfig.contains("rank-3")) {
            return entriesConfig.getConfigurationSection("rank-3");
        } else if (rank >= 4 && rank <= 10 && entriesConfig.contains("rank-top10")) {
            return entriesConfig.getConfigurationSection("rank-top10");
        } else if (entriesConfig.contains("rank-default")) {
            return entriesConfig.getConfigurationSection("rank-default");
        }

        return null;
    }

    /**
     * Create default ranking item (fallback).
     */
    private ItemStack createDefaultRankingItem(RankingEntry entry) {
        Map<String, String> placeholders = createRankingPlaceholders(entry);

        List<String> lore = Arrays.asList(
            "&7Player: &f" + entry.playerName,
            "&7Level: &a" + entry.level,
            "&7Total XP: &b" + entry.xp
        );

        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
            "PLAYER_HEAD", "#{rank} - {player}", lore, false
        );

        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, placeholders);
    }

    /**
     * Create placeholders for ranking entry.
     */
    private Map<String, String> createRankingPlaceholders(RankingEntry entry) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{rank}", String.valueOf(entry.rank));
        placeholders.put("{player}", entry.playerName);
        placeholders.put("{player_name}", entry.playerName);
        placeholders.put("{level}", String.valueOf(entry.level));
        placeholders.put("{xp}", String.valueOf(entry.xp));
        placeholders.put("{rank_color}", getRankColorFromConfig(entry.rank));
        return placeholders;
    }

    /**
     * Get rank material from configuration.
     */
    private String getRankMaterialFromConfig(int rank, org.bukkit.configuration.ConfigurationSection config) {
        org.bukkit.configuration.ConfigurationSection materials = config.getConfigurationSection("rank-materials");
        if (materials == null) {
            return "PLAYER_HEAD";
        }

        if (rank == 1 && materials.contains("1")) {
            return materials.getString("1");
        } else if (rank == 2 && materials.contains("2")) {
            return materials.getString("2");
        } else if (rank == 3 && materials.contains("3")) {
            return materials.getString("3");
        } else if (rank <= 10 && materials.contains("top-10")) {
            return materials.getString("top-10");
        } else {
            return materials.getString("default", "PLAYER_HEAD");
        }
    }

    /**
     * Get rank color from configuration.
     */
    private String getRankColorFromConfig(int rank) {
        org.bukkit.configuration.ConfigurationSection colorConfig =
            config.getRawConfig().getConfigurationSection("ranking-entry-format.rank-colors");

        if (colorConfig == null) {
            return "<white>";
        }

        if (rank == 1 && colorConfig.contains("1")) {
            return colorConfig.getString("1");
        } else if (rank == 2 && colorConfig.contains("2")) {
            return colorConfig.getString("2");
        } else if (rank == 3 && colorConfig.contains("3")) {
            return colorConfig.getString("3");
        } else if (rank <= 10 && colorConfig.contains("top-10")) {
            return colorConfig.getString("top-10");
        } else {
            return colorConfig.getString("default", "<white>");
        }
    }
    
    /**
     * Add navigation items.
     */
    private void addNavigationItems() {
        Map<String, MenuItemConfig> navItems = config.getNavigationItems();
        MenuItemUtils.addNavigationItems(inventory, navItems, currentPage, hasNextPage(),
            getNavigationPlaceholders(), config -> createMenuItem(config, getNavigationPlaceholders()));
    }
    
    /**
     * Add static items from configuration.
     */
    private void addStaticItems() {
        Map<String, String> placeholders = selectedJob != null ? 
            MenuItemUtils.createJobPlaceholders(
                selectedJob, 
                plugin.getJobManager().getJob(selectedJob).getName(),
                plugin.getJobManager().getJob(selectedJob).getDescription()
            ) : new HashMap<>();
        
        MenuItemUtils.addStaticItems(inventory, config.getStaticItems(), placeholders,
            config -> createMenuItem(config, placeholders));
    }
    
    @Override
    public void handleClick(int slot, InventoryClickEvent event) {
        // Handle navigation clicks first
        if (handleNavigationClickWithSound(slot)) {
            return;
        }
        
        // Handle job selection buttons using dynamic slot detection
        String clickedJobId = getJobIdBySlot(slot);
        if (clickedJobId != null) {
            if (!clickedJobId.equals(selectedJob)) {
                selectedJob = clickedJobId;
                currentPage = 0; // Reset to first page
                refresh();
            }
            return;
        }
        
        // Handle ranking item clicks (for future expansion - maybe show player details)
        List<Integer> contentSlots = config.getContentSlots();
        if (contentSlots.contains(slot)) {
            // Could implement player profile viewing here
        }
    }
    
    @Override
    protected boolean hasNextPage() {
        if (selectedJob == null) return false;
        
        List<RankingEntry> rankings = jobRankings.getOrDefault(selectedJob, new ArrayList<>());
        return (currentPage + 1) * config.getItemsPerPage() < rankings.size();
    }
    
    @Override
    protected void handleBackButton() {
        plugin.getMenuManager().openJobsMainMenu(player);
    }

    /**
     * Get the job ID that corresponds to the clicked slot.
     */
    private String getJobIdBySlot(int clickedSlot) {
        org.bukkit.configuration.ConfigurationSection selectionConfig =
            config.getRawConfig().getConfigurationSection("job-selection");

        if (selectionConfig == null) {
            return null;
        }

        // Get default slots
        org.bukkit.configuration.ConfigurationSection defaultFormat =
            selectionConfig.getConfigurationSection("default-format");
        List<Integer> defaultSlots = new ArrayList<>();

        if (defaultFormat != null && defaultFormat.contains("slots")) {
            List<?> slotsList = defaultFormat.getList("slots");
            if (slotsList != null) {
                for (Object slot : slotsList) {
                    if (slot instanceof Integer) {
                        defaultSlots.add((Integer) slot);
                    } else if (slot instanceof String) {
                        try {
                            defaultSlots.add(Integer.parseInt((String) slot));
                        } catch (NumberFormatException e) {
                            // Skip invalid slots
                        }
                    }
                }
            }
        }

        org.bukkit.configuration.ConfigurationSection jobsConfig =
            selectionConfig.getConfigurationSection("jobs");

        // Check specific job slot configurations first
        if (jobsConfig != null) {
            for (String jobId : availableJobs) {
                if (jobsConfig.contains(jobId)) {
                    org.bukkit.configuration.ConfigurationSection jobConfig = jobsConfig.getConfigurationSection(jobId);
                    if (jobConfig != null && jobConfig.contains("slot")) {
                        int jobSlot = jobConfig.getInt("slot");
                        if (jobSlot == clickedSlot) {
                            return jobId;
                        }
                    }
                }
            }
        }

        // Check default slots
        int slotIndex = 0;
        for (String jobId : availableJobs) {
            // Skip jobs that have specific slot configuration
            boolean hasSpecificSlot = false;
            if (jobsConfig != null && jobsConfig.contains(jobId)) {
                org.bukkit.configuration.ConfigurationSection jobConfig = jobsConfig.getConfigurationSection(jobId);
                if (jobConfig != null && jobConfig.contains("slot")) {
                    hasSpecificSlot = true;
                }
            }

            if (!hasSpecificSlot && slotIndex < defaultSlots.size()) {
                int defaultSlot = defaultSlots.get(slotIndex);
                if (defaultSlot == clickedSlot) {
                    return jobId;
                }
                slotIndex++;
            }
        }

        return null;
    }
    
    /**
     * Get navigation placeholders.
     */
    private Map<String, String> getNavigationPlaceholders() {
        List<RankingEntry> rankings = selectedJob != null ? 
            jobRankings.getOrDefault(selectedJob, new ArrayList<>()) : new ArrayList<>();
        
        Map<String, String> placeholders = MenuItemUtils.createNavigationPlaceholders(
            currentPage, rankings.size(), config.getItemsPerPage());
        placeholders.put("total_players", String.valueOf(rankings.size()));
        placeholders.put("selected_job", selectedJob != null ? selectedJob : "None");
        return placeholders;
    }
    
    
    /**
     * Get material for ranking position.
     */
    private String getRankMaterial(int rank) {
        return switch (rank) {
            case 1 -> "GOLD_INGOT";
            case 2 -> "IRON_INGOT";
            case 3 -> "COPPER_INGOT";
            default -> rank <= 10 ? "EMERALD" : "PLAYER_HEAD";
        };
    }
    
    /**
     * Get color for ranking position.
     */
    private String getRankColor(int rank) {
        return switch (rank) {
            case 1 -> "&6&l";
            case 2 -> "&7&l";
            case 3 -> "&c&l";
            default -> rank <= 10 ? "&a" : "&f";
        };
    }
    
    
    /**
     * Container for ranking entry.
     */
    private static class RankingEntry {
        final UUID playerId;
        final String playerName;
        final int level;
        final long xp;
        int rank;
        Integer preferredSlot;
        
        RankingEntry(UUID playerId, String playerName, int level, long xp) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.level = level;
            this.xp = xp;
        }
    }
    
}