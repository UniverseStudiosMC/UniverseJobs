package fr.ax_dev.jobsAdventure.condition;

import fr.ax_dev.jobsAdventure.condition.impl.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * A group of conditions that can be evaluated with AND/OR logic.
 * Supports nested condition groups for complex requirement structures.
 */
public class ConditionGroup {
    
    /**
     * Logic operator for combining conditions.
     */
    public enum Logic {
        AND, OR
    }
    
    private final Logic logic;
    private final List<Condition> conditions = new ArrayList<>();
    private final List<ConditionGroup> subGroups = new ArrayList<>();
    
    /**
     * Create a condition group from configuration.
     * 
     * @param config The configuration section
     */
    public ConditionGroup(ConfigurationSection config) {
        // Determine logic operator (default to AND)
        String logicStr = config.getString("logic", "AND");
        this.logic = Logic.valueOf(logicStr.toUpperCase());
        
        // Load individual conditions
        loadConditions(config);
        
        // Load sub-groups
        loadSubGroups(config);
    }
    
    /**
     * Create a condition group with specified logic.
     * 
     * @param logic The logic operator
     */
    public ConditionGroup(Logic logic) {
        this.logic = logic;
    }
    
    /**
     * Load individual conditions from configuration.
     * 
     * @param config The configuration section
     */
    private void loadConditions(ConfigurationSection config) {
        for (String key : config.getKeys(false)) {
            if (key.equals("logic") || key.equals("groups") || key.equals("multiple_condition")) continue;
            
            Object value = config.get(key);
            if (value instanceof ConfigurationSection conditionConfig) {
                Condition condition = createCondition(key, conditionConfig);
                if (condition != null) {
                    conditions.add(condition);
                }
            }
        }
        
        // Handle multiple_condition structure
        ConfigurationSection multipleCondition = config.getConfigurationSection("multiple_condition");
        if (multipleCondition != null) {
            loadMultipleConditions(multipleCondition);
        }
    }
    
    /**
     * Load multiple conditions from the multiple_condition section.
     * This allows for named conditions with individual accept/deny messages.
     * 
     * @param multipleCondition The multiple_condition configuration section
     */
    private void loadMultipleConditions(ConfigurationSection multipleCondition) {
        for (String conditionName : multipleCondition.getKeys(false)) {
            if (conditionName.equals("accept") || conditionName.equals("deny")) continue;
            
            ConfigurationSection conditionConfig = multipleCondition.getConfigurationSection(conditionName);
            if (conditionConfig != null) {
                // Create a wrapper condition that includes the global accept/deny messages
                MultipleConditionWrapper wrapper = new MultipleConditionWrapper(
                    conditionName, 
                    conditionConfig, 
                    multipleCondition.getConfigurationSection("accept"),
                    multipleCondition.getConfigurationSection("deny")
                );
                conditions.add(wrapper);
            }
        }
    }
    
    /**
     * Load sub-groups from configuration.
     * 
     * @param config The configuration section
     */
    private void loadSubGroups(ConfigurationSection config) {
        ConfigurationSection groupsSection = config.getConfigurationSection("groups");
        if (groupsSection != null) {
            for (String groupKey : groupsSection.getKeys(false)) {
                ConfigurationSection groupConfig = groupsSection.getConfigurationSection(groupKey);
                if (groupConfig != null) {
                    subGroups.add(new ConditionGroup(groupConfig));
                }
            }
        }
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
    
    /**
     * Add a condition to this group.
     * 
     * @param condition The condition to add
     */
    public void addCondition(Condition condition) {
        conditions.add(condition);
    }
    
    /**
     * Add a sub-group to this group.
     * 
     * @param group The sub-group to add
     */
    public void addSubGroup(ConditionGroup group) {
        subGroups.add(group);
    }
    
    /**
     * Evaluate all conditions in this group.
     * 
     * @param player The player to evaluate for
     * @param event The event context
     * @param context The condition context
     * @return The evaluation result
     */
    public ConditionResult evaluate(Player player, Event event, ConditionContext context) {
        if (conditions.isEmpty() && subGroups.isEmpty()) {
            return ConditionResult.allow();
        }
        
        boolean result = (logic == Logic.AND);
        ConditionResult denyResult = null;
        ConditionResult acceptResult = null;
        
        // Evaluate individual conditions
        for (Condition condition : conditions) {
            boolean conditionMet = condition.isMet(player, event, context);
            
            if (logic == Logic.AND) {
                if (!conditionMet) {
                    denyResult = condition.getDenyResult();
                    result = false;
                    break; // Short-circuit on first failure
                } else if (acceptResult == null) {
                    // Collect accept result from first met condition
                    acceptResult = condition.getAcceptResult();
                }
            } else { // OR logic
                if (conditionMet) {
                    // For OR logic, use accept result from the condition that passed
                    acceptResult = condition.getAcceptResult();
                    result = true;
                    break; // Short-circuit on first success
                } else if (denyResult == null) {
                    denyResult = condition.getDenyResult();
                }
            }
        }
        
        // Evaluate sub-groups if needed
        if ((logic == Logic.AND && result) || (logic == Logic.OR && !result)) {
            for (ConditionGroup subGroup : subGroups) {
                ConditionResult subResult = subGroup.evaluate(player, event, context);
                
                if (logic == Logic.AND) {
                    if (!subResult.isAllowed()) {
                        return subResult; // Return the denial from sub-group
                    } else if (acceptResult == null && subResult.isAllowed()) {
                        // Use accept result from sub-group if we don't have one yet
                        acceptResult = subResult;
                    }
                } else { // OR logic
                    if (subResult.isAllowed()) {
                        acceptResult = subResult;
                        result = true;
                        break; // Short-circuit on first success
                    } else if (denyResult == null) {
                        denyResult = subResult;
                    }
                }
            }
        }
        
        if (result) {
            // Return accept result if available, otherwise return generic allow
            return acceptResult != null ? acceptResult : ConditionResult.allow();
        } else {
            return denyResult != null ? denyResult : ConditionResult.deny();
        }
    }
    
    /**
     * Check if this group has any conditions.
     * 
     * @return true if there are conditions or sub-groups
     */
    public boolean hasConditions() {
        return !conditions.isEmpty() || !subGroups.isEmpty();
    }
    
    /**
     * Get the logic operator for this group.
     * 
     * @return The logic operator
     */
    public Logic getLogic() {
        return logic;
    }
    
    @Override
    public String toString() {
        return "ConditionGroup{logic=" + logic + ", conditions=" + conditions.size() + 
               ", subGroups=" + subGroups.size() + "}";
    }
}