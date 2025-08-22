package fr.ax_dev.universejobs.storage;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.storage.pool.ConnectionPool;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Level;

/**
 * Database initializer for setting up tables and schema.
 * Automatically creates necessary tables and indexes on first run.
 */
public class DatabaseInitializer {
    
    private final UniverseJobs plugin;
    private final ConnectionPool connectionPool;
    
    /**
     * Create a new DatabaseInitializer.
     * 
     * @param plugin The plugin instance
     * @param connectionPool The connection pool
     */
    public DatabaseInitializer(UniverseJobs plugin, ConnectionPool connectionPool) {
        this.plugin = plugin;
        this.connectionPool = connectionPool;
    }
    
    /**
     * Initialize the database schema.
     * Creates tables, indexes, and stored procedures if they don't exist.
     * 
     * @return true if initialization was successful
     */
    public boolean initializeDatabase() {
        if (!connectionPool.isEnabled()) {
            plugin.getLogger().info("Database is disabled, skipping initialization");
            return true;
        }
        
        try {
            plugin.getLogger().info("Initializing database schema...");
            
            // Read and execute the initialization SQL script
            String initScript = readInitScript();
            if (initScript == null) {
                plugin.getLogger().severe("Failed to read database initialization script");
                return false;
            }
            
            // Execute the script
            executeScript(initScript);
            
            // Insert initial migration record if it doesn't exist
            insertInitialMigrationRecord();
            
            // Verify tables were created
            if (verifyTables()) {
                plugin.getLogger().info("Database initialization completed successfully");
                return true;
            } else {
                plugin.getLogger().severe("Database table verification failed");
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }
    
    /**
     * Read the database initialization script from resources.
     * 
     * @return The SQL script content, or null if failed
     */
    private String readInitScript() {
        try {
            InputStream inputStream = plugin.getResource("database/init.sql");
            if (inputStream == null) {
                plugin.getLogger().severe("Database init script not found in resources");
                return null;
            }
            
            StringBuilder script = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Skip comments and empty lines
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("--") || line.startsWith("/*")) {
                        continue;
                    }
                    script.append(line).append("\n");
                }
            }
            
            return script.toString();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read database init script", e);
            return null;
        }
    }
    
    /**
     * Execute SQL script with proper statement splitting.
     * This method only executes pre-defined schema statements from the plugin resources.
     * No user input is involved - all SQL comes from the bundled init.sql file.
     * 
     * @param trustedSchemaScript The SQL script from plugin resources (not user input)
     */
    private void executeScript(String trustedSchemaScript) throws Exception {
        Connection connection = connectionPool.getConnection();
        try {
            // Split the trusted schema script by semicolon
            String[] schemaStatements = splitSqlScript(trustedSchemaScript);
            
            try (Statement stmt = connection.createStatement()) {
                for (String schemaStatement : schemaStatements) {
                    schemaStatement = schemaStatement.trim();
                    if (schemaStatement.isEmpty()) continue;
                    
                    // Validate that this is a safe schema statement (no user input)
                    if (isValidSchemaStatement(schemaStatement)) {
                        try {
                            stmt.execute(schemaStatement);
                            plugin.getLogger().fine("Executed database initialization statement successfully");
                        } catch (Exception e) {
                            // Log warning but continue with other statements
                            plugin.getLogger().log(Level.WARNING, "Failed to execute database initialization statement", e);
                        }
                    } else {
                        plugin.getLogger().warning("Skipped invalid schema statement during initialization");
                    }
                }
            }
            
        } finally {
            connectionPool.returnConnection(connection);
        }
    }
    
    /**
     * Validate that a SQL statement is a safe schema statement.
     * This ensures we only execute trusted DDL statements during initialization.
     * 
     * @param statement The SQL statement to validate
     * @return true if the statement is a safe schema statement
     */
    private boolean isValidSchemaStatement(String statement) {
        if (statement == null || statement.isEmpty()) {
            return false;
        }
        
        String upperStatement = statement.toUpperCase().trim();
        
        // Only allow safe DDL statements for schema initialization
        return upperStatement.startsWith("CREATE TABLE") ||
               upperStatement.startsWith("CREATE INDEX") ||
               upperStatement.startsWith("ALTER TABLE") ||
               upperStatement.startsWith("DROP INDEX") ||
               (upperStatement.startsWith("INSERT INTO") && upperStatement.contains("schema_migrations"));
    }

    /**
     * Insert initial migration record if it doesn't exist.
     * This is done in Java code to avoid database-specific INSERT IGNORE syntax.
     */
    private void insertInitialMigrationRecord() {
        try {
            Connection connection = connectionPool.getConnection();
            try {
                // Check if the initial migration record already exists
                try (java.sql.PreparedStatement checkStmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM schema_migrations WHERE version = ?")) {
                    checkStmt.setString(1, "1.0.0");
                    try (java.sql.ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            // Insert the initial migration record
                            try (java.sql.PreparedStatement insertStmt = connection.prepareStatement(
                                "INSERT INTO schema_migrations (version, description) VALUES (?, ?)")) {
                                insertStmt.setString(1, "1.0.0");
                                insertStmt.setString(2, "Initial database schema");
                                insertStmt.executeUpdate();
                                plugin.getLogger().info("Inserted initial migration record");
                            }
                        }
                    }
                }
            } finally {
                connectionPool.returnConnection(connection);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to insert initial migration record", e);
        }
    }

    /**
     * Split SQL script into individual statements, handling stored procedures correctly.
     * 
     * @param script The SQL script
     * @return Array of SQL statements
     */
    private String[] splitSqlScript(String script) {
        // Basic SQL script splitting by semicolon (stored procedures not currently used)
        java.util.List<String> statements = new java.util.ArrayList<>();
        
        String[] parts = script.split(";");
        StringBuilder currentStatement = new StringBuilder();
        boolean inStoredProcedure = false;
        
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            
            currentStatement.append(part);
            
            // Check for stored procedure markers
            if (part.toUpperCase().contains("DELIMITER") || 
                part.toUpperCase().contains("CREATE PROCEDURE") ||
                part.toUpperCase().contains("CREATE FUNCTION")) {
                inStoredProcedure = true;
                currentStatement.append(";");
                continue;
            }
            
            if (inStoredProcedure && (part.toUpperCase().contains("END$$") || part.toUpperCase().contains("DELIMITER ;"))) {
                inStoredProcedure = false;
                statements.add(currentStatement.toString());
                currentStatement = new StringBuilder();
                continue;
            }
            
            if (!inStoredProcedure) {
                statements.add(currentStatement.toString());
                currentStatement = new StringBuilder();
            } else {
                currentStatement.append(";");
            }
        }
        
        // Add any remaining statement
        if (currentStatement.length() > 0) {
            statements.add(currentStatement.toString());
        }
        
        return statements.toArray(new String[0]);
    }
    
    /**
     * Verify that required tables exist in the database.
     * 
     * @return true if all required tables exist
     */
    private boolean verifyTables() {
        String[] requiredTables = {
            "player_job_data",
            "player_reward_claims",
            "performance_stats",
            "server_config",
            "schema_migrations"
        };
        
        try {
            Connection connection = connectionPool.getConnection();
            try {
                for (String tableName : requiredTables) {
                    if (!tableExists(connection, tableName)) {
                        plugin.getLogger().severe("Required table '" + tableName + "' does not exist");
                        return false;
                    }
                }
                
                plugin.getLogger().info("All required database tables verified");
                return true;
                
            } finally {
                connectionPool.returnConnection(connection);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to verify database tables", e);
            return false;
        }
    }
    
    /**
     * Check if a table exists in the database.
     * 
     * @param connection Database connection
     * @param tableName Table name to check
     * @return true if table exists
     */
    private boolean tableExists(Connection connection, String tableName) {
        try {
            // Use database metadata to check if table exists
            java.sql.DatabaseMetaData metaData = connection.getMetaData();
            try (java.sql.ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
                return rs.next();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check if table '" + tableName + "' exists", e);
            return false;
        }
    }
    
    /**
     * Create database indexes for performance optimization.
     * This method can be called separately to add indexes to existing tables.
     * 
     * @return true if indexes were created successfully
     */
    public boolean createPerformanceIndexes() {
        if (!connectionPool.isEnabled()) {
            return true;
        }
        
        String[] indexStatements = {
            "CREATE INDEX IF NOT EXISTS idx_player_job_data_last_updated ON player_job_data (last_updated)",
            "CREATE INDEX IF NOT EXISTS idx_player_reward_claims_player_uuid ON player_reward_claims (player_uuid)",
            "CREATE INDEX IF NOT EXISTS idx_player_reward_claims_job_id ON player_reward_claims (job_id)",
            "CREATE INDEX IF NOT EXISTS idx_player_reward_claims_claim_time ON player_reward_claims (claim_time)",
            "CREATE INDEX IF NOT EXISTS idx_performance_stats_metric_name ON performance_stats (metric_name)",
            "CREATE INDEX IF NOT EXISTS idx_performance_stats_timestamp ON performance_stats (timestamp)"
        };
        
        try {
            Connection connection = connectionPool.getConnection();
            try (Statement stmt = connection.createStatement()) {
                for (String indexSql : indexStatements) {
                    try {
                        stmt.execute(indexSql);
                        plugin.getLogger().fine("Created database index successfully");
                    } catch (Exception e) {
                        // Index might already exist, log as warning
                        plugin.getLogger().log(Level.WARNING, "Failed to create database index", e);
                    }
                }
                
                plugin.getLogger().info("Performance indexes creation completed");
                return true;
                
            } finally {
                connectionPool.returnConnection(connection);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create performance indexes", e);
            return false;
        }
    }
    
    /**
     * Get database information and statistics.
     * 
     * @return Map containing database information
     */
    public java.util.Map<String, Object> getDatabaseInfo() {
        java.util.Map<String, Object> info = new java.util.HashMap<>();
        
        if (!connectionPool.isEnabled()) {
            info.put("enabled", false);
            return info;
        }
        
        try {
            Connection connection = connectionPool.getConnection();
            try {
                java.sql.DatabaseMetaData metaData = connection.getMetaData();
                
                info.put("enabled", true);
                info.put("database_product", metaData.getDatabaseProductName());
                info.put("database_version", metaData.getDatabaseProductVersion());
                info.put("driver_name", metaData.getDriverName());
                info.put("driver_version", metaData.getDriverVersion());
                info.put("url", metaData.getURL());
                info.put("username", metaData.getUserName());
                
                // Get table counts
                try (Statement stmt = connection.createStatement()) {
                    try (java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player_job_data")) {
                        if (rs.next()) {
                            info.put("player_job_data_count", rs.getInt(1));
                        }
                    }
                    
                    try (java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player_reward_claims")) {
                        if (rs.next()) {
                            info.put("player_reward_claims_count", rs.getInt(1));
                        }
                    }
                } catch (Exception e) {
                    info.put("table_counts_error", e.getMessage());
                }
                
            } finally {
                connectionPool.returnConnection(connection);
            }
            
        } catch (Exception e) {
            info.put("error", e.getMessage());
        }
        
        return info;
    }
}