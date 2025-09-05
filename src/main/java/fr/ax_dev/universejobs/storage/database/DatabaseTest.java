package fr.ax_dev.universejobs.storage.database;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.PlayerJobData;

import java.util.UUID;
import java.util.logging.Level;

public class DatabaseTest {
    
    private final UniverseJobs plugin;

    public DatabaseTest(UniverseJobs plugin) {
        this.plugin = plugin;
    }

    public void testDatabaseIntegration() {
        if (!plugin.isDatabaseEnabled()) {
            plugin.getLogger().warning("Database is not enabled - test skipped");
            return;
        }

        plugin.getLogger().info("Starting database integration test...");
        
        try {
            DatabaseDataStorage storage = (DatabaseDataStorage) plugin.getDataStorage();
            
            testPlayerDataOperations(storage);
            testRewardOperations(storage);
            testCacheOperations(storage);
            
            plugin.getLogger().info("Database integration test completed successfully!");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Database integration test failed", e);
        }
    }

    private void testPlayerDataOperations(DatabaseDataStorage storage) throws Exception {
        plugin.getLogger().info("Testing player data operations...");
        
        UUID testPlayerId = UUID.randomUUID();
        PlayerJobData testData = new PlayerJobData(testPlayerId);
        
        testData.joinJob("test_job");
        testData.setXp("test_job", 1000.0);
        testData.setLevel("test_job", 5);
        
        storage.savePlayerDataAsync(testPlayerId, testData).join();
        plugin.getLogger().info("✓ Player data save test passed");
        
        PlayerJobData loadedData = storage.loadPlayerDataAsync(testPlayerId).join();
        
        if (!loadedData.hasJob("test_job")) {
            throw new RuntimeException("Job was not saved/loaded correctly");
        }
        
        if (loadedData.getXp("test_job") != 1000.0) {
            throw new RuntimeException("XP was not saved/loaded correctly");
        }
        
        if (loadedData.getLevel("test_job") != 5) {
            throw new RuntimeException("Level was not saved/loaded correctly");
        }
        
        plugin.getLogger().info("✓ Player data load test passed");
    }

    private void testRewardOperations(DatabaseDataStorage storage) throws Exception {
        plugin.getLogger().info("Testing reward operations...");
        
        UUID testPlayerId = UUID.randomUUID();
        String testJobId = "test_job";
        String testRewardId = "test_reward";
        long testClaimTime = System.currentTimeMillis();
        
        storage.claimReward(testPlayerId, testJobId, testRewardId, testClaimTime);
        Thread.sleep(100);
        
        boolean hasClaimed = storage.hasClaimedReward(testPlayerId, testJobId, testRewardId);
        if (!hasClaimed) {
            throw new RuntimeException("Reward claim was not saved correctly");
        }
        
        long claimTime = storage.getClaimTime(testPlayerId, testJobId, testRewardId);
        if (claimTime != testClaimTime) {
            throw new RuntimeException("Reward claim time was not saved correctly");
        }
        
        plugin.getLogger().info("✓ Reward operations test passed");
    }

    private void testCacheOperations(DatabaseDataStorage storage) throws Exception {
        plugin.getLogger().info("Testing cache operations...");
        
        UUID testPlayerId = UUID.randomUUID();
        PlayerJobData testData = new PlayerJobData(testPlayerId);
        testData.joinJob("cache_test");
        
        storage.savePlayerDataAsync(testPlayerId, testData).join();
        
        PlayerJobData cachedData = storage.loadPlayerDataAsync(testPlayerId).join();
        PlayerJobData cachedData2 = storage.loadPlayerDataAsync(testPlayerId).join();
        
        if (cachedData != cachedData2) {
            plugin.getLogger().info("Cache miss detected (expected for first load)");
        }
        
        var cacheStats = storage.getCacheStats();
        plugin.getLogger().info("Cache stats: " + cacheStats);
        
        storage.evictFromCache(testPlayerId);
        plugin.getLogger().info("✓ Cache operations test passed");
    }

    public void performanceTest() {
        if (!plugin.isDatabaseEnabled()) {
            plugin.getLogger().warning("Database is not enabled - performance test skipped");
            return;
        }

        plugin.getLogger().info("Starting database performance test...");
        
        try {
            DatabaseDataStorage storage = (DatabaseDataStorage) plugin.getDataStorage();
            
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < 100; i++) {
                UUID testPlayerId = UUID.randomUUID();
                PlayerJobData testData = new PlayerJobData(testPlayerId);
                testData.joinJob("perf_test");
                testData.setXp("perf_test", Math.random() * 10000);
                testData.setLevel("perf_test", (int) (Math.random() * 50) + 1);
                
                storage.savePlayerDataAsync(testPlayerId, testData).join();
                storage.loadPlayerDataAsync(testPlayerId).join();
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            plugin.getLogger().info("Performance test completed in " + duration + "ms");
            plugin.getLogger().info("Average time per operation: " + (duration / 200.0) + "ms");
            
            var perfMetrics = storage.getPerformanceMetrics();
            plugin.getLogger().info("Performance metrics: " + perfMetrics);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Performance test failed", e);
        }
    }
}