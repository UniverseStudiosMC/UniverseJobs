package fr.ax_dev.jobsAdventure.condition.impl;

import fr.ax_dev.jobsAdventure.condition.*;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Condition that checks PlaceholderAPI placeholders against values.
 */
public class PlaceholderCondition extends AbstractCondition {
    
    private final String placeholder;
    private final String operator;
    private final String value;
    
    /**
     * Create a placeholder condition from configuration.
     * 
     * @param config The configuration section
     */
    public PlaceholderCondition(ConfigurationSection config) {
        super(config);
        this.placeholder = config.getString("placeholder", "");
        this.operator = config.getString("operator", "equals");
        this.value = config.getString("value", "");
    }
    
    @Override
    public boolean isMet(Player player, Event event, ConditionContext context) {
        // Check if PlaceholderAPI is available
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return false;
        }
        
        // Parse placeholder
        String parsedValue = PlaceholderAPI.setPlaceholders(player, placeholder);
        
        // Compare based on operator
        return switch (operator.toLowerCase()) {
            case "equals", "=" -> parsedValue.equals(value);
            case "not_equals", "!=" -> !parsedValue.equals(value);
            case "contains" -> parsedValue.toLowerCase().contains(value.toLowerCase());
            case "not_contains" -> !parsedValue.toLowerCase().contains(value.toLowerCase());
            case "starts_with" -> parsedValue.toLowerCase().startsWith(value.toLowerCase());
            case "ends_with" -> parsedValue.toLowerCase().endsWith(value.toLowerCase());
            case "greater_than", ">" -> compareNumeric(parsedValue, value) > 0;
            case "less_than", "<" -> compareNumeric(parsedValue, value) < 0;
            case "greater_equal", ">=" -> compareNumeric(parsedValue, value) >= 0;
            case "less_equal", "<=" -> compareNumeric(parsedValue, value) <= 0;
            case "regex" -> parsedValue.matches(value);
            default -> false;
        };
    }
    
    /**
     * Compare two values numerically.
     * 
     * @param value1 First value
     * @param value2 Second value
     * @return Comparison result (-1, 0, 1)
     */
    private int compareNumeric(String value1, String value2) {
        try {
            double num1 = Double.parseDouble(value1);
            double num2 = Double.parseDouble(value2);
            return Double.compare(num1, num2);
        } catch (NumberFormatException e) {
            // Fall back to string comparison
            return value1.compareTo(value2);
        }
    }
    
    @Override
    public ConditionType getType() {
        return ConditionType.PLACEHOLDER;
    }
    
    
    @Override
    public String toString() {
        return "PlaceholderCondition{placeholder='" + placeholder + "', operator='" + operator + 
               "', value='" + value + "'}";
    }
}