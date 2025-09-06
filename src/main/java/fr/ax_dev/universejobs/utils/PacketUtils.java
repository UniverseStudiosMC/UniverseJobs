package fr.ax_dev.universejobs.utils;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.Map;
import java.util.function.Consumer;

/**
 * High-performance packet-based message sender.
 * Eliminates scheduler overhead by using direct packets and async operations.
 */
public class PacketUtils {
    
    // Async executor for non-blocking operations
    private static final ScheduledExecutorService ASYNC_EXECUTOR = Executors.newScheduledThreadPool(2, r -> {
        Thread thread = new Thread(r, "UniverseJobs-PacketSender");
        thread.setDaemon(true);
        return thread;
    });
    
    // BossBar management - no scheduler tasks needed
    private static final Map<UUID, BossBar> ACTIVE_BOSSBARS = new ConcurrentHashMap<>();
    private static final Map<UUID, CompletableFuture<Void>> BOSSBAR_CLEANUPS = new ConcurrentHashMap<>();
    private static final Map<UUID, Consumer<Player>> BOSSBAR_CLEANUP_CALLBACKS = new ConcurrentHashMap<>();
    private static final Object BOSSBAR_LOCK = new Object();
    
    static {
        initializeReflection();
    }
    
    /**
     * Send actionbar message using pure async approach.
     * No scheduler tasks, immediate packet sending.
     */
    public static void sendActionBarAsync(Player player, String message, int durationTicks) {
        sendActionBarAsync(player, message, durationTicks, 20); // Default tick interval
    }
    
    /**
     * Send actionbar message with custom tick update interval.
     * Allows control over how often the actionbar updates.
     */
    public static void sendActionBarAsync(Player player, String message, int durationTicks, int tickUpdateInterval) {
        if (!player.isOnline()) return;
        
        // Send immediately using Bukkit API
        player.sendActionBar(MessageUtils.parseMessage(message));
        
        // Schedule cleanup/updates using async executor
        if (durationTicks > 0) {
            long delayMs = durationTicks * 50L; // Convert ticks to milliseconds
            long tickIntervalMs = tickUpdateInterval * 50L;
            
            if (tickUpdateInterval != 20 && tickIntervalMs < delayMs) {
                // Create a task that updates the actionbar at specified intervals
                ASYNC_EXECUTOR.schedule(() -> {
                    try {
                        long endTime = System.currentTimeMillis() + delayMs;
                        
                        while (System.currentTimeMillis() < endTime && player.isOnline()) {
                            Thread.sleep(tickIntervalMs);
                            
                            if (player.isOnline()) {
                                // Refresh the actionbar message
                                player.sendActionBar(MessageUtils.parseMessage(message));
                            } else {
                                break;
                            }
                        }
                        
                        // Final cleanup - clear actionbar
                        if (player.isOnline()) {
                            player.sendActionBar(MessageUtils.parseMessage(""));
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                }, 0, TimeUnit.MILLISECONDS);
            } else {
                // Standard cleanup without intervals
                ASYNC_EXECUTOR.schedule(() -> {
                    if (player.isOnline()) {
                        player.sendActionBar(MessageUtils.parseMessage(""));
                    }
                }, delayMs, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    /**
     * Send bossbar message using pure async approach.
     * Reuses existing bossbars, minimal object creation.
     */
    public static void sendBossBarAsync(Player player, String message, BarColor color, 
                                      BarStyle style, double progress, int durationTicks) {
        sendBossBarAsync(player, message, color, style, progress, durationTicks, 20); // Default tick interval
    }
    
    /**
     * Send bossbar message with cleanup callback.
     */
    public static void sendBossBarAsync(Player player, String message, BarColor color, 
                                      BarStyle style, double progress, int durationTicks, 
                                      int tickUpdateInterval, Consumer<Player> cleanupCallback) {
        sendBossBarAsyncInternal(player, message, color, style, progress, durationTicks, tickUpdateInterval, cleanupCallback);
    }
    
    /**
     * Send bossbar message with custom tick update interval.
     * Allows control over how often the bossbar updates.
     */
    public static void sendBossBarAsync(Player player, String message, BarColor color, 
                                      BarStyle style, double progress, int durationTicks, int tickUpdateInterval) {
        sendBossBarAsyncInternal(player, message, color, style, progress, durationTicks, tickUpdateInterval, null);
    }
    
    /**
     * Internal method for sending bossbar messages with optional cleanup callback.
     */
    private static void sendBossBarAsyncInternal(Player player, String message, BarColor color, 
                                      BarStyle style, double progress, int durationTicks, int tickUpdateInterval, 
                                      Consumer<Player> cleanupCallback) {
        if (!player.isOnline()) return;
        
        UUID playerId = player.getUniqueId();
        BossBar bossBar;
        
        synchronized (BOSSBAR_LOCK) {
            // Cancel any existing cleanup for this player
            CompletableFuture<Void> existingCleanup = BOSSBAR_CLEANUPS.remove(playerId);
            if (existingCleanup != null) {
                existingCleanup.cancel(true);
            }
            
            // Execute existing cleanup callback if any
            Consumer<Player> existingCallback = BOSSBAR_CLEANUP_CALLBACKS.remove(playerId);
            if (existingCallback != null) {
                try {
                    existingCallback.accept(player);
                } catch (Exception e) {
                    // Ignore callback errors
                }
            }
            
            // Clean up any existing bossbar first
            BossBar existingBar = ACTIVE_BOSSBARS.remove(playerId);
            if (existingBar != null) {
                try {
                    existingBar.removePlayer(player);
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
            
            // Store new cleanup callback
            if (cleanupCallback != null) {
                BOSSBAR_CLEANUP_CALLBACKS.put(playerId, cleanupCallback);
            }
            
            // Create new bossbar
            bossBar = Bukkit.createBossBar(
                MessageUtils.stripFormatting(message),
                color,
                style
            );
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            bossBar.addPlayer(player);
            ACTIVE_BOSSBARS.put(playerId, bossBar);
        }
        
        // Schedule cleanup using async executor with custom tick interval
        if (durationTicks > 0) {
            long delayMs = durationTicks * 50L;
            long tickIntervalMs = tickUpdateInterval * 50L; // Convert ticks to milliseconds
            final BossBar finalBossBar = bossBar;
            
            // If tick interval is different from default, set up periodic updates
            CompletableFuture<Void> cleanup;
            if (tickUpdateInterval != 20 && tickIntervalMs < delayMs) {
                // Create a task that updates the bossbar at specified intervals
                cleanup = CompletableFuture.runAsync(() -> {
                    try {
                        long endTime = System.currentTimeMillis() + delayMs;
                        
                        while (System.currentTimeMillis() < endTime && player.isOnline()) {
                            Thread.sleep(tickIntervalMs);
                            
                            // Update bossbar if still active
                            BossBar currentBar = ACTIVE_BOSSBARS.get(playerId);
                            if (currentBar == finalBossBar && player.isOnline()) {
                                // Refresh the bossbar (this allows dynamic progress updates)
                                currentBar.setTitle(MessageUtils.stripFormatting(message));
                                currentBar.setColor(color);
                                currentBar.setStyle(style);
                                currentBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                            } else {
                                break; // Bossbar was replaced or player disconnected
                            }
                        }
                        
                        // Final cleanup with synchronization
                        synchronized (BOSSBAR_LOCK) {
                            BossBar currentBar = ACTIVE_BOSSBARS.get(playerId);
                            if (currentBar == finalBossBar) {
                                try {
                                    currentBar.removePlayer(player);
                                    ACTIVE_BOSSBARS.remove(playerId);
                                } catch (Exception e) {
                                    // Force remove even if cleanup fails
                                    ACTIVE_BOSSBARS.remove(playerId);
                                }
                                
                                // Execute cleanup callback
                                Consumer<Player> callback = BOSSBAR_CLEANUP_CALLBACKS.remove(playerId);
                                if (callback != null) {
                                    try {
                                        callback.accept(player);
                                    } catch (Exception e) {
                                        // Ignore callback errors
                                    }
                                }
                            }
                            BOSSBAR_CLEANUPS.remove(playerId);
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                }, ASYNC_EXECUTOR);
            } else {
                // Standard cleanup without intervals (same as before)
                cleanup = CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(delayMs);
                        
                        // Remove bossbar if it's still the same instance with synchronization
                        synchronized (BOSSBAR_LOCK) {
                            BossBar currentBar = ACTIVE_BOSSBARS.get(playerId);
                            if (currentBar == finalBossBar) {
                                try {
                                    currentBar.removePlayer(player);
                                    ACTIVE_BOSSBARS.remove(playerId);
                                } catch (Exception e) {
                                    // Force remove even if cleanup fails
                                    ACTIVE_BOSSBARS.remove(playerId);
                                }
                                
                                // Execute cleanup callback
                                Consumer<Player> callback = BOSSBAR_CLEANUP_CALLBACKS.remove(playerId);
                                if (callback != null) {
                                    try {
                                        callback.accept(player);
                                    } catch (Exception e) {
                                        // Ignore callback errors
                                    }
                                }
                            }
                            BOSSBAR_CLEANUPS.remove(playerId);
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                }, ASYNC_EXECUTOR);
            }
            
            BOSSBAR_CLEANUPS.put(playerId, cleanup);
        }
    }
    
    /**
     * Send title message asynchronously.
     */
    public static void sendTitleAsync(Player player, String message, int fadeIn, int stay, int fadeOut) {
        sendTitleAsync(player, message, fadeIn, stay, fadeOut, 20); // Default tick interval
    }
    
    /**
     * Send title message with custom tick update interval.
     */
    public static void sendTitleAsync(Player player, String message, int fadeIn, int stay, int fadeOut, int tickUpdateInterval) {
        if (!player.isOnline()) return;
        
        // Send immediately using Bukkit API with proper Component types
        net.kyori.adventure.text.Component titleComponent = MessageUtils.parseMessage(message);
        net.kyori.adventure.text.Component subtitleComponent = net.kyori.adventure.text.Component.empty();
        
        player.showTitle(net.kyori.adventure.title.Title.title(
            titleComponent, 
            subtitleComponent,
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(fadeIn * 50L),
                java.time.Duration.ofMillis(stay * 50L),
                java.time.Duration.ofMillis(fadeOut * 50L)
            )
        ));
        
        // If tick interval is different from default, set up periodic updates
        if (tickUpdateInterval != 20 && stay > 0) {
            long tickIntervalMs = tickUpdateInterval * 50L;
            long stayMs = stay * 50L;
            
            if (tickIntervalMs < stayMs) {
                ASYNC_EXECUTOR.schedule(() -> {
                    try {
                        long endTime = System.currentTimeMillis() + stayMs;
                        
                        while (System.currentTimeMillis() < endTime && player.isOnline()) {
                            Thread.sleep(tickIntervalMs);
                            
                            if (player.isOnline()) {
                                // Refresh the title message
                                net.kyori.adventure.text.Component refreshedTitle = MessageUtils.parseMessage(message);
                                int remainingStay = Math.min(stay, (int)(tickIntervalMs / 50));
                                
                                player.showTitle(net.kyori.adventure.title.Title.title(
                                    refreshedTitle,
                                    net.kyori.adventure.text.Component.empty(),
                                    net.kyori.adventure.title.Title.Times.times(
                                        java.time.Duration.ofMillis(0),
                                        java.time.Duration.ofMillis(remainingStay * 50L),
                                        java.time.Duration.ofMillis(0)
                                    )
                                ));
                            } else {
                                break;
                            }
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                }, 0, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    /**
     * Send chat message asynchronously.
     */
    public static void sendChatAsync(Player player, String message) {
        if (!player.isOnline()) return;
        
        // Use async task to avoid blocking main thread
        CompletableFuture.runAsync(() -> {
            if (player.isOnline()) {
                MessageUtils.sendMessage(player, message);
            }
        }, ASYNC_EXECUTOR);
    }
    
    /**
     * Clean up all resources for a player.
     * Called when player disconnects.
     */
    public static void cleanupPlayer(UUID playerId) {
        synchronized (BOSSBAR_LOCK) {
            // Cancel any pending cleanup
            CompletableFuture<Void> cleanup = BOSSBAR_CLEANUPS.remove(playerId);
            if (cleanup != null) {
                cleanup.cancel(true);
            }
            
            // Remove cleanup callback without executing it (player disconnected)
            BOSSBAR_CLEANUP_CALLBACKS.remove(playerId);
            
            // Remove and cleanup bossbar
            BossBar bossBar = ACTIVE_BOSSBARS.remove(playerId);
            if (bossBar != null) {
                try {
                    bossBar.removeAll();
                } catch (Exception e) {
                    // Ignore cleanup errors but log for debugging
                }
            }
        }
    }
    
    /**
     * Force cleanup all bossbars for a player.
     * Use this if player experiences stuck bossbars.
     */
    public static void forceCleanupPlayerBossbars(Player player) {
        if (player == null || !player.isOnline()) return;
        
        UUID playerId = player.getUniqueId();
        synchronized (BOSSBAR_LOCK) {
            // Cancel cleanup tasks
            CompletableFuture<Void> cleanup = BOSSBAR_CLEANUPS.remove(playerId);
            if (cleanup != null) {
                cleanup.cancel(true);
            }
            
            // Remove from our tracking
            BossBar bossBar = ACTIVE_BOSSBARS.remove(playerId);
            if (bossBar != null) {
                try {
                    bossBar.removePlayer(player);
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            // Force clear any remaining bossbars by sending empty one with immediate cleanup
            try {
                BossBar clearBar = Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SOLID);
                clearBar.addPlayer(player);
                clearBar.removePlayer(player);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Shutdown all async operations.
     */
    public static void shutdown() {
        // Cancel all pending cleanups
        BOSSBAR_CLEANUPS.values().forEach(future -> future.cancel(false));
        BOSSBAR_CLEANUPS.clear();
        
        // Clear all cleanup callbacks
        BOSSBAR_CLEANUP_CALLBACKS.clear();
        
        // Clean up all bossbars
        ACTIVE_BOSSBARS.values().forEach(bar -> {
            try {
                bar.removeAll();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        });
        ACTIVE_BOSSBARS.clear();
        
        // Shutdown executor
        ASYNC_EXECUTOR.shutdown();
        try {
            if (!ASYNC_EXECUTOR.awaitTermination(2, TimeUnit.SECONDS)) {
                ASYNC_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            ASYNC_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Initialize reflection for potential packet optimizations.
     * Currently using Bukkit API which is already efficient.
     */
    private static void initializeReflection() {
        try {
            
        } catch (Exception e) {
        }
    }
    
    /**
     * Get executor for custom async operations.
     */
    public static ScheduledExecutorService getAsyncExecutor() {
        return ASYNC_EXECUTOR;
    }
    
    /**
     * Schedule a task without using Bukkit scheduler.
     */
    public static CompletableFuture<Void> runDelayed(Runnable task, long delayMs) {
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayMs);
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, ASYNC_EXECUTOR);
    }
}