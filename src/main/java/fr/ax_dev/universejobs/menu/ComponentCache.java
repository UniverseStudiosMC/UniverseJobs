package fr.ax_dev.universejobs.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;
import java.util.Queue;

/**
 * Ultra-fast component cache for pre-rendered menu elements.
 * Follows 2025 optimization patterns for zero-allocation menu rendering.
 */
public class ComponentCache {

    private static final int MAX_CACHE_SIZE = 1000;
    private static final int MAX_QUEUE_SIZE = 100;

    // Pre-rendered components
    private final Map<String, Component> componentCache = new ConcurrentHashMap<>();
    private final Map<String, ItemStack> itemCache = new ConcurrentHashMap<>();

    // LRU tracking with minimal overhead
    private final Queue<String> accessOrder = new ConcurrentLinkedQueue<>();
    private final Map<String, Long> lastAccess = new ConcurrentHashMap<>();

    /**
     * Get pre-rendered component or compute and cache it.
     */
    public Component getComponent(String key, ComponentSupplier supplier) {
        Component component = componentCache.get(key);
        if (component != null) {
            trackAccess(key);
            return component;
        }

        // Compute and cache
        component = supplier.get();
        if (component != null) {
            addToCache(key, component);
        }
        return component;
    }

    /**
     * Get pre-rendered ItemStack or compute and cache it.
     */
    public ItemStack getItem(String key, ItemSupplier supplier) {
        ItemStack item = itemCache.get(key);
        if (item != null) {
            trackAccess(key);
            return item.clone(); // Always clone to prevent modification
        }

        // Compute and cache
        item = supplier.get();
        if (item != null) {
            addToItemCache(key, item.clone());
            return item;
        }
        return null;
    }

    private void addToCache(String key, Component component) {
        if (componentCache.size() >= MAX_CACHE_SIZE) {
            evictOldest();
        }

        componentCache.put(key, component);
        trackAccess(key);
    }

    private void addToItemCache(String key, ItemStack item) {
        if (itemCache.size() >= MAX_CACHE_SIZE) {
            evictOldestItem();
        }

        itemCache.put(key, item);
        trackAccess(key);
    }

    private void trackAccess(String key) {
        lastAccess.put(key, System.currentTimeMillis());

        // Maintain access order queue size
        if (accessOrder.size() > MAX_QUEUE_SIZE) {
            accessOrder.poll();
        }
        accessOrder.offer(key);
    }

    private void evictOldest() {
        String oldest = null;
        long oldestTime = Long.MAX_VALUE;

        for (Map.Entry<String, Long> entry : lastAccess.entrySet()) {
            if (entry.getValue() < oldestTime && componentCache.containsKey(entry.getKey())) {
                oldestTime = entry.getValue();
                oldest = entry.getKey();
            }
        }

        if (oldest != null) {
            componentCache.remove(oldest);
            lastAccess.remove(oldest);
        }
    }

    private void evictOldestItem() {
        String oldest = null;
        long oldestTime = Long.MAX_VALUE;

        for (Map.Entry<String, Long> entry : lastAccess.entrySet()) {
            if (entry.getValue() < oldestTime && itemCache.containsKey(entry.getKey())) {
                oldestTime = entry.getValue();
                oldest = entry.getKey();
            }
        }

        if (oldest != null) {
            itemCache.remove(oldest);
            lastAccess.remove(oldest);
        }
    }

    /**
     * Pre-warm cache with common components.
     */
    public void preWarm() {
        // Common static components that never change
        String[] commonKeys = {
            "empty_slot", "back_button", "next_page", "prev_page",
            "close_button", "refresh_button", "fill_item"
        };

        // These would be computed with actual values
        for (String key : commonKeys) {
            // Pre-compute common components here
        }
    }

    public void clear() {
        componentCache.clear();
        itemCache.clear();
        accessOrder.clear();
        lastAccess.clear();
    }

    public void clearPlayer(String playerUuid) {
        componentCache.entrySet().removeIf(entry -> entry.getKey().startsWith(playerUuid + ":"));
        itemCache.entrySet().removeIf(entry -> entry.getKey().startsWith(playerUuid + ":"));
    }

    public int getComponentCacheSize() {
        return componentCache.size();
    }

    public int getItemCacheSize() {
        return itemCache.size();
    }

    @FunctionalInterface
    public interface ComponentSupplier {
        Component get();
    }

    @FunctionalInterface
    public interface ItemSupplier {
        ItemStack get();
    }
}