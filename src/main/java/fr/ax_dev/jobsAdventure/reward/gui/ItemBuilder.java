package fr.ax_dev.jobsAdventure.reward.gui;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.reward.Reward;
import fr.ax_dev.jobsAdventure.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building ItemStacks with support for custom integrations.
 * Handles Nexo, ItemsAdder, and custom model data integration.
 */
public class ItemBuilder {
    
    private final JobsAdventure plugin;
    private ItemStack item;
    private ItemMeta meta;
    
    /**
     * Create a new ItemBuilder.
     * 
     * @param plugin The plugin instance
     * @param material The base material
     */
    public ItemBuilder(JobsAdventure plugin, Material material) {
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
    public ItemBuilder(JobsAdventure plugin, ItemStack item) {
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
    public static ItemBuilder of(JobsAdventure plugin, String materialName) {
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown material: " + materialName + ", using STONE");
            material = Material.STONE;
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
    public static ItemBuilder fromRewardItem(JobsAdventure plugin, Reward.RewardItem rewardItem) {
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
            meta.setDisplayName(MessageUtils.colorize(name));
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
                colorizedLore.add(MessageUtils.colorize(line));
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
            lore.add(MessageUtils.colorize(line));
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
            // Use reflection to access Nexo API
            Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
            Object itemBuilder = nexoItemsClass.getMethod("itemFromId", String.class).invoke(null, nexoId);
            
            if (itemBuilder != null) {
                return (ItemStack) itemBuilder.getClass().getMethod("build").invoke(itemBuilder);
            }
        } catch (Exception e) {
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
            // Use reflection to access ItemsAdder API
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object customStack = customStackClass.getMethod("getInstance", String.class).invoke(null, itemsAdderId);
            
            if (customStack != null) {
                return (ItemStack) customStack.getClass().getMethod("getItemStack").invoke(customStack);
            }
        } catch (Exception e) {
            // ItemsAdder not available or API changed
        }
        
        return null;
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
    public static ItemStack createNavigationItem(JobsAdventure plugin, Material material, String name, String... lore) {
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
    public static ItemStack createFillerItem(JobsAdventure plugin) {
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
    public static ItemBuilder fromMaterialName(JobsAdventure plugin, String materialName) {
        // Check for Nexo items (nexo:item_id format)
        if (materialName.startsWith("nexo:")) {
            String nexoId = materialName.substring(5); // Remove "nexo:" prefix
            ItemStack nexoItem = createNexoItem(nexoId);
            if (nexoItem != null) {
                return new ItemBuilder(plugin, nexoItem);
            } else {
                plugin.getLogger().warning("Nexo item not found: " + nexoId + ", falling back to STONE");
                return new ItemBuilder(plugin, Material.STONE);
            }
        }
        
        // Check for ItemsAdder items (itemsadder:item_id format)
        if (materialName.startsWith("itemsadder:")) {
            String iaId = materialName.substring(11); // Remove "itemsadder:" prefix
            ItemStack iaItem = createItemsAdderItem(iaId);
            if (iaItem != null) {
                return new ItemBuilder(plugin, iaItem);
            } else {
                plugin.getLogger().warning("ItemsAdder item not found: " + iaId + ", falling back to STONE");
                return new ItemBuilder(plugin, Material.STONE);
            }
        }
        
        // Regular Bukkit material
        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            return new ItemBuilder(plugin, material);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown material: " + materialName + ", using STONE");
            return new ItemBuilder(plugin, Material.STONE);
        }
    }
}