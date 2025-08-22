package fr.ax_dev.universejobs.protection;

import fr.ax_dev.universejobs.UniverseJobs;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;

// Nexo imports handled via reflection to avoid compilation errors when Nexo is not available

import java.util.logging.Level;

/**
 * Manages block protection to prevent XP farming exploits.
 * Uses NBT tags to mark player-placed blocks.
 */
public class BlockProtectionManager {
    
    private static final String BLOCK_PREFIX = "block_";
    
    private final UniverseJobs plugin;
    private boolean enabled;
    private boolean nexoEnabled;
    
    /**
     * Create a new block protection manager.
     * 
     * @param plugin The plugin instance
     */
    public BlockProtectionManager(UniverseJobs plugin) {
        this.plugin = plugin;
        new NamespacedKey(plugin, "player_placed");
        
        loadConfiguration();
        checkNexoCompatibility();
        
        // Block protection initialized
    }
    
    /**
     * Load configuration settings.
     */
    private void loadConfiguration() {
        this.enabled = plugin.getConfig().getBoolean("block-protection.enabled", true);
    }
    
    /**
     * Check if Nexo plugin is available for custom block compatibility.
     */
    private void checkNexoCompatibility() {
        this.nexoEnabled = plugin.getServer().getPluginManager().isPluginEnabled("Nexo");
        if (nexoEnabled && plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Nexo plugin detected - custom blocks will be tracked for anti-exploit protection");
        }
    }
    
    /**
     * Check if block protection is enabled.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Record a block placement by a player.
     * Handles both vanilla blocks and Nexo custom blocks.
     * 
     * @param player The player who placed the block
     * @param block The block that was placed
     */
    public void recordBlockPlacement(Player player, Block block) {
        if (!enabled) return;
        
        try {
            // Add NBT tag to mark this block as player-placed
            NamespacedKey blockKey = new NamespacedKey(plugin, BLOCK_PREFIX + block.getX() + "_" + block.getY() + "_" + block.getZ());
            String playerData = player.getUniqueId().toString();
            
            // Check if this is a Nexo custom block and add that information
            if (nexoEnabled) {
                String nexoBlockId = getNexoBlockId(block);
                if (nexoBlockId != null) {
                    playerData += "|NEXO:" + nexoBlockId;
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Detected Nexo custom block: " + nexoBlockId + " at " + block.getLocation());
                    }
                }
            }
            
            block.getChunk().getPersistentDataContainer().set(blockKey, PersistentDataType.STRING, playerData);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Marked block at " + block.getLocation() + " as player-placed by " + player.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to mark block as player-placed", e);
        }
    }
    
    /**
     * Check if a block was placed by a player.
     * 
     * @param block The block to check
     * @return true if the block was placed by a player
     */
    public boolean isPlayerPlacedBlock(Block block) {
        if (!enabled) return false;
        
        try {
            // Check if this block has the player-placed NBT tag
            NamespacedKey blockKey = new NamespacedKey(plugin, BLOCK_PREFIX + block.getX() + "_" + block.getY() + "_" + block.getZ());
            String placedBy = block.getChunk().getPersistentDataContainer().get(blockKey, PersistentDataType.STRING);
            
            return placedBy != null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check if block is player-placed", e);
            return false;
        }
    }
    
    /**
     * Remove a tracked block.
     * 
     * @param block The block to remove
     */
    public void removeTrackedBlock(Block block) {
        if (!enabled) return;
        
        try {
            // Remove the NBT tag for this block
            NamespacedKey blockKey = new NamespacedKey(plugin, BLOCK_PREFIX + block.getX() + "_" + block.getY() + "_" + block.getZ());
            block.getChunk().getPersistentDataContainer().remove(blockKey);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Removed player-placed tag from block at " + block.getLocation());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to remove block tracking", e);
        }
    }
    
    /**
     * Get the Nexo block ID for a custom block, if applicable.
     * Uses direct Nexo API calls.
     * 
     * @param block The block to check
     * @return The Nexo block ID, or null if not a Nexo block
     */
    private String getNexoBlockId(Block block) {
        if (!nexoEnabled) return null;
        
        try {
            // Use direct Nexo API
            CustomBlockMechanic mechanic = NexoBlocks.customBlockMechanic(block.getLocation());
            
            return mechanic != null ? mechanic.getItemID() : null;
            
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Failed to check Nexo block ID: " + e.getMessage());
            }
            return null;
        }
    }
    
    
}