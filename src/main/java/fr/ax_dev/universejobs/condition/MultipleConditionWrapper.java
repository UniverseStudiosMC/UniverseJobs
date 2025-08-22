package fr.ax_dev.universejobs.condition;

import fr.ax_dev.universejobs.condition.impl.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Wrapper for conditions within a multiple_condition structure.
 * Handles individual condition logic while supporting global accept/deny messages.
 */
public class MultipleConditionWrapper implements Condition {
    
    private final String conditionName;
    private final Condition wrappedCondition;
    private final ConditionResult globalAcceptResult;
    private final ConditionResult globalDenyResult;
    
    /**
     * Create a wrapper for a condition within multiple_condition structure.
     * 
     * @param conditionName The name of this condition
     * @param conditionConfig The configuration for this specific condition
     * @param globalAccept Global accept configuration (can be null)
     * @param globalDeny Global deny configuration (can be null)
     */
    public MultipleConditionWrapper(String conditionName, ConfigurationSection conditionConfig, 
                                    ConfigurationSection globalAccept, ConfigurationSection globalDeny) {
        this.conditionName = conditionName;
        this.wrappedCondition = createCondition(conditionName, conditionConfig);
        
        // Create global results if configured
        this.globalAcceptResult = globalAccept != null ? new ConditionResult(globalAccept, true) : null;
        this.globalDenyResult = globalDeny != null ? new ConditionResult(globalDeny, false) : null;
    }
    
    /**
     * Create a condition instance from configuration.
     * 
     * @param type The condition type string
     * @param config The condition configuration
     * @return The condition instance or null if invalid
     */
    private Condition createCondition(String type, ConfigurationSection config) {
        ConditionType conditionType = ConditionType.fromString(type);
        if (conditionType == null) return null;
        
        return switch (conditionType) {
            case PLACEHOLDER -> new PlaceholderCondition(config);
            case PERMISSION -> new PermissionCondition(config);
            case ITEM -> new ItemCondition(config);
            case WORLD -> new WorldCondition(config);
            case TIME -> new TimeCondition(config);
            case WEATHER -> new WeatherCondition(config);
            case BIOME -> new BiomeCondition(config);
            default -> null;
        };
    }
    
    @Override
    public boolean isMet(Player player, Event event, ConditionContext context) {
        if (wrappedCondition == null) return false;
        return wrappedCondition.isMet(player, event, context);
    }
    
    @Override
    public ConditionResult getAcceptResult() {
        // Use global accept result if available, otherwise use wrapped condition's result
        if (globalAcceptResult != null) {
            return globalAcceptResult;
        }
        return wrappedCondition != null ? wrappedCondition.getAcceptResult() : ConditionResult.allow();
    }
    
    @Override
    public ConditionResult getDenyResult() {
        // Use global deny result if available (this takes priority for multiple_condition)
        if (globalDenyResult != null) {
            return globalDenyResult;
        }
        
        // Fall back to wrapped condition's deny result
        if (wrappedCondition != null) {
            ConditionResult wrappedDeny = wrappedCondition.getDenyResult();
            if (wrappedDeny != null && !wrappedDeny.isDefault()) {
                return wrappedDeny;
            }
        }
        
        return ConditionResult.deny();
    }
    
    @Override
    public ConditionType getType() {
        return wrappedCondition != null ? wrappedCondition.getType() : ConditionType.CUSTOM;
    }
    
    @Override
    public String toString() {
        return "MultipleConditionWrapper{name='" + conditionName + "', wrapped=" + wrappedCondition + "}";
    }
}