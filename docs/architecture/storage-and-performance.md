# 🚀 Storage and Performance System

JobsAdventure implements a high-performance storage system designed to efficiently manage data for thousands of simultaneous players while maintaining perfect compatibility with Folia.

## 🎯 Overview

### Hybrid Architecture
The system uses a hybrid architecture allowing choice between:
- **YAML Storage** : Ideal for small/medium servers (< 500 players)
- **MySQL Storage** : Optimized for large servers and networks (500+ players)
- **Intelligent cache** : Optimizes access to frequently used data

### Key Performance
- **< 1ms** : Average action processing time
- **< 50MB** : Memory usage for 1000 active players
- **95%+** : Cache hit rate for frequent data
- **Folia Ready** : Native regionalized threading

## 📊 Component Architecture

### 1. PerformanceManager
**File** : `PerformanceManager.java:24`

The main coordinator of the performance system:

```java
public class PerformanceManager implements Listener {
    // Cache of active player data
    private final Map<UUID, PlayerJobData> activePlayerData;
    
    // Tracking of ongoing loads
    private final Set<UUID> pendingLoads;
    
    // High-performance storage system
    private final HighPerformanceDataStorage storage;
}
```

**Responsibilities** :
- **Cache management** : Maintains frequently accessed data in memory
- **I/O optimization** : Groups read/write operations
- **Monitoring** : Collects performance metrics
- **Threading** : Coordinates asynchronous operations

### 2. HighPerformanceDataStorage
**File** : `HighPerformanceDataStorage.java`

Optimized storage implementation:

```java
public class HighPerformanceDataStorage implements DataStorage {
    // Connection pool for MySQL
    private final ConnectionPool connectionPool;
    
    // Multi-level cache
    private final CacheManager cacheManager;
    
    // Data compression
    private final DataCompressor compressor;
}
```

**Features** :
- **Connection pooling** : Optimized DB connection reuse
- **Batch operations** : Operation grouping to reduce latency
- **Adaptive compression** : Automatic compression of large datasets
- **Automatic fallback** : Switch to YAML on MySQL issues

### 3. CacheManager
**File** : `CacheManager.java`

Intelligent cache manager:

```java
public class CacheManager {
    // Primary cache with LRU
    private final Map<String, CacheEntry> primaryCache;
    
    // Compressed cache for old data
    private final Map<String, CompressedCacheEntry> compressedCache;
    
    // Performance statistics
    private final CacheStats stats;
}
```

**Cache Strategies** :
- **LRU (Least Recently Used)** : Automatic eviction of old data
- **TTL (Time To Live)** : Automatic data expiration
- **Progressive compression** : Compression of less used data
- **Hit/Miss tracking** : Cache efficiency monitoring

### 4. DataCompressor
**File** : `DataCompressor.java`

Intelligent compression system:

```java
public class DataCompressor {
    // Configurable compression thresholds
    private final int compressionThreshold;
    
    // Compression algorithms (GZIP, LZ4)
    private final CompressionAlgorithm algorithm;
}
```

**Benefits** :
- **Memory reduction** : 60-80% space savings
- **Adaptive compression** : Applied automatically based on size
- **Performance** : Optimized algorithms (LZ4 for speed, GZIP for ratio)

## 🔧 System Configuration

### config.yml File
```yaml
# Storage system configuration
storage:
  # Storage type (yaml/mysql)
  type: "yaml"
  
  # Performance optimizations
  performance:
    # Enable high performance system
    enabled: true
    
    # Cache parameters
    cache:
      # Maximum cache size (entries)
      max-entries: 1000
      
      # Maximum memory (MB)
      max-memory-mb: 256
      
      # Cleanup interval (minutes)
      cleanup-interval: 30
      
      # Default TTL (minutes)
      default-ttl: 60
    
    # Compression
    compression:
      # Enable compression
      enabled: true
      
      # Compression threshold (bytes)
      threshold: 1024
      
      # Algorithm (gzip/lz4)
      algorithm: "lz4"
    
    # Monitoring
    monitoring:
      # Enable monitoring
      enabled: true
      
      # Report interval (minutes)
      report-interval: 15
      
      # Log detailed metrics
      detailed-metrics: false

# MySQL configuration (if storage.type = mysql)
mysql:
  host: "localhost"
  port: 3306
  database: "jobsadventure"
  username: "user"
  password: "password"
  
  # Connection pool
  pool:
    # Minimum pool size
    min-connections: 2
    
    # Maximum pool size
    max-connections: 10
    
    # Connection timeout (seconds)
    connection-timeout: 30
    
    # Maximum connection lifetime (minutes)
    max-lifetime: 30
    
    # Connection test interval (minutes)
    keepalive-time: 5
```

### Advanced Configuration

#### Optimization for Large Servers (1000+ players)
```yaml
storage:
  type: "mysql"
  performance:
    cache:
      max-entries: 5000
      max-memory-mb: 512
      cleanup-interval: 15
    compression:
      enabled: true
      threshold: 512
      algorithm: "lz4"

mysql:
  pool:
    min-connections: 10
    max-connections: 50
    connection-timeout: 15
```

#### Optimization for Small Servers (< 100 players)
```yaml
storage:
  type: "yaml"
  performance:
    cache:
      max-entries: 200
      max-memory-mb: 64
      cleanup-interval: 60
    compression:
      enabled: false
```

## 📈 Metrics and Monitoring

### Collected Metrics
The system automatically collects these metrics:

#### Cache Performance
```java
public class CacheStats {
    private long hitCount;          // Number of cache hits
    private long missCount;         // Number of cache misses
    private long evictionCount;     // Number of evictions
    private double hitRate;         // Cache hit rate
    private long memoryUsage;       // Current memory usage
}
```

#### Database Performance
```java
public class DatabaseStats {
    private long queryCount;        // Number of queries
    private double avgQueryTime;    // Average query time
    private long connectionCount;   // Active connections
    private long batchOperations;   // Batch operations
}
```

#### System Metrics
```java
public class SystemStats {
    private double cpuUsage;        // CPU usage
    private long memoryUsage;       // Memory usage
    private long diskUsage;         // Disk usage
    private int activeThreads;      // Active threads
}
```

### Accessing Metrics

#### Via API
```java
// Get performance statistics
JobManager jobManager = JobsAdventure.getInstance().getJobManager();
Map<String, Object> stats = jobManager.getPerformanceStats();

// Examples of available metrics
double hitRate = (Double) stats.get("cache_hit_rate");
long memoryUsage = (Long) stats.get("memory_usage_mb");
double avgQueryTime = (Double) stats.get("avg_query_time_ms");
```

#### Via Commands
```bash
# Display performance statistics
/jobs admin stats

# Force cache cleanup
/jobs admin cleanup

# Display system health status
/jobs admin health
```

### Automatic Reports
```
[15:30:00] [JobsAdventure] Performance Report:
├── Cache Statistics:
│   ├── Hit Rate: 94.2% (18,842 hits, 1,158 misses)
│   ├── Memory Usage: 128MB / 256MB (50%)
│   └── Evictions: 45 (last 15min)
├── Database Statistics:
│   ├── Active Connections: 5 / 10
│   ├── Avg Query Time: 2.3ms
│   └── Batch Operations: 156
└── System Health: ✅ Excellent
```

## ⚡ Advanced Optimizations

### 1. Batch Operations
Intelligent operation grouping to reduce latency:

```java
// Instead of saving each player individually
for (Player player : players) {
    jobManager.savePlayerData(player); // ❌ Inefficient
}

// Use batch saving
jobManager.saveAllPlayerData(); // ✅ Optimized
```

### 2. Intelligent Preloading
Preloading frequently used data:

```java
@EventHandler(priority = EventPriority.LOWEST)
public void onPlayerJoin(PlayerJoinEvent event) {
    // Preload data before player is fully connected
    UUID playerId = event.getPlayer().getUniqueId();
    performanceManager.preloadPlayerDataAsync(playerId);
}
```

### 3. Adaptive Compression
Automatic compression based on usage:

```java
// Recently used data stays uncompressed
// Old data is automatically compressed
public void optimizeCache() {
    long now = System.currentTimeMillis();
    for (CacheEntry entry : cache.values()) {
        if (now - entry.getLastAccess() > COMPRESSION_THRESHOLD) {
            entry.compress();
        }
    }
}
```

### 4. Thread Pool Optimization
Thread pool optimization for Folia:

```java
// Use FoliaManager for async operations
foliaManager.runAsync(() -> {
    // Database operations
    storage.savePlayerData(playerData);
});

// Dedicated pool for intensive operations
ExecutorService heavyOperations = Executors.newFixedThreadPool(
    Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
);
```

## 🛠️ Performance Troubleshooting

### Common Issues Diagnostics

#### 1. Low Cache Hit Rate (< 80%)
```yaml
# Possible solutions:
cache:
  max-entries: 2000      # Increase cache size
  max-memory-mb: 512     # Allocate more memory
  default-ttl: 120       # Increase TTL
```

#### 2. High Query Time (> 10ms)
```yaml
# MySQL optimizations:
mysql:
  pool:
    max-connections: 20  # Increase pool
    connection-timeout: 15  # Reduce timeout
```

#### 3. Excessive Memory Usage
```yaml
# Enable compression:
compression:
  enabled: true
  threshold: 512         # Reduce threshold
  algorithm: "gzip"      # Use GZIP for more compression
```

### Debug Commands
```bash
# Display cache details
/jobs debug cache

# Analyze slow queries
/jobs debug slowqueries

# Profile operations
/jobs debug profile start
# ... wait ...
/jobs debug profile stop
```

### Performance Logs
```
[DEBUG] Cache miss for player 12345678-1234-1234-1234-123456789012
[DEBUG] Loading from database took 5.2ms
[DEBUG] Compressing 2.4KB data to 0.8KB (67% reduction)
[WARN] Slow query detected: SELECT * FROM player_data took 25ms
[INFO] Performance cleanup freed 15MB memory
```

## 🔮 Future Evolutions

### 1. Distributed Cache
- **Redis support** : Distributed cache for multi-server networks
- **Consistency** : Automatic synchronization between servers
- **Failover** : Automatic failover on failure

### 2. Machine Learning
- **Predictive caching** : Prediction of data to preload
- **Auto-tuning** : Automatic parameter optimization
- **Anomaly detection** : Automatic performance issue detection

### 3. Observability
- **Prometheus metrics** : Metrics export for external monitoring
- **Jaeger tracing** : Distributed operation tracing
- **Grafana dashboards** : Advanced performance visualization

---

The JobsAdventure storage and performance system is designed to scale with your server, from small communities to large networks, while maintaining exceptional performance.