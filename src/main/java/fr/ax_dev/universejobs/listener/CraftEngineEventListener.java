package fr.ax_dev.universejobs.listener;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionProcessor;
import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.condition.ConditionContext;
import fr.ax_dev.universejobs.protection.BlockProtectionManager;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.block.ImmutableBlockState;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class CraftEngineEventListener implements Listener {
    
    private static final String TARGET_PREFIX = "target";
    private static final String CRAFTENGINE_PREFIX = "craftengine:";
    private static final String CRAFTENGINE_BLOCK_ID = "craftengine_block_id";
    
    private final UniverseJobs plugin;
    private final ActionProcessor actionProcessor;
    private final BlockProtectionManager protectionManager;
    
    public CraftEngineEventListener(UniverseJobs plugin, ActionProcessor actionProcessor, BlockProtectionManager protectionManager) {
        this.plugin = plugin;
        this.actionProcessor = actionProcessor;
        this.protectionManager = protectionManager;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        if (!isCraftEngineBlock(block)) {
            return;
        }
        
        String craftEngineBlockId = getCraftEngineBlockId(block);
        if (craftEngineBlockId == null) {
            return;
        }
        
        protectionManager.recordBlockPlacement(player, block);
        
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set(TARGET_PREFIX, CRAFTENGINE_PREFIX + craftEngineBlockId)
                .set(CRAFTENGINE_BLOCK_ID, craftEngineBlockId);
        
        actionProcessor.processAction(player, ActionType.PLACE, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CraftEngine block placed: " + craftEngineBlockId + " by " + player.getName() + " at " + block.getLocation());
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        if (!isCraftEngineBlock(block)) {
            return;
        }
        
        String craftEngineBlockId = getCraftEngineBlockId(block);
        if (craftEngineBlockId == null) {
            return;
        }
        
        if (protectionManager.isPlayerPlacedBlock(block)) {
            protectionManager.removeTrackedBlock(block);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " mined a player-placed CraftEngine block (" + craftEngineBlockId + ") - no XP awarded");
            }
            return;
        }
        
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set(TARGET_PREFIX, CRAFTENGINE_PREFIX + craftEngineBlockId)
                .set(CRAFTENGINE_BLOCK_ID, craftEngineBlockId);
        
        actionProcessor.processAction(player, ActionType.BREAK, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CraftEngine block broken: " + craftEngineBlockId + " by " + player.getName() + " at " + block.getLocation());
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        
        if (block == null || !isCraftEngineBlock(block)) {
            return;
        }
        
        String craftEngineBlockId = getCraftEngineBlockId(block);
        if (craftEngineBlockId == null) {
            return;
        }
        
        String interactType;
        boolean isRightClick = event.getAction().name().contains("RIGHT_CLICK");
        interactType = player.isSneaking() ? 
            (isRightClick ? "RIGHT_SHIFT_CLICK" : "LEFT_SHIFT_CLICK") : 
            (isRightClick ? "RIGHT_CLICK" : "LEFT_CLICK");
        
        ConditionContext context = new ConditionContext()
                .setBlock(block)
                .set(TARGET_PREFIX, CRAFTENGINE_PREFIX + craftEngineBlockId)
                .set(CRAFTENGINE_BLOCK_ID, craftEngineBlockId)
                .set("interact-type", interactType);
        
        actionProcessor.processAction(player, ActionType.BLOCK_INTERACT, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CraftEngine block interact: " + craftEngineBlockId + " by " + player.getName() + " - interact-type: " + interactType);
        }
    }
    
    /**
     * Check if a block is a CraftEngine custom block using the CraftEngine API.
     */
    private boolean isCraftEngineBlock(Block block) {
        try {
            return CraftEngineBlocks.isCustomBlock(block);
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Error checking if block is CraftEngine block: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Get the CraftEngine block ID from a block using the CraftEngine API.
     */
    private String getCraftEngineBlockId(Block block) {
        try {
            ImmutableBlockState blockState = CraftEngineBlocks.getCustomBlockState(block);
            if (blockState != null) {
                var nbtData = blockState.getNbtToSave();
                if (nbtData != null && nbtData.containsKey("id")) {
                    return nbtData.getString("id");
                }
            }
            return null;
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Error getting CraftEngine block ID: " + e.getMessage());
            }
            return null;
        }
    }
}