package fr.ax_dev.universejobs.menu;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.menu.config.MenuItemConfig;
import fr.ax_dev.universejobs.menu.config.SingleMenuConfig;
import fr.ax_dev.universejobs.reward.gui.ItemBuilder;
import fr.ax_dev.universejobs.utils.MessageUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base class for all menus providing common functionality.
 */
public abstract class BaseMenu implements InventoryHolder {
    
    protected final UniverseJobs plugin;
    protected final Player player;
    protected final SingleMenuConfig config;
    protected Inventory inventory;
    protected int currentPage = 0;
    
    public BaseMenu(UniverseJobs plugin, Player player, SingleMenuConfig config) {
        this.plugin = plugin;
        this.player = player;
        this.config = config;
        
        createInventory();
        populateInventory();
    }
    
    /**
     * Create the inventory with the configured title and size.
     */
    protected void createInventory() {
        String title = processPlaceholders(config.getTitle());
        Component titleComponent = MessageUtils.parseMessage(title);
        this.inventory = Bukkit.createInventory(this, config.getSize(), titleComponent);
    }
    
    /**
     * Populate the inventory with items.
     * This method should be implemented by each specific menu.
     */
    protected abstract void populateInventory();
    
    /**
     * Handle click events for this menu.
     */
    public abstract void handleClick(int slot, InventoryClickEvent event);
    
    /**
     * Open the menu for the player.
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Close the menu.
     */
    public void close() {
        player.closeInventory();
    }
    
    /**
     * Called when the inventory is closed.
     */
    public void onClose() {
        // Override in subclasses if needed
    }
    
    /**
     * Check if the given inventory belongs to this menu.
     */
    public boolean isInventory(Inventory inventory) {
        return this.inventory.equals(inventory);
    }
    
    /**
     * Refresh the menu by repopulating it.
     */
    public void refresh() {
        inventory.clear();
        populateInventory();
    }
    
    /**
     * Create an ItemStack from a MenuItemConfig using the existing ItemBuilder.
     */
    protected ItemStack createMenuItem(MenuItemConfig itemConfig) {
        return createMenuItem(itemConfig, null);
    }
    
    /**
     * Create an ItemStack from a MenuItemConfig with custom placeholders using existing ItemBuilder.
     */
    protected ItemStack createMenuItem(MenuItemConfig itemConfig, Map<String, String> customPlaceholders) {
        // Use existing ItemBuilder instead of duplicating functionality
        ItemBuilder builder = ItemBuilder.fromMaterialName(plugin, itemConfig.getMaterial())
                .amount(itemConfig.getAmount());
        
        // Process display name
        String displayName = itemConfig.getDisplayName();
        if (customPlaceholders != null) {
            displayName = replacePlaceholders(displayName, customPlaceholders);
        }
        displayName = processPlaceholders(displayName);
        builder.name(displayName);
        
        // Process lore
        List<String> lore = new ArrayList<>();
        for (String loreLine : itemConfig.getLore()) {
            String processedLore = loreLine;
            if (customPlaceholders != null) {
                processedLore = replacePlaceholders(processedLore, customPlaceholders);
            }
            processedLore = processPlaceholders(processedLore);
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
        
        // Add enchantments and glow effect after building
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
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
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Fill empty slots with the configured fill item.
     */
    protected void addFillItems() {
        MenuItemConfig fillConfig = config.getFillItem();
        if (fillConfig == null || !fillConfig.isEnabled()) return;
        
        ItemStack fillItem = createMenuItem(fillConfig);
        
        for (int slot : config.getFillSlots()) {
            if (slot >= 0 && slot < inventory.getSize() && inventory.getItem(slot) == null) {
                inventory.setItem(slot, fillItem);
            }
        }
    }
    
    /**
     * Process PlaceholderAPI placeholders in a string.
     */
    protected String processPlaceholders(String text) {
        if (text == null) return "";
        
        // Process PlaceholderAPI placeholders
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
    protected String replacePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null) return text;
        
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return result;
    }
    
    /**
     * Handle navigation button clicks.
     */
    protected boolean handleNavigationClick(int slot) {
        // Previous page
        if (config.getNavigationSlots().containsKey("previous") && 
            config.getNavigationSlots().get("previous").contains(slot)) {
            if (currentPage > 0) {
                currentPage--;
                refresh();
                return true;
            }
        }
        
        // Next page  
        if (config.getNavigationSlots().containsKey("next") && 
            config.getNavigationSlots().get("next").contains(slot)) {
            if (hasNextPage()) {
                currentPage++;
                refresh();
                return true;
            }
        }
        
        // Close button
        if (config.getNavigationSlots().containsKey("close") && 
            config.getNavigationSlots().get("close").contains(slot)) {
            close();
            return true;
        }
        
        // Back button
        if (config.getNavigationSlots().containsKey("back") && 
            config.getNavigationSlots().get("back").contains(slot)) {
            handleBackButton();
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if there's a next page available.
     */
    protected abstract boolean hasNextPage();
    
    /**
     * Handle back button click.
     */
    protected void handleBackButton() {
        // Default implementation - close the menu
        close();
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}