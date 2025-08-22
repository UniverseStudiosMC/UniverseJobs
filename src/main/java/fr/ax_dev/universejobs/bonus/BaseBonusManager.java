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
    protected boolean cleanupRunning = false;
    
    protected BaseBonusManager(UniverseJobs plugin) {
        this.plugin = plugin;
        this.foliaManager = plugin.getFoliaManager();
        startCleanupTask();
    }
    
    /**
     * Create a new bonus instance (factory method).
     */
    protected abstract T createBonus(UUID playerId, String jobId, double multiplier, long duration, String reason, String grantedBy);
    
    /**
     * Get the bonus type name for logging.
     */
    protected abstract String getBonusTypeName();
    
    @Override
    public int addGlobalBonus(double multiplier, long duration, String reason, String grantedBy) {
        int count = 0;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            T bonus = createBonus(player.getUniqueId(), null, multiplier, duration, reason, grantedBy);
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
        T bonus = createBonus(playerId, null, multiplier, duration, reason, grantedBy);
        addBonus(bonus);
    }
    
    @Override
    public void addJobBonus(UUID playerId, String jobId, double multiplier, long duration, String reason, String grantedBy) {
        T bonus = createBonus(playerId, jobId, multiplier, duration, reason, grantedBy);
        addBonus(bonus);
    }
    
    protected void addBonus(T bonus) {
        playerBonuses.computeIfAbsent(bonus.getPlayerId(), k -> new ArrayList<>()).add(bonus);
        
        Player player = Bukkit.getPlayer(bonus.getPlayerId());
        if (player != null) {
            String message = getBonusTypeName() + " bonus received: " + bonus.getMultiplier() + "x for " + bonus.getRemainingTimeFormatted();
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
            if (removed && bonuses.isEmpty()) {
                playerBonuses.remove(bonus.getPlayerId());
            }
            return removed;
        }
        return false;
    }
    
    @Override
    public int removeAllBonuses(UUID playerId) {
        List<T> bonuses = playerBonuses.remove(playerId);
        return bonuses != null ? bonuses.size() : 0;
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
                
                int sizeBefore = bonuses.size();
                bonuses.removeIf(bonus -> !bonus.isActive());
                cleaned += sizeBefore - bonuses.size();
                
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
        List<T> activeBonuses = getActiveBonuses(playerId, jobId);
        
        double totalMultiplier = 1.0;
        for (T bonus : activeBonuses) {
            totalMultiplier *= bonus.getMultiplier();
        }
        
        return totalMultiplier;
    }
    
    private void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupExpiredBonuses, 1200L, 1200L);
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
}