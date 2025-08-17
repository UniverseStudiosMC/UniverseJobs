package fr.ax_dev.jobsAdventure.reward.storage;

import java.util.Set;
import java.util.UUID;

/**
 * Interface for storing and retrieving player reward claim data.
 * Supports different storage backends (file, database, etc.).
 */
public interface RewardStorage {
    
    /**
     * Initialize the storage system.
     * Creates necessary tables, files, or connections.
     */
    void initialize();
    
    /**
     * Shutdown the storage system.
     * Closes connections and saves any pending data.
     */
    void shutdown();
    
    /**
     * Check if a player has claimed a specific reward.
     * 
     * @param playerId The player's UUID
     * @param jobId The job ID
     * @param rewardId The reward ID
     * @return true if the reward has been claimed
     */
    boolean hasClaimedReward(UUID playerId, String jobId, String rewardId);
    
    /**
     * Mark a reward as claimed for a player.
     * 
     * @param playerId The player's UUID
     * @param jobId The job ID
     * @param rewardId The reward ID
     * @param claimTime The timestamp when claimed
     */
    void claimReward(UUID playerId, String jobId, String rewardId, long claimTime);
    
    /**
     * Get the timestamp when a reward was claimed.
     * 
     * @param playerId The player's UUID
     * @param jobId The job ID
     * @param rewardId The reward ID
     * @return The claim timestamp or -1 if not claimed
     */
    long getClaimTime(UUID playerId, String jobId, String rewardId);
    
    /**
     * Get all claimed rewards for a player in a specific job.
     * 
     * @param playerId The player's UUID
     * @param jobId The job ID
     * @return Set of claimed reward IDs
     */
    Set<String> getClaimedRewards(UUID playerId, String jobId);
    
    /**
     * Get all claimed rewards for a player across all jobs.
     * 
     * @param playerId The player's UUID
     * @return Set of "jobId:rewardId" strings
     */
    Set<String> getAllClaimedRewards(UUID playerId);
    
    /**
     * Reset a specific reward claim for a player.
     * Used for repeatable rewards or admin commands.
     * 
     * @param playerId The player's UUID
     * @param jobId The job ID
     * @param rewardId The reward ID
     */
    void resetRewardClaim(UUID playerId, String jobId, String rewardId);
    
    /**
     * Reset all reward claims for a player in a specific job.
     * 
     * @param playerId The player's UUID
     * @param jobId The job ID
     */
    void resetJobRewards(UUID playerId, String jobId);
    
    /**
     * Reset all reward claims for a player.
     * 
     * @param playerId The player's UUID
     */
    void resetAllRewards(UUID playerId);
    
    /**
     * Save any pending data to storage.
     * Used for async operations and periodic saves.
     */
    void save();
    
    /**
     * Load player data from storage.
     * Called when a player joins the server.
     * 
     * @param playerId The player's UUID
     */
    void loadPlayerData(UUID playerId);
    
    /**
     * Unload player data from memory.
     * Called when a player leaves the server.
     * 
     * @param playerId The player's UUID
     */
    void unloadPlayerData(UUID playerId);
}