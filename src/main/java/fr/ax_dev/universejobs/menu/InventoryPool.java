package fr.ax_dev.universejobs.menu;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import net.kyori.adventure.text.Component;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * High-performance inventory pool to avoid creating/destroying inventories.
 * Implements 2025 best practices for Minecraft server optimization.
 */
public class InventoryPool {

    private static final int MAX_POOL_SIZE = 50;
    private static final int DEFAULT_SIZE = 54;

    // Pool per size for optimal reuse
    private final Map<Integer, ConcurrentLinkedQueue<Inventory>> poolsBySize = new ConcurrentHashMap<>();
    private final Map<Inventory, Long> lastUsed = new ConcurrentHashMap<>();
    private static final long CLEANUP_INTERVAL = 300000; // 5 minutes

    private volatile long lastCleanup = System.currentTimeMillis();

    /**
     * Get or create an inventory from the pool.
     * Zero-allocation when inventory is available in pool.
     */
    public Inventory getInventory(int size, Component title, InventoryHolder holder) {
        // FIXME: Inventory pooling disabled for menus due to title persistence issue
        // In Bukkit, inventory titles cannot be changed once created
        // Pool reuse would show wrong titles for different jobs

        // Always create new inventory to ensure correct title
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        lastUsed.put(inventory, System.currentTimeMillis());
        return inventory;
    }

    /**
     * Return inventory to pool for reuse.
     * Call this when menu is closed.
     */
    public void returnInventory(Inventory inventory) {
        if (inventory == null) return;

        int size = inventory.getSize();
        ConcurrentLinkedQueue<Inventory> pool = poolsBySize.get(size);

        if (pool != null && pool.size() < MAX_POOL_SIZE) {
            inventory.clear(); // Clean state
            pool.offer(inventory);
        }

        lastUsed.remove(inventory);

        // Periodic cleanup
        cleanupIfNeeded();
    }

    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL) {
            cleanup();
            lastCleanup = now;
        }
    }

    private void cleanup() {
        long threshold = System.currentTimeMillis() - CLEANUP_INTERVAL;

        for (ConcurrentLinkedQueue<Inventory> pool : poolsBySize.values()) {
            pool.removeIf(inv -> {
                Long lastUse = lastUsed.get(inv);
                return lastUse != null && lastUse < threshold;
            });
        }
    }

    public void shutdown() {
        poolsBySize.clear();
        lastUsed.clear();
    }

    public int getPoolSize() {
        return poolsBySize.values().stream().mapToInt(ConcurrentLinkedQueue::size).sum();
    }
}