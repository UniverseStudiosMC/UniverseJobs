package fr.ax_dev.universejobs.utils;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks cumulative XP and money gains per player for BossBar messages.
 * Accumulates gains until BossBar duration expires.
 */
public class CumulativeGainTracker {
    
    private static final Map<UUID, PlayerGains> PLAYER_GAINS = new ConcurrentHashMap<>();
    
    /**
     * Add gains for a player and return the cumulative totals.
     * 
     * @param player The player
     * @param xpGain XP gained this action
     * @param moneyGain Money gained this action
     * @return Array with [totalXp, totalMoney, xpPerAction, moneyPerAction]
     */
    public static double[] addGains(Player player, double xpGain, double moneyGain) {
        UUID playerId = player.getUniqueId();
        PlayerGains gains = PLAYER_GAINS.computeIfAbsent(playerId, k -> new PlayerGains());
        
        synchronized (gains) {
            gains.addGains(xpGain, moneyGain);
            System.out.println("[DEBUG] Added gains for " + player.getName() + ": +" + xpGain + " XP, +" + moneyGain + " money. Total: " + gains.totalXp + " XP, " + gains.totalMoney + " money");
            return new double[] {
                gains.totalXp,
                gains.totalMoney,
                gains.getCurrentXpPerAction(),
                gains.getCurrentMoneyPerAction()
            };
        }
    }
    
    /**
     * Check if player has active gains being tracked.
     */
    public static boolean hasActiveGains(Player player) {
        PlayerGains gains = PLAYER_GAINS.get(player.getUniqueId());
        return gains != null && gains.hasGains();
    }
    
    /**
     * Check if player has gains within the specified duration (in milliseconds).
     * This prevents creating multiple BossBars for rapid actions.
     */
    public static boolean hasRecentGains(Player player, long durationMs) {
        PlayerGains gains = PLAYER_GAINS.get(player.getUniqueId());
        if (gains == null) return false;
        
        synchronized (gains) {
            long timeSinceLastUpdate = System.currentTimeMillis() - gains.lastUpdateTime;
            return timeSinceLastUpdate < durationMs && gains.hasGains();
        }
    }
    
    /**
     * Clear gains for a player.
     */
    public static void clearGains(Player player) {
        System.out.println("[DEBUG] Clearing gains for " + player.getName());
        PLAYER_GAINS.remove(player.getUniqueId());
    }
    
    /**
     * Clear all gains (cleanup on plugin disable).
     */
    public static void clearAllGains() {
        PLAYER_GAINS.clear();
    }
    
    private static class PlayerGains {
        private double totalXp = 0.0;
        private double totalMoney = 0.0;
        private double lastXpPerAction = 0.0;
        private double lastMoneyPerAction = 0.0;
        private int actionCount = 0;
        private long lastUpdateTime = System.currentTimeMillis();
        
        void addGains(double xp, double money) {
            totalXp += xp;
            totalMoney += money;
            lastXpPerAction = xp;
            lastMoneyPerAction = money;
            actionCount++;
            lastUpdateTime = System.currentTimeMillis();
        }
        
        boolean hasGains() {
            return totalXp != 0.0 || totalMoney != 0.0;
        }
        
        double getCurrentXpPerAction() {
            return lastXpPerAction;
        }
        
        double getCurrentMoneyPerAction() {
            return lastMoneyPerAction;
        }
    }
}