package fr.ax_dev.universejobs.menu.utils;

import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.menu.config.MenuItemConfig;
import fr.ax_dev.universejobs.menu.config.SimpleConfigurationSection;

import java.util.*;

/**
 * Utility class for creating common menu items and reducing code duplication.
 */
public class MenuItemUtils {
    
    /**
     * Create a standard item configuration map.
     */
    public static Map<String, Object> createItemConfigMap(String material, String displayName, List<String> lore, boolean glow) {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("enabled", true);
        configMap.put("material", material);
        configMap.put("amount", 1);
        configMap.put("display-name", displayName);
        configMap.put("lore", lore);
        configMap.put("custom-model-data", 0);
        configMap.put("glow", glow);
        configMap.put("hide-attributes", false);
        configMap.put("hide-enchants", glow);
        configMap.put("slots", new ArrayList<Integer>());
        configMap.put("action", "none");
        configMap.put("action-value", "");
        return configMap;
    }
    
    /**
     * Create a MenuItemConfig from basic parameters.
     */
    public static MenuItemConfig createMenuItemConfig(String material, String displayName, List<String> lore, String action, boolean glow) {
        Map<String, Object> configMap = createItemConfigMap(material, displayName, lore, glow);
        configMap.put("action", action);
        return new MenuItemConfig(new SimpleConfigurationSection(configMap));
    }
    
    /**
     * Create a join/leave button configuration.
     */
    public static MenuItemConfig createJoinLeaveButton(boolean hasJob, String jobName) {
        String material = hasJob ? "RED_CONCRETE" : "GREEN_CONCRETE";
        String displayName = hasJob ? "&c&lLeave Job" : "&a&lJoin Job";
        String action = hasJob ? "leave_job" : "join_job";
        
        List<String> lore = new ArrayList<>();
        if (hasJob) {
            lore.add("&7Click to leave this job");
            lore.add("");
            lore.add("&c&lWARNING:");
            lore.add("&cYou will lose all progress!");
        } else {
            lore.add("&7Click to join this job");
        }
        
        return createMenuItemConfig(material, displayName, lore, action, false);
    }
    
    /**
     * Create an actions button configuration.
     */
    public static MenuItemConfig createActionsButton() {
        List<String> lore = Arrays.asList(
            "&7View all available actions",
            "&7and their rewards for this job",
            "",
            "&e▶ Click to open actions menu"
        );
        
        return createMenuItemConfig("DIAMOND_PICKAXE", "&6&lActions & Rewards", lore, "open_actions", false);
    }
    
    /**
     * Create a rankings button configuration.
     */
    public static MenuItemConfig createRankingsButton() {
        List<String> lore = Arrays.asList(
            "&7View global job rankings",
            "&7and see top players",
            "",
            "&e▶ Click to open rankings"
        );
        
        return createMenuItemConfig("GOLD_INGOT", "&6&lGlobal Rankings", lore, "open_rankings", false);
    }
    
    /**
     * Create a back button configuration.
     */
    public static MenuItemConfig createBackButton(String displayName, String loreText) {
        List<String> lore = Arrays.asList("&7" + loreText);
        return createMenuItemConfig("ARROW", displayName, lore, "back", false);
    }
    
    /**
     * Create a close button configuration.
     */
    public static MenuItemConfig createCloseButton() {
        List<String> lore = Arrays.asList("&7Close this menu");
        return createMenuItemConfig("BARRIER", "&c&lClose", lore, "close", false);
    }
    
    /**
     * Get appropriate material for action type display.
     */
    public static String getActionMaterial(ActionType actionType) {
        return switch (actionType) {
            case KILL -> "DIAMOND_SWORD";
            case PLACE -> "GRASS_BLOCK";
            case BREAK -> "IRON_PICKAXE";
            case HARVEST -> "WHEAT";
            case BLOCK_INTERACT -> "OAK_BUTTON";
            case ENTITY_INTERACT -> "LEAD";
            case BREED -> "WHEAT_SEEDS";
            case FISH -> "FISHING_ROD";
            case CRAFT -> "CRAFTING_TABLE";
            case SMELT -> "FURNACE";
            case ENCHANT -> "ENCHANTING_TABLE";
            case TRADE -> "EMERALD";
            case TAME -> "BONE";
            case SHEAR -> "SHEARS";
            case MILK -> "MILK_BUCKET";
            case EAT -> "COOKED_BEEF";
            case POTION -> "POTION";
            case CUSTOM -> "COMMAND_BLOCK";
            default -> "STONE";
        };
    }
    
    /**
     * Format action name for display.
     */
    public static String formatActionName(ActionType actionType) {
        return actionType.name().toLowerCase().replace("_", " ");
    }
    
    /**
     * Create standard navigation placeholders.
     */
    public static Map<String, String> createNavigationPlaceholders(int currentPage, int totalItems, int itemsPerPage) {
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("current_page", String.valueOf(currentPage + 1));
        placeholders.put("total_pages", String.valueOf(totalPages));
        placeholders.put("total_items", String.valueOf(totalItems));
        return placeholders;
    }
    
    /**
     * Create job-specific placeholders.
     */
    public static Map<String, String> createJobPlaceholders(String jobId, String jobName, String jobDescription) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("job_id", jobId);
        placeholders.put("job_name", jobName);
        placeholders.put("job_description", jobDescription);
        return placeholders;
    }
    
    /**
     * Add navigation items to inventory from configuration.
     */
    public static void addNavigationItems(org.bukkit.inventory.Inventory inventory, Map<String, MenuItemConfig> navItems,
                                        int currentPage, boolean hasNextPage, Map<String, String> placeholders,
                                        java.util.function.Function<MenuItemConfig, org.bukkit.inventory.ItemStack> itemCreator) {
        
        // Previous page
        if (navItems.containsKey("previous") && currentPage > 0) {
            MenuItemConfig prevConfig = navItems.get("previous");
            org.bukkit.inventory.ItemStack prevItem = itemCreator.apply(prevConfig);
            
            for (int slot : prevConfig.getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, prevItem);
                }
            }
        }
        
        // Next page
        if (navItems.containsKey("next") && hasNextPage) {
            MenuItemConfig nextConfig = navItems.get("next");
            org.bukkit.inventory.ItemStack nextItem = itemCreator.apply(nextConfig);
            
            for (int slot : nextConfig.getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, nextItem);
                }
            }
        }
        
        // Back button
        if (navItems.containsKey("back")) {
            MenuItemConfig backConfig = navItems.get("back");
            org.bukkit.inventory.ItemStack backItem = itemCreator.apply(backConfig);
            
            for (int slot : backConfig.getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, backItem);
                }
            }
        }
        
        // Close button
        if (navItems.containsKey("close")) {
            MenuItemConfig closeConfig = navItems.get("close");
            org.bukkit.inventory.ItemStack closeItem = itemCreator.apply(closeConfig);
            
            for (int slot : closeConfig.getSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, closeItem);
                }
            }
        }
    }
    
    /**
     * Add static items to inventory from configuration.
     */
    public static void addStaticItems(org.bukkit.inventory.Inventory inventory, Map<String, MenuItemConfig> staticItems,
                                    Map<String, String> placeholders,
                                    java.util.function.Function<MenuItemConfig, org.bukkit.inventory.ItemStack> itemCreator) {
        
        for (Map.Entry<String, MenuItemConfig> entry : staticItems.entrySet()) {
            MenuItemConfig itemConfig = entry.getValue();
            if (!itemConfig.isEnabled()) continue;
            
            org.bukkit.inventory.ItemStack item = itemCreator.apply(itemConfig);
            
            for (int slot : itemConfig.getSlots()) {
                if (slot >= 0 && slot < inventory.getSize() && inventory.getItem(slot) == null) {
                    inventory.setItem(slot, item);
                }
            }
        }
    }
}