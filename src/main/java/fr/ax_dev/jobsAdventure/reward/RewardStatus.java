package fr.ax_dev.jobsAdventure.reward;

/**
 * Represents the different states a reward can be in for a player.
 */
public enum RewardStatus {
    
    /**
     * The reward can be claimed by the player.
     * Player meets all requirements and hasn't claimed it yet.
     */
    RETRIEVABLE("&a✓", "&aClick to claim!"),
    
    /**
     * The reward is blocked/locked for the player.
     * Player doesn't meet the requirements yet.
     */
    BLOCKED("&c✗", "&cRequirements not met"),
    
    /**
     * The reward has already been retrieved/claimed.
     * Player cannot claim it again.
     */
    RETRIEVED("&8✓", "&7Already claimed");
    
    private final String indicator;
    private final String description;
    
    /**
     * Create a new RewardStatus.
     * 
     * @param indicator The visual indicator shown in GUI
     * @param description The description shown to players
     */
    RewardStatus(String indicator, String description) {
        this.indicator = indicator;
        this.description = description;
    }
    
    /**
     * Get the visual indicator for this status.
     * 
     * @return The colored indicator text
     */
    public String getIndicator() {
        return indicator;
    }
    
    /**
     * Get the description for this status.
     * 
     * @return The status description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this reward can be claimed.
     * 
     * @return true if the reward is retrievable
     */
    public boolean canClaim() {
        return this == RETRIEVABLE;
    }
    
    /**
     * Check if this reward has been claimed.
     * 
     * @return true if the reward is retrieved
     */
    public boolean isClaimed() {
        return this == RETRIEVED;
    }
    
    /**
     * Check if this reward is blocked.
     * 
     * @return true if the reward is blocked
     */
    public boolean isBlocked() {
        return this == BLOCKED;
    }
}