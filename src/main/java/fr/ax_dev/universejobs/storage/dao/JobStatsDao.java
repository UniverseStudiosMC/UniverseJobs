package fr.ax_dev.universejobs.storage.dao;

import fr.ax_dev.universejobs.storage.database.DatabaseConfig;
import fr.ax_dev.universejobs.storage.database.HikariConnectionPool;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class JobStatsDao {
    
    private final HikariConnectionPool connectionPool;
    private final String tableName;
    
    private final String insertOrUpdateSql;
    private final String selectByJobAndStatSql;
    private final String selectByJobSql;
    private final String deleteByJobAndStatSql;
    private final String deleteByJobSql;

    public JobStatsDao(HikariConnectionPool connectionPool, DatabaseConfig config) {
        this.connectionPool = connectionPool;
        this.tableName = config.getPrefix() + "job_stats";
        
        if (config.getType().getName().equals("mysql")) {
            this.insertOrUpdateSql = "INSERT INTO " + tableName + " (job_id, stat_name, stat_value, last_updated) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE stat_value = VALUES(stat_value), last_updated = VALUES(last_updated)";
        } else {
            this.insertOrUpdateSql = "INSERT OR REPLACE INTO " + tableName + " (job_id, stat_name, stat_value, last_updated) VALUES (?, ?, ?, ?)";
        }
        
        this.selectByJobAndStatSql = "SELECT stat_value FROM " + tableName + " WHERE job_id = ? AND stat_name = ?";
        this.selectByJobSql = "SELECT stat_name, stat_value, last_updated FROM " + tableName + " WHERE job_id = ?";
        this.deleteByJobAndStatSql = "DELETE FROM " + tableName + " WHERE job_id = ? AND stat_name = ?";
        this.deleteByJobSql = "DELETE FROM " + tableName + " WHERE job_id = ?";
    }

    public CompletableFuture<Void> setStat(String jobId, String statName, String value) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertOrUpdateSql)) {
                
                stmt.setString(1, jobId);
                stmt.setString(2, statName);
                stmt.setString(3, value);
                stmt.setLong(4, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to set stat " + statName + " for job " + jobId, e);
            }
        });
    }

    public CompletableFuture<String> getStat(String jobId, String statName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectByJobAndStatSql)) {
                
                stmt.setString(1, jobId);
                stmt.setString(2, statName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("stat_value");
                    }
                    return null;
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get stat " + statName + " for job " + jobId, e);
            }
        });
    }

    public CompletableFuture<Map<String, String>> getJobStats(String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> stats = new HashMap<>();
            
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectByJobSql)) {
                
                stmt.setString(1, jobId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String statName = rs.getString("stat_name");
                        String statValue = rs.getString("stat_value");
                        stats.put(statName, statValue);
                    }
                }
                
                return stats;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get stats for job " + jobId, e);
            }
        });
    }

    public CompletableFuture<Void> removeStat(String jobId, String statName) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(deleteByJobAndStatSql)) {
                
                stmt.setString(1, jobId);
                stmt.setString(2, statName);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to remove stat " + statName + " for job " + jobId, e);
            }
        });
    }

    public CompletableFuture<Void> removeAllJobStats(String jobId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(deleteByJobSql)) {
                
                stmt.setString(1, jobId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to remove all stats for job " + jobId, e);
            }
        });
    }
}