package fr.ax_dev.universejobs.condition;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Base interface for all condition types.
 * Conditions are used to determine if an action should award XP.
 */
public interface Condition {
    
    /**
     * Check if this condition is met.
     * 
     * @param player The player performing the action
     * @param event The event that triggered the action (optional)
     * @param context Additional context data (e.g., block type, entity type)
     * @return true if the condition is met
     */
    boolean isMet(Player player, Event event, ConditionContext context);
    
    /**
     * Get the type of this condition.
     * 
     * @return The condition type
     */
    ConditionType getType();
    
    /**
     * Get the result when this condition is not met.
     * 
     * @return The condition result for denial
     */
    ConditionResult getDenyResult();
    
    /**
     * Get the result when this condition is met.
     * 
     * @return The condition result for acceptance (may be null)
     */
    default ConditionResult getAcceptResult() {
        return null;
    }
}