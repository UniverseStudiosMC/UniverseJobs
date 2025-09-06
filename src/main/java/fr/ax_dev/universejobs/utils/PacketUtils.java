package fr.ax_dev.universejobs.utils;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
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
    
    // BossBar management - ultra simple
    private static final Map<UUID, BossBar> ACTIVE_BOSSBARS = new ConcurrentHashMap<>();
    private static final Map<UUID, CompletableFuture<Void>> BOSSBAR_CLEANUPS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PACKET_BOSSBAR_IDS = new ConcurrentHashMap<>();
    private static final Object BOSSBAR_LOCK = new Object();
    
    // Packet reflection cache
    private static Class<?> CLIENTBOUND_BOSS_EVENT_PACKET_CLASS;
    private static Constructor<?> BOSS_EVENT_PACKET_CONSTRUCTOR;
    private static Method SEND_PACKET_METHOD;
    private static Method GET_HANDLE_METHOD;
    private static Field CONNECTION_FIELD;
    private static Object ADD_ACTION;
    private static Object REMOVE_ACTION;
    private static Object UPDATE_HEALTH_ACTION;
    private static Object UPDATE_TITLE_ACTION;
    private static Object UPDATE_STYLE_ACTION;
    private static boolean PACKET_REFLECTION_AVAILABLE = false;
    
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
     * Uses direct packets when available, falls back to Bukkit API.
     */
    public static void sendBossBarAsync(Player player, String message, BarColor color, 
                                      BarStyle style, double progress, int durationTicks) {
        if (PACKET_REFLECTION_AVAILABLE) {
            sendBossBarPacketAsync(player, message, color, style, progress, durationTicks);
        } else {
            sendBossBarAsyncInternal(player, message, color, style, progress, durationTicks);
        }
    }
    
    /**
     * Send bossbar using direct packet approach for maximum performance.
     * Bypasses Bukkit API completely when reflection is available.
     */
    private static void sendBossBarPacketAsync(Player player, String message, BarColor color, 
                                             BarStyle style, double progress, int durationTicks) {
        if (!player.isOnline()) return;
        
        UUID playerId = player.getUniqueId();
        UUID bossBarId = PACKET_BOSSBAR_IDS.get(playerId);
        boolean isNewBossBar = (bossBarId == null);
        
        if (isNewBossBar) {
            bossBarId = UUID.randomUUID();
            PACKET_BOSSBAR_IDS.put(playerId, bossBarId);
        }
        
        try {
            if (isNewBossBar) {
                sendAddBossBarPacket(player, bossBarId, message, color, style, progress);
            } else {
                // Update existing bossbar
                sendUpdateBossBarTitlePacket(player, bossBarId, message);
                sendUpdateBossBarHealthPacket(player, bossBarId, progress);
                sendUpdateBossBarStylePacket(player, bossBarId, color, style);
                
                // Cancel old cleanup
                CompletableFuture<Void> oldCleanup = BOSSBAR_CLEANUPS.remove(playerId);
                if (oldCleanup != null) {
                    oldCleanup.cancel(true);
                }
            }
            
            // Schedule cleanup
            if (durationTicks > 0) {
                long delayMs = durationTicks * 50L;
                final UUID finalBossBarId = bossBarId;
                
                CompletableFuture<Void> cleanup = CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(delayMs);
                        
                        if (PACKET_BOSSBAR_IDS.get(playerId) == finalBossBarId) {
                            sendRemoveBossBarPacket(player, finalBossBarId);
                            PACKET_BOSSBAR_IDS.remove(playerId);
                            CumulativeGainTracker.clearGains(player);
                        }
                        BOSSBAR_CLEANUPS.remove(playerId);
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, ASYNC_EXECUTOR);
                
                BOSSBAR_CLEANUPS.put(playerId, cleanup);
            }
            
        } catch (Exception e) {
            // Fallback to Bukkit API if packet sending fails
            sendBossBarAsyncInternal(player, message, color, style, progress, durationTicks);
        }
    }
    
    /**
     * Send ADD bossbar packet directly.
     */
    private static void sendAddBossBarPacket(Player player, UUID bossBarId, String message, 
                                           BarColor color, BarStyle style, double progress) throws Exception {
        if (ADD_ACTION == null || CLIENTBOUND_BOSS_EVENT_PACKET_CLASS == null) return;
        
        // Create packet components
        Object titleComponent = createChatComponent(message);
        Object bossBarColor = convertBukkitColorToNms(color);
        Object bossBarStyle = convertBukkitStyleToNms(style);
        
        // Try different constructor signatures
        Constructor<?>[] constructors = CLIENTBOUND_BOSS_EVENT_PACKET_CLASS.getConstructors();
        Object packet = null;
        
        for (Constructor<?> constructor : constructors) {
            try {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length >= 3) {
                    // Try UUID, Action, additional params
                    if (paramTypes[0] == UUID.class) {
                        packet = constructor.newInstance(bossBarId, ADD_ACTION, titleComponent, 
                                                       (float) progress, bossBarColor, bossBarStyle, false, false, false);
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }
        
        if (packet != null) {
            sendPacketToPlayer(player, packet);
        }
    }
    
    /**
     * Send REMOVE bossbar packet directly.
     */
    private static void sendRemoveBossBarPacket(Player player, UUID bossBarId) {
        try {
            if (REMOVE_ACTION == null || CLIENTBOUND_BOSS_EVENT_PACKET_CLASS == null) return;
            
            Constructor<?>[] constructors = CLIENTBOUND_BOSS_EVENT_PACKET_CLASS.getConstructors();
            Object packet = null;
            
            for (Constructor<?> constructor : constructors) {
                try {
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    if (paramTypes.length >= 2 && paramTypes[0] == UUID.class) {
                        packet = constructor.newInstance(bossBarId, REMOVE_ACTION);
                        break;
                    }
                } catch (Exception ignored) {}
            }
            
            if (packet != null) {
                sendPacketToPlayer(player, packet);
            }
        } catch (Exception ignored) {}
    }
    
    /**
     * Send UPDATE_TITLE bossbar packet directly.
     */
    private static void sendUpdateBossBarTitlePacket(Player player, UUID bossBarId, String message) {
        try {
            if (UPDATE_TITLE_ACTION == null) return;
            
            Object titleComponent = createChatComponent(message);
            Constructor<?>[] constructors = CLIENTBOUND_BOSS_EVENT_PACKET_CLASS.getConstructors();
            Object packet = null;
            
            for (Constructor<?> constructor : constructors) {
                try {
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    if (paramTypes.length >= 3 && paramTypes[0] == UUID.class) {
                        packet = constructor.newInstance(bossBarId, UPDATE_TITLE_ACTION, titleComponent);
                        break;
                    }
                } catch (Exception ignored) {}
            }
            
            if (packet != null) {
                sendPacketToPlayer(player, packet);
            }
        } catch (Exception ignored) {}
    }
    
    /**
     * Send UPDATE_HEALTH bossbar packet directly.
     */
    private static void sendUpdateBossBarHealthPacket(Player player, UUID bossBarId, double progress) {
        try {
            if (UPDATE_HEALTH_ACTION == null) return;
            
            Constructor<?>[] constructors = CLIENTBOUND_BOSS_EVENT_PACKET_CLASS.getConstructors();
            Object packet = null;
            
            for (Constructor<?> constructor : constructors) {
                try {
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    if (paramTypes.length >= 3 && paramTypes[0] == UUID.class) {
                        packet = constructor.newInstance(bossBarId, UPDATE_HEALTH_ACTION, (float) progress);
                        break;
                    }
                } catch (Exception ignored) {}
            }
            
            if (packet != null) {
                sendPacketToPlayer(player, packet);
            }
        } catch (Exception ignored) {}
    }
    
    /**
     * Send UPDATE_STYLE bossbar packet directly.
     */
    private static void sendUpdateBossBarStylePacket(Player player, UUID bossBarId, BarColor color, BarStyle style) {
        try {
            if (UPDATE_STYLE_ACTION == null) return;
            
            Object bossBarColor = convertBukkitColorToNms(color);
            Object bossBarStyle = convertBukkitStyleToNms(style);
            
            Constructor<?>[] constructors = CLIENTBOUND_BOSS_EVENT_PACKET_CLASS.getConstructors();
            Object packet = null;
            
            for (Constructor<?> constructor : constructors) {
                try {
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    if (paramTypes.length >= 4 && paramTypes[0] == UUID.class) {
                        packet = constructor.newInstance(bossBarId, UPDATE_STYLE_ACTION, bossBarColor, bossBarStyle);
                        break;
                    }
                } catch (Exception ignored) {}
            }
            
            if (packet != null) {
                sendPacketToPlayer(player, packet);
            }
        } catch (Exception ignored) {}
    }
    
    /**
     * Send packet to player using reflection.
     */
    private static void sendPacketToPlayer(Player player, Object packet) throws Exception {
        if (GET_HANDLE_METHOD == null || SEND_PACKET_METHOD == null) {
            // Try to lazy-initialize the connection method
            Object nmsPlayer = GET_HANDLE_METHOD.invoke(player);
            
            if (CONNECTION_FIELD == null) {
                // Try common field names for the connection
                String[] connectionFieldNames = {"connection", "playerConnection", "b"};
                for (String fieldName : connectionFieldNames) {
                    try {
                        Field field = nmsPlayer.getClass().getDeclaredField(fieldName);
                        field.setAccessible(true);
                        CONNECTION_FIELD = field;
                        break;
                    } catch (Exception ignored) {}
                }
            }
            
            if (SEND_PACKET_METHOD == null && CONNECTION_FIELD != null) {
                Object connection = CONNECTION_FIELD.get(nmsPlayer);
                // Try common method names for sending packets
                String[] methodNames = {"sendPacket", "send", "a"};
                for (String methodName : methodNames) {
                    try {
                        Method method = connection.getClass().getMethod(methodName, packet.getClass().getSuperclass());
                        SEND_PACKET_METHOD = method;
                        break;
                    } catch (Exception ignored) {}
                }
            }
        }
        
        if (GET_HANDLE_METHOD != null && CONNECTION_FIELD != null && SEND_PACKET_METHOD != null) {
            Object nmsPlayer = GET_HANDLE_METHOD.invoke(player);
            Object connection = CONNECTION_FIELD.get(nmsPlayer);
            SEND_PACKET_METHOD.invoke(connection, packet);
        }
    }
    
    /**
     * Create chat component from string for modern versions.
     */
    private static Object createChatComponent(String message) throws Exception {
        // Try Paper's Component API first
        try {
            return MessageUtils.parseMessage(message);
        } catch (Exception e) {
            // Fallback to NMS component creation
            try {
                Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
                Method literalMethod = componentClass.getMethod("literal", String.class);
                return literalMethod.invoke(null, MessageUtils.colorize(message));
            } catch (Exception fallback) {
                // Last resort: return string
                return MessageUtils.colorize(message);
            }
        }
    }
    
    /**
     * Convert Bukkit BarColor to NMS equivalent.
     */
    private static Object convertBukkitColorToNms(BarColor bukkitColor) {
        try {
            // Try to find BossEvent.BossBarColor enum
            Class<?> colorClass = Class.forName("net.minecraft.world.BossEvent$BossBarColor");
            Object[] colors = colorClass.getEnumConstants();
            
            for (Object color : colors) {
                if (color.toString().equalsIgnoreCase(bukkitColor.name())) {
                    return color;
                }
            }
            
            // Default to YELLOW if not found
            for (Object color : colors) {
                if (color.toString().equalsIgnoreCase("YELLOW")) {
                    return color;
                }
            }
            
            return colors[0]; // Fallback to first available
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Convert Bukkit BarStyle to NMS equivalent.
     */
    private static Object convertBukkitStyleToNms(BarStyle bukkitStyle) {
        try {
            // Try to find BossEvent.BossBarOverlay enum
            Class<?> styleClass = Class.forName("net.minecraft.world.BossEvent$BossBarOverlay");
            Object[] styles = styleClass.getEnumConstants();
            
            String styleName = bukkitStyle.name();
            if (styleName.equals("SOLID")) styleName = "PROGRESS";
            
            for (Object style : styles) {
                if (style.toString().equalsIgnoreCase(styleName)) {
                    return style;
                }
            }
            
            return styles[0]; // Fallback to first available
        } catch (Exception e) {
            return null;
        }
    }
    
    
    /**
     * Ultra performance BossBar system - packet-based with Bukkit fallback.
     */
    private static void sendBossBarAsyncInternal(Player player, String message, BarColor color, 
                                      BarStyle style, double progress, int durationTicks) {
        if (!player.isOnline()) return;
        
        UUID playerId = player.getUniqueId();
        
        // Try packet-based approach first for maximum performance
        if (isPacketBossBarAvailable()) {
            UUID bossBarId = PACKET_BOSSBAR_IDS.get(playerId);
            
            if (bossBarId != null) {
                // Update existing packet BossBar
                try {
                    sendUpdateBossBarTitlePacket(player, bossBarId, message);
                    sendUpdateBossBarHealthPacket(player, bossBarId, (float) Math.max(0.0, Math.min(1.0, progress)));
                    sendUpdateBossBarStylePacket(player, bossBarId, color, style);
                } catch (Exception ignored) {}
                
                // Cancel old timer
                CompletableFuture<Void> oldCleanup = BOSSBAR_CLEANUPS.remove(playerId);
                if (oldCleanup != null) {
                    oldCleanup.cancel(true);
                }
            } else {
                // Create new packet BossBar
                bossBarId = UUID.randomUUID();
                try {
                    sendAddBossBarPacket(player, bossBarId, message, color, style, (float) Math.max(0.0, Math.min(1.0, progress)));
                    PACKET_BOSSBAR_IDS.put(playerId, bossBarId);
                } catch (Exception ignored) {
                    // Fallback to Bukkit API on packet failure
                    BossBar bossBar = Bukkit.createBossBar(MessageUtils.colorize(message), color, style);
                    bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                    bossBar.addPlayer(player);
                    ACTIVE_BOSSBARS.put(playerId, bossBar);
                }
            }
        } else {
            // Fallback to optimized Bukkit API
            BossBar bossBar = ACTIVE_BOSSBARS.get(playerId);
            
            if (bossBar != null) {
                // Update existing BossBar
                bossBar.setTitle(MessageUtils.colorize(message));
                bossBar.setColor(color);
                bossBar.setStyle(style);
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                
                // Cancel old timer
                CompletableFuture<Void> oldCleanup = BOSSBAR_CLEANUPS.remove(playerId);
                if (oldCleanup != null) {
                    oldCleanup.cancel(true);
                }
            } else {
                // Create new BossBar
                bossBar = Bukkit.createBossBar(MessageUtils.colorize(message), color, style);
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                bossBar.addPlayer(player);
                ACTIVE_BOSSBARS.put(playerId, bossBar);
            }
        }
        
        // Smart cleanup timer with timestamp validation
        if (durationTicks > 0) {
            long delayMs = durationTicks * 50L;
            final long creationTime = System.currentTimeMillis();
            
            final CompletableFuture<Void>[] cleanupHolder = new CompletableFuture[1];
            CompletableFuture<Void> cleanup = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(delayMs);
                    
                    // Check if this cleanup is still valid (no new BossBar created after this one)
                    CompletableFuture<Void> currentCleanup = BOSSBAR_CLEANUPS.get(playerId);
                    if (currentCleanup != cleanupHolder[0]) {
                        // A newer cleanup was scheduled, this one is obsolete
                        return;
                    }
                    
                    // Clean up packet BossBar if exists
                    UUID currentBossBarId = PACKET_BOSSBAR_IDS.get(playerId);
                    if (currentBossBarId != null) {
                        sendRemoveBossBarPacket(player, currentBossBarId);
                        PACKET_BOSSBAR_IDS.remove(playerId);
                    }
                    
                    // Clean up Bukkit BossBar if exists
                    BossBar currentBossBar = ACTIVE_BOSSBARS.get(playerId);
                    if (currentBossBar != null) {
                        try {
                            currentBossBar.removePlayer(player);
                        } catch (Exception ignored) {}
                        ACTIVE_BOSSBARS.remove(playerId);
                    }
                    
                    // Clear cumulative gains
                    CumulativeGainTracker.clearGains(player);
                    BOSSBAR_CLEANUPS.remove(playerId);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, ASYNC_EXECUTOR);
            
            cleanupHolder[0] = cleanup;
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
     */
    public static void cleanupPlayer(UUID playerId) {
        CompletableFuture<Void> cleanup = BOSSBAR_CLEANUPS.remove(playerId);
        if (cleanup != null) {
            cleanup.cancel(true);
        }
        
        // Clean up packet-based BossBar
        UUID packetBossBarId = PACKET_BOSSBAR_IDS.remove(playerId);
        if (packetBossBarId != null) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                sendRemoveBossBarPacket(player, packetBossBarId);
            }
        }
        
        // Clean up Bukkit API BossBar
        BossBar bossBar = ACTIVE_BOSSBARS.remove(playerId);
        if (bossBar != null) {
            try {
                bossBar.removeAll();
            } catch (Exception ignored) {}
        }
        
        CumulativeGainTracker.clearGains(Bukkit.getPlayer(playerId));
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
            
            // Clean up packet-based BossBar
            UUID packetBossBarId = PACKET_BOSSBAR_IDS.remove(playerId);
            if (packetBossBarId != null) {
                sendRemoveBossBarPacket(player, packetBossBarId);
            }
            
            // Remove from Bukkit API tracking
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
        BOSSBAR_CLEANUPS.values().forEach(future -> future.cancel(false));
        BOSSBAR_CLEANUPS.clear();
        
        // Clean up packet-based bossbars
        PACKET_BOSSBAR_IDS.entrySet().forEach(entry -> {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                sendRemoveBossBarPacket(player, entry.getValue());
            }
        });
        PACKET_BOSSBAR_IDS.clear();
        
        // Clean up Bukkit API bossbars
        ACTIVE_BOSSBARS.values().forEach(bar -> {
            try {
                bar.removeAll();
            } catch (Exception ignored) {}
        });
        ACTIVE_BOSSBARS.clear();
        
        CumulativeGainTracker.clearAllGains();
        
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
     * Initialize reflection for packet-based BossBar implementation.
     * Attempts to setup direct packet sending for maximum performance.
     */
    private static void initializeReflection() {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
            boolean isModern = version.contains("1_20") || version.contains("1_21") || version.contains("1_19");
            
            if (isModern) {
                initializeModernReflection();
            } else {
                initializeLegacyReflection(version);
            }
        } catch (Exception e) {
            PACKET_REFLECTION_AVAILABLE = false;
        }
    }
    
    /**
     * Initialize reflection for Paper 1.19+ with mapped names.
     */
    private static void initializeModernReflection() {
        try {
            // Try Paper/Modern approach first
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + getServerVersion() + ".entity.CraftPlayer");
            GET_HANDLE_METHOD = craftPlayerClass.getMethod("getHandle");
            
            // Try to get connection field from ServerPlayer
            Object dummyPlayer = null; // We'll need a real player to test this
            
            // Try common Paper/Spigot class names for BossEvent packet
            String[] packetNames = {
                "net.minecraft.network.protocol.game.ClientboundBossEventPacket",
                "net.minecraft.server." + getServerVersion() + ".PacketPlayOutBoss",
                "net.minecraft.server.network.protocol.game.PacketPlayOutBoss"
            };
            
            for (String packetName : packetNames) {
                try {
                    CLIENTBOUND_BOSS_EVENT_PACKET_CLASS = Class.forName(packetName);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }
            
            if (CLIENTBOUND_BOSS_EVENT_PACKET_CLASS != null) {
                // Try to find constructor and action enums
                initializePacketActions();
                PACKET_REFLECTION_AVAILABLE = true;
            }
            
        } catch (Exception e) {
            PACKET_REFLECTION_AVAILABLE = false;
        }
    }
    
    /**
     * Initialize reflection for older versions.
     */
    private static void initializeLegacyReflection(String version) {
        try {
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            GET_HANDLE_METHOD = craftPlayerClass.getMethod("getHandle");
            
            CLIENTBOUND_BOSS_EVENT_PACKET_CLASS = Class.forName("net.minecraft.server." + version + ".PacketPlayOutBoss");
            
            if (CLIENTBOUND_BOSS_EVENT_PACKET_CLASS != null) {
                initializePacketActions();
                PACKET_REFLECTION_AVAILABLE = true;
            }
            
        } catch (Exception e) {
            PACKET_REFLECTION_AVAILABLE = false;
        }
    }
    
    /**
     * Initialize packet actions and constructor.
     */
    private static void initializePacketActions() throws Exception {
        // Try to find action enums within the packet class or separate enum
        Class<?>[] innerClasses = CLIENTBOUND_BOSS_EVENT_PACKET_CLASS.getDeclaredClasses();
        Class<?> actionClass = null;
        
        for (Class<?> innerClass : innerClasses) {
            if (innerClass.isEnum() && innerClass.getSimpleName().contains("Action")) {
                actionClass = innerClass;
                break;
            }
        }
        
        if (actionClass != null) {
            Object[] actions = actionClass.getEnumConstants();
            for (Object action : actions) {
                String name = action.toString();
                switch (name) {
                    case "ADD":
                        ADD_ACTION = action;
                        break;
                    case "REMOVE":
                        REMOVE_ACTION = action;
                        break;
                    case "UPDATE_HEALTH":
                    case "UPDATE_PCT":
                        UPDATE_HEALTH_ACTION = action;
                        break;
                    case "UPDATE_TITLE":
                    case "UPDATE_NAME":
                        UPDATE_TITLE_ACTION = action;
                        break;
                    case "UPDATE_STYLE":
                    case "UPDATE_PROPERTIES":
                        UPDATE_STYLE_ACTION = action;
                        break;
                }
            }
        }
    }
    
    /**
     * Get current server version string.
     */
    private static String getServerVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
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
    
    /**
     * Check if packet-based BossBar is available and working.
     */
    public static boolean isPacketBossBarAvailable() {
        return PACKET_REFLECTION_AVAILABLE && 
               ADD_ACTION != null && 
               REMOVE_ACTION != null && 
               CLIENTBOUND_BOSS_EVENT_PACKET_CLASS != null;
    }
    
    /**
     * Get BossBar implementation status for debugging.
     */
    public static String getBossBarImplementationStatus() {
        StringBuilder status = new StringBuilder();
        status.append("BossBar Implementation Status:\n");
        status.append("- Packet Reflection Available: ").append(PACKET_REFLECTION_AVAILABLE).append("\n");
        status.append("- Server Version: ").append(getServerVersion()).append("\n");
        status.append("- ClientboundBossEventPacket: ").append(CLIENTBOUND_BOSS_EVENT_PACKET_CLASS != null ? "Found" : "Not Found").append("\n");
        status.append("- ADD Action: ").append(ADD_ACTION != null ? "Found" : "Not Found").append("\n");
        status.append("- REMOVE Action: ").append(REMOVE_ACTION != null ? "Found" : "Not Found").append("\n");
        status.append("- UPDATE_HEALTH Action: ").append(UPDATE_HEALTH_ACTION != null ? "Found" : "Not Found").append("\n");
        status.append("- UPDATE_TITLE Action: ").append(UPDATE_TITLE_ACTION != null ? "Found" : "Not Found").append("\n");
        status.append("- Active Packet BossBars: ").append(PACKET_BOSSBAR_IDS.size()).append("\n");
        status.append("- Active Bukkit BossBars: ").append(ACTIVE_BOSSBARS.size());
        return status.toString();
    }
}