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
import org.bukkit.event.Event;

public class OraxenEventListener implements Listener {
    
    private static final String TARGET_PREFIX = "target";
    private static final String ORAXEN_PREFIX = "oraxen:";
    private static final String ORAXEN_ITEM_ID = "oraxen_item_id";
    
    private final UniverseJobs plugin;
    private final ActionProcessor actionProcessor;
    private final BlockProtectionManager protectionManager;
    
    public OraxenEventListener(UniverseJobs plugin, ActionProcessor actionProcessor, BlockProtectionManager protectionManager) {
        this.plugin = plugin;
        this.actionProcessor = actionProcessor;
        this.protectionManager = protectionManager;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenNoteBlockPlace(OraxenNoteBlockPlaceEvent event) {
        handleOraxenBlockPlace(event.getPlayer(), event.getBlock(), event.getMechanic().getItemID(), event, "NoteBlock");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenStringBlockPlace(OraxenStringBlockPlaceEvent event) {
        handleOraxenBlockPlace(event.getPlayer(), event.getBlock(), event.getMechanic().getItemID(), event, "StringBlock");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenFurniturePlace(OraxenFurniturePlaceEvent event) {
        Block block = event.getBaseEntity().getLocation().getBlock();
        handleOraxenBlockPlace(event.getPlayer(), block, event.getMechanic().getItemID(), event, "Furniture");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenNoteBlockBreak(OraxenNoteBlockBreakEvent event) {
        handleOraxenBlockBreak(event.getPlayer(), event.getBlock(), event.getMechanic().getItemID(), event, "NoteBlock");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenStringBlockBreak(OraxenStringBlockBreakEvent event) {
        handleOraxenBlockBreak(event.getPlayer(), event.getBlock(), event.getMechanic().getItemID(), event, "StringBlock");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenFurnitureBreak(OraxenFurnitureBreakEvent event) {
        Block block = event.getBaseEntity().getLocation().getBlock();
        handleOraxenBlockBreak(event.getPlayer(), block, event.getMechanic().getItemID(), event, "Furniture");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOraxenNoteBlockInteract(OraxenNoteBlockInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String oraxenBlockId = event.getMechanic().getItemID();
        
        String interactType = determineInteractType(player, event.getAction());
        
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set(TARGET_PREFIX, ORAXEN_PREFIX + oraxenBlockId)
                .set(ORAXEN_ITEM_ID, oraxenBlockId)
                .set("interact-type", interactType);
        
        actionProcessor.processAction(player, ActionType.BLOCK_INTERACT, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Oraxen NoteBlock interact: " + oraxenBlockId + " by " + player.getName() + " - interact-type: " + interactType);
        }
    }
    
    private void handleOraxenBlockPlace(Player player, Block block, String oraxenItemId, Event event, String blockType) {
        protectionManager.recordBlockPlacement(player, block);
        
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set(TARGET_PREFIX, ORAXEN_PREFIX + oraxenItemId)
                .set(ORAXEN_ITEM_ID, oraxenItemId);
        
        actionProcessor.processAction(player, ActionType.PLACE, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Oraxen " + blockType + " placed: " + oraxenItemId + " by " + player.getName() + " at " + block.getLocation());
        }
    }
    
    private void handleOraxenBlockBreak(Player player, Block block, String oraxenBlockId, Event event, String blockType) {
        if (protectionManager.isPlayerPlacedBlock(block)) {
            protectionManager.removeTrackedBlock(block);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " mined a player-placed Oraxen " + blockType + " (" + oraxenBlockId + ") - no XP awarded");
            }
            return;
        }
        
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set(TARGET_PREFIX, ORAXEN_PREFIX + oraxenBlockId)
                .set(ORAXEN_ITEM_ID, oraxenBlockId);
        
        actionProcessor.processAction(player, ActionType.BREAK, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Oraxen " + blockType + " broken: " + oraxenBlockId + " by " + player.getName() + " at " + block.getLocation());
        }
    }
    
    private String determineInteractType(Player player, org.bukkit.event.block.Action action) {
        try {
            boolean isRightClick = (action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK);
            
            return player.isSneaking() ? 
                (isRightClick ? "RIGHT_SHIFT_CLICK" : "LEFT_SHIFT_CLICK") : 
                (isRightClick ? "RIGHT_CLICK" : "LEFT_CLICK");
        } catch (Exception e) {
            return player.isSneaking() ? "RIGHT_SHIFT_CLICK" : "RIGHT_CLICK";
        }
    }
}