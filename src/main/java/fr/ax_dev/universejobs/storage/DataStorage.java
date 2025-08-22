package fr.ax_dev.universejobs.storage;

import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.reward.storage.RewardStorage;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * High-performance data storage abstraction layer.
 * Supports both file-based and database storage with async operations.
 */
public interface DataStorage extends RewardStorage {
    
    /**
     * Initialize the storage system.
     * 
     * @return CompletableFuture that completes when initialization is done
     */
    CompletableFuture<Void> initializeAsync();
    
    /**
     * Shutdown the storage system gracefully.
     * 
     * @return CompletableFuture that completes when shutdown is done
     */
    CompletableFuture<Void> shutdownAsync();
    
    // Player Job Data Operations
    
    /**
     * Save player job data asynchronously.
     * 
     * @param playerId The player UUID
     * @param data The player job data
     * @return CompletableFuture that completes when save is done
     */
    CompletableFuture<Void> savePlayerDataAsync(UUID playerId, PlayerJobData data);
    
    /**
     * Load player job data asynchronously.
     * 
     * @param playerId The player UUID
     * @return CompletableFuture containing the player job data
     */
    CompletableFuture<PlayerJobData> loadPlayerDataAsync(UUID playerId);
    
    /**
     * Save multiple player data entries in a batch operation.
     * 
     * @param playerDataMap Map of player UUIDs to their job data
     * @return CompletableFuture that completes when batch save is done
     */
    CompletableFuture<Void> saveBatchPlayerData(Map<UUID, PlayerJobData> playerDataMap);
    
    /**
     * Load multiple player data entries in a batch operation.
     * 
     * @param playerIds Set of player UUIDs to load
     * @return CompletableFuture containing a map of loaded player data
     */
    CompletableFuture<Map<UUID, PlayerJobData>> loadBatchPlayerData(Set<UUID> playerIds);
    
    // Cache Management
    
    /**
     * Preload data for specific players into cache.
     * 
     * @param playerIds Set of player UUIDs to preload
     * @return CompletableFuture that completes when preloading is done
     */
    CompletableFuture<Void> preloadPlayerData(Set<UUID> playerIds);
    
    /**
     * Evict player data from cache.
     * 
     * @param playerId The player UUID to evict
     */
    void evictFromCache(UUID playerId);
    
    /**
     * Clear all cached data.
     */
    void clearCache();
    
    /**
     * Get cache statistics.
     * 
     * @return Map containing cache statistics
     */
    Map<String, Object> getCacheStats();
    
    // Performance Monitoring
    
    /**
     * Get storage performance metrics.
     * 
     * @return Map containing performance metrics
     */
    Map<String, Object> getPerformanceMetrics();
    
    /**
     * Reset performance metrics.
     */
    void resetPerformanceMetrics();
    
    // Health Check
    
    /**
     * Check if the storage system is healthy.
     * 
     * @return true if storage is healthy
     */
    boolean isHealthy();
    
    /**
     * Get detailed health information.
     * 
     * @return Map containing health information
     */
    Map<String, Object> getHealthInfo();
}