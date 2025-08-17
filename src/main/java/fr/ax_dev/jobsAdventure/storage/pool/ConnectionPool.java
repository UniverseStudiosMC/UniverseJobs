package fr.ax_dev.jobsAdventure.storage.pool;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * High-performance database connection pool.
 * Manages database connections efficiently with health monitoring.
 */
public class ConnectionPool {
    
    private final JobsAdventure plugin;
    private final boolean enabled;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int minConnections;
    private final int maxConnections;
    private final long connectionTimeoutMs;
    private final long validationIntervalMs;
    
    private final ConcurrentLinkedQueue<PooledConnection> availableConnections;
    private final AtomicInteger activeConnections;
    private final AtomicInteger totalConnections;
    private final AtomicBoolean initialized;
    
    /**
     * Create a new ConnectionPool.
     * 
     * @param plugin The plugin instance
     * @param config Configuration containing database settings
     */
    public ConnectionPool(JobsAdventure plugin, FileConfiguration config) {
        this.plugin = plugin;
        
        // Try to read from main config first, then fall back to performance config
        this.enabled = config.getBoolean("database.enabled", 
                      config.getBoolean("storage.database.enabled", false));
        this.jdbcUrl = config.getString("database.url", 
                       config.getString("storage.database.url", "jdbc:mysql://localhost:3306/jobsadventure"));
        this.username = config.getString("database.username", 
                        config.getString("storage.database.username", "jobsadventure"));
        this.password = config.getString("database.password", 
                        config.getString("storage.database.password", ""));
        this.minConnections = config.getInt("database.pool.min-connections", 
                             config.getInt("storage.database.pool.min-connections", 2));
        this.maxConnections = config.getInt("database.pool.max-connections", 
                             config.getInt("storage.database.pool.max-connections", 10));
        this.connectionTimeoutMs = config.getLong("database.pool.connection-timeout-ms", 
                                   config.getLong("storage.database.pool.connection-timeout-ms", 30000));
        this.validationIntervalMs = config.getLong("database.pool.validation-interval-ms", 
                                    config.getLong("storage.database.pool.validation-interval-ms", 300000));
        
        this.availableConnections = new ConcurrentLinkedQueue<>();
        this.activeConnections = new AtomicInteger(0);
        this.totalConnections = new AtomicInteger(0);
        this.initialized = new AtomicBoolean(false);
    }
    
    /**
     * Initialize the connection pool.
     */
    public void initialize() {
        if (!enabled) {
            plugin.getLogger().info("Database connection pool disabled");
            return;
        }
        
        try {
            // Load database driver based on URL
            loadDatabaseDriver();
            
            // Create minimum connections
            for (int i = 0; i < minConnections; i++) {
                createConnection();
            }
            
            initialized.set(true);
            plugin.getLogger().info("Database connection pool initialized with " + 
                totalConnections.get() + " connections");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database connection pool", e);
        }
    }
    
    /**
     * Get a connection from the pool.
     * 
     * @return A database connection
     * @throws SQLException if no connection is available
     */
    public Connection getConnection() throws SQLException {
        if (!enabled || !initialized.get()) {
            throw new SQLException("Connection pool not available");
        }
        
        PooledConnection pooledConnection = availableConnections.poll();
        
        if (pooledConnection == null) {
            // No available connections, create new one if under limit
            if (totalConnections.get() < maxConnections) {
                pooledConnection = createConnection();
            } else {
                throw new SQLException("Maximum number of connections reached");
            }
        }
        
        // Validate connection before returning
        if (!isConnectionValid(pooledConnection)) {
            // Connection is invalid, create a new one
            closeConnection(pooledConnection);
            pooledConnection = createConnection();
        }
        
        activeConnections.incrementAndGet();
        return pooledConnection.getConnection();
    }
    
    /**
     * Return a connection to the pool.
     * 
     * @param connection The connection to return
     */
    public void returnConnection(Connection connection) {
        if (connection == null) return;
        
        try {
            // Reset connection state
            if (!connection.isClosed()) {
                connection.setAutoCommit(true);
                connection.clearWarnings();
                
                PooledConnection pooledConnection = new PooledConnection(connection);
                availableConnections.offer(pooledConnection);
                activeConnections.decrementAndGet();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error returning connection to pool", e);
            closeConnectionSafely(connection);
        }
    }
    
    /**
     * Shutdown the connection pool.
     */
    public void shutdown() {
        if (!enabled) return;
        
        initialized.set(false);
        
        // Close all available connections
        PooledConnection connection;
        while ((connection = availableConnections.poll()) != null) {
            closeConnection(connection);
        }
        
        plugin.getLogger().info("Database connection pool shutdown complete");
    }
    
    /**
     * Check if the connection pool is enabled.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled && initialized.get();
    }
    
    /**
     * Check if the connection pool is healthy.
     * 
     * @return true if healthy
     */
    public boolean isHealthy() {
        if (!enabled || !initialized.get()) return false;
        
        // Check if we have any available connections
        return totalConnections.get() > 0;
    }
    
    /**
     * Get the number of active connections.
     * 
     * @return Number of active connections
     */
    public int getActiveConnections() {
        return activeConnections.get();
    }
    
    /**
     * Get the total number of connections.
     * 
     * @return Total number of connections
     */
    public int getTotalConnections() {
        return totalConnections.get();
    }
    
    /**
     * Get the number of available connections.
     * 
     * @return Number of available connections
     */
    public int getAvailableConnections() {
        return availableConnections.size();
    }
    
    /**
     * Get pool statistics.
     * 
     * @return Map containing pool statistics
     */
    public java.util.Map<String, Object> getStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("enabled", enabled);
        stats.put("initialized", initialized.get());
        stats.put("active_connections", getActiveConnections());
        stats.put("available_connections", getAvailableConnections());
        stats.put("total_connections", getTotalConnections());
        stats.put("max_connections", maxConnections);
        stats.put("min_connections", minConnections);
        return stats;
    }
    
    private PooledConnection createConnection() throws SQLException {
        try {
            Connection connection = java.sql.DriverManager.getConnection(jdbcUrl, username, password);
            connection.setAutoCommit(true);
            
            PooledConnection pooledConnection = new PooledConnection(connection);
            totalConnections.incrementAndGet();
            
            return pooledConnection;
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database connection", e);
            throw e;
        }
    }
    
    private boolean isConnectionValid(PooledConnection pooledConnection) {
        try {
            Connection connection = pooledConnection.getConnection();
            
            // Check if connection is closed
            if (connection.isClosed()) {
                return false;
            }
            
            // Check if connection is too old
            if (System.currentTimeMillis() - pooledConnection.getCreatedTime() > validationIntervalMs) {
                return false;
            }
            
            // Test connection with a simple query
            return connection.isValid(5); // 5 second timeout
            
        } catch (SQLException e) {
            return false;
        }
    }
    
    private void closeConnection(PooledConnection pooledConnection) {
        if (pooledConnection != null) {
            closeConnectionSafely(pooledConnection.getConnection());
            totalConnections.decrementAndGet();
        }
    }
    
    private void closeConnectionSafely(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error closing connection", e);
            }
        }
    }
    
    private void loadDatabaseDriver() throws ClassNotFoundException {
        String driverClass;
        
        if (jdbcUrl.startsWith("jdbc:mysql:")) {
            driverClass = "com.mysql.cj.jdbc.Driver";
        } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            driverClass = "org.postgresql.Driver";
        } else if (jdbcUrl.startsWith("jdbc:sqlite:")) {
            driverClass = "org.sqlite.JDBC";
        } else {
            throw new ClassNotFoundException("Unsupported database URL: " + jdbcUrl);
        }
        
        Class.forName(driverClass);
        plugin.getLogger().info("Loaded database driver: " + driverClass);
    }
    
    /**
     * Wrapper class for database connections with metadata.
     */
    private static class PooledConnection {
        private final Connection connection;
        private final long createdTime;
        
        public PooledConnection(Connection connection) {
            this.connection = connection;
            this.createdTime = System.currentTimeMillis();
        }
        
        public Connection getConnection() {
            return connection;
        }
        
        public long getCreatedTime() {
            return createdTime;
        }
    }
}