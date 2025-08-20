package fr.ax_dev.jobsAdventure.listener;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent;
import dev.lone.itemsadder.api.Events.CustomBlockInteractEvent;

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
 * Listens for ItemsAdder custom block events using the direct ItemsAdder API.
 * Handles CustomBlockPlaceEvent and CustomBlockBreakEvent for better ItemsAdder integration.
 */
public class ItemsAdderEventListener implements Listener {
    
    private final JobsAdventure plugin;
    private final ActionProcessor actionProcessor;
    private final BlockProtectionManager protectionManager;
    
    /**
     * Create a new ItemsAdderEventListener.
     * 
     * @param plugin The plugin instance
     * @param actionProcessor The action processor
     * @param protectionManager The block protection manager
     */
    public ItemsAdderEventListener(JobsAdventure plugin, ActionProcessor actionProcessor, BlockProtectionManager protectionManager) {
        this.plugin = plugin;
        this.actionProcessor = actionProcessor;
        this.protectionManager = protectionManager;
    }
    
    /**
     * Handle ItemsAdder custom block placement.
     * Uses ItemsAdder's direct API for better accuracy and performance.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemsAdderBlockPlace(CustomBlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String itemsAdderBlockId = event.getNamespacedID();
        
        // Track the placed block for anti-exploit protection
        protectionManager.recordBlockPlacement(player, block);
        
        // Create context with ItemsAdder information
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set("target", "itemsadder:" + itemsAdderBlockId)
                .set("itemsadder_block_id", itemsAdderBlockId);
        
        // Process the action
        actionProcessor.processAction(player, ActionType.PLACE, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("ItemsAdder block placed: " + itemsAdderBlockId + " by " + player.getName() + " at " + block.getLocation());
        }
    }
    
    /**
     * Handle ItemsAdder custom block breaking.
     * Uses ItemsAdder's direct API for better accuracy and performance.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemsAdderBlockBreak(CustomBlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Get the custom block to retrieve its ID
        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
        if (customBlock == null) {
            // This shouldn't happen in a CustomBlockBreakEvent, but just in case
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("CustomBlockBreakEvent fired but no custom block found at location: " + block.getLocation());
            }
            return;
        }
        
        String itemsAdderBlockId = customBlock.getNamespacedID();
        
        // Check if this block was placed by a player (anti-exploit for ItemsAdder blocks)
        if (protectionManager.isPlayerPlacedBlock(block)) {
            // Remove from tracking but don't give XP
            protectionManager.removeTrackedBlock(block);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " mined a player-placed ItemsAdder block (" + itemsAdderBlockId + ") - no XP awarded");
            }
            return;
        }
        
        // Create context with ItemsAdder information
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set("target", "itemsadder:" + itemsAdderBlockId)
                .set("itemsadder_block_id", itemsAdderBlockId);
        
        // Process the action and award XP
        actionProcessor.processAction(player, ActionType.BREAK, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("ItemsAdder block broken: " + itemsAdderBlockId + " by " + player.getName() + " at " + block.getLocation());
        }
    }
    
    /**
     * Handle ItemsAdder custom block interactions.
     * Uses ItemsAdder's direct API for better accuracy and performance.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemsAdderBlockInteract(CustomBlockInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockClicked(); // Correct API method name
        String itemsAdderBlockId = event.getNamespacedID();
        
        // Determine interact type based on the interaction
        String interactType;
        try {
            // Get action type from event
            org.bukkit.event.block.Action action = event.getAction();
            boolean isRightClick = (action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK);
            
            interactType = player.isSneaking() ? 
                (isRightClick ? "RIGHT_SHIFT_CLICK" : "LEFT_SHIFT_CLICK") : 
                (isRightClick ? "RIGHT_CLICK" : "LEFT_CLICK");
        } catch (Exception e) {
            // Fallback to RIGHT_CLICK if we can't determine the interaction type
            interactType = player.isSneaking() ? "RIGHT_SHIFT_CLICK" : "RIGHT_CLICK";
        }
        
        // Create context with ItemsAdder information
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set("target", "itemsadder:" + itemsAdderBlockId)
                .set("itemsadder_block_id", itemsAdderBlockId)
                .set("interact-type", interactType);
        
        // Process the action
        actionProcessor.processAction(player, ActionType.BLOCK_INTERACT, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("ItemsAdder block interact: " + itemsAdderBlockId + " by " + player.getName() + " - interact-type: " + interactType);
        }
    }
}