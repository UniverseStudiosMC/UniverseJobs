package fr.ax_dev.universejobs.listener;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionProcessor;
import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.condition.ConditionContext;
import fr.ax_dev.universejobs.integration.MythicMobsHandler;
import fr.ax_dev.universejobs.protection.BlockProtectionManager;
import fr.ax_dev.universejobs.cache.ConfigurationCache;
import fr.ax_dev.universejobs.cache.PlayerJobCache;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Sheep;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.Action;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Listens for job-related actions and processes them.
 */
public class JobActionListener implements Listener {
    
    private static final String TARGET_SUFFIX = " - target: ";
    private static final String COLOR_CODE_PATTERN = "§[0-9a-fk-or]";
    private static final String TARGET_KEY = "target";
    
    private final UniverseJobs plugin;
    private final ActionProcessor actionProcessor;
    private final BlockProtectionManager protectionManager;
    private final MythicMobsHandler mythicMobsHandler;
    private final ConfigurationCache configCache;
    private final PlayerJobCache playerCache;
    
    // Statistiques ultra-légères
    private final AtomicLong totalEvents = new AtomicLong(0);
    private final AtomicLong processedEvents = new AtomicLong(0);
    
    // Furnace tracking for SMELT action - tracks who put items in each furnace
    private final Map<String, UUID> furnaceOwners = new ConcurrentHashMap<>();
    private final Map<String, Long> furnaceLastUse = new ConcurrentHashMap<>();
    /**
     * Create a new ultra-fast JobActionListener with caching.
     * 
     * @param plugin The plugin instance
     * @param actionProcessor The action processor
     * @param protectionManager The block protection manager
     * @param mythicMobsHandler The MythicMobs integration handler
     * @param configCache The configuration cache
     * @param playerCache The player cache
     */
    public JobActionListener(UniverseJobs plugin, ActionProcessor actionProcessor, BlockProtectionManager protectionManager, 
                           MythicMobsHandler mythicMobsHandler, ConfigurationCache configCache, PlayerJobCache playerCache) {
        this.plugin = plugin;
        this.actionProcessor = actionProcessor;
        this.protectionManager = protectionManager;
        this.mythicMobsHandler = mythicMobsHandler;
        this.configCache = configCache;
        this.playerCache = playerCache;
    }
    
    
    /**
     * Get performance statistics avec cache ultra-rapide.
     * 
     * @return Map containing performance data
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("total_events", totalEvents.get());
        stats.put("processed_events", processedEvents.get());
        stats.put("mythicmobs_available", mythicMobsHandler.isAvailable());
        
        // Intègre les stats du cache
        stats.putAll(playerCache.getStats());
        
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
        totalEvents.incrementAndGet();
        
        try {
            // Create context
            ConditionContext context = new ConditionContext()
                    .setEntity(killed)
                    .set(TARGET_KEY, killed.getType().name());
            
            // Check for MythicMobs using official API
            mythicMobsHandler.populateMythicMobContext(killed, context);
            
            // Process the action
            actionProcessor.processAction(killer, ActionType.KILL, event, context);
            processedEvents.incrementAndGet();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing KILL action for player " + killer.getName() + ": " + e.getMessage());
        }
    }
    
    
    /**
     * Handle block breaking (BREAK action).
     * Works with both vanilla blocks and Nexo custom blocks.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // Debug avec cache instantané
        if (configCache.isDebugEnabled()) {
            plugin.getLogger().info("Block break event: " + event.getBlock().getType() + " by " + player.getName());
        }
        
        totalEvents.incrementAndGet();
        
        try {
            // Check if this block was placed by a player (anti-exploit for both vanilla and Nexo blocks)
            if (protectionManager.isPlayerPlacedBlock(event.getBlock())) {
                // Remove from tracking but don't give XP
                protectionManager.removeTrackedBlock(event.getBlock());
                
                if (configCache.isDebugEnabled()) {
                    plugin.getLogger().info("Player " + player.getName() + " mined a player-placed block - no XP awarded");
                }
                return;
            }
            
            // Create context
            ConditionContext context = new ConditionContext()
                    .setBlock(event.getBlock())
                    .set(TARGET_KEY, event.getBlock().getType().name());
            
            if (configCache.isDebugEnabled()) {
                plugin.getLogger().info("Processing BREAK action for " + player.getName() + TARGET_SUFFIX + event.getBlock().getType().name());
            }
            
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
                .set(TARGET_KEY, event.getBlock().getType().name());
        
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
                .set(TARGET_KEY, event.getEntityType().name());
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.BREED, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle fishing (FISH action).
     * Supports both vanilla fish and items from fishing.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        
        Player player = event.getPlayer();
        String target = "FISH"; // Default fallback
        
        // Try to determine the specific fish type caught
        if (event.getCaught() instanceof org.bukkit.entity.Item) {
            org.bukkit.entity.Item caughtItem = (org.bukkit.entity.Item) event.getCaught();
            org.bukkit.inventory.ItemStack itemStack = caughtItem.getItemStack();
            
            if (itemStack != null) {
                // Use the material name as target for vanilla fish
                target = itemStack.getType().name();
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Vanilla fishing caught: " + target + " by " + player.getName());
                }
            }
        }
        
        // Create context with specific fish type
        ConditionContext context = new ConditionContext()
                .set(TARGET_KEY, target);
        
        if (event.getCaught() != null) {
            context.setEntity(event.getCaught());
            
            // Add item info if it's an item entity
            if (event.getCaught() instanceof org.bukkit.entity.Item) {
                org.bukkit.entity.Item itemEntity = (org.bukkit.entity.Item) event.getCaught();
                context.setItem(itemEntity.getItemStack());
            }
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Processing FISH action with target: " + target + " by " + player.getName());
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
                .set(TARGET_KEY, event.getEntityType().name());
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.TAME, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle sheep shearing (SHEAR action).
     * Supports color filtering for sheep.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        Player player = event.getPlayer();
        
        // Create context
        ConditionContext context = new ConditionContext()
                .setEntity(event.getEntity())
                .set(TARGET_KEY, event.getEntity().getType().name());
        
        // Add sheep color information if it's a sheep
        if (event.getEntity() instanceof Sheep) {
            Sheep sheep = (Sheep) event.getEntity();
            context.set("color", sheep.getColor().name());
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Shearing sheep with color: " + sheep.getColor().name() + 
                    " by " + player.getName());
            }
        }
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.SHEAR, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle food and potion consumption (EAT and POTION actions).
     * Supports custom items (CustomCrops, CustomFishing, Nexo, ItemsAdder) and NBT detection.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null) return;
        
        // Detect the target format for the item, supporting all plugin formats
        String target = detectItemTarget(item);
        
        // Create context with enhanced item information
        ConditionContext context = new ConditionContext()
                .setItem(item)
                .set(TARGET_KEY, target);
        
        // Add NBT information if available
        if (item.hasItemMeta()) {
            // Add custom item information to context
            addCustomItemContext(item, context);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " consumed item with target: " + target);
            }
        }
        
        // Determine if it's a potion or food
        ActionType actionType;
        if (item.getType().name().contains("POTION")) {
            actionType = ActionType.POTION;
            
            // Add potion-specific information
            addPotionContext(item, context);
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
        totalEvents.incrementAndGet();
        
        // Determine interact type (only right clicks)
        String interactType = player.isSneaking() ? "RIGHT_SHIFT_CLICK" : "RIGHT_CLICK";
        
        // Create context
        ConditionContext context = new ConditionContext()
                .setBlock(event.getClickedBlock())
                .set(TARGET_KEY, event.getClickedBlock().getType().name())
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
        totalEvents.incrementAndGet();
        
        // Determine interact type (right-click interaction)
        String interactType = player.isSneaking() ? "RIGHT_SHIFT_CLICK" : "RIGHT_CLICK";
        
        // Create context
        ConditionContext context = new ConditionContext()
                .setEntity(entity)
                .set(TARGET_KEY, entity.getType().name())
                .set("interact-type", interactType);
        
        // Check for MythicMobs using official API
        mythicMobsHandler.populateMythicMobContext(entity, context);
        
        // Process the action and check if we should cancel
        boolean shouldCancel = actionProcessor.processAction(player, ActionType.ENTITY_INTERACT, event, context);
        if (shouldCancel) {
            event.setCancelled(true);
        }
        
        processedEvents.incrementAndGet();
    }
    
    /**
     * Handle item crafting (CRAFT action).
     * Post-detection for shift-clicks.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!isValidCraftEvent(event)) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        totalEvents.incrementAndGet();
        
        ItemStack resultStack = event.getRecipe().getResult();
        ItemStack toCraft = event.getCurrentItem();
        
        if (!canProcessCraftEvent(player, event, resultStack, toCraft)) {
            return;
        }
        
        debugLogCraftEvent(player, event, toCraft);
        processCraftAction(player, event, resultStack, toCraft);
        
        processedEvents.incrementAndGet();
    }
    
    /**
     * Validate if the craft event should be processed.
     */
    private boolean isValidCraftEvent(CraftItemEvent event) {
        // Filter invalid actions
        switch (event.getAction()) {
            case NOTHING:
            case PLACE_ONE:
            case PLACE_ALL:
            case PLACE_SOME:
                return false;
            default:
                // Continue with validation for other action types
        }
        
        // Must be result slot
        if (event.getSlotType() != org.bukkit.event.inventory.InventoryType.SlotType.RESULT) {
            return false;
        }
        
        // Must be left or right click
        return event.isLeftClick() || event.isRightClick();
    }
    
    /**
     * Check if the craft event can be processed.
     */
    private boolean canProcessCraftEvent(Player player, CraftItemEvent event, ItemStack resultStack, ItemStack toCraft) {
        // Check if inventory can accept the crafted items (prevent duplication)
        if (event.isShiftClick() && !canInventoryAcceptItems(player.getInventory(), resultStack)) {
            debugLog("Craft blocked - inventory cannot accept items with shift-click by " + player.getName());
            return false;
        }
        
        // Make sure we are actually crafting anything
        return hasItems(toCraft);
    }
    
    /**
     * Log debug information about craft event.
     */
    private void debugLogCraftEvent(Player player, CraftItemEvent event, ItemStack toCraft) {
        debugLog("Craft event: " + toCraft.getType() + 
            " x" + toCraft.getAmount() + 
            ", Action: " + event.getAction() + 
            ", Shift-click: " + event.isShiftClick() + 
            " by " + player.getName());
    }
    
    /**
     * Process the crafting action.
     */
    private void processCraftAction(Player player, CraftItemEvent event, ItemStack resultStack, ItemStack toCraft) {
        if (event.isShiftClick()) {
            // Use post-detection for shift-clicks
            schedulePostDetection(player, toCraft.clone(), resultStack.clone());
        } else {
            // Direct processing for normal clicks
            // The items are stored in the cursor. Make sure there's enough space.
            if (isStackSumLegal(toCraft, event.getCursor())) {
                int craftCount = toCraft.getAmount() / resultStack.getAmount();
                processCraftRewards(player, resultStack, craftCount);
            }
        }
    }
    
    /**
     * Helper method for debug logging.
     */
    private void debugLog(String message) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info(message);
        }
    }
    
    /**
     * Schedule post-detection for shift-click crafting
     * Compare inventory before and after to determine actual crafted amount.
     * 
     * @param player The player crafting
     * @param compareItem The item to compare
     * @param resultStack The recipe result
     */
    private void schedulePostDetection(Player player, org.bukkit.inventory.ItemStack compareItem, org.bukkit.inventory.ItemStack resultStack) {
        final org.bukkit.inventory.ItemStack[] preInv = cloneInventoryContents(player.getInventory().getContents());
        
        // Schedule comparison for next tick
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> 
            processPostDetectionComparison(player, compareItem, resultStack, preInv), 1L);
    }
    
    /**
     * Clone inventory contents to avoid mutation issues.
     */
    private org.bukkit.inventory.ItemStack[] cloneInventoryContents(org.bukkit.inventory.ItemStack[] contents) {
        org.bukkit.inventory.ItemStack[] cloned = new org.bukkit.inventory.ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                cloned[i] = contents[i].clone();
            }
        }
        return cloned;
    }
    
    /**
     * Process the comparison between pre and post crafting inventories.
     */
    private void processPostDetectionComparison(Player player, org.bukkit.inventory.ItemStack compareItem, 
                                              org.bukkit.inventory.ItemStack resultStack, org.bukkit.inventory.ItemStack[] preInv) {
        final org.bukkit.inventory.ItemStack[] postInv = player.getInventory().getContents();
        int newItemsCount = calculateNewItemsCount(preInv, postInv, compareItem);
        
        if (resultStack != null && newItemsCount > 0) {
            int craftCount = newItemsCount / resultStack.getAmount();
            debugLog("Post-detection: " + player.getName() + " crafted " + newItemsCount + " " + compareItem.getType() + " (" + craftCount + " crafts)");
            processCraftRewards(player, resultStack, craftCount);
        }
    }
    
    /**
     * Calculate the number of new items by comparing inventories.
     */
    private int calculateNewItemsCount(org.bukkit.inventory.ItemStack[] preInv, org.bukkit.inventory.ItemStack[] postInv, 
                                     org.bukkit.inventory.ItemStack compareItem) {
        int newItemsCount = 0;
        
        // Count new items by comparing before/after inventory
        for (int i = 0; i < preInv.length; i++) {
            org.bukkit.inventory.ItemStack pre = preInv[i];
            org.bukkit.inventory.ItemStack post = postInv[i];
            
            // We're only interested in filled slots that are different
            if (hasSameItem(compareItem, post) && (hasSameItem(compareItem, pre) || pre == null)) {
                newItemsCount += post.getAmount() - (pre != null ? pre.getAmount() : 0);
            }
        }
        
        return newItemsCount;
    }
    
    /**
     * Process craft rewards for the player.
     * 
     * @param player The player
     * @param resultStack The crafted item
     * @param craftCount Number of times the recipe was executed
     */
    private void processCraftRewards(Player player, org.bukkit.inventory.ItemStack resultStack, int craftCount) {
        if (craftCount <= 0) return;
        
        // Create context with craft multiplier
        ConditionContext context = new ConditionContext()
                .setItem(resultStack)
                .set(TARGET_KEY, resultStack.getType().name())
                .set("amount", resultStack.getAmount() * craftCount)
                .set("recipe_yield", resultStack.getAmount())
                .set("recipe_executions", craftCount)
                .set("craft_multiplier", craftCount);
        
        // Process the action with multiplier
        actionProcessor.processAction(player, ActionType.CRAFT, null, context);
    }
    
    /**
     * Check if an ItemStack has items (not null and amount > 0).
     * 
     * @param stack The ItemStack to check
     * @return true if it has items
     */
    private boolean hasItems(org.bukkit.inventory.ItemStack stack) {
        return stack != null && stack.getAmount() > 0;
    }
    
    /**
     * Check if two ItemStacks are the same item (ignoring amount).
     * 
     * @param a First ItemStack
     * @param b Second ItemStack
     * @return true if they're the same item
     */
    private boolean hasSameItem(org.bukkit.inventory.ItemStack a, org.bukkit.inventory.ItemStack b) {
        if (a == null) return b == null;
        else if (b == null) return false;
        
        return a.getType() == b.getType() && 
               a.getDurability() == b.getDurability() &&
               java.util.Objects.equals(a.getEnchantments(), b.getEnchantments()) &&
               java.util.Objects.equals(a.getItemMeta(), b.getItemMeta());
    }
    
    /**
     * Check if the sum of two stacks is legal (doesn't exceed max stack size).
     * 
     * @param a First ItemStack
     * @param b Second ItemStack  
     * @return true if sum is legal
     */
    private boolean isStackSumLegal(ItemStack a, ItemStack b) {
        // Treat null as empty stack
        if (a == null || b == null) return true;
        
        return a.getAmount() + b.getAmount() <= a.getType().getMaxStackSize();
    }
    
    /**
     * Check if an inventory can accept items by considering both empty slots and stackable items.
     * 
     * @param inventory The inventory to check
     * @param itemToAdd The item that would be added
     * @return true if the inventory can accept at least some of the items
     */
    private boolean canInventoryAcceptItems(org.bukkit.inventory.PlayerInventory inventory, org.bukkit.inventory.ItemStack itemToAdd) {
        if (itemToAdd == null || itemToAdd.getAmount() <= 0) {
            return true;
        }
        
        // Check for empty slots first (fastest check)
        if (inventory.firstEmpty() != -1) {
            return true; // Has empty slots, can definitely accept items
        }
        
        // No empty slots - check if we can stack with existing items
        int maxStackSize = itemToAdd.getMaxStackSize();
        
        // Go through inventory slots (exclude armor, offhand, etc.)
        for (int i = 0; i < 36; i++) { // Main inventory slots (0-35)
            org.bukkit.inventory.ItemStack slot = inventory.getItem(i);
            
            if (slot != null && hasSameItem(itemToAdd, slot)) {
                // Found same item - check if we can add to this stack
                if (slot.getAmount() < maxStackSize) {
                    return true; // Can stack with this item
                }
            }
        }
        
        // No empty slots and no stackable items found
        return false;
    }
    
    /**
     * Track furnace interactions to know who should get XP for smelting.
     * Monitors when players put items into furnace input slots.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        // Only track furnace inventories
        if (event.getInventory().getType() != InventoryType.FURNACE) {
            return;
        }
        
        // Only track players
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // Skip if not putting items into the furnace (slot 0 = input, slot 1 = fuel, slot 2 = result)
        if (event.getSlot() != 0 && event.getSlot() != 1) {
            return; // Only track input and fuel slots
        }
        
        // Skip if removing items (empty cursor means putting items in)
        if (event.getCursor() == null || event.getCursor().getType().isAir()) {
            return; // Not adding items to furnace
        }
        
        // Get furnace location as key
        org.bukkit.Location furnaceLocation = event.getInventory().getLocation();
        if (furnaceLocation == null) {
            return;
        }
        
        String furnaceKey = locationToKey(furnaceLocation);
        long currentTime = System.currentTimeMillis();
        
        // Track this player as the owner of this furnace
        furnaceOwners.put(furnaceKey, player.getUniqueId());
        furnaceLastUse.put(furnaceKey, currentTime);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Furnace owner tracked: " + player.getName() + 
                " at " + furnaceLocation.toString() + " (slot " + event.getSlot() + ")");
        }
    }
    
    /**
     * Convert location to string key for furnace tracking.
     */
    private String locationToKey(org.bukkit.Location location) {
        return location.getWorld().getName() + ":" + 
               location.getBlockX() + ":" + 
               location.getBlockY() + ":" + 
               location.getBlockZ();
    }
    
    /**
     * Handle item smelting (SMELT action).
     * Supports detection of nexo:, itemsadder:, customfishing:, and customcrops: items.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        // Find the player who owns this furnace by checking our tracking system
        Player player = getFurnaceOwner(event.getBlock().getLocation());
        
        if (player == null) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("No tracked owner for furnace at " + event.getBlock().getLocation() + " - no XP awarded");
            }
            return; // No tracked owner for this furnace
        }
        
        // Rate limiting check
        totalEvents.incrementAndGet();
        
        ItemStack result = event.getResult();
        if (result == null) {
            return;
        }
        
        try {
            // Create context with item information
            ConditionContext context = new ConditionContext()
                    .setItem(result)
                    .set(TARGET_KEY, detectItemTarget(result))
                    .set("amount", result.getAmount());
            
            // Add source item information if available
            ItemStack source = event.getSource();
            if (source != null) {
                context.set("source_target", detectItemTarget(source))
                       .set("source_amount", source.getAmount());
            }
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Processing SMELT action for " + player.getName() + 
                    TARGET_SUFFIX + context.get(TARGET_KEY) + 
                    " - source: " + context.get("source_target"));
            }
            
            // Process the action
            actionProcessor.processAction(player, ActionType.SMELT, event, context);
            processedEvents.incrementAndGet();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing SMELT action for player " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Get the owner of a furnace based on our tracking system.
     * Returns the player who last put items into this furnace.
     */
    private Player getFurnaceOwner(org.bukkit.Location furnaceLocation) {
        String furnaceKey = locationToKey(furnaceLocation);
        UUID ownerUUID = furnaceOwners.get(furnaceKey);
        
        if (ownerUUID == null) {
            return null; // No tracked owner
        }
        
        Player owner = plugin.getServer().getPlayer(ownerUUID);
        if (owner == null || !owner.isOnline()) {
            // Player is offline, clean up tracking
            furnaceOwners.remove(furnaceKey);
            furnaceLastUse.remove(furnaceKey);
            return null;
        }
        
        // Check if the tracking is too old (30 minutes max)
        Long lastUse = furnaceLastUse.get(furnaceKey);
        if (lastUse != null && (System.currentTimeMillis() - lastUse) > 30 * 60 * 1000L) {
            // Tracking expired, clean up
            furnaceOwners.remove(furnaceKey);
            furnaceLastUse.remove(furnaceKey);
            return null;
        }
        
        return owner;
    }
    
    /**
     * Detect the target format for an item, supporting all plugin formats.
     * Returns the appropriate target string for nexo:, itemsadder:, customfishing:, customcrops:, or vanilla items.
     */
    private String detectItemTarget(ItemStack item) {
        if (item == null) {
            return "AIR";
        }
        
        // Try to detect Nexo items
        String nexoTarget = detectNexoItem(item);
        if (nexoTarget != null) {
            return nexoTarget;
        }
        
        // Try to detect ItemsAdder items
        String itemsAdderTarget = detectItemsAdderItem(item);
        if (itemsAdderTarget != null) {
            return itemsAdderTarget;
        }
        
        // Try to detect CustomFishing items
        String customFishingTarget = detectCustomFishingItem(item);
        if (customFishingTarget != null) {
            return customFishingTarget;
        }
        
        // Try to detect CustomCrops items
        String customCropsTarget = detectCustomCropsItem(item);
        if (customCropsTarget != null) {
            return customCropsTarget;
        }
        
        // Fallback to vanilla item name
        return item.getType().name();
    }
    
    /**
     * Detect Nexo items by checking for Nexo-specific metadata.
     */
    private String detectNexoItem(ItemStack item) {
        try {
            // Try to use Nexo API if available
            if (plugin.getServer().getPluginManager().isPluginEnabled("Nexo") && item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                // Nexo items typically have specific NBT tags or custom model data
                // This is a basic detection - in a real implementation,
                // you'd use the Nexo API to properly identify items
                return "nexo:item_" + item.getItemMeta().getCustomModelData();
            }
        } catch (Exception e) {
            // Nexo not available or error occurred
        }
        return null;
    }
    
    /**
     * Detect ItemsAdder items by checking for ItemsAdder-specific metadata.
     */
    private String detectItemsAdderItem(ItemStack item) {
        try {
            // Try to use ItemsAdder API if available
            if (plugin.getServer().getPluginManager().isPluginEnabled("ItemsAdder") && item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                // ItemsAdder items typically have specific NBT tags
                // This is a basic detection - in a real implementation,
                // you'd use the ItemsAdder API to properly identify items
                return "itemsadder:item_" + item.getItemMeta().getCustomModelData();
            }
        } catch (Exception e) {
            // ItemsAdder not available or error occurred
        }
        return null;
    }
    
    /**
     * Detect CustomFishing items by checking for CustomFishing-specific metadata.
     */
    private String detectCustomFishingItem(ItemStack item) {
        try {
            // Try to use CustomFishing API if available
            if (plugin.getServer().getPluginManager().isPluginEnabled("CustomFishing") && item.hasItemMeta()) {
                // Check for fish-like items or CustomFishing NBT
                String displayName = item.getItemMeta().getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    String cleanName = displayName.replaceAll(COLOR_CODE_PATTERN, "").toLowerCase();
                    if (cleanName.contains("fish") || cleanName.contains("catch")) {
                        return "customfishing:" + cleanName.replaceAll("[^a-z0-9]", "_");
                    }
                }
            }
        } catch (Exception e) {
            // CustomFishing not available or error occurred
        }
        return null;
    }
    
    /**
     * Detect CustomCrops items by checking for CustomCrops-specific metadata.
     */
    private String detectCustomCropsItem(ItemStack item) {
        try {
            // Try to use CustomCrops API if available
            if (plugin.getServer().getPluginManager().isPluginEnabled("CustomCrops") && item.hasItemMeta()) {
                // Check for crop-like items or CustomCrops NBT
                String displayName = item.getItemMeta().getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    String cleanName = displayName.replaceAll(COLOR_CODE_PATTERN, "").toLowerCase();
                    if (cleanName.contains("crop") || cleanName.contains("seed") || cleanName.contains("harvest")) {
                        return "customcrops:" + cleanName.replaceAll("[^a-z0-9]", "_");
                    }
                }
            }
        } catch (Exception e) {
            // CustomCrops not available or error occurred
        }
        return null;
    }
    
    /**
     * Handle villager trading (TRADE action).
     * Supports profession filtering for villagers.
     * Handles trading by monitoring merchant inventory clicks.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMerchantTradeClick(InventoryClickEvent event) {
        // Only handle merchant inventory (villager trading)
        if (event.getInventory().getType() != InventoryType.MERCHANT) {
            return;
        }
        
        // Only track players
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // Only handle result slot clicks (slot 2 is result)
        if (event.getSlot() != 2) {
            return;
        }
        
        // Make sure the click will result in taking the item
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) {
            return;
        }
        
        // Rate limiting check
        totalEvents.incrementAndGet();
        
        try {
            // Get merchant inventory to find the villager
            org.bukkit.inventory.MerchantInventory merchantInv = (org.bukkit.inventory.MerchantInventory) event.getInventory();
            org.bukkit.inventory.Merchant merchant = merchantInv.getMerchant();
            
            // Create context with trade information
            ConditionContext context = new ConditionContext()
                    .setItem(result)
                    .set(TARGET_KEY, result.getType().name());
            
            // Add villager profession if the merchant is a villager
            if (merchant instanceof Villager) {
                Villager villager = (Villager) merchant;
                context.setEntity(villager)
                       .set("profession", villager.getProfession().name());
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Trade with villager profession: " + villager.getProfession().name() + 
                        TARGET_SUFFIX + result.getType().name() + " by " + player.getName());
                }
            }
            
            // Add trade recipe information if available
            org.bukkit.inventory.MerchantRecipe selectedRecipe = merchantInv.getSelectedRecipe();
            if (selectedRecipe != null) {
                context.set("trade_uses", selectedRecipe.getUses())
                       .set("trade_max_uses", selectedRecipe.getMaxUses());
            }
            
            // Process the action and check if we should cancel
            boolean shouldCancel = actionProcessor.processAction(player, ActionType.TRADE, event, context);
            if (shouldCancel) {
                event.setCancelled(true);
            }
            
            processedEvents.incrementAndGet();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing TRADE action for player " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Add custom item context information including NBT data.
     * Supports MMOItems, CustomCrops, CustomFishing, Nexo, and ItemsAdder.
     */
    private void addCustomItemContext(ItemStack item, ConditionContext context) {
        if (!item.hasItemMeta()) {
            return;
        }
        
        try {
            // Check for MMOItems NBT
            String mmoItemsNBT = detectMMOItemsNBT(item);
            if (mmoItemsNBT != null) {
                context.set("nbt", mmoItemsNBT);
                context.set("mmoitems_type", mmoItemsNBT);
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Detected MMOItems NBT: " + mmoItemsNBT);
                }
            }
            
            // Check for other plugin NBTs/metadata
            addPluginSpecificContext(item, context);
            
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Error detecting custom item context: " + e.getMessage());
            }
        }
    }
    
    /**
     * Detect MMOItems NBT format (MMOITEMS:TYPE:ID).
     */
    private String detectMMOItemsNBT(ItemStack item) {
        try {
            if (plugin.getServer().getPluginManager().isPluginEnabled("MMOItems")) {
                // Try to detect MMOItems using display name and lore patterns
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    // Basic detection - in a real implementation, you'd use MMOItems API
                    String displayName = item.getItemMeta().getDisplayName();
                    if (displayName.contains("§") && item.getItemMeta().hasLore()) {
                        // This is a simplified detection - MMOItems usually have specific NBT
                        // You would need MMOItems API for proper detection
                        return "MMOITEMS:CONSUMABLE:" + displayName.replaceAll(COLOR_CODE_PATTERN, "").replaceAll("[^A-Z0-9]", "_").toUpperCase();
                    }
                }
            }
        } catch (Exception e) {
            // MMOItems not available or error occurred
        }
        return null;
    }
    
    /**
     * Add plugin-specific context for CustomCrops, CustomFishing, Nexo, and ItemsAdder.
     */
    private void addPluginSpecificContext(ItemStack item, ConditionContext context) {
        // Check for CustomCrops items
        if (plugin.getServer().getPluginManager().isPluginEnabled("CustomCrops")) {
            String customCropsId = detectCustomCropsItem(item);
            if (customCropsId != null) {
                context.set("customcrops_id", customCropsId);
                context.set("nbt", customCropsId);
            }
        }
        
        // Check for CustomFishing items
        if (plugin.getServer().getPluginManager().isPluginEnabled("CustomFishing")) {
            String customFishingId = detectCustomFishingItem(item);
            if (customFishingId != null) {
                context.set("customfishing_id", customFishingId);
                context.set("nbt", customFishingId);
            }
        }
        
        // Check for Nexo items
        if (plugin.getServer().getPluginManager().isPluginEnabled("Nexo")) {
            String nexoId = detectNexoItem(item);
            if (nexoId != null) {
                context.set("nexo_id", nexoId);
                context.set("nbt", nexoId);
            }
        }
        
        // Check for ItemsAdder items
        if (plugin.getServer().getPluginManager().isPluginEnabled("ItemsAdder")) {
            String itemsAdderId = detectItemsAdderItem(item);
            if (itemsAdderId != null) {
                context.set("itemsadder_id", itemsAdderId);
                context.set("nbt", itemsAdderId);
            }
        }
    }
    
    /**
     * Add potion-specific context information including potion type and level.
     */
    private void addPotionContext(ItemStack item, ConditionContext context) {
        if (!item.hasItemMeta() || !(item.getItemMeta() instanceof PotionMeta)) {
            return;
        }
        
        try {
            PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
            
            // Get base potion type
            if (potionMeta.getBasePotionType() != null) {
                PotionType baseType = potionMeta.getBasePotionType();
                String potionTypeName = baseType.name();
                
                // Check if it's an extended or upgraded potion
                // Note: In newer versions, this information might be part of the PotionType itself
                boolean isExtended = potionTypeName.contains("LONG");
                boolean isUpgraded = potionTypeName.contains("STRONG");
                
                // Extract the base effect name and level
                String effectName = potionTypeName.replace("LONG_", "").replace("STRONG_", "");
                int level = isUpgraded ? 2 : 1;
                
                // Create potion-type string (e.g., "STRENGTH:2")
                String potionTypeString = effectName + ":" + level;
                context.set("potion-type", potionTypeString);
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Detected potion type: " + potionTypeString + 
                        " (base: " + potionTypeName + ", extended: " + isExtended + ", upgraded: " + isUpgraded + ")");
                }
            }
            
            // Check for custom potion effects (for custom potions)
            if (potionMeta.hasCustomEffects() && !potionMeta.getCustomEffects().isEmpty()) {
                // Use the first custom effect for the potion-type
                PotionEffect firstEffect = potionMeta.getCustomEffects().get(0);
                String effectType = firstEffect.getType().getName().toUpperCase();
                int amplifier = firstEffect.getAmplifier() + 1; // Amplifier is 0-based, but we want 1-based
                
                String customPotionType = effectType + ":" + amplifier;
                context.set("potion-type", customPotionType);
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Detected custom potion effect: " + customPotionType);
                }
            }
            
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Error detecting potion context: " + e.getMessage());
            }
        }
    }
    
}