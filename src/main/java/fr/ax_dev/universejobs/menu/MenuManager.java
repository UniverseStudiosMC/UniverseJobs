package fr.ax_dev.universejobs.menu;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.menu.impl.JobActionsMenu;
import fr.ax_dev.universejobs.menu.impl.JobsMainMenu;
import fr.ax_dev.universejobs.menu.impl.SingleJobMenu;
import fr.ax_dev.universejobs.menu.impl.GlobalRankingsMenu;
import fr.ax_dev.universejobs.menu.config.JobSlotManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for all job-related menus.
 * Handles menu creation, event delegation, and cleanup.
 */
public class MenuManager implements Listener {
    
    private final UniverseJobs plugin;
    private final Map<UUID, BaseMenu> openMenus;
    private final MenuConfig menuConfig;
    private final JobSlotManager jobSlotManager;
    
    public MenuManager(UniverseJobs plugin) {
        this.plugin = plugin;
        this.openMenus = new ConcurrentHashMap<>();
        this.menuConfig = new MenuConfig(plugin);
        this.jobSlotManager = new JobSlotManager(plugin);
        
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Load menu configurations
        menuConfig.loadConfigurations();
    }
    
    /**
     * Open the main jobs menu for a player.
     */
    public void openJobsMainMenu(Player player) {
        closeCurrentMenu(player);
        
        JobsMainMenu menu = new JobsMainMenu(plugin, player, menuConfig.getMainMenuConfig(), jobSlotManager);
        openMenus.put(player.getUniqueId(), menu);
        menu.open();
    }
    
    /**
     * Open the menu for a specific job.
     */
    public void openJobMenu(Player player, String jobId) {
        closeCurrentMenu(player);
        
        SingleJobMenu menu = new SingleJobMenu(plugin, player, jobId, menuConfig.getJobMenuConfig());
        openMenus.put(player.getUniqueId(), menu);
        menu.open();
    }
    
    /**
     * Open the actions menu for a specific job.
     */
    public void openJobActionsMenu(Player player, String jobId) {
        closeCurrentMenu(player);
        
        JobActionsMenu menu = new JobActionsMenu(plugin, player, jobId, menuConfig.getActionsMenuConfig());
        openMenus.put(player.getUniqueId(), menu);
        menu.open();
    }
    
    /**
     * Open the global rankings menu.
     */
    public void openGlobalRankingsMenu(Player player) {
        closeCurrentMenu(player);
        
        GlobalRankingsMenu menu = new GlobalRankingsMenu(plugin, player, menuConfig.getRankingsMenuConfig());
        openMenus.put(player.getUniqueId(), menu);
        menu.open();
    }
    
    /**
     * Close the current menu for a player.
     */
    public void closeCurrentMenu(Player player) {
        BaseMenu currentMenu = openMenus.remove(player.getUniqueId());
        if (currentMenu != null) {
            currentMenu.close();
        }
    }
    
    /**
     * Handle inventory click events.
     * SECURITY: Only allow actions in the top inventory (menu), not in player inventory.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu != null && event.getView().getTopInventory().equals(menu.getInventory())) {
            // Player has our menu open - always cancel to prevent item theft/movement
            event.setCancelled(true);
            
            // Only process menu actions if click is in the TOP inventory (our menu)
            if (isClickInMenuInventory(event, menu)) {
                // Additional security: Block potentially dangerous click types
                if (isSecureClickType(event)) {
                    menu.handleClick(event.getSlot(), event);
                }
                // Dangerous click types (like number keys, middle click, etc.) are blocked
            }
            // Clicks in bottom inventory (player inventory) are cancelled but ignored
            // This prevents players from:
            // - Stealing items from the menu
            // - Moving items between menu and their inventory  
            // - Shift-clicking items into the menu
            // - Using hotbar keys to swap items
            // - Double-clicking to gather items
        }
    }
    
    /**
     * Handle inventory drag events to prevent item dragging in menus.
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu != null) {
            // Check if dragging involves our menu inventory
            if (event.getView().getTopInventory().equals(menu.getInventory())) {
                // Cancel any drag that involves the menu inventory
                for (int slot : event.getRawSlots()) {
                    if (slot < event.getView().getTopInventory().getSize()) {
                        event.setCancelled(true);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Handle inventory close events.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu != null && menu.isInventory(event.getInventory())) {
            openMenus.remove(player.getUniqueId());
            menu.onClose();
        }
    }
    
    /**
     * Close all open menus.
     */
    public void closeAllMenus() {
        openMenus.values().forEach(BaseMenu::close);
        openMenus.clear();
    }
    
    /**
     * Reload menu configurations.
     */
    public void reloadConfigurations() {
        menuConfig.loadConfigurations();
        jobSlotManager.reload();
    }
    
    /**
     * Get the menu configuration.
     */
    public MenuConfig getMenuConfig() {
        return menuConfig;
    }
    
    /**
     * Get the job slot manager.
     */
    public JobSlotManager getJobSlotManager() {
        return jobSlotManager;
    }
    
    /**
     * Refresh the current menu for a player (if any is open).
     * This is useful after job join/leave operations to update button states.
     */
    public void refreshPlayerMenu(Player player) {
        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu != null) {
            menu.refresh();
        }
    }
    
    /**
     * Check if a player has a menu open.
     */
    public boolean hasMenuOpen(Player player) {
        return openMenus.containsKey(player.getUniqueId());
    }
    
    /**
     * Get the currently open menu for a player.
     */
    public BaseMenu getCurrentMenu(Player player) {
        return openMenus.get(player.getUniqueId());
    }
    
    /**
     * Check if a click is in the top inventory (menu) vs bottom inventory (player).
     */
    private boolean isClickInMenuInventory(InventoryClickEvent event, BaseMenu menu) {
        return event.getClickedInventory() != null && 
               event.getClickedInventory().equals(event.getView().getTopInventory()) &&
               event.getClickedInventory().equals(menu.getInventory());
    }
    
    /**
     * Check if a click type is secure for menu interactions.
     * Blocks potentially exploitable click types.
     */
    private boolean isSecureClickType(InventoryClickEvent event) {
        switch (event.getClick()) {
            // Allow basic clicks
            case LEFT:
            case RIGHT:
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                return true;
                
            // Block potentially dangerous click types
            case DOUBLE_CLICK:          // Could gather items
            case NUMBER_KEY:            // Hotbar key swapping
            case DROP:                  // Drop items
            case CONTROL_DROP:          // Drop stack
            case CREATIVE:              // Creative mode middle-click
            case UNKNOWN:               // Unknown behavior
                return false;
                
            // Block other edge cases
            default:
                return false;
        }
    }
}