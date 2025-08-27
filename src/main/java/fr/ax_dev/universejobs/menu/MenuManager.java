package fr.ax_dev.universejobs.menu;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.menu.impl.JobActionsMenu;
import fr.ax_dev.universejobs.menu.impl.JobsMainMenu;
import fr.ax_dev.universejobs.menu.impl.SingleJobMenu;
import fr.ax_dev.universejobs.menu.impl.GlobalRankingsMenu;
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
    
    public MenuManager(UniverseJobs plugin) {
        this.plugin = plugin;
        this.openMenus = new ConcurrentHashMap<>();
        this.menuConfig = new MenuConfig(plugin);
        
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
        
        JobsMainMenu menu = new JobsMainMenu(plugin, player, menuConfig.getMainMenuConfig());
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
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu != null) {
            // Check if player has our menu open
            if (menu.isInventory(event.getInventory())) {
                // This is a click in our menu (top inventory)
                event.setCancelled(true);
                
                // Only handle clicks in the top inventory (our menu)
                if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                    menu.handleClick(event.getSlot(), event);
                }
                // Clicks in bottom inventory (player inventory) are ignored but still cancelled
                
            } else if (event.getView().getTopInventory().equals(menu.getInventory())) {
                // Player has our menu open but clicked elsewhere - cancel to prevent item movement
                event.setCancelled(true);
            }
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
    }
    
    /**
     * Get the menu configuration.
     */
    public MenuConfig getMenuConfig() {
        return menuConfig;
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
}