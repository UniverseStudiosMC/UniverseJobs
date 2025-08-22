package fr.ax_dev.universejobs.listener;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionProcessor;
import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.condition.ConditionContext;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;

/**
 * Listens for enchantment events to award XP for ENCHANT actions.
 * Supports namespace-aware enchantment targeting (e.g., "excellentenchants:tunnel").
 */
public class EnchantEventListener implements Listener {
    
    private final UniverseJobs plugin;
    private final ActionProcessor actionProcessor;
    
    /**
     * Create a new EnchantEventListener.
     * 
     * @param plugin The plugin instance
     * @param actionProcessor The action processor
     */
    public EnchantEventListener(UniverseJobs plugin, ActionProcessor actionProcessor) {
        this.plugin = plugin;
        this.actionProcessor = actionProcessor;
    }
    
    /**
     * Handle enchantment events.
     * Processes each enchantment applied during the event.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("EnchantItemEvent: " + player.getName() + " enchanting " + 
                                  event.getItem().getType() + " with " + event.getEnchantsToAdd().size() + " enchantments");
        }
        
        // Process each enchantment being applied
        for (Enchantment enchantment : event.getEnchantsToAdd().keySet()) {
            int level = event.getEnchantsToAdd().get(enchantment);
            
            // Get enchantment key with namespace support
            String enchantmentKey = getEnchantmentKey(enchantment);
            
            // Create context for this specific enchantment
            ConditionContext context = new ConditionContext()
                    .set("target", enchantmentKey)
                    .set("enchantment", enchantmentKey)
                    .set("enchantment_level", String.valueOf(level))
                    .set("item_type", event.getItem().getType().name())
                    .set("experience_cost", String.valueOf(event.getExpLevelCost()));
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Processing enchantment: " + enchantmentKey + " level " + level);
            }
            
            // Process the enchantment action
            actionProcessor.processAction(player, ActionType.ENCHANT, event, context);
        }
    }
    
    /**
     * Get the enchantment key with proper namespace support.
     * For vanilla enchantments, returns "minecraft:enchantment_name".
     * For modded enchantments, attempts to preserve the original namespace.
     * 
     * @param enchantment The enchantment
     * @return The enchantment key with namespace
     */
    private String getEnchantmentKey(Enchantment enchantment) {
        try {
            // Try to get the NamespacedKey (1.13+ method)
            if (enchantment.getKey() != null) {
                return enchantment.getKey().toString(); // Returns "namespace:key" format
            }
        } catch (NoSuchMethodError | Exception e) {
            // Fallback for older versions or if getKey() is not available
        }
        
        try {
            // Fallback: use getName() and add minecraft namespace
            String name = enchantment.getName();
            if (name != null) {
                // Convert to lowercase and add minecraft namespace if no namespace present
                String key = name.toLowerCase();
                if (!key.contains(":")) {
                    key = "minecraft:" + key;
                }
                return key;
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Failed to get enchantment name: " + e.getMessage());
            }
        }
        
        // Last resort fallback
        return "minecraft:unknown";
    }
}