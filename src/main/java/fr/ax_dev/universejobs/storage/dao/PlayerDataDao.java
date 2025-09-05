package fr.ax_dev.universejobs.storage.dao;

import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.storage.database.DatabaseConfig;
import fr.ax_dev.universejobs.storage.database.HikariConnectionPool;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PlayerDataDao {
    
    private final HikariConnectionPool connectionPool;
    private final String tableName;
    
    private final String insertSql;
    private final String updateSql;
    private final String selectByPlayerSql;
    private final String selectByPlayerAndJobSql;
    private final String deleteByPlayerAndJobSql;
    private final String selectAllByPlayersSql;

    public PlayerDataDao(HikariConnectionPool connectionPool, DatabaseConfig config) {
        this.connectionPool = connectionPool;
        this.tableName = config.getPrefix() + "player_data";
        
        this.insertSql = "INSERT INTO " + tableName + " (player_uuid, job_id, xp, level, last_modified) VALUES (?, ?, ?, ?, ?)";
        this.updateSql = "UPDATE " + tableName + " SET xp = ?, level = ?, last_modified = ? WHERE player_uuid = ? AND job_id = ?";
        this.selectByPlayerSql = "SELECT job_id, xp, level, last_modified FROM " + tableName + " WHERE player_uuid = ?";
        this.selectByPlayerAndJobSql = "SELECT xp, level, last_modified FROM " + tableName + " WHERE player_uuid = ? AND job_id = ?";
        this.deleteByPlayerAndJobSql = "DELETE FROM " + tableName + " WHERE player_uuid = ? AND job_id = ?";
        this.selectAllByPlayersSql = "SELECT player_uuid, job_id, xp, level, last_modified FROM " + tableName + " WHERE player_uuid IN ";
    }

    public CompletableFuture<Void> savePlayerData(UUID playerId, PlayerJobData data) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionPool.getConnection()) {
                conn.setAutoCommit(false);
                
                try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM " + tableName + " WHERE player_uuid = ?")) {
                    deleteStmt.setString(1, playerId.toString());
                    deleteStmt.executeUpdate();
                }
                
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    for (String jobId : data.getJobs()) {
                        insertStmt.setString(1, playerId.toString());
                        insertStmt.setString(2, jobId);
                        insertStmt.setDouble(3, data.getXp(jobId));
                        insertStmt.setInt(4, data.getLevel(jobId));
                        insertStmt.setLong(5, data.getLastModified());
                        insertStmt.addBatch();
                    }
                    insertStmt.executeBatch();
                }
                
                conn.commit();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save player data for " + playerId, e);
            }
        });
    }

    public CompletableFuture<PlayerJobData> loadPlayerData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerJobData data = new PlayerJobData(playerId);
            
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectByPlayerSql)) {
                
                stmt.setString(1, playerId.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String jobId = rs.getString("job_id");
                        double xp = rs.getDouble("xp");
                        int level = rs.getInt("level");
                        
                        data.joinJob(jobId);
                        data.setXp(jobId, xp);
                        data.setLevel(jobId, level);
                    }
                }
                
                return data;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load player data for " + playerId, e);
            }
        });
    }

    public CompletableFuture<Map<UUID, PlayerJobData>> loadPlayerDataBatch(Set<UUID> playerIds) {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, PlayerJobData> result = new HashMap<>();
            
            if (playerIds.isEmpty()) {
                return result;
            }
            
            StringBuilder inClause = new StringBuilder("(");
            for (int i = 0; i < playerIds.size(); i++) {
                inClause.append("?");
                if (i < playerIds.size() - 1) {
                    inClause.append(",");
                }
            }
            inClause.append(")");
            
            String sql = selectAllByPlayersSql + inClause.toString();
            
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                int index = 1;
                for (UUID playerId : playerIds) {
                    stmt.setString(index++, playerId.toString());
                    result.put(playerId, new PlayerJobData(playerId));
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                        String jobId = rs.getString("job_id");
                        double xp = rs.getDouble("xp");
                        int level = rs.getInt("level");
                        
                        PlayerJobData data = result.get(playerId);
                        if (data != null) {
                            data.joinJob(jobId);
                            data.setXp(jobId, xp);
                            data.setLevel(jobId, level);
                        }
                    }
                }
                
                return result;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load player data batch", e);
            }
        });
    }

    public CompletableFuture<Void> updateJobData(UUID playerId, String jobId, double xp, int level) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionPool.getConnection()) {
                boolean exists = false;
                
                try (PreparedStatement checkStmt = conn.prepareStatement(selectByPlayerAndJobSql)) {
                    checkStmt.setString(1, playerId.toString());
                    checkStmt.setString(2, jobId);
                    
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        exists = rs.next();
                    }
                }
                
                if (exists) {
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setDouble(1, xp);
                        updateStmt.setInt(2, level);
                        updateStmt.setLong(3, System.currentTimeMillis());
                        updateStmt.setString(4, playerId.toString());
                        updateStmt.setString(5, jobId);
                        updateStmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, playerId.toString());
                        insertStmt.setString(2, jobId);
                        insertStmt.setDouble(3, xp);
                        insertStmt.setInt(4, level);
                        insertStmt.setLong(5, System.currentTimeMillis());
                        insertStmt.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update job data for " + playerId + " job " + jobId, e);
            }
        });
    }

    public CompletableFuture<Void> removeJobData(UUID playerId, String jobId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(deleteByPlayerAndJobSql)) {
                
                stmt.setString(1, playerId.toString());
                stmt.setString(2, jobId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to remove job data for " + playerId + " job " + jobId, e);
            }
        });
    }
}