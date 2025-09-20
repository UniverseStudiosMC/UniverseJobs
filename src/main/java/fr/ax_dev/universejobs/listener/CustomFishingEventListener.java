package fr.ax_dev.universejobs.listener;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionProcessor;
import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.condition.ConditionContext;
import net.momirealms.customfishing.api.event.FishingResultEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event listener for CustomFishing plugin compatibility.
 * Uses the official CustomFishing API to handle fishing events with target format "customfishing:fish_id".
 */
public class CustomFishingEventListener implements Listener {
    
    private final UniverseJobs plugin;
    private final ActionProcessor actionProcessor;
    
    // Anti-double action protection for fishing events
    private final Map<UUID, Long> lastFishingTime = new HashMap<>();
    private static final long FISHING_COOLDOWN_MS = 100; // 100ms cooldown
    
    public CustomFishingEventListener(UniverseJobs plugin, ActionProcessor actionProcessor) {
        this.plugin = plugin;
        this.actionProcessor = actionProcessor;

        // Always log creation (not just in debug mode)
        plugin.getLogger().info("CustomFishingEventListener initialized successfully");
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CustomFishing integration enabled with debug mode");
        }
    }
    /**
     * Handle CustomFishing result events.
     * Uses the same pattern as Nexo and ItemsAdder with target format "customfishing:fish_id".
     * Includes anti-double action protection.
     */   

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFishingResult(FishingResultEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CustomFishing trigger onFishingResult");
        }

        if (event.getResult() != FishingResultEvent.Result.SUCCESS) {
            return;
        }
        // Anti-double action protection
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastFishingTime.get(playerUUID);
        
        if (lastTime != null && (currentTime - lastTime) < FISHING_COOLDOWN_MS) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("CustomFishing result blocked (double-action protection): " + player.getName() + " cooldown remaining: " + (FISHING_COOLDOWN_MS - (currentTime - lastTime)) + "ms");
            }
            return;
        }
        
        // Update last fishing time
        lastFishingTime.put(playerUUID, currentTime);
        
        // Get fish information from the loot
        if (event.getLoot() == null) {
            return;
        }

        String fishId = event.getLoot().id();

        if (fishId == null || fishId.isEmpty()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("CustomFishing: Unable to determine fish ID from loot");
            }
            return;
        }
        
        // Create context with CustomFishing information following the same pattern as Nexo/ItemsAdder
        ConditionContext context = new ConditionContext()
                .set("target", "customfishing:" + fishId)
                .set("customfishing_fish_id", fishId)
                .set("loot", event.getLoot());
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CustomFishing caught: customfishing:" + fishId + " by " + player.getName());
            plugin.getLogger().info("Processing FISH action with target: customfishing:" + fishId + " by " + player.getName());
        }
        
        // Process as fish action
        actionProcessor.processAction(player, ActionType.FISH, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CustomFishing result processed: " + fishId + " by " + player.getName() + " at " + player.getLocation());
        }
    }
    /**
     * Clean up old fishing time entries to prevent memory leaks.
     * Called periodically to remove entries older than 10 minutes.
     */
    public void cleanupOldFishingTimes() {
        long currentTime = System.currentTimeMillis();
        long cleanupThreshold = 10L * 60 * 1000; // 10 minutes
        
        lastFishingTime.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > cleanupThreshold);
    }
}