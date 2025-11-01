package fr.ax_dev.universejobs.bonus;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.compatibility.FoliaCompatibilityManager;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Base class for bonus managers to eliminate code duplication.
 */
public abstract class BaseBonusManager<T extends BaseBonus> implements BonusManager<T> {
    
    protected final UniverseJobs plugin;
    protected final FoliaCompatibilityManager foliaManager;
    protected final Map<UUID, List<T>> playerBonuses = new ConcurrentHashMap<>();
    
    // Multiplier cache for performance (5 second cache)
    private final Map<String, Double> multiplierCache = new ConcurrentHashMap<>();
    private final Map<String, Long> multiplierCacheTimestamps = new ConcurrentHashMap<>();
    private static final long MULTIPLIER_CACHE_DURATION = 5000L; // 5 seconds
    protected final Map<String, T> boostIdMap = new ConcurrentHashMap<>();
    protected boolean cleanupRunning = false;
    protected int nextBoostCounter = 1;
    
    protected BaseBonusManager(UniverseJobs plugin) {
        this.plugin = plugin;
        this.foliaManager = plugin.getFoliaManager();
        startCleanupTask();
    }
    
    /**
     * Create a new bonus instance (factory method).
     */
    protected abstract T createBonus(UUID playerId, String jobId, double multiplier, long duration, String reason, String grantedBy, String boostId, boolean isGlobal, String actionType, String actionId);

    /**
     * Get the bonus type name for logging.
     */
    protected abstract String getBonusTypeName();

    /**
     * Get the permission type used for multipliers (e.g., "money" or "exp").
     */
    protected abstract String getMultiplierPermissionType();
    
    /**
     * Generate a unique boost ID.
     */
    protected String generateBoostId() {
        String prefix = getBonusTypeName().toLowerCase();
        if (prefix.endsWith("bonus")) {
            prefix = prefix.substring(0, prefix.length() - 5);
        }
        return prefix + nextBoostCounter++;
    }
    
    @Override
    public int addGlobalBonus(double multiplier, long duration, String reason, String grantedBy) {
        int count = 0;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            String boostId = generateBoostId();
            T bonus = createBonus(player.getUniqueId(), null, multiplier, duration, reason, grantedBy, boostId, true, null, null);
            addBonus(bonus);
            count++;
        }
        
        if (count > 0) {
            String message = "Global " + getBonusTypeName() + " bonus started: " + multiplier + "x for " + formatDuration(duration);
            for (Player player : Bukkit.getOnlinePlayers()) {
                MessageUtils.sendMessage(player, message);
            }
        }
        
        return count;
    }
    
    @Override
    public void addPlayerBonus(UUID playerId, double multiplier, long duration, String reason, String grantedBy) {
        String boostId = generateBoostId();
        T bonus = createBonus(playerId, null, multiplier, duration, reason, grantedBy, boostId, false, null, null);
        addBonus(bonus);
    }
    
    @Override
    public void addJobBonus(UUID playerId, String jobId, double multiplier, long duration, String reason, String grantedBy) {
        String boostId = generateBoostId();
        T bonus = createBonus(playerId, jobId, multiplier, duration, reason, grantedBy, boostId, false, null, null);
        addBonus(bonus);
    }
    
    public void addActionBonus(UUID playerId, String jobId, String actionType, String actionId, double multiplier, long duration, String reason, String grantedBy, boolean isGlobal) {
        String boostId = generateBoostId();
        T bonus = createBonus(playerId, jobId, multiplier, duration, reason, grantedBy, boostId, isGlobal, actionType, actionId);
        addBonus(bonus);
    }
    
    protected void addBonus(T bonus) {
        playerBonuses.computeIfAbsent(bonus.getPlayerId(), k -> new ArrayList<>()).add(bonus);
        boostIdMap.put(bonus.getBoostId(), bonus);
        
        Player player = Bukkit.getPlayer(bonus.getPlayerId());
        if (player != null) {
            String message = getBonusTypeName() + " boost [" + bonus.getBoostId() + "] received: " + bonus.getMultiplier() + "x for " + bonus.getRemainingTimeFormatted();
            if (bonus.getJobId() != null) {
                message += " (Job: " + bonus.getJobId() + ")";
            }
            MessageUtils.sendMessage(player, message);
        }
    }
    
    @Override
    public List<T> getActiveBonuses(UUID playerId) {
        List<T> bonuses = playerBonuses.get(playerId);
        if (bonuses == null) {
            return new ArrayList<>();
        }
        
        return bonuses.stream()
                .filter(BaseBonus::isActive)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<T> getActiveBonuses(UUID playerId, String jobId) {
        return getActiveBonuses(playerId).stream()
                .filter(bonus -> bonus.appliesTo(jobId))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean removeBonus(T bonus) {
        List<T> bonuses = playerBonuses.get(bonus.getPlayerId());
        if (bonuses != null) {
            boolean removed = bonuses.remove(bonus);
            if (removed) {
                boostIdMap.remove(bonus.getBoostId());
                if (bonuses.isEmpty()) {
                    playerBonuses.remove(bonus.getPlayerId());
                }
            }
            return removed;
        }
        return false;
    }
    
    public boolean removeBoostById(String boostId) {
        T bonus = boostIdMap.get(boostId);
        if (bonus != null) {
            return removeBonus(bonus);
        }
        return false;
    }
    
    public T getBoostById(String boostId) {
        return boostIdMap.get(boostId);
    }
    
    public List<String> getAllActiveBoostIds() {
        return boostIdMap.values().stream()
                .filter(BaseBonus::isActive)
                .map(BaseBonus::getBoostId)
                .collect(Collectors.toList());
    }
    
    @Override
    public int removeAllBonuses(UUID playerId) {
        List<T> bonuses = playerBonuses.remove(playerId);
        if (bonuses != null) {
            for (T bonus : bonuses) {
                boostIdMap.remove(bonus.getBoostId());
            }
            return bonuses.size();
        }
        return 0;
    }
    
    @Override
    public void cleanupExpiredBonuses() {
        if (cleanupRunning) return;
        cleanupRunning = true;
        
        try {
            int cleaned = 0;
            Iterator<Map.Entry<UUID, List<T>>> iterator = playerBonuses.entrySet().iterator();
            
            while (iterator.hasNext()) {
                Map.Entry<UUID, List<T>> entry = iterator.next();
                List<T> bonuses = entry.getValue();
                
                Iterator<T> bonusIterator = bonuses.iterator();
                while (bonusIterator.hasNext()) {
                    T bonus = bonusIterator.next();
                    if (!bonus.isActive()) {
                        bonusIterator.remove();
                        boostIdMap.remove(bonus.getBoostId());
                        cleaned++;
                    }
                }
                
                if (bonuses.isEmpty()) {
                    iterator.remove();
                }
            }
            
            if (cleaned > 0) {
                plugin.getLogger().info("Cleaned up " + cleaned + " expired " + getBonusTypeName().toLowerCase() + " bonuses");
            }
        } finally {
            cleanupRunning = false;
        }
    }
    
    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPlayers", playerBonuses.size());
        stats.put("totalBonuses", playerBonuses.values().stream().mapToInt(List::size).sum());
        stats.put("activeBonuses", playerBonuses.values().stream()
                .flatMap(List::stream)
                .mapToInt(bonus -> bonus.isActive() ? 1 : 0)
                .sum());
        return stats;
    }
    
    public double getTotalMultiplier(UUID playerId, String jobId) {
        // Check cache first
        String cacheKey = playerId + ":" + jobId;
        long currentTime = System.currentTimeMillis();
        
        Long cacheTime = multiplierCacheTimestamps.get(cacheKey);
        if (cacheTime != null && (currentTime - cacheTime) < MULTIPLIER_CACHE_DURATION) {
            Double cachedMultiplier = multiplierCache.get(cacheKey);
            if (cachedMultiplier != null) {
                return cachedMultiplier;
            }
        }
        
        // Calculate multiplier
        List<T> activeBonuses = getActiveBonuses(playerId, jobId);

        // Calculate base multiplier from active bonuses
        double baseMultiplier;
        if (activeBonuses.isEmpty()) {
            baseMultiplier = 1.0;
        } else {
            // Récupérer le mode de calcul depuis la configuration
            fr.ax_dev.universejobs.config.ConfigManager.BoostCalculationMode mode =
                plugin.getConfigManager().getBoostCalculationMode();

            switch (mode) {
                case ADDITIVE:
                    // Mode 1: Additionner les multiplicateurs (2.5x + 2.5x = 5.0x)
                    double sum = 0.0;
                    for (T bonus : activeBonuses) {
                        sum += (bonus.getMultiplier() - 1.0); // Soustraire 1 pour éviter de compter la base plusieurs fois
                    }
                    baseMultiplier = 1.0 + sum;
                    break;

                case MULTIPLICATIVE:
                    // Mode 2: Multiplier les multiplicateurs (2.5x * 2.5x = 6.25x)
                    double product = 1.0;
                    for (T bonus : activeBonuses) {
                        product *= bonus.getMultiplier();
                    }
                    baseMultiplier = product;
                    break;

                case HIGHEST:
                    // Mode 3: Utiliser uniquement le multiplicateur le plus élevé
                    double highest = 1.0;
                    for (T bonus : activeBonuses) {
                        if (bonus.getMultiplier() > highest) {
                            highest = bonus.getMultiplier();
                        }
                    }
                    baseMultiplier = highest;
                    break;

                default:
                    // Par défaut, utiliser le mode multiplicatif
                    double defaultProduct = 1.0;
                    for (T bonus : activeBonuses) {
                        defaultProduct *= bonus.getMultiplier();
                    }
                    baseMultiplier = defaultProduct;
                    break;
            }
        }

        // Apply permission-based bonus multiplier
        double permissionMultiplier = 1.0;
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            String type = getMultiplierPermissionType();
            
            // Check if player has wildcard permission
            boolean hasWildcard = player.hasPermission("*") || 
                                 player.hasPermission("universejobs.*") || 
                                 player.hasPermission("universejobs.multiplier.*") ||
                                 player.hasPermission("universejobs.multiplier." + type + ".*");
            
            // If player has wildcard, only use explicitly defined permissions
            if (hasWildcard) {
                // Check effective permissions to find explicitly set ones
                for (org.bukkit.permissions.PermissionAttachmentInfo info : player.getEffectivePermissions()) {
                    String perm = info.getPermission();
                    if (perm.startsWith("universejobs.multiplier." + type + ".") && !perm.endsWith("*")) {
                        try {
                            String numberStr = perm.substring(("universejobs.multiplier." + type + ".").length());
                            int value = Integer.parseInt(numberStr);
                            if (value >= 1 && value <= 100 && value > permissionMultiplier) {
                                permissionMultiplier = value;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            } else {
                // Normal permission check for players without wildcard
                for (int i = 100; i >= 1; i--) {
                    String permission = "universejobs.multiplier." + type + "." + i;
                    if (player.hasPermission(permission)) {
                        permissionMultiplier = i;
                        break;
                    }
                }
            }
        }

        double finalMultiplier = baseMultiplier * permissionMultiplier;
        
        // Cache the result
        multiplierCache.put(cacheKey, finalMultiplier);
        multiplierCacheTimestamps.put(cacheKey, currentTime);
        
        return finalMultiplier;
    }
    
    private void startCleanupTask() {
        plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, task -> cleanupExpiredBonuses(), 60L, 60L, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    /**
     * Shutdown the bonus manager.
     */
    public void shutdown() {
        cleanupRunning = false;
    }
    
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
    
    public Set<UUID> getPlayersWithActiveBonuses() {
        return playerBonuses.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(BaseBonus::isActive))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
    
    public Set<String> getJobsWithActiveBonuses(UUID playerId) {
        List<T> bonuses = getActiveBonuses(playerId);
        return bonuses.stream()
                .map(BaseBonus::getJobId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}