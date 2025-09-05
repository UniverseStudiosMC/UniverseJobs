package fr.ax_dev.universejobs.storage.dao;

import fr.ax_dev.universejobs.storage.database.DatabaseConfig;
import fr.ax_dev.universejobs.storage.database.HikariConnectionPool;
import fr.ax_dev.universejobs.storage.SqlIdentifierValidator;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RewardDao {
    
    private final HikariConnectionPool connectionPool;
    private final String tableName;
    
    private final String insertSql;
    private final String selectByPlayerAndJobAndRewardSql;
    private final String selectByPlayerAndJobSql;
    private final String selectByPlayerSql;
    private final String deleteByPlayerAndJobAndRewardSql;
    private final String deleteByPlayerAndJobSql;
    private final String deleteByPlayerSql;

    public RewardDao(HikariConnectionPool connectionPool, DatabaseConfig config) {
        this.connectionPool = connectionPool;
        this.tableName = SqlIdentifierValidator.buildSafeTableName(config.getPrefix(), "reward_claims");
        
        this.insertSql = "INSERT INTO " + tableName + " (player_uuid, job_id, reward_id, claim_time) VALUES (?, ?, ?, ?)";
        this.selectByPlayerAndJobAndRewardSql = "SELECT claim_time FROM " + tableName + " WHERE player_uuid = ? AND job_id = ? AND reward_id = ?";
        this.selectByPlayerAndJobSql = "SELECT reward_id, claim_time FROM " + tableName + " WHERE player_uuid = ? AND job_id = ?";
        this.selectByPlayerSql = "SELECT job_id, reward_id, claim_time FROM " + tableName + " WHERE player_uuid = ?";
        this.deleteByPlayerAndJobAndRewardSql = "DELETE FROM " + tableName + " WHERE player_uuid = ? AND job_id = ? AND reward_id = ?";
        this.deleteByPlayerAndJobSql = "DELETE FROM " + tableName + " WHERE player_uuid = ? AND job_id = ?";
        this.deleteByPlayerSql = "DELETE FROM " + tableName + " WHERE player_uuid = ?";
    }

    public CompletableFuture<Void> claimReward(UUID playerId, String jobId, String rewardId, long claimTime) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                
                stmt.setString(1, playerId.toString());
                stmt.setString(2, jobId);
                stmt.setString(3, rewardId);
                stmt.setLong(4, claimTime);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to claim reward " + rewardId + " for player " + playerId, e);
            }
        });
    }

    public CompletableFuture<Boolean> hasClaimedReward(UUID playerId, String jobId, String rewardId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectByPlayerAndJobAndRewardSql)) {
                
                stmt.setString(1, playerId.toString());
                stmt.setString(2, jobId);
                stmt.setString(3, rewardId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to check reward claim for " + playerId, e);
            }
        });
    }

    public CompletableFuture<Long> getClaimTime(UUID playerId, String jobId, String rewardId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectByPlayerAndJobAndRewardSql)) {
                
                stmt.setString(1, playerId.toString());
                stmt.setString(2, jobId);
                stmt.setString(3, rewardId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("claim_time");
                    }
                    return -1L;
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get claim time for " + playerId, e);
            }
        });
    }

    public CompletableFuture<Set<String>> getClaimedRewards(UUID playerId, String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> claimedRewards = new HashSet<>();
            
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectByPlayerAndJobSql)) {
                
                stmt.setString(1, playerId.toString());
                stmt.setString(2, jobId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        claimedRewards.add(rs.getString("reward_id"));
                    }
                }
                
                return claimedRewards;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get claimed rewards for " + playerId, e);
            }
        });
    }

    public CompletableFuture<Set<String>> getAllClaimedRewards(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> allClaimedRewards = new HashSet<>();
            
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectByPlayerSql)) {
                
                stmt.setString(1, playerId.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String jobId = rs.getString("job_id");
                        String rewardId = rs.getString("reward_id");
                        allClaimedRewards.add(jobId + ":" + rewardId);
                    }
                }
                
                return allClaimedRewards;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get all claimed rewards for " + playerId, e);
            }
        });
    }

    public CompletableFuture<Map<String, Long>> getClaimedRewardsWithTime(UUID playerId, String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Long> claimedRewards = new HashMap<>();
            
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectByPlayerAndJobSql)) {
                
                stmt.setString(1, playerId.toString());
                stmt.setString(2, jobId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String rewardId = rs.getString("reward_id");
                        long claimTime = rs.getLong("claim_time");
                        claimedRewards.put(rewardId, claimTime);
                    }
                }
                
                return claimedRewards;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get claimed rewards with time for " + playerId, e);
            }
        });
    }

    public CompletableFuture<Void> resetRewardClaim(UUID playerId, String jobId, String rewardId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(deleteByPlayerAndJobAndRewardSql)) {
                
                stmt.setString(1, playerId.toString());
                stmt.setString(2, jobId);
                stmt.setString(3, rewardId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to reset reward claim for " + playerId, e);
            }
        });
    }

    public CompletableFuture<Void> resetJobRewards(UUID playerId, String jobId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(deleteByPlayerAndJobSql)) {
                
                stmt.setString(1, playerId.toString());
                stmt.setString(2, jobId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to reset job rewards for " + playerId, e);
            }
        });
    }

    public CompletableFuture<Void> resetAllRewards(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(deleteByPlayerSql)) {
                
                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to reset all rewards for " + playerId, e);
            }
        });
    }
}