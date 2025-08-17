package fr.ax_dev.jobsAdventure.condition.impl;

import fr.ax_dev.jobsAdventure.condition.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import de.tr7zw.changeme.nbtapi.NBTItem;

/**
 * Condition that checks the item in the player's hand.
 * Supports material, NBT, custom model data, and MMOItems integration.
 */
public class ItemCondition extends AbstractCondition {
    
    private final String material;
    private final String nbtKey;
    private final String nbtValue;
    private final Integer customModelData;
    private final String mmoItemsType;
    private final String mmoItemsId;
    
    /**
     * Create an item condition from configuration.
     * 
     * @param config The configuration section
     */
    public ItemCondition(ConfigurationSection config) {
        super(config);
        this.material = config.getString("material");
        this.nbtKey = config.getString("nbt.key");
        this.nbtValue = config.getString("nbt.value");
        this.customModelData = config.contains("custom-model-data") ? 
                              config.getInt("custom-model-data") : null;
        this.mmoItemsType = config.getString("mmoitems.type");
        this.mmoItemsId = config.getString("mmoitems.id");
    }
    
    @Override
    public boolean isMet(Player player, Event event, ConditionContext context) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        // Check material
        if (material != null) {
            try {
                Material requiredMaterial = Material.valueOf(material.toUpperCase());
                if (item.getType() != requiredMaterial) {
                    return false;
                }
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        
        // Check custom model data
        if (customModelData != null) {
            if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData() ||
                item.getItemMeta().getCustomModelData() != customModelData) {
                return false;
            }
        }
        
        // Check NBT data
        if (nbtKey != null && nbtValue != null) {
            try {
                de.tr7zw.changeme.nbtapi.NBTItem nbtItem = new NBTItem(item);
                if (!nbtItem.hasKey(nbtKey)) {
                    return false;
                }
                
                String itemNbtValue = nbtItem.getString(nbtKey);
                if (!nbtValue.equals(itemNbtValue)) {
                    return false;
                }
            } catch (Exception e) {
                // NBT-API not available or error occurred
                return false;
            }
        }
        
        // Check MMOItems data
        if (mmoItemsType != null && mmoItemsId != null) {
            if (!checkMMOItemsData(item)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check MMOItems data using NBT.
     * 
     * @param item The item to check
     * @return true if it matches
     */
    private boolean checkMMOItemsData(ItemStack item) {
        try {
            NBTItem nbtItem = new NBTItem(item);
            
            // Check if item has MMOItems NBT
            if (!nbtItem.hasKey("MMOITEMS_ITEM_TYPE") || !nbtItem.hasKey("MMOITEMS_ITEM_ID")) {
                return false;
            }
            
            String itemType = nbtItem.getString("MMOITEMS_ITEM_TYPE");
            String itemId = nbtItem.getString("MMOITEMS_ITEM_ID");
            
            return mmoItemsType.equals(itemType) && mmoItemsId.equals(itemId);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public ConditionType getType() {
        return ConditionType.ITEM;
    }
    
    @Override
    public String toString() {
        return "ItemCondition{material='" + material + "', hasNBT=" + (nbtKey != null) + 
               ", hasMMOItems=" + (mmoItemsType != null) + 
               "}";
    }
}