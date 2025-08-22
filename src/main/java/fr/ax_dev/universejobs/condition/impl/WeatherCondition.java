package fr.ax_dev.universejobs.condition.impl;

import fr.ax_dev.universejobs.condition.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Condition that checks the weather in the player's world.
 */
public class WeatherCondition extends AbstractCondition {
    
    public enum WeatherType {
        CLEAR, RAIN, THUNDER
    }
    
    private final WeatherType requiredWeather;
    
    /**
     * Create a weather condition from configuration.
     * 
     * @param config The configuration section
     */
    public WeatherCondition(ConfigurationSection config) {
        super(config);
        String weatherStr = config.getString("weather", "CLEAR");
        WeatherType tempWeather;
        try {
            tempWeather = WeatherType.valueOf(weatherStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            tempWeather = WeatherType.CLEAR;
        }
        this.requiredWeather = tempWeather;
    }
    
    @Override
    public boolean isMet(Player player, Event event, ConditionContext context) {
        WeatherType currentWeather = getCurrentWeather(player);
        return currentWeather == requiredWeather;
    }
    
    /**
     * Get the current weather type for the player's world.
     * 
     * @param player The player
     * @return The current weather type
     */
    private WeatherType getCurrentWeather(Player player) {
        if (player.getWorld().isThundering()) {
            return WeatherType.THUNDER;
        } else if (player.getWorld().hasStorm()) {
            return WeatherType.RAIN;
        } else {
            return WeatherType.CLEAR;
        }
    }
    
    @Override
    public ConditionType getType() {
        return ConditionType.WEATHER;
    }
    
    
    @Override
    public String toString() {
        return "WeatherCondition{requiredWeather=" + requiredWeather + "}";
    }
}