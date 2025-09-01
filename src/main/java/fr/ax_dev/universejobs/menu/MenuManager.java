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
    private final Map<UUID, BoostManagerGui> openBoostGuis;
    private final MenuConfig menuConfig;
    private final JobSlotManager jobSlotManager;
    
    public MenuManager(UniverseJobs plugin) {
        this.plugin = plugin;
        this.openMenus = new ConcurrentHashMap<>();
        this.openBoostGuis = new ConcurrentHashMap<>();
        this.menuConfig = new MenuConfig(plugin);
        this.jobSlotManager = new JobSlotManager(plugin);
        
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Load menu configurations
        menuConfig.loadConfigurations();
        
        // Initialize job slot manager with MenuConfig reference
        jobSlotManager.initialize(menuConfig);
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
        
        // Use centralized accessor for cleaner code
        var accessor = plugin.getAccessor();
        
        // Check if job exists before creating menu
        if (accessor.getJobManager().getJob(jobId) == null) {
            accessor.getLanguageManager().sendMessage(player, "job-not-found", jobId);
            return;
        }
        
        try {
            SingleJobMenu menu = new SingleJobMenu(plugin, player, jobId, menuConfig.getJobMenuConfig());
            openMenus.put(player.getUniqueId(), menu);
            menu.open();
        } catch (IllegalArgumentException e) {
            accessor.logWarning("Failed to create SingleJobMenu: " + e.getMessage());
            // Menu creation failed silently - job was already validated above
        }
    }
    
    /**
     * Open the actions menu for a specific job.
     */
    public void openJobActionsMenu(Player player, String jobId) {
        closeCurrentMenu(player);
        
        // Use centralized accessor for cleaner code
        var accessor = plugin.getAccessor();
        
        // Check if job exists before creating menu
        if (accessor.getJobManager().getJob(jobId) == null) {
            accessor.getLanguageManager().sendMessage(player, "job-not-found", jobId);
            return;
        }
        
        try {
            JobActionsMenu menu = new JobActionsMenu(plugin, player, jobId, menuConfig.getActionsMenuConfig());
            openMenus.put(player.getUniqueId(), menu);
            menu.open();
        } catch (IllegalArgumentException e) {
            accessor.logWarning("Failed to create JobActionsMenu: " + e.getMessage());
            // Menu creation failed silently - job was already validated above
        }
    }
    
    /**
     * Open the rewards menu for a specific job.
     */
    public void openRewardsMenu(Player player, String jobId) {
        closeCurrentMenu(player);
        
        // Use centralized accessor for cleaner code
        var accessor = plugin.getAccessor();
        
        // Check if job exists before creating menu
        if (accessor.getJobManager().getJob(jobId) == null) {
            accessor.getLanguageManager().sendMessage(player, "job-not-found", jobId);
            return;
        }
        
        // Open rewards GUI using existing RewardGuiManager
        accessor.getRewardGuiManager().openRewardsGui(player, jobId);
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
        
        BoostManagerGui currentBoostGui = openBoostGuis.remove(player.getUniqueId());
        if (currentBoostGui != null) {
            currentBoostGui.onInventoryClose(player);
            player.closeInventory();
        }
    }
    
    /**
     * Register a BoostManagerGui as open for a player.
     */
    public void registerBoostGui(Player player, BoostManagerGui boostGui) {
        closeCurrentMenu(player);
        openBoostGuis.put(player.getUniqueId(), boostGui);
    }
    
    /**
     * Unregister a BoostManagerGui for a player.
     */
    public void unregisterBoostGui(Player player) {
        BoostManagerGui boostGui = openBoostGuis.remove(player.getUniqueId());
        if (boostGui != null) {
            boostGui.onInventoryClose(player);
        }
    }
    
    /**
     * Get the currently open BoostManagerGui for a player.
     */
    public BoostManagerGui getCurrentBoostGui(Player player) {
        return openBoostGuis.get(player.getUniqueId());
    }
    
    /**
     * Handle inventory click events.
     * SECURITY: Only allow actions in the top inventory (menu), not in player inventory.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Check for BaseMenu first
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
            return;
        }
        
        // Check for BoostManagerGui
        BoostManagerGui boostGui = openBoostGuis.get(player.getUniqueId());
        if (boostGui != null && event.getInventory().getHolder() == boostGui) {
            // Player has boost GUI open - always cancel to prevent item theft/movement
            event.setCancelled(true);
            
            // Only process if click is in the boost GUI inventory and is a secure click
            if (event.getClickedInventory() != null && 
                event.getClickedInventory().equals(event.getView().getTopInventory()) &&
                event.getClickedInventory().getHolder() == boostGui &&
                isSecureClickType(event)) {
                
                boostGui.handleClick(player, event.getSlot(), event.isRightClick());
            }
            return;
        }
    }
    
    /**
     * Handle inventory drag events to prevent item dragging in menus.
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Check BaseMenu first
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
            return;
        }
        
        // Check BoostManagerGui
        BoostManagerGui boostGui = openBoostGuis.get(player.getUniqueId());
        if (boostGui != null && event.getView().getTopInventory().getHolder() == boostGui) {
            // Cancel any drag that involves the boost GUI inventory
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    break;
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
        
        // Check BaseMenu first
        BaseMenu menu = openMenus.get(player.getUniqueId());
        if (menu != null && menu.isInventory(event.getInventory())) {
            openMenus.remove(player.getUniqueId());
            menu.onClose();
            return;
        }
        
        // Check BoostManagerGui
        BoostManagerGui boostGui = openBoostGuis.get(player.getUniqueId());
        if (boostGui != null && boostGui.isInventory(event.getInventory())) {
            openBoostGuis.remove(player.getUniqueId());
            boostGui.onInventoryClose(player);
        }
    }
    
    /**
     * Close all open menus.
     */
    public void closeAllMenus() {
        openMenus.values().forEach(BaseMenu::close);
        openMenus.clear();
        
        for (Map.Entry<UUID, BoostManagerGui> entry : openBoostGuis.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                entry.getValue().onInventoryClose(player);
                player.closeInventory();
            }
        }
        openBoostGuis.clear();
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