package fr.ax_dev.universejobs.reward;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.config.LanguageManager;
import fr.ax_dev.universejobs.condition.ConditionContext;
import fr.ax_dev.universejobs.condition.ConditionResult;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.reward.gui.GuiConfig;
import fr.ax_dev.universejobs.reward.gui.GuiConfigLoader;
import fr.ax_dev.universejobs.reward.gui.ItemBuilder;
import fr.ax_dev.universejobs.reward.storage.RewardStorage;
import fr.ax_dev.universejobs.storage.DataStorage;
import fr.ax_dev.universejobs.utils.MessageUtils;
import fr.ax_dev.universejobs.menu.MenuUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Main manager for the reward system.
 * Handles loading, managing, and processing rewards for jobs.
 */
public class RewardManager {
    
    private final UniverseJobs plugin;
    private final LanguageManager languageManager;
    private final RewardStorage storage;
    private final Map<String, List<Reward>> jobRewards;
    private final Map<String, Reward> allRewards;
    private final GuiConfigLoader guiConfigLoader;
    
    /**
     * Create a new RewardManager.
     * 
     * @param plugin The plugin instance
     */
    public RewardManager(UniverseJobs plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.jobRewards = new ConcurrentHashMap<>();
        this.allRewards = new ConcurrentHashMap<>();
        this.guiConfigLoader = new GuiConfigLoader(plugin);
        
        this.storage = plugin.getDataStorage();
    }
    
    /**
     * Initialize the reward system.
     */
    public void initialize() {
        storage.initialize();
        loadRewards();
        guiConfigLoader.loadGuiConfigs();
        
        // Reward system initialized
    }
    
    /**
     * Shutdown the reward system.
     */
    public void shutdown() {
        storage.shutdown();
        jobRewards.clear();
        allRewards.clear();
        
        plugin.getLogger().info("Reward system shutdown complete");
    }
    
    /**
     * Load all rewards from configuration files.
     */
    public void loadRewards() {
        jobRewards.clear();
        allRewards.clear();
        
        File rewardsFolder = new File(plugin.getDataFolder(), "rewards");
        if (!rewardsFolder.exists()) {
            if (rewardsFolder.mkdirs()) {
                // Created rewards folder for the first time - generate all default reward files
                createDefaultRewardFiles();
            } else {
                plugin.getLogger().severe("Failed to create rewards folder: " + rewardsFolder.getPath());
                return;
            }
        }
        
        // Load rewards based on job configurations
        Set<String> processedFiles = new HashSet<>();
        
        // First pass: Load rewards from jobs that have rewards file specified
        for (Job job : plugin.getJobManager().getAllJobs()) {
            String rewardsFileName = job.getRewardsFile();
            if (rewardsFileName != null && !rewardsFileName.isEmpty()) {
                File rewardFile = new File(rewardsFolder, rewardsFileName + ".yml");
                if (rewardFile.exists()) {
                    loadRewardFileForJob(rewardFile, job.getId());
                    processedFiles.add(rewardsFileName + ".yml");
                } else {
                    plugin.getLogger().warning("Job '" + job.getId() + "' specifies rewards '" + 
                        rewardsFileName + "' but file " + rewardFile.getName() + " does not exist");
                }
            }
        }
        
        // Second pass: Load remaining files using filename as job ID (legacy support)
        File[] files = rewardsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                if (!processedFiles.contains(file.getName())) {
                    loadRewardFile(file);
                }
            }
        }
        
        // Rewards loaded
    }
    
    /**
     * Load rewards from a specific file for a specific job.
     * 
     * @param file The reward file
     * @param jobId The job ID to associate these rewards with
     */
    private void loadRewardFileForJob(File file, String jobId) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            // Check if job exists
            Job job = plugin.getJobManager().getJob(jobId);
            if (job == null) {
                plugin.getLogger().warning("Reward file " + file.getName() + " references unknown job: " + jobId);
                return;
            }
            
            List<Reward> rewards = new ArrayList<>();
            
            // Load rewards from file
            ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
            if (rewardsSection != null) {
                for (String rewardId : rewardsSection.getKeys(false)) {
                    ConfigurationSection rewardConfig = rewardsSection.getConfigurationSection(rewardId);
                    if (rewardConfig != null) {
                        try {
                            Reward reward = new Reward(rewardId, jobId, rewardConfig);
                            rewards.add(reward);
                            allRewards.put(jobId + ":" + rewardId, reward);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load reward " + rewardId + 
                                " from file " + file.getName() + ": " + e.getMessage());
                        }
                    }
                }
            }
            
            jobRewards.put(jobId, rewards);
            // Rewards loaded for job
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load reward file " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Load rewards from a specific file.
     * 
     * @param file The reward file
     */
    private void loadRewardFile(File file) {
        String jobId = file.getName().replace(".yml", "");
        loadRewardFileForJob(file, jobId);
    }
    
    /**
     * Get all rewards for a specific job.
     * 
     * @param jobId The job ID
     * @return List of rewards
     */
    public List<Reward> getJobRewards(String jobId) {
        return new ArrayList<>(jobRewards.getOrDefault(jobId, new ArrayList<>()));
    }
    
    /**
     * Get a specific reward by job and reward ID.
     * 
     * @param jobId The job ID
     * @param rewardId The reward ID
     * @return The reward or null if not found
     */
    public Reward getReward(String jobId, String rewardId) {
        return allRewards.get(jobId + ":" + rewardId);
    }
    
    /**
     * Get the status of a reward for a player.
     * 
     * @param player The player
     * @param reward The reward
     * @return The reward status
     */
    public RewardStatus getRewardStatus(Player player, Reward reward) {
        // Check if already claimed and not repeatable
        if (storage.hasClaimedReward(player.getUniqueId(), reward.getJobId(), reward.getId())) {
            if (!reward.isRepeatable()) {
                return RewardStatus.RETRIEVED;
            }
            
            // Check cooldown for repeatable rewards
            if (reward.getCooldownHours() > 0) {
                long lastClaim = storage.getClaimTime(player.getUniqueId(), reward.getJobId(), reward.getId());
                long cooldownMs = reward.getCooldownHours() * 3600000L;
                
                if (System.currentTimeMillis() - lastClaim < cooldownMs) {
                    return RewardStatus.RETRIEVED;
                }
            }
        }
        
        // Check requirements (without showing feedback messages)
        if (!canClaimReward(player, reward, false)) {
            return RewardStatus.BLOCKED;
        }
        
        return RewardStatus.RETRIEVABLE;
    }
    
    /**
     * Check if a player can claim a specific reward.
     * 
     * @param player The player
     * @param reward The reward
     * @return true if the player can claim the reward
     */
    public boolean canClaimReward(Player player, Reward reward) {
        return canClaimReward(player, reward, true);
    }
    
    /**
     * Check if a player can claim a specific reward with optional feedback.
     * 
     * @param player The player
     * @param reward The reward
     * @param showFeedback Whether to show feedback messages/sounds
     * @return true if the player can claim the reward
     */
    public boolean canClaimReward(Player player, Reward reward, boolean showFeedback) {
        // Check if player has the job
        if (!plugin.getJobManager().hasJob(player, reward.getJobId())) {
            if (showFeedback) {
                MessageUtils.sendMessage(player, languageManager.getMessage("rewards.claim.no-job", "job", reward.getJobId()));
            }
            return false;
        }
        
        // Check level requirement
        int playerLevel = plugin.getJobManager().getLevel(player, reward.getJobId());
        if (playerLevel < reward.getRequiredLevel()) {
            if (showFeedback) {
                MessageUtils.sendMessage(player, languageManager.getMessage("rewards.claim.level-requirement", 
                    "required", String.valueOf(reward.getRequiredLevel()), 
                    "job", reward.getJobId(), 
                    "current", String.valueOf(playerLevel)));
            }
            return false;
        }
        
        // Check permission
        if (reward.getPermission() != null && !player.hasPermission(reward.getPermission())) {
            if (showFeedback) {
                MessageUtils.sendMessage(player, languageManager.getMessage("rewards.claim.no-permission", "permission", reward.getPermission()));
            }
            return false;
        }
        
        // Check custom requirements
        if (reward.hasRequirements()) {
            ConditionContext context = new ConditionContext();
            ConditionResult result = reward.getRequirements().evaluate(player, null, context);
            if (!result.isAllowed()) {
                if (showFeedback) {
                    // Use the condition system's built-in feedback
                    result.execute(player);
                }
                return false;
            } else if (showFeedback && (result.getAcceptMessage() != null || result.getAcceptSound() != null || result.getAcceptCommands() != null)) {
                // Execute accept actions if they exist
                result.execute(player);
            }
        }
        
        return true;
    }
    
    /**
     * Attempt to claim a reward for a player.
     * 
     * @param player The player
     * @param reward The reward
     * @return true if the reward was successfully claimed
     */
    public boolean claimReward(Player player, Reward reward) {
        // Check if can claim
        RewardStatus status = getRewardStatus(player, reward);
        if (status != RewardStatus.RETRIEVABLE) {
            return false;
        }
        
        // Give reward items
        if (reward.hasItems()) {
            for (Reward.RewardItem rewardItem : reward.getItems()) {
                ItemStack item = ItemBuilder.fromRewardItem(plugin, rewardItem).build();
                
                // Try to add to inventory, drop if full
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                for (ItemStack overflowItem : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
                }
            }
        }
        
        // Give economy reward
        if (reward.hasEconomyReward()) {
            try {
                net.milkbowl.vault.economy.Economy economy = null;
                if (plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class) != null) {
                    economy = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class).getProvider();
                }
                
                if (economy != null) {
                    economy.depositPlayer(player, reward.getEconomyReward());
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Economy reward of " + reward.getEconomyReward() + " given to player " + player.getName());
                    }
                } else {
                    plugin.getLogger().warning("Economy reward of " + reward.getEconomyReward() + " could not be given to " + player.getName() + " - Vault/Economy not found");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to give economy reward to " + player.getName() + ": " + e.getMessage());
            }
        }
        
        // Execute commands
        if (reward.hasCommands()) {
            for (String command : reward.getCommands()) {
                String processedCommand = command.replace("{player}", player.getName())
                                                .replace("{job}", reward.getJobId())
                                                .replace("{reward}", reward.getId())
                                                .replace("%player_name%", player.getName());
                
                // Process PlaceholderAPI placeholders
                processedCommand = MenuUtils.processPlaceholders(player, processedCommand);
                
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
            }
        }
        
        // Mark as claimed
        storage.claimReward(player.getUniqueId(), reward.getJobId(), reward.getId(), System.currentTimeMillis());
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Player " + player.getName() + " claimed reward: " + reward.getId() + " from job: " + reward.getJobId());
        }
        
        return true;
    }
    
    /**
     * Get the last claim time for a reward.
     * 
     * @param player The player
     * @param reward The reward
     * @return The last claim time or -1 if never claimed
     */
    public long getLastClaimTime(Player player, Reward reward) {
        return storage.getClaimTime(player.getUniqueId(), reward.getJobId(), reward.getId());
    }
    
    /**
     * Reset a reward claim for a player.
     * 
     * @param player The player
     * @param reward The reward
     */
    public void resetRewardClaim(Player player, Reward reward) {
        storage.resetRewardClaim(player.getUniqueId(), reward.getJobId(), reward.getId());
    }
    
    /**
     * Reset all rewards for a player in a specific job.
     * 
     * @param player The player
     * @param jobId The job ID
     */
    public void resetJobRewards(Player player, String jobId) {
        storage.resetJobRewards(player.getUniqueId(), jobId);
    }
    
    /**
     * Reset all rewards for a player in a specific job using UUID.
     * 
     * @param playerUuid The player UUID
     * @param jobId The job ID
     */
    public void resetJobRewards(UUID playerUuid, String jobId) {
        storage.resetJobRewards(playerUuid, jobId);
    }
    
    /**
     * Reset all rewards for a player.
     * 
     * @param player The player
     */
    public void resetAllRewards(Player player) {
        storage.resetAllRewards(player.getUniqueId());
    }
    
    /**
     * Load player data when they join.
     * 
     * @param player The player
     */
    public void loadPlayerData(Player player) {
        storage.loadPlayerData(player.getUniqueId());
    }
    
    /**
     * Unload player data when they leave.
     * 
     * @param player The player
     */
    public void unloadPlayerData(Player player) {
        storage.unloadPlayerData(player.getUniqueId());
    }
    
    /**
     * Create default reward files from resources.
     */
    private void createDefaultRewardFiles() {
        String[] defaultRewardFiles = {"example_rewards.yml", "miner_rewards.yml", "farmer_rewards.yml", "hunter_rewards.yml", "lumberjack_rewards.yml"};
        int createdCount = 0;

        for (String rewardFile : defaultRewardFiles) {
            try {
                plugin.saveResource("rewards/" + rewardFile, false);
                createdCount++;
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Could not create reward file " + rewardFile + ": " + e.getMessage());
            }
        }

        if (createdCount > 0) {
            plugin.getLogger().info("Created " + createdCount + " default reward files");
        }
    }


    /**
     * Save all pending data.
     */
    public void saveAll() {
        storage.save();
    }
    
    /**
     * Reload all rewards from files.
     */
    public void reloadRewards() {
        loadRewards();
        guiConfigLoader.reloadGuiConfigs();
    }
    
    /**
     * Get the reward storage instance.
     * 
     * @return The reward storage
     */
    public RewardStorage getStorage() {
        return storage;
    }
    
    /**
     * Get the GUI configuration for a job.
     * 
     * @param jobId The job ID
     * @return The GUI configuration, or null if not found
     */
    public GuiConfig getGuiConfig(String jobId) {
        // First check if the job has a specific gui-reward file configured
        Job job = plugin.getJobManager().getJob(jobId);
        if (job != null && job.getGuiReward() != null) {
            return guiConfigLoader.getGuiConfig(job.getGuiReward());
        }
        
        // Fallback to jobId-based lookup (legacy)
        return guiConfigLoader.getGuiConfig(jobId);
    }
    
    /**
     * Get all rewards across all jobs.
     * 
     * @return Map of all rewards (key: jobId:rewardId)
     */
    public Map<String, Reward> getAllRewards() {
        return new HashMap<>(allRewards);
    }
    
    /**
     * Get statistics about the reward system.
     * 
     * @return Map containing statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_rewards", allRewards.size());
        stats.put("jobs_with_rewards", jobRewards.size());
        
        int totalRepeatable = 0;
        int totalWithCooldown = 0;
        
        for (Reward reward : allRewards.values()) {
            if (reward.isRepeatable()) totalRepeatable++;
            if (reward.getCooldownHours() > 0) totalWithCooldown++;
        }
        
        stats.put("repeatable_rewards", totalRepeatable);
        stats.put("cooldown_rewards", totalWithCooldown);
        
        return stats;
    }
    
    /**
     * Debug method to check player reward eligibility.
     * 
     * @param player The player
     * @param jobId The job ID
     */
    public void debugPlayerRewards(Player player, String jobId) {
        plugin.getLogger().info("=== DEBUG REWARD INFO FOR " + player.getName() + " ===");
        
        // Check if player has the job
        boolean hasJob = plugin.getJobManager().hasJob(player, jobId);
        plugin.getLogger().info("Has job '" + jobId + "': " + hasJob);
        
        if (hasJob) {
            int level = plugin.getJobManager().getLevel(player, jobId);
            plugin.getLogger().info("Current level in '" + jobId + "': " + level);
        }
        
        // List all rewards for this job
        List<Reward> rewards = getJobRewards(jobId);
        plugin.getLogger().info("Total rewards for job '" + jobId + "': " + rewards.size());
        
        for (Reward reward : rewards) {
            RewardStatus status = getRewardStatus(player, reward);
            plugin.getLogger().info("Reward '" + reward.getId() + "' - Required Level: " + 
                reward.getRequiredLevel() + " - Status: " + status);
        }
        
        plugin.getLogger().info("=== END DEBUG INFO ===");
    }
}