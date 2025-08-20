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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.GameMode;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.block.Action;

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
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Block break event: " + event.getBlock().getType() + " by " + player.getName());
        }
        
        // Rate limiting check
        if (!checkRateLimit(player)) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Rate limited for player " + player.getName());
            }
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
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Processing BREAK action for " + player.getName() + " - target: " + event.getBlock().getType().name());
            }
            
            // Process the action and check if we should cancel
            boolean shouldCancel = actionProcessor.processAction(player, ActionType.BREAK, event, context);
            if (shouldCancel) {
                event.setCancelled(true);
            }
            
            processedEvents.incrementAndGet();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing BREAK action for player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
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
     * Handle food and potion consumption (EAT and POTION actions).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        
        // Create context
        ConditionContext context = new ConditionContext()
                .setItem(event.getItem())
                .set("target", event.getItem().getType().name());
        
        // Determine if it's a potion or food
        ActionType actionType;
        if (event.getItem().getType().name().contains("POTION")) {
            actionType = ActionType.POTION;
        } else {
            actionType = ActionType.EAT;
        }
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, actionType, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle block interactions (BLOCK_INTERACT action).
     * Only handles RIGHT_CLICK interactions - left clicks that break blocks are handled by BlockBreakEvent
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Only handle main hand interactions to avoid duplicate events
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        // Only handle block interactions (right click on blocks)
        if (event.getClickedBlock() == null) {
            return;
        }
        
        // Only handle RIGHT_CLICK actions - left clicks are handled by BlockBreakEvent
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Rate limiting check
        if (!checkRateLimit(player)) {
            return;
        }
        
        // Determine interact type (only right clicks)
        String interactType = player.isSneaking() ? "RIGHT_SHIFT_CLICK" : "RIGHT_CLICK";
        
        // Create context
        ConditionContext context = new ConditionContext()
                .setBlock(event.getClickedBlock())
                .set("target", event.getClickedBlock().getType().name())
                .set("interact-type", interactType);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Block interact (RIGHT_CLICK): " + event.getClickedBlock().getType() + " by " + player.getName() + " - interact-type: " + interactType);
        }
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.BLOCK_INTERACT, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
        
        processedEvents.incrementAndGet();
    }
    
    /**
     * Handle LEFT_CLICK block interactions that don't break blocks (BLOCK_INTERACT action).
     * This handles LEFT_CLICK interactions where the block isn't actually broken.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerLeftClickBlock(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Only handle LEFT_CLICK_BLOCK actions
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        // Only handle main hand interactions to avoid duplicate events
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        // Only handle block interactions
        if (event.getClickedBlock() == null) {
            return;
        }
        
        // Skip if this left click will break the block (to avoid conflict with BlockBreakEvent)
        if (willBreakBlock(player, event.getClickedBlock())) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Skipping LEFT_CLICK interact - block will be broken by BlockBreakEvent");
            }
            return;
        }
        
        // Rate limiting check
        if (!checkRateLimit(player)) {
            return;
        }
        
        // Determine interact type (left clicks)
        String interactType = player.isSneaking() ? "LEFT_SHIFT_CLICK" : "LEFT_CLICK";
        
        // Create context
        ConditionContext context = new ConditionContext()
                .setBlock(event.getClickedBlock())
                .set("target", event.getClickedBlock().getType().name())
                .set("interact-type", interactType);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Block interact (LEFT_CLICK): " + event.getClickedBlock().getType() + " by " + player.getName() + " - interact-type: " + interactType);
        }
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.BLOCK_INTERACT, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
        
        processedEvents.incrementAndGet();
    }
    
    /**
     * Check if a left click will break the block.
     * This helps avoid conflicts between PlayerInteractEvent and BlockBreakEvent.
     */
    private boolean willBreakBlock(Player player, org.bukkit.block.Block block) {
        // In creative mode, left click always breaks blocks instantly
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        
        // In survival/adventure, check if player can break the block
        // This is a simplified check - you might want to make it more sophisticated
        if (player.getGameMode() == GameMode.SURVIVAL || 
            player.getGameMode() == GameMode.ADVENTURE) {
            
            // Check if block is breakable and player has proper tool
            // For now, assume most blocks can be broken in survival mode
            return block.getType().getHardness() >= 0; // Negative hardness means unbreakable
        }
        
        // In spectator mode, can't break blocks
        return false;
    }
    
    /**
     * Handle entity interactions (ENTITY_INTERACT action) - Right click.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        
        // Only handle main hand interactions to avoid duplicate events
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        // Rate limiting check
        if (!checkRateLimit(player)) {
            return;
        }
        
        // Determine interact type (right-click interaction)
        String interactType = player.isSneaking() ? "RIGHT_SHIFT_CLICK" : "RIGHT_CLICK";
        
        // Create context
        ConditionContext context = new ConditionContext()
                .setEntity(entity)
                .set("target", entity.getType().name())
                .set("interact-type", interactType);
        
        // Check for MythicMobs
        if (mythicMobsAvailable) {
            try {
                Object apiInstance = mythicMobsInstMethod.invoke(null);
                Object mobManager = getMobManagerMethod.invoke(apiInstance);
                Object activeMob = getActiveMobMethod.invoke(mobManager, entity);
                
                if (activeMob != null) {
                    Object mobType = getTypeMethod.invoke(activeMob);
                    String internalName = (String) getInternalNameMethod.invoke(mobType);
                    context.set("target", "mythicmobs:" + internalName);
                    context.set("mythicmob", true);
                    context.set("mythicmob_type", internalName);
                }
            } catch (Exception e) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().warning("Failed to check MythicMobs data: " + e.getMessage());
                }
            }
        }
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.ENTITY_INTERACT, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
        
        processedEvents.incrementAndGet();
    }
    
    /**
     * Handle item crafting (CRAFT action).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Rate limiting check
        if (!checkRateLimit(player)) {
            return;
        }
        
        // Get the crafted item
        if (event.getCurrentItem() == null) return;
        
        // Create context
        ConditionContext context = new ConditionContext()
                .setItem(event.getCurrentItem())
                .set("target", event.getCurrentItem().getType().name())
                .set("amount", event.getCurrentItem().getAmount());
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Craft event: " + event.getCurrentItem().getType() + " x" + event.getCurrentItem().getAmount() + " by " + player.getName());
        }
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.CRAFT, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
        
        processedEvents.incrementAndGet();
    }
    
    /**
     * Handle entity interactions (ENTITY_INTERACT action) - Left click.
     * We use EntityDamageByEntityEvent with 0 damage to detect left-clicks.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamageForInteract(EntityDamageByEntityEvent event) {
        // Only handle player damage
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        
        Entity entity = event.getEntity();
        
        // Rate limiting check
        if (!checkRateLimit(player)) {
            return;
        }
        
        // Determine interact type (left-click interaction)
        String interactType = player.isSneaking() ? "LEFT_SHIFT_CLICK" : "LEFT_CLICK";
        
        // Create context
        ConditionContext context = new ConditionContext()
                .setEntity(entity)
                .set("target", entity.getType().name())
                .set("interact-type", interactType);
        
        // Check for MythicMobs
        if (mythicMobsAvailable) {
            try {
                Object apiInstance = mythicMobsInstMethod.invoke(null);
                Object mobManager = getMobManagerMethod.invoke(apiInstance);
                Object activeMob = getActiveMobMethod.invoke(mobManager, entity);
                
                if (activeMob != null) {
                    Object mobType = getTypeMethod.invoke(activeMob);
                    String internalName = (String) getInternalNameMethod.invoke(mobType);
                    context.set("target", "mythicmobs:" + internalName);
                    context.set("mythicmob", true);
                    context.set("mythicmob_type", internalName);
                }
            } catch (Exception e) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().warning("Failed to check MythicMobs data: " + e.getMessage());
                }
            }
        }
        
        // Process the action and check if we should cancel
        // We cancel the damage event if the action is processed to prevent actual damage
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.ENTITY_INTERACT, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
        
        processedEvents.incrementAndGet();
    }
    
}