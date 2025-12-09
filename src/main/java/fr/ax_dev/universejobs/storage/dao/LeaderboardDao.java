package fr.ax_dev.universejobs.storage.dao;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.storage.database.DatabaseConfig;
import fr.ax_dev.universejobs.storage.database.HikariConnectionPool;
import fr.ax_dev.universejobs.storage.SqlIdentifierValidator;

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
    private final String leaderboardCacheTable;
    
    private final String selectJobLeaderboardSql;
    private final Map<String, List<LeaderboardEntry>> jobLeaderboardCache;
    private final List<LeaderboardEntry> globalLeaderboardCache;
    private volatile long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 60000; // 1 minute
    
    public LeaderboardDao(UniverseJobs plugin, HikariConnectionPool connectionPool, DatabaseConfig config) {
        this.plugin = plugin;
        this.connectionPool = connectionPool;
        SqlIdentifierValidator.validateAndSanitizeIdentifier(config.getPrefix(), "Database prefix");
        this.leaderboardCacheTable = SqlIdentifierValidator.buildSafeTableName(config.getPrefix(), "leaderboard_cache");
        
        this.selectJobLeaderboardSql = "SELECT lc.player_uuid, lc.player_name, lc.xp, lc.level " +
                "FROM " + leaderboardCacheTable + " lc " +
                "WHERE lc.job_id = ? " +
                "ORDER BY lc.xp DESC, lc.level DESC " +
                "LIMIT ?";
                
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
            
            List<LeaderboardEntry> entries = new ArrayList<>();
            
            try (Connection connection = connectionPool.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(selectJobLeaderboardSql)) {
                
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
                        "FROM " + leaderboardCacheTable + " lc " +
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
            // Skip if connection pool is shutdown
            if (connectionPool == null) {
                return;
            }
            String sql = "INSERT OR REPLACE INTO " + leaderboardCacheTable + " " +
                        "(player_uuid, player_name, job_id, xp, level, last_updated) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
            
            if (plugin.getConfig().getString("database.type", "sqlite").equals("mysql")) {
                sql = "INSERT INTO " + leaderboardCacheTable + " " +
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
                if (!connectionPool.isInitialized()) {
                    // Connection pool is shutdown, ignore silently
                    return;
                }
                if (e.getMessage() != null && (e.getMessage().contains("has been closed") || 
                    e.getMessage().contains("Connection pool not initialized"))) {
                    // Database connection pool is shutdown, ignore silently
                    return;
                }
                plugin.getLogger().log(Level.SEVERE, "Failed to update leaderboard entry", e);
            }
        });
    }
    
    public CompletableFuture<Integer> getPlayerRank(UUID playerId, String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) + 1 as rank FROM " + leaderboardCacheTable + " lc1 " +
                        "WHERE lc1.job_id = ? AND " +
                        "(lc1.xp > (SELECT xp FROM " + leaderboardCacheTable + " lc2 " +
                        "WHERE lc2.player_uuid = ? AND lc2.job_id = ?) " +
                        "OR (lc1.xp = (SELECT xp FROM " + leaderboardCacheTable + " lc3 " +
                        "WHERE lc3.player_uuid = ? AND lc3.job_id = ?) " +
                        "AND lc1.level > (SELECT level FROM " + leaderboardCacheTable + " lc4 " +
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
                        "SELECT player_uuid, SUM(xp) as total_xp FROM " + leaderboardCacheTable + " " +
                        "GROUP BY player_uuid" +
                        ") rankings WHERE total_xp > (" +
                        "SELECT SUM(xp) FROM " + leaderboardCacheTable + " " +
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

    public CompletableFuture<Void> removeFromLeaderboard(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM " + leaderboardCacheTable + " WHERE player_uuid = ?";
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();
                invalidateCache();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to remove player from leaderboard: " + playerId, e);
            }
        });
    }

    public CompletableFuture<Void> syncLeaderboardFromPlayerData(String playerDataTable) {
        return CompletableFuture.runAsync(() -> {
            String isMysql = plugin.getConfig().getString("database.type", "sqlite").equalsIgnoreCase("mysql") ? "mysql" : "sqlite";

            String sql;
            if (isMysql.equals("mysql")) {
                sql = "INSERT INTO " + leaderboardCacheTable + " (player_uuid, player_name, job_id, xp, level, last_updated) " +
                      "SELECT pd.player_uuid, COALESCE(lc.player_name, 'Unknown'), pd.job_id, pd.xp, pd.level, ? " +
                      "FROM " + playerDataTable + " pd " +
                      "LEFT JOIN " + leaderboardCacheTable + " lc ON pd.player_uuid = lc.player_uuid AND pd.job_id = lc.job_id " +
                      "ON DUPLICATE KEY UPDATE xp = pd.xp, level = pd.level, last_updated = VALUES(last_updated)";
            } else {
                sql = "INSERT OR REPLACE INTO " + leaderboardCacheTable + " (player_uuid, player_name, job_id, xp, level, last_updated) " +
                      "SELECT pd.player_uuid, COALESCE(lc.player_name, 'Unknown'), pd.job_id, pd.xp, pd.level, ? " +
                      "FROM " + playerDataTable + " pd " +
                      "LEFT JOIN " + leaderboardCacheTable + " lc ON pd.player_uuid = lc.player_uuid AND pd.job_id = lc.job_id";
            }

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, System.currentTimeMillis());
                stmt.executeUpdate();
                invalidateCache();

                plugin.getLogger().info("Leaderboard synchronized with player data");
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to sync leaderboard from player data: " + e.getMessage());
            }
        });
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