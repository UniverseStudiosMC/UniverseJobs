package fr.ax_dev.universejobs.menu;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.menu.config.MenuItemConfig;
import fr.ax_dev.universejobs.menu.config.SingleMenuConfig;
import fr.ax_dev.universejobs.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

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
        
        // Don't create inventory here - let subclass call initialize() when ready
    }
    
    /**
     * Initialize the menu after all fields are set.
     * Must be called by subclasses after their initialization is complete.
     */
    protected final void initialize() {
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
        return MenuUtils.createMenuItem(plugin, player, itemConfig);
    }
    
    /**
     * Create an ItemStack from a MenuItemConfig with custom placeholders using existing ItemBuilder.
     */
    protected ItemStack createMenuItem(MenuItemConfig itemConfig, Map<String, String> customPlaceholders) {
        return MenuUtils.createMenuItem(plugin, player, itemConfig, customPlaceholders);
    }
    
    /**
     * Fill empty slots with the configured fill item.
     * Uses fill-item slots if defined, otherwise uses global fill-slots.
     */
    protected void addFillItems() {
        MenuItemConfig fillConfig = config.getFillItem();
        if (fillConfig == null || !fillConfig.isEnabled()) return;

        ItemStack fillItem = createMenuItem(fillConfig);
        if (fillItem == null) {
            plugin.getLogger().warning("Failed to create fill item for menu. Material: " + fillConfig.getMaterial());
            return;
        }

        // Use fill-item's specific slots if defined, otherwise use global fill-slots
        List<Integer> slotsToFill = fillConfig.getSlots();
        if (slotsToFill.isEmpty()) {
            slotsToFill = config.getFillSlots();
        }

        // If no specific slots defined, fill all empty slots
        if (slotsToFill.isEmpty()) {
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, fillItem);
                }
            }
        } else {
            for (int slot : slotsToFill) {
                if (slot >= 0 && slot < inventory.getSize() && inventory.getItem(slot) == null) {
                    inventory.setItem(slot, fillItem);
                }
            }
        }
    }
    
    /**
     * Process PlaceholderAPI placeholders in a string.
     */
    protected String processPlaceholders(String text) {
        return MenuUtils.processPlaceholders(player, text);
    }
    
    /**
     * Replace custom placeholders in a string.
     */
    protected String replacePlaceholders(String text, Map<String, String> placeholders) {
        return MenuUtils.replacePlaceholders(text, placeholders);
    }
    
    /**
     * Handle navigation button clicks based on action property.
     */
    protected boolean handleNavigationClick(int slot) {
        // Check navigation items for this slot
        for (MenuItemConfig navItem : config.getNavigationItems().values()) {
            if (navItem.getSlots().contains(slot)) {
                String action = navItem.getAction();
                return handleActionClick(action);
            }
        }
        
        return false;
    }
    
    /**
     * Handle specific action clicks.
     */
    protected boolean handleActionClick(String action) {
        switch (action) {
            case "previous_page":
                if (currentPage > 0) {
                    currentPage--;
                    refresh();
                    return true;
                }
                break;
            case "next_page":
                if (hasNextPage()) {
                    currentPage++;
                    refresh();
                    return true;
                }
                break;
            case "close":
                close();
                return true;
            case "back":
                handleBackButton();
                return true;
            case "none":
                return true; // Consume click but do nothing
        }
        return false;
    }
    
    /**
     * Handle navigation button clicks based on action property with sound support.
     */
    protected boolean handleNavigationClickWithSound(int slot) {
        // Check navigation items for this slot
        for (MenuItemConfig navItem : config.getNavigationItems().values()) {
            if (navItem.getSlots().contains(slot)) {
                String action = navItem.getAction();
                String sound = navItem.getSound();
                
                // Play sound if specified
                if (sound != null && !sound.isEmpty()) {
                    playSound(sound);
                }
                
                // Execute commands if present
                if (navItem.getCommands() != null && !navItem.getCommands().isEmpty()) {
                    executeCommands(navItem.getCommands());
                }
                
                return handleActionClick(action);
            }
        }
        
        return false;
    }
    
    /**
     * Execute commands for menu items.
     */
    protected void executeCommands(List<String> commands) {
        if (commands == null || commands.isEmpty()) return;
        
        for (String command : commands) {
            // Replace placeholders
            String processedCommand = command.replace("{player}", player.getName());
            
            // Handle command prefixes
            if (processedCommand.startsWith("[console] ")) {
                // Execute as console
                String consoleCommand = processedCommand.substring(10);
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), consoleCommand);
            } else if (processedCommand.startsWith("[player] ")) {
                // Execute as player
                String playerCommand = processedCommand.substring(9);
                plugin.getServer().dispatchCommand(player, playerCommand);
            } else {
                // Default: execute as console
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), processedCommand);
            }
        }
    }
    
    /**
     * Play a sound for the player.
     */
    protected void playSound(String soundName) {
        if (soundName == null || soundName.isEmpty()) return;
        
        org.bukkit.Sound sound = fr.ax_dev.universejobs.utils.EnumUtils.parseSound(soundName.replace(".", "_"), null);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } else {
            // Try with the sound name as-is for custom sounds
            try {
                player.playSound(player.getLocation(), soundName, 1.0f, 1.0f);
            } catch (Exception ex) {
                plugin.getLogger().warning("Invalid sound: " + soundName);
            }
        }
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