package fr.ax_dev.jobsAdventure.condition.impl;

import fr.ax_dev.jobsAdventure.condition.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Condition that checks the in-game time.
 */
public class TimeCondition extends AbstractCondition {
    
    private final long minTime;
    private final long maxTime;
    
    /**
     * Create a time condition from configuration.
     * 
     * @param config The configuration section
     */
    public TimeCondition(ConfigurationSection config) {
        super(config);
        this.minTime = config.getLong("min", 0);
        this.maxTime = config.getLong("max", 24000);
    }
    
    @Override
    public boolean isMet(Player player, Event event, ConditionContext context) {
        long worldTime = player.getWorld().getTime();
        
        if (minTime <= maxTime) {
            // Normal range (e.g., 6000 to 18000)
            return worldTime >= minTime && worldTime <= maxTime;
        } else {
            // Wrapping range (e.g., 18000 to 6000 - night time)
            return worldTime >= minTime || worldTime <= maxTime;
        }
    }
    
    @Override
    public ConditionType getType() {
        return ConditionType.TIME;
    }
    
    
    @Override
    public String toString() {
        return "TimeCondition{minTime=" + minTime + ", maxTime=" + maxTime + "}";
    }
}