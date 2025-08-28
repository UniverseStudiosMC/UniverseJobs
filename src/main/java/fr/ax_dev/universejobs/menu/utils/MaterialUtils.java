package fr.ax_dev.universejobs.menu.utils;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * Utility class for determining materials based on action targets.
 */
public class MaterialUtils {
    
    /**
     * Get the appropriate material for an action target.
     * For blocks, returns the block material.
     * For mobs, returns the spawn egg or special materials.
     * For MythicMobs, returns WITHER_SKELETON_SKULL.
     * Target names are case-insensitive.
     * 
     * @param target The action target (case-insensitive)
     * @return The material to use for the item
     */
    public static Material getMaterialForTarget(String target) {
        if (target == null || target.isEmpty()) {
            return Material.STONE;
        }
        
        // Normalize target to handle case-insensitivity
        String normalizedTarget = target.trim();
        
        // Handle MythicMobs (contains colon)
        if (normalizedTarget.contains(":")) {
            String[] parts = normalizedTarget.split(":", 2); // Limit to 2 parts for safety
            String namespace = parts[0].toLowerCase();
            
            // MythicMobs detection
            if ("mythicmobs".equals(namespace) || "mm".equals(namespace)) {
                return Material.WITHER_SKELETON_SKULL;
            }
            
            // CustomCrops or other plugins - try to get material from the second part
            if (parts.length > 1) {
                String itemName = parts[1];
                
                // First try direct material lookup
                try {
                    return Material.valueOf(itemName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Try entity lookup for namespaced entities
                    Material spawnEgg = getSpawnEggForEntity(itemName);
                    if (spawnEgg != null) {
                        return spawnEgg;
                    }
                    
                    // Fallback for custom items
                    return getGenericMaterialForNamespace(namespace);
                }
            }
        }
        
        // Try to get material directly (for blocks) - case insensitive
        try {
            return Material.valueOf(normalizedTarget.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Not a valid material, might be an entity
        }
        
        // Try to get spawn egg for entities - case insensitive
        Material spawnEgg = getSpawnEggForEntity(normalizedTarget);
        if (spawnEgg != null) {
            return spawnEgg;
        }
        
        // Fallback to stone
        return Material.STONE;
    }
    
    /**
     * Get spawn egg material for an entity type.
     * Entity names are case-insensitive.
     * 
     * @param entityName The entity name (case-insensitive)
     * @return The spawn egg material or null if not found
     */
    private static Material getSpawnEggForEntity(String entityName) {
        if (entityName == null || entityName.trim().isEmpty()) {
            return null;
        }
        
        try {
            EntityType entityType = EntityType.valueOf(entityName.trim().toUpperCase());
            
            // Map entity types to their spawn eggs
            return switch (entityType) {
                case ZOMBIE -> Material.ZOMBIE_SPAWN_EGG;
                case SKELETON -> Material.SKELETON_SPAWN_EGG;
                case CREEPER -> Material.CREEPER_SPAWN_EGG;
                case SPIDER -> Material.SPIDER_SPAWN_EGG;
                case ENDERMAN -> Material.ENDERMAN_SPAWN_EGG;
                case WITCH -> Material.WITCH_SPAWN_EGG;
                case BLAZE -> Material.BLAZE_SPAWN_EGG;
                case GHAST -> Material.GHAST_SPAWN_EGG;
                case SLIME -> Material.SLIME_SPAWN_EGG;
                case MAGMA_CUBE -> Material.MAGMA_CUBE_SPAWN_EGG;
                case ENDER_DRAGON -> Material.DRAGON_EGG;
                case WITHER -> Material.WITHER_SKELETON_SKULL;
                case COW -> Material.COW_SPAWN_EGG;
                case PIG -> Material.PIG_SPAWN_EGG;
                case SHEEP -> Material.SHEEP_SPAWN_EGG;
                case CHICKEN -> Material.CHICKEN_SPAWN_EGG;
                case HORSE -> Material.HORSE_SPAWN_EGG;
                case WOLF -> Material.WOLF_SPAWN_EGG;
                case CAT -> Material.CAT_SPAWN_EGG;
                case VILLAGER -> Material.VILLAGER_SPAWN_EGG;
                case IRON_GOLEM -> Material.IRON_INGOT;
                case SNOW_GOLEM -> Material.SNOWBALL;
                case SQUID -> Material.SQUID_SPAWN_EGG;
                case BAT -> Material.BAT_SPAWN_EGG;
                case OCELOT -> Material.OCELOT_SPAWN_EGG;
                case RABBIT -> Material.RABBIT_SPAWN_EGG;
                case GUARDIAN -> Material.GUARDIAN_SPAWN_EGG;
                case ELDER_GUARDIAN -> Material.ELDER_GUARDIAN_SPAWN_EGG;
                case SHULKER -> Material.SHULKER_SPAWN_EGG;
                case ENDERMITE -> Material.ENDERMITE_SPAWN_EGG;
                case SILVERFISH -> Material.SILVERFISH_SPAWN_EGG;
                case CAVE_SPIDER -> Material.CAVE_SPIDER_SPAWN_EGG;
                case ZOMBIFIED_PIGLIN -> Material.ZOMBIFIED_PIGLIN_SPAWN_EGG;
                case WITHER_SKELETON -> Material.WITHER_SKELETON_SPAWN_EGG;
                case STRAY -> Material.STRAY_SPAWN_EGG;
                case HUSK -> Material.HUSK_SPAWN_EGG;
                case ZOMBIE_VILLAGER -> Material.ZOMBIE_VILLAGER_SPAWN_EGG;
                case SKELETON_HORSE -> Material.SKELETON_HORSE_SPAWN_EGG;
                case ZOMBIE_HORSE -> Material.ZOMBIE_HORSE_SPAWN_EGG;
                case DONKEY -> Material.DONKEY_SPAWN_EGG;
                case MULE -> Material.MULE_SPAWN_EGG;
                case LLAMA -> Material.LLAMA_SPAWN_EGG;
                case PARROT -> Material.PARROT_SPAWN_EGG;
                case POLAR_BEAR -> Material.POLAR_BEAR_SPAWN_EGG;
                case DOLPHIN -> Material.DOLPHIN_SPAWN_EGG;
                case TURTLE -> Material.TURTLE_SPAWN_EGG;
                case PHANTOM -> Material.PHANTOM_SPAWN_EGG;
                case COD -> Material.COD_SPAWN_EGG;
                case SALMON -> Material.SALMON_SPAWN_EGG;
                case PUFFERFISH -> Material.PUFFERFISH_SPAWN_EGG;
                case TROPICAL_FISH -> Material.TROPICAL_FISH_SPAWN_EGG;
                case DROWNED -> Material.DROWNED_SPAWN_EGG;
                case PILLAGER -> Material.PILLAGER_SPAWN_EGG;
                case RAVAGER -> Material.RAVAGER_SPAWN_EGG;
                case VINDICATOR -> Material.VINDICATOR_SPAWN_EGG;
                case EVOKER -> Material.EVOKER_SPAWN_EGG;
                case VEX -> Material.VEX_SPAWN_EGG;
                case WANDERING_TRADER -> Material.WANDERING_TRADER_SPAWN_EGG;
                case TRADER_LLAMA -> Material.TRADER_LLAMA_SPAWN_EGG;
                case FOX -> Material.FOX_SPAWN_EGG;
                case BEE -> Material.BEE_SPAWN_EGG;
                case HOGLIN -> Material.HOGLIN_SPAWN_EGG;
                case PIGLIN -> Material.PIGLIN_SPAWN_EGG;
                case PIGLIN_BRUTE -> Material.PIGLIN_BRUTE_SPAWN_EGG;
                case ZOGLIN -> Material.ZOGLIN_SPAWN_EGG;
                case STRIDER -> Material.STRIDER_SPAWN_EGG;
                case AXOLOTL -> Material.AXOLOTL_SPAWN_EGG;
                case GLOW_SQUID -> Material.GLOW_SQUID_SPAWN_EGG;
                case GOAT -> Material.GOAT_SPAWN_EGG;
                case ALLAY -> Material.ALLAY_SPAWN_EGG;
                case FROG -> Material.FROG_SPAWN_EGG;
                case TADPOLE -> Material.TADPOLE_SPAWN_EGG;
                case WARDEN -> Material.SCULK_SHRIEKER;
                case CAMEL -> Material.CAMEL_SPAWN_EGG;
                case SNIFFER -> Material.SNIFFER_SPAWN_EGG;
                default -> null;
            };
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Get a generic material for a plugin namespace.
     * 
     * @param namespace The plugin namespace
     * @return A representative material
     */
    private static Material getGenericMaterialForNamespace(String namespace) {
        return switch (namespace.toLowerCase()) {
            case "customcrops", "crops" -> Material.WHEAT_SEEDS;
            case "itemsadder", "ia" -> Material.COMMAND_BLOCK;
            case "oraxen" -> Material.BARRIER;
            case "nexo" -> Material.STRUCTURE_VOID;
            case "mmoitems", "mmo" -> Material.DIAMOND;
            default -> Material.PAPER;
        };
    }
}