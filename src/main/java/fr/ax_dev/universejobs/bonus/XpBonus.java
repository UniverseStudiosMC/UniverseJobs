package fr.ax_dev.universejobs.bonus;

import java.util.UUID;

/**
 * Represents a temporary XP bonus for a player.
 */
public class XpBonus extends BaseBonus {
    
    /**
     * Create a new XP bonus.
     * 
     * @param playerId The player UUID
     * @param jobId The job ID (null for all jobs)
     * @param multiplier The XP multiplier (1.5 = 50% bonus)
     * @param duration Duration in seconds
     * @param reason Reason for the bonus
     * @param grantedBy Who granted the bonus
     */
    public XpBonus(UUID playerId, String jobId, double multiplier, long duration, String reason, String grantedBy) {
        super(playerId, jobId, multiplier, duration, reason, grantedBy);
    }
    
    /**
     * Create a global XP bonus for all jobs.
     * 
     * @param playerId The player UUID
     * @param multiplier The XP multiplier
     * @param duration Duration in seconds
     * @param reason Reason for the bonus
     * @param grantedBy Who granted the bonus
     * @return The XP bonus
     */
    public static XpBonus createGlobalBonus(UUID playerId, double multiplier, long duration, String reason, String grantedBy) {
        return new XpBonus(playerId, null, multiplier, duration, reason, grantedBy);
    }
    
    /**
     * Create a job-specific XP bonus.
     * 
     * @param playerId The player UUID
     * @param jobId The specific job ID
     * @param multiplier The XP multiplier
     * @param duration Duration in seconds
     * @param reason Reason for the bonus
     * @param grantedBy Who granted the bonus
     * @return The XP bonus
     */
    public static XpBonus createJobBonus(UUID playerId, String jobId, double multiplier, long duration, String reason, String grantedBy) {
        return new XpBonus(playerId, jobId, multiplier, duration, reason, grantedBy);
    }
    
    
    @Override
    protected String getBonusTypeName() {
        return "XpBonus";
    }
}