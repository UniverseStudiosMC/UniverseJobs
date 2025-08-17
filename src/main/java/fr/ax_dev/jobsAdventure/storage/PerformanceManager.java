package fr.ax_dev.jobsAdventure.storage;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.job.PlayerJobData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Performance manager that integrates high-performance storage with the existing JobManager.
 * Provides transparent performance improvements while maintaining API compatibility.
 */
public class PerformanceManager implements Listener {
    
    private final JobsAdventure plugin;
    private final HighPerformanceDataStorage storage;
    private final Map<UUID, PlayerJobData> activePlayerData;
    private final Set<UUID> pendingLoads;
    
    // Performance monitoring
    private long lastPerformanceReport = 0;
    private static final long PERFORMANCE_REPORT_INTERVAL = TimeUnit.MINUTES.toMillis(15);
    
    /**
     * Create a new PerformanceManager.
     * 
     * @param plugin The plugin instance
     */
    public PerformanceManager(JobsAdventure plugin) {
        this.plugin = plugin;
        this.storage = new HighPerformanceDataStorage(plugin);
        this.activePlayerData = new ConcurrentHashMap<>();
        this.pendingLoads = ConcurrentHashMap.newKeySet();
        
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Initialize the performance manager.
     * 
     * @return CompletableFuture that completes when initialization is done
     */
    public CompletableFuture<Void> initialize() {
        plugin.getLogger().info("Initializing high-performance data storage...");
        
        return storage.initializeAsync().thenRun(() -> {
            // Preload data for online players
            preloadOnlinePlayersAsync();
            
            // Start performance monitoring
            startPerformanceMonitoring();
            
            plugin.getLogger().info("Performance manager initialized successfully");
        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize performance manager", throwable);
            return null;
        });
    }
    
    /**
     * Shutdown the performance manager.
     * 
     * @return CompletableFuture that completes when shutdown is done
     */
    public CompletableFuture<Void> shutdown() {
        plugin.getLogger().info("Shutting down performance manager...");
        
        return saveAllPlayerDataAsync().thenCompose(v -> storage.shutdownAsync()).thenRun(() -> {
            activePlayerData.clear();
            pendingLoads.clear();
            plugin.getLogger().info("Performance manager shutdown complete");
        });
    }
    
    /**
     * Get player data with performance optimizations.
     * 
     * @param playerId Player UUID
     * @return PlayerJobData, loaded if necessary
     */
    public PlayerJobData getPlayerData(UUID playerId) {
        PlayerJobData data = activePlayerData.get(playerId);
        if (data != null) {
            return data;
        }
        
        // Check if load is pending
        if (pendingLoads.contains(playerId)) {
            // Return temporary data while loading
            PlayerJobData tempData = createTemporaryPlayerData(playerId);
            activePlayerData.put(playerId, tempData);
            return tempData;
        }
        
        // Load synchronously for immediate access
        try {
            data = storage.loadPlayerDataAsync(playerId).get(5, TimeUnit.SECONDS);
            if (data != null) {
                activePlayerData.put(playerId, data);
                return data;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load player data for " + playerId, e);
        }
        
        // Fallback to new data
        data = createTemporaryPlayerData(playerId);
        activePlayerData.put(playerId, data);
        return data;
    }
    
    /**
     * Save player data asynchronously.
     * 
     * @param playerId Player UUID
     * @param data Player job data
     * @return CompletableFuture that completes when save is done
     */
    public CompletableFuture<Void> savePlayerDataAsync(UUID playerId, PlayerJobData data) {
        activePlayerData.put(playerId, data);
        return storage.savePlayerDataAsync(playerId, data);
    }
    
    /**
     * Save all active player data.
     * 
     * @return CompletableFuture that completes when save is done
     */
    public CompletableFuture<Void> saveAllPlayerDataAsync() {
        if (activePlayerData.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        Map<UUID, PlayerJobData> dataToSave = new HashMap<>(activePlayerData);
        return storage.saveBatchPlayerData(dataToSave);
    }
    
    /**
     * Preload data for specific players.
     * 
     * @param playerIds Set of player UUIDs
     * @return CompletableFuture that completes when preloading is done
     */
    public CompletableFuture<Void> preloadPlayersAsync(Set<UUID> playerIds) {
        Set<UUID> toLoad = playerIds.stream()
            .filter(id -> !activePlayerData.containsKey(id))
            .collect(Collectors.toSet());
        
        if (toLoad.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        pendingLoads.addAll(toLoad);
        
        return storage.loadBatchPlayerData(toLoad).thenAccept(loadedData -> {
            activePlayerData.putAll(loadedData);
            pendingLoads.removeAll(toLoad);
            
            plugin.getLogger().info("Preloaded data for " + loadedData.size() + " players");
        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.WARNING, "Failed to preload some player data", throwable);
            pendingLoads.removeAll(toLoad);
            return null;
        });
    }
    
    /**
     * Unload player data and save if necessary.
     * 
     * @param playerId Player UUID
     */
    public void unloadPlayerData(UUID playerId) {
        PlayerJobData data = activePlayerData.remove(playerId);
        if (data != null) {
            // Save asynchronously before unloading
            storage.savePlayerDataAsync(playerId, data).thenRun(() -> {
                storage.unloadPlayerData(playerId);
            });
        }
    }
    
    /**
     * Get performance statistics.
     * 
     * @return Map containing performance statistics
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("active_players", activePlayerData.size());
        stats.put("pending_loads", pendingLoads.size());
        stats.put("storage_healthy", storage.isHealthy());
        
        // Add storage metrics
        stats.putAll(storage.getPerformanceMetrics());
        
        // Add JVM memory info
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        stats.put("jvm_max_memory_mb", maxMemory / (1024 * 1024));
        stats.put("jvm_total_memory_mb", totalMemory / (1024 * 1024));
        stats.put("jvm_used_memory_mb", usedMemory / (1024 * 1024));
        stats.put("jvm_free_memory_mb", freeMemory / (1024 * 1024));
        stats.put("jvm_memory_usage_percent", (usedMemory * 100.0) / maxMemory);
        
        return stats;
    }
    
    /**
     * Get detailed health information.
     * 
     * @return Map containing health information
     */
    public Map<String, Object> getHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("performance_manager_healthy", true);
        health.put("active_players", activePlayerData.size());
        health.put("pending_loads", pendingLoads.size());
        
        // Add storage health info
        health.putAll(storage.getHealthInfo());
        
        return health;
    }
    
    /**
     * Force garbage collection and cleanup.
     */
    public void performCleanup() {
        // Clean up expired cache entries
        storage.clearCache();
        
        // Clean up inactive player data (not online for more than 1 hour)
        Set<UUID> onlinePlayerIds = Bukkit.getOnlinePlayers().stream()
            .map(Player::getUniqueId)
            .collect(Collectors.toSet());
        
        Set<UUID> toRemove = new HashSet<>();
        for (UUID playerId : activePlayerData.keySet()) {
            if (!onlinePlayerIds.contains(playerId)) {
                toRemove.add(playerId);
            }
        }
        
        // Save and remove inactive players
        if (!toRemove.isEmpty()) {
            Map<UUID, PlayerJobData> toSave = new HashMap<>();
            for (UUID playerId : toRemove) {
                PlayerJobData data = activePlayerData.remove(playerId);
                if (data != null) {
                    toSave.put(playerId, data);
                }
            }
            
            if (!toSave.isEmpty()) {
                storage.saveBatchPlayerData(toSave).thenRun(() -> {
                    plugin.getLogger().info("Cleaned up data for " + toSave.size() + " inactive players");
                });
            }
        }
        
        // Suggest garbage collection
        System.gc();
    }
    
    // Event handlers
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        // Preload player data asynchronously
        preloadPlayersAsync(Set.of(playerId));
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        // Unload player data with small delay to allow for final operations
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            unloadPlayerData(playerId);
        }, 20L); // 1 second delay
    }
    
    // Private methods
    
    private PlayerJobData createTemporaryPlayerData(UUID playerId) {
        PlayerJobData data = new PlayerJobData(playerId);
        data.setJobManager(plugin.getJobManager());
        return data;
    }
    
    private void preloadOnlinePlayersAsync() {
        Set<UUID> onlinePlayerIds = Bukkit.getOnlinePlayers().stream()
            .map(Player::getUniqueId)
            .collect(Collectors.toSet());
        
        if (!onlinePlayerIds.isEmpty()) {
            preloadPlayersAsync(onlinePlayerIds).thenRun(() -> {
                plugin.getLogger().info("Preloaded data for " + onlinePlayerIds.size() + " online players");
            });
        }
    }
    
    private void startPerformanceMonitoring() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - lastPerformanceReport > PERFORMANCE_REPORT_INTERVAL) {
                reportPerformanceStats();
                lastPerformanceReport = currentTime;
            }
            
            // Periodic cleanup every 30 minutes
            if (currentTime % TimeUnit.MINUTES.toMillis(30) < 20000) { // 20 second window
                performCleanup();
            }
            
        }, 20L * 60, 20L * 60); // Every minute
    }
    
    private void reportPerformanceStats() {
        if (!plugin.getConfigManager().isDebugEnabled()) return;
        
        Map<String, Object> stats = getPerformanceStats();
        plugin.getLogger().info("=== PERFORMANCE REPORT ===");
        plugin.getLogger().info("Active players: " + stats.get("active_players"));
        plugin.getLogger().info("Cache hit rate: " + String.format("%.2f%%", stats.get("hit_rate")));
        plugin.getLogger().info("Memory usage: " + String.format("%.1f%%", stats.get("jvm_memory_usage_percent")));
        plugin.getLogger().info("Total reads: " + stats.get("total_reads"));
        plugin.getLogger().info("Total writes: " + stats.get("total_writes"));
        plugin.getLogger().info("Batch operations: " + stats.get("batch_operations"));
        plugin.getLogger().info("=========================");
    }
    
    /**
     * Get the underlying storage instance.
     * 
     * @return The high-performance storage instance
     */
    public HighPerformanceDataStorage getStorage() {
        return storage;
    }
}