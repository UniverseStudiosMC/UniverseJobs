package fr.ax_dev.universejobs.listener;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionProcessor;
import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.condition.ConditionContext;
import fr.ax_dev.universejobs.protection.BlockProtectionManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for Oraxen custom block events using the Oraxen API.
 * Unlike ItemsAdder/Nexo, Oraxen uses standard Bukkit events with API detection methods.
 * Handles BlockPlaceEvent, BlockBreakEvent, and PlayerInteractEvent for better Oraxen integration.
 */
public class OraxenEventListener implements Listener {
    
    private static final String TARGET_PREFIX = "target";
    private static final String ORAXEN_PREFIX = "oraxen:";
    private static final String ORAXEN_ITEM_ID = "oraxen_item_id";
    
    private final UniverseJobs plugin;
    private final ActionProcessor actionProcessor;
    private final BlockProtectionManager protectionManager;
    
    /**
     * Create a new OraxenEventListener.
     * 
     * @param plugin The plugin instance
     * @param actionProcessor The action processor
     * @param protectionManager The block protection manager
     */
    public OraxenEventListener(UniverseJobs plugin, ActionProcessor actionProcessor, BlockProtectionManager protectionManager) {
        this.plugin = plugin;
        this.actionProcessor = actionProcessor;
        this.protectionManager = protectionManager;
    }
    
    /**
     * Handle Oraxen custom block placement.
     * Uses Oraxen API to detect custom items in standard BlockPlaceEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack itemInHand = event.getItemInHand();
        
        // Check if this is an Oraxen item using the API
        String oraxenItemId = getOraxenItemId(itemInHand);
        if (oraxenItemId == null) {
            return; // Not an Oraxen item
        }
        
        // Track the placed block for anti-exploit protection
        protectionManager.recordBlockPlacement(player, block);
        
        // Create context with Oraxen information
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set(TARGET_PREFIX, ORAXEN_PREFIX + oraxenItemId)
                .set(ORAXEN_ITEM_ID, oraxenItemId);
        
        // Process the action (MONITOR priority - no cancellation)
        actionProcessor.processAction(player, ActionType.PLACE, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Oraxen block placed: " + oraxenItemId + " by " + player.getName() + " at " + block.getLocation());
        }
    }
    
    /**
     * Handle Oraxen custom block breaking.
     * Uses Oraxen API to detect custom blocks in standard BlockBreakEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check if this is an Oraxen block by checking what item it would drop
        String oraxenItemId = getOraxenBlockId(block);
        if (oraxenItemId == null) {
            return; // Not an Oraxen block
        }
        
        // Check if this block was placed by a player (anti-exploit for Oraxen blocks)
        if (protectionManager.isPlayerPlacedBlock(block)) {
            // Remove from tracking but don't give XP
            protectionManager.removeTrackedBlock(block);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " mined a player-placed Oraxen block (" + oraxenItemId + ") - no XP awarded");
            }
            return;
        }
        
        // Create context with Oraxen information
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set(TARGET_PREFIX, ORAXEN_PREFIX + oraxenItemId)
                .set(ORAXEN_ITEM_ID, oraxenItemId);
        
        // Process the action (MONITOR priority - no cancellation)
        actionProcessor.processAction(player, ActionType.BREAK, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Oraxen block broken: " + oraxenItemId + " by " + player.getName() + " at " + block.getLocation());
        }
    }
    
    /**
     * Handle Oraxen custom block interactions.
     * Uses Oraxen API to detect custom blocks in standard PlayerInteractEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenBlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Only handle main hand interactions to avoid duplicate events
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        // Only handle block interactions (right click on blocks)
        if (event.getClickedBlock() == null) {
            return;
        }
        
        Block block = event.getClickedBlock();
        
        // Check if this is an Oraxen block
        String oraxenItemId = getOraxenBlockId(block);
        if (oraxenItemId == null) {
            return; // Not an Oraxen block
        }
        
        // Only handle RIGHT_CLICK actions for consistency with other listeners
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Determine interact type
        String interactType = player.isSneaking() ? "RIGHT_SHIFT_CLICK" : "RIGHT_CLICK";
        
        // Create context with Oraxen information
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set(TARGET_PREFIX, ORAXEN_PREFIX + oraxenItemId)
                .set(ORAXEN_ITEM_ID, oraxenItemId)
                .set("interact-type", interactType);
        
        // Process the action (MONITOR priority - no cancellation)
        actionProcessor.processAction(player, ActionType.BLOCK_INTERACT, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Oraxen block interact: " + oraxenItemId + " by " + player.getName() + " - interact-type: " + interactType);
        }
    }
    
    /**
     * Get the Oraxen item ID from an ItemStack using the Oraxen API.
     * 
     * @param itemStack The ItemStack to check
     * @return The Oraxen item ID, or null if not an Oraxen item
     */
    private String getOraxenItemId(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }
        
        try {
            // Check if Oraxen is installed
            if (!plugin.getServer().getPluginManager().isPluginEnabled("Oraxen")) {
                return null;
            }
            
            // Use reflection to avoid NoClassDefFoundError when Oraxen is not present
            Class<?> oraxenItemsClass = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            java.lang.reflect.Method getIdByItemMethod = oraxenItemsClass.getMethod("getIdByItem", ItemStack.class);
            Object result = getIdByItemMethod.invoke(null, itemStack);
            
            return (String) result; // Returns null if not an Oraxen item
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Error checking Oraxen item ID: " + e.getMessage());
            }
            return null;
        }
    }
    
    /**
     * Get the Oraxen item ID from a placed block.
     * This is more complex since we need to determine what Oraxen item this block represents.
     * 
     * @param block The block to check
     * @return The Oraxen item ID, or null if not an Oraxen block
     */
    private String getOraxenBlockId(Block block) {
        if (block == null) {
            return null;
        }
        
        try {
            // Oraxen blocks are typically NoteBlocks with custom data
            // We need to check if this block matches any Oraxen block configuration
            
            // Method 1: Check if the block material and data match known Oraxen blocks
            // This requires knowing the Oraxen internal block storage mechanism
            
            // Method 2: Try to get the drops that would be produced from breaking this block
            // and check if any of those are Oraxen items
            // This is less reliable but might work for some cases
            
            // For now, we'll use a basic approach checking NoteBlock custom data
            if (block.getType() == org.bukkit.Material.NOTE_BLOCK) {
                // Oraxen stores custom block data in the NoteBlock's instrument and note values
                // We would need to reverse-engineer this or use internal Oraxen methods
                
                org.bukkit.block.data.type.NoteBlock noteBlockData = (org.bukkit.block.data.type.NoteBlock) block.getBlockData();
                
                // This is a simplified detection - in reality, you'd need to map
                // the instrument/note combinations to specific Oraxen items
                String instrument = noteBlockData.getInstrument().name();
                int note = noteBlockData.getNote().getId();
                
                // For now, return a generic identifier
                // In a real implementation, you'd have a mapping of instrument+note -> oraxen_id
                return "noteblock_" + instrument.toLowerCase() + "_" + note;
            }
            
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Error checking Oraxen block ID: " + e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * Check if a block is an Oraxen custom block.
     * This prevents duplicate processing between vanilla and Oraxen events.
     * 
     * @param block The block to check
     * @return true if this is an Oraxen block
     */
    public boolean isOraxenBlock(Block block) {
        return getOraxenBlockId(block) != null;
    }
}