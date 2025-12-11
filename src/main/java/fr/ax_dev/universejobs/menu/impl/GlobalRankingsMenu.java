package fr.ax_dev.universejobs.menu.impl;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.item.ModelDataComponentConfig;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.menu.BaseMenu;
import fr.ax_dev.universejobs.menu.config.MenuItemConfig;
import fr.ax_dev.universejobs.menu.config.SingleMenuConfig;
import fr.ax_dev.universejobs.menu.config.SimpleConfigurationSection;
import fr.ax_dev.universejobs.menu.utils.MenuItemUtils;
import fr.ax_dev.universejobs.utils.NBTItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Menu showing global job rankings for all jobs.
 * Optimizado para Folia - evita bloqueos en CompletableFuture.get()
 */
public class GlobalRankingsMenu extends BaseMenu {

    private static final String JOB_MODEL_PLACEHOLDER = "{job_custom-model-data}";
    private static final String JOB_MODEL_STRINGS_PLACEHOLDER = "{job_model_data_component_strings}";
    private static final int MAX_ITEMS_PER_PAGE = 36;

    private final Map<String, List<RankingEntry>> jobRankings;
    private final List<String> availableJobs;
    private final Map<String, ItemStack> itemCache;
    private String selectedJob;

    public GlobalRankingsMenu(UniverseJobs plugin, org.bukkit.entity.Player player, SingleMenuConfig config) {
        this(plugin, player, config, null);
    }

    public GlobalRankingsMenu(UniverseJobs plugin, org.bukkit.entity.Player player, SingleMenuConfig config, String preSelectedJob) {
        super(plugin, player, config);

        this.jobRankings = new HashMap<>();
        this.itemCache = new HashMap<>();
        this.availableJobs = plugin.getJobManager().getJobs().values().stream()
                .filter(Job::isEnabled)
                .map(Job::getId)
                .sorted()
                .collect(Collectors.toList());

        if (preSelectedJob != null && availableJobs.contains(preSelectedJob)) {
            this.selectedJob = preSelectedJob;
        } else {
            this.selectedJob = availableJobs.isEmpty() ? null :
                    availableJobs.stream()
                            .filter(jobId -> !jobId.startsWith("_"))
                            .findFirst()
                            .orElse(availableJobs.get(0));
        }

        if (!plugin.isDatabaseEnabled()) {
            for (String jobId : availableJobs) {
                jobRankings.put(jobId, calculateJobRankingsFromMemory(jobId));
            }
            initialize();
        } else {
            java.util.concurrent.CompletableFuture<Void> loadFuture = loadRankingsAsync();
            loadFuture.thenRun(() -> plugin.getFoliaManager().runAtEntity(player, () -> {
                initialize();
                open();
            }));
        }
    }

    public boolean isReady() {
        return inventory != null;
    }

    private java.util.concurrent.CompletableFuture<Void> loadRankingsAsync() {
        List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();

        for (String jobId : availableJobs) {
            java.util.concurrent.CompletableFuture<Void> future = plugin.getDataStorage()
                    .getJobLeaderboard(jobId, 100)
                    .thenAccept(entries -> {
                        List<RankingEntry> rankings = new ArrayList<>(entries.size());
                        for (int i = 0; i < entries.size(); i++) {
                            var entry = entries.get(i);
                            RankingEntry rankEntry = new RankingEntry(
                                    entry.getPlayerId(),
                                    entry.getPlayerName(),
                                    entry.getLevel(),
                                    (long) entry.getXp()
                            );
                            rankEntry.rank = i + 1;
                            rankings.add(rankEntry);

                            if (i < 20) {
                                fr.ax_dev.universejobs.utils.PlayerTextureCache.getPlayerProfile(
                                        entry.getPlayerId(), entry.getPlayerName());
                            }
                        }
                        jobRankings.put(jobId, rankings);
                    })
                    .exceptionally(ex -> {
                        plugin.getLogger().warning("Failed to load rankings for " + jobId + ", using memory fallback: " + ex.getMessage());
                        jobRankings.put(jobId, calculateJobRankingsFromMemory(jobId));
                        return null;
                    });
            futures.add(future);
        }

        return java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]));
    }

    private List<RankingEntry> calculateJobRankingsFromMemory(String jobId) {
        List<RankingEntry> rankings = new ArrayList<>(64);

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

        rankings.sort((a, b) -> {
            int levelCompare = Integer.compare(b.level, a.level);
            if (levelCompare != 0) return levelCompare;
            return Long.compare(b.xp, a.xp);
        });

        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).rank = i + 1;
        }

        return rankings;
    }

    @Override
    protected void populateInventory() {
        inventory.clear();
        addJobSelectionButtons();
        addRankingEntries();
        addNavigationItems();
        addStaticItems();
        addFillItems();
    }

    private void addJobSelectionButtons() {
        if (availableJobs.isEmpty()) return;

        org.bukkit.configuration.ConfigurationSection selectionConfig =
                config.getRawConfig().getConfigurationSection("job-selection");

        if (selectionConfig == null) {
            plugin.getLogger().warning("Missing job-selection configuration in rankings-menu.yml");
            return;
        }

        org.bukkit.configuration.ConfigurationSection jobsConfig =
                selectionConfig.getConfigurationSection("jobs");

        Map<String, Integer> jobSlots = plugin.getMenuManager().getJobSlotManager().getAllJobSlots();

        for (String jobId : availableJobs) {
            Job job = plugin.getJobManager().getJob(jobId);
            if (job == null) continue;

            Integer slot = null;

            if (jobsConfig != null && jobsConfig.contains(jobId)) {
                org.bukkit.configuration.ConfigurationSection jobConfig = jobsConfig.getConfigurationSection(jobId);
                if (jobConfig != null && jobConfig.contains("slot")) {
                    slot = jobConfig.getInt("slot");
                }
            }

            if (slot == null && jobSlots.containsKey(jobId)) {
                slot = jobSlots.get(jobId);
            }

            if (slot != null && slot >= 0 && slot < inventory.getSize()) {
                ItemStack button = createJobSelectionButton(job, jobId.equals(selectedJob), selectionConfig, jobsConfig);
                if (button != null) {
                    inventory.setItem(slot, button);
                }
            }
        }
    }

    private ItemStack createJobSelectionButton(Job job, boolean selected,
                                               org.bukkit.configuration.ConfigurationSection selectionConfig,
                                               org.bukkit.configuration.ConfigurationSection jobsConfig) {

        List<RankingEntry> rankings = jobRankings.getOrDefault(job.getId(), new ArrayList<>());

        Map<String, String> placeholders = new HashMap<>(16);
        placeholders.put("{job_id}", job.getId());
        placeholders.put("{job_name}", job.getName());
        placeholders.put("{job_material}", job.getIconMaterial() != null ? job.getIconMaterial() : "PAPER");

        ModelDataComponentConfig jobModelData = job.getIconModelData();
        String legacyModelValue = jobModelData != null
                ? jobModelData.getLegacyCustomModelData()
                .map(String::valueOf)
                .orElseGet(() -> jobModelData.getFirstString().orElse("0"))
                : "0";
        placeholders.put("{job_custom-model-data}", legacyModelValue);
        placeholders.put("{job_model_data_component_strings}",
                jobModelData != null && !jobModelData.getStrings().isEmpty()
                        ? String.join(",", jobModelData.getStrings())
                        : "");

        placeholders.put("{player_count}", String.valueOf(rankings.size()));

        if (!rankings.isEmpty()) {
            RankingEntry topPlayer = rankings.get(0);
            placeholders.put("{top_player}", topPlayer.playerName);
            placeholders.put("{top_level}", String.valueOf(topPlayer.level));
        } else {
            placeholders.put("{top_player}", "None");
            placeholders.put("{top_level}", "0");
        }

        org.bukkit.configuration.ConfigurationSection stateConfig = null;
        String stateSuffix = selected ? "selected" : "not_selected";

        if (jobsConfig != null && jobsConfig.contains(job.getId())) {
            org.bukkit.configuration.ConfigurationSection jobConfig = jobsConfig.getConfigurationSection(job.getId());
            if (jobConfig != null && jobConfig.contains(stateSuffix)) {
                stateConfig = jobConfig.getConfigurationSection(stateSuffix);
            }
        }

        if (stateConfig == null) {
            org.bukkit.configuration.ConfigurationSection defaultFormat =
                    selectionConfig.getConfigurationSection("default-format");
            if (defaultFormat != null && defaultFormat.contains(stateSuffix)) {
                stateConfig = defaultFormat.getConfigurationSection(stateSuffix);
            }
        }

        if (stateConfig == null) {
            return createLegacyJobButton(job, selected, rankings.size());
        }

        Map<String, Object> configMap = new HashMap<>(16);

        String material = stateConfig.getString("material", job.getIconMaterial());
        if (material != null && material.equals("{job_material}")) {
            material = job.getIconMaterial() != null ? job.getIconMaterial() : "PAPER";
        }
        configMap.put("material", material);

        applyModelData(configMap, job.getIconModelData(), stateConfig);

        configMap.put("display-name", stateConfig.getString("display-name", job.getName()));
        configMap.put("lore", stateConfig.getStringList("lore"));
        configMap.put("enabled", true);
        configMap.put("amount", stateConfig.getInt("amount", 1));
        configMap.put("glow", stateConfig.getBoolean("glow", false));
        configMap.put("hide-attributes", stateConfig.getBoolean("hide-attributes", true));
        configMap.put("hide-enchants", stateConfig.getBoolean("hide-enchants", false));
        configMap.put("action", "select_job");
        configMap.put("action-value", job.getId());

        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        ItemStack item = createMenuItem(itemConfig, placeholders);

        if (item != null) {
            item = NBTItemUtils.setStringNBT(item, "universe_job_id", job.getId());
        }

        return item;
    }

    private ItemStack createLegacyJobButton(Job job, boolean selected, int playerCount) {
        List<String> lore = Arrays.asList(
                "&7Click to view rankings",
                "",
                "&7Players: &e" + playerCount,
                selected ? "&a▶ Currently Selected" : ""
        );

        Map<String, Object> configMap = MenuItemUtils.createItemConfigMap(
                job.getIconMaterial(), "&e" + job.getName(), lore, selected
        );
        configMap.put("enabled", true);

        applyModelData(configMap, job.getIconModelData(), null);

        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        ItemStack item = createMenuItem(itemConfig, new HashMap<>());

        if (item != null) {
            item = NBTItemUtils.setStringNBT(item, "universe_job_id", job.getId());
        }

        return item;
    }


    private void addRankingEntries() {
        if (selectedJob == null) return;

        List<RankingEntry> allRankings = jobRankings.getOrDefault(selectedJob, new ArrayList<>());
        if (allRankings.isEmpty()) return;


        int itemsPerPage = config.getItemsPerPage();
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allRankings.size());
        List<RankingEntry> pageRankings = allRankings.subList(startIndex, endIndex);

        List<Integer> contentSlots = new ArrayList<>(config.getContentSlots());
        Set<Integer> usedSlots = new HashSet<>(contentSlots.size());


        for (RankingEntry entry : pageRankings) {
            ItemStack rankingItem = createRankingItem(entry);
            if (rankingItem == null) continue;

            if (entry.preferredSlot != null && entry.preferredSlot >= 0 && entry.preferredSlot < inventory.getSize()) {
                inventory.setItem(entry.preferredSlot, rankingItem);
                usedSlots.add(entry.preferredSlot);
                contentSlots.remove(entry.preferredSlot);
            }
        }

        int slotIndex = 0;
        for (RankingEntry entry : pageRankings) {
            if (entry.preferredSlot != null) continue;

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


    private ItemStack createRankingItem(RankingEntry entry) {

        String cacheKey = selectedJob + ":" + entry.rank;

        if (itemCache.containsKey(cacheKey)) {
            return itemCache.get(cacheKey).clone();
        }

        org.bukkit.configuration.ConfigurationSection entriesConfig =
                config.getRawConfig().getConfigurationSection("ranking-entries");

        if (entriesConfig == null) {
            return createDefaultRankingItem(entry);
        }

        org.bukkit.configuration.ConfigurationSection rankConfig = getRankConfig(entriesConfig, entry.rank);

        if (rankConfig == null || !rankConfig.getBoolean("enabled", true)) {
            return null;
        }

        Map<String, String> placeholders = createRankingPlaceholders(entry);
        Map<String, Object> configMap = new HashMap<>(16);

        String material = rankConfig.getString("material", "PLAYER_HEAD");
        configMap.put("material", material);

        if (material.equalsIgnoreCase("PLAYER_HEAD")) {
            String headTexture = rankConfig.getString("player-head", "");
            if (headTexture.isEmpty() || headTexture.equals("{player_texture}") || headTexture.equals("{player_name}")) {
                configMap.put("player-head", entry.playerName);
            } else {
                configMap.put("player-head", headTexture);
            }
        }

        configMap.put("display-name", rankConfig.getString("display-name", "#{rank} - {player}"));
        configMap.put("lore", new ArrayList<>(rankConfig.getStringList("lore")));
        configMap.put("enabled", true);
        configMap.put("amount", rankConfig.getInt("amount", 1));
        configMap.put("glow", rankConfig.getBoolean("glow", false) ||
                (entry.playerId.equals(player.getUniqueId()) && entry.rank <= 3));
        configMap.put("hide-attributes", rankConfig.getBoolean("hide-attributes", true));
        configMap.put("hide-enchants", rankConfig.getBoolean("hide-enchants", false));

        ModelDataComponentConfig fallbackModelData = ModelDataComponentConfig.empty();
        if (selectedJob != null) {
            Job selected = plugin.getJobManager().getJob(selectedJob);
            if (selected != null) {
                fallbackModelData = selected.getIconModelData();
            }
        }

        applyModelData(configMap, fallbackModelData, rankConfig);

        if (rankConfig.contains("slot")) {
            entry.preferredSlot = rankConfig.getInt("slot");
        }

        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        ItemStack item = createMenuItem(itemConfig, placeholders);


        if (item != null) {
            itemCache.put(cacheKey, item);
        }

        return item;
    }

    private org.bukkit.configuration.ConfigurationSection getRankConfig(
            org.bukkit.configuration.ConfigurationSection entriesConfig, int rank) {

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
        configMap.put("enabled", true);
        configMap.put("player-head", entry.playerName);

        MenuItemConfig itemConfig = new MenuItemConfig(new SimpleConfigurationSection(configMap));
        return createMenuItem(itemConfig, placeholders);
    }

    private void applyModelData(Map<String, Object> target, ModelDataComponentConfig fallback, ConfigurationSection overrides) {
        ModelDataComponentConfig resolved = resolveModelData(fallback, overrides);
        if (resolved != null && !resolved.isEmpty()) {
            target.putAll(resolved.toConfigurationValues());
        }
    }

    private ModelDataComponentConfig resolveModelData(ModelDataComponentConfig fallback, ConfigurationSection overrides) {
        if (overrides == null) {
            return fallback;
        }

        if (containsPlaceholder(overrides)) {
            return fallback;
        }

        ModelDataComponentConfig overrideConfig = ModelDataComponentConfig.fromSection(overrides);
        return overrideConfig.isEmpty() ? fallback : overrideConfig;
    }

    private boolean containsPlaceholder(ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        String customModelValue = section.getString("custom-model-data");
        if (isPlaceholder(customModelValue)) {
            return true;
        }

        ConfigurationSection component = section.getConfigurationSection("model_data_component");
        if (component != null) {
            for (String value : component.getStringList("strings")) {
                if (isPlaceholder(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPlaceholder(String value) {
        if (value == null) {
            return false;
        }

        String trimmed = value.trim();
        return JOB_MODEL_PLACEHOLDER.equalsIgnoreCase(trimmed)
                || JOB_MODEL_STRINGS_PLACEHOLDER.equalsIgnoreCase(trimmed);
    }

    private Map<String, String> createRankingPlaceholders(RankingEntry entry) {
        Map<String, String> placeholders = new HashMap<>(10);
        placeholders.put("{rank}", String.valueOf(entry.rank));
        placeholders.put("{player}", entry.playerName);
        placeholders.put("{player_name}", entry.playerName);
        placeholders.put("{player_uuid}", entry.playerId.toString());
        placeholders.put("{level}", String.valueOf(entry.level));
        placeholders.put("{xp}", String.valueOf(entry.xp));
        placeholders.put("{rank_color}", getRankColorFromConfig(entry.rank));
        return placeholders;
    }

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

    private void addNavigationItems() {
        Map<String, MenuItemConfig> navItems = config.getNavigationItems();
        MenuItemUtils.addNavigationItems(inventory, navItems, currentPage, hasNextPage(),
                getNavigationPlaceholders(), config -> createMenuItem(config, getNavigationPlaceholders()));
    }

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
        if (handleNavigationClickWithSound(slot)) {
            return;
        }

        ItemStack clickedItem = inventory.getItem(slot);
        if (clickedItem != null) {
            String jobId = NBTItemUtils.getStringNBT(clickedItem, "universe_job_id");
            if (jobId != null && !jobId.equals(selectedJob)) {
                selectedJob = jobId;
                currentPage = 0;
                itemCache.clear();
                refresh();
                return;
            }
        }

        List<Integer> contentSlots = config.getContentSlots();
        if (contentSlots.contains(slot)) {

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

    private Map<String, String> getNavigationPlaceholders() {
        List<RankingEntry> rankings = selectedJob != null ?
                jobRankings.getOrDefault(selectedJob, new ArrayList<>()) : new ArrayList<>();

        Map<String, String> placeholders = MenuItemUtils.createNavigationPlaceholders(
                currentPage, rankings.size(), config.getItemsPerPage());
        placeholders.put("total_players", String.valueOf(rankings.size()));
        placeholders.put("selected_job", selectedJob != null ? selectedJob : "None");
        return placeholders;
    }

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