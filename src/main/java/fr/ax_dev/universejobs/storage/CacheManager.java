package fr.ax_dev.universejobs.storage;

import fr.ax_dev.universejobs.job.PlayerJobData;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * High-performance LRU cache manager for player data.
 * Thread-safe implementation with configurable size limits.
 */
public class CacheManager {
    
    private final int maxSize;
    private final long maxMemoryMB;
    private final Map<UUID, CacheEntry> cache;
    private final Map<UUID, CacheEntry> accessOrder;
    private final ReentrantReadWriteLock lock;
    
    // Performance metrics
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);
    
    private static class CacheEntry {
        final PlayerJobData data;
        final long timestamp;
        final long accessTime;
        
        CacheEntry(PlayerJobData data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.accessTime = System.currentTimeMillis();
        }
        
        CacheEntry updateAccess() {
            return new CacheEntry(this.data) {
            };
        }
    }
    
    /**
     * Create a new CacheManager.
     * 
     * @param config Configuration containing cache settings
     */
    public CacheManager(FileConfiguration config) {
        this.maxSize = config.getInt("cache.max-entries", 1000);
        this.maxMemoryMB = config.getLong("cache.max-memory-mb", 256);
        this.cache = new ConcurrentHashMap<>(Math.min(maxSize, 1024));
        this.accessOrder = new LinkedHashMap<UUID, CacheEntry>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, CacheEntry> eldest) {
                return size() > maxSize;
            }
        };
        this.lock = new ReentrantReadWriteLock();
    }
    
    /**
     * Get player data from cache.
     * 
     * @param playerId Player UUID
     * @return PlayerJobData if cached, null otherwise
     */
    public PlayerJobData get(UUID playerId) {
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(playerId);
            if (entry != null) {
                hits.incrementAndGet();
                // Update access order
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    accessOrder.put(playerId, entry.updateAccess());
                    lock.readLock().lock();
                } finally {
                    lock.writeLock().unlock();
                }
                return entry.data;
            } else {
                misses.incrementAndGet();
                return null;
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Put player data in cache.
     * 
     * @param playerId Player UUID
     * @param data Player job data
     */
    public void put(UUID playerId, PlayerJobData data) {
        lock.writeLock().lock();
        try {
            // Check memory usage before adding
            if (shouldEvictForMemory()) {
                evictLeastRecentlyUsed();
            }
            
            CacheEntry entry = new CacheEntry(data);
            cache.put(playerId, entry);
            accessOrder.put(playerId, entry);
            
            // Auto-evict if over size limit
            while (cache.size() > maxSize) {
                evictLeastRecentlyUsed();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Remove player data from cache.
     * 
     * @param playerId Player UUID
     */
    public void remove(UUID playerId) {
        lock.writeLock().lock();
        try {
            cache.remove(playerId);
            accessOrder.remove(playerId);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Clear all cached data.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
            accessOrder.clear();
            hits.set(0);
            misses.set(0);
            evictions.set(0);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Check if player data is cached.
     * 
     * @param playerId Player UUID
     * @return true if cached
     */
    public boolean contains(UUID playerId) {
        lock.readLock().lock();
        try {
            return cache.containsKey(playerId);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get all cached player IDs.
     * 
     * @return Set of cached player UUIDs
     */
    public Set<UUID> getCachedPlayers() {
        lock.readLock().lock();
        try {
            return new HashSet<>(cache.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get cache statistics.
     * 
     * @return Map containing cache statistics
     */
    public Map<String, Object> getStats() {
        lock.readLock().lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("size", cache.size());
            stats.put("max_size", maxSize);
            stats.put("hits", hits.get());
            stats.put("misses", misses.get());
            stats.put("evictions", evictions.get());
            stats.put("hit_rate", getHitRate());
            stats.put("memory_usage_mb", getEstimatedMemoryUsageMB());
            stats.put("max_memory_mb", maxMemoryMB);
            return stats;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get cache hit rate.
     * 
     * @return Hit rate as percentage (0-100)
     */
    public double getHitRate() {
        long totalRequests = hits.get() + misses.get();
        return totalRequests > 0 ? (hits.get() * 100.0) / totalRequests : 0.0;
    }
    
    /**
     * Evict least recently used entries.
     */
    private void evictLeastRecentlyUsed() {
        if (accessOrder.isEmpty()) return;
        
        Iterator<Map.Entry<UUID, CacheEntry>> iterator = accessOrder.entrySet().iterator();
        if (iterator.hasNext()) {
            UUID oldestKey = iterator.next().getKey();
            iterator.remove();
            cache.remove(oldestKey);
            evictions.incrementAndGet();
        }
    }
    
    /**
     * Check if we should evict entries due to memory usage.
     * 
     * @return true if memory usage is too high
     */
    private boolean shouldEvictForMemory() {
        return getEstimatedMemoryUsageMB() > maxMemoryMB;
    }
    
    /**
     * Estimate memory usage in MB.
     * 
     * @return Estimated memory usage in MB
     */
    private long getEstimatedMemoryUsageMB() {
        // Rough estimation: each PlayerJobData ~2KB + overhead
        return (cache.size() * 2048L) / (1024 * 1024);
    }
    
    /**
     * Preload multiple players into cache.
     * 
     * @param playerDataMap Map of player data to cache
     */
    public void preload(Map<UUID, PlayerJobData> playerDataMap) {
        lock.writeLock().lock();
        try {
            for (Map.Entry<UUID, PlayerJobData> entry : playerDataMap.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Cleanup expired entries.
     * 
     * @param maxAgeMinutes Maximum age in minutes
     */
    public void cleanupExpired(int maxAgeMinutes) {
        lock.writeLock().lock();
        try {
            long cutoffTime = System.currentTimeMillis() - (maxAgeMinutes * 60 * 1000L);
            Set<UUID> toRemove = new HashSet<>();
            
            for (Map.Entry<UUID, CacheEntry> entry : cache.entrySet()) {
                if (entry.getValue().accessTime < cutoffTime) {
                    toRemove.add(entry.getKey());
                }
            }
            
            for (UUID playerId : toRemove) {
                cache.remove(playerId);
                accessOrder.remove(playerId);
                evictions.incrementAndGet();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}