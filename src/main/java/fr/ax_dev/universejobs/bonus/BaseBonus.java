package fr.ax_dev.universejobs.bonus;

import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all bonus types to eliminate code duplication.
 */
public abstract class BaseBonus {
    
    protected final UUID playerId;
    protected final String jobId; // null for all jobs
    protected final double multiplier;
    protected final long startTime;
    protected final long duration; // in milliseconds
    protected final String reason;
    protected final String grantedBy;
    
    /**
     * Bonus type enum.
     */
    public enum BonusType {
        GLOBAL,     // Applies to all jobs
        SPECIFIC    // Applies to specific job only
    }
    
    /**
     * Create a new bonus.
     * 
     * @param playerId The player UUID
     * @param jobId The job ID (null for all jobs)
     * @param multiplier The multiplier (1.5 = 50% bonus)
     * @param duration Duration in seconds
     * @param reason Reason for the bonus
     * @param grantedBy Who granted the bonus
     */
    protected BaseBonus(UUID playerId, String jobId, double multiplier, long duration, String reason, String grantedBy) {
        this.playerId = playerId;
        this.jobId = jobId;
        this.multiplier = multiplier;
        this.startTime = System.currentTimeMillis();
        this.duration = duration * 1000; // Convert to milliseconds
        this.reason = reason;
        this.grantedBy = grantedBy;
    }
    
    /**
     * Check if this bonus is still active.
     * 
     * @return true if the bonus is still active
     */
    public boolean isActive() {
        return System.currentTimeMillis() < (startTime + duration);
    }
    
    /**
     * Check if this bonus applies to a specific job.
     * 
     * @param jobId The job ID to check
     * @return true if the bonus applies
     */
    public boolean appliesTo(String jobId) {
        return this.jobId == null || this.jobId.equals(jobId);
    }
    
    /**
     * Get the remaining time in seconds.
     * 
     * @return Remaining time in seconds
     */
    public long getRemainingTime() {
        long remaining = (startTime + duration) - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
    
    /**
     * Get the remaining time formatted as a string.
     * 
     * @return Formatted remaining time
     */
    public String getRemainingTimeFormatted() {
        long remainingSeconds = getRemainingTime();
        
        if (remainingSeconds <= 0) {
            return "Expired";
        }
        
        long hours = remainingSeconds / 3600;
        long minutes = (remainingSeconds % 3600) / 60;
        long seconds = remainingSeconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Get the bonus type.
     * 
     * @return The bonus type
     */
    public BonusType getType() {
        return jobId == null ? BonusType.GLOBAL : BonusType.SPECIFIC;
    }
    
    /**
     * Get the player UUID.
     * 
     * @return The player UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Get the job ID (null for global bonus).
     * 
     * @return The job ID or null
     */
    public String getJobId() {
        return jobId;
    }
    
    /**
     * Get the multiplier.
     * 
     * @return The multiplier
     */
    public double getMultiplier() {
        return multiplier;
    }
    
    /**
     * Get the bonus percentage.
     * 
     * @return The bonus percentage (e.g., 50 for 50% bonus)
     */
    public int getBonusPercentage() {
        return (int) Math.round((multiplier - 1.0) * 100);
    }
    
    /**
     * Get the start time.
     * 
     * @return The start time in milliseconds
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * Get the duration.
     * 
     * @return The duration in milliseconds
     */
    public long getDuration() {
        return duration;
    }
    
    /**
     * Get the reason for this bonus.
     * 
     * @return The reason
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * Get who granted this bonus.
     * 
     * @return The granter
     */
    public String getGrantedBy() {
        return grantedBy;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BaseBonus baseBonus = (BaseBonus) obj;
        return Objects.equals(playerId, baseBonus.playerId) &&
               Objects.equals(jobId, baseBonus.jobId) &&
               startTime == baseBonus.startTime;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(playerId, jobId, startTime);
    }
    
    /**
     * Get the bonus type name for display purposes.
     * Subclasses should override this.
     * 
     * @return The bonus type name
     */
    protected abstract String getBonusTypeName();
    
    @Override
    public String toString() {
        return getBonusTypeName() + "{" +
               "playerId=" + playerId +
               ", jobId='" + jobId + '\'' +
               ", multiplier=" + multiplier +
               ", remaining=" + getRemainingTimeFormatted() +
               ", reason='" + reason + '\'' +
               '}';
    }
}