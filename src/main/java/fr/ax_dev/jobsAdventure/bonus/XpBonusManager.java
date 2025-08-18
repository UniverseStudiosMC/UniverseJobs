package fr.ax_dev.jobsAdventure.bonus;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.compatibility.FoliaCompatibilityManager;
import fr.ax_dev.jobsAdventure.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages temporary XP bonuses for players.
 */
public class XpBonusManager {
    
    private final JobsAdventure plugin;
    private final FoliaCompatibilityManager foliaManager;
    private final Map<UUID, List<XpBonus>> playerBonuses = new ConcurrentHashMap<>();
    private boolean cleanupRunning = false;
    
    /**
     * Create a new XP bonus manager.
     * 
     * @param plugin The plugin instance
     */
    public XpBonusManager(JobsAdventure plugin) {
        this.plugin = plugin;
        this.foliaManager = plugin.getFoliaManager();
        startCleanupTask();
    }
    
    /**
     * Add a bonus to a player.
     * 
     * @param bonus The XP bonus
     */
    public void addBonus(XpBonus bonus) {
        playerBonuses.computeIfAbsent(bonus.getPlayerId(), k -> new ArrayList<>()).add(bonus);
        
        // Notify the player
        Player player = Bukkit.getPlayer(bonus.getPlayerId());
        if (player != null && player.isOnline()) {
            notifyPlayer(player, bonus, true);
        }
    }
    
    /**
     * Add a global bonus to all online players.
     * 
     * @param multiplier The XP multiplier
     * @param duration Duration in seconds
     * @param reason Reason for the bonus
     * @param grantedBy Who granted the bonus
     * @return Number of players affected
     */
    public int addGlobalBonus(double multiplier, long duration, String reason, String grantedBy) {
        int count = 0;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            XpBonus bonus = XpBonus.createGlobalBonus(player.getUniqueId(), multiplier, duration, reason, grantedBy);
            addBonus(bonus);
            count++;
        }
        
        // Broadcast to all players
        String message = plugin.getConfig().getString("messages.global-bonus-announced", 
                "&e&lBONUS XP! &6All players received &e{bonus}% &6bonus XP for &e{duration}&6!")
                .replace("{bonus}", String.valueOf((int)((multiplier - 1) * 100)))
                .replace("{duration}", formatDuration(duration))
                .replace("{reason}", reason);
        
        Bukkit.broadcastMessage(MessageUtils.parseMessage(message).toString());
        
        return count;
    }
    
    /**
     * Add a job-specific bonus to a player.
     * 
     * @param playerId The player UUID
     * @param jobId The job ID
     * @param multiplier The XP multiplier
     * @param duration Duration in seconds
     * @param reason Reason for the bonus
     * @param grantedBy Who granted the bonus
     */
    public void addJobBonus(UUID playerId, String jobId, double multiplier, long duration, String reason, String grantedBy) {
        XpBonus bonus = XpBonus.createJobBonus(playerId, jobId, multiplier, duration, reason, grantedBy);
        addBonus(bonus);
    }
    
    /**
     * Add a global bonus to a specific player.
     * 
     * @param playerId The player UUID
     * @param multiplier The XP multiplier
     * @param duration Duration in seconds
     * @param reason Reason for the bonus
     * @param grantedBy Who granted the bonus
     */
    public void addPlayerBonus(UUID playerId, double multiplier, long duration, String reason, String grantedBy) {
        XpBonus bonus = XpBonus.createGlobalBonus(playerId, multiplier, duration, reason, grantedBy);
        addBonus(bonus);
    }
    
    /**
     * Get the total XP multiplier for a player and job.
     * 
     * @param playerId The player UUID
     * @param jobId The job ID
     * @return The total multiplier
     */
    public double getTotalMultiplier(UUID playerId, String jobId) {
        List<XpBonus> bonuses = playerBonuses.get(playerId);
        if (bonuses == null || bonuses.isEmpty()) {
            return 1.0;
        }
        
        double totalMultiplier = 1.0;
        
        for (XpBonus bonus : bonuses) {
            if (bonus.isActive() && bonus.appliesTo(jobId)) {
                // Stack bonuses multiplicatively
                totalMultiplier *= bonus.getMultiplier();
            }
        }
        
        return totalMultiplier;
    }
    
    /**
     * Get all active bonuses for a player.
     * 
     * @param playerId The player UUID
     * @return List of active bonuses
     */
    public List<XpBonus> getActiveBonuses(UUID playerId) {
        List<XpBonus> bonuses = playerBonuses.get(playerId);
        if (bonuses == null) {
            return new ArrayList<>();
        }
        
        return bonuses.stream()
                .filter(XpBonus::isActive)
                .collect(Collectors.toList());
    }
    
    /**
     * Get active bonuses for a specific job.
     * 
     * @param playerId The player UUID
     * @param jobId The job ID
     * @return List of active bonuses for the job
     */
    public List<XpBonus> getActiveBonuses(UUID playerId, String jobId) {
        return getActiveBonuses(playerId).stream()
                .filter(bonus -> bonus.appliesTo(jobId))
                .collect(Collectors.toList());
    }
    
    /**
     * Remove a specific bonus.
     * 
     * @param bonus The bonus to remove
     * @return true if removed
     */
    public boolean removeBonus(XpBonus bonus) {
        List<XpBonus> bonuses = playerBonuses.get(bonus.getPlayerId());
        if (bonuses != null) {
            boolean removed = bonuses.remove(bonus);
            
            if (removed) {
                Player player = Bukkit.getPlayer(bonus.getPlayerId());
                if (player != null && player.isOnline()) {
                    notifyPlayer(player, bonus, false);
                }
            }
            
            return removed;
        }
        return false;
    }
    
    /**
     * Remove all bonuses for a player.
     * 
     * @param playerId The player UUID
     * @return Number of bonuses removed
     */
    public int removeAllBonuses(UUID playerId) {
        List<XpBonus> bonuses = playerBonuses.remove(playerId);
        return bonuses != null ? bonuses.size() : 0;
    }
    
    /**
     * Clean up expired bonuses.
     */
    public void cleanupExpiredBonuses() {
        int removedCount = 0;
        
        for (Map.Entry<UUID, List<XpBonus>> entry : playerBonuses.entrySet()) {
            List<XpBonus> bonuses = entry.getValue();
            Iterator<XpBonus> iterator = bonuses.iterator();
            
            while (iterator.hasNext()) {
                XpBonus bonus = iterator.next();
                if (!bonus.isActive()) {
                    iterator.remove();
                    removedCount++;
                    
                    // Notify player if online
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        notifyPlayer(player, bonus, false);
                    }
                }
            }
            
            // Remove empty lists
            if (bonuses.isEmpty()) {
                playerBonuses.remove(entry.getKey());
            }
        }
        
        if (removedCount > 0) {
            plugin.getLogger().info("Cleaned up " + removedCount + " expired XP bonuses");
        }
    }
    
    /**
     * Start the cleanup task.
     */
    private void startCleanupTask() {
        cleanupRunning = true;
        
        // Run every 30 seconds asynchronously
        foliaManager.runTimerAsync(() -> {
            if (cleanupRunning) {
                cleanupExpiredBonuses();
            }
        }, 600L, 600L);
    }
    
    /**
     * Stop the cleanup task.
     */
    public void shutdown() {
        cleanupRunning = false;
    }
    
    /**
     * Notify a player about bonus changes.
     * 
     * @param player The player
     * @param bonus The bonus
     * @param added true if added, false if expired/removed
     */
    private void notifyPlayer(Player player, XpBonus bonus, boolean added) {
        String messageKey = added ? "messages.bonus-added" : "messages.bonus-expired";
        String defaultMessage = added ? 
                "&a&lXP BOOST! &e+{bonus}% XP &afor &e{duration} &a({reason})" :
                "&c&lXP BOOST EXPIRED! &7{bonus}% bonus ended ({reason})";
        
        String message = plugin.getConfig().getString(messageKey, defaultMessage)
                .replace("{bonus}", String.valueOf(bonus.getBonusPercentage()))
                .replace("{duration}", bonus.getRemainingTimeFormatted())
                .replace("{reason}", bonus.getReason())
                .replace("{job}", bonus.getJobId() != null ? bonus.getJobId() : "All Jobs");
        
        MessageUtils.sendMessage(player, message);
        
        // Play sound
        if (added) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        } else {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
        }
    }
    
    /**
     * Format duration in a human-readable way.
     * 
     * @param seconds Duration in seconds
     * @return Formatted duration
     */
    public static String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + "m" + (remainingSeconds > 0 ? " " + remainingSeconds + "s" : "");
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h" + (minutes > 0 ? " " + minutes + "m" : "");
        }
    }
    
    /**
     * Get statistics about active bonuses.
     * 
     * @return Map with statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalPlayers = playerBonuses.size();
        int totalBonuses = playerBonuses.values().stream()
                .mapToInt(List::size)
                .sum();
        int activeBonuses = playerBonuses.values().stream()
                .flatMap(List::stream)
                .mapToInt(bonus -> bonus.isActive() ? 1 : 0)
                .sum();
        
        stats.put("total_players", totalPlayers);
        stats.put("total_bonuses", totalBonuses);
        stats.put("active_bonuses", activeBonuses);
        
        return stats;
    }
}