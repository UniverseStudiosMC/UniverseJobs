package fr.ax_dev.jobsAdventure.action;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.bonus.XpBonusManager;
import fr.ax_dev.jobsAdventure.bonus.MoneyBonusManager;
import fr.ax_dev.jobsAdventure.condition.ConditionContext;
import fr.ax_dev.jobsAdventure.condition.ConditionResult;
import fr.ax_dev.jobsAdventure.config.MessageConfig;
import fr.ax_dev.jobsAdventure.job.Job;
import fr.ax_dev.jobsAdventure.job.JobManager;
import fr.ax_dev.jobsAdventure.utils.MessageUtils;
import fr.ax_dev.jobsAdventure.utils.XpMessageSender;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;
import java.util.Set;

/**
 * Processes actions and awards XP when requirements are met.
 */
public class ActionProcessor {
    
    private final JobsAdventure plugin;
    private final JobManager jobManager;
    private final XpBonusManager bonusManager;
    private final MoneyBonusManager moneyBonusManager;
    private final XpMessageSender messageSender;
    private final ActionLimitManager limitManager;
    
    /**
     * Create a new ActionProcessor.
     * 
     * @param plugin The plugin instance
     * @param jobManager The job manager
     * @param bonusManager The XP bonus manager
     * @param moneyBonusManager The money bonus manager
     * @param messageSender The XP message sender
     * @param limitManager The action limit manager
     */
    public ActionProcessor(JobsAdventure plugin, JobManager jobManager, XpBonusManager bonusManager, MoneyBonusManager moneyBonusManager, XpMessageSender messageSender, ActionLimitManager limitManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.bonusManager = bonusManager;
        this.moneyBonusManager = moneyBonusManager;
        this.messageSender = messageSender;
        this.limitManager = limitManager;
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
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Processing action " + actionType + " for player " + player.getName());
        }
        
        // Get all jobs the player has
        Set<String> playerJobs = jobManager.getPlayerJobs(player);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Player " + player.getName() + " has jobs: " + playerJobs);
        }
        
        for (String jobId : playerJobs) {
            Job job = jobManager.getJob(jobId);
            if (job == null || !job.isEnabled()) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Job " + jobId + " is null or disabled");
                }
                continue;
            }
            
            // Process all actions of this type for the job
            List<JobAction> actions = job.getActions(actionType);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Job " + jobId + " has " + actions.size() + " actions for type " + actionType);
            }
            
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
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Checking action target: " + action.getTarget() + " against context target: " + target);
        }
        
        // Check target matching based on action type
        ActionType actionType = job.getActionTypeForAction(action);
        boolean targetMatches = false;
        
        if (actionType == ActionType.ENCHANT) {
            // Use enchantment-specific matching for ENCHANT actions
            targetMatches = action.matchesEnchantTarget(target);
        } else {
            // Use standard matching for all other action types
            targetMatches = action.matchesTarget(target, nexoBlockId);
        }
        
        if (!targetMatches) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Target mismatch - action target: " + action.getTarget() + ", context target: " + target);
            }
            return false;
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Target matched! Processing action for " + player.getName());
        }
        
        // For BLOCK_INTERACT and ENTITY_INTERACT, check if interact type matches
        if (actionType == ActionType.BLOCK_INTERACT || actionType == ActionType.ENTITY_INTERACT) {
            String eventInteractType = context.get("interact-type");
            String actionInteractType = action.getInteractType();
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Interact type check - event: " + eventInteractType + ", action: " + actionInteractType);
            }
            
            // If action specifies an interact-type but event doesn't provide one, skip
            if (actionInteractType != null && !actionInteractType.equals("RIGHT_CLICK") && eventInteractType == null) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Action requires specific interact-type (" + actionInteractType + ") but event doesn't provide interact-type info - skipping");
                }
                return false;
            }
            
            if (eventInteractType != null && actionInteractType != null && 
                !eventInteractType.equalsIgnoreCase(actionInteractType)) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Interact type mismatch: expected " + actionInteractType + 
                                          ", got " + eventInteractType);
                }
                return false;
            }
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
        
        // Award XP and money together
        double xp = action.getXp();
        double money = action.getMoney();
        
        // Apply craft multiplier if present in context
        Object craftMultiplierObj = context.get("craft_multiplier");
        if (craftMultiplierObj instanceof Integer) {
            int craftMultiplier = (Integer) craftMultiplierObj;
            xp *= craftMultiplier;
            money *= craftMultiplier;
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Applied craft multiplier " + craftMultiplier + 
                    " - XP: " + action.getXp() + " -> " + xp + 
                    ", Money: " + action.getMoney() + " -> " + money);
            }
        }
        
        if (xp > 0 || money > 0) {
            awardRewards(player, job, action, xp, money);
        }
        
        // Execute action-level message and commands (after XP award)
        executeActionEffects(player, action);
        
        return shouldCancel;
    }
    
    /**
     * Award XP and money to a player for a job action.
     * 
     * @param player The player
     * @param job The job
     * @param action The job action
     * @param xp The XP amount
     * @param money The money amount
     */
    private void awardRewards(Player player, Job job, JobAction action, double xp, double money) {
        // Check action limits first
        if (action.hasLimits()) {
            ActionLimitManager.ActionGains allowedGains = limitManager.checkAndConsumeLimit(
                player, job.getId(), action.getTarget(), xp, money);
            
            xp = allowedGains.getXp();
            money = allowedGains.getMoney();
            
            // If no gains allowed due to limits, return early
            if (!allowedGains.hasGains()) {
                return;
            }
        }
        
        double finalXp = 0.0;
        double finalMoney = 0.0;
        
        // Process XP if present
        if (xp > 0) {
            // Get current level for level cap check
            int currentLevel = jobManager.getLevel(player, job.getId());
            if (currentLevel < job.getMaxLevel()) {
                // Apply any XP multipliers
                finalXp = applyMultipliers(player, job, xp);
                
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
            }
        }
        
        // Process money if present
        if (money > 0) {
            // Apply any money multipliers
            finalMoney = applyMoneyMultipliers(player, job, money);
            
            // Apply money bonus multipliers
            double moneyBonusMultiplier = moneyBonusManager.getTotalMultiplier(player.getUniqueId(), job.getId());
            finalMoney *= moneyBonusMultiplier;
            
            // Add money to the player
            addPlayerMoney(player, finalMoney);
        }
        
        // Send combined message if either reward was given
        if (finalXp > 0 || finalMoney > 0) {
            fr.ax_dev.jobsAdventure.job.PlayerJobData playerData = jobManager.getPlayerData(player);
            messageSender.sendXpMessage(player, job, finalXp, finalMoney, playerData);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " earned " + finalXp + " XP and " + finalMoney + " money from job " + job.getId());
            }
        }
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
     * Apply money multipliers based on various factors (similar to XP multipliers).
     * 
     * @param player The player
     * @param job The job
     * @param baseMoney The base money amount
     * @return The modified money amount
     */
    private double applyMoneyMultipliers(Player player, Job job, double baseMoney) {
        double multiplier = 1.0;
        
        // Check for permission-based multipliers (same as XP)
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
        
        return baseMoney * multiplier;
    }
    
    /**
     * Add money to a player's balance.
     * This method handles integration with economy plugins like Vault.
     * 
     * @param player The player
     * @param amount The amount to add
     */
    private void addPlayerMoney(Player player, double amount) {
        // Check if Vault is available and try to use it
        if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            try {
                // Try to get Vault integration
                net.milkbowl.vault.economy.Economy economy = getVaultEconomy();
                if (economy != null) {
                    economy.depositPlayer(player, amount);
                    return;
                }
            } catch (Exception e) {
                // Vault integration failed, log and continue to fallback
                plugin.getLogger().warning("Failed to use Vault for money reward: " + e.getMessage());
            }
        }
        
        // Fallback: Use commands to give money (works with most economy plugins)
        String command = plugin.getConfig().getString("economy.money-command", "eco give {player} {amount}")
                .replace("{player}", player.getName())
                .replace("{amount}", String.valueOf(amount));
        
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
    }
    
    /**
     * Get Vault economy instance if available.
     * 
     * @return Economy instance or null
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
        // Level-up commands are now handled by LevelUpActionManager
        // This method is kept for backward compatibility but does nothing
        // The levelup-actions are configured in individual job files
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