package fr.ax_dev.universejobs.job;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import fr.ax_dev.universejobs.levelup.SimpleLevelUpActionManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    
    // Reference to JobManager for XP curve calculations
    private volatile JobManager jobManager;
    
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
        
        // Validate XP amount
        if (Double.isNaN(xp) || Double.isInfinite(xp) || xp < 0) {
            return;
        }
        
        dataLock.writeLock().lock();
        try {
            double currentXp = xpData.getOrDefault(jobId, 0.0);
            double newXp = currentXp + xp;
            
            // Prevent overflow
            if (newXp > Double.MAX_VALUE / 2) {
                newXp = Double.MAX_VALUE / 2;
            }
            
            xpData.put(jobId, newXp);
            lastModified = System.currentTimeMillis();
            
            // Check for level up and trigger actions
            checkLevelUp(jobId, xp);
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