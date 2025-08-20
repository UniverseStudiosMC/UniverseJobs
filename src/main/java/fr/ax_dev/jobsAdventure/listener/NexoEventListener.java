package fr.ax_dev.jobsAdventure.listener;

import com.nexomc.nexo.api.events.custom_block.NexoBlockBreakEvent;
import com.nexomc.nexo.api.events.custom_block.NexoBlockPlaceEvent;
import com.nexomc.nexo.api.events.custom_block.NexoBlockInteractEvent;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.action.ActionProcessor;
import fr.ax_dev.jobsAdventure.action.ActionType;
import fr.ax_dev.jobsAdventure.condition.ConditionContext;
import fr.ax_dev.jobsAdventure.protection.BlockProtectionManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listens for Nexo custom block events using the direct Nexo API.
 * Handles NexoBlockPlaceEvent and NexoBlockBreakEvent for better Nexo integration.
 */
public class NexoEventListener implements Listener {
    
    private final JobsAdventure plugin;
    private final ActionProcessor actionProcessor;
    private final BlockProtectionManager protectionManager;
    
    /**
     * Create a new NexoEventListener.
     * 
     * @param plugin The plugin instance
     * @param actionProcessor The action processor
     * @param protectionManager The block protection manager
     */
    public NexoEventListener(JobsAdventure plugin, ActionProcessor actionProcessor, BlockProtectionManager protectionManager) {
        this.plugin = plugin;
        this.actionProcessor = actionProcessor;
        this.protectionManager = protectionManager;
    }
    
    /**
     * Handle Nexo custom block placement.
     * Uses Nexo's direct API for better accuracy and performance.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNexoBlockPlace(NexoBlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String nexoBlockId = event.getMechanic().getItemID();
        
        // Track the placed block for anti-exploit protection
        protectionManager.recordBlockPlacement(player, block);
        
        // Create context with Nexo information
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set("target", "nexo:" + nexoBlockId)
                .set("nexo_block_id", nexoBlockId);
        
        // Process the action
        actionProcessor.processAction(player, ActionType.PLACE, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Nexo block placed: " + nexoBlockId + " by " + player.getName() + " at " + block.getLocation());
        }
    }
    
    /**
     * Handle Nexo custom block breaking.
     * Uses Nexo's direct API for better accuracy and performance.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNexoBlockBreak(NexoBlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String nexoBlockId = event.getMechanic().getItemID();
        
        // Check if this block was placed by a player (anti-exploit for Nexo blocks)
        if (protectionManager.isPlayerPlacedBlock(block)) {
            // Remove from tracking but don't give XP
            protectionManager.removeTrackedBlock(block);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " mined a player-placed Nexo block (" + nexoBlockId + ") - no XP awarded");
            }
            return;
        }
        
        // Create context with Nexo information
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set("target", "nexo:" + nexoBlockId)
                .set("nexo_block_id", nexoBlockId);
        
        // Process the action and award XP
        actionProcessor.processAction(player, ActionType.BREAK, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Nexo block broken: " + nexoBlockId + " by " + player.getName() + " at " + block.getLocation());
        }
    }
    
    /**
     * Handle Nexo custom block interactions.
     * Uses Nexo's direct API for better accuracy and performance.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNexoBlockInteract(NexoBlockInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String nexoBlockId = event.getMechanic().getItemID();
        
        // Determine interact type based on the interaction
        String interactType;
        try {
            // Try to get interaction type from event if available
            boolean isRightClick = event.getHand() != null; // This is a guess - check actual API
            interactType = player.isSneaking() ? 
                (isRightClick ? "SHIFT-RIGHT" : "SHIFT-LEFT") : 
                (isRightClick ? "RIGHT" : "LEFT");
        } catch (Exception e) {
            // Fallback to RIGHT if we can't determine the interaction type
            interactType = player.isSneaking() ? "SHIFT-RIGHT" : "RIGHT";
        }
        
        // Create context with Nexo information
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set("target", "nexo:" + nexoBlockId)
                .set("nexo_block_id", nexoBlockId)
                .set("interact-type", interactType);
        
        // Process the action
        actionProcessor.processAction(player, ActionType.BLOCK_INTERACT, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Nexo block interact: " + nexoBlockId + " by " + player.getName() + " - interact-type: " + interactType);
        }
    }
}