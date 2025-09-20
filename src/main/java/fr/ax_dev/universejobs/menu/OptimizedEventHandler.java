package fr.ax_dev.universejobs.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.InventoryHolder;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ultra-optimized event handler for menu interactions.
 * Implements 2025 best practices for minimal overhead event processing.
 */
public class OptimizedEventHandler implements Listener {

    private final MenuManager menuManager;
    private final Set<UUID> activeMenuPlayers = ConcurrentHashMap.newKeySet();

    public OptimizedEventHandler(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    /**
     * Pre-filter events - only process if player has active menu.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        // Fast exit if not a player
        if (!(event.getWhoClicked() instanceof Player player)) return;

        UUID playerId = player.getUniqueId();

        // Ultra-fast check: if player not in active set, ignore
        if (!activeMenuPlayers.contains(playerId)) return;

        InventoryHolder holder = event.getInventory().getHolder();

        // Fast type check with optimized holder
        if (!(holder instanceof OptimizedMenuHolder menuHolder)) return;

        // Verify this event belongs to this player
        if (!menuHolder.belongsTo(playerId)) return;

        // Cancel event immediately to prevent item manipulation
        event.setCancelled(true);

        // Route to specific menu handler
        BaseMenu menu = menuHolder.getMenu();
        if (menu != null) {
            try {
                menu.handleClick(event.getSlot(), event);
            } catch (Exception e) {
                // Log error but don't crash
                menuManager.getPlugin().getLogger().severe("Error handling menu click: " + e.getMessage());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        UUID playerId = player.getUniqueId();

        // Remove from active set immediately
        activeMenuPlayers.remove(playerId);

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof OptimizedMenuHolder menuHolder) {
            if (menuHolder.belongsTo(playerId)) {
                BaseMenu menu = menuHolder.getMenu();
                if (menu != null) {
                    menu.onClose();
                }

                // Return inventory to pool
                menuManager.returnInventoryToPool(event.getInventory());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        UUID playerId = player.getUniqueId();
        if (!activeMenuPlayers.contains(playerId)) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof OptimizedMenuHolder menuHolder) {
            if (menuHolder.belongsTo(playerId)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Register player as having an active menu.
     */
    public void registerActivePlayer(UUID playerId) {
        activeMenuPlayers.add(playerId);
    }

    /**
     * Unregister player from active menus.
     */
    public void unregisterActivePlayer(UUID playerId) {
        activeMenuPlayers.remove(playerId);
    }

    /**
     * Check if player has active menu.
     */
    public boolean hasActiveMenu(UUID playerId) {
        return activeMenuPlayers.contains(playerId);
    }

    /**
     * Get number of players with active menus.
     */
    public int getActiveMenuCount() {
        return activeMenuPlayers.size();
    }

    /**
     * Cleanup on shutdown.
     */
    public void shutdown() {
        activeMenuPlayers.clear();
    }
}