package fr.ax_dev.jobsAdventure.listener;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.action.ActionProcessor;
import fr.ax_dev.jobsAdventure.action.ActionType;
import fr.ax_dev.jobsAdventure.condition.ConditionContext;
import fr.ax_dev.jobsAdventure.protection.BlockProtectionManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Listens for job-related actions and processes them.
 */
public class JobActionListener implements Listener {
    
    private final JobsAdventure plugin;
    private final ActionProcessor actionProcessor;
    private final BlockProtectionManager protectionManager;
    
    // Performance optimization
    private final Map<UUID, Long> lastActionTime = new ConcurrentHashMap<>();
    private final AtomicLong totalEvents = new AtomicLong(0);
    private final AtomicLong processedEvents = new AtomicLong(0);
    private final AtomicLong rateLimitedEvents = new AtomicLong(0);
    
    // Reflection cache for MythicMobs
    private Method mythicMobsInstMethod;
    private Method getMobManagerMethod;
    private Method getActiveMobMethod;
    private Method getTypeMethod;
    private Method getInternalNameMethod;
    private boolean mythicMobsAvailable = false;
    private boolean mythicMobsChecked = false;
    
    // Rate limiting (per player)
    private static final long ACTION_COOLDOWN_MS = 50; // 50ms between actions
    private static final long CLEANUP_INTERVAL = 300000L; // 5 minutes
    private volatile long lastCleanup = System.currentTimeMillis();
    
    /**
     * Create a new JobActionListener.
     * 
     * @param plugin The plugin instance
     * @param actionProcessor The action processor
     * @param protectionManager The block protection manager
     */
    public JobActionListener(JobsAdventure plugin, ActionProcessor actionProcessor, BlockProtectionManager protectionManager) {
        this.plugin = plugin;
        this.actionProcessor = actionProcessor;
        this.protectionManager = protectionManager;
        
        // Initialize MythicMobs reflection cache
        initializeMythicMobsReflection();
    }
    
    /**
     * Check if action should be rate limited.
     * 
     * @param player The player performing the action
     * @return true if action should be processed, false if rate limited
     */
    private boolean checkRateLimit(Player player) {
        totalEvents.incrementAndGet();
        
        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        
        Long lastTime = lastActionTime.get(playerId);
        if (lastTime != null && (currentTime - lastTime) < ACTION_COOLDOWN_MS) {
            rateLimitedEvents.incrementAndGet();
            return false;
        }
        
        lastActionTime.put(playerId, currentTime);
        
        // Periodic cleanup
        if (currentTime - lastCleanup > CLEANUP_INTERVAL) {
            cleanupOldEntries();
        }
        
        return true;
    }
    
    /**
     * Clean up old entries from the rate limiting map.
     */
    private void cleanupOldEntries() {
        long cutoffTime = System.currentTimeMillis() - (ACTION_COOLDOWN_MS * 10);
        lastActionTime.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
        lastCleanup = System.currentTimeMillis();
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("JobActionListener cleanup completed. Active entries: " + lastActionTime.size());
        }
    }
    
    /**
     * Initialize MythicMobs reflection methods for better performance.
     */
    private void initializeMythicMobsReflection() {
        try {
            if (plugin.getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
                Class<?> mythicMobsAPI = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
                mythicMobsInstMethod = mythicMobsAPI.getMethod("inst");
                
                Object apiInstance = mythicMobsInstMethod.invoke(null);
                getMobManagerMethod = apiInstance.getClass().getMethod("getMobManager");
                
                Object mobManager = getMobManagerMethod.invoke(apiInstance);
                getActiveMobMethod = mobManager.getClass().getMethod("getActiveMob", org.bukkit.entity.Entity.class);
                
                // Cache methods for mob type retrieval
                getTypeMethod = Class.forName("io.lumine.mythic.core.mobs.ActiveMob").getMethod("getType");
                getInternalNameMethod = Class.forName("io.lumine.mythic.core.mobs.MobType").getMethod("getInternalName");
                
                mythicMobsAvailable = true;
                plugin.getLogger().info("MythicMobs integration initialized with reflection cache");
            }
        } catch (Exception e) {
            mythicMobsAvailable = false;
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("MythicMobs not available or failed to initialize: " + e.getMessage());
            }
        }
        mythicMobsChecked = true;
    }
    
    /**
     * Get performance statistics.
     * 
     * @return Map containing performance data
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("total_events", totalEvents.get());
        stats.put("processed_events", processedEvents.get());
        stats.put("rate_limited_events", rateLimitedEvents.get());
        stats.put("active_players", lastActionTime.size());
        stats.put("mythicmobs_available", mythicMobsAvailable);
        
        long processed = processedEvents.get();
        long total = totalEvents.get();
        double processingRate = total > 0 ? (double) processed / total * 100 : 0;
        stats.put("processing_rate_percent", processingRate);
        
        return stats;
    }
    
    /**
     * Handle entity deaths (KILL action).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity killed = event.getEntity();
        Player killer = null;
        
        // getKiller() is only available on LivingEntity
        if (killed instanceof LivingEntity) {
            killer = ((LivingEntity) killed).getKiller();
        }
        
        if (killer == null) return;
        
        // Rate limiting check
        if (!checkRateLimit(killer)) {
            return;
        }
        
        try {
            // Create context
            ConditionContext context = new ConditionContext()
                    .setEntity(killed)
                    .set("target", killed.getType().name());
            
            // Check for MythicMobs using cached reflection
            if (mythicMobsAvailable) {
                handleMythicMobKillOptimized(killer, killed, context);
            }
            
            // Process the action
            actionProcessor.processAction(killer, ActionType.KILL, event, context);
            processedEvents.incrementAndGet();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing KILL action for player " + killer.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Handle MythicMobs entity kills (legacy method - kept for compatibility).
     */
    private void handleMythicMobKill(Player killer, Entity killed, ConditionContext context) {
        if (mythicMobsAvailable) {
            handleMythicMobKillOptimized(killer, killed, context);
        }
    }
    
    /**
     * Handle MythicMobs entity kills using cached reflection methods.
     */
    private void handleMythicMobKillOptimized(Player killer, Entity killed, ConditionContext context) {
        try {
            // Use cached reflection methods for better performance
            Object apiInstance = mythicMobsInstMethod.invoke(null);
            Object mobManager = getMobManagerMethod.invoke(apiInstance);
            Object activeMob = getActiveMobMethod.invoke(mobManager, killed);
            
            if (activeMob != null) {
                // Get MythicMob internal name using cached methods
                Object mobType = getTypeMethod.invoke(activeMob);
                String internalName = (String) getInternalNameMethod.invoke(mobType);
                
                // Set MythicMob target
                context.set("target", "MYTHICMOB:" + internalName);
                context.set("mythicmob", true);
                context.set("mythicmob_type", internalName);
            }
        } catch (Exception e) {
            // Error occurred - log and fall back
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Failed to check MythicMobs data with cached reflection: " + e.getMessage());
            }
            
            // Disable MythicMobs integration if there are persistent errors
            mythicMobsAvailable = false;
        }
    }
    
    /**
     * Handle block breaking (BREAK action).
     * Works with both vanilla blocks and Nexo custom blocks.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // Rate limiting check
        if (!checkRateLimit(player)) {
            return;
        }
        
        try {
            // Check if this block was placed by a player (anti-exploit for both vanilla and Nexo blocks)
            if (protectionManager.isPlayerPlacedBlock(event.getBlock())) {
                // Remove from tracking but don't give XP
                protectionManager.removeTrackedBlock(event.getBlock());
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Player " + player.getName() + " mined a player-placed block - no XP awarded");
                }
                return;
            }
            
            // Create context
            ConditionContext context = new ConditionContext()
                    .setBlock(event.getBlock())
                    .set("target", event.getBlock().getType().name());
            
            // Process the action and check if we should cancel
            boolean shouldCancel = actionProcessor.processAction(player, ActionType.BREAK, event, context);
            if (shouldCancel) {
                event.setCancelled(true);
            }
            
            processedEvents.incrementAndGet();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing BREAK action for player " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Handle block placement (PLACE action).
     * Works with both vanilla blocks and Nexo custom blocks.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        // Track the placed block for anti-exploit protection (handles both vanilla and Nexo blocks)
        protectionManager.recordBlockPlacement(player, event.getBlock());
        
        // Create context
        ConditionContext context = new ConditionContext()
                .setBlock(event.getBlock())
                .set("target", event.getBlock().getType().name());
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.PLACE, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
            // If cancelled, remove the block from tracking
            protectionManager.removeTrackedBlock(event.getBlock());
        }
    }
    
    /**
     * Handle animal breeding (BREED action).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) return;
        
        // Create context
        ConditionContext context = new ConditionContext()
                .setEntity(event.getEntity())
                .set("target", event.getEntityType().name());
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.BREED, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle fishing (FISH action).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        
        Player player = event.getPlayer();
        
        // Create context
        ConditionContext context = new ConditionContext()
                .set("target", "FISH");
        
        if (event.getCaught() != null) {
            context.setEntity(event.getCaught());
        }
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.FISH, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle animal taming (TAME action).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) return;
        
        // Create context
        ConditionContext context = new ConditionContext()
                .setEntity(event.getEntity())
                .set("target", event.getEntityType().name());
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.TAME, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle sheep shearing (SHEAR action).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        Player player = event.getPlayer();
        
        // Create context
        ConditionContext context = new ConditionContext()
                .setEntity(event.getEntity())
                .set("target", event.getEntity().getType().name());
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.SHEAR, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle food consumption (EAT action).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        
        // Create context
        ConditionContext context = new ConditionContext()
                .setItem(event.getItem())
                .set("target", event.getItem().getType().name());
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.EAT, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
    }
    
}