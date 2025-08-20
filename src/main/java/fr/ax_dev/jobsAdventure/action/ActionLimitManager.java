package fr.ax_dev.jobsAdventure.action;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages action limits for players to prevent XP/money farming.
 * Supports per-action cooldowns and automatic restoration.
 */
public class ActionLimitManager {
    
    private final JobsAdventure plugin;
    
    // Map: Player UUID -> Job ID -> Action Target -> ActionLimitData
    private final Map<UUID, Map<String, Map<String, ActionLimitData>>> playerLimits = new ConcurrentHashMap<>();
    
    // Map: Job ID -> Action Target -> ActionLimit configuration
    private final Map<String, Map<String, ActionLimit>> actionLimits = new ConcurrentHashMap<>();
    
    public ActionLimitManager(JobsAdventure plugin) {
        this.plugin = plugin;
        startAutoRestoreTask();
    }
    
    /**
     * Set action limit configuration for a specific job and action target.
     * 
     * @param jobId The job ID
     * @param target The action target
     * @param limit The action limit configuration
     */
    public void setActionLimit(String jobId, String target, ActionLimit limit) {
        actionLimits.computeIfAbsent(jobId, k -> new ConcurrentHashMap<>()).put(target, limit);
    }
    
    /**
     * Check if a player can perform an action and consume from their limit.
     * 
     * @param player The player
     * @param jobId The job ID
     * @param target The action target
     * @param xpGain The XP that would be gained
     * @param moneyGain The money that would be gained
     * @return The modified gains (may be reduced or zero if limit reached)
     */
    public ActionGains checkAndConsumeLimit(Player player, String jobId, String target, double xpGain, double moneyGain) {
        ActionLimit limit = getActionLimit(jobId, target);
        if (limit == null) {
            return new ActionGains(xpGain, moneyGain); // No limits configured
        }
        
        UUID playerId = player.getUniqueId();
        ActionLimitData data = getOrCreateLimitData(playerId, jobId, target, limit);
        
        // Check if action is on cooldown
        long currentTime = System.currentTimeMillis();
        if (currentTime < data.getCooldownEndTime()) {
            return new ActionGains(0, 0); // Action on cooldown
        }
        
        // Check if action limit reached
        if (data.getCurrentActionsPerformed() >= limit.getMaxActionsPerPeriod()) {
            // Set cooldown and return no gains
            data.setCooldownEndTime(currentTime + (limit.getCooldownMinutes() * 60 * 1000L));
            return new ActionGains(0, 0);
        }
        
        // Consume one action
        data.consumeAction();
        
        // Apply blocking rules
        double finalXp = limit.isBlockExp() ? 0 : xpGain;
        double finalMoney = limit.isBlockMoney() ? 0 : moneyGain;
        
        // Check if we've reached the limit after this action
        if (data.getCurrentActionsPerformed() >= limit.getMaxActionsPerPeriod()) {
            data.setCooldownEndTime(currentTime + (limit.getCooldownMinutes() * 60 * 1000L));
        }
        
        return new ActionGains(finalXp, finalMoney);
    }
    
    /**
     * Get action limit configuration for a job and target.
     * 
     * @param jobId The job ID
     * @param target The action target
     * @return The action limit or null if none configured
     */
    private ActionLimit getActionLimit(String jobId, String target) {
        Map<String, ActionLimit> jobLimits = actionLimits.get(jobId);
        if (jobLimits == null) return null;
        
        // Check exact match first
        ActionLimit limit = jobLimits.get(target);
        if (limit != null) return limit;
        
        // Check wildcard matches
        for (Map.Entry<String, ActionLimit> entry : jobLimits.entrySet()) {
            String pattern = entry.getKey();
            if (matchesPattern(target, pattern)) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Check if target matches a pattern (supports wildcards).
     * 
     * @param target The target to check
     * @param pattern The pattern to match against
     * @return true if matches
     */
    private boolean matchesPattern(String target, String pattern) {
        if (pattern.equals("*")) return true;
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return target.toUpperCase().startsWith(prefix.toUpperCase());
        }
        if (pattern.startsWith("*")) {
            String suffix = pattern.substring(1);
            return target.toUpperCase().endsWith(suffix.toUpperCase());
        }
        return target.equalsIgnoreCase(pattern);
    }
    
    /**
     * Get or create limit data for a player.
     * 
     * @param playerId The player UUID
     * @param jobId The job ID
     * @param target The action target
     * @param limit The action limit configuration
     * @return The action limit data
     */
    private ActionLimitData getOrCreateLimitData(UUID playerId, String jobId, String target, ActionLimit limit) {
        return playerLimits
                .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(jobId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(target, k -> new ActionLimitData(limit));
    }
    
    /**
     * Manually restore a player's action limit for a specific target.
     * 
     * @param player The player
     * @param jobId The job ID (or "*" for all jobs)
     * @param target The action target (or "*" for all targets)
     * @return The number of actions restored
     */
    public int restorePlayerLimit(Player player, String jobId, String target) {
        UUID playerId = player.getUniqueId();
        Map<String, Map<String, ActionLimitData>> playerJobLimits = playerLimits.get(playerId);
        
        if (playerJobLimits == null) return 0;
        
        int restored = 0;
        
        if ("*".equals(jobId)) {
            // Restore all jobs
            for (Map<String, ActionLimitData> jobLimits : playerJobLimits.values()) {
                restored += restoreJobLimits(jobLimits, target);
            }
        } else {
            // Restore specific job
            Map<String, ActionLimitData> jobLimits = playerJobLimits.get(jobId);
            if (jobLimits != null) {
                restored = restoreJobLimits(jobLimits, target);
            }
        }
        
        return restored;
    }
    
    /**
     * Restore limits for a specific job's actions.
     * 
     * @param jobLimits The job's action limits
     * @param target The target to restore (or "*" for all)
     * @return The number of actions restored
     */
    private int restoreJobLimits(Map<String, ActionLimitData> jobLimits, String target) {
        int restored = 0;
        
        if ("*".equals(target)) {
            // Restore all targets
            for (ActionLimitData data : jobLimits.values()) {
                data.reset();
                restored++;
            }
        } else {
            // Restore specific target
            ActionLimitData data = jobLimits.get(target);
            if (data != null) {
                data.reset();
                restored = 1;
            }
        }
        
        return restored;
    }
    
    /**
     * Get player's current limit status for display.
     * 
     * @param player The player
     * @param jobId The job ID
     * @param target The action target
     * @return Status information or null if no limits
     */
    public ActionLimitStatus getPlayerLimitStatus(Player player, String jobId, String target) {
        ActionLimit limit = getActionLimit(jobId, target);
        if (limit == null) return null;
        
        UUID playerId = player.getUniqueId();
        ActionLimitData data = getOrCreateLimitData(playerId, jobId, target, limit);
        
        return new ActionLimitStatus(
            limit,
            data.getCurrentActionsPerformed(),
            data.getCooldownEndTime(),
            System.currentTimeMillis()
        );
    }
    
    /**
     * Start the automatic restore task based on configured schedule.
     */
    private void startAutoRestoreTask() {
        // Check for scheduled restore every minute
        new BukkitRunnable() {
            @Override
            public void run() {
                checkScheduledRestore();
            }
        }.runTaskTimer(plugin, 20 * 60, 20 * 60); // Every minute
    }
    
    /**
     * Check if it's time for scheduled restore and execute if needed.
     */
    private void checkScheduledRestore() {
        String scheduleTime = plugin.getConfig().getString("action-limits.auto-restore.time");
        if (scheduleTime == null || scheduleTime.isEmpty()) return;
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime scheduled = LocalDateTime.parse(scheduleTime + ":00", DateTimeFormatter.ofPattern("HH:mm:ss"));
            
            // Check if we're within 1 minute of the scheduled time
            if (Math.abs(now.getHour() - scheduled.getHour()) == 0 && 
                Math.abs(now.getMinute() - scheduled.getMinute()) == 0) {
                
                // Execute auto restore
                executeAutoRestore();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid auto-restore time format: " + scheduleTime);
        }
    }
    
    /**
     * Execute automatic restore for all players.
     */
    private void executeAutoRestore() {
        int totalRestored = 0;
        
        for (Map<String, Map<String, ActionLimitData>> playerJobLimits : playerLimits.values()) {
            for (Map<String, ActionLimitData> jobLimits : playerJobLimits.values()) {
                for (ActionLimitData data : jobLimits.values()) {
                    data.reset();
                    totalRestored++;
                }
            }
        }
        
        if (totalRestored > 0) {
            plugin.getLogger().info("Auto-restored " + totalRestored + " action limits");
        }
    }
    
    /**
     * Clear all stored limit data (useful for reload).
     */
    public void clearAllLimits() {
        playerLimits.clear();
        actionLimits.clear();
    }
    
    /**
     * Represents the result of an action limit check.
     */
    public static class ActionGains {
        private final double xp;
        private final double money;
        
        public ActionGains(double xp, double money) {
            this.xp = xp;
            this.money = money;
        }
        
        public double getXp() { return xp; }
        public double getMoney() { return money; }
        
        public boolean hasGains() {
            return xp > 0 || money > 0;
        }
    }
    
    /**
     * Configuration for action limits.
     */
    public static class ActionLimit {
        private final int maxActionsPerPeriod;
        private final int cooldownMinutes;
        private final boolean blockExp;
        private final boolean blockMoney;
        
        public ActionLimit(int maxActionsPerPeriod, int cooldownMinutes, boolean blockExp, boolean blockMoney) {
            this.maxActionsPerPeriod = maxActionsPerPeriod;
            this.cooldownMinutes = cooldownMinutes;
            this.blockExp = blockExp;
            this.blockMoney = blockMoney;
        }
        
        public int getMaxActionsPerPeriod() { return maxActionsPerPeriod; }
        public int getCooldownMinutes() { return cooldownMinutes; }
        public boolean isBlockExp() { return blockExp; }
        public boolean isBlockMoney() { return blockMoney; }
    }
    
    /**
     * Stores current limit data for a player's specific action.
     */
    private static class ActionLimitData {
        private final ActionLimit limit;
        private int currentActionsPerformed;
        private long cooldownEndTime;
        
        public ActionLimitData(ActionLimit limit) {
            this.limit = limit;
            this.currentActionsPerformed = 0;
            this.cooldownEndTime = 0;
        }
        
        public void consumeAction() {
            this.currentActionsPerformed++;
        }
        
        public void reset() {
            this.currentActionsPerformed = 0;
            this.cooldownEndTime = 0;
        }
        
        public int getCurrentActionsPerformed() { return currentActionsPerformed; }
        public long getCooldownEndTime() { return cooldownEndTime; }
        public void setCooldownEndTime(long cooldownEndTime) { this.cooldownEndTime = cooldownEndTime; }
    }
    
    /**
     * Status information for display purposes.
     */
    public static class ActionLimitStatus {
        private final ActionLimit limit;
        private final int currentActionsPerformed;
        private final long cooldownEndTime;
        private final long currentTime;
        
        public ActionLimitStatus(ActionLimit limit, int currentActionsPerformed, 
                               long cooldownEndTime, long currentTime) {
            this.limit = limit;
            this.currentActionsPerformed = currentActionsPerformed;
            this.cooldownEndTime = cooldownEndTime;
            this.currentTime = currentTime;
        }
        
        public boolean isOnCooldown() {
            return currentTime < cooldownEndTime;
        }
        
        public long getRemainingCooldownSeconds() {
            return Math.max(0, (cooldownEndTime - currentTime) / 1000);
        }
        
        public int getRemainingActions() {
            return Math.max(0, limit.getMaxActionsPerPeriod() - currentActionsPerformed);
        }
        
        public ActionLimit getLimit() { return limit; }
        public int getCurrentActionsPerformed() { return currentActionsPerformed; }
    }
}