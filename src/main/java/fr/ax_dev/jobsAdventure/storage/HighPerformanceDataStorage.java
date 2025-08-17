package fr.ax_dev.jobsAdventure.storage;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.job.PlayerJobData;
import fr.ax_dev.jobsAdventure.storage.pool.ConnectionPool;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * High-performance data storage implementation with caching, compression,
 * and async operations.
 */
public class HighPerformanceDataStorage implements DataStorage {
    
    private final JobsAdventure plugin;
    private final CacheManager cache;
    private final ConnectionPool connectionPool;
    private final ExecutorService asyncExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    private final File dataFolder;
    private final File rewardDataFolder;
    private final boolean compressionEnabled;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    // Performance metrics
    private final AtomicLong totalReads = new AtomicLong(0);
    private final AtomicLong totalWrites = new AtomicLong(0);
    private final AtomicLong totalReadTime = new AtomicLong(0);
    private final AtomicLong totalWriteTime = new AtomicLong(0);
    private final AtomicLong batchOperations = new AtomicLong(0);
    
    // Dirty tracking for batch writes
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Map<String, Map<String, Long>>> rewardData = new ConcurrentHashMap<>();
    
    /**
     * Create a new HighPerformanceDataStorage.
     * 
     * @param plugin The plugin instance
     */
    public HighPerformanceDataStorage(JobsAdventure plugin) {
        this.plugin = plugin;
        
        FileConfiguration config = plugin.getConfig();
        this.cache = new CacheManager(config);
        this.connectionPool = new ConnectionPool(plugin, config);
        
        // Create thread pools with proper sizing
        int coreThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        int maxThreads = Math.max(4, Runtime.getRuntime().availableProcessors());
        
        this.asyncExecutor = new ThreadPoolExecutor(
            coreThreads, maxThreads, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            r -> new Thread(r, "JobsAdventure-Storage-" + r.hashCode()),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        this.scheduledExecutor = Executors.newScheduledThreadPool(2, 
            r -> new Thread(r, "JobsAdventure-Scheduler-" + r.hashCode()));
        
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        this.rewardDataFolder = new File(plugin.getDataFolder(), "reward-data");
        this.compressionEnabled = config.getBoolean("storage.compression.enabled", true);
        
        // Create directories
        if (!dataFolder.exists()) dataFolder.mkdirs();
        if (!rewardDataFolder.exists()) rewardDataFolder.mkdirs();
    }
    
    @Override
    public CompletableFuture<Void> initializeAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Initialize connection pool if using database
                connectionPool.initialize();
                
                // Initialize database schema if enabled
                if (connectionPool.isEnabled()) {
                    DatabaseInitializer dbInitializer = new DatabaseInitializer(plugin, connectionPool);
                    if (!dbInitializer.initializeDatabase()) {
                        plugin.getLogger().warning("Database initialization failed, falling back to file storage");
                    } else {
                        plugin.getLogger().info("Database schema initialized successfully");
                        
                        // Create performance indexes
                        dbInitializer.createPerformanceIndexes();
                        
                        // Log database info
                        java.util.Map<String, Object> dbInfo = dbInitializer.getDatabaseInfo();
                        plugin.getLogger().info("Database: " + dbInfo.get("database_product") + " " + dbInfo.get("database_version"));
                    }
                }
                
                // Start periodic tasks
                startPeriodicTasks();
                
                initialized.set(true);
                plugin.getLogger().info("High-performance data storage initialized");
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize storage", e);
                throw new RuntimeException(e);
            }
        }, asyncExecutor);
    }
    
    @Override
    public CompletableFuture<Void> shutdownAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                initialized.set(false);
                
                // Save all dirty data
                saveBatchPlayerData(getAllCachedPlayerData()).join();
                save();
                
                // Shutdown executors
                scheduledExecutor.shutdown();
                asyncExecutor.shutdown();
                
                try {
                    if (!asyncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                        asyncExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    asyncExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                
                // Shutdown connection pool
                connectionPool.shutdown();
                
                plugin.getLogger().info("High-performance data storage shutdown complete");
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during storage shutdown", e);
            }
        }, asyncExecutor);
    }
    
    @Override
    public CompletableFuture<Void> savePlayerDataAsync(UUID playerId, PlayerJobData data) {
        return CompletableFuture.runAsync(() -> {
            long startTime = System.nanoTime();
            
            try {
                // Update cache
                cache.put(playerId, data);
                
                // Save to storage
                if (connectionPool.isEnabled()) {
                    saveToDatabaseAsync(playerId, data);
                } else {
                    saveToFileAsync(playerId, data);
                }
                
                // Update metrics
                totalWrites.incrementAndGet();
                totalWriteTime.addAndGet(System.nanoTime() - startTime);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player data: " + playerId, e);
                throw new RuntimeException(e);
            }
        }, asyncExecutor);
    }
    
    @Override
    public CompletableFuture<PlayerJobData> loadPlayerDataAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            
            try {
                // Check cache first
                PlayerJobData cached = cache.get(playerId);
                if (cached != null) {
                    return cached;
                }
                
                PlayerJobData data;
                
                // Load from storage
                if (connectionPool.isEnabled()) {
                    data = loadFromDatabase(playerId);
                } else {
                    data = loadFromFile(playerId);
                }
                
                if (data != null) {
                    // Cache the loaded data
                    cache.put(playerId, data);
                }
                
                // Update metrics
                totalReads.incrementAndGet();
                totalReadTime.addAndGet(System.nanoTime() - startTime);
                
                return data;
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load player data: " + playerId, e);
                return createFallbackPlayerData(playerId);
            }
        }, asyncExecutor);
    }
    
    @Override
    public CompletableFuture<Void> saveBatchPlayerData(Map<UUID, PlayerJobData> playerDataMap) {
        return CompletableFuture.runAsync(() -> {
            long startTime = System.nanoTime();
            
            try {
                batchOperations.incrementAndGet();
                
                if (connectionPool.isEnabled()) {
                    saveBatchToDatabase(playerDataMap);
                } else {
                    saveBatchToFiles(playerDataMap);
                }
                
                // Update cache
                for (Map.Entry<UUID, PlayerJobData> entry : playerDataMap.entrySet()) {
                    cache.put(entry.getKey(), entry.getValue());
                }
                
                // Update metrics
                totalWrites.addAndGet(playerDataMap.size());
                totalWriteTime.addAndGet(System.nanoTime() - startTime);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save batch player data", e);
                throw new RuntimeException(e);
            }
        }, asyncExecutor);
    }
    
    @Override
    public CompletableFuture<Map<UUID, PlayerJobData>> loadBatchPlayerData(Set<UUID> playerIds) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            Map<UUID, PlayerJobData> result = new ConcurrentHashMap<>();
            
            try {
                // Check cache first
                Set<UUID> toLoad = new HashSet<>();
                for (UUID playerId : playerIds) {
                    PlayerJobData cached = cache.get(playerId);
                    if (cached != null) {
                        result.put(playerId, cached);
                    } else {
                        toLoad.add(playerId);
                    }
                }
                
                // Load remaining from storage
                if (!toLoad.isEmpty()) {
                    Map<UUID, PlayerJobData> loaded;
                    if (connectionPool.isEnabled()) {
                        loaded = loadBatchFromDatabase(toLoad);
                    } else {
                        loaded = loadBatchFromFiles(toLoad);
                    }
                    
                    result.putAll(loaded);
                    
                    // Cache loaded data
                    for (Map.Entry<UUID, PlayerJobData> entry : loaded.entrySet()) {
                        cache.put(entry.getKey(), entry.getValue());
                    }
                }
                
                // Update metrics
                totalReads.addAndGet(playerIds.size());
                totalReadTime.addAndGet(System.nanoTime() - startTime);
                
                return result;
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load batch player data", e);
                return result; // Return partial results
            }
        }, asyncExecutor);
    }
    
    @Override
    public CompletableFuture<Void> preloadPlayerData(Set<UUID> playerIds) {
        return loadBatchPlayerData(playerIds).thenApply(data -> null);
    }
    
    @Override
    public void evictFromCache(UUID playerId) {
        cache.remove(playerId);
    }
    
    @Override
    public void clearCache() {
        cache.clear();
    }
    
    @Override
    public Map<String, Object> getCacheStats() {
        return cache.getStats();
    }
    
    @Override
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        long reads = totalReads.get();
        long writes = totalWrites.get();
        
        metrics.put("total_reads", reads);
        metrics.put("total_writes", writes);
        metrics.put("batch_operations", batchOperations.get());
        metrics.put("avg_read_time_ms", reads > 0 ? (totalReadTime.get() / reads) / 1_000_000.0 : 0);
        metrics.put("avg_write_time_ms", writes > 0 ? (totalWriteTime.get() / writes) / 1_000_000.0 : 0);
        metrics.put("compression_enabled", compressionEnabled);
        
        // Add cache metrics
        metrics.putAll(getCacheStats());
        
        // Add compression metrics if enabled
        if (compressionEnabled) {
            metrics.put("compression", DataCompressor.getStats());
        }
        
        return metrics;
    }
    
    @Override
    public void resetPerformanceMetrics() {
        totalReads.set(0);
        totalWrites.set(0);
        totalReadTime.set(0);
        totalWriteTime.set(0);
        batchOperations.set(0);
        
        if (compressionEnabled) {
            DataCompressor.resetStats();
        }
    }
    
    @Override
    public boolean isHealthy() {
        if (!initialized.get()) return false;
        if (asyncExecutor.isShutdown()) return false;
        if (connectionPool.isEnabled() && !connectionPool.isHealthy()) return false;
        
        return true;
    }
    
    @Override
    public Map<String, Object> getHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("initialized", initialized.get());
        health.put("executor_shutdown", asyncExecutor.isShutdown());
        health.put("cache_size", cache.getStats().get("size"));
        health.put("dirty_players", dirtyPlayers.size());
        
        if (connectionPool.isEnabled()) {
            health.put("database_healthy", connectionPool.isHealthy());
            health.put("active_connections", connectionPool.getActiveConnections());
        }
        
        return health;
    }
    
    // File-based storage methods
    
    private void saveToFileAsync(UUID playerId, PlayerJobData data) {
        File dataFile = new File(dataFolder, playerId.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        data.save(config);
        
        try {
            String yamlContent = config.saveToString();
            if (compressionEnabled) {
                yamlContent = DataCompressor.compress(yamlContent);
            }
            
            // Save compressed/raw data
            config.set("_compressed", compressionEnabled);
            config.set("_data", yamlContent);
            config.save(dataFile);
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data file: " + playerId, e);
        }
    }
    
    private PlayerJobData loadFromFile(UUID playerId) {
        File dataFile = new File(dataFolder, playerId.toString() + ".yml");
        if (!dataFile.exists()) {
            return createFallbackPlayerData(playerId);
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            
            String yamlContent;
            boolean compressed = config.getBoolean("_compressed", false);
            
            if (compressed) {
                yamlContent = DataCompressor.decompress(config.getString("_data"));
                // Re-parse decompressed YAML
                config = YamlConfiguration.loadConfiguration(new java.io.StringReader(yamlContent));
            }
            
            PlayerJobData data = new PlayerJobData(playerId);
            data.setJobManager(plugin.getJobManager());
            data.load(config);
            
            return data;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data file: " + playerId, e);
            return createFallbackPlayerData(playerId);
        }
    }
    
    private void saveBatchToFiles(Map<UUID, PlayerJobData> playerDataMap) {
        // Use parallel streams for better performance
        playerDataMap.entrySet().parallelStream().forEach(entry -> {
            saveToFileAsync(entry.getKey(), entry.getValue());
        });
    }
    
    private Map<UUID, PlayerJobData> loadBatchFromFiles(Set<UUID> playerIds) {
        return playerIds.parallelStream()
            .collect(Collectors.toConcurrentMap(
                playerId -> playerId,
                this::loadFromFile
            ));
    }
    
    // Database methods (placeholder for future implementation)
    
    private void saveToDatabaseAsync(UUID playerId, PlayerJobData data) {
        try {
            Connection connection = connectionPool.getConnection();
            try {
                // Convert PlayerJobData to JSON string for storage
                String jsonData = playerDataToJson(data);
                
                // Compress if enabled
                if (compressionEnabled) {
                    jsonData = DataCompressor.compress(jsonData);
                }
                
                // Upsert player data
                String sql = "INSERT INTO player_job_data (player_uuid, data, compressed, last_updated) " +
                           "VALUES (?, ?, ?, ?) " +
                           "ON DUPLICATE KEY UPDATE data = VALUES(data), compressed = VALUES(compressed), last_updated = VALUES(last_updated)";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerId.toString());
                    stmt.setString(2, jsonData);
                    stmt.setBoolean(3, compressionEnabled);
                    stmt.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
                    
                    stmt.executeUpdate();
                }
                
            } finally {
                connectionPool.returnConnection(connection);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Database save failed for " + playerId + ", falling back to file storage", e);
            saveToFileAsync(playerId, data);
        }
    }
    
    private PlayerJobData loadFromDatabase(UUID playerId) {
        try {
            Connection connection = connectionPool.getConnection();
            try {
                String sql = "SELECT data, compressed FROM player_job_data WHERE player_uuid = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerId.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String jsonData = rs.getString("data");
                            boolean compressed = rs.getBoolean("compressed");
                            
                            // Decompress if needed
                            if (compressed) {
                                jsonData = DataCompressor.decompress(jsonData);
                            }
                            
                            // Convert JSON back to PlayerJobData
                            return playerDataFromJson(playerId, jsonData);
                        }
                    }
                }
                
            } finally {
                connectionPool.returnConnection(connection);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Database load failed for " + playerId + ", falling back to file storage", e);
        }
        
        // Fallback to file storage
        return loadFromFile(playerId);
    }
    
    private void saveBatchToDatabase(Map<UUID, PlayerJobData> playerDataMap) {
        if (playerDataMap.isEmpty()) return;
        
        try {
            Connection connection = connectionPool.getConnection();
            try {
                // Start transaction for batch operation
                connection.setAutoCommit(false);
                
                String sql = "INSERT INTO player_job_data (player_uuid, data, compressed, last_updated) " +
                           "VALUES (?, ?, ?, ?) " +
                           "ON DUPLICATE KEY UPDATE data = VALUES(data), compressed = VALUES(compressed), last_updated = VALUES(last_updated)";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    int batchCount = 0;
                    long currentTime = System.currentTimeMillis();
                    
                    for (Map.Entry<UUID, PlayerJobData> entry : playerDataMap.entrySet()) {
                        UUID playerId = entry.getKey();
                        PlayerJobData data = entry.getValue();
                        
                        // Convert to JSON
                        String jsonData = playerDataToJson(data);
                        
                        // Compress if enabled
                        if (compressionEnabled) {
                            jsonData = DataCompressor.compress(jsonData);
                        }
                        
                        stmt.setString(1, playerId.toString());
                        stmt.setString(2, jsonData);
                        stmt.setBoolean(3, compressionEnabled);
                        stmt.setTimestamp(4, new java.sql.Timestamp(currentTime));
                        
                        stmt.addBatch();
                        batchCount++;
                        
                        // Execute batch every 100 records to avoid memory issues
                        if (batchCount % 100 == 0) {
                            stmt.executeBatch();
                            stmt.clearBatch();
                        }
                    }
                    
                    // Execute remaining batch
                    if (batchCount % 100 != 0) {
                        stmt.executeBatch();
                    }
                }
                
                // Commit transaction
                connection.commit();
                
                plugin.getLogger().info("Successfully saved " + playerDataMap.size() + " player records to database");
                
            } catch (Exception e) {
                // Rollback on error
                try {
                    connection.rollback();
                } catch (Exception rollbackError) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to rollback transaction", rollbackError);
                }
                throw e;
                
            } finally {
                // Restore auto-commit
                try {
                    connection.setAutoCommit(true);
                } catch (Exception autoCommitError) {
                    plugin.getLogger().log(Level.WARNING, "Failed to restore auto-commit", autoCommitError);
                }
                connectionPool.returnConnection(connection);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Batch database save failed, falling back to file storage", e);
            saveBatchToFiles(playerDataMap);
        }
    }
    
    private Map<UUID, PlayerJobData> loadBatchFromDatabase(Set<UUID> playerIds) {
        Map<UUID, PlayerJobData> result = new ConcurrentHashMap<>();
        
        if (playerIds.isEmpty()) return result;
        
        try {
            Connection connection = connectionPool.getConnection();
            try {
                // Build IN clause for batch load
                StringBuilder inClause = new StringBuilder();
                for (int i = 0; i < playerIds.size(); i++) {
                    if (i > 0) inClause.append(",");
                    inClause.append("?");
                }
                
                String sql = "SELECT player_uuid, data, compressed FROM player_job_data WHERE player_uuid IN (" + inClause + ")";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    int paramIndex = 1;
                    for (UUID playerId : playerIds) {
                        stmt.setString(paramIndex++, playerId.toString());
                    }
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String playerUuidStr = rs.getString("player_uuid");
                            String jsonData = rs.getString("data");
                            boolean compressed = rs.getBoolean("compressed");
                            
                            try {
                                UUID playerId = UUID.fromString(playerUuidStr);
                                
                                // Decompress if needed
                                if (compressed) {
                                    jsonData = DataCompressor.decompress(jsonData);
                                }
                                
                                // Convert JSON back to PlayerJobData
                                PlayerJobData data = playerDataFromJson(playerId, jsonData);
                                result.put(playerId, data);
                                
                            } catch (Exception e) {
                                plugin.getLogger().log(Level.WARNING, "Failed to parse player data for " + playerUuidStr, e);
                            }
                        }
                    }
                }
                
                plugin.getLogger().info("Successfully loaded " + result.size() + "/" + playerIds.size() + " player records from database");
                
            } finally {
                connectionPool.returnConnection(connection);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Batch database load failed, falling back to file storage", e);
        }
        
        // Load missing players from file storage
        Set<UUID> missingIds = new HashSet<>(playerIds);
        missingIds.removeAll(result.keySet());
        
        if (!missingIds.isEmpty()) {
            Map<UUID, PlayerJobData> fileData = loadBatchFromFiles(missingIds);
            result.putAll(fileData);
        }
        
        return result;
    }
    
    // Utility methods
    
    private PlayerJobData createFallbackPlayerData(UUID playerId) {
        PlayerJobData data = new PlayerJobData(playerId);
        data.setJobManager(plugin.getJobManager());
        return data;
    }
    
    private Map<UUID, PlayerJobData> getAllCachedPlayerData() {
        Set<UUID> cachedPlayers = cache.getCachedPlayers();
        Map<UUID, PlayerJobData> result = new HashMap<>();
        
        for (UUID playerId : cachedPlayers) {
            PlayerJobData data = cache.get(playerId);
            if (data != null) {
                result.put(playerId, data);
            }
        }
        
        return result;
    }
    
    private void startPeriodicTasks() {
        // Auto-save task every 5 minutes
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                if (!dirtyPlayers.isEmpty()) {
                    saveBatchPlayerData(getAllCachedPlayerData());
                    dirtyPlayers.clear();
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error in auto-save task", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
        
        // Cache cleanup task every 30 minutes
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                cache.cleanupExpired(60); // Remove entries older than 1 hour
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error in cache cleanup task", e);
            }
        }, 30, 30, TimeUnit.MINUTES);
    }
    
    // RewardStorage implementation (delegated to existing logic for now)
    
    @Override
    public void initialize() {
        initializeAsync().join();
    }
    
    @Override
    public void shutdown() {
        shutdownAsync().join();
    }
    
    @Override
    public boolean hasClaimedReward(UUID playerId, String jobId, String rewardId) {
        Map<String, Map<String, Long>> jobRewards = rewardData.get(playerId);
        if (jobRewards == null) return false;
        
        Map<String, Long> rewards = jobRewards.get(jobId);
        if (rewards == null) return false;
        
        return rewards.containsKey(rewardId);
    }
    
    @Override
    public void claimReward(UUID playerId, String jobId, String rewardId, long claimTime) {
        rewardData.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                  .computeIfAbsent(jobId, k -> new ConcurrentHashMap<>())
                  .put(rewardId, claimTime);
        dirtyPlayers.add(playerId);
    }
    
    @Override
    public long getClaimTime(UUID playerId, String jobId, String rewardId) {
        Map<String, Map<String, Long>> jobRewards = rewardData.get(playerId);
        if (jobRewards == null) return -1;
        
        Map<String, Long> rewards = jobRewards.get(jobId);
        if (rewards == null) return -1;
        
        return rewards.getOrDefault(rewardId, -1L);
    }
    
    @Override
    public Set<String> getClaimedRewards(UUID playerId, String jobId) {
        Map<String, Map<String, Long>> jobRewards = rewardData.get(playerId);
        if (jobRewards == null) return new HashSet<>();
        
        Map<String, Long> rewards = jobRewards.get(jobId);
        if (rewards == null) return new HashSet<>();
        
        return new HashSet<>(rewards.keySet());
    }
    
    @Override
    public Set<String> getAllClaimedRewards(UUID playerId) {
        Set<String> allRewards = new HashSet<>();
        Map<String, Map<String, Long>> jobRewards = rewardData.get(playerId);
        
        if (jobRewards != null) {
            for (Map.Entry<String, Map<String, Long>> jobEntry : jobRewards.entrySet()) {
                String jobId = jobEntry.getKey();
                for (String rewardId : jobEntry.getValue().keySet()) {
                    allRewards.add(jobId + ":" + rewardId);
                }
            }
        }
        
        return allRewards;
    }
    
    @Override
    public void resetRewardClaim(UUID playerId, String jobId, String rewardId) {
        Map<String, Map<String, Long>> jobRewards = rewardData.get(playerId);
        if (jobRewards == null) return;
        
        Map<String, Long> rewards = jobRewards.get(jobId);
        if (rewards == null) return;
        
        if (rewards.remove(rewardId) != null) {
            dirtyPlayers.add(playerId);
        }
    }
    
    @Override
    public void resetJobRewards(UUID playerId, String jobId) {
        Map<String, Map<String, Long>> jobRewards = rewardData.get(playerId);
        if (jobRewards == null) return;
        
        if (jobRewards.remove(jobId) != null) {
            dirtyPlayers.add(playerId);
        }
    }
    
    @Override
    public void resetAllRewards(UUID playerId) {
        if (rewardData.remove(playerId) != null) {
            dirtyPlayers.add(playerId);
        }
    }
    
    @Override
    public void save() {
        // Save reward data
        saveBatchPlayerData(getAllCachedPlayerData()).join();
    }
    
    @Override
    public void loadPlayerData(UUID playerId) {
        loadPlayerDataAsync(playerId).join();
    }
    
    @Override
    public void unloadPlayerData(UUID playerId) {
        // Save if dirty, then evict from cache
        if (dirtyPlayers.contains(playerId)) {
            PlayerJobData data = cache.get(playerId);
            if (data != null) {
                savePlayerDataAsync(playerId, data).join();
            }
            dirtyPlayers.remove(playerId);
        }
        
        evictFromCache(playerId);
        rewardData.remove(playerId);
    }
    
    // JSON Conversion Methods
    
    /**
     * Convert PlayerJobData to JSON string for database storage.
     * 
     * @param data The player job data
     * @return JSON representation
     */
    private String playerDataToJson(PlayerJobData data) {
        try {
            // Create a simple JSON structure using StringBuilder for better performance
            StringBuilder json = new StringBuilder();
            json.append("{");
            
            // Add player UUID
            json.append("\"player_uuid\":\"").append(data.getPlayerUuid()).append("\",");
            
            // Add jobs
            json.append("\"jobs\":[");
            Set<String> jobs = data.getJobs();
            boolean firstJob = true;
            for (String jobId : jobs) {
                if (!firstJob) json.append(",");
                json.append("\"").append(jobId).append("\"");
                firstJob = false;
            }
            json.append("],");
            
            // Add XP data
            json.append("\"xp\":{");
            boolean firstXp = true;
            for (String jobId : jobs) {
                if (!firstXp) json.append(",");
                double xp = data.getXp(jobId);
                json.append("\"").append(jobId).append("\":").append(xp);
                firstXp = false;
            }
            json.append("},");
            
            // Add level data
            json.append("\"levels\":{");
            boolean firstLevel = true;
            for (String jobId : jobs) {
                if (!firstLevel) json.append(",");
                int level = data.getLevel(jobId);
                json.append("\"").append(jobId).append("\":").append(level);
                firstLevel = false;
            }
            json.append("},");
            
            // Add timestamp
            json.append("\"last_updated\":").append(System.currentTimeMillis());
            
            json.append("}");
            return json.toString();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to convert player data to JSON", e);
            throw new RuntimeException("JSON conversion failed", e);
        }
    }
    
    /**
     * Convert JSON string back to PlayerJobData.
     * 
     * @param playerId The player UUID
     * @param jsonData The JSON data
     * @return PlayerJobData object
     */
    private PlayerJobData playerDataFromJson(UUID playerId, String jsonData) {
        try {
            PlayerJobData data = new PlayerJobData(playerId);
            data.setJobManager(plugin.getJobManager());
            
            // Simple JSON parsing using string operations for better performance
            // Note: In production, consider using a proper JSON library like Gson or Jackson
            
            // Parse jobs array
            String jobsStart = "\"jobs\":[";
            String jobsEnd = "]";
            int jobsStartIndex = jsonData.indexOf(jobsStart);
            if (jobsStartIndex != -1) {
                int jobsEndIndex = jsonData.indexOf(jobsEnd, jobsStartIndex);
                if (jobsEndIndex != -1) {
                    String jobsJson = jsonData.substring(jobsStartIndex + jobsStart.length(), jobsEndIndex);
                    if (!jobsJson.trim().isEmpty()) {
                        String[] jobs = jobsJson.replace("\"", "").split(",");
                        for (String job : jobs) {
                            if (!job.trim().isEmpty()) {
                                data.joinJob(job.trim());
                            }
                        }
                    }
                }
            }
            
            // Parse XP data
            String xpStart = "\"xp\":{";
            String xpEnd = "}";
            int xpStartIndex = jsonData.indexOf(xpStart);
            if (xpStartIndex != -1) {
                int xpEndIndex = jsonData.indexOf(xpEnd, xpStartIndex);
                if (xpEndIndex != -1) {
                    String xpJson = jsonData.substring(xpStartIndex + xpStart.length(), xpEndIndex);
                    parseXpData(data, xpJson);
                }
            }
            
            // Parse level data
            String levelsStart = "\"levels\":{";
            String levelsEnd = "}";
            int levelsStartIndex = jsonData.indexOf(levelsStart);
            if (levelsStartIndex != -1) {
                int levelsEndIndex = jsonData.indexOf(levelsEnd, levelsStartIndex);
                if (levelsEndIndex != -1) {
                    String levelsJson = jsonData.substring(levelsStartIndex + levelsStart.length(), levelsEndIndex);
                    parseLevelData(data, levelsJson);
                }
            }
            
            return data;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to convert JSON to player data", e);
            return createFallbackPlayerData(playerId);
        }
    }
    
    /**
     * Parse XP data from JSON string.
     * 
     * @param data The player job data to update
     * @param xpJson The XP JSON string
     */
    private void parseXpData(PlayerJobData data, String xpJson) {
        if (xpJson.trim().isEmpty()) return;
        
        try {
            String[] xpEntries = xpJson.split(",");
            for (String entry : xpEntries) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    String jobId = parts[0].replace("\"", "").trim();
                    double xp = Double.parseDouble(parts[1].trim());
                    
                    // Set XP directly (bypass addXp to avoid duplicates)
                    data.setXp(jobId, xp);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse XP data: " + xpJson, e);
        }
    }
    
    /**
     * Parse level data from JSON string.
     * 
     * @param data The player job data to update
     * @param levelsJson The levels JSON string
     */
    private void parseLevelData(PlayerJobData data, String levelsJson) {
        if (levelsJson.trim().isEmpty()) return;
        
        try {
            String[] levelEntries = levelsJson.split(",");
            for (String entry : levelEntries) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    String jobId = parts[0].replace("\"", "").trim();
                    int level = Integer.parseInt(parts[1].trim());
                    
                    // Set level directly (if PlayerJobData supports it)
                    data.setLevel(jobId, level);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse level data: " + levelsJson, e);
        }
    }
}