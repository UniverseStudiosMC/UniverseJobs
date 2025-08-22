package fr.ax_dev.universejobs.bonus;

import java.util.UUID;

/**
 * Represents a temporary money bonus for a player.
 */
public class MoneyBonus extends BaseBonus {
    
    /**
     * Create a new money bonus.
     * 
     * @param playerId The player UUID
     * @param jobId The job ID (null for all jobs)
     * @param multiplier The money multiplier (1.5 = 50% bonus)
     * @param duration Duration in seconds
     * @param reason Reason for the bonus
     * @param grantedBy Who granted the bonus
     */
    public MoneyBonus(UUID playerId, String jobId, double multiplier, long duration, String reason, String grantedBy) {
        super(playerId, jobId, multiplier, duration, reason, grantedBy);
    }
    
    /**
     * Create a global money bonus for all jobs.
     * 
     * @param playerId The player UUID
     * @param multiplier The money multiplier
     * @param duration Duration in seconds
     * @param reason Reason for the bonus
     * @param grantedBy Who granted the bonus
     * @return The money bonus
     */
    public static MoneyBonus createGlobalBonus(UUID playerId, double multiplier, long duration, String reason, String grantedBy) {
        return new MoneyBonus(playerId, null, multiplier, duration, reason, grantedBy);
    }
    
    /**
     * Create a job-specific money bonus.
     * 
     * @param playerId The player UUID
     * @param jobId The specific job ID
     * @param multiplier The money multiplier
     * @param duration Duration in seconds
     * @param reason Reason for the bonus
     * @param grantedBy Who granted the bonus
     * @return The money bonus
     */
    public static MoneyBonus createJobBonus(UUID playerId, String jobId, double multiplier, long duration, String reason, String grantedBy) {
        return new MoneyBonus(playerId, jobId, multiplier, duration, reason, grantedBy);
    }
    
    
    @Override
    protected String getBonusTypeName() {
        return "MoneyBonus";
    }
}