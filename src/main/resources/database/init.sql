-- JobsAdventure Database Schema
-- High-performance optimized tables for player job data and rewards

-- Create database (if using MySQL/PostgreSQL)
-- CREATE DATABASE IF NOT EXISTS jobsadventure;
-- USE jobsadventure;

-- Main player job data table
CREATE TABLE IF NOT EXISTS player_job_data (
    player_uuid VARCHAR(36) PRIMARY KEY,
    data TEXT NOT NULL,
    compressed BOOLEAN DEFAULT FALSE,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes for performance
    INDEX idx_last_updated (last_updated),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Player reward claim data table
CREATE TABLE IF NOT EXISTS player_reward_claims (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    reward_id VARCHAR(128) NOT NULL,
    claim_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Unique constraint to prevent duplicate claims (unless repeatable)
    UNIQUE KEY uk_player_job_reward (player_uuid, job_id, reward_id),
    
    -- Indexes for performance
    INDEX idx_player_uuid (player_uuid),
    INDEX idx_job_id (job_id),
    INDEX idx_claim_time (claim_time),
    INDEX idx_player_job (player_uuid, job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Performance statistics table
CREATE TABLE IF NOT EXISTS performance_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(128) NOT NULL,
    metric_value DECIMAL(15,4),
    metric_type ENUM('counter', 'gauge', 'timer', 'histogram') DEFAULT 'gauge',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes for performance
    INDEX idx_metric_name (metric_name),
    INDEX idx_timestamp (timestamp),
    INDEX idx_metric_type (metric_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Server configuration table
CREATE TABLE IF NOT EXISTS server_config (
    config_key VARCHAR(128) PRIMARY KEY,
    config_value TEXT,
    config_type ENUM('string', 'number', 'boolean', 'json') DEFAULT 'string',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Index for performance
    INDEX idx_last_updated (last_updated)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Migration version tracking
CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(32) PRIMARY KEY,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert initial migration record
INSERT IGNORE INTO schema_migrations (version, description) 
VALUES ('1.0.0', 'Initial database schema');

-- Optional: Create views for common queries

-- View for active players (players with recent activity)
CREATE OR REPLACE VIEW active_players AS
SELECT 
    player_uuid,
    last_updated,
    DATEDIFF(NOW(), last_updated) as days_since_activity
FROM player_job_data 
WHERE last_updated > DATE_SUB(NOW(), INTERVAL 30 DAY)
ORDER BY last_updated DESC;

-- View for reward statistics
CREATE OR REPLACE VIEW reward_statistics AS
SELECT 
    job_id,
    reward_id,
    COUNT(*) as claim_count,
    COUNT(DISTINCT player_uuid) as unique_claimers,
    MIN(claim_time) as first_claim,
    MAX(claim_time) as latest_claim
FROM player_reward_claims
GROUP BY job_id, reward_id
ORDER BY claim_count DESC;

-- Stored procedures for maintenance

DELIMITER $$

-- Procedure to cleanup old performance stats
CREATE PROCEDURE CleanupOldPerformanceStats(IN days_to_keep INT)
BEGIN
    DELETE FROM performance_stats 
    WHERE timestamp < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);
    
    SELECT ROW_COUNT() as deleted_rows;
END$$

-- Procedure to get player activity summary
CREATE PROCEDURE GetPlayerActivitySummary(IN player_uuid_param VARCHAR(36))
BEGIN
    SELECT 
        pjd.player_uuid,
        pjd.last_updated as last_job_activity,
        COUNT(prc.id) as total_rewards_claimed,
        COUNT(DISTINCT prc.job_id) as jobs_with_rewards
    FROM player_job_data pjd
    LEFT JOIN player_reward_claims prc ON pjd.player_uuid = prc.player_uuid
    WHERE pjd.player_uuid = player_uuid_param
    GROUP BY pjd.player_uuid, pjd.last_updated;
END$$

-- Procedure to backup player data
CREATE PROCEDURE BackupPlayerData(IN backup_table_suffix VARCHAR(32))
BEGIN
    SET @sql = CONCAT('CREATE TABLE player_job_data_backup_', backup_table_suffix, ' AS SELECT * FROM player_job_data');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    SET @sql = CONCAT('CREATE TABLE player_reward_claims_backup_', backup_table_suffix, ' AS SELECT * FROM player_reward_claims');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    SELECT CONCAT('Backup completed with suffix: ', backup_table_suffix) as result;
END$$

DELIMITER ;

-- Create user and grant permissions (adjust as needed)
-- CREATE USER IF NOT EXISTS 'jobsadventure'@'localhost' IDENTIFIED BY 'secure_password_here';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON jobsadventure.* TO 'jobsadventure'@'localhost';
-- FLUSH PRIVILEGES;

-- Performance optimization hints
-- Consider adding these for production:

-- Enable query cache (MySQL)
-- SET GLOBAL query_cache_type = ON;
-- SET GLOBAL query_cache_size = 268435456; -- 256MB

-- Optimize InnoDB settings (add to my.cnf)
-- innodb_buffer_pool_size = 1G
-- innodb_log_file_size = 256M
-- innodb_flush_log_at_trx_commit = 2
-- innodb_file_per_table = 1