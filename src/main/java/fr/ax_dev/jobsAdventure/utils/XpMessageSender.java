package fr.ax_dev.jobsAdventure.utils;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.job.Job;
import fr.ax_dev.jobsAdventure.job.XpMessageSettings;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for sending XP messages using different display methods.
 */
public class XpMessageSender {
    
    private final JobsAdventure plugin;
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeActionBars = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeBossBarTasks = new HashMap<>();
    
    public XpMessageSender(JobsAdventure plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Send an XP message to a player using the job's configured display method.
     * 
     * @param player The player to send the message to
     * @param job The job that granted XP
     * @param xp The amount of XP gained
     * @param playerData The player's job data (for progress calculation)
     */
    public void sendXpMessage(Player player, Job job, double xp, fr.ax_dev.jobsAdventure.job.PlayerJobData playerData) {
        if (!plugin.getConfig().getBoolean("messages.show-xp-gain", true)) {
            return;
        }
        
        XpMessageSettings settings = job.getXpMessageSettings();
        
        // Use custom text from job settings, fallback to global config
        String messageTemplate = settings.getText();
        if (messageTemplate == null || messageTemplate.isEmpty()) {
            messageTemplate = plugin.getConfig().getString("messages.xp-gain", "&e+{xp} XP ({job})");
        }
        
        // Get current level and progress for additional placeholders
        int currentLevel = plugin.getJobManager().getLevel(player, job.getId());
        double[] progress = playerData.getXpProgress(job.getId());
        double currentXpInLevel = progress[0];
        double xpNeededForNext = progress[1];
        double progressPercent = (xpNeededForNext > 0) ? (currentXpInLevel / xpNeededForNext) * 100 : 100;
        
        String message = messageTemplate
                .replace("{xp}", String.format("%.1f", xp))
                .replace("{exp}", String.format("%.1f", xp)) // Support both {xp} and {exp}
                .replace("{job}", job.getName())
                .replace("{level}", String.valueOf(currentLevel))
                .replace("{progress}", String.format("%.1f", progressPercent))
                .replace("{current_xp}", String.format("%.1f", currentXpInLevel))
                .replace("{needed_xp}", String.format("%.1f", xpNeededForNext))
                .replace("{player}", player.getName());
        
        // Process PlaceholderAPI placeholders if available
        message = processPlaceholderAPI(player, message);
        
        switch (settings.getMessageType()) {
            case CHAT:
                sendChatMessage(player, message);
                break;
            case ACTIONBAR:
                sendActionBarMessage(player, message, settings.getActionbarDuration());
                break;
            case BOSSBAR:
                sendBossBarMessage(player, message, settings, job, playerData);
                break;
            default:
                // Fallback to actionbar
                sendActionBarMessage(player, message, settings.getActionbarDuration());
                break;
        }
    }
    
    /**
     * Send a chat message.
     */
    private void sendChatMessage(Player player, String message) {
        MessageUtils.sendMessage(player, message);
    }
    
    /**
     * Send an actionbar message with duration.
     */
    private void sendActionBarMessage(Player player, String message, int durationTicks) {
        try {
            UUID playerId = player.getUniqueId();
            
            // Cancel existing actionbar task for this player
            BukkitRunnable existingTask = activeActionBars.get(playerId);
            if (existingTask != null) {
                existingTask.cancel();
            }
            
            // Send initial actionbar message
            player.sendActionBar(MessageUtils.parseMessage(message));
            
            // Create task to clear actionbar after duration
            BukkitRunnable clearTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.sendActionBar(MessageUtils.parseMessage(""));
                    }
                    activeActionBars.remove(playerId);
                }
            };
            
            activeActionBars.put(playerId, clearTask);
            clearTask.runTaskLater(plugin, durationTicks);
            
        } catch (Exception e) {
            // Fallback to chat message
            sendChatMessage(player, message);
        }
    }
    
    /**
     * Send a bossbar message with custom settings.
     */
    private void sendBossBarMessage(Player player, String message, XpMessageSettings settings, Job job, fr.ax_dev.jobsAdventure.job.PlayerJobData playerData) {
        try {
            UUID playerId = player.getUniqueId();
            
            // Check if player already has a bossbar
            BossBar existingBar = activeBossBars.get(playerId);
            
            if (existingBar != null) {
                // Update existing bossbar with new message
                existingBar.setTitle(MessageUtils.stripFormatting(message));
                
                // Set progress based on configuration
                double progress = 1.0;
                if (settings.shouldShowProgress() && playerData != null) {
                    progress = calculateXpProgress(job.getId(), playerData);
                }
                existingBar.setProgress(progress);
                
                // Cancel existing removal task
                BukkitRunnable existingTask = activeBossBarTasks.get(playerId);
                if (existingTask != null) {
                    existingTask.cancel();
                }
                
                // Create new removal task
                BukkitRunnable newTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        BossBar currentBar = activeBossBars.get(playerId);
                        if (currentBar != null && currentBar.equals(existingBar)) {
                            currentBar.removePlayer(player);
                            activeBossBars.remove(playerId);
                            activeBossBarTasks.remove(playerId);
                        }
                    }
                };
                
                activeBossBarTasks.put(playerId, newTask);
                newTask.runTaskLater(plugin, settings.getBossbarDuration());
                
            } else {
                // Create new bossbar
                BarColor color = settings.toBukkitBarColor();
                BarStyle style = settings.toBukkitBarStyle();
                
                BossBar bossBar = Bukkit.createBossBar(
                    MessageUtils.stripFormatting(message), 
                    color, 
                    style
                );
                
                bossBar.addPlayer(player);
                
                // Set progress based on configuration
                double progress = 1.0;
                if (settings.shouldShowProgress() && playerData != null) {
                    progress = calculateXpProgress(job.getId(), playerData);
                }
                bossBar.setProgress(progress);
                
                activeBossBars.put(playerId, bossBar);
                
                // Schedule removal after duration
                BukkitRunnable removeTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        BossBar currentBar = activeBossBars.get(playerId);
                        if (currentBar != null && currentBar.equals(bossBar)) {
                            currentBar.removePlayer(player);
                            activeBossBars.remove(playerId);
                            activeBossBarTasks.remove(playerId);
                        }
                    }
                };
                
                activeBossBarTasks.put(playerId, removeTask);
                removeTask.runTaskLater(plugin, settings.getBossbarDuration());
            }
            
        } catch (Exception e) {
            // Fallback to actionbar
            sendActionBarMessage(player, message, settings.getActionbarDuration());
        }
    }
    
    /**
     * Clean up resources when a player leaves.
     * 
     * @param player The player that left
     */
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Clean up bossbar
        BossBar bossBar = activeBossBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
        
        // Clean up bossbar task
        BukkitRunnable bossBarTask = activeBossBarTasks.remove(playerId);
        if (bossBarTask != null) {
            bossBarTask.cancel();
        }
        
        // Clean up actionbar task
        BukkitRunnable actionBarTask = activeActionBars.remove(playerId);
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
    }
    
    /**
     * Clean up all resources (plugin disable).
     */
    public void cleanup() {
        // Clean up all bossbars
        for (BossBar bossBar : activeBossBars.values()) {
            bossBar.removeAll();
        }
        activeBossBars.clear();
        
        // Clean up all bossbar tasks
        for (BukkitRunnable task : activeBossBarTasks.values()) {
            task.cancel();
        }
        activeBossBarTasks.clear();
        
        // Clean up all actionbar tasks
        for (BukkitRunnable task : activeActionBars.values()) {
            task.cancel();
        }
        activeActionBars.clear();
    }
    
    /**
     * Calculate XP progress for a job (0.0 to 1.0).
     * 
     * @param jobId The job ID
     * @param playerData The player's job data
     * @return Progress value between 0.0 and 1.0
     */
    private double calculateXpProgress(String jobId, fr.ax_dev.jobsAdventure.job.PlayerJobData playerData) {
        try {
            double[] progress = playerData.getXpProgress(jobId);
            double currentXpInLevel = progress[0];
            double xpNeededForNext = progress[1];
            
            // Handle max level case
            if (xpNeededForNext <= 0) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Player at max level for job " + jobId + " - showing full bar");
                }
                return 1.0; // Max level - show full bar
            }
            
            // Ensure non-negative values
            currentXpInLevel = Math.max(0.0, currentXpInLevel);
            
            // Calculate progress percentage (0.0 to 1.0)
            double progressRatio = currentXpInLevel / xpNeededForNext;
            double clampedProgress = Math.max(0.0, Math.min(1.0, progressRatio));
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                int currentLevel = playerData.getLevel(jobId);
                plugin.getLogger().info(String.format(
                    "Job %s - Level %d: %.1f/%.1f XP (%.1f%% progress)", 
                    jobId, currentLevel, currentXpInLevel, xpNeededForNext, clampedProgress * 100
                ));
            }
            
            return clampedProgress;
            
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Failed to calculate XP progress for job " + jobId + ": " + e.getMessage());
            }
            return 1.0; // Fallback to full bar
        }
    }
    
    /**
     * Send a custom action message using the same bossbar system as XP messages.
     * This ensures bossbars are merged/reused instead of spamming.
     */
    public void sendActionMessage(Player player, String message, fr.ax_dev.jobsAdventure.config.MessageConfig messageConfig) {
        String processedMessage = processPlaceholderAPI(player, message);
        
        switch (messageConfig.getType()) {
            case CHAT:
                sendChatMessage(player, processedMessage);
                break;
            case ACTIONBAR:
                sendActionBarMessage(player, processedMessage, messageConfig.getDuration());
                break;
            case BOSSBAR:
                sendCustomBossBarMessage(player, processedMessage, messageConfig);
                break;
            default:
                sendActionBarMessage(player, processedMessage, messageConfig.getDuration());
                break;
        }
    }
    
    /**
     * Send a custom bossbar message using the same fusion system as XP bossbars.
     */
    private void sendCustomBossBarMessage(Player player, String message, fr.ax_dev.jobsAdventure.config.MessageConfig messageConfig) {
        try {
            UUID playerId = player.getUniqueId();
            
            // Check if player already has a bossbar
            BossBar existingBar = activeBossBars.get(playerId);
            
            if (existingBar != null) {
                // Update existing bossbar with new message
                existingBar.setTitle(MessageUtils.stripFormatting(message));
                existingBar.setColor(messageConfig.getBossbarColor());
                existingBar.setStyle(messageConfig.getBossbarStyle());
                existingBar.setProgress(1.0); // Action messages show full progress
                
                // Cancel existing removal task
                BukkitRunnable existingTask = activeBossBarTasks.get(playerId);
                if (existingTask != null) {
                    existingTask.cancel();
                }
                
                // Create new removal task
                BukkitRunnable newTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        BossBar currentBar = activeBossBars.get(playerId);
                        if (currentBar != null && currentBar.equals(existingBar)) {
                            currentBar.removePlayer(player);
                            activeBossBars.remove(playerId);
                            activeBossBarTasks.remove(playerId);
                        }
                    }
                };
                
                newTask.runTaskLater(plugin, messageConfig.getDuration());
                activeBossBarTasks.put(playerId, newTask);
                
            } else {
                // Create new bossbar
                BossBar newBar = Bukkit.createBossBar(
                    MessageUtils.stripFormatting(message),
                    messageConfig.getBossbarColor(),
                    messageConfig.getBossbarStyle()
                );
                newBar.setProgress(1.0);
                newBar.addPlayer(player);
                activeBossBars.put(playerId, newBar);
                
                // Remove after duration
                BukkitRunnable removalTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        BossBar currentBar = activeBossBars.get(playerId);
                        if (currentBar != null && currentBar.equals(newBar)) {
                            currentBar.removePlayer(player);
                            activeBossBars.remove(playerId);
                            activeBossBarTasks.remove(playerId);
                        }
                    }
                };
                
                removalTask.runTaskLater(plugin, messageConfig.getDuration());
                activeBossBarTasks.put(playerId, removalTask);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send custom bossbar message: " + e.getMessage());
            // Fallback to chat
            sendChatMessage(player, message);
        }
    }
    
    /**
     * Process PlaceholderAPI placeholders if the plugin is available.
     * 
     * @param player The player for context
     * @param message The message to process
     * @return The processed message
     */
    private String processPlaceholderAPI(Player player, String message) {
        // Check if PlaceholderAPI is available
        if (!plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return message;
        }
        
        try {
            // Use PlaceholderAPI to process placeholders
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message);
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Failed to process PlaceholderAPI placeholders: " + e.getMessage());
            }
            return message; // Return original message if processing fails
        }
    }
}