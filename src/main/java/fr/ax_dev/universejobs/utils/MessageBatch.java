package fr.ax_dev.universejobs.utils;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.compatibility.FoliaCompatibilityManager;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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

    // Per-player processors (avoids GlobalRegionScheduler on Folia)
    private final Map<UUID, WrappedTask> playerProcessors = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastProcessTimes = new ConcurrentHashMap<>();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    
    public MessageBatch(UniverseJobs plugin) {
        this.plugin = plugin;
        this.foliaManager = plugin.getFoliaManager();
    }
    
    /**
     * Queue a message for batched sending.
     */
    public void queueMessage(Player player, MessageType type, String content, int duration) {
        if (shutdown.get()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        
        PendingMessage message = new PendingMessage(type, content, duration, System.currentTimeMillis());
        
        pendingMessages.computeIfAbsent(playerId, k -> new ConcurrentLinkedQueue<>()).offer(message);

        ensurePlayerProcessor(player);
    }
    
    /**
     * Ensure a per-player batch processor exists.
     */
    private void ensurePlayerProcessor(Player player) {
        UUID playerId = player.getUniqueId();
        playerProcessors.computeIfAbsent(playerId, id -> {
            lastProcessTimes.put(id, System.currentTimeMillis());

            return foliaManager.runTimerAtEntity(player, () -> {
                if (shutdown.get()) {
                    stopPlayerProcessor(id);
                    return;
                }

                if (!player.isOnline()) {
                    stopPlayerProcessor(id);
                    return;
                }

                Queue<PendingMessage> messages = pendingMessages.get(id);
                if (messages == null || messages.isEmpty()) {
                    long last = lastProcessTimes.getOrDefault(id, 0L);
                    if (System.currentTimeMillis() - last > 10000L) {
                        stopPlayerProcessor(id);
                    }
                    return;
                }

                processPlayerBatch(player, messages);
                lastProcessTimes.put(id, System.currentTimeMillis());
            }, 2L, 2L);
        });
    }

    private void processPlayerBatch(Player player, Queue<PendingMessage> messages) {
        // Process only the most recent message of each type to avoid spam
        PendingMessage latestActionBar = null;
        PendingMessage latestBossBar = null;

        while (true) {
            PendingMessage message = messages.poll();
            if (message == null) break;
            switch (message.type) {
                case ACTIONBAR -> latestActionBar = message;
                case BOSSBAR -> latestBossBar = message;
                case CHAT -> MessageUtils.sendMessage(player, message.content);
            }
        }

        if (latestActionBar != null) {
            sendActionBarBatched(player, latestActionBar);
        }

        if (latestBossBar != null) {
            sendBossBarBatched(player, latestBossBar);
        }

        if (messages.isEmpty()) {
            pendingMessages.remove(player.getUniqueId());
        }
    }
    
    /**
     * Send actionbar message with minimal scheduler overhead.
     */
    private void sendActionBarBatched(Player player, PendingMessage message) {
        if (!player.isOnline()) {
            return;
        }

        player.sendActionBar(MessageUtils.parseMessage(message.content));

        if (message.duration > 0) {
            foliaManager.runLaterAtEntity(player, () -> {
                if (player.isOnline()) {
                    player.sendActionBar(MessageUtils.parseMessage(""));
                }
            }, message.duration);
        }
    }
    
    /**
     * Send bossbar message with minimal scheduler overhead.
     */
    private void sendBossBarBatched(Player player, PendingMessage message) {
        // Delegate to XpMessageSender for bossbar management
        // This could be optimized further by batching bossbar updates too
        if (player.isOnline()) {
            plugin.getXpMessageSender().sendProgressUpdate(player, message.content, 1.0, message.duration);
        }
    }

    private void stopPlayerProcessor(UUID playerId) {
        WrappedTask task = playerProcessors.remove(playerId);
        if (task != null) {
            foliaManager.cancelTask(task);
        }
        lastProcessTimes.remove(playerId);
        pendingMessages.remove(playerId);
    }
    
    /**
     * Clean up resources for a player.
     */
    public void cleanupPlayer(UUID playerId) {
        stopPlayerProcessor(playerId);
    }
    
    /**
     * Shutdown the batch processor.
     */
    public void shutdown() {
        shutdown.set(true);
        for (UUID playerId : java.util.List.copyOf(playerProcessors.keySet())) {
            stopPlayerProcessor(playerId);
        }
        pendingMessages.clear();
    }
    
    public enum MessageType {
        CHAT, ACTIONBAR, BOSSBAR
    }
    
    private record PendingMessage(MessageType type, String content, int duration, long timestamp) {}
}
