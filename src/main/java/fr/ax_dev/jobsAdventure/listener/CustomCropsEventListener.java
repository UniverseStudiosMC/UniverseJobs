package fr.ax_dev.jobsAdventure.listener;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.action.ActionProcessor;
import fr.ax_dev.jobsAdventure.action.ActionType;
import fr.ax_dev.jobsAdventure.condition.ConditionContext;
import fr.ax_dev.jobsAdventure.protection.BlockProtectionManager;
import net.momirealms.customcrops.api.event.CropBreakEvent;
import net.momirealms.customcrops.api.event.CropInteractEvent;
import net.momirealms.customcrops.api.event.CropPlantEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event listener for CustomCrops plugin compatibility.
 * Uses the official CustomCrops API to handle crop events with the same pattern as Nexo and ItemsAdder.
 */
public class CustomCropsEventListener implements Listener {
    
    private final JobsAdventure plugin;
    private final ActionProcessor actionProcessor;
    private final BlockProtectionManager protectionManager;
    
    // Anti-double click protection for INTERACT actions
    private final Map<UUID, Long> lastInteractTime = new HashMap<>();
    private static final long INTERACT_COOLDOWN_MS = 50; // 50ms cooldown
    
    public CustomCropsEventListener(JobsAdventure plugin, ActionProcessor actionProcessor, BlockProtectionManager protectionManager) {
        this.plugin = plugin;
        this.actionProcessor = actionProcessor;
        this.protectionManager = protectionManager;
    }
    
    /**
     * Handle CustomCrops crop break events.
     * Uses the same pattern as Nexo and ItemsAdder with target format "customcrops:crop_id".
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropBreak(CropBreakEvent event) {
        // Only handle player breaks (entity can be null for other causes)
        if (!(event.entityBreaker() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.entityBreaker();
        Location location = event.location();
    
        
        // Get crop information from the event
        String cropStageItemID = event.cropStageItemID();
        if (cropStageItemID == null || cropStageItemID.isEmpty()) {
            return;
        }
        
        // Create context with CustomCrops information following the same pattern as Nexo/ItemsAdder
        ConditionContext context = new ConditionContext()
                .setBlock(location.getBlock())
                .set("target", "customcrops:" + cropStageItemID)
                .set("customcrops_crop_stage_id", cropStageItemID);
        
        // Process the action
        actionProcessor.processAction(player, ActionType.BREAK, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CustomCrops break: " + cropStageItemID + " by " + player.getName() + " at " + location);
        }
    }
    
    /**
     * Handle CustomCrops crop interact events (replaces harvest).
     * Uses the same pattern as Nexo and ItemsAdder with target format "customcrops:crop_id".
     * Includes anti-double click protection.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropInteract(CropInteractEvent event) {
        Player player = event.getPlayer();
        Location location = event.location();
        
        // Anti-double click protection
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastInteractTime.get(playerUUID);
        
        if (lastTime != null && (currentTime - lastTime) < INTERACT_COOLDOWN_MS) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("CustomCrops interact blocked (double-click protection): " + player.getName() + " cooldown remaining: " + (INTERACT_COOLDOWN_MS - (currentTime - lastTime)) + "ms");
            }
            return;
        }
        
        // Update last interact time
        lastInteractTime.put(playerUUID, currentTime);
        
        // Get crop information from the event
        String cropStageItemID = event.cropStageItemID();
        if (cropStageItemID == null || cropStageItemID.isEmpty()) {
            return;
        }
        
        // Create context with CustomCrops information following the same pattern as Nexo/ItemsAdder
        ConditionContext context = new ConditionContext()
                .setBlock(location.getBlock())
                .set("target", "customcrops:" + cropStageItemID)
                .set("customcrops_crop_stage_id", cropStageItemID);
        
        // Process as interact action
        actionProcessor.processAction(player, ActionType.INTERACT, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CustomCrops interact/harvest: " + cropStageItemID + " by " + player.getName() + " at " + location);
        }
    }
    
    /**
     * Handle CustomCrops crop plant events.
     * Uses the same pattern as Nexo and ItemsAdder with target format "customcrops:crop_id".
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropPlant(CropPlantEvent event) {
        Player player = event.getPlayer();
        Location location = event.location();
        
        // Track the placed crop for anti-exploit protection (same as Nexo/ItemsAdder)
        protectionManager.recordBlockPlacement(player, location.getBlock());
        
        // Get crop information from the event - use the crop config ID
        String cropID = event.cropConfig().id();
        if (cropID == null || cropID.isEmpty()) {
            return;
        }
        
        // Create context with CustomCrops information following the same pattern as Nexo/ItemsAdder
        ConditionContext context = new ConditionContext()
                .setBlock(location.getBlock())
                .set("target", "customcrops:" + cropID)
                .set("customcrops_crop_id", cropID)
                .set("customcrops_crop_point", String.valueOf(event.point()));
        
        // Process the action
        boolean processed = actionProcessor.processAction(player, ActionType.PLACE, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CustomCrops plant: " + cropID + " (point: " + event.point() + ") by " + player.getName() + " at " + location);
            plugin.getLogger().info("CustomCrops context target: " + context.getTarget());
            plugin.getLogger().info("CustomCrops action processed: " + processed);
        }
    }
    
    /**
     * Clean up old interact time entries to prevent memory leaks.
     * Called periodically to remove entries older than 10 minutes.
     */
    public void cleanupOldInteractTimes() {
        long currentTime = System.currentTimeMillis();
        long cleanupThreshold = 10 * 60 * 1000; // 10 minutes
        
        lastInteractTime.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > cleanupThreshold);
    }
}