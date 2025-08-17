package fr.ax_dev.jobsAdventure.job;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.xp.XpCurve;
import fr.ax_dev.jobsAdventure.xp.XpCurveManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all jobs and player job data.
 * Handles job loading, player job assignments, and data persistence.
 */
public class JobManager {
    
    private final JobsAdventure plugin;
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerJobData> playerData = new ConcurrentHashMap<>();
    private final File jobsFolder;
    private final File dataFolder;
    private XpCurveManager xpCurveManager;
    
    /**
     * Create a new JobManager instance.
     * 
     * @param plugin The plugin instance
     */
    public JobManager(JobsAdventure plugin) {
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
    }
    
    /**
     * Load all jobs from the jobs folder.
     */
    public void loadJobs() {
        jobs.clear();
        
        if (!jobsFolder.exists()) {
            plugin.getLogger().warning("Jobs folder does not exist, creating it...");
            jobsFolder.mkdirs();
            createExampleJobs();
        }
        
        File[] jobFiles = jobsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (jobFiles == null || jobFiles.length == 0) {
            plugin.getLogger().warning("No job files found in " + jobsFolder.getPath());
            createExampleJobs();
            // Reload after creating example jobs
            jobFiles = jobsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (jobFiles == null || jobFiles.length == 0) {
                return;
            }
        }
        
        int loadedCount = 0;
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
                        loadedCount++;
                        plugin.getLogger().info("Loaded job: " + job.getName() + " (" + jobId + ")");
                    } else {
                        plugin.getLogger().severe("Job " + jobId + " is disabled due to XP curve error: " + job.getXpCurveErrorMessage());
                    }
                } else {
                    plugin.getLogger().info("Skipped disabled job: " + jobId);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load job file: " + jobFile.getName(), e);
            }
        }
        
        plugin.getLogger().info("Loaded " + loadedCount + " jobs successfully");
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
        return playerData.computeIfAbsent(playerUuid, uuid -> {
            PlayerJobData data = new PlayerJobData(uuid);
            data.setJobManager(this); // Set JobManager reference for XP curve calculations
            return data;
        });
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
        if (!player.hasPermission(job.getPermission())) {
            return false;
        }
        
        PlayerJobData data = getPlayerData(player);
        return data.joinJob(jobId);
    }
    
    /**
     * Make a player leave a job.
     * 
     * @param player The player
     * @param jobId The job ID
     * @return true if successful
     */
    public boolean leaveJob(Player player, String jobId) {
        PlayerJobData data = getPlayerData(player);
        return data.leaveJob(jobId);
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
     * Add XP to a player's job.
     * 
     * @param player The player
     * @param jobId The job ID
     * @param xp The XP amount
     */
    public void addXp(Player player, String jobId, double xp) {
        PlayerJobData data = getPlayerData(player);
        data.addXp(jobId, xp);
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
        PlayerJobData data = playerData.get(playerUuid);
        if (data == null) return;
        
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
        try {
            File dataFile = new File(dataFolder, playerUuid.toString() + ".yml");
            if (!dataFile.exists()) {
                // Create new player data
                PlayerJobData newData = new PlayerJobData(playerUuid);
                newData.setJobManager(this);
                playerData.put(playerUuid, newData);
                return;
            }
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            PlayerJobData data = new PlayerJobData(playerUuid);
            data.setJobManager(this); // Set JobManager reference for XP curve calculations
            data.load(config);
            playerData.put(playerUuid, data);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + playerUuid, e);
            // Create new player data as fallback
            PlayerJobData fallbackData = new PlayerJobData(playerUuid);
            fallbackData.setJobManager(this);
            playerData.put(playerUuid, fallbackData);
        }
    }
    
    /**
     * Create example job files.
     */
    private void createExampleJobs() {
        plugin.getLogger().info("Creating example job files...");
        
        // Save miner.yml
        plugin.saveResource("jobs/miner.yml", false);
        
        // Save farmer.yml
        plugin.saveResource("jobs/farmer.yml", false);
        
        // Save hunter.yml
        plugin.saveResource("jobs/hunter.yml", false);
        
        plugin.getLogger().info("Example job files created successfully!");
    }
    
    /**
     * Save all player data.
     */
    public void saveAllPlayerData() {
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
                plugin.getLogger().info("Job " + job.getId() + " using XP equation: " + job.getXpEquation());
            } else if (job.getXpCurveName() != null) {
                // Use file-based curve
                curve = xpCurveManager.getCurve(job.getXpCurveName());
                plugin.getLogger().info("Job " + job.getId() + " using XP curve: " + job.getXpCurveName());
            } else {
                // Use default curve
                curve = xpCurveManager.getDefaultCurve();
                plugin.getLogger().info("Job " + job.getId() + " using default XP curve");
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
                
                plugin.getLogger().info("Job " + job.getId() + " XP curve validated successfully");
                
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
}