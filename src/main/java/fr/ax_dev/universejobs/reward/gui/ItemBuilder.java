package fr.ax_dev.universejobs.reward.gui;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.reward.Reward;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

/**
 * Utility class for building ItemStacks with support for custom integrations.
 * Handles Nexo, ItemsAdder, and custom model data integration.
 */
public class ItemBuilder {

    private ItemStack item;
    private ItemMeta meta;
    private final UniverseJobs plugin;
    
    /**
     * Create a new ItemBuilder.
     * 
     * @param plugin The plugin instance
     * @param material The base material
     */
    public ItemBuilder(UniverseJobs plugin, Material material) {
        this.plugin = plugin;
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }
    
    /**
     * Create a new ItemBuilder from an existing ItemStack.
     * 
     * @param plugin The plugin instance
     * @param item The base ItemStack
     */
    public ItemBuilder(UniverseJobs plugin, ItemStack item) {
        this.plugin = plugin;
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }
    
    /**
     * Create an ItemBuilder from a material name.
     * 
     * @param plugin The plugin instance
     * @param materialName The material name
     * @return New ItemBuilder instance
     */
    public static ItemBuilder of(UniverseJobs plugin, String materialName) {
        Material material = fr.ax_dev.universejobs.utils.EnumUtils.parseMaterial(materialName, null);
        if (material == null) {
            plugin.getLogger().severe("Unknown material: " + materialName);
            return null;
        }
        return new ItemBuilder(plugin, material);
    }
    
    /**
     * Create an ItemBuilder from a RewardItem.
     * Handles all custom integrations automatically.
     * 
     * @param plugin The plugin instance
     * @param rewardItem The reward item configuration
     * @return New ItemBuilder instance
     */
    public static ItemBuilder fromRewardItem(UniverseJobs plugin, Reward.RewardItem rewardItem) {
        // Check for Nexo items first
        if (rewardItem.isNexoItem()) {
            ItemStack nexoItem = createNexoItem(rewardItem.getNexoId());
            if (nexoItem != null) {
                ItemBuilder builder = new ItemBuilder(plugin, nexoItem);
                return builder.amount(rewardItem.getAmount());
            }
        }
        
        // Check for ItemsAdder items
        if (rewardItem.isItemsAdderItem()) {
            ItemStack iaItem = createItemsAdderItem(rewardItem.getItemsAdderId());
            if (iaItem != null) {
                ItemBuilder builder = new ItemBuilder(plugin, iaItem);
                return builder.amount(rewardItem.getAmount());
            }
        }
        
        // Fall back to regular material
        ItemBuilder builder = ItemBuilder.of(plugin, rewardItem.getMaterial())
                .amount(rewardItem.getAmount());
        
        // Apply custom model data if specified
        if (rewardItem.hasCustomModelData()) {
            builder.customModelData(rewardItem.getCustomModelData());
        }
        
        // Apply display name and lore if specified
        if (rewardItem.getDisplayName() != null) {
            builder.name(rewardItem.getDisplayName());
        }
        
        if (!rewardItem.getLore().isEmpty()) {
            builder.lore(rewardItem.getLore());
        }
        
        return builder;
    }
    
    /**
     * Set the display name of the item.
     * 
     * @param name The display name
     * @return This ItemBuilder instance
     */
    public ItemBuilder name(String name) {
        if (meta != null && name != null) {
            String finalName = name.startsWith("<!italic>") ? name : "<!italic><white>" + name;
            meta.setDisplayName(MessageUtils.colorize(finalName));
        }
        return this;
    }
    
    /**
     * Set the lore of the item.
     * 
     * @param lore The lore lines
     * @return This ItemBuilder instance
     */
    public ItemBuilder lore(List<String> lore) {
        if (meta != null && lore != null) {
            List<String> colorizedLore = new ArrayList<>();
            for (String line : lore) {
                String finalLine = line.startsWith("<!italic>") ? line : "<!italic><white>" + line;
                colorizedLore.add(MessageUtils.colorize(finalLine));
            }
            meta.setLore(colorizedLore);
        }
        return this;
    }
    
    /**
     * Add a line to the lore.
     * 
     * @param line The lore line to add
     * @return This ItemBuilder instance
     */
    public ItemBuilder addLore(String line) {
        if (meta != null && line != null) {
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            String finalLine = line.startsWith("<!italic>") ? line : "<!italic><white>" + line;
            lore.add(MessageUtils.colorize(finalLine));
            meta.setLore(lore);
        }
        return this;
    }
    
    /**
     * Set the amount of the item.
     * 
     * @param amount The amount
     * @return This ItemBuilder instance
     */
    public ItemBuilder amount(int amount) {
        if (amount > 0) {
            item.setAmount(Math.min(amount, item.getMaxStackSize()));
        }
        return this;
    }
    
    /**
     * Set the custom model data.
     * 
     * @param modelData The custom model data
     * @return This ItemBuilder instance
     */
    public ItemBuilder customModelData(int modelData) {
        if (meta != null && modelData > 0) {
            meta.setCustomModelData(modelData);
        }
        return this;
    }
    
    /**
     * Add item flags to hide attributes.
     * 
     * @param flags The item flags to add
     * @return This ItemBuilder instance
     */
    public ItemBuilder flags(ItemFlag... flags) {
        if (meta != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }
    
    /**
     * Hide all attributes.
     * 
     * @return This ItemBuilder instance
     */
    public ItemBuilder hideAttributes() {
        return flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, 
                    ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS);
    }
    
    /**
     * Set the item as unbreakable.
     * 
     * @param unbreakable Whether the item is unbreakable
     * @return This ItemBuilder instance
     */
    public ItemBuilder unbreakable(boolean unbreakable) {
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
        }
        return this;
    }
    
    /**
     * Set the player head texture or owner.
     * 
     * @param playerHead The player name or texture value
     * @return This ItemBuilder instance
     */
    public ItemBuilder playerHead(String playerHead) {
        if (item.getType() == Material.PLAYER_HEAD && meta instanceof SkullMeta && playerHead != null) {
            SkullMeta skullMeta = (SkullMeta) meta;
            try {
                // Check if it's a texture value (base64)
                if (playerHead.length() > 20 && isValidBase64(playerHead)) {
                    setSkullTexture(skullMeta, playerHead);
                } else {
                    // It's a player name
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerHead));
                }
            } catch (Exception e) {
                // Fallback to player name
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerHead));
            }
        }
        return this;
    }
    
    /**
     * Build the final ItemStack.
     * 
     * @return The built ItemStack
     */
    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item.clone();
    }
    
    /**
     * Create a Nexo item if the plugin is available.
     * 
     * @param nexoId The Nexo item ID
     * @return The Nexo ItemStack or null if not available
     */
    private static ItemStack createNexoItem(String nexoId) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
            return null;
        }
        
        try {
            com.nexomc.nexo.items.ItemBuilder itemBuilder = com.nexomc.nexo.api.NexoItems.itemFromId(nexoId);
            if (itemBuilder != null) {
                return itemBuilder.build();
            }
        } catch (NoClassDefFoundError | Exception e) {
            // Nexo not available or API changed
        }
        
        return null;
    }
    
    /**
     * Create an ItemsAdder item if the plugin is available.
     * 
     * @param itemsAdderId The ItemsAdder item ID
     * @return The ItemsAdder ItemStack or null if not available
     */
    private static ItemStack createItemsAdderItem(String itemsAdderId) {
        if (!Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            return null;
        }
        
        try {
            // Use reflection to avoid NoClassDefFoundError when ItemsAdder is not present
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            java.lang.reflect.Method getInstanceMethod = customStackClass.getMethod("getInstance", String.class);
            Object customStack = getInstanceMethod.invoke(null, itemsAdderId);
            
            if (customStack != null) {
                java.lang.reflect.Method getItemStackMethod = customStack.getClass().getMethod("getItemStack");
                return (ItemStack) getItemStackMethod.invoke(customStack);
            }
        } catch (Exception e) {
            // ItemsAdder not available or API changed
        }
        
        return null;
    }
    
    private boolean isValidBase64(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    private void setSkullTexture(SkullMeta skullMeta, String textureValue) {
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
    
    /**
     * Create a navigation item for GUI navigation.
     * 
     * @param plugin The plugin instance
     * @param material The material for the nav item
     * @param name The display name
     * @param lore The lore lines
     * @return The navigation ItemStack
     */
    public static ItemStack createNavigationItem(UniverseJobs plugin, Material material, String name, String... lore) {
        ItemBuilder builder = new ItemBuilder(plugin, material)
                .name(name)
                .hideAttributes();
        
        for (String line : lore) {
            builder.addLore(line);
        }
        
        return builder.build();
    }
    
    /**
     * Create a filler item for empty GUI slots.
     * 
     * @param plugin The plugin instance
     * @return The filler ItemStack
     */
    public static ItemStack createFillerItem(UniverseJobs plugin) {
        return new ItemBuilder(plugin, Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .hideAttributes()
                .build();
    }
    
    /**
     * Create an ItemBuilder from a material name that supports custom integrations.
     * Handles Nexo, ItemsAdder and regular materials.
     * 
     * @param plugin The plugin instance
     * @param materialName The material name (can be nexo:item_id, itemsadder:item_id, or MATERIAL_NAME)
     * @return New ItemBuilder instance
     */
    public static ItemBuilder fromMaterialName(UniverseJobs plugin, String materialName) {
        // Check for Nexo items (nexo:item_id format)
        if (materialName.startsWith("nexo:")) {
            String nexoId = materialName.substring(5); // Remove "nexo:" prefix
            ItemStack nexoItem = createNexoItem(nexoId);
            if (nexoItem != null) {
                return new ItemBuilder(plugin, nexoItem);
            } else {
                plugin.getLogger().severe("Nexo item not found: " + nexoId);
                return null;
            }
        }
        
        // Check for ItemsAdder items (itemsadder:item_id format)
        if (materialName.startsWith("itemsadder:")) {
            String iaId = materialName.substring(11); // Remove "itemsadder:" prefix
            ItemStack iaItem = createItemsAdderItem(iaId);
            if (iaItem != null) {
                return new ItemBuilder(plugin, iaItem);
            } else {
                plugin.getLogger().severe("ItemsAdder item not found: " + iaId);
                return null;
            }
        }
        
        // Regular Bukkit material
        try {
            Material material = fr.ax_dev.universejobs.utils.EnumUtils.parseMaterial(materialName, null);
            if (material == null) {
                return null;
            }
            return new ItemBuilder(plugin, material);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe("Unknown material: " + materialName);
            return null;
        }
    }
}