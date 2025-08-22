package fr.ax_dev.universejobs.reward.storage;

import fr.ax_dev.universejobs.UniverseJobs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * File-based implementation of RewardStorage.
 * Stores reward claim data in YAML files in the plugin's data folder.
 */
public class FileRewardStorage implements RewardStorage {
    
    private final UniverseJobs plugin;
    private final File dataFolder;
    private final Map<UUID, Map<String, Map<String, Long>>> playerRewards;
    private final Set<UUID> dirtyPlayers;
    
    /**
     * Create a new FileRewardStorage.
     * 
     * @param plugin The plugin instance
     */
    public FileRewardStorage(UniverseJobs plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "reward-data");
        this.playerRewards = new ConcurrentHashMap<>();
        this.dirtyPlayers = ConcurrentHashMap.newKeySet();
    }
    
    @Override
    public void initialize() {
        // Create data folder if it doesn't exist
        if (!dataFolder.exists()) {
            if (dataFolder.mkdirs()) {
                // Created reward data folder
            } else {
                plugin.getLogger().severe("Failed to create reward data folder: " + dataFolder.getPath());
            }
        }
        
        // File-based reward storage initialized
    }
    
    @Override
    public void shutdown() {
        // Save all dirty player data
        save();
        
        // Clear memory
        playerRewards.clear();
        dirtyPlayers.clear();
        
        plugin.getLogger().info("File-based reward storage shutdown complete");
    }
    
    @Override
    public boolean hasClaimedReward(UUID playerId, String jobId, String rewardId) {
        Map<String, Map<String, Long>> jobRewards = playerRewards.get(playerId);
        if (jobRewards == null) return false;
        
        Map<String, Long> rewards = jobRewards.get(jobId);
        if (rewards == null) return false;
        
        return rewards.containsKey(rewardId);
    }
    
    @Override
    public void claimReward(UUID playerId, String jobId, String rewardId, long claimTime) {
        playerRewards.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                     .computeIfAbsent(jobId, k -> new ConcurrentHashMap<>())
                     .put(rewardId, claimTime);
        
        dirtyPlayers.add(playerId);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info(String.format("Player %s claimed reward %s:%s", 
                    playerId, jobId, rewardId));
        }
    }
    
    @Override
    public long getClaimTime(UUID playerId, String jobId, String rewardId) {
        Map<String, Map<String, Long>> jobRewards = playerRewards.get(playerId);
        if (jobRewards == null) return -1;
        
        Map<String, Long> rewards = jobRewards.get(jobId);
        if (rewards == null) return -1;
        
        return rewards.getOrDefault(rewardId, -1L);
    }
    
    @Override
    public Set<String> getClaimedRewards(UUID playerId, String jobId) {
        Map<String, Map<String, Long>> jobRewards = playerRewards.get(playerId);
        if (jobRewards == null) return new HashSet<>();
        
        Map<String, Long> rewards = jobRewards.get(jobId);
        if (rewards == null) return new HashSet<>();
        
        return new HashSet<>(rewards.keySet());
    }
    
    @Override
    public Set<String> getAllClaimedRewards(UUID playerId) {
        Set<String> allRewards = new HashSet<>();
        Map<String, Map<String, Long>> jobRewards = playerRewards.get(playerId);
        
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
        Map<String, Map<String, Long>> jobRewards = playerRewards.get(playerId);
        if (jobRewards == null) return;
        
        Map<String, Long> rewards = jobRewards.get(jobId);
        if (rewards == null) return;
        
        if (rewards.remove(rewardId) != null) {
            dirtyPlayers.add(playerId);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info(String.format("Reset reward claim %s:%s for player %s", 
                        jobId, rewardId, playerId));
            }
        }
    }
    
    @Override
    public void resetJobRewards(UUID playerId, String jobId) {
        Map<String, Map<String, Long>> jobRewards = playerRewards.get(playerId);
        if (jobRewards == null) return;
        
        if (jobRewards.remove(jobId) != null) {
            dirtyPlayers.add(playerId);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info(String.format("Reset all rewards for job %s for player %s", 
                        jobId, playerId));
            }
        }
    }
    
    @Override
    public void resetAllRewards(UUID playerId) {
        if (playerRewards.remove(playerId) != null) {
            dirtyPlayers.add(playerId);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info(String.format("Reset all rewards for player %s", playerId));
            }
        }
    }
    
    @Override
    public void save() {
        if (dirtyPlayers.isEmpty()) return;
        
        Set<UUID> toSave = new HashSet<>(dirtyPlayers);
        dirtyPlayers.clear();
        
        for (UUID playerId : toSave) {
            savePlayerData(playerId);
        }
    }
    
    @Override
    public void loadPlayerData(UUID playerId) {
        if (playerRewards.containsKey(playerId)) {
            return; // Already loaded
        }
        
        File playerFile = new File(dataFolder, playerId.toString() + ".yml");
        if (!playerFile.exists()) {
            playerRewards.put(playerId, new ConcurrentHashMap<>());
            return;
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            Map<String, Map<String, Long>> jobRewards = new ConcurrentHashMap<>();
            
            for (String jobId : config.getKeys(false)) {
                if (config.isConfigurationSection(jobId)) {
                    Map<String, Long> rewards = new ConcurrentHashMap<>();
                    
                    for (String rewardId : config.getConfigurationSection(jobId).getKeys(false)) {
                        long claimTime = config.getLong(jobId + "." + rewardId, System.currentTimeMillis());
                        rewards.put(rewardId, claimTime);
                    }
                    
                    jobRewards.put(jobId, rewards);
                }
            }
            
            playerRewards.put(playerId, jobRewards);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Loaded reward data for player: " + playerId);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load reward data for player " + playerId, e);
            playerRewards.put(playerId, new ConcurrentHashMap<>());
        }
    }
    
    @Override
    public void unloadPlayerData(UUID playerId) {
        // Save if dirty
        if (dirtyPlayers.contains(playerId)) {
            savePlayerData(playerId);
            dirtyPlayers.remove(playerId);
        }
        
        // Remove from memory
        playerRewards.remove(playerId);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Unloaded reward data for player: " + playerId);
        }
    }
    
    /**
     * Save a specific player's data to file.
     * 
     * @param playerId The player's UUID
     */
    private void savePlayerData(UUID playerId) {
        Map<String, Map<String, Long>> jobRewards = playerRewards.get(playerId);
        if (jobRewards == null || jobRewards.isEmpty()) {
            // Delete file if no rewards
            File playerFile = new File(dataFolder, playerId.toString() + ".yml");
            if (playerFile.exists()) {
                if (playerFile.delete()) {
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Deleted empty reward file for player: " + playerId);
                    }
                }
            }
            return;
        }
        
        File playerFile = new File(dataFolder, playerId.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<String, Map<String, Long>> jobEntry : jobRewards.entrySet()) {
            String jobId = jobEntry.getKey();
            Map<String, Long> rewards = jobEntry.getValue();
            
            for (Map.Entry<String, Long> rewardEntry : rewards.entrySet()) {
                String rewardId = rewardEntry.getKey();
                long claimTime = rewardEntry.getValue();
                config.set(jobId + "." + rewardId, claimTime);
            }
        }
        
        try {
            config.save(playerFile);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Saved reward data for player: " + playerId);
            }
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save reward data for player " + playerId, e);
        }
    }
}