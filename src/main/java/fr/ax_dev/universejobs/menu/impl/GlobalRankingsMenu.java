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
        
        // Populate inventory after all fields are initialized
        populateInventory();
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
     * Add job selection buttons.
     */
    private void addJobSelectionButtons() {
        if (availableJobs.isEmpty()) return;
        
        // Job selection buttons in the second row
        int startSlot = 10;
        int maxButtons = 7; // Slots 10-16
        
        for (int i = 0; i < Math.min(availableJobs.size(), maxButtons); i++) {
            String jobId = availableJobs.get(i);
            Job job = plugin.getJobManager().getJob(jobId);
            if (job == null) continue;
            
            ItemStack jobButton = createJobSelectionButton(job, jobId.equals(selectedJob));
            inventory.setItem(startSlot + i, jobButton);
        }
    }
    
    /**
     * Create job selection button.
     */
    private ItemStack createJobSelectionButton(Job job, boolean selected) {
        List<RankingEntry> rankings = jobRankings.getOrDefault(job.getId(), new ArrayList<>());
        
        List<String> lore = new ArrayList<>();
        lore.add("&7Click to view rankings for this job");
        lore.add("");
        lore.add("&7Players: &e" + rankings.size());
        
        if (selected) {
            lore.add("");
            lore.add("&a▶ Currently Selected");
        }
        
        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
            job.getIconMaterial(), "&e" + job.getName(), lore, selected
        );
        
        // Apply custom model data if set
        if (job.getCustomModelData() > 0) {
            configMap.put("custom-model-data", job.getCustomModelData());
        }
        
        configMap.put("action", "select_job");
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, MenuItemUtils.createJobPlaceholders(job.getId(), job.getName(), job.getDescription()));
    }
    
    /**
     * Add ranking entries to the menu.
     */
    private void addRankingEntries() {
        if (selectedJob == null) return;
        
        List<RankingEntry> rankings = jobRankings.getOrDefault(selectedJob, new ArrayList<>());
        if (rankings.isEmpty()) return;
        
        List<Integer> contentSlots = config.getContentSlots();
        int startIndex = currentPage * config.getItemsPerPage();
        int endIndex = Math.min(startIndex + config.getItemsPerPage(), rankings.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            RankingEntry entry = rankings.get(i);
            int slotIndex = i - startIndex;
            
            if (slotIndex >= contentSlots.size()) break;
            
            int slot = contentSlots.get(slotIndex);
            ItemStack rankingItem = createRankingItem(entry);
            inventory.setItem(slot, rankingItem);
        }
    }
    
    /**
     * Create ranking entry item.
     */
    private ItemStack createRankingItem(RankingEntry entry) {
        // Choose material based on rank
        String material = getRankMaterial(entry.rank);
        String rankColor = getRankColor(entry.rank);
        
        List<String> lore = new ArrayList<>();
        lore.add("&7Player: &f" + entry.playerName);
        lore.add("&7Level: &a" + entry.level);
        lore.add("&7Total XP: &b" + entry.xp);
        
        // Show if it's the current player
        if (entry.playerId.equals(player.getUniqueId())) {
            lore.add("");
            lore.add("&e⭐ This is you!");
        }
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("rank", String.valueOf(entry.rank));
        placeholders.put("player", entry.playerName);
        placeholders.put("level", String.valueOf(entry.level));
        placeholders.put("xp", String.valueOf(entry.xp));
        
        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
            material, rankColor + "#" + entry.rank + " - " + entry.playerName, lore, 
            entry.playerId.equals(player.getUniqueId())
        );
        
        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, placeholders);
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
        if (handleNavigationClick(slot)) {
            return;
        }
        
        // Handle job selection buttons (slots 10-16)
        if (slot >= 10 && slot <= 16) {
            int jobIndex = slot - 10;
            if (jobIndex < availableJobs.size()) {
                String newSelectedJob = availableJobs.get(jobIndex);
                if (!newSelectedJob.equals(selectedJob)) {
                    selectedJob = newSelectedJob;
                    currentPage = 0; // Reset to first page
                    refresh();
                }
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
        
        RankingEntry(UUID playerId, String playerName, int level, long xp) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.level = level;
            this.xp = xp;
        }
    }
    
}