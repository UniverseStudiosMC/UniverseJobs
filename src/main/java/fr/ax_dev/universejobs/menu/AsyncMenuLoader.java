package fr.ax_dev.universejobs.menu;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.menu.config.MenuItemConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AsyncMenuLoader {

    private final UniverseJobs plugin;
    private final ExecutorService executor;
    private final Map<String, PreparedMenuData> menuCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> playerPermissionCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 60000; // 1 minute

    public AsyncMenuLoader(UniverseJobs plugin) {
        this.plugin = plugin;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setName("UniverseJobs-MenuLoader");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
    }

    public void preloadPlayerData(Player player) {
        CompletableFuture.runAsync(() -> {
            UUID playerId = player.getUniqueId();
            Map<String, Integer> permissions = new ConcurrentHashMap<>();

            // Pre-fetch all job max levels
            for (Job job : plugin.getJobManager().getJobs().values()) {
                PlayerJobData playerData = plugin.getJobManager().getPlayerData(playerId);
                int maxLevel = playerData.getMaxLevel(job.getId());
                permissions.put(job.getId(), maxLevel);
            }

            playerPermissionCache.put(playerId, permissions);
        }, executor);
    }

    /**
     * Preload all data needed for JobsMainMenu async.
     */
    public JobsMainMenuData preloadJobsMainMenuData(Player player) {
        UUID playerId = player.getUniqueId();
        JobsMainMenuData data = new JobsMainMenuData();

        // Load player data
        PlayerJobData playerData = plugin.getJobManager().getPlayerData(playerId);
        data.playerData = playerData;

        // Load all jobs with their cached placeholders
        data.jobPlaceholders = new ConcurrentHashMap<>();
        for (Job job : plugin.getJobManager().getJobs().values()) {
            if (job.isEnabled()) {
                Map<String, String> placeholders = createJobPlaceholdersAsync(job, playerData);
                data.jobPlaceholders.put(job.getId(), placeholders);
            }
        }

        return data;
    }

    /**
     * Create menu items async for JobsMainMenu using existing configuration.
     */
    public Map<String, ItemStack> createJobsMainMenuItems(Player player, JobsMainMenuData data) {
        // This method should be called sync actually, since we need access to existing config
        // Return empty map to force fallback to normal creation
        return new ConcurrentHashMap<>();
    }

    /**
     * Create job placeholders async-safe.
     */
    private Map<String, String> createJobPlaceholdersAsync(Job job, PlayerJobData playerData) {
        Map<String, String> placeholders = new ConcurrentHashMap<>();

        // Basic job info
        placeholders.put("{job_id}", job.getId());
        placeholders.put("{job_name}", job.getName());
        placeholders.put("{job_description}", job.getDescription());
        placeholders.put("{job_description_lines}", String.join("\n", job.getDescriptionLines()));

        // Player-specific data
        boolean hasJob = playerData.hasJob(job.getId());
        placeholders.put("{has_job}", String.valueOf(hasJob));

        if (hasJob) {
            int level = playerData.getLevel(job.getId());
            double xp = playerData.getXp(job.getId());
            int maxLevel = playerData.getMaxLevel(job.getId());

            placeholders.put("{level}", String.valueOf(level));
            placeholders.put("{xp}", String.valueOf((long) xp));
            placeholders.put("{max_level}", String.valueOf(maxLevel));

            // Calculate progress
            if (level < maxLevel && job.getXpCurve() != null) {
                long nextLevelXp = (long) job.getXpCurve().getXpForLevel(level + 1);
                long xpToNext = Math.max(0, nextLevelXp - (long) xp);
                double progress = Math.min(1.0, xp / nextLevelXp);

                placeholders.put("{xp_to_next}", String.valueOf(xpToNext));
                placeholders.put("{next_level_xp}", String.valueOf(nextLevelXp));
                placeholders.put("{progress_percent}", String.format("%.1f", progress * 100));
            }
        } else {
            placeholders.put("{level}", "0");
            placeholders.put("{xp}", "0");
            placeholders.put("{max_level}", String.valueOf(playerData.getMaxLevel(job.getId())));
            placeholders.put("{xp_to_next}", "0");
            placeholders.put("{next_level_xp}", "0");
            placeholders.put("{progress_percent}", "0.0");
        }

        return placeholders;
    }
    /**
     * Data container for preloaded JobsMainMenu data.
     */
    public static class JobsMainMenuData {
        public PlayerJobData playerData;
        public Map<String, Map<String, String>> jobPlaceholders;
    }

    public CompletableFuture<PreparedMenuData> prepareJobsMenu(Player player) {
        String cacheKey = "jobs_" + player.getUniqueId();
        PreparedMenuData cached = menuCache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            PreparedMenuData data = new PreparedMenuData();

            // Prepare all job placeholders async
            for (Job job : plugin.getJobManager().getJobs().values()) {
                Map<String, String> placeholders = createJobPlaceholdersAsync(player, job);
                data.addPlaceholders(job.getId(), placeholders);
            }

            menuCache.put(cacheKey, data);
            return data;
        }, executor);
    }

    public CompletableFuture<PreparedMenuData> prepareJobActionsMenu(Player player, String jobId) {
        String cacheKey = "actions_" + player.getUniqueId() + "_" + jobId;
        PreparedMenuData cached = menuCache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            PreparedMenuData data = new PreparedMenuData();
            Job job = plugin.getJobManager().getJob(jobId);

            if (job == null) return data;

            // Pre-process all actions async
            job.getActionTypes().forEach(actionType -> {
                job.getActions(actionType).forEach(action -> {
                    String key = actionType.name() + "_" + action.getTarget();
                    Map<String, String> placeholders = Map.of(
                        "action_type", actionType.toString(),
                        "target", action.getTarget(),
                        "xp", String.valueOf(action.getXp()),
                        "money", String.valueOf(action.getMoney())
                    );
                    data.addPlaceholders(key, placeholders);
                });
            });

            menuCache.put(cacheKey, data);
            return data;
        }, executor);
    }

    private Map<String, String> createJobPlaceholdersAsync(Player player, Job job) {
        PlayerJobData playerData = plugin.getJobManager().getPlayerData(player.getUniqueId());

        Map<String, String> placeholders = new ConcurrentHashMap<>();
        placeholders.put("{job_id}", job.getId());
        placeholders.put("{job_name}", job.getDisplayName());
        placeholders.put("{job_description}", job.getDescription());

        boolean hasJob = playerData.hasJob(job.getId());
        placeholders.put("{has_job}", String.valueOf(hasJob));

        if (hasJob) {
            placeholders.put("{level}", String.valueOf(playerData.getLevel(job.getId())));
            placeholders.put("{xp}", String.valueOf(Math.round(playerData.getXp(job.getId()))));

            double[] xpProgress = playerData.getXpProgress(job.getId());
            placeholders.put("{xp_current}", String.valueOf(Math.round(xpProgress[0])));
            placeholders.put("{xp_required}", String.valueOf(Math.round(xpProgress[1])));
            placeholders.put("{xp_percent}", String.valueOf(Math.round(xpProgress[0] / xpProgress[1] * 100)));
        }

        // Use cached permission if available
        Map<String, Integer> perms = playerPermissionCache.get(player.getUniqueId());
        if (perms != null && perms.containsKey(job.getId())) {
            placeholders.put("{max_level}", String.valueOf(perms.get(job.getId())));
        } else {
            placeholders.put("{max_level}", String.valueOf(playerData.getMaxLevel(job.getId())));
        }

        return placeholders;
    }

    public void clearCache(UUID playerId) {
        menuCache.entrySet().removeIf(entry -> entry.getKey().contains(playerId.toString()));
        playerPermissionCache.remove(playerId);
    }

    public void shutdown() {
        executor.shutdown();
        menuCache.clear();
        playerPermissionCache.clear();
    }

    public static class PreparedMenuData {
        private final Map<String, ItemStack> items = new ConcurrentHashMap<>();
        private final Map<String, Map<String, String>> placeholders = new ConcurrentHashMap<>();
        private final long createdAt = System.currentTimeMillis();

        public void addItem(String key, ItemStack item) {
            items.put(key, item);
        }

        public ItemStack getItem(String key) {
            return items.get(key);
        }

        public Map<String, ItemStack> getAllItems() {
            return new ConcurrentHashMap<>(items);
        }

        public void addPlaceholders(String key, Map<String, String> placeholderMap) {
            placeholders.put(key, placeholderMap);
        }

        public Map<String, String> getPlaceholders(String key) {
            return placeholders.getOrDefault(key, new ConcurrentHashMap<>());
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > CACHE_DURATION;
        }
    }
}