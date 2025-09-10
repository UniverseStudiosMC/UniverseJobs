package fr.ax_dev.universejobs.storage.database;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.storage.SqlIdentifierValidator;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class DatabaseSchema {
    
    private final UniverseJobs plugin;
    private final DatabaseConfig config;
    private final HikariConnectionPool connectionPool;

    public DatabaseSchema(UniverseJobs plugin, DatabaseConfig config, HikariConnectionPool connectionPool) {
        this.plugin = plugin;
        this.config = config;
        this.connectionPool = connectionPool;
    }

    public void initializeSchema() throws SQLException {
        try (Connection connection = connectionPool.getConnection()) {
            createTables(connection);
            createIndexes(connection);
            plugin.getLogger().info("Database schema initialized successfully");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database schema", e);
            throw e;
        }
    }

    private void createTables(Connection connection) throws SQLException {
        String prefix = config.getPrefix();
        
        createPlayerDataTable(connection, prefix);
        createRewardClaimsTable(connection, prefix);
        createJobStatsTable(connection, prefix);
        createLeaderboardCacheTable(connection, prefix);
    }

    private void createPlayerDataTable(Connection connection, String prefix) throws SQLException {
        String validatedPrefix = SqlIdentifierValidator.validateAndSanitizeIdentifier(prefix, "Database prefix");
        String tableName = SqlIdentifierValidator.buildSafeTableName(validatedPrefix, "player_data");
        
        String sql;
        if (config.getType() == DatabaseType.MYSQL) {
            sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "job_id VARCHAR(64) NOT NULL, " +
                    "xp DOUBLE NOT NULL DEFAULT 0, " +
                    "level INT NOT NULL DEFAULT 1, " +
                    "last_modified BIGINT NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (player_uuid, job_id), " +
                    "INDEX idx_player (player_uuid), " +
                    "INDEX idx_job (job_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "player_uuid TEXT NOT NULL, " +
                    "job_id TEXT NOT NULL, " +
                    "xp REAL NOT NULL DEFAULT 0, " +
                    "level INTEGER NOT NULL DEFAULT 1, " +
                    "last_modified INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (player_uuid, job_id)" +
                    ")";
        }
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql); // NOSONAR - SQL is safely constructed using SqlIdentifierValidator
        }
    }

    private void createRewardClaimsTable(Connection connection, String prefix) throws SQLException {
        String validatedPrefix = SqlIdentifierValidator.validateAndSanitizeIdentifier(prefix, "Database prefix");
        String tableName = SqlIdentifierValidator.buildSafeTableName(validatedPrefix, "reward_claims");
        
        String sql;
        if (config.getType() == DatabaseType.MYSQL) {
            sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "job_id VARCHAR(64) NOT NULL, " +
                    "reward_id VARCHAR(64) NOT NULL, " +
                    "claim_time BIGINT NOT NULL, " +
                    "PRIMARY KEY (player_uuid, job_id, reward_id), " +
                    "INDEX idx_player_rewards (player_uuid), " +
                    "INDEX idx_job_rewards (job_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "player_uuid TEXT NOT NULL, " +
                    "job_id TEXT NOT NULL, " +
                    "reward_id TEXT NOT NULL, " +
                    "claim_time INTEGER NOT NULL, " +
                    "PRIMARY KEY (player_uuid, job_id, reward_id)" +
                    ")";
        }
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql); // NOSONAR - SQL is safely constructed using SqlIdentifierValidator
        }
    }

    private void createJobStatsTable(Connection connection, String prefix) throws SQLException {
        String validatedPrefix = SqlIdentifierValidator.validateAndSanitizeIdentifier(prefix, "Database prefix");
        String tableName = SqlIdentifierValidator.buildSafeTableName(validatedPrefix, "job_stats");
        
        String sql;
        if (config.getType() == DatabaseType.MYSQL) {
            sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "job_id VARCHAR(64) NOT NULL, " +
                    "stat_name VARCHAR(64) NOT NULL, " +
                    "stat_value TEXT, " +
                    "last_updated BIGINT NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (job_id, stat_name)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "job_id TEXT NOT NULL, " +
                    "stat_name TEXT NOT NULL, " +
                    "stat_value TEXT, " +
                    "last_updated INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (job_id, stat_name)" +
                    ")";
        }
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql); // NOSONAR - SQL is safely constructed using SqlIdentifierValidator
        }
    }

    private void createIndexes(Connection connection) throws SQLException {
        if (config.getType() == DatabaseType.SQLITE) {
            String prefix = SqlIdentifierValidator.validateAndSanitizeIdentifier(config.getPrefix(), "Database prefix");
            
            try (Statement stmt = connection.createStatement()) {
                String playerDataTable = SqlIdentifierValidator.buildSafeTableName(prefix, "player_data");
                String rewardClaimsTable = SqlIdentifierValidator.buildSafeTableName(prefix, "reward_claims");
                String leaderboardCacheTable = SqlIdentifierValidator.buildSafeTableName(prefix, "leaderboard_cache");
                
                stmt.execute("CREATE INDEX IF NOT EXISTS " + SqlIdentifierValidator.buildSafeIndexName(prefix, "player_data_player") + " ON " + playerDataTable + " (player_uuid)");
                stmt.execute("CREATE INDEX IF NOT EXISTS " + SqlIdentifierValidator.buildSafeIndexName(prefix, "player_data_job") + " ON " + playerDataTable + " (job_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS " + SqlIdentifierValidator.buildSafeIndexName(prefix, "reward_claims_player") + " ON " + rewardClaimsTable + " (player_uuid)");
                stmt.execute("CREATE INDEX IF NOT EXISTS " + SqlIdentifierValidator.buildSafeIndexName(prefix, "reward_claims_job") + " ON " + rewardClaimsTable + " (job_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS " + SqlIdentifierValidator.buildSafeIndexName(prefix, "leaderboard_cache_job") + " ON " + leaderboardCacheTable + " (job_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS " + SqlIdentifierValidator.buildSafeIndexName(prefix, "leaderboard_cache_xp") + " ON " + leaderboardCacheTable + " (xp DESC)");
                stmt.execute("CREATE INDEX IF NOT EXISTS " + SqlIdentifierValidator.buildSafeIndexName(prefix, "leaderboard_cache_level") + " ON " + leaderboardCacheTable + " (level DESC)");
            }
        }
    }

    private void createLeaderboardCacheTable(Connection connection, String prefix) throws SQLException {
        String validatedPrefix = SqlIdentifierValidator.validateAndSanitizeIdentifier(prefix, "Database prefix");
        String tableName = SqlIdentifierValidator.buildSafeTableName(validatedPrefix, "leaderboard_cache");
        
        String sql;
        if (config.getType() == DatabaseType.MYSQL) {
            sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "player_name VARCHAR(16) NOT NULL, " +
                    "job_id VARCHAR(64) NOT NULL, " +
                    "xp DOUBLE NOT NULL DEFAULT 0, " +
                    "level INT NOT NULL DEFAULT 1, " +
                    "last_updated BIGINT NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (player_uuid, job_id), " +
                    "INDEX idx_job_xp (job_id, xp DESC), " +
                    "INDEX idx_job_level (job_id, level DESC), " +
                    "INDEX idx_global_xp (xp DESC), " +
                    "INDEX idx_global_level (level DESC)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "player_uuid TEXT NOT NULL, " +
                    "player_name TEXT NOT NULL, " +
                    "job_id TEXT NOT NULL, " +
                    "xp REAL NOT NULL DEFAULT 0, " +
                    "level INTEGER NOT NULL DEFAULT 1, " +
                    "last_updated INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (player_uuid, job_id)" +
                    ")";
        }
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql); // NOSONAR - SQL is safely constructed using SqlIdentifierValidator
        }
    }
}