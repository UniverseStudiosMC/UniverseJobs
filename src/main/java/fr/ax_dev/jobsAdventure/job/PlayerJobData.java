package fr.ax_dev.jobsAdventure.job;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores job-related data for a specific player.
 * Handles XP, levels, and job membership.
 */
public class PlayerJobData {
    
    private final UUID playerUuid;
    private final Set<String> jobs = ConcurrentHashMap.newKeySet();
    private final Map<String, Double> xpData = new ConcurrentHashMap<>();
    private final Map<String, Integer> levelData = new ConcurrentHashMap<>();
    
    // Reference to JobManager for XP curve calculations
    private JobManager jobManager;
    
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
        if (jobs.add(jobId)) {
            // Initialize XP and level if not present
            xpData.putIfAbsent(jobId, 0.0);
            levelData.putIfAbsent(jobId, 1);
            return true;
        }
        return false;
    }
    
    /**
     * Make the player leave a job.
     * 
     * @param jobId The job ID
     * @return true if successful (was in the job)
     */
    public boolean leaveJob(String jobId) {
        return jobs.remove(jobId);
        // Note: We keep XP and level data even after leaving
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
        return new HashSet<>(jobs);
    }
    
    /**
     * Add XP to a job.
     * 
     * @param jobId The job ID
     * @param xp The XP amount to add
     */
    public void addXp(String jobId, double xp) {
        if (!hasJob(jobId)) return;
        
        double currentXp = getXp(jobId);
        double newXp = currentXp + xp;
        xpData.put(jobId, newXp);
        
        // Check for level up
        checkLevelUp(jobId);
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
        checkLevelUp(jobId);
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
     * Calculate required XP for a specific level using job's XP curve.
     * 
     * @param jobId The job ID
     * @param level The target level
     * @return Required XP
     */
    private double getRequiredXp(String jobId, int level) {
        if (jobManager == null) {
            return fallbackRequiredXp(level); // Fallback to old system
        }
        
        Job job = jobManager.getJob(jobId);
        if (job != null && job.getXpCurve() != null) {
            return job.getXpCurve().getXpForLevel(level);
        }
        
        return fallbackRequiredXp(level);
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
        return 100.0 * Math.pow(1.5, level - 2);
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
        
        return level - 1;
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
     * @return true if leveled up
     */
    private boolean checkLevelUp(String jobId) {
        double totalXp = getXp(jobId);
        int currentLevel = getLevel(jobId);
        int calculatedLevel = getLevelFromXp(jobId, totalXp);
        
        if (calculatedLevel > currentLevel) {
            setLevel(jobId, calculatedLevel);
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
        config.set("uuid", playerUuid.toString());
        config.set("jobs", new ArrayList<>(jobs));
        
        ConfigurationSection xpSection = config.createSection("xp");
        for (Map.Entry<String, Double> entry : xpData.entrySet()) {
            xpSection.set(entry.getKey(), entry.getValue());
        }
        
        ConfigurationSection levelSection = config.createSection("levels");
        for (Map.Entry<String, Integer> entry : levelData.entrySet()) {
            levelSection.set(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Load data from configuration.
     * 
     * @param config The configuration to load from
     */
    public void load(FileConfiguration config) {
        // Load jobs
        List<String> jobsList = config.getStringList("jobs");
        jobs.clear();
        jobs.addAll(jobsList);
        
        // Load XP data
        ConfigurationSection xpSection = config.getConfigurationSection("xp");
        if (xpSection != null) {
            xpData.clear();
            for (String jobId : xpSection.getKeys(false)) {
                xpData.put(jobId, xpSection.getDouble(jobId));
            }
        }
        
        // Load level data
        ConfigurationSection levelSection = config.getConfigurationSection("levels");
        if (levelSection != null) {
            levelData.clear();
            for (String jobId : levelSection.getKeys(false)) {
                levelData.put(jobId, levelSection.getInt(jobId));
            }
        }
        
        // Ensure all jobs have XP and level data
        for (String jobId : jobs) {
            xpData.putIfAbsent(jobId, 0.0);
            levelData.putIfAbsent(jobId, 1);
        }
    }
    
    @Override
    public String toString() {
        return "PlayerJobData{playerUuid=" + playerUuid + ", jobs=" + jobs.size() + "}";
    }
}