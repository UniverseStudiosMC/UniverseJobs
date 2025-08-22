package fr.ax_dev.universejobs.storage.pool;

import fr.ax_dev.universejobs.UniverseJobs;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * High-performance database connection pool.
 * Manages database connections efficiently with health monitoring.
 */
public class ConnectionPool {
    
    private final UniverseJobs plugin;
    private final boolean enabled;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int minConnections;
    private final int maxConnections;
    private final long validationIntervalMs;
    
    private final ConcurrentLinkedQueue<PooledConnection> availableConnections;
    private final AtomicInteger activeConnections;
    private final AtomicInteger totalConnections;
    private final AtomicBoolean initialized;
    
    // Performance monitoring
    private final AtomicLong totalConnectionsCreated;
    private final AtomicLong totalConnectionsDestroyed;
    private final AtomicLong totalGetConnectionCalls;
    private final AtomicLong totalConnectionWaitTime;
    private final ScheduledExecutorService healthChecker;
    
    /**
     * Create a new ConnectionPool.
     * 
     * @param plugin The plugin instance
     * @param config Configuration containing database settings
     */
    public ConnectionPool(UniverseJobs plugin, FileConfiguration config) {
        this.plugin = plugin;
        
        // Read database configuration
        this.enabled = config.getBoolean("database.enabled", false);
        
        // Build JDBC URL from host, port and prefix
        String host = config.getString("database.host", "localhost");
        String port = config.getString("database.port", "3306");
        String prefix = config.getString("database.prefix", "UniverseJobs_");
        this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + prefix;
        
        this.username = config.getString("database.username", "UniverseJobs");
        this.password = config.getString("database.password", "your_password_here");
        this.minConnections = config.getInt("database.pool.min-connections", 2);
        this.maxConnections = config.getInt("database.pool.max-connections", 10);
        config.getLong("database.pool.connection-timeout-ms", 30000);
        this.validationIntervalMs = config.getLong("database.pool.validation-interval-ms", 300000);
        
        this.availableConnections = new ConcurrentLinkedQueue<>();
        this.activeConnections = new AtomicInteger(0);
        this.totalConnections = new AtomicInteger(0);
        this.initialized = new AtomicBoolean(false);
        
        // Initialize performance monitoring
        this.totalConnectionsCreated = new AtomicLong(0);
        this.totalConnectionsDestroyed = new AtomicLong(0);
        this.totalGetConnectionCalls = new AtomicLong(0);
        this.totalConnectionWaitTime = new AtomicLong(0);
        this.healthChecker = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "UniverseJobs-ConnectionPool-HealthChecker"));
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
            
            // Start health checking
            startHealthChecker();
            
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
        
        long startTime = System.nanoTime();
        totalGetConnectionCalls.incrementAndGet();
        
        try {
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
            
        } finally {
            // Record wait time
            long waitTime = System.nanoTime() - startTime;
            totalConnectionWaitTime.addAndGet(waitTime);
        }
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
        
        // Shutdown health checker
        if (healthChecker != null && !healthChecker.isShutdown()) {
            healthChecker.shutdown();
            try {
                if (!healthChecker.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthChecker.shutdownNow();
                }
            } catch (InterruptedException e) {
                healthChecker.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Close all available connections
        PooledConnection connection;
        while ((connection = availableConnections.poll()) != null) {
            closeConnection(connection);
        }
        
        plugin.getLogger().info("Database connection pool shutdown complete. " +
                "Total connections created: " + totalConnectionsCreated.get() + 
                ", destroyed: " + totalConnectionsDestroyed.get());
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
        
        // Performance metrics
        stats.put("total_connections_created", totalConnectionsCreated.get());
        stats.put("total_connections_destroyed", totalConnectionsDestroyed.get());
        stats.put("total_get_connection_calls", totalGetConnectionCalls.get());
        
        long calls = totalGetConnectionCalls.get();
        double avgWaitTimeMs = calls > 0 ? (totalConnectionWaitTime.get() / (double) calls) / 1_000_000.0 : 0;
        stats.put("avg_connection_wait_time_ms", avgWaitTimeMs);
        
        // Connection pool efficiency
        double poolEfficiency = calls > 0 ? (double) (calls - totalConnectionsCreated.get()) / (double) calls * 100 : 0;
        stats.put("pool_efficiency_percent", poolEfficiency);
        
        return stats;
    }
    
    private PooledConnection createConnection() throws SQLException {
        try {
            Connection connection = java.sql.DriverManager.getConnection(jdbcUrl, username, password);
            connection.setAutoCommit(true);
            
            PooledConnection pooledConnection = new PooledConnection(connection);
            totalConnections.incrementAndGet();
            totalConnectionsCreated.incrementAndGet();
            
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
            totalConnectionsDestroyed.incrementAndGet();
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
     * Start the health checker to monitor connection pool health.
     */
    private void startHealthChecker() {
        healthChecker.scheduleAtFixedRate(() -> {
            try {
                // Remove stale connections
                cleanupStaleConnections();
                
                // Ensure minimum connections
                ensureMinimumConnections();
                
                // Log health status if debug is enabled
                if (plugin.getConfigManager().isDebugEnabled()) {
                    logHealthStatus();
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error in connection pool health check", e);
            }
        }, validationIntervalMs / 1000L, validationIntervalMs / 1000L, TimeUnit.SECONDS);
    }
    
    /**
     * Clean up stale connections from the pool.
     */
    private void cleanupStaleConnections() {
        int cleaned = 0;
        
        // Check all available connections
        java.util.Iterator<PooledConnection> iterator = availableConnections.iterator();
        while (iterator.hasNext()) {
            PooledConnection pooledConnection = iterator.next();
            
            if (!isConnectionValid(pooledConnection)) {
                iterator.remove();
                closeConnection(pooledConnection);
                cleaned++;
            }
        }
        
        if (cleaned > 0 && plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Cleaned up " + cleaned + " stale database connections");
        }
    }
    
    /**
     * Ensure minimum number of connections in the pool.
     */
    private void ensureMinimumConnections() {
        int currentTotal = totalConnections.get();
        int needed = minConnections - currentTotal;
        
        if (needed > 0) {
            try {
                for (int i = 0; i < needed; i++) {
                    PooledConnection connection = createConnection();
                    availableConnections.offer(connection);
                }
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Created " + needed + " new database connections to maintain minimum pool size");
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to create minimum connections", e);
            }
        }
    }
    
    /**
     * Log health status for debugging.
     */
    private void logHealthStatus() {
        java.util.Map<String, Object> stats = getStats();
        plugin.getLogger().info("Connection Pool Health: " +
                "Active=" + stats.get("active_connections") +
                ", Available=" + stats.get("available_connections") +
                ", Total=" + stats.get("total_connections") +
                ", Created=" + stats.get("total_connections_created") +
                ", Destroyed=" + stats.get("total_connections_destroyed") +
                ", Efficiency=" + String.format("%.1f%%", stats.get("pool_efficiency_percent")));
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