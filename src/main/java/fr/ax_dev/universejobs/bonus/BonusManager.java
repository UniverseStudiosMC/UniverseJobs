package fr.ax_dev.universejobs.bonus;

import java.util.List;
import java.util.UUID;

/**
 * Common interface for bonus managers.
 */
public interface BonusManager<T> {
    
    /**
     * Add a global bonus for all online players.
     */
    int addGlobalBonus(double multiplier, long duration, String reason, String grantedBy);
    
    /**
     * Add a bonus for a specific player for all jobs.
     */
    void addPlayerBonus(UUID playerId, double multiplier, long duration, String reason, String grantedBy);
    
    /**
     * Add a bonus for a specific player and job.
     */
    void addJobBonus(UUID playerId, String jobId, double multiplier, long duration, String reason, String grantedBy);
    
    /**
     * Remove all bonuses for a player.
     */
    int removeAllBonuses(UUID playerId);
    
    /**
     * Remove a specific bonus.
     */
    boolean removeBonus(T bonus);
    
    /**
     * Get all active bonuses for a player.
     */
    List<T> getActiveBonuses(UUID playerId);
    
    /**
     * Get active bonuses for a player and specific job.
     */
    List<T> getActiveBonuses(UUID playerId, String jobId);
    
    /**
     * Get bonus manager statistics.
     */
    java.util.Map<String, Object> getStats();
    
    /**
     * Clean up expired bonuses.
     */
    void cleanupExpiredBonuses();
}