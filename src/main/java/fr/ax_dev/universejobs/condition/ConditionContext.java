package fr.ax_dev.universejobs.condition;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Context object that holds data for condition evaluation.
 * Provides easy access to common action-related data.
 */
public class ConditionContext {
    
    private final Map<String, Object> data = new HashMap<>();
    
    /**
     * Create a new empty context.
     */
    public ConditionContext() {}
    
    /**
     * Set a value in the context.
     * 
     * @param key The key
     * @param value The value
     * @return This context for chaining
     */
    public ConditionContext set(String key, Object value) {
        data.put(key, value);
        return this;
    }
    
    /**
     * Get a value from the context.
     * 
     * @param key The key
     * @param defaultValue The default value if not found
     * @param <T> The value type
     * @return The value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object value = data.get(key);
        if (value == null) return defaultValue;
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
    
    /**
     * Get a value from the context.
     * 
     * @param key The key
     * @param <T> The value type
     * @return The value or null
     */
    public <T> T get(String key) {
        return get(key, null);
    }
    
    /**
     * Check if a key exists in the context.
     * 
     * @param key The key
     * @return true if the key exists
     */
    public boolean has(String key) {
        return data.containsKey(key);
    }
    
    /**
     * Set the block involved in the action.
     * 
     * @param block The block
     * @return This context for chaining
     */
    public ConditionContext setBlock(Block block) {
        set("block", block).set("material", block.getType());
        
        // Try to detect Nexo block ID
        String nexoBlockId = detectNexoBlockId(block);
        if (nexoBlockId != null) {
            set("nexo_block_id", nexoBlockId);
        }
        
        return this;
    }
    
    /**
     * Set the entity involved in the action.
     * 
     * @param entity The entity
     * @return This context for chaining
     */
    public ConditionContext setEntity(Entity entity) {
        return set("entity", entity).set("entityType", entity.getType());
    }
    
    /**
     * Set the item involved in the action.
     * 
     * @param item The item
     * @return This context for chaining
     */
    public ConditionContext setItem(ItemStack item) {
        if (item != null) {
            set("item", item).set("material", item.getType());
        }
        return this;
    }
    
    /**
     * Set the material involved in the action.
     * 
     * @param material The material
     * @return This context for chaining
     */
    public ConditionContext setMaterial(Material material) {
        return set("material", material);
    }
    
    /**
     * Get the block from the context.
     * 
     * @return The block or null
     */
    public Block getBlock() {
        return get("block");
    }
    
    /**
     * Get the entity from the context.
     * 
     * @return The entity or null
     */
    public Entity getEntity() {
        return get("entity");
    }
    
    /**
     * Get the item from the context.
     * 
     * @return The item or null
     */
    public ItemStack getItem() {
        return get("item");
    }
    
    /**
     * Get the material from the context.
     * 
     * @return The material or null
     */
    public Material getMaterial() {
        return get("material");
    }
    
    /**
     * Get the target string (for matching against action targets).
     * 
     * @return The target string
     */
    public String getTarget() {
        // First check if there's an explicit target set (for custom blocks like Nexo, ItemsAdder, CustomCrops)
        String explicitTarget = get("target", (String) null);
        if (explicitTarget != null && !explicitTarget.isEmpty()) {
            return explicitTarget;
        }
        
        // Fallback to material
        Material material = getMaterial();
        if (material != null) {
            return material.name();
        }
        
        // Fallback to entity
        Entity entity = getEntity();
        if (entity != null) {
            return entity.getType().name();
        }
        
        return "";
    }
    
    /**
     * Get the Nexo block ID if this context involves a Nexo custom block.
     * 
     * @return The Nexo block ID or null
     */
    public String getNexoBlockId() {
        return get("nexo_block_id");
    }
    
    /**
     * Detect if a block is a Nexo custom block and return its ID.
     * Uses direct Nexo API calls.
     * 
     * @param block The block to check
     * @return The Nexo block ID or null
     */
    private String detectNexoBlockId(Block block) {
        try {
            // Use direct Nexo API
            com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic mechanic = 
                com.nexomc.nexo.api.NexoBlocks.customBlockMechanic((Location) block);
            
            return mechanic != null ? mechanic.getItemID() : null;
            
        } catch (Exception ignored) {
            // Nexo not available or error occurred
            return null;
        }
    }
    
    @Override
    public String toString() {
        return "ConditionContext{data=" + data + "}";
    }
}