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

import java.net.URL;
import java.util.Base64;
import java.util.UUID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;

/**
 * Utility class for menu-related operations.
 */
public class MenuUtils {

    // Cache for processed strings to avoid repeated operations
    private static final Map<String, String> PLACEHOLDER_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<Component>> LORE_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    
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
     * Create an ItemStack async-safe (without player-specific operations).
     * This can be called from async threads safely.
     */
    public static ItemStack createMenuItemAsync(UniverseJobs plugin, Player player, MenuItemConfig itemConfig, Map<String, String> customPlaceholders) {
        // Create base item without player-specific operations
        ItemBuilder builder = ItemBuilder.fromMaterialName(plugin, itemConfig.getMaterial());
        if (builder == null) {
            builder = new ItemBuilder(plugin, Material.STONE);
        }
        builder.amount(itemConfig.getAmount());

        // Process display name with pre-calculated placeholders
        String displayName = itemConfig.getDisplayName();
        if (customPlaceholders != null) {
            displayName = replacePlaceholders(displayName, customPlaceholders);
        }
        builder.name(displayName);

        // Process lore with pre-calculated placeholders
        List<String> lore = new ArrayList<>();
        for (String loreLine : itemConfig.getLore()) {
            if (loreLine.contains("{job_description}") && customPlaceholders != null && customPlaceholders.containsKey("job_description_lines")) {
                String[] descriptionLines = customPlaceholders.get("job_description_lines").split("\n");
                String baseFormat = loreLine.replace("{job_description}", "");

                for (String descLine : descriptionLines) {
                    if (descLine.trim().isEmpty()) {
                        lore.add("");
                    } else {
                        String processedLine = baseFormat + descLine;
                        if (customPlaceholders != null) {
                            processedLine = replacePlaceholders(processedLine, customPlaceholders);
                        }
                        lore.add(processedLine);
                    }
                }
            } else {
                String processedLore = loreLine;
                if (customPlaceholders != null) {
                    processedLore = replacePlaceholders(processedLore, customPlaceholders);
                }
                lore.add(processedLore);
            }
        }
        builder.lore(lore);

        // Custom model data
        if (itemConfig.getModelData() != null && !itemConfig.getModelData().isEmpty()) {
            builder.modelData(itemConfig.getModelData());
        }

        // Item flags
        if (itemConfig.isHideAttributes() || itemConfig.isHideEnchants()) {
            builder.hideAttributes();
        }

        return builder.build();
    }
    
    /**
     * Create an ItemStack from a MenuItemConfig with custom placeholders using existing ItemBuilder.
     * Ultra-optimized version with caching and minimal allocations.
     */
    public static ItemStack createMenuItem(UniverseJobs plugin, Player player, MenuItemConfig itemConfig, Map<String, String> customPlaceholders) {
        if (itemConfig == null || !itemConfig.isEnabled()) {
            return null;
        }

        // Generate cache key for this item
        String cacheKey = generateItemCacheKey(player.getUniqueId(), itemConfig, customPlaceholders);

        // Try to get from component cache first
        return plugin.getAccessor().getMenuManager().getComponentCache().getItem(cacheKey, () -> {
            return createMenuItemUncached(plugin, player, itemConfig, customPlaceholders);
        });
    }

    /**
     * Create menu item without caching - internal optimized version.
     */
    private static ItemStack createMenuItemUncached(UniverseJobs plugin, Player player, MenuItemConfig itemConfig, Map<String, String> customPlaceholders) {
        // Pre-allocate builder
        ItemBuilder builder = ItemBuilder.fromMaterialName(plugin, itemConfig.getMaterial());
        if (builder == null) {
            return null; // Fail fast instead of fallback
        }

        // Batch apply basic properties
        builder.amount(itemConfig.getAmount());

        // Process display name with original working method
        String displayName = itemConfig.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            if (customPlaceholders != null) {
                displayName = replacePlaceholders(displayName, customPlaceholders);
            }
            displayName = processPlaceholders(player, displayName);
            builder.name(displayName);
        }

        // Process lore with multi-line description support
        List<String> originalLore = itemConfig.getLore();
        if (originalLore != null && !originalLore.isEmpty()) {
            List<String> processedLore = new ArrayList<>();
            for (String loreLine : originalLore) {
                processedLore.addAll(processLoreLineWithMultiLine(loreLine, customPlaceholders, player));
            }
            builder.lore(processedLore);
        }

        // Batch apply visual properties
        if (itemConfig.getModelData() != null && !itemConfig.getModelData().isEmpty()) {
            builder.modelData(itemConfig.getModelData());
        }

        if (itemConfig.isHideAttributes() || itemConfig.isHideEnchants()) {
            builder.hideAttributes();
        }

        // Build base item
        ItemStack item = builder.build();

        // Apply meta modifications in single operation
        applyItemMetaOptimized(item, itemConfig, customPlaceholders, player, plugin);

        return item;
    }

    /**
     * Generate optimized cache key for items.
     */
    private static String generateItemCacheKey(UUID playerId, MenuItemConfig itemConfig, Map<String, String> customPlaceholders) {
        StringBuilder key = new StringBuilder(128); // Pre-allocate
        key.append(playerId.toString()).append(':')
           .append(itemConfig.getMaterial()).append(':')
           .append(itemConfig.getDisplayName() != null ? itemConfig.getDisplayName().hashCode() : 0).append(':')
           .append(itemConfig.getLore() != null ? itemConfig.getLore().hashCode() : 0);

        if (customPlaceholders != null && !customPlaceholders.isEmpty()) {
            // Include job_id from placeholders for unique cache keys
            String jobId = customPlaceholders.get("{job_id}");
            if (jobId != null) {
                key.append(':').append(jobId);
            }
            key.append(':').append(customPlaceholders.hashCode());
        }

        return key.toString();
    }

    /**
     * Optimized placeholder processing with caching and minimal string operations.
     */
    private static String processPlaceholdersOptimized(String text, Map<String, String> customPlaceholders, Player player) {
        if (text == null || text.isEmpty()) return text;

        // Check cache first
        String cacheKey = text + ":" + (customPlaceholders != null ? customPlaceholders.hashCode() : 0) + ":" + player.getUniqueId();
        String cached = PLACEHOLDER_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String result = text;

        // Apply custom placeholders first (most efficient)
        if (customPlaceholders != null && !customPlaceholders.isEmpty()) {
            for (Map.Entry<String, String> entry : customPlaceholders.entrySet()) {
                String key = entry.getKey();
                if (result.contains(key)) { // Pre-check before replace
                    result = result.replace(key, entry.getValue());
                }
            }
        }

        // Only process PlaceholderAPI if needed
        if (result.contains("%")) {
            result = processPlaceholders(player, result);
        }

        // Cache result if not too large
        if (PLACEHOLDER_CACHE.size() < MAX_CACHE_SIZE) {
            PLACEHOLDER_CACHE.put(cacheKey, result);
        }

        return result;
    }

    /**
     * Optimized lore processing with bulk operations and pre-allocation.
     */
    private static List<String> processLoreOptimized(List<String> lore, Map<String, String> customPlaceholders, Player player) {
        if (lore == null || lore.isEmpty()) return new ArrayList<>();

        // Pre-allocate with expected size (accounting for multi-line expansion)
        List<String> processedLore = new ArrayList<>(lore.size() * 2);

        for (String loreLine : lore) {
            if (loreLine.contains("{job_description}") && customPlaceholders != null && customPlaceholders.containsKey("job_description_lines")) {
                // Handle multi-line descriptions efficiently
                processMultiLineDescription(loreLine, customPlaceholders, player, processedLore);
            } else {
                // Standard single-line processing
                String processed = processPlaceholdersOptimized(loreLine, customPlaceholders, player);
                processedLore.add(processed);
            }
        }

        return processedLore;
    }

    /**
     * Process multi-line descriptions efficiently.
     */
    private static void processMultiLineDescription(String loreLine, Map<String, String> customPlaceholders, Player player, List<String> output) {
        String[] descriptionLines = customPlaceholders.get("job_description_lines").split("\n");
        String baseFormat = loreLine.replace("{job_description}", "");

        for (String descLine : descriptionLines) {
            if (descLine.trim().isEmpty()) {
                output.add("");
            } else {
                String processedLine = baseFormat + descLine;
                if (customPlaceholders != null) {
                    processedLine = replacePlaceholders(processedLine, customPlaceholders);
                }
                processedLine = processPlaceholders(player, processedLine);
                output.add(processedLine);
            }
        }
    }

    /**
     * Apply item meta modifications in optimized batch operation.
     */
    private static void applyItemMetaOptimized(ItemStack item, MenuItemConfig itemConfig, Map<String, String> customPlaceholders, Player player, UniverseJobs plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        boolean metaModified = false;

        // Handle skull owner for player heads
        if (item.getType() == Material.PLAYER_HEAD && meta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) meta;

            if (!itemConfig.getPlayerHead().isEmpty()) {
                String playerHead = itemConfig.getPlayerHead();
                if (customPlaceholders != null) {
                    playerHead = replacePlaceholders(playerHead, customPlaceholders);
                }
                playerHead = processPlaceholders(player, playerHead);

                String playerUuidStr = customPlaceholders != null ? customPlaceholders.get("{player_uuid}") : null;

                try {
                    if (playerHead.length() > 20 && isValidBase64(playerHead)) {
                        setSkullTexture(skullMeta, playerHead);
                        metaModified = true;
                    } else {
                        UUID targetUuid = null;
                        if (playerUuidStr != null && !playerUuidStr.isEmpty()) {
                            try {
                                targetUuid = UUID.fromString(playerUuidStr);
                            } catch (IllegalArgumentException ignored) {}
                        }

                        if (targetUuid != null) {
                            // Never block region threads waiting for skin/profile lookups.
                            // Only use an already-cached profile; otherwise fall back to offline player.
                            PlayerProfile cachedProfile = fr.ax_dev.universejobs.utils.PlayerTextureCache
                                .getCachedProfile(targetUuid);

                            if (cachedProfile != null && cachedProfile.getTextures().getSkin() != null) {
                                skullMeta.setOwnerProfile(cachedProfile);
                                metaModified = true;
                            } else {
                                org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(targetUuid);
                                skullMeta.setOwningPlayer(offlinePlayer);
                                metaModified = true;
                            }
                        } else {
                            org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerHead);
                            if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
                                skullMeta.setOwningPlayer(offlinePlayer);
                                metaModified = true;
                            } else {
                                // Non-blocking fallback: only apply a texture if already cached.
                                PlayerProfile profile = fr.ax_dev.universejobs.utils.PlayerTextureCache
                                    .getCachedProfileByName(playerHead);
                                if (profile != null && profile.getTextures().getSkin() != null) {
                                    skullMeta.setOwnerProfile(profile);
                                } else {
                                    skullMeta.setOwningPlayer(offlinePlayer);
                                }
                                metaModified = true;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Silently fail for performance
                }
            } else if (!itemConfig.getSkullOwner().isEmpty()) {
                String skullOwner = itemConfig.getSkullOwner();
                if (customPlaceholders != null) {
                    skullOwner = replacePlaceholders(skullOwner, customPlaceholders);
                }
                skullOwner = processPlaceholders(player, skullOwner);
                try {
                    skullMeta.setOwningPlayer(plugin.getServer().getOfflinePlayer(skullOwner));
                    metaModified = true;
                } catch (Exception e) {
                    // Silently fail for performance
                }
            }
        }

        // Batch apply enchantments
        Map<String, Integer> enchantments = itemConfig.getEnchantments();
        if (enchantments != null && !enchantments.isEmpty()) {
            for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                try {
                    Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(entry.getKey().toLowerCase()));
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, entry.getValue(), true);
                        metaModified = true;
                    }
                } catch (Exception e) {
                    // Silently fail for performance
                }
            }
        }

        // Glow effect
        if (itemConfig.isGlow() && (enchantments == null || enchantments.isEmpty())) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            metaModified = true;
        }

        // Hide tooltip
        if (itemConfig.isHideToolTip()) {
            meta.setHideTooltip(true);
            metaModified = true;
        }

        // Only apply meta if it was actually modified
        if (metaModified) {
            item.setItemMeta(meta);
        }
    }

    /**
     * Clear caches to prevent memory leaks.
     */
    public static void clearCaches() {
        PLACEHOLDER_CACHE.clear();
        LORE_CACHE.clear();
    }

    /**
     * Process lore line with multi-line support (legacy method for compatibility).
     */
    public static List<Component> processLoreLineWithMultiLine(String loreLine) {
        List<Component> result = new ArrayList<>();
        result.add(parseMessage(loreLine));
        return result;
    }

    /**
     * Parse message to Component (placeholder for MessageUtils).
     */
    private static Component parseMessage(String message) {
        return fr.ax_dev.universejobs.utils.MessageUtils.parseMessage(message);
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

                UUID textureUuid = UUID.nameUUIDFromBytes(textureUrl.getBytes());
                PlayerProfile profile = Bukkit.createPlayerProfile(textureUuid);
                profile.getTextures().setSkin(new URL(textureUrl));
                skullMeta.setOwnerProfile(profile);
            }
        } catch (Exception e) {
            try {
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.nameUUIDFromBytes(textureValue.getBytes()));
                String textureUrl = "http://textures.minecraft.net/texture/" + textureValue;
                profile.getTextures().setSkin(new URL(textureUrl));
                skullMeta.setOwnerProfile(profile);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to set skull texture", ex);
            }
        }
    }
}
