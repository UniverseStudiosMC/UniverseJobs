package fr.ax_dev.jobsAdventure.listener;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.action.ActionProcessor;
import fr.ax_dev.jobsAdventure.action.ActionType;
import fr.ax_dev.jobsAdventure.condition.ConditionContext;
import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.event.FishingLootSpawnEvent;
import net.momirealms.customfishing.api.event.FishingResultEvent;
import net.momirealms.customfishing.api.mechanic.item.ItemManager;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event listener for CustomFishing plugin compatibility.
 * Uses the official CustomFishing API to handle fishing events with target format "customfishing:fish_id".
 */
public class CustomFishingEventListener implements Listener {
    
    private final JobsAdventure plugin;
    private final ActionProcessor actionProcessor;
    
    // Anti-double action protection for fishing events
    private final Map<UUID, Long> lastFishingTime = new HashMap<>();
    private static final long FISHING_COOLDOWN_MS = 100; // 100ms cooldown
    
    public CustomFishingEventListener(JobsAdventure plugin, ActionProcessor actionProcessor) {
        this.plugin = plugin;
        this.actionProcessor = actionProcessor;

        // Always log creation (not just in debug mode)
        plugin.getLogger().info("CustomFishingEventListener initialized successfully");
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CustomFishing integration enabled with debug mode");
        }
    }
    /**
     * Handle CustomFishing loot spawn events.
     * Uses the same pattern as Nexo and ItemsAdder with target format "customfishing:fish_id".
     * Includes anti-double action protection.
     */   

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFishingLootSpawn(FishingLootSpawnEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CustomFishing trigger onFishingLootSpawn");
        }
        // Anti-double action protection
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastFishingTime.get(playerUUID);
        
        if (lastTime != null && (currentTime - lastTime) < FISHING_COOLDOWN_MS) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("CustomFishing loot spawn blocked (double-action protection): " + player.getName() + " cooldown remaining: " + (FISHING_COOLDOWN_MS - (currentTime - lastTime)) + "ms");
            }
            return;
        }
        
        // Update last fishing time
        lastFishingTime.put(playerUUID, currentTime);
        
        // Get fish information from the spawned entity
        if (!(event.getEntity() instanceof Item item)) {
            return;
        }
        
        ItemStack itemStack = item.getItemStack();
        
        // Debug logging to understand the item structure
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CustomFishing ItemStack debug:");
            plugin.getLogger().info("  Material: " + itemStack.getType());
            if (itemStack.hasItemMeta()) {
                if (itemStack.getItemMeta().hasDisplayName()) {
                    plugin.getLogger().info("  Display name: " + itemStack.getItemMeta().getDisplayName());
                }
                if (itemStack.getItemMeta().hasCustomModelData()) {
                    plugin.getLogger().info("  Custom model data: " + itemStack.getItemMeta().getCustomModelData());
                }
                if (itemStack.getItemMeta().hasLore()) {
                    plugin.getLogger().info("  Lore: " + itemStack.getItemMeta().getLore());
                }
            }
        }
        
        String fishId = getFishIdFromItemStack(itemStack);
        
        if (fishId == null || fishId.isEmpty()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("CustomFishing: Unable to determine fish ID from ItemStack: " + itemStack.getType());
            }
            return;
        }
        
        // Create context with CustomFishing information following the same pattern as Nexo/ItemsAdder
        ConditionContext context = new ConditionContext()
                .set("target", "customfishing:" + fishId)
                .set("customfishing_fish_id", fishId)
                .set("itemstack", itemStack);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CustomFishing caught: customfishing:" + fishId + " by " + player.getName());
            plugin.getLogger().info("Processing FISH action with target: customfishing:" + fishId + " by " + player.getName());
        }
        
        // Process as fish action
        actionProcessor.processAction(player, ActionType.FISH, event, context);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("CustomFishing loot spawn processed: " + fishId + " by " + player.getName() + " at " + player.getLocation());
        }
    }
    
    /**
     * Extract fish ID from ItemStack.
     * This method attempts to determine the fish ID from various sources.
     * Priority order:
     * 1. Try to reverse-lookup using CustomFishing API
     * 2. Custom model data (fallback)
     * 3. Material name (last resort)
     */
    private String getFishIdFromItemStack(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        
        // First attempt: Try to reverse-lookup using CustomFishing API
        try {
            BukkitCustomFishingPlugin customFishingApi = BukkitCustomFishingPlugin.getInstance();
            if (customFishingApi != null) {
                ItemManager itemManager = customFishingApi.getItemManager();
                
                // Try to identify the item through the CustomFishing ItemManager
                // This is the most reliable method as it uses the internal API
                String possibleId = identifyCustomFishingItem(itemManager, itemStack);
                if (possibleId != null && !possibleId.isEmpty()) {
                    return possibleId;
                }
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Failed to use CustomFishing API for item identification: " + e.getMessage());
            }
        }
        
        // Fallback 1: Try to get fish ID from custom model data
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasCustomModelData()) {
            int customModelData = itemStack.getItemMeta().getCustomModelData();
            return "fish_" + customModelData;
        }
        
        // Fallback 2: Material name
        String materialName = itemStack.getType().name().toLowerCase();
        if (materialName.contains("fish") || materialName.contains("cod") || materialName.contains("salmon") || materialName.contains("tropical")) {
            return materialName;
        }
        
        // Last resort: try to extract from display name
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
            String displayName = itemStack.getItemMeta().getDisplayName();
            // Remove color codes and special characters, convert to lowercase
            String cleanName = displayName.replaceAll("§[0-9a-fk-or]", "").replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
            return cleanName;
        }
        
        // Default fallback
        return materialName;
    }
    
    /**
     * Attempt to identify a CustomFishing item using the ItemManager.
     * This method tries different approaches to reverse-engineer the item ID.
     */
    private String identifyCustomFishingItem(ItemManager itemManager, ItemStack itemStack) {
        // Unfortunately, the CustomFishing API doesn't provide a direct reverse-lookup method
        // We need to use alternative approaches
        
        // Method 1: Check for CustomFishing NBT data or persistent data
        if (itemStack.hasItemMeta()) {
            // Look for any CustomFishing-specific metadata
            // This might include NBT tags, persistent data, or custom lore patterns
            
            // Check display name for patterns like fish names
            if (itemStack.getItemMeta().hasDisplayName()) {
                String displayName = itemStack.getItemMeta().getDisplayName();
                // Remove color codes and extract potential fish name
                String cleanName = displayName.replaceAll("§[0-9a-fk-or]", "").trim();
                
                // Convert common fish names to potential IDs
                String potentialId = convertDisplayNameToId(cleanName);
                if (potentialId != null) {
                    return potentialId;
                }
            }
            
            // Check lore for fish information
            if (itemStack.getItemMeta().hasLore()) {
                for (String loreLine : itemStack.getItemMeta().getLore()) {
                    String cleanLore = loreLine.replaceAll("§[0-9a-fk-or]", "").trim();
                    // Look for patterns that might indicate fish type
                    if (cleanLore.toLowerCase().contains("fish") || cleanLore.toLowerCase().contains("catch")) {
                        String potentialId = extractIdFromLore(cleanLore);
                        if (potentialId != null) {
                            return potentialId;
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Convert display name to potential CustomFishing ID.
     */
    private String convertDisplayNameToId(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return null;
        }
        
        String lowerName = displayName.toLowerCase();
        
        // Common fish name mappings
        if (lowerName.contains("cod")) return "cod";
        if (lowerName.contains("salmon")) return "salmon";
        if (lowerName.contains("tropical")) return "tropical_fish";
        if (lowerName.contains("pufferfish")) return "pufferfish";
        if (lowerName.contains("bass")) return "bass";
        if (lowerName.contains("carp")) return "carp";
        if (lowerName.contains("tuna")) return "tuna";
        if (lowerName.contains("mackerel")) return "mackerel";
        if (lowerName.contains("sardine")) return "sardine";
        
        // Convert spaces to underscores and remove special characters
        return lowerName.replaceAll("[^a-z0-9]", "_").replaceAll("_+", "_").trim();
    }
    
    /**
     * Extract potential ID from lore text.
     */
    private String extractIdFromLore(String lore) {
        if (lore == null || lore.isEmpty()) {
            return null;
        }
        
        String lowerLore = lore.toLowerCase();
        
        // Look for specific patterns in lore that might indicate fish type
        if (lowerLore.contains("rarity:") || lowerLore.contains("type:")) {
            // Extract the value after the colon
            String[] parts = lowerLore.split(":");
            if (parts.length > 1) {
                return parts[1].trim().replaceAll("[^a-z0-9]", "_");
            }
        }
        
        return null;
    }
    
    /**
     * Clean up old fishing time entries to prevent memory leaks.
     * Called periodically to remove entries older than 10 minutes.
     */
    public void cleanupOldFishingTimes() {
        long currentTime = System.currentTimeMillis();
        long cleanupThreshold = 10 * 60 * 1000; // 10 minutes
        
        lastFishingTime.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > cleanupThreshold);
    }
}