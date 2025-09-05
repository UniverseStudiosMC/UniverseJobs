package fr.ax_dev.universejobs.storage.dao;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.storage.database.DatabaseConfig;
import fr.ax_dev.universejobs.storage.database.HikariConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class LeaderboardDao {
    
    private final UniverseJobs plugin;
    private final HikariConnectionPool connectionPool;
    private final String prefix;
    
    private final Map<String, List<LeaderboardEntry>> jobLeaderboardCache;
    private final List<LeaderboardEntry> globalLeaderboardCache;
    private volatile long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 60000; // 1 minute
    
    public LeaderboardDao(UniverseJobs plugin, HikariConnectionPool connectionPool, DatabaseConfig config) {
        this.plugin = plugin;
        this.connectionPool = connectionPool;
        this.prefix = config.getPrefix();
        this.jobLeaderboardCache = new ConcurrentHashMap<>();
        this.globalLeaderboardCache = new ArrayList<>();
    }
    
    public CompletableFuture<List<LeaderboardEntry>> getJobLeaderboard(String jobId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            if (shouldUseCache()) {
                List<LeaderboardEntry> cached = jobLeaderboardCache.get(jobId);
                if (cached != null) {
                    return cached.stream().limit(limit).toList();
                }
            }
            
            String sql = "SELECT lc.player_uuid, lc.player_name, lc.xp, lc.level " +
                        "FROM " + prefix + "leaderboard_cache lc " +
                        "WHERE lc.job_id = ? " +
                        "ORDER BY lc.xp DESC, lc.level DESC " +
                        "LIMIT ?";
            
            List<LeaderboardEntry> entries = new ArrayList<>();
            
            try (Connection connection = connectionPool.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, jobId);
                stmt.setInt(2, limit);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        entries.add(new LeaderboardEntry(
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name"),
                            rs.getDouble("xp"),
                            rs.getInt("level")
                        ));
                    }
                }
                
                jobLeaderboardCache.put(jobId, new ArrayList<>(entries));
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to fetch job leaderboard for " + jobId, e);
            }
            
            return entries;
        });
    }
    
    public CompletableFuture<List<LeaderboardEntry>> getGlobalLeaderboard(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            if (shouldUseCache() && !globalLeaderboardCache.isEmpty()) {
                return globalLeaderboardCache.stream().limit(limit).toList();
            }
            
            String sql = "SELECT lc.player_uuid, lc.player_name, " +
                        "SUM(lc.xp) as total_xp, SUM(lc.level) as total_levels " +
                        "FROM " + prefix + "leaderboard_cache lc " +
                        "GROUP BY lc.player_uuid, lc.player_name " +
                        "ORDER BY total_xp DESC, total_levels DESC " +
                        "LIMIT ?";
            
            List<LeaderboardEntry> entries = new ArrayList<>();
            
            try (Connection connection = connectionPool.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setInt(1, limit);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        entries.add(new LeaderboardEntry(
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name"),
                            rs.getDouble("total_xp"),
                            rs.getInt("total_levels")
                        ));
                    }
                }
                
                globalLeaderboardCache.clear();
                globalLeaderboardCache.addAll(entries);
                lastCacheUpdate = System.currentTimeMillis();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to fetch global leaderboard", e);
            }
            
            return entries;
        });
    }
    
    public CompletableFuture<Void> updatePlayerLeaderboardEntry(UUID playerId, String playerName, String jobId, double xp, int level) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO " + prefix + "leaderboard_cache " +
                        "(player_uuid, player_name, job_id, xp, level, last_updated) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
            
            if (plugin.getConfig().getString("database.type", "sqlite").equals("mysql")) {
                sql = "INSERT INTO " + prefix + "leaderboard_cache " +
                     "(player_uuid, player_name, job_id, xp, level, last_updated) " +
                     "VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "player_name = VALUES(player_name), xp = VALUES(xp), level = VALUES(level), " +
                     "last_updated = VALUES(last_updated)";
            }
            
            try (Connection connection = connectionPool.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, playerId.toString());
                stmt.setString(2, playerName);
                stmt.setString(3, jobId);
                stmt.setDouble(4, xp);
                stmt.setInt(5, level);
                stmt.setLong(6, System.currentTimeMillis());
                
                stmt.executeUpdate();
                
                invalidateCache();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update leaderboard entry", e);
            }
        });
    }
    
    public CompletableFuture<Integer> getPlayerRank(UUID playerId, String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) + 1 as rank FROM " + prefix + "leaderboard_cache lc1 " +
                        "WHERE lc1.job_id = ? AND " +
                        "(lc1.xp > (SELECT xp FROM " + prefix + "leaderboard_cache lc2 " +
                        "WHERE lc2.player_uuid = ? AND lc2.job_id = ?) " +
                        "OR (lc1.xp = (SELECT xp FROM " + prefix + "leaderboard_cache lc3 " +
                        "WHERE lc3.player_uuid = ? AND lc3.job_id = ?) " +
                        "AND lc1.level > (SELECT level FROM " + prefix + "leaderboard_cache lc4 " +
                        "WHERE lc4.player_uuid = ? AND lc4.job_id = ?)))";
            
            try (Connection connection = connectionPool.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                String playerIdStr = playerId.toString();
                stmt.setString(1, jobId);
                stmt.setString(2, playerIdStr);
                stmt.setString(3, jobId);
                stmt.setString(4, playerIdStr);
                stmt.setString(5, jobId);
                stmt.setString(6, playerIdStr);
                stmt.setString(7, jobId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("rank");
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player rank", e);
            }
            
            return -1;
        });
    }
    
    public CompletableFuture<Integer> getGlobalPlayerRank(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) + 1 as rank FROM (" +
                        "SELECT player_uuid, SUM(xp) as total_xp FROM " + prefix + "leaderboard_cache " +
                        "GROUP BY player_uuid" +
                        ") rankings WHERE total_xp > (" +
                        "SELECT SUM(xp) FROM " + prefix + "leaderboard_cache " +
                        "WHERE player_uuid = ?)";
            
            try (Connection connection = connectionPool.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, playerId.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("rank");
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get global player rank", e);
            }
            
            return -1;
        });
    }
    
    private boolean shouldUseCache() {
        return System.currentTimeMillis() - lastCacheUpdate < CACHE_DURATION;
    }
    
    private void invalidateCache() {
        jobLeaderboardCache.clear();
        globalLeaderboardCache.clear();
        lastCacheUpdate = 0;
    }
    
    public static class LeaderboardEntry {
        private final UUID playerId;
        private final String playerName;
        private final double xp;
        private final int level;
        
        public LeaderboardEntry(UUID playerId, String playerName, double xp, int level) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.xp = xp;
            this.level = level;
        }
        
        public UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public double getXp() { return xp; }
        public int getLevel() { return level; }
    }
}