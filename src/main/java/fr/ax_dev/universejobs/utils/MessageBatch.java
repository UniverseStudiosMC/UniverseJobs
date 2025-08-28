package fr.ax_dev.universejobs.utils;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.compatibility.FoliaCompatibilityManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Batches messages to reduce scheduler overhead.
 * Instead of creating individual tasks for each message,
 * processes messages in batches every few ticks.
 */
public class MessageBatch {
    
    private final UniverseJobs plugin;
    private final FoliaCompatibilityManager foliaManager;
    
    // Queue of pending messages per player
    private final Map<UUID, Queue<PendingMessage>> pendingMessages = new ConcurrentHashMap<>();
    
    // Single task that processes all batched messages
    private BukkitRunnable batchProcessor;
    private boolean isRunning = false;
    
    public MessageBatch(UniverseJobs plugin) {
        this.plugin = plugin;
        this.foliaManager = plugin.getFoliaManager();
    }
    
    /**
     * Queue a message for batched sending.
     */
    public void queueMessage(Player player, MessageType type, String content, int duration) {
        UUID playerId = player.getUniqueId();
        
        PendingMessage message = new PendingMessage(type, content, duration, System.currentTimeMillis());
        
        pendingMessages.computeIfAbsent(playerId, k -> new ConcurrentLinkedQueue<>()).offer(message);
        
        // Start batch processor if not running
        if (!isRunning) {
            startBatchProcessor();
        }
    }
    
    /**
     * Start the batch processor that runs every 2 ticks (100ms).
     * This dramatically reduces scheduler overhead.
     */
    private void startBatchProcessor() {
        if (isRunning) return;
        
        isRunning = true;
        batchProcessor = new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingMessages.isEmpty()) {
                    // Stop if no messages pending for 10 seconds
                    if (System.currentTimeMillis() - lastProcessTime > 10000) {
                        isRunning = false;
                        cancel();
                        return;
                    }
                    return;
                }
                
                processBatch();
                lastProcessTime = System.currentTimeMillis();
            }
        };
        
        // Run every 2 ticks (100ms) instead of immediately for each message
        batchProcessor.runTaskTimer(plugin, 0, 2);
    }
    
    private long lastProcessTime = System.currentTimeMillis();
    
    /**
     * Process all pending messages in one go.
     */
    private void processBatch() {
        for (Map.Entry<UUID, Queue<PendingMessage>> entry : pendingMessages.entrySet()) {
            UUID playerId = entry.getKey();
            Queue<PendingMessage> messages = entry.getValue();
            
            if (messages.isEmpty()) continue;
            
            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                messages.clear();
                continue;
            }
            
            // Process only the most recent message of each type to avoid spam
            PendingMessage latestActionBar = null;
            PendingMessage latestBossBar = null;
            
            while (true) {
                final PendingMessage message = messages.poll();
                if (message == null) break;
                switch (message.type) {
                    case ACTIONBAR -> latestActionBar = message;
                    case BOSSBAR -> latestBossBar = message;
                    case CHAT -> {
                        // Send chat messages immediately
                        foliaManager.runAtEntity(player, () -> {
                            if (player.isOnline()) {
                                MessageUtils.sendMessage(player, message.content);
                            }
                        });
                    }
                }
            }
            
            // Send latest actionbar message
            if (latestActionBar != null) {
                sendActionBarBatched(player, latestActionBar);
            }
            
            // Send latest bossbar message
            if (latestBossBar != null) {
                sendBossBarBatched(player, latestBossBar);
            }
        }
        
        // Clean up empty queues
        pendingMessages.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    
    /**
     * Send actionbar message with minimal scheduler overhead.
     */
    private void sendActionBarBatched(Player player, PendingMessage message) {
        foliaManager.runAtEntity(player, () -> {
            if (player.isOnline()) {
                player.sendActionBar(MessageUtils.parseMessage(message.content));
                
                // Schedule cleanup only once per player, reusing cleanup task
                if (message.duration > 0) {
                    foliaManager.runLater(() -> {
                        foliaManager.runAtEntity(player, () -> {
                            if (player.isOnline()) {
                                player.sendActionBar(MessageUtils.parseMessage(""));
                            }
                        });
                    }, message.duration);
                }
            }
        });
    }
    
    /**
     * Send bossbar message with minimal scheduler overhead.
     */
    private void sendBossBarBatched(Player player, PendingMessage message) {
        // Delegate to XpMessageSender for bossbar management
        // This could be optimized further by batching bossbar updates too
        foliaManager.runAtEntity(player, () -> {
            if (player.isOnline()) {
                plugin.getXpMessageSender().sendProgressUpdate(player, message.content, 1.0, message.duration);
            }
        });
    }
    
    /**
     * Clean up resources for a player.
     */
    public void cleanupPlayer(UUID playerId) {
        pendingMessages.remove(playerId);
    }
    
    /**
     * Shutdown the batch processor.
     */
    public void shutdown() {
        if (batchProcessor != null) {
            batchProcessor.cancel();
        }
        pendingMessages.clear();
        isRunning = false;
    }
    
    public enum MessageType {
        CHAT, ACTIONBAR, BOSSBAR
    }
    
    private record PendingMessage(MessageType type, String content, int duration, long timestamp) {}
}