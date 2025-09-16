package fr.ax_dev.universejobs.rewards;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.JobManager;
import fr.ax_dev.universejobs.job.PlayerJobData;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * High-performance reward batching system.
 * Accumulates rewards and processes them in batches for massive performance gains.
 */
public class BatchedRewardManager {
    
    private final UniverseJobs plugin;
    private final JobManager jobManager;
    private final Economy economy;
    
    // Batch accumulation maps
    private final Map<String, BatchedReward> xpBatch = new ConcurrentHashMap<>();
    private final Map<UUID, Double> moneyBatch = new ConcurrentHashMap<>();
    private final Map<String, BatchedCommand> commandBatch = new ConcurrentHashMap<>();
    
    // Batch timers
    private ScheduledExecutorService batchExecutor = Executors.newScheduledThreadPool(1, r -> {
        Thread thread = new Thread(r, "UniverseJobs-BatchProcessor");
        thread.setDaemon(true);
        return thread;
    });
    
    // Configuration
    private int xpBatchTicks;
    private int moneyBatchTicks;
    private int othersBatchTicks;
    
    // Last flush timestamps
    private long lastXpFlush = System.currentTimeMillis();
    private long lastMoneyFlush = System.currentTimeMillis();
    private long lastOthersFlush = System.currentTimeMillis();
    
    public BatchedRewardManager(UniverseJobs plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.economy = getVaultEconomy();
        
        // Load configuration
        this.xpBatchTicks = plugin.getConfig().getInt("performance.batching-xp", 60);
        this.moneyBatchTicks = plugin.getConfig().getInt("performance.batching-money", 60);
        this.othersBatchTicks = plugin.getConfig().getInt("performance.batching-others", 40);
        
        // Start batch processors
        startBatchProcessors();
    }
    
    /**
     * Add XP to batch for later processing.
     */
    public void batchXp(Player player, String jobId, double xp) {
        if (xpBatchTicks <= 0) {
            // Batching disabled, process immediately
            processXpImmediate(player, jobId, xp);
            return;
        }
        
        String key = player.getUniqueId() + ":" + jobId;
        xpBatch.compute(key, (k, existing) -> {
            if (existing == null) {
                return new BatchedReward(player.getUniqueId(), player.getName(), jobId, xp);
            }
            existing.addAmount(xp);
            return existing;
        });
        
        // Check if we should flush
        checkXpFlush();
    }
    
    /**
     * Add money to batch for later processing.
     */
    public void batchMoney(Player player, double money) {
        if (moneyBatchTicks <= 0) {
            // Batching disabled, process immediately
            processMoneyImmediate(player, money);
            return;
        }
        
        moneyBatch.merge(player.getUniqueId(), money, Double::sum);
        
        // Check if we should flush
        checkMoneyFlush();
    }
    
    /**
     * Add command to batch for later processing.
     */
    public void batchCommand(String command, int delay) {
        if (othersBatchTicks <= 0) {
            // Batching disabled, process immediately
            Bukkit.getScheduler().runTaskLater(plugin, () -> 
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command), delay);
            return;
        }
        
        String key = command + ":" + delay;
        commandBatch.compute(key, (k, existing) -> {
            if (existing == null) {
                return new BatchedCommand(command, delay);
            }
            existing.incrementCount();
            return existing;
        });
        
        // Check if we should flush
        checkOthersFlush();
    }
    
    /**
     * Process XP immediately (no batching).
     */
    private void processXpImmediate(Player player, String jobId, double xp) {
        PlayerJobData data = jobManager.getPlayerData(player.getUniqueId());
        if (data != null) {
            data.addXp(jobId, xp);
            jobManager.savePlayerData(player.getUniqueId());
        }
    }
    
    /**
     * Process money immediately (no batching).
     */
    private void processMoneyImmediate(Player player, double money) {
        if (economy != null && money != 0) {
            if (money > 0) {
                economy.depositPlayer(player, money);
            } else {
                economy.withdrawPlayer(player, Math.abs(money));
            }
        }
    }
    
    /**
     * Check if XP batch should be flushed.
     */
    private void checkXpFlush() {
        long currentTime = System.currentTimeMillis();
        long timeSinceFlush = currentTime - lastXpFlush;
        long flushInterval = xpBatchTicks * 50L; // Convert ticks to milliseconds
        
        if (timeSinceFlush >= flushInterval) {
            flushXpBatch();
            lastXpFlush = currentTime;
        }
    }
    
    /**
     * Check if money batch should be flushed.
     */
    private void checkMoneyFlush() {
        long currentTime = System.currentTimeMillis();
        long timeSinceFlush = currentTime - lastMoneyFlush;
        long flushInterval = moneyBatchTicks * 50L;
        
        if (timeSinceFlush >= flushInterval) {
            flushMoneyBatch();
            lastMoneyFlush = currentTime;
        }
    }
    
    /**
     * Check if others batch should be flushed.
     */
    private void checkOthersFlush() {
        long currentTime = System.currentTimeMillis();
        long timeSinceFlush = currentTime - lastOthersFlush;
        long flushInterval = othersBatchTicks * 50L;
        
        if (timeSinceFlush >= flushInterval) {
            flushCommandBatch();
            lastOthersFlush = currentTime;
        }
    }
    
    /**
     * Flush all XP in batch.
     */
    public void flushXpBatch() {
        if (xpBatch.isEmpty()) return;
        
        Map<String, BatchedReward> toProcess = new ConcurrentHashMap<>(xpBatch);
        xpBatch.clear();
        
        // Process on main thread for thread safety
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (BatchedReward reward : toProcess.values()) {
                PlayerJobData data = jobManager.getPlayerData(reward.playerUuid);
                if (data != null) {
                    data.addXp(reward.jobId, reward.amount);
                    
                    // Log batch processing if debug
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("[BATCH] Processed " + reward.amount + " XP for " + 
                            reward.playerName + " in job " + reward.jobId);
                    }
                }
            }
            
            // Save all at once
            toProcess.values().stream()
                .map(r -> r.playerUuid)
                .distinct()
                .forEach(uuid -> jobManager.savePlayerData(uuid));
        });
    }
    
    /**
     * Flush all money in batch.
     */
    public void flushMoneyBatch() {
        if (moneyBatch.isEmpty()) return;
        
        Map<UUID, Double> toProcess = new ConcurrentHashMap<>(moneyBatch);
        moneyBatch.clear();
        
        // Process on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (economy != null) {
                for (Map.Entry<UUID, Double> entry : toProcess.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        double amount = entry.getValue();
                        if (amount > 0) {
                            economy.depositPlayer(player, amount);
                        } else if (amount < 0) {
                            economy.withdrawPlayer(player, Math.abs(amount));
                        }
                        
                        // Log batch processing if debug
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("[BATCH] Processed $" + entry.getValue() + 
                                " for " + player.getName());
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Flush all commands in batch.
     */
    public void flushCommandBatch() {
        if (commandBatch.isEmpty()) return;
        
        Map<String, BatchedCommand> toProcess = new ConcurrentHashMap<>(commandBatch);
        commandBatch.clear();
        
        // Process commands
        for (BatchedCommand cmd : toProcess.values()) {
            for (int i = 0; i < cmd.count; i++) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> 
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.command), 
                    cmd.delay + (i * 2)); // Slight delay between multiple same commands
            }
        }
    }
    
    /**
     * Start batch processors with configured intervals.
     */
    private void startBatchProcessors() {
        // XP batch processor
        if (xpBatchTicks > 0) {
            long xpInterval = xpBatchTicks * 50L;
            batchExecutor.scheduleAtFixedRate(this::flushXpBatch, 
                xpInterval, xpInterval, TimeUnit.MILLISECONDS);
        }
        
        // Money batch processor
        if (moneyBatchTicks > 0) {
            long moneyInterval = moneyBatchTicks * 50L;
            batchExecutor.scheduleAtFixedRate(this::flushMoneyBatch, 
                moneyInterval, moneyInterval, TimeUnit.MILLISECONDS);
        }
        
        // Commands batch processor
        if (othersBatchTicks > 0) {
            long othersInterval = othersBatchTicks * 50L;
            batchExecutor.scheduleAtFixedRate(this::flushCommandBatch, 
                othersInterval, othersInterval, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Flush all batches and shutdown.
     */
    public void shutdown() {
        // Flush all pending batches
        flushXpBatch();
        flushMoneyBatch();
        flushCommandBatch();
        
        // Shutdown executor
        batchExecutor.shutdown();
        try {
            if (!batchExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                batchExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            batchExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Force flush all batches immediately.
     */
    public void forceFlushAll() {
        flushXpBatch();
        flushMoneyBatch();
        flushCommandBatch();
    }
    
    /**
     * Reload configuration and restart batch processors.
     */
    public void reloadConfig() {
        // Get new configuration values
        int newXpBatchTicks = plugin.getConfig().getInt("performance.batching-xp", 60);
        int newMoneyBatchTicks = plugin.getConfig().getInt("performance.batching-money", 60);
        int newOthersBatchTicks = plugin.getConfig().getInt("performance.batching-others", 40);
        
        // Shutdown existing processors
        batchExecutor.shutdown();
        try {
            if (!batchExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                batchExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            batchExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Create new executor service
        batchExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r, "UniverseJobs-BatchProcessor");
            thread.setDaemon(true);
            return thread;
        });
        
        // Update configuration values directly
        this.xpBatchTicks = newXpBatchTicks;
        this.moneyBatchTicks = newMoneyBatchTicks;
        this.othersBatchTicks = newOthersBatchTicks;
        
        // Restart batch processors with new configuration
        startBatchProcessors();
    }
    
    /**
     * Get current batch statistics.
     */
    public BatchStatistics getStatistics() {
        return new BatchStatistics(
            xpBatch.size(),
            moneyBatch.size(),
            commandBatch.size(),
            xpBatch.values().stream().mapToDouble(r -> r.amount).sum(),
            moneyBatch.values().stream().mapToDouble(Double::doubleValue).sum()
        );
    }
    
    // Data classes
    private static class BatchedReward {
        final UUID playerUuid;
        final String playerName;
        final String jobId;
        double amount;
        
        BatchedReward(UUID playerUuid, String playerName, String jobId, double amount) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.jobId = jobId;
            this.amount = amount;
        }
        
        void addAmount(double additional) {
            this.amount += additional;
        }
    }
    
    private static class BatchedCommand {
        final String command;
        final int delay;
        int count = 1;
        
        BatchedCommand(String command, int delay) {
            this.command = command;
            this.delay = delay;
        }
        
        void incrementCount() {
            this.count++;
        }
    }
    
    /**
     * Get Vault economy instance if available.
     */
    private Economy getVaultEconomy() {
        try {
            if (plugin.getServer().getServicesManager().getRegistration(Economy.class) != null) {
                return plugin.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
            }
        } catch (Exception e) {
            // Class not found or other error
        }
        return null;
    }
    
    public static class BatchStatistics {
        public final int xpBatchSize;
        public final int moneyBatchSize;
        public final int commandBatchSize;
        public final double totalXpPending;
        public final double totalMoneyPending;
        
        BatchStatistics(int xpBatchSize, int moneyBatchSize, int commandBatchSize,
                       double totalXpPending, double totalMoneyPending) {
            this.xpBatchSize = xpBatchSize;
            this.moneyBatchSize = moneyBatchSize;
            this.commandBatchSize = commandBatchSize;
            this.totalXpPending = totalXpPending;
            this.totalMoneyPending = totalMoneyPending;
        }
    }
}