package fr.ax_dev.universejobs.menu;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.menu.config.MenuItemConfig;
import fr.ax_dev.universejobs.reward.gui.ItemBuilder;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for menu-related operations.
 */
public class MenuUtils {
    
    /**
     * Process PlaceholderAPI placeholders in a string.
     */
    public static String processPlaceholders(Player player, String text) {
        if (text == null) return "";
        
        try {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                text = PlaceholderAPI.setPlaceholders(player, text);
            }
        } catch (Exception e) {
            // Ignore if PlaceholderAPI is not available or fails
        }
        
        return text;
    }
    
    /**
     * Replace custom placeholders in a string.
     */
    public static String replacePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null) return text;
        
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("{") && key.endsWith("}")) {
                result = result.replace(key, entry.getValue());
            } else {
                result = result.replace("{" + key + "}", entry.getValue());
            }
        }
        
        return result;
    }
    
    /**
     * Create an ItemStack from a MenuItemConfig using the existing ItemBuilder.
     */
    public static ItemStack createMenuItem(UniverseJobs plugin, Player player, MenuItemConfig itemConfig) {
        return createMenuItem(plugin, player, itemConfig, null);
    }
    
    /**
     * Create an ItemStack from a MenuItemConfig with custom placeholders using existing ItemBuilder.
     */
    public static ItemStack createMenuItem(UniverseJobs plugin, Player player, MenuItemConfig itemConfig, Map<String, String> customPlaceholders) {
        ItemBuilder builder = ItemBuilder.fromMaterialName(plugin, itemConfig.getMaterial())
                .amount(itemConfig.getAmount());
        
        // Process display name
        String displayName = itemConfig.getDisplayName();
        if (customPlaceholders != null) {
            displayName = replacePlaceholders(displayName, customPlaceholders);
        }
        displayName = processPlaceholders(player, displayName);
        builder.name(displayName);
        
        // Process lore
        List<String> lore = new ArrayList<>();
        for (String loreLine : itemConfig.getLore()) {
            String processedLore = loreLine;
            if (customPlaceholders != null) {
                processedLore = replacePlaceholders(processedLore, customPlaceholders);
            }
            processedLore = processPlaceholders(player, processedLore);
            lore.add(processedLore);
        }
        builder.lore(lore);
        
        // Custom model data
        if (itemConfig.getCustomModelData() > 0) {
            builder.customModelData(itemConfig.getCustomModelData());
        }
        
        // Item flags
        if (itemConfig.isHideAttributes() || itemConfig.isHideEnchants()) {
            builder.hideAttributes();
        }
        
        ItemStack item = builder.build();
        
        // Add enchantments, glow effect, and skull owner after building
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Handle skull owner for player heads
            if (item.getType() == Material.PLAYER_HEAD && !itemConfig.getSkullOwner().isEmpty()) {
                if (meta instanceof SkullMeta) {
                    SkullMeta skullMeta = (SkullMeta) meta;
                    String skullOwner = itemConfig.getSkullOwner();
                    if (customPlaceholders != null) {
                        skullOwner = replacePlaceholders(skullOwner, customPlaceholders);
                    }
                    skullOwner = processPlaceholders(player, skullOwner);
                    try {
                        skullMeta.setOwningPlayer(plugin.getServer().getOfflinePlayer(skullOwner));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to set skull owner: " + skullOwner);
                    }
                }
            }
            
            // Enchantments
            for (Map.Entry<String, Integer> entry : itemConfig.getEnchantments().entrySet()) {
                try {
                    Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(entry.getKey().toLowerCase()));
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, entry.getValue(), true);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid enchantment: " + entry.getKey());
                }
            }
            
            // Glow effect
            if (itemConfig.isGlow() && itemConfig.getEnchantments().isEmpty()) {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            // Hide tooltip (all item information)
            if (itemConfig.isHideToolTip()) {
                meta.setHideTooltip(true);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
}