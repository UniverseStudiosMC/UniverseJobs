package fr.ax_dev.universejobs.job;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import fr.ax_dev.universejobs.levelup.SimpleLevelUpActionManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Stores job-related data for a specific player.
 * Handles XP, levels, and job membership.
 */
public class PlayerJobData {
    
    private static final String LAST_MODIFIED_KEY = "lastModified";
    
    private final UUID playerUuid;
    private final Set<String> jobs = ConcurrentHashMap.newKeySet();
    private final Map<String, Double> xpData = new ConcurrentHashMap<>();
    private final Map<String, Integer> levelData = new ConcurrentHashMap<>();
    
    // Thread safety
    private final ReadWriteLock dataLock = new ReentrantReadWriteLock();
    private final AtomicBoolean isLoading = new AtomicBoolean(false);
    private volatile long lastModified = System.currentTimeMillis();
    private volatile long lastLogin = System.currentTimeMillis();
    
    // Reference to JobManager for XP curve calculations
    private volatile JobManager jobManager;

    // Permission cache with size limit
    private static final int MAX_CACHE_SIZE = 100;
    private static final Map<String, Integer> maxLevelCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> cacheAccessTimes = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = TimeUnit.SECONDS.toMillis(30);
    private static volatile long lastCacheClear = System.currentTimeMillis();
    
    /**
     * Create new player job data.
     * 
     * @param playerUuid The player's UUID
     */
    public PlayerJobData(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }
    
    /**
     * Set the JobManager reference for XP curve calculations.
     *
     * @param jobManager The JobManager instance
     */
    public void setJobManager(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    /**
     * Clear permission cache for this player.
     * Should be called when player leaves or permissions change.
     */
    public void clearPermissionCache() {
        String playerKey = playerUuid.toString();
        maxLevelCache.entrySet().removeIf(entry -> entry.getKey().startsWith(playerKey + ":"));
        cacheAccessTimes.entrySet().removeIf(entry -> entry.getKey().startsWith(playerKey + ":"));
    }
    
    /**
     * Get the player's UUID.
     * 
     * @return The UUID
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    /**
     * Make the player join a job.
     * 
     * @param jobId The job ID
     * @return true if successful (wasn't already in the job)
     */
    public boolean joinJob(String jobId) {
        if (jobId == null || jobId.isEmpty()) {
            return false;
        }
        
        dataLock.writeLock().lock();
        try {
            if (jobs.add(jobId)) {
                // Initialize XP and level if not present
                xpData.putIfAbsent(jobId, 0.0);
                levelData.putIfAbsent(jobId, 1);
                lastModified = System.currentTimeMillis();
                return true;
            }
            return false;
        } finally {
            dataLock.writeLock().unlock();
        }
    }
    
    /**
     * Make the player leave a job.
     * 
     * @param jobId The job ID
     * @return true if successful (was in the job)
     */
    public boolean leaveJob(String jobId) {
        if (jobId == null || jobId.isEmpty()) {
            return false;
        }
        
        dataLock.writeLock().lock();
        try {
            boolean removed = jobs.remove(jobId);
            if (removed) {
                lastModified = System.currentTimeMillis();
            }
            return removed;
            // Note: We keep XP and level data even after leaving
        } finally {
            dataLock.writeLock().unlock();
        }
    }
    
    /**
     * Check if the player has a specific job.
     * 
     * @param jobId The job ID
     * @return true if the player has the job
     */
    public boolean hasJob(String jobId) {
        return jobs.contains(jobId);
    }
    
    /**
     * Get all jobs the player has.
     * 
     * @return Set of job IDs
     */
    public Set<String> getJobs() {
        dataLock.readLock().lock();
        try {
            return new HashSet<>(jobs);
        } finally {
            dataLock.readLock().unlock();
        }
    }
    
    /**
     * Add XP to a job.
     * 
     * @param jobId The job ID
     * @param xp The XP amount to add
     */
    public void addXp(String jobId, double xp) {
        if (jobId == null || !hasJob(jobId)) {
            return;
        }
        
        // Validate XP amount (allow negative values for removal)
        if (Double.isNaN(xp) || Double.isInfinite(xp)) {
            return;
        }
        
        dataLock.writeLock().lock();
        try {
            double currentXp = xpData.getOrDefault(jobId, 0.0);
            double newXp = currentXp + xp;
            
            // Prevent negative XP (minimum is 0)
            if (newXp < 0) {
                newXp = 0;
            }
            
            // Prevent overflow
            if (newXp > Double.MAX_VALUE / 2) {
                newXp = Double.MAX_VALUE / 2;
            }
            
            xpData.put(jobId, newXp);
            lastModified = System.currentTimeMillis();
            
            // Check for level up and trigger actions only if XP was added
            if (xp > 0) {
                checkLevelUp(jobId, xp);
            }
        } finally {
            dataLock.writeLock().unlock();
        }
    }
    
    /**
     * Get XP for a job.
     * 
     * @param jobId The job ID
     * @return The XP amount
     */
    public double getXp(String jobId) {
        return xpData.getOrDefault(jobId, 0.0);
    }
    
    /**
     * Set XP for a job.
     * 
     * @param jobId The job ID
     * @param xp The XP amount
     */
    public void setXp(String jobId, double xp) {
        xpData.put(jobId, xp);
        checkLevelUp(jobId, 0); // No XP gained since this is a direct set
    }
    
    /**
     * Get level for a job.
     * 
     * @param jobId The job ID
     * @return The level
     */
    public int getLevel(String jobId) {
        return levelData.getOrDefault(jobId, 1);
    }
    
    /**
     * Set level for a job.
     *
     * @param jobId The job ID
     * @param level The level
     */
    public void setLevel(String jobId, int level) {
        levelData.put(jobId, Math.max(1, level));
    }

    /**
     * Get the maximum level this player can reach for a job.
     * Takes into account both job configuration and player permissions.
     *
     * @param jobId The job ID
     * @return The effective max level
     */
    public int getMaxLevel(String jobId) {
        return getEffectiveMaxLevel(jobId);
    }
    
    /**
     * Calculate total XP required to reach a level using job's XP curve.
     * 
     * @param jobId The job ID  
     * @param level The target level
     * @return Total XP required
     */
    private double getTotalXpForLevel(String jobId, int level) {
        if (jobManager == null) {
            return fallbackTotalXpForLevel(level);
        }
        
        Job job = jobManager.getJob(jobId);
        if (job != null && job.getXpCurve() != null) {
            return job.getXpCurve().getXpForLevel(level);
        }
        
        return fallbackTotalXpForLevel(level);
    }
    
    /**
     * Calculate level from total XP using job's XP curve.
     * 
     * @param jobId The job ID
     * @param totalXp The total XP
     * @return The level
     */
    private int getLevelFromXp(String jobId, double totalXp) {
        if (jobManager == null) {
            return fallbackLevelFromXp(totalXp);
        }
        
        Job job = jobManager.getJob(jobId);
        if (job != null && job.getXpCurve() != null) {
            return job.getXpCurve().getLevelForXp(totalXp, job.getMaxLevel());
        }
        
        return fallbackLevelFromXp(totalXp);
    }
    
    // Fallback methods using old hardcoded values (for backwards compatibility)
    private static double fallbackRequiredXp(int level) {
        if (level <= 1) return 0;
        return 100.0 * Math.pow(1.5, (double) level - 2);
    }
    
    private static double fallbackTotalXpForLevel(int level) {
        double totalXp = 0;
        for (int i = 2; i <= level; i++) {
            totalXp += fallbackRequiredXp(i);
        }
        return totalXp;
    }
    
    private static int fallbackLevelFromXp(double totalXp) {
        if (totalXp <= 0) return 1;
        
        int level = 1;
        double xpNeeded = 0;
        
        while (xpNeeded <= totalXp) {
            level++;
            xpNeeded += fallbackRequiredXp(level);
        }
        
        return (int) ((double) level - 1);
    }
    
    /**
     * Get XP progress to next level.
     * 
     * @param jobId The job ID
     * @return Array with [current XP in level, XP needed for next level]
     */
    public double[] getXpProgress(String jobId) {
        double totalXp = getXp(jobId);
        int currentLevel = getLevel(jobId);
        
        // XP required to reach current level
        double xpForCurrentLevel = getTotalXpForLevel(jobId, currentLevel);
        // XP required to reach next level  
        double xpForNextLevel = getTotalXpForLevel(jobId, currentLevel + 1);
        
        // Current XP within this level
        double currentXpInLevel = totalXp - xpForCurrentLevel;
        // Total XP needed to complete this level
        double xpNeededForNext = xpForNextLevel - xpForCurrentLevel;
        
        return new double[]{currentXpInLevel, xpNeededForNext};
    }
    
    /**
     * Get the effective max level for a player in a job, considering permissions.
     *
     * @param jobId The job ID
     * @return The max level the player can reach
     */
    private int getEffectiveMaxLevel(String jobId) {
        if (jobManager == null) {
            return Integer.MAX_VALUE;
        }

        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerUuid);
        if (player == null || !player.isOnline()) {
            Job job = jobManager.getJob(jobId);
            return job != null ? job.getMaxLevel() : 100;
        }

        clearCacheIfNeeded();

        String cacheKey = playerUuid.toString() + ":" + jobId;
        Integer cachedLevel = maxLevelCache.get(cacheKey);
        if (cachedLevel != null) {
            cacheAccessTimes.put(cacheKey, System.currentTimeMillis());
            return cachedLevel;
        }

        Job job = jobManager.getJob(jobId);
        int defaultMaxLevel = job != null ? job.getMaxLevel() : 100;
        int maxLevel = defaultMaxLevel;

        // Parse effective permissions to find the highest maxlevel permission
        String permissionPrefix = "universejobs.job." + jobId + ".maxlevel.";

        for (org.bukkit.permissions.PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (info.getValue() && info.getPermission().startsWith(permissionPrefix)) {
                String levelStr = info.getPermission().substring(permissionPrefix.length());
                try {
                    int level = Integer.parseInt(levelStr);
                    if (level > maxLevel) {
                        maxLevel = level;
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore non-numeric permission endings
                }
            }
        }

        // Add to cache with size management
        addToCache(cacheKey, maxLevel);
        return maxLevel;
    }

    private void addToCache(String key, int value) {
        // Check if cache is full
        if (maxLevelCache.size() >= MAX_CACHE_SIZE) {
            // Remove oldest entry
            String oldestKey = null;
            long oldestTime = Long.MAX_VALUE;

            for (Map.Entry<String, Long> entry : cacheAccessTimes.entrySet()) {
                if (entry.getValue() < oldestTime) {
                    oldestTime = entry.getValue();
                    oldestKey = entry.getKey();
                }
            }

            if (oldestKey != null) {
                maxLevelCache.remove(oldestKey);
                cacheAccessTimes.remove(oldestKey);
            }
        }

        maxLevelCache.put(key, value);
        cacheAccessTimes.put(key, System.currentTimeMillis());
    }

    private void clearCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCacheClear > CACHE_DURATION) {
            maxLevelCache.clear();
            cacheAccessTimes.clear();
            lastCacheClear = now;
        }
    }



    /**
     * Check if the player should level up and update accordingly.
     *
     * @param jobId The job ID
     * @param xpGained The XP gained that might trigger level up
     * @return true if leveled up
     */
    private boolean checkLevelUp(String jobId, double xpGained) {
        double totalXp = getXp(jobId);
        int currentLevel = getLevel(jobId);
        int calculatedLevel = getLevelFromXp(jobId, totalXp);
        int maxLevel = getEffectiveMaxLevel(jobId);

        if (jobManager != null && jobManager.getPlugin() != null) {
            jobManager.getPlugin().getLogger().info("[DEBUG] CheckLevelUp - Job: " + jobId + ", TotalXP: " + totalXp + ", CurrentLevel: " + currentLevel + ", CalculatedLevel: " + calculatedLevel);
            if (jobManager.getJob(jobId) != null && jobManager.getJob(jobId).getXpCurve() != null) {
                double xpForCurrentLevel = jobManager.getJob(jobId).getXpCurve().getXpForLevel(currentLevel);
                double xpForNextLevel = jobManager.getJob(jobId).getXpCurve().getXpForLevel(currentLevel + 1);
                jobManager.getPlugin().getLogger().info("[DEBUG] XP Curve - Level " + currentLevel + " requires: " + xpForCurrentLevel + " XP, Level " + (currentLevel + 1) + " requires: " + xpForNextLevel + " XP");
            }
        }

        if (calculatedLevel > maxLevel) {
            calculatedLevel = maxLevel;
        }

        if (calculatedLevel > currentLevel) {
            setLevel(jobId, calculatedLevel);
            
            // Trigger level up actions if JobManager is available
            if (jobManager != null) {
                try {
                    // Get the player from the UUID
                    org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerUuid);
                    if (player != null && player.isOnline()) {
                        // Get the level up action manager from the plugin
                        SimpleLevelUpActionManager actionManager =
                            jobManager.getPlugin().getLevelUpActionManager();
                        if (actionManager != null) {
                            actionManager.executeLevelUpActions(player, jobId, currentLevel, calculatedLevel, totalXp, xpGained);
                        }

                        jobManager.getPlugin().getMenuManager().refreshPlayerMenu(player);
                    }
                } catch (Exception e) {
                    // Log error but don't fail the level up
                    if (jobManager.getPlugin() != null) {
                        jobManager.getPlugin().getLogger().warning("Failed to execute level up actions for player " + playerUuid + " in job " + jobId + ": " + e.getMessage());
                    }
                }
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Save data to configuration.
     * 
     * @param config The configuration to save to
     */
    public void save(FileConfiguration config) {
        dataLock.readLock().lock();
        try {
            config.set("uuid", playerUuid.toString());
            config.set(LAST_MODIFIED_KEY, lastModified);
            config.set("jobs", new ArrayList<>(jobs));
            
            ConfigurationSection xpSection = config.createSection("xp");
            for (Map.Entry<String, Double> entry : xpData.entrySet()) {
                xpSection.set(entry.getKey(), entry.getValue());
            }
            
            ConfigurationSection levelSection = config.createSection("levels");
            for (Map.Entry<String, Integer> entry : levelData.entrySet()) {
                levelSection.set(entry.getKey(), entry.getValue());
            }
        } finally {
            dataLock.readLock().unlock();
        }
    }
    
    /**
     * Load data from configuration.
     * 
     * @param config The configuration to load from
     */
    public void load(FileConfiguration config) {
        if (isLoading.compareAndSet(false, true)) {
            dataLock.writeLock().lock();
            try {
                // Load timestamp
                lastModified = config.getLong(LAST_MODIFIED_KEY, System.currentTimeMillis());
                
                // Load jobs
                List<String> jobsList = config.getStringList("jobs");
                jobs.clear();
                jobs.addAll(jobsList);
                
                // Load XP data
                ConfigurationSection xpSection = config.getConfigurationSection("xp");
                if (xpSection != null) {
                    xpData.clear();
                    for (String jobId : xpSection.getKeys(false)) {
                        double xp = xpSection.getDouble(jobId);
                        // Validate loaded XP
                        if (!Double.isNaN(xp) && !Double.isInfinite(xp) && xp >= 0) {
                            xpData.put(jobId, xp);
                        }
                    }
                }
                
                // Load level data
                ConfigurationSection levelSection = config.getConfigurationSection("levels");
                if (levelSection != null) {
                    levelData.clear();
                    for (String jobId : levelSection.getKeys(false)) {
                        int level = levelSection.getInt(jobId);
                        // Validate loaded level
                        if (level >= 1 && level <= 10000) { // Reasonable bounds
                            levelData.put(jobId, level);
                        }
                    }
                }
                
                // Ensure all jobs have XP and level data
                for (String jobId : jobs) {
                    xpData.putIfAbsent(jobId, 0.0);
                    levelData.putIfAbsent(jobId, 1);
                }
            } finally {
                dataLock.writeLock().unlock();
                isLoading.set(false);
            }
        }
    }
    
    /**
     * Get the last modification time.
     *
     * @return Last modification timestamp
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Get the last login time.
     *
     * @return Last login timestamp
     */
    public long getLastLogin() {
        return lastLogin;
    }

    /**
     * Set the last login time.
     *
     * @param lastLogin The last login timestamp
     */
    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    /**
     * Check if data is currently being loaded.
     * 
     * @return true if loading is in progress
     */
    public boolean isLoading() {
        return isLoading.get();
    }
    
    /**
     * Get a thread-safe snapshot of the data for debugging.
     * 
     * @return Map containing data summary
     */
    public Map<String, Object> getDataSnapshot() {
        dataLock.readLock().lock();
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("playerUuid", playerUuid.toString());
            snapshot.put("jobCount", jobs.size());
            snapshot.put("totalXp", xpData.values().stream().mapToDouble(Double::doubleValue).sum());
            snapshot.put(LAST_MODIFIED_KEY, new Date(lastModified));
            snapshot.put("isLoading", isLoading.get());
            return snapshot;
        } finally {
            dataLock.readLock().unlock();
        }
    }
    
    @Override
    public String toString() {
        return "PlayerJobData{playerUuid=" + playerUuid + ", jobs=" + jobs.size() + ", lastModified=" + new Date(lastModified) + "}";
    }
}