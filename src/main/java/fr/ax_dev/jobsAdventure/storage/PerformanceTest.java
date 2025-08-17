package fr.ax_dev.jobsAdventure.storage;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.job.PlayerJobData;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Performance testing utility for the data storage system.
 * Provides benchmarking capabilities to validate performance improvements.
 */
public class PerformanceTest {
    
    private final JobsAdventure plugin;
    private final HighPerformanceDataStorage storage;
    
    /**
     * Create a new PerformanceTest.
     * 
     * @param plugin The plugin instance
     */
    public PerformanceTest(JobsAdventure plugin) {
        this.plugin = plugin;
        this.storage = new HighPerformanceDataStorage(plugin);
    }
    
    /**
     * Run comprehensive performance tests.
     * 
     * @return Map containing test results
     */
    public Map<String, Object> runPerformanceTests() {
        Map<String, Object> results = new HashMap<>();
        
        plugin.getLogger().info("Starting performance tests...");
        
        try {
            // Initialize storage
            storage.initializeAsync().get(30, TimeUnit.SECONDS);
            
            // Test cache performance
            results.put("cache_test", testCachePerformance());
            
            // Test compression
            results.put("compression_test", testCompressionPerformance());
            
            // Test batch operations
            results.put("batch_test", testBatchOperations());
            
            // Test concurrent access
            results.put("concurrency_test", testConcurrentAccess());
            
            // Test memory usage
            results.put("memory_test", testMemoryUsage());
            
            plugin.getLogger().info("Performance tests completed successfully");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Performance tests failed", e);
            results.put("error", e.getMessage());
        } finally {
            // Cleanup
            try {
                storage.shutdownAsync().get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error during test cleanup", e);
            }
        }
        
        return results;
    }
    
    /**
     * Test cache performance with various scenarios.
     * 
     * @return Cache performance test results
     */
    private Map<String, Object> testCachePerformance() {
        Map<String, Object> results = new HashMap<>();
        
        try {
            // Generate test data
            int testSize = 1000;
            Map<UUID, PlayerJobData> testData = generateTestPlayerData(testSize);
            
            // Test cache writes
            long startTime = System.nanoTime();
            for (Map.Entry<UUID, PlayerJobData> entry : testData.entrySet()) {
                storage.savePlayerDataAsync(entry.getKey(), entry.getValue());
            }
            long writeTime = System.nanoTime() - startTime;
            
            // Wait for async operations to complete
            Thread.sleep(1000);
            
            // Test cache reads
            startTime = System.nanoTime();
            for (UUID playerId : testData.keySet()) {
                storage.loadPlayerDataAsync(playerId).get(1, TimeUnit.SECONDS);
            }
            long readTime = System.nanoTime() - startTime;
            
            results.put("write_time_ms", writeTime / 1_000_000.0);
            results.put("read_time_ms", readTime / 1_000_000.0);
            results.put("avg_write_time_ms", (writeTime / testSize) / 1_000_000.0);
            results.put("avg_read_time_ms", (readTime / testSize) / 1_000_000.0);
            results.put("cache_stats", storage.getCacheStats());
            
        } catch (Exception e) {
            results.put("error", e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Test compression performance and efficiency.
     * 
     * @return Compression test results
     */
    private Map<String, Object> testCompressionPerformance() {
        Map<String, Object> results = new HashMap<>();
        
        try {
            // Generate sample data
            StringBuilder sampleData = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sampleData.append("jobs:\n")
                          .append("  miner:\n")
                          .append("    xp: ").append(i * 100).append("\n")
                          .append("    level: ").append(i / 10).append("\n");
            }
            
            String testData = sampleData.toString();
            
            // Test compression
            Map<String, Object> compressionResults = DataCompressor.testCompression(testData);
            results.putAll(compressionResults);
            
            // Add compression statistics
            results.put("compression_stats", DataCompressor.getStats());
            
        } catch (Exception e) {
            results.put("error", e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Test batch operations performance.
     * 
     * @return Batch operations test results
     */
    private Map<String, Object> testBatchOperations() {
        Map<String, Object> results = new HashMap<>();
        
        try {
            int batchSize = 100;
            Map<UUID, PlayerJobData> testData = generateTestPlayerData(batchSize);
            
            // Test single operations
            long startTime = System.nanoTime();
            for (Map.Entry<UUID, PlayerJobData> entry : testData.entrySet()) {
                storage.savePlayerDataAsync(entry.getKey(), entry.getValue()).get(5, TimeUnit.SECONDS);
            }
            long singleOpTime = System.nanoTime() - startTime;
            
            // Test batch operations
            startTime = System.nanoTime();
            storage.saveBatchPlayerData(testData).get(30, TimeUnit.SECONDS);
            long batchOpTime = System.nanoTime() - startTime;
            
            results.put("single_operations_time_ms", singleOpTime / 1_000_000.0);
            results.put("batch_operation_time_ms", batchOpTime / 1_000_000.0);
            results.put("performance_improvement", ((double) singleOpTime / batchOpTime));
            results.put("batch_size", batchSize);
            
        } catch (Exception e) {
            results.put("error", e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Test concurrent access performance.
     * 
     * @return Concurrency test results
     */
    private Map<String, Object> testConcurrentAccess() {
        Map<String, Object> results = new HashMap<>();
        
        try {
            int threadCount = 10;
            int operationsPerThread = 100;
            Map<UUID, PlayerJobData> testData = generateTestPlayerData(threadCount * operationsPerThread);
            
            List<UUID> playerIds = new ArrayList<>(testData.keySet());
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            long startTime = System.nanoTime();
            
            // Create concurrent operations
            for (int i = 0; i < threadCount; i++) {
                int startIndex = i * operationsPerThread;
                int endIndex = startIndex + operationsPerThread;
                
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (int j = startIndex; j < endIndex; j++) {
                        UUID playerId = playerIds.get(j);
                        PlayerJobData data = testData.get(playerId);
                        try {
                            storage.savePlayerDataAsync(playerId, data).get(5, TimeUnit.SECONDS);
                            storage.loadPlayerDataAsync(playerId).get(5, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Concurrent operation failed", e);
                        }
                    }
                });
                
                futures.add(future);
            }
            
            // Wait for all operations to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);
            
            long totalTime = System.nanoTime() - startTime;
            
            results.put("total_time_ms", totalTime / 1_000_000.0);
            results.put("thread_count", threadCount);
            results.put("operations_per_thread", operationsPerThread);
            results.put("total_operations", threadCount * operationsPerThread * 2); // read + write
            results.put("operations_per_second", (threadCount * operationsPerThread * 2) / (totalTime / 1_000_000_000.0));
            
        } catch (Exception e) {
            results.put("error", e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Test memory usage characteristics.
     * 
     * @return Memory usage test results
     */
    private Map<String, Object> testMemoryUsage() {
        Map<String, Object> results = new HashMap<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            
            // Baseline memory
            System.gc();
            long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Load test data
            int testSize = 10000;
            Map<UUID, PlayerJobData> testData = generateTestPlayerData(testSize);
            
            // Memory after loading
            long afterLoadMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Cache all data
            storage.saveBatchPlayerData(testData).get(30, TimeUnit.SECONDS);
            
            // Memory after caching
            long afterCacheMemory = runtime.totalMemory() - runtime.freeMemory();
            
            results.put("baseline_memory_mb", baselineMemory / (1024.0 * 1024.0));
            results.put("after_load_memory_mb", afterLoadMemory / (1024.0 * 1024.0));
            results.put("after_cache_memory_mb", afterCacheMemory / (1024.0 * 1024.0));
            results.put("memory_per_player_bytes", (afterCacheMemory - baselineMemory) / testSize);
            results.put("test_data_size", testSize);
            
        } catch (Exception e) {
            results.put("error", e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Generate test player data for performance testing.
     * 
     * @param count Number of player data entries to generate
     * @return Map of test player data
     */
    private Map<UUID, PlayerJobData> generateTestPlayerData(int count) {
        Map<UUID, PlayerJobData> testData = new HashMap<>();
        
        for (int i = 0; i < count; i++) {
            UUID playerId = UUID.randomUUID();
            PlayerJobData data = new PlayerJobData(playerId);
            
            // Add some test job data
            data.joinJob("miner");
            data.addXp("miner", i * 100);
            data.joinJob("farmer");
            data.addXp("farmer", i * 50);
            
            testData.put(playerId, data);
        }
        
        return testData;
    }
    
    /**
     * Print detailed test results.
     * 
     * @param results Test results map
     */
    public void printTestResults(Map<String, Object> results) {
        plugin.getLogger().info("=== PERFORMANCE TEST RESULTS ===");
        
        for (Map.Entry<String, Object> entry : results.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                plugin.getLogger().info(key + ":");
                @SuppressWarnings("unchecked")
                Map<String, Object> subResults = (Map<String, Object>) value;
                for (Map.Entry<String, Object> subEntry : subResults.entrySet()) {
                    plugin.getLogger().info("  " + subEntry.getKey() + ": " + subEntry.getValue());
                }
            } else {
                plugin.getLogger().info(key + ": " + value);
            }
        }
        
        plugin.getLogger().info("================================");
    }
}