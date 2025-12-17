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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for all job-related menus.
 * Handles menu creation, event delegation, and cleanup.
 */
public class MenuManager {
    
    private final UniverseJobs plugin;
    private final Map<UUID, BaseMenu> openMenus;
    private final Map<UUID, BoostManagerGui> openBoostGuis;
    private final MenuConfig menuConfig;
    private final JobSlotManager jobSlotManager;
    private final AsyncMenuLoader asyncLoader;

    // 2025 Performance Systems
    private final InventoryPool inventoryPool;
    private final ComponentCache componentCache;
    private final OptimizedEventHandler eventHandler;
    private final MenuScheduler scheduler;

    public MenuManager(UniverseJobs plugin) {
        this.plugin = plugin;
        this.openMenus = new ConcurrentHashMap<>();
        this.openBoostGuis = new ConcurrentHashMap<>();
        this.menuConfig = new MenuConfig(plugin);
        this.jobSlotManager = new JobSlotManager(plugin);
        this.asyncLoader = new AsyncMenuLoader(plugin);

        // Initialize 2025 performance systems
        this.inventoryPool = new InventoryPool();
        this.componentCache = new ComponentCache();
        this.eventHandler = new OptimizedEventHandler(this);
        this.scheduler = new MenuScheduler(plugin);

        // Register optimized event handler instead of default
        Bukkit.getPluginManager().registerEvents(eventHandler, plugin);

        // Load menu configurations
        menuConfig.loadConfigurations();

        // Initialize job slot manager with MenuConfig reference
        jobSlotManager.initialize(menuConfig);

        // Pre-warm caches for optimal performance
        componentCache.preWarm();
    }
    
    /**
     * Open the main jobs menu for a player.
     */
    public void openJobsMainMenu(Player player) {
        closeCurrentMenu(player);

        // Register player as having active menu for fast event filtering
        eventHandler.registerActivePlayer(player.getUniqueId());

        // Optimized async preload + sync creation
        scheduler.runAsync(() -> {
            // Pre-calculate placeholders async
            asyncLoader.preloadJobsMainMenuData(player);
        });

        // Create menu sync with optimized systems
        scheduler.runSync(player, () -> {
            JobsMainMenu menu = new JobsMainMenu(plugin, player, menuConfig.getMainMenuConfig(), jobSlotManager);
            openMenus.put(player.getUniqueId(), menu);
            menu.open();
        });
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

        // Register player as having active menu for fast event filtering
        eventHandler.registerActivePlayer(player.getUniqueId());

        scheduler.runSync(player, () -> {
            try {
                SingleJobMenu menu = new SingleJobMenu(plugin, player, jobId, menuConfig.getJobMenuConfig());
                openMenus.put(player.getUniqueId(), menu);
                menu.open();
            } catch (IllegalArgumentException e) {
                accessor.logWarning("Failed to create SingleJobMenu: " + e.getMessage());
                // Menu creation failed silently - job was already validated above
            }
        });
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

        // Register player as having active menu for fast event filtering
        eventHandler.registerActivePlayer(player.getUniqueId());

        scheduler.runSync(player, () -> {
            try {
                JobActionsMenu menu = new JobActionsMenu(plugin, player, jobId, menuConfig.getActionsMenuConfig());
                openMenus.put(player.getUniqueId(), menu);
                menu.open();
            } catch (IllegalArgumentException e) {
                accessor.logWarning("Failed to create JobActionsMenu: " + e.getMessage());
            }
        });
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
        openGlobalRankingsMenu(player, null);
    }

    /**
     * Open the global rankings menu with a pre-selected job.
     */
    public void openGlobalRankingsMenu(Player player, String preSelectedJob) {
        closeCurrentMenu(player);

        eventHandler.registerActivePlayer(player.getUniqueId());

        GlobalRankingsMenu menu = new GlobalRankingsMenu(plugin, player, menuConfig.getRankingsMenuConfig(), preSelectedJob);
        openMenus.put(player.getUniqueId(), menu);

        if (menu.isReady()) {
            scheduler.runSync(player, menu::open);
        }
    }
    
    /**
     * Close the current menu for a player.
     */
    public void closeCurrentMenu(Player player) {
        UUID playerId = player.getUniqueId();

        BaseMenu currentMenu = openMenus.remove(playerId);
        if (currentMenu != null) {
            currentMenu.close();
        }

        BoostManagerGui currentBoostGui = openBoostGuis.remove(playerId);
        if (currentBoostGui != null) {
            currentBoostGui.onInventoryClose(player);
            player.closeInventory();
        }

        // Unregister from active players and clear cache
        eventHandler.unregisterActivePlayer(playerId);
        componentCache.clearPlayer(playerId.toString());
    }

    /**
     * Get inventory from pool for optimal performance.
     */
    public org.bukkit.inventory.Inventory getInventoryFromPool(int size, net.kyori.adventure.text.Component title, org.bukkit.inventory.InventoryHolder holder) {
        return inventoryPool.getInventory(size, title, holder);
    }

    /**
     * Return inventory to pool when menu is closed.
     */
    public void returnInventoryToPool(org.bukkit.inventory.Inventory inventory) {
        inventoryPool.returnInventory(inventory);
    }

    /**
     * Get component cache for menu rendering.
     */
    public ComponentCache getComponentCache() {
        return componentCache;
    }

    /**
     * Get optimized scheduler for menu operations.
     */
    public MenuScheduler getScheduler() {
        return scheduler;
    }

    /**
     * Get plugin reference.
     */
    public UniverseJobs getPlugin() {
        return plugin;
    }
    
    /**
     * Register a BoostManagerGui as open for a player.
     */
    public void registerBoostGui(Player player, BoostManagerGui boostGui) {
        closeCurrentMenu(player);
        eventHandler.registerActivePlayer(player.getUniqueId());
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
    
    // Event handlers are now managed by OptimizedEventHandler for better performance
    
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
     * Shutdown all menu systems for optimal cleanup.
     */
    public void shutdown() {
        // Close all open menus
        for (BaseMenu menu : openMenus.values()) {
            if (menu != null) {
                menu.close();
            }
        }
        openMenus.clear();

        for (BoostManagerGui gui : openBoostGuis.values()) {
            if (gui != null) {
                // Close without calling onInventoryClose to avoid issues
            }
        }
        openBoostGuis.clear();

        // Shutdown performance systems
        scheduler.shutdown();
        inventoryPool.shutdown();
        componentCache.clear();
        eventHandler.shutdown();
        asyncLoader.shutdown();
    }

    /**
     * Get performance statistics.
     */
    public String getPerformanceStats() {
        return String.format("MenuManager Stats: " +
            "Active Menus: %d, " +
            "Pool Size: %d, " +
            "Component Cache: %d items, " +
            "Active Menu Players: %d, " +
            "Scheduler: %s",
            openMenus.size(),
            inventoryPool.getPoolSize(),
            componentCache.getComponentCacheSize(),
            eventHandler.getActiveMenuCount(),
            scheduler.getStats().toString()
        );
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
