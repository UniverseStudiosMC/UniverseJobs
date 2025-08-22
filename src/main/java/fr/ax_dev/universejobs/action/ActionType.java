package fr.ax_dev.universejobs.action;

/**
 * Enumeration of all supported action types in the jobs system.
 * Each action type corresponds to a specific player activity that can award XP.
 */
public enum ActionType {
    
    /**
     * Killing entities (mobs, players, MythicMobs, etc.)
     */
    KILL,
    
    /**
     * Placing blocks
     */
    PLACE,
    
    /**
     * Breaking blocks
     */
    BREAK,
    
    /**
     * Harvesting crops (including CustomCrops)
     */
    HARVEST,
    
    /**
     * Interacting with blocks
     */
    BLOCK_INTERACT,
    
    /**
     * Interacting with entities
     */
    ENTITY_INTERACT,
    
    /**
     * Breeding animals
     */
    BREED,
    
    /**
     * Fishing
     */
    FISH,
    
    /**
     * Crafting items
     */
    CRAFT,
    
    /**
     * Smelting items
     */
    SMELT,
    
    /**
     * Enchanting items
     */
    ENCHANT,
    
    /**
     * Trading with villagers
     */
    TRADE,
    
    /**
     * Taming animals
     */
    TAME,
    
    /**
     * Shearing sheep
     */
    SHEAR,
    
    /**
     * Milking cows
     */
    MILK,
    
    /**
     * Eating food
     */
    EAT,
    
    /**
     * Drinking potions
     */
    POTION,
    
    /**
     * Custom action for plugin integrations
     */
    CUSTOM;
    
    /**
     * Get an ActionType from a string, case-insensitive.
     * 
     * @param name The name of the action type
     * @return The ActionType or null if not found
     */
    public static ActionType fromString(String name) {
        if (name == null) return null;
        
        try {
            return ActionType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Check if this action type is valid.
     * 
     * @return true if this is a valid action type
     */
    public boolean isValid() {
        return this != null;
    }
}