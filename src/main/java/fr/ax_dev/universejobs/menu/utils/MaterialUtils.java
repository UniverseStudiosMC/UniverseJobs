package fr.ax_dev.universejobs.menu.utils;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import fr.ax_dev.universejobs.action.ActionType;

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
        return getMaterialForTarget(target, null);
    }
    
    /**
     * Get the source material for display (e.g., COPPER_ORE instead of COPPER_INGOT).
     * 
     * @param target The action target (case-insensitive)
     * @param actionType The action type for better material selection
     * @return The source material to use for the item display
     */
    public static Material getSourceMaterialForTarget(String target, ActionType actionType) {
        Material baseMaterial = getMaterialForTarget(target, actionType);
        return getSourceMaterial(baseMaterial);
    }
    
    /**
     * Get the appropriate material for an action target with ActionType context.
     * 
     * @param target The action target (case-insensitive)
     * @param actionType The action type for better material selection
     * @return The material to use for the item
     */
    public static Material getMaterialForTarget(String target, ActionType actionType) {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("Target cannot be null or empty");
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
        
        // Try ActionType-specific materials
        if (actionType != null) {
            Material actionMaterial = getMaterialForActionType(actionType, normalizedTarget);
            if (actionMaterial != null) {
                return actionMaterial;
            }
        }
        
        // Try enchantments and other special cases
        Material fallbackMaterial = getFallbackMaterial(normalizedTarget);
        if (fallbackMaterial != null) {
            return fallbackMaterial;
        }
        
        // No valid material found
        throw new IllegalArgumentException("No valid material found for target: " + target);
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
     * Get material based on ActionType for better context.
     */
    private static Material getMaterialForActionType(ActionType actionType, String target) {
        return switch (actionType) {
            case ENCHANT -> getEnchantmentMaterial(target.toLowerCase());
            case POTION -> Material.POTION;
            case BREW -> Material.BREWING_STAND;
            case FISH -> Material.FISHING_ROD;
            case CRAFT -> Material.CRAFTING_TABLE;
            case SMELT -> Material.FURNACE;
            case MILK -> Material.MILK_BUCKET;
            case EAT -> Material.BREAD;
            case TAME -> Material.BONE;
            case BREED -> Material.WHEAT;
            case SHEAR -> Material.SHEARS;
            default -> null;
        };
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
    
    /**
     * Get fallback material for special cases like enchantments, potions, etc.
     * 
     * @param target The action target
     * @return A representative material or null if no fallback found
     */
    private static Material getFallbackMaterial(String target) {
        String lowerTarget = target.toLowerCase();
        
        // Enchantments
        if (isEnchantment(lowerTarget)) {
            return getEnchantmentMaterial(lowerTarget);
        }
        
        // Potions
        if (isPotion(lowerTarget)) {
            return Material.POTION;
        }
        
        // Foods
        if (isFood(lowerTarget)) {
            return Material.BREAD;
        }
        
        // Tools/Weapons
        if (isTool(lowerTarget)) {
            return getToolMaterial(lowerTarget);
        }
        
        return null;
    }
    
    /**
     * Check if target is an enchantment name.
     */
    private static boolean isEnchantment(String target) {
        return target.equals("sharpness") || target.equals("protection") || 
               target.equals("efficiency") || target.equals("unbreaking") ||
               target.equals("looting") || target.equals("fortune") ||
               target.equals("silk_touch") || target.equals("mending") ||
               target.equals("fire_aspect") || target.equals("knockback") ||
               target.equals("power") || target.equals("punch") ||
               target.equals("flame") || target.equals("infinity");
    }
    
    /**
     * Get material for enchantment type.
     */
    private static Material getEnchantmentMaterial(String enchantment) {
        return switch (enchantment) {
            case "sharpness", "fire_aspect", "knockback", "looting" -> Material.DIAMOND_SWORD;
            case "protection" -> Material.DIAMOND_CHESTPLATE;
            case "efficiency", "fortune", "silk_touch" -> Material.DIAMOND_PICKAXE;
            case "unbreaking", "mending" -> Material.ENCHANTED_BOOK;
            case "power", "punch", "flame", "infinity" -> Material.BOW;
            default -> Material.ENCHANTED_BOOK;
        };
    }
    
    /**
     * Check if target is a potion name.
     */
    private static boolean isPotion(String target) {
        return target.contains("potion") || target.equals("healing") || 
               target.equals("strength") || target.equals("speed") ||
               target.equals("regeneration") || target.equals("poison");
    }
    
    /**
     * Check if target is a food name.
     */
    private static boolean isFood(String target) {
        return target.equals("eating") || target.equals("food") || target.equals("consume");
    }
    
    /**
     * Check if target is a tool name.
     */
    private static boolean isTool(String target) {
        return target.contains("sword") || target.contains("pickaxe") ||
               target.contains("axe") || target.contains("shovel") ||
               target.contains("hoe");
    }
    
    /**
     * Get material for tool type.
     */
    private static Material getToolMaterial(String tool) {
        if (tool.contains("sword")) return Material.DIAMOND_SWORD;
        if (tool.contains("pickaxe")) return Material.DIAMOND_PICKAXE;
        if (tool.contains("axe")) return Material.DIAMOND_AXE;
        if (tool.contains("shovel")) return Material.DIAMOND_SHOVEL;
        if (tool.contains("hoe")) return Material.DIAMOND_HOE;
        return Material.STICK;
    }
    
    /**
     * Map processed/refined materials to their source materials for better display.
     * 
     * @param material The processed material
     * @return The source material for display
     */
    private static Material getSourceMaterial(Material material) {
        return switch (material) {
            // Copper materials
            case COPPER_INGOT -> Material.COPPER_ORE;
            case COPPER_BLOCK -> Material.COPPER_ORE;
            
            // Iron materials  
            case IRON_INGOT -> Material.IRON_ORE;
            case IRON_BLOCK -> Material.IRON_ORE;
            case IRON_NUGGET -> Material.IRON_ORE;
            
            // Gold materials
            case GOLD_INGOT -> Material.GOLD_ORE;
            case GOLD_BLOCK -> Material.GOLD_ORE;
            case GOLD_NUGGET -> Material.GOLD_ORE;
            
            // Diamond materials
            case DIAMOND_BLOCK -> Material.DIAMOND_ORE;
            
            // Emerald materials
            case EMERALD_BLOCK -> Material.EMERALD_ORE;
            
            // Coal materials
            case COAL_BLOCK -> Material.COAL_ORE;
            
            // Redstone materials
            case REDSTONE_BLOCK -> Material.REDSTONE_ORE;
            
            // Lapis materials
            case LAPIS_BLOCK -> Material.LAPIS_ORE;
            
            // Netherite materials
            case NETHERITE_INGOT -> Material.ANCIENT_DEBRIS;
            case NETHERITE_BLOCK -> Material.ANCIENT_DEBRIS;
            
            // Wood materials -> logs
            case OAK_PLANKS -> Material.OAK_LOG;
            case BIRCH_PLANKS -> Material.BIRCH_LOG;
            case SPRUCE_PLANKS -> Material.SPRUCE_LOG;
            case JUNGLE_PLANKS -> Material.JUNGLE_LOG;
            case ACACIA_PLANKS -> Material.ACACIA_LOG;
            case DARK_OAK_PLANKS -> Material.DARK_OAK_LOG;
            case MANGROVE_PLANKS -> Material.MANGROVE_LOG;
            case CHERRY_PLANKS -> Material.CHERRY_LOG;
            case BAMBOO_PLANKS -> Material.BAMBOO;
            case CRIMSON_PLANKS -> Material.CRIMSON_STEM;
            case WARPED_PLANKS -> Material.WARPED_STEM;
            
            // Stone materials
            case STONE_BRICKS -> Material.STONE;
            case SMOOTH_STONE -> Material.STONE;
            case STONE_BRICK_SLAB -> Material.STONE;
            case STONE_BRICK_STAIRS -> Material.STONE;
            
            // Cobblestone derivatives
            case COBBLESTONE_SLAB -> Material.COBBLESTONE;
            case COBBLESTONE_STAIRS -> Material.COBBLESTONE;
            case COBBLESTONE_WALL -> Material.COBBLESTONE;
            
            // Food materials -> crops
            case BREAD -> Material.WHEAT;
            case COOKIE -> Material.COCOA_BEANS;
            case CAKE -> Material.WHEAT;
            case PUMPKIN_PIE -> Material.PUMPKIN;
            
            // Glass materials -> sand
            case GLASS -> Material.SAND;
            case GLASS_PANE -> Material.SAND;
            case WHITE_STAINED_GLASS -> Material.SAND;
            case BLACK_STAINED_GLASS -> Material.SAND;
            case BLUE_STAINED_GLASS -> Material.SAND;
            case BROWN_STAINED_GLASS -> Material.SAND;
            case CYAN_STAINED_GLASS -> Material.SAND;
            case GRAY_STAINED_GLASS -> Material.SAND;
            case GREEN_STAINED_GLASS -> Material.SAND;
            case LIGHT_BLUE_STAINED_GLASS -> Material.SAND;
            case LIGHT_GRAY_STAINED_GLASS -> Material.SAND;
            case LIME_STAINED_GLASS -> Material.SAND;
            case MAGENTA_STAINED_GLASS -> Material.SAND;
            case ORANGE_STAINED_GLASS -> Material.SAND;
            case PINK_STAINED_GLASS -> Material.SAND;
            case PURPLE_STAINED_GLASS -> Material.SAND;
            case RED_STAINED_GLASS -> Material.SAND;
            case YELLOW_STAINED_GLASS -> Material.SAND;
            
            // Brick materials -> clay
            case BRICKS -> Material.CLAY;
            case BRICK_SLAB -> Material.CLAY;
            case BRICK_STAIRS -> Material.CLAY;
            case BRICK_WALL -> Material.CLAY;
            
            // Default: return the same material
            default -> material;
        };
    }
}