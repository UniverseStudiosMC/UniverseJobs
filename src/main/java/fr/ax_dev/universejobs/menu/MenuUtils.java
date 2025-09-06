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
import org.bukkit.profile.PlayerProfile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

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
     * Process a lore line with support for multi-line placeholders.
     */
    private static List<String> processLoreLineWithMultiLine(String loreLine, Map<String, String> customPlaceholders, Player player) {
        List<String> result = new ArrayList<>();
        
        // Check if this line contains {job_description} placeholder
        if (loreLine.contains("{job_description}") && customPlaceholders != null && customPlaceholders.containsKey("job_description_lines")) {
            // Use the multi-line description
            String[] descriptionLines = customPlaceholders.get("job_description_lines").split("\n");
            String baseFormat = loreLine.replace("{job_description}", "");
            
            for (String descLine : descriptionLines) {
                if (descLine.trim().isEmpty()) {
                    result.add("");
                } else {
                    String processedLine = baseFormat + descLine;
                    if (customPlaceholders != null) {
                        processedLine = replacePlaceholders(processedLine, customPlaceholders);
                    }
                    processedLine = processPlaceholders(player, processedLine);
                    result.add(processedLine);
                }
            }
        } else {
            // Standard processing
            String processedLore = loreLine;
            if (customPlaceholders != null) {
                processedLore = replacePlaceholders(processedLore, customPlaceholders);
            }
            processedLore = processPlaceholders(player, processedLore);
            result.add(processedLore);
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
        
        // Process lore with multi-line placeholder support
        List<String> lore = new ArrayList<>();
        for (String loreLine : itemConfig.getLore()) {
            List<String> processedLines = processLoreLineWithMultiLine(loreLine, customPlaceholders, player);
            lore.addAll(processedLines);
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
            if (item.getType() == Material.PLAYER_HEAD) {
                if (meta instanceof SkullMeta) {
                    SkullMeta skullMeta = (SkullMeta) meta;
                    
                    // Priority to player-head over skull-owner
                    if (!itemConfig.getPlayerHead().isEmpty()) {
                        String playerHead = itemConfig.getPlayerHead();
                        if (customPlaceholders != null) {
                            playerHead = replacePlaceholders(playerHead, customPlaceholders);
                        }
                        playerHead = processPlaceholders(player, playerHead);
                        
                        try {
                            // Check if it's a texture value (base64)
                            if (playerHead.length() > 20 && isValidBase64(playerHead)) {
                                setSkullTexture(skullMeta, playerHead);
                            } else {
                                // It's a player name
                                skullMeta.setOwningPlayer(plugin.getServer().getOfflinePlayer(playerHead));
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to set player head: " + playerHead);
                        }
                    } else if (!itemConfig.getSkullOwner().isEmpty()) {
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
    
    private static boolean isValidBase64(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    private static void setSkullTexture(SkullMeta skullMeta, String textureValue) {
        try {
            String decodedTexture = new String(Base64.getDecoder().decode(textureValue));
            if (decodedTexture.contains("\"url\":\"")) {
                String textureUrl = decodedTexture.split("\"url\":\"")[1].split("\"")[0];
                
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "");
                profile.getTextures().setSkin(new URL(textureUrl));
                skullMeta.setOwnerProfile(profile);
            }
        } catch (Exception e) {
            // Fallback: set texture directly without decoding
            try {
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "");
                // Create URL from texture value - assuming it's already a valid texture URL
                String textureUrl = "http://textures.minecraft.net/texture/" + textureValue;
                profile.getTextures().setSkin(new URL(textureUrl));
                skullMeta.setOwnerProfile(profile);
            } catch (Exception ex) {
                // If all fails, log the error
                throw new RuntimeException("Failed to set skull texture", ex);
            }
        }
    }
}