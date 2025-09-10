package fr.ax_dev.universejobs.listener;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionProcessor;
import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.condition.ConditionContext;
import fr.ax_dev.universejobs.protection.BlockProtectionManager;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockBreakEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockPlaceEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockInteractEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockBreakEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockPlaceEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurniturePlaceEvent;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for Oraxen custom block events using the direct Oraxen API.
 * Uses specialized Oraxen events: OraxenNoteBlockEvents, OraxenStringBlockEvents, and OraxenFurnitureEvents.
 * Only uses events that actually exist in the Oraxen API.
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
     * Handle Oraxen NoteBlock placement using specialized Oraxen events.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenNoteBlockPlace(OraxenNoteBlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String oraxenItemId = event.getMechanic().getItemID();
        
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
            plugin.getLogger().info("Oraxen NoteBlock placed: " + oraxenItemId + " by " + player.getName() + " at " + block.getLocation());
        }
    }
    
    /**
     * Handle Oraxen StringBlock placement using specialized Oraxen events.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenStringBlockPlace(OraxenStringBlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String oraxenItemId = event.getMechanic().getItemID();
        
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
            plugin.getLogger().info("Oraxen StringBlock placed: " + oraxenItemId + " by " + player.getName() + " at " + block.getLocation());
        }
    }
    
    /**
     * Handle Oraxen Furniture placement using specialized Oraxen events.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenFurniturePlace(OraxenFurniturePlaceEvent event) {
        Player player = event.getPlayer();
        org.bukkit.entity.Entity baseEntity = event.getBaseEntity();
        String oraxenItemId = event.getMechanic().getItemID();
        
        // For furniture, we use the BaseEntity location as the "block" location
        Block block = baseEntity.getLocation().getBlock();
        
        // Track the placed furniture for anti-exploit protection
        protectionManager.recordBlockPlacement(player, block);
        
        // Create context with Oraxen information
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set(TARGET_PREFIX, ORAXEN_PREFIX + oraxenItemId)
                .set(ORAXEN_ITEM_ID, oraxenItemId);
        
        // Process the action (MONITOR priority - no cancellation)
        actionProcessor.processAction(player, ActionType.PLACE, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Oraxen Furniture placed: " + oraxenItemId + " by " + player.getName() + " at " + baseEntity.getLocation());
        }
    }
    
    /**
     * Handle Oraxen NoteBlock breaking using specialized Oraxen events.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenNoteBlockBreak(OraxenNoteBlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String oraxenBlockId = event.getMechanic().getItemID();
        
        // Check if this block was placed by a player (anti-exploit for Oraxen blocks)
        if (protectionManager.isPlayerPlacedBlock(block)) {
            // Remove from tracking but don't give XP
            protectionManager.removeTrackedBlock(block);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " mined a player-placed Oraxen NoteBlock (" + oraxenBlockId + ") - no XP awarded");
            }
            return;
        }
        
        // Create context with Oraxen information
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set(TARGET_PREFIX, ORAXEN_PREFIX + oraxenBlockId)
                .set(ORAXEN_ITEM_ID, oraxenBlockId);
        
        // Process the action (MONITOR priority - no cancellation)
        actionProcessor.processAction(player, ActionType.BREAK, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Oraxen NoteBlock broken: " + oraxenBlockId + " by " + player.getName() + " at " + block.getLocation());
        }
    }
    
    /**
     * Handle Oraxen StringBlock breaking using specialized Oraxen events.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenStringBlockBreak(OraxenStringBlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String oraxenBlockId = event.getMechanic().getItemID();
        
        // Check if this block was placed by a player (anti-exploit for Oraxen blocks)
        if (protectionManager.isPlayerPlacedBlock(block)) {
            // Remove from tracking but don't give XP
            protectionManager.removeTrackedBlock(block);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " mined a player-placed Oraxen StringBlock (" + oraxenBlockId + ") - no XP awarded");
            }
            return;
        }
        
        // Create context with Oraxen information
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set(TARGET_PREFIX, ORAXEN_PREFIX + oraxenBlockId)
                .set(ORAXEN_ITEM_ID, oraxenBlockId);
        
        // Process the action (MONITOR priority - no cancellation)
        actionProcessor.processAction(player, ActionType.BREAK, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Oraxen StringBlock broken: " + oraxenBlockId + " by " + player.getName() + " at " + block.getLocation());
        }
    }
    
    /**
     * Handle Oraxen Furniture breaking using specialized Oraxen events.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenFurnitureBreak(OraxenFurnitureBreakEvent event) {
        Player player = event.getPlayer();
        org.bukkit.entity.Entity baseEntity = event.getBaseEntity();
        String oraxenBlockId = event.getMechanic().getItemID();
        
        // For furniture, we use the BaseEntity location as the "block" location
        Block block = baseEntity.getLocation().getBlock();
        
        // Check if this furniture was placed by a player (anti-exploit for Oraxen furniture)
        if (protectionManager.isPlayerPlacedBlock(block)) {
            // Remove from tracking but don't give XP
            protectionManager.removeTrackedBlock(block);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " removed a player-placed Oraxen Furniture (" + oraxenBlockId + ") - no XP awarded");
            }
            return;
        }
        
        // Create context with Oraxen information
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set(TARGET_PREFIX, ORAXEN_PREFIX + oraxenBlockId)
                .set(ORAXEN_ITEM_ID, oraxenBlockId);
        
        // Process the action (MONITOR priority - no cancellation)
        actionProcessor.processAction(player, ActionType.BREAK, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Oraxen Furniture broken: " + oraxenBlockId + " by " + player.getName() + " at " + baseEntity.getLocation());
        }
    }
    
    /**
     * Handle Oraxen NoteBlock interactions using specialized Oraxen events.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenNoteBlockInteract(OraxenNoteBlockInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String oraxenBlockId = event.getMechanic().getItemID();
        
        // Determine interact type based on the interaction
        String interactType;
        try {
            org.bukkit.event.block.Action action = event.getAction();
            boolean isRightClick = (action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK);
            
            interactType = player.isSneaking() ? 
                (isRightClick ? "RIGHT_SHIFT_CLICK" : "LEFT_SHIFT_CLICK") : 
                (isRightClick ? "RIGHT_CLICK" : "LEFT_CLICK");
        } catch (Exception e) {
            interactType = player.isSneaking() ? "RIGHT_SHIFT_CLICK" : "RIGHT_CLICK";
        }
        
        // Create context with Oraxen information
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set(TARGET_PREFIX, ORAXEN_PREFIX + oraxenBlockId)
                .set(ORAXEN_ITEM_ID, oraxenBlockId)
                .set("interact-type", interactType);
        
        // Process the action (MONITOR priority - no cancellation)
        actionProcessor.processAction(player, ActionType.BLOCK_INTERACT, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Oraxen NoteBlock interact: " + oraxenBlockId + " by " + player.getName() + " - interact-type: " + interactType);
        }
    }
    
    
}