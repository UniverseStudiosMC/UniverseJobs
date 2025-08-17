package fr.ax_dev.jobsAdventure.action;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.bonus.XpBonusManager;
import fr.ax_dev.jobsAdventure.condition.ConditionContext;
import fr.ax_dev.jobsAdventure.condition.ConditionResult;
import fr.ax_dev.jobsAdventure.config.MessageConfig;
import fr.ax_dev.jobsAdventure.job.Job;
import fr.ax_dev.jobsAdventure.job.JobManager;
import fr.ax_dev.jobsAdventure.utils.MessageUtils;
import fr.ax_dev.jobsAdventure.utils.XpMessageSender;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

/**
 * Processes actions and awards XP when requirements are met.
 */
public class ActionProcessor {
    
    private final JobsAdventure plugin;
    private final JobManager jobManager;
    private final XpBonusManager bonusManager;
    private final XpMessageSender messageSender;
    
    /**
     * Create a new ActionProcessor.
     * 
     * @param plugin The plugin instance
     * @param jobManager The job manager
     * @param bonusManager The XP bonus manager
     * @param messageSender The XP message sender
     */
    public ActionProcessor(JobsAdventure plugin, JobManager jobManager, XpBonusManager bonusManager, XpMessageSender messageSender) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.bonusManager = bonusManager;
        this.messageSender = messageSender;
    }
    
    /**
     * Process an action for a player.
     * 
     * @param player The player performing the action
     * @param actionType The type of action
     * @param event The event that triggered the action
     * @param context The action context
     * @return true if the event should be cancelled
     */
    public boolean processAction(Player player, ActionType actionType, Event event, ConditionContext context) {
        boolean shouldCancel = false;
        
        // Get all jobs the player has
        for (String jobId : jobManager.getPlayerJobs(player)) {
            Job job = jobManager.getJob(jobId);
            if (job == null || !job.isEnabled()) continue;
            
            // Process all actions of this type for the job
            List<JobAction> actions = job.getActions(actionType);
            for (JobAction action : actions) {
                boolean cancelThisAction = processJobAction(player, job, action, event, context);
                if (cancelThisAction) {
                    shouldCancel = true;
                }
            }
        }
        
        return shouldCancel;
    }
    
    /**
     * Process a specific job action.
     * 
     * @param player The player
     * @param job The job
     * @param action The job action
     * @param event The event
     * @param context The context
     * @return true if the event should be cancelled
     */
    private boolean processJobAction(Player player, Job job, JobAction action, Event event, ConditionContext context) {
        // Check if target matches (supports both vanilla and Nexo blocks)
        String target = context.getTarget();
        String nexoBlockId = context.getNexoBlockId();
        
        if (!action.matchesTarget(target, nexoBlockId)) {
            return false;
        }
        
        boolean shouldCancel = false;
        
        // Check requirements if they exist
        if (action.hasRequirements()) {
            ConditionResult result = action.getRequirements().evaluate(player, event, context);
            
            // Check if we should cancel the event
            if (result.shouldCancelEvent()) {
                shouldCancel = true;
            }
            
            if (!result.isAllowed()) {
                // Execute deny actions (message, sound, commands)
                result.execute(player);
                return shouldCancel;
            } else {
                // Execute accept actions (message, sound, commands)
                result.execute(player);
            }
        }
        
        // Award XP
        double xp = action.getXp();
        if (xp > 0) {
            awardXp(player, job, xp);
        }
        
        // Execute action-level message and commands (after XP award)
        executeActionEffects(player, action);
        
        return shouldCancel;
    }
    
    /**
     * Award XP to a player for a job.
     * 
     * @param player The player
     * @param job The job
     * @param xp The XP amount
     */
    private void awardXp(Player player, Job job, double xp) {
        // Get current level for level cap check
        int currentLevel = jobManager.getLevel(player, job.getId());
        if (currentLevel >= job.getMaxLevel()) {
            return; // Player is at max level
        }
        
        // Apply any XP multipliers here if needed
        double finalXp = applyMultipliers(player, job, xp);
        
        // Apply bonus multipliers
        double bonusMultiplier = bonusManager.getTotalMultiplier(player.getUniqueId(), job.getId());
        finalXp *= bonusMultiplier;
        
        // Add XP to the player
        jobManager.addXp(player, job.getId(), finalXp);
        
        // Check for level up
        int newLevel = jobManager.getLevel(player, job.getId());
        if (newLevel > currentLevel) {
            handleLevelUp(player, job, currentLevel, newLevel);
        }
        
        // Send XP gain message using the configured display method
        fr.ax_dev.jobsAdventure.job.PlayerJobData playerData = jobManager.getPlayerData(player);
        messageSender.sendXpMessage(player, job, finalXp, playerData);
    }
    
    /**
     * Apply XP multipliers based on various factors.
     * 
     * @param player The player
     * @param job The job
     * @param baseXp The base XP amount
     * @return The modified XP amount
     */
    private double applyMultipliers(Player player, Job job, double baseXp) {
        double multiplier = 1.0;
        
        // Check for permission-based multipliers
        // Skip if player is OP or has wildcard permission to avoid overpowered bonuses
        if (!player.isOp() && !player.hasPermission("*")) {
            for (int i = 10; i >= 1; i--) {
                String permission = "jobsadventure.multiplier." + i;
                // Check if player has the specific permission (not through wildcard)
                if (player.hasPermission(permission) && !hasWildcardPermission(player)) {
                    multiplier = i;
                    break;
                }
            }
        }
        
        // Could add other multipliers here:
        // - Time-based bonuses
        // - World-based bonuses
        // - Job-level bonuses
        // - Item-based bonuses
        
        return baseXp * multiplier;
    }
    
    /**
     * Handle level up events.
     * 
     * @param player The player
     * @param job The job
     * @param oldLevel The old level
     * @param newLevel The new level
     */
    private void handleLevelUp(Player player, Job job, int oldLevel, int newLevel) {
        // Send level up message
        String message = plugin.getConfig().getString("messages.level-up", 
                "&aCongratulations! You reached level {level} in {job}!")
                .replace("{level}", String.valueOf(newLevel))
                .replace("{job}", job.getName());
        
        MessageUtils.sendMessage(player, message);
        
        // Play level up sound
        try {
            String soundName = plugin.getConfig().getString("sounds.level-up", "ENTITY_PLAYER_LEVELUP");
            // Try new Registry method first
            NamespacedKey key = NamespacedKey.minecraft(soundName.toLowerCase().replace("_", "."));
            Sound sound = Registry.SOUNDS.get(key);
            if (sound == null) {
                // Fallback to valueOf for compatibility
                sound = Sound.valueOf(soundName);
            }
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
        } catch (Exception ignored) {}
        
        // Could trigger level up commands/rewards here
        executeLevelUpCommands(player, job, newLevel);
    }
    
    /**
     * Execute level up commands.
     * 
     * @param player The player
     * @param job The job
     * @param level The new level
     */
    private void executeLevelUpCommands(Player player, Job job, int level) {
        // This could be expanded to read level-specific commands from job config
        List<String> commands = plugin.getConfig().getStringList("level-up-commands");
        
        for (String command : commands) {
            String processedCommand = command
                    .replace("{player}", player.getName())
                    .replace("{job}", job.getId())
                    .replace("{level}", String.valueOf(level));
            
            plugin.getServer().dispatchCommand(
                plugin.getServer().getConsoleSender(),
                processedCommand
            );
        }
    }
    
    /**
     * Check if player has wildcard permissions.
     * 
     * @param player The player to check
     * @return true if player has wildcard permissions
     */
    private boolean hasWildcardPermission(Player player) {
        // Check for common wildcard permissions
        return player.hasPermission("*") || 
               player.hasPermission("jobsadventure.*") ||
               player.hasPermission("jobsadventure.multiplier.*");
    }
    
    /**
     * Execute action-level message and commands.
     * 
     * @param player The player
     * @param action The job action
     */
    private void executeActionEffects(Player player, JobAction action) {
        // Execute message if present
        if (action.hasMessage()) {
            MessageConfig messageConfig = action.getMessage();
            String text = messageConfig.getText();
            
            switch (messageConfig.getType()) {
                case CHAT:
                    MessageUtils.sendMessage(player, text);
                    break;
                    
                case ACTIONBAR:
                    MessageUtils.sendActionBar(player, text);
                    // Schedule clear after duration
                    if (messageConfig.getDuration() > 0) {
                        plugin.getFoliaManager().runLater(() -> {
                            if (player.isOnline()) {
                                MessageUtils.sendActionBar(player, "");
                            }
                        }, messageConfig.getDuration());
                    }
                    break;
                    
                case BOSSBAR:
                    // Use the unified message sender to avoid bossbar spam
                    messageSender.sendActionMessage(player, text, messageConfig);
                    break;
            }
        }
        
        // Execute commands if present
        if (action.hasCommands()) {
            for (String command : action.getCommands()) {
                String processedCommand = command.replace("{player}", player.getName());
                plugin.getServer().dispatchCommand(
                    plugin.getServer().getConsoleSender(),
                    processedCommand
                );
            }
        }
    }
    
    /**
     * Get the listener instance. Placeholder implementation.
     * 
     * @return null for now (not implemented yet)
     */
    public Object getListener() {
        return null; // TODO: Implement performance listener
    }
    
}