package fr.ax_dev.universejobs.job;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionLimitManager;
import fr.ax_dev.universejobs.config.ConfigManager;
import fr.ax_dev.universejobs.xp.XpCurve;
import fr.ax_dev.universejobs.xp.XpCurveManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.lang.ref.WeakReference;

/**
 * Manages all jobs and player job data.
 * Handles job loading, player job assignments, and data persistence.
 */
public class JobManager {
    
    private final UniverseJobs plugin;
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerJobData> playerData = new ConcurrentHashMap<>();
    private final File jobsFolder;
    private final File dataFolder;
    private XpCurveManager xpCurveManager;
    
    // Thread safety and resource management
    private final ReadWriteLock dataLock = new ReentrantReadWriteLock();
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private final Set<WeakReference<PlayerJobData>> trackedPlayerData = ConcurrentHashMap.newKeySet();
    
    // Memory management
    private volatile long lastCleanupTime = System.currentTimeMillis();
    private static final long CLEANUP_INTERVAL = 300000L; // 5 minutes
    private static final int MAX_CACHED_PLAYERS = 1000;
    
    /**
     * Create a new JobManager instance.
     * 
     * @param plugin The plugin instance
     */
    public JobManager(UniverseJobs plugin) {
        this.plugin = plugin;
        this.jobsFolder = new File(plugin.getDataFolder(), "jobs");
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        
        // Create directories if they don't exist
        if (!jobsFolder.exists()) {
            jobsFolder.mkdirs();
        }
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Initialize XP curve manager
        this.xpCurveManager = new XpCurveManager(plugin);
        
        // Performance manager removed as it was not needed
    }
    
    /**
     * Load all jobs from the jobs folder.
     */
    public void loadJobs() {
        jobs.clear();
        
        if (!jobsFolder.exists()) {
            plugin.getLogger().warning("Jobs folder does not exist, creating it...");
            jobsFolder.mkdirs();
        }
        
        File[] jobFiles = jobsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (jobFiles == null || jobFiles.length == 0) {
            plugin.getLogger().warning("No job files found in " + jobsFolder.getPath());
            
            // Only create example job if configured to do so (default: true for first startup)
            boolean createExamples = plugin.getConfig().getBoolean("create-example-jobs", true);
            if (createExamples) {
                createExampleJobs();
                // Set the config to false after first creation to prevent re-creation on reload
                plugin.getConfig().set("create-example-jobs", false);
                plugin.saveConfig();
                
                // Reload after creating example jobs
                jobFiles = jobsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            }
            
            if (jobFiles == null || jobFiles.length == 0) {
                plugin.getLogger().info("UniverseJobs started with no jobs. Add .yml files to " + jobsFolder.getPath() + " to create jobs.");
                return;
            }
        }
        
        for (File jobFile : jobFiles) {
            try {
                String jobId = jobFile.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(jobFile);
                
                Job job = new Job(jobId, config);
                if (job.isEnabledInConfig()) {
                    // Set XP curve for the job
                    setupJobXpCurve(job);
                    
                    if (job.isEnabled()) {
                        jobs.put(jobId, job);
                        
                        // Configure auto-restore for action limits if enabled
                        if (job.isAutoRestoreEnabled()) {
                            ActionLimitManager limitManager = plugin.getLimitManager();
                            limitManager.setAutoRestoreConfig(jobId, true, job.getAutoRestoreTime());
                            plugin.getLogger().info("Auto-restore enabled for job '" + jobId + "' at " + job.getAutoRestoreTime());
                        }
                    } else {
                        plugin.getLogger().severe("Job " + jobId + " is disabled due to XP curve error: " + job.getXpCurveErrorMessage());
                    }
                } else {
                    // Skipped disabled job
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load job file: " + jobFile.getName(), e);
            }
        }
        
        // Jobs loaded successfully
    }
    
    /**
     * Get a job by its ID.
     * 
     * @param jobId The job ID
     * @return The job, or null if not found
     */
    public Job getJob(String jobId) {
        return jobs.get(jobId);
    }
    
    /**
     * Get all loaded jobs.
     * 
     * @return Collection of all jobs
     */
    public Collection<Job> getAllJobs() {
        return new ArrayList<>(jobs.values());
    }
    
    /**
     * Get all enabled jobs.
     * 
     * @return Collection of enabled jobs
     */
    public Collection<Job> getEnabledJobs() {
        return jobs.values().stream()
                .filter(Job::isEnabled)
                .toList();
    }
    
    /**
     * Check if a job exists.
     * 
     * @param jobId The job ID
     * @return true if the job exists
     */
    public boolean hasJob(String jobId) {
        return jobs.containsKey(jobId);
    }
    
    /**
     * Get player job data.
     * 
     * @param player The player
     * @return Player job data
     */
    public PlayerJobData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    /**
     * Get player job data by UUID.
     * 
     * @param playerUuid The player UUID
     * @return Player job data
     */
    public PlayerJobData getPlayerData(UUID playerUuid) {
        if (isShutdown.get()) {
            throw new IllegalStateException("JobManager is shutdown");
        }
        
        // Check if cleanup is needed
        checkAndPerformCleanup();
        
        // Performance manager removed - using standard data access only
        
        // Fallback to original implementation with thread safety
        dataLock.readLock().lock();
        try {
            PlayerJobData existingData = playerData.get(playerUuid);
            if (existingData != null) {
                return existingData;
            }
        } finally {
            dataLock.readLock().unlock();
        }
        
        // Need to create new data
        dataLock.writeLock().lock();
        try {
            // Double-check in case another thread created it
            PlayerJobData existingData = playerData.get(playerUuid);
            if (existingData != null) {
                return existingData;
            }
            
            PlayerJobData data = new PlayerJobData(playerUuid);
            data.setJobManager(this); // Set JobManager reference for XP curve calculations
            playerData.put(playerUuid, data);
            trackPlayerData(data);
            return data;
        } finally {
            dataLock.writeLock().unlock();
        }
    }
    
    /**
     * Make a player join a job.
     * 
     * @param player The player
     * @param jobId The job ID
     * @return true if successful
     */
    public boolean joinJob(Player player, String jobId) {
        Job job = getJob(jobId);
        if (job == null || !job.isEnabled()) {
            return false;
        }
        
        // Check permission
        if (job.getPermission() != null && !player.hasPermission(job.getPermission())) {
            return false;
        }
        
        return joinJob(player.getUniqueId(), jobId);
    }
    
    /**
     * Make a player leave a job.
     * 
     * @param player The player
     * @param jobId The job ID
     * @return true if successful
     */
    public boolean leaveJob(Player player, String jobId) {
        return leaveJob(player.getUniqueId(), jobId);
    }
    
    /**
     * Make a player join a job by UUID.
     * 
     * @param playerUuid The player UUID
     * @param jobId The job ID
     * @return true if successful
     */
    public boolean joinJob(UUID playerUuid, String jobId) {
        if (isShutdown.get()) {
            return false;
        }
        
        Job job = getJob(jobId);
        if (job == null || !job.isEnabled()) {
            return false;
        }
        
        PlayerJobData data = getPlayerData(playerUuid);
        synchronized (data) {
            return data.joinJob(jobId);
        }
    }
    
    /**
     * Make a player leave a job by UUID.
     * 
     * @param playerUuid The player UUID
     * @param jobId The job ID
     * @return true if successful
     */
    public boolean leaveJob(UUID playerUuid, String jobId) {
        if (isShutdown.get()) {
            return false;
        }
        
        // Check if this is a default job that cannot be left
        if (plugin.getConfigManager().isDefaultJob(jobId)) {
            return false; // Cannot leave default jobs
        }
        
        PlayerJobData data = getPlayerData(playerUuid);
        synchronized (data) {
            return data.leaveJob(jobId);
        }
    }
    
    /**
     * Check if a player has a specific job.
     * 
     * @param player The player
     * @param jobId The job ID
     * @return true if the player has the job
     */
    public boolean hasJob(Player player, String jobId) {
        PlayerJobData data = getPlayerData(player);
        return data.hasJob(jobId);
    }
    
    /**
     * Get all jobs a player has.
     * 
     * @param player The player
     * @return Set of job IDs
     */
    public Set<String> getPlayerJobs(Player player) {
        PlayerJobData data = getPlayerData(player);
        return data.getJobs();
    }
    
    /**
     * Get all jobs as a map.
     * 
     * @return Map of all jobs (jobId -> Job)
     */
    public Map<String, Job> getJobs() {
        return new HashMap<>(jobs);
    }
    
    /**
     * Get all player data.
     * 
     * @return Map of all player data (UUID -> PlayerJobData)
     */
    public Map<UUID, PlayerJobData> getAllPlayerData() {
        dataLock.readLock().lock();
        try {
            return new HashMap<>(playerData);
        } finally {
            dataLock.readLock().unlock();
        }
    }
    
    /**
     * Add XP to a player's job.
     * 
     * @param player The player
     * @param jobId The job ID
     * @param xp The XP amount
     */
    public void addXp(Player player, String jobId, double xp) {
        if (isShutdown.get()) {
            return;
        }
        
        // Validate XP amount to prevent exploitation
        if (Double.isNaN(xp) || Double.isInfinite(xp) || xp < 0 || xp > 1000000) {
            plugin.getLogger().warning("Invalid XP amount attempted for player " + player.getName() + ": " + xp);
            return;
        }
        
        PlayerJobData data = getPlayerData(player);
        synchronized (data) {
            data.addXp(jobId, xp);
        }
    }
    
    /**
     * Get a player's XP in a job.
     * 
     * @param player The player
     * @param jobId The job ID
     * @return The XP amount
     */
    public double getXp(Player player, String jobId) {
        PlayerJobData data = getPlayerData(player);
        return data.getXp(jobId);
    }
    
    /**
     * Get a player's level in a job.
     * 
     * @param player The player
     * @param jobId The job ID
     * @return The level
     */
    public int getLevel(Player player, String jobId) {
        PlayerJobData data = getPlayerData(player);
        Job job = getJob(jobId);
        if (job != null && job.getXpCurve() != null) {
            double xp = data.getXp(jobId);
            return job.getXpCurve().getLevelForXp(xp, job.getMaxLevel());
        }
        return data.getLevel(jobId);
    }
    
    /**
     * Get XP required for a specific level in a job.
     * 
     * @param jobId The job ID
     * @param level The level
     * @return The XP required
     */
    public double getXpRequiredForLevel(String jobId, int level) {
        Job job = getJob(jobId);
        if (job != null && job.getXpCurve() != null) {
            return job.getXpCurve().getXpForLevel(level);
        }
        // Fallback to simple calculation
        return level * 1000.0;
    }
    
    /**
     * Get XP required to reach the next level.
     * 
     * @param player The player
     * @param jobId The job ID
     * @return The XP required for next level
     */
    public double getXpToNextLevel(Player player, String jobId) {
        Job job = getJob(jobId);
        if (job != null && job.getXpCurve() != null) {
            int currentLevel = getLevel(player, jobId);
            double currentXp = getXp(player, jobId);
            double nextLevelXp = job.getXpCurve().getXpForLevel(currentLevel + 1);
            return Math.max(0, nextLevelXp - currentXp);
        }
        return 1000.0; // Fallback
    }
    
    /**
     * Save player data to file.
     * 
     * @param player The player
     */
    public void savePlayerData(Player player) {
        savePlayerData(player.getUniqueId());
    }
    
    /**
     * Save player data to file by UUID.
     * 
     * @param playerUuid The player UUID
     */
    public void savePlayerData(UUID playerUuid) {
        if (isShutdown.get()) {
            plugin.getLogger().warning("Attempted to save player data after JobManager shutdown: " + playerUuid);
            return;
        }
        
        PlayerJobData data;
        dataLock.readLock().lock();
        try {
            data = playerData.get(playerUuid);
        } finally {
            dataLock.readLock().unlock();
        }
        
        if (data == null) {
            return;
        }
        
        // Performance manager removed - using standard sync implementation
        savePlayerDataInternal(playerUuid, data);
    }
    
    /**
     * Load player data from file.
     * 
     * @param player The player
     */
    public void loadPlayerData(Player player) {
        loadPlayerData(player.getUniqueId());
    }
    
    /**
     * Load player data from file by UUID.
     * 
     * @param playerUuid The player UUID
     */
    public void loadPlayerData(UUID playerUuid) {
        if (isShutdown.get()) {
            plugin.getLogger().warning("Attempted to load player data after JobManager shutdown: " + playerUuid);
            return;
        }
        
        // Check if data is already loaded
        dataLock.readLock().lock();
        try {
            if (playerData.containsKey(playerUuid)) {
                return; // Already loaded
            }
        } finally {
            dataLock.readLock().unlock();
        }
        
        try {
            File dataFile = new File(dataFolder, playerUuid.toString() + ".yml");
            PlayerJobData data;
            
            if (!dataFile.exists()) {
                // Create new player data
                data = new PlayerJobData(playerUuid);
                data.setJobManager(this);
            } else {
                FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
                data = new PlayerJobData(playerUuid);
                data.setJobManager(this); // Set JobManager reference for XP curve calculations
                data.load(config);
            }
            
            // Auto-assign default jobs
            assignDefaultJobs(data);
            
            // Store with thread safety
            dataLock.writeLock().lock();
            try {
                playerData.put(playerUuid, data);
                trackPlayerData(data);
            } finally {
                dataLock.writeLock().unlock();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + playerUuid, e);
            // Create new player data as fallback
            PlayerJobData fallbackData = new PlayerJobData(playerUuid);
            fallbackData.setJobManager(this);
            assignDefaultJobs(fallbackData);
            
            dataLock.writeLock().lock();
            try {
                playerData.put(playerUuid, fallbackData);
                trackPlayerData(fallbackData);
            } finally {
                dataLock.writeLock().unlock();
            }
        }
    }
    
    /**
     * Assign default jobs to a player based on configuration.
     * 
     * @param data The player job data
     */
    private void assignDefaultJobs(PlayerJobData data) {
        ConfigManager config = plugin.getConfigManager();
        
        if (config.isAllJobsByDefault()) {
            // Assign all available jobs
            for (Job job : jobs.values()) {
                if (job.isEnabled() && !data.hasJob(job.getId())) {
                    data.joinJob(job.getId());
                }
            }
        } else {
            // Assign specific default jobs
            List<String> defaultJobs = config.getJobsByDefault();
            for (String jobId : defaultJobs) {
                Job job = getJob(jobId);
                if (job != null && job.isEnabled() && !data.hasJob(jobId)) {
                    data.joinJob(jobId);
                }
            }
        }
    }
    
    /**
     * Create example job files.
     */
    private void createExampleJobs() {
        // Creating example job files
        try {
            // Save example.yml (the only job file that exists in resources)
            plugin.saveResource("jobs/example.yml", false);
            plugin.getLogger().info("Created example job file: example.yml");
        } catch (IllegalArgumentException e) {
            // Handle case where example.yml doesn't exist in resources
            plugin.getLogger().warning("Could not create example job file: " + e.getMessage());
            plugin.getLogger().info("No example jobs will be created. You can create your own job files manually.");
        }
    }
    
    /**
     * Save all player data.
     */
    public void saveAllPlayerData() {
        // Performance manager removed - using standard implementation
        for (UUID playerUuid : playerData.keySet()) {
            savePlayerData(playerUuid);
        }
    }
    
    /**
     * Reload all jobs.
     */
    public void reloadJobs() {
        // Reload XP curves first
        xpCurveManager.reload();
        loadJobs();
    }
    
    /**
     * Setup XP curve for a job based on its configuration.
     * 
     * @param job The job to setup
     */
    private void setupJobXpCurve(Job job) {
        try {
            XpCurve curve;
            
            if (job.getXpEquation() != null) {
                // Use equation-based curve
                curve = xpCurveManager.getCurveFromEquation(job.getXpEquation());
                // Job using XP equation
            } else if (job.getXpCurveName() != null) {
                // Use file-based curve
                curve = xpCurveManager.getCurve(job.getXpCurveName());
                // Job using XP curve
            } else {
                // Use default curve
                curve = xpCurveManager.getDefaultCurve();
                // Job using default XP curve
            }
            
            job.setXpCurve(curve);
            
            // Test the curve with sample calculations
            try {
                double testXp1 = curve.getXpForLevel(1);
                double testXp10 = curve.getXpForLevel(10);
                double testXp50 = curve.getXpForLevel(50);
                
                if (Double.isNaN(testXp1) || Double.isInfinite(testXp1) ||
                    Double.isNaN(testXp10) || Double.isInfinite(testXp10) ||
                    Double.isNaN(testXp50) || Double.isInfinite(testXp50)) {
                    throw new RuntimeException("XP curve returned invalid values (NaN or Infinite)");
                }
                
                if (testXp1 < 0 || testXp10 < 0 || testXp50 < 0) {
                    throw new RuntimeException("XP curve returned negative values");
                }
                
                // XP curve validated successfully
                
            } catch (Exception e) {
                throw new RuntimeException("XP curve validation failed: " + e.getMessage(), e);
            }
            
        } catch (Exception e) {
            String errorMessage = "XP curve setup failed: " + e.getMessage();
            job.setXpCurveError(true, errorMessage);
            plugin.getLogger().severe("Job " + job.getId() + " disabled - " + errorMessage);
            
            if (job.getXpEquation() != null) {
                plugin.getLogger().severe("Check your XP equation syntax: " + job.getXpEquation());
                plugin.getLogger().severe("Supported functions: Math.pow, Math.sqrt, Math.floor, Math.ceil");
                plugin.getLogger().severe("Example: 100 * Math.pow(level, 2)");
            } else if (job.getXpCurveName() != null) {
                plugin.getLogger().severe("Check if XP curve file exists: xp-curves/" + job.getXpCurveName() + ".yml");
            }
        }
    }
    
    /**
     * Get the XP curve manager.
     * 
     * @return The XP curve manager
     */
    public XpCurveManager getXpCurveManager() {
        return xpCurveManager;
    }
    
    /**
     * Get the performance manager.
     * 
     * @return The performance manager
     */
    public Object getPerformanceManager() {
        return null; // Performance manager feature removed
    }
    
    /**
     * Initialize the performance manager asynchronously.
     * 
     * @return CompletableFuture that completes when initialization is done
     */
    public java.util.concurrent.CompletableFuture<Void> initializePerformanceManager() {
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }
    
    /**
     * Shutdown the performance manager gracefully.
     * 
     * @return CompletableFuture that completes when shutdown is done
     */
    public java.util.concurrent.CompletableFuture<Void> shutdownPerformanceManager() {
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }
    
    /**
     * Get performance statistics from the performance manager.
     * 
     * @return Map containing performance statistics
     */
    public Map<String, Object> getPerformanceStats() {
        return new java.util.HashMap<>(); // Performance manager removed
    }
    
    /**
     * Get health information from the performance manager.
     * 
     * @return Map containing health information
     */
    public Map<String, Object> getHealthInfo() {
        // Performance manager removed
        
        Map<String, Object> health = new HashMap<>();
        health.put("performance_manager_enabled", false);
        return health;
    }
    
    /**
     * Force cleanup and optimization.
     */
    public void performCleanup() {
        if (isShutdown.get()) {
            return;
        }
        
        try {
            // Clean up player data cache
            cleanupPlayerDataCache();
            
            // Clean up job references
            cleanupJobReferences();
            
            // Performance manager removed - no cleanup needed
            
            lastCleanupTime = System.currentTimeMillis();
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("JobManager cleanup completed. Active player data: " + playerData.size());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during JobManager cleanup", e);
        }
    }
    
    /**
     * Cleanup player data cache, removing stale entries.
     */
    private void cleanupPlayerDataCache() {
        if (playerData.size() <= MAX_CACHED_PLAYERS) {
            return;
        }
        
        dataLock.writeLock().lock();
        try {
            // Remove offline players from cache if we exceed the limit
            Iterator<Map.Entry<UUID, PlayerJobData>> iterator = playerData.entrySet().iterator();
            int removedCount = 0;
            
            while (iterator.hasNext() && playerData.size() > MAX_CACHED_PLAYERS) {
                Map.Entry<UUID, PlayerJobData> entry = iterator.next();
                Player player = plugin.getServer().getPlayer(entry.getKey());
                
                if (player == null || !player.isOnline()) {
                    // Save data before removing from cache
                    try {
                        savePlayerDataInternal(entry.getKey(), entry.getValue());
                        iterator.remove();
                        removedCount++;
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to save player data during cleanup for " + entry.getKey(), e);
                    }
                }
            }
            
            if (removedCount > 0 && plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Cleaned up " + removedCount + " offline player data entries from cache");
            }
        } finally {
            dataLock.writeLock().unlock();
        }
    }
    
    /**
     * Clean up weak references to player data.
     */
    private void cleanupJobReferences() {
        trackedPlayerData.removeIf(ref -> ref.get() == null);
    }
    
    /**
     * Shutdown the JobManager and clean up all resources.
     */
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            plugin.getLogger().info("Shutting down JobManager...");
            
            try {
                // Save all player data before shutdown
                saveAllPlayerDataSync();
                
                // Performance manager removed - no shutdown needed
                
                // Clear all caches
                dataLock.writeLock().lock();
                try {
                    playerData.clear();
                    trackedPlayerData.clear();
                } finally {
                    dataLock.writeLock().unlock();
                }
                
                // Shutdown XP curve manager
                if (xpCurveManager != null) {
                    // XpCurveManager doesn't have explicit shutdown, but clear reference
                    xpCurveManager = null;
                }
                
                plugin.getLogger().info("JobManager shutdown completed");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during JobManager shutdown", e);
            }
        }
    }
    
    /**
     * Save all player data synchronously (used during shutdown).
     */
    private void saveAllPlayerDataSync() {
        dataLock.readLock().lock();
        try {
            for (Map.Entry<UUID, PlayerJobData> entry : playerData.entrySet()) {
                try {
                    savePlayerDataInternal(entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save player data during shutdown for " + entry.getKey(), e);
                }
            }
        } finally {
            dataLock.readLock().unlock();
        }
    }
    
    /**
     * Internal method to save player data without external dependencies.
     */
    private void savePlayerDataInternal(UUID playerUuid, PlayerJobData data) {
        if (data == null) {
            return;
        }
        
        try {
            File dataFile = new File(dataFolder, playerUuid.toString() + ".yml");
            FileConfiguration config = new YamlConfiguration();
            data.save(config);
            config.save(dataFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + playerUuid, e);
        }
    }
    
    /**
     * Check if automatic cleanup is needed and perform it.
     */
    private void checkAndPerformCleanup() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
            plugin.getFoliaManager().runAsync(this::performCleanup);
        }
    }
    
    /**
     * Track a PlayerJobData instance for memory management.
     * 
     * @param data The player data to track
     */
    private void trackPlayerData(PlayerJobData data) {
        if (data != null) {
            trackedPlayerData.add(new WeakReference<>(data));
        }
    }
    
    /**
     * Get memory usage statistics.
     * 
     * @return Map containing memory usage information
     */
    public Map<String, Object> getMemoryStats() {
        Map<String, Object> stats = new HashMap<>();
        
        dataLock.readLock().lock();
        try {
            stats.put("cached_players", playerData.size());
            stats.put("max_cached_players", MAX_CACHED_PLAYERS);
            stats.put("loaded_jobs", jobs.size());
            stats.put("tracked_references", trackedPlayerData.size());
            stats.put("is_shutdown", isShutdown.get());
            stats.put("last_cleanup", new Date(lastCleanupTime));
        } finally {
            dataLock.readLock().unlock();
        }
        
        return stats;
    }
    
    /**
     * Get the plugin instance.
     * 
     * @return The plugin instance
     */
    public UniverseJobs getPlugin() {
        return plugin;
    }
}