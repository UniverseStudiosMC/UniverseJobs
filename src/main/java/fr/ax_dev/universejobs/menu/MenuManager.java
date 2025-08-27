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
        if (menu != null && menu.isInventory(event.getInventory())) {
            event.setCancelled(true);
            menu.handleClick(event.getSlot(), event);
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
}