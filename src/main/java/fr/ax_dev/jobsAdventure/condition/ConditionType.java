package fr.ax_dev.jobsAdventure.condition;

/**
 * Enumeration of all supported condition types.
 */
public enum ConditionType {
    
    /**
     * PlaceholderAPI placeholder condition
     */
    PLACEHOLDER,
    
    /**
     * Permission check condition
     */
    PERMISSION,
    
    /**
     * Item in hand condition
     */
    ITEM,
    
    /**
     * World condition
     */
    WORLD,
    
    /**
     * Region condition (WorldGuard integration)
     */
    REGION,
    
    /**
     * Time condition (in-game time)
     */
    TIME,
    
    /**
     * Weather condition
     */
    WEATHER,
    
    /**
     * Biome condition
     */
    BIOME,
    
    /**
     * Custom condition for plugin integrations
     */
    CUSTOM;
    
    /**
     * Get a ConditionType from string, case-insensitive.
     * 
     * @param name The condition type name
     * @return The ConditionType or null if not found
     */
    public static ConditionType fromString(String name) {
        if (name == null) return null;
        
        try {
            return ConditionType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}