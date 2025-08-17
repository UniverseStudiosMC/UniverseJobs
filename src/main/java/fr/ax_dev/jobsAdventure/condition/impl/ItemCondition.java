package fr.ax_dev.jobsAdventure.condition.impl;

import fr.ax_dev.jobsAdventure.condition.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import de.tr7zw.changeme.nbtapi.NBTItem;
import java.util.List;
import java.util.ArrayList;

/**
 * Condition that checks the item in the player's hand.
 * Supports material, NBT, custom model data, and MMOItems integration.
 */
public class ItemCondition extends AbstractCondition {
    
    private final List<String> materials;
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
        
        // Handle both single material and material list
        this.materials = new ArrayList<>();
        if (config.isList("material")) {
            this.materials.addAll(config.getStringList("material"));
        } else if (config.getString("material") != null) {
            this.materials.add(config.getString("material"));
        }
        
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
        
        // Check materials (OR logic - item must match at least one material)
        if (!materials.isEmpty()) {
            boolean materialMatched = false;
            for (String materialName : materials) {
                try {
                    Material requiredMaterial = Material.valueOf(materialName.toUpperCase());
                    if (item.getType() == requiredMaterial) {
                        materialMatched = true;
                        break;
                    }
                } catch (IllegalArgumentException e) {
                    // Invalid material name, continue checking others
                    continue;
                }
            }
            if (!materialMatched) {
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
        return "ItemCondition{materials=" + materials + ", hasNBT=" + (nbtKey != null) + 
               ", hasMMOItems=" + (mmoItemsType != null) + 
               "}";
    }
}