package fr.ax_dev.universejobs.cache;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.PlayerJobData;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache ultra-rapide des données joueur.
 * Lookup instantané sans attente de base de données.
 */
public class PlayerJobCache {
    
    private final UniverseJobs plugin;
    
    // Cache principal des jobs par joueur
    private final Map<UUID, Set<String>> playerJobsCache = new ConcurrentHashMap<>();
    
    // Cache des niveaux par joueur/job (évite les calculs XP)
    private final Map<UUID, Map<String, Integer>> playerLevelsCache = new ConcurrentHashMap<>();
    
    // Cache des XP par joueur/job  
    private final Map<UUID, Map<String, Double>> playerXpCache = new ConcurrentHashMap<>();
    
    // Cache des permissions par joueur
    private final Map<UUID, Map<String, Boolean>> playerPermissionsCache = new ConcurrentHashMap<>();
    
    // Cache des multipliers par joueur
    private final Map<UUID, Double> playerMultipliersCache = new ConcurrentHashMap<>();
    
    
    // Statistiques de performance
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    
    public PlayerJobCache(UniverseJobs plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Précharge les données des joueurs connectés.
     */
    public void preloadOnlinePlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            preloadPlayer(player.getUniqueId());
        }
    }
    
    /**
     * Précharge les données d'un joueur de manière asynchrone.
     */
    public CompletableFuture<Void> preloadPlayer(UUID playerUuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                PlayerJobData data = plugin.getJobManager().getPlayerData(playerUuid);
                
                // Cache jobs
                playerJobsCache.put(playerUuid, new HashSet<>(data.getJobs()));
                
                // Cache levels et XP
                Map<String, Integer> levels = new ConcurrentHashMap<>();
                Map<String, Double> xp = new ConcurrentHashMap<>();
                
                for (String jobId : data.getJobs()) {
                    levels.put(jobId, data.getLevel(jobId));
                    xp.put(jobId, data.getXp(jobId));
                }
                
                playerLevelsCache.put(playerUuid, levels);
                playerXpCache.put(playerUuid, xp);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to preload player data for " + playerUuid);
            }
        });
    }
    
    /**
     * Lookup instantané des jobs d'un joueur (0ms).
     */
    public Set<String> getPlayerJobs(UUID playerUuid) {
        Set<String> jobs = playerJobsCache.get(playerUuid);
        if (jobs != null) {
            cacheHits.incrementAndGet();
            return jobs;
        }
        
        cacheMisses.incrementAndGet();
        // Fallback async - ne bloque pas
        preloadPlayer(playerUuid);
        return Collections.emptySet();
    }
    
    
    /**
     * Cache des permissions avec TTL.
     */
    public boolean hasPermissionCached(UUID playerUuid, String permission) {
        Map<String, Boolean> perms = playerPermissionsCache.get(playerUuid);
        if (perms != null && perms.containsKey(permission)) {
            cacheHits.incrementAndGet();
            return perms.get(permission);
        }
        
        cacheMisses.incrementAndGet();
        // Fallback - check réel et cache le résultat
        Player player = plugin.getServer().getPlayer(playerUuid);
        if (player != null) {
            boolean result = player.hasPermission(permission);
            playerPermissionsCache.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                .put(permission, result);
            return result;
        }
        
        return false;
    }
    
    /**
     * Cache du multiplier d'un joueur.
     */
    public double getPlayerMultiplier(UUID playerUuid) {
        Double multiplier = playerMultipliersCache.get(playerUuid);
        if (multiplier != null) {
            cacheHits.incrementAndGet();
            return multiplier;
        }
        
        cacheMisses.incrementAndGet();
        // Calcul et cache
        Player player = plugin.getServer().getPlayer(playerUuid);
        if (player != null) {
            double result = calculateMultiplier(player);
            playerMultipliersCache.put(playerUuid, result);
            return result;
        }
        
        return 1.0;
    }
    
    /**
     * Get niveau avec cache instantané.
     */
    public int getPlayerLevel(UUID playerUuid, String jobId) {
        Map<String, Integer> levels = playerLevelsCache.get(playerUuid);
        if (levels != null && levels.containsKey(jobId)) {
            cacheHits.incrementAndGet();
            return levels.get(jobId);
        }
        
        cacheMisses.incrementAndGet();
        return 0; // Ou preload async
    }
    
    /**
     * Get XP avec cache instantané.
     */
    public double getPlayerXp(UUID playerUuid, String jobId) {
        Map<String, Double> xp = playerXpCache.get(playerUuid);
        if (xp != null && xp.containsKey(jobId)) {
            cacheHits.incrementAndGet();
            return xp.get(jobId);
        }
        
        cacheMisses.incrementAndGet();
        return 0.0; // Ou preload async
    }
    
    /**
     * Mise à jour du cache après gain d'XP.
     */
    public void updatePlayerXp(UUID playerUuid, String jobId, double newXp, int newLevel) {
        // Update XP cache
        playerXpCache.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
            .put(jobId, newXp);
            
        // Update level cache
        playerLevelsCache.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
            .put(jobId, newLevel);
    }
    
    /**
     * Ajout d'un job dans le cache.
     */
    public void addPlayerJob(UUID playerUuid, String jobId) {
        playerJobsCache.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet())
            .add(jobId);
            
        // Initialize avec 0 XP/level
        updatePlayerXp(playerUuid, jobId, 0.0, 0);
    }
    
    /**
     * Retrait d'un job du cache.
     */
    public void removePlayerJob(UUID playerUuid, String jobId) {
        Set<String> jobs = playerJobsCache.get(playerUuid);
        if (jobs != null) {
            jobs.remove(jobId);
        }
        
        // Cleanup related caches
        Map<String, Integer> levels = playerLevelsCache.get(playerUuid);
        if (levels != null) levels.remove(jobId);
        
        Map<String, Double> xp = playerXpCache.get(playerUuid);
        if (xp != null) xp.remove(jobId);
    }
    
    /**
     * Nettoyage complet d'un joueur (déconnexion).
     */
    public void cleanupPlayer(UUID playerUuid) {
        playerJobsCache.remove(playerUuid);
        playerLevelsCache.remove(playerUuid);
        playerXpCache.remove(playerUuid);
        playerPermissionsCache.remove(playerUuid);
        playerMultipliersCache.remove(playerUuid);
        
    }
    
    /**
     * Nettoyage des caches expirés.
     */
    public void performCleanup() {
        playerPermissionsCache.entrySet().removeIf(entry -> {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            return player == null || !player.isOnline();
        });
        
        // Nettoie les multipliers
        playerMultipliersCache.entrySet().removeIf(entry -> {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            return player == null || !player.isOnline();
        });
    }
    
    /**
     * Calcul du multiplier pour un joueur.
     */
    private double calculateMultiplier(Player player) {
        if (player.isOp() || player.hasPermission("*")) {
            return 1.0; // Évite les bonus OP
        }
        
        // Check multipliers 10 -> 1 for money and exp
        for (int i = 10; i >= 1; i--) {
            if (player.hasPermission("universejobs.multiplier.money." + i) ||
                player.hasPermission("universejobs.multiplier.exp." + i)) {
                return i;
            }
        }
        
        return 1.0;
    }
    
    /**
     * Statistiques de performance du cache.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cache_hits", cacheHits.get());
        stats.put("cache_misses", cacheMisses.get());
        stats.put("cached_players", playerJobsCache.size());
        stats.put("cached_levels", playerLevelsCache.size());
        stats.put("cached_permissions", playerPermissionsCache.size());
        
        long totalRequests = cacheHits.get() + cacheMisses.get();
        if (totalRequests > 0) {
            stats.put("hit_rate", (double) cacheHits.get() / totalRequests * 100);
        }
        
        return stats;
    }
    
    
    /**
     * Reset des statistiques.
     */
    public void resetStats() {
        cacheHits.set(0);
        cacheMisses.set(0);
    }
}