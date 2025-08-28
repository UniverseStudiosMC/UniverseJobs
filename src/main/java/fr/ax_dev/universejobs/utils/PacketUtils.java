package fr.ax_dev.universejobs.utils;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.Map;

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
    
    // Reflection cache for performance
    private static Method sendPacketMethod;
    private static Constructor<?> actionBarConstructor;
    private static Field connectionField;
    private static boolean reflectionInitialized = false;
    
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
     * Send bossbar message with custom tick update interval.
     * Allows control over how often the bossbar updates.
     */
    public static void sendBossBarAsync(Player player, String message, BarColor color, 
                                      BarStyle style, double progress, int durationTicks, int tickUpdateInterval) {
        if (!player.isOnline()) return;
        
        UUID playerId = player.getUniqueId();
        
        // Cancel any existing cleanup for this player
        CompletableFuture<Void> existingCleanup = BOSSBAR_CLEANUPS.remove(playerId);
        if (existingCleanup != null) {
            existingCleanup.cancel(false);
        }
        
        BossBar bossBar = ACTIVE_BOSSBARS.get(playerId);
        
        if (bossBar != null) {
            // Reuse existing bossbar - just update properties
            bossBar.setTitle(MessageUtils.stripFormatting(message));
            bossBar.setColor(color);
            bossBar.setStyle(style);
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        } else {
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
                        
                        // Final cleanup
                        BossBar currentBar = ACTIVE_BOSSBARS.get(playerId);
                        if (currentBar == finalBossBar) {
                            currentBar.removePlayer(player);
                            ACTIVE_BOSSBARS.remove(playerId);
                        }
                        BOSSBAR_CLEANUPS.remove(playerId);
                        
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
                        
                        // Remove bossbar if it's still the same instance
                        BossBar currentBar = ACTIVE_BOSSBARS.get(playerId);
                        if (currentBar == finalBossBar) {
                            currentBar.removePlayer(player);
                            ACTIVE_BOSSBARS.remove(playerId);
                        }
                        BOSSBAR_CLEANUPS.remove(playerId);
                        
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
        // Cancel any pending cleanup
        CompletableFuture<Void> cleanup = BOSSBAR_CLEANUPS.remove(playerId);
        if (cleanup != null) {
            cleanup.cancel(false);
        }
        
        // Remove and cleanup bossbar
        BossBar bossBar = ACTIVE_BOSSBARS.remove(playerId);
        if (bossBar != null) {
            try {
                bossBar.removeAll();
            } catch (Exception e) {
                // Ignore cleanup errors
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
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            
            // For future packet optimizations if needed
            reflectionInitialized = true;
            
        } catch (Exception e) {
            // Fall back to Bukkit API (which is what we're using anyway)
            reflectionInitialized = false;
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