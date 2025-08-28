package fr.ax_dev.universejobs.action;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.bonus.XpBonusManager;
import fr.ax_dev.universejobs.bonus.MoneyBonusManager;
import fr.ax_dev.universejobs.condition.ConditionContext;
import fr.ax_dev.universejobs.condition.ConditionResult;
import fr.ax_dev.universejobs.config.MessageConfig;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.JobManager;
import fr.ax_dev.universejobs.utils.MessageUtils;
import fr.ax_dev.universejobs.utils.AsyncXpMessageSender;
import fr.ax_dev.universejobs.cache.ConfigurationCache;
import fr.ax_dev.universejobs.cache.PlayerJobCache;
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
    
    private static final String MATCHES_SUFFIX = ", matches: ";
    
    private final UniverseJobs plugin;
    private final JobManager jobManager;
    private final XpBonusManager bonusManager;
    private final MoneyBonusManager moneyBonusManager;
    private final AsyncXpMessageSender messageSender;
    private final ActionLimitManager limitManager;
    private final ConfigurationCache configCache;
    private final PlayerJobCache playerCache;
    
    /**
     * Create a new ActionProcessor with ultra-fast caching.
     * 
     * @param plugin The plugin instance
     * @param jobManager The job manager
     * @param bonusManager The XP bonus manager
     * @param moneyBonusManager The money bonus manager
     * @param messageSender The XP message sender
     * @param limitManager The action limit manager
     * @param configCache The configuration cache
     * @param playerCache The player cache
     */
    public ActionProcessor(UniverseJobs plugin, JobManager jobManager, XpBonusManager bonusManager, 
                          MoneyBonusManager moneyBonusManager, AsyncXpMessageSender messageSender, 
                          ActionLimitManager limitManager, ConfigurationCache configCache, 
                          PlayerJobCache playerCache) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.bonusManager = bonusManager;
        this.moneyBonusManager = moneyBonusManager;
        this.messageSender = messageSender;
        this.limitManager = limitManager;
        this.configCache = configCache;
        this.playerCache = playerCache;
    }
    
    /**
     * Process an action for a player with ULTRA-FAST cache lookup.
     * 
     * @param player The player performing the action
     * @param actionType The type of action
     * @param event The event that triggered the action
     * @param context The action context
     * @return true if the event should be cancelled
     */
    public boolean processAction(Player player, ActionType actionType, Event event, ConditionContext context) {
        // Rate limiting check (ultra-fast array lookup)
        if (!playerCache.checkRateLimit(player.getUniqueId(), configCache.getActionCooldownMs())) {
            return false;
        }
        
        // Skip debug si désactivé (cache lookup instantané)
        if (configCache.isDebugEnabled()) {
            plugin.getLogger().info("Processing action " + actionType + " for player " + player.getName());
        }
        
        // Lookup instantané des jobs (cache pré-chargé)
        Set<String> playerJobs = playerCache.getPlayerJobs(player.getUniqueId());
        if (playerJobs.isEmpty()) {
            if (configCache.isDebugEnabled()) {
                plugin.getLogger().info("DEBUG: Player " + player.getName() + " has no jobs! Available jobs in cache: " + 
                    playerCache.getStats().getOrDefault("cached_players", "0"));
            }
            return false;
        }
        
        if (configCache.isDebugEnabled()) {
            plugin.getLogger().info("DEBUG: Player " + player.getName() + " has jobs: " + playerJobs);
        }
        
        // Process en parallèle pour performance maximale
        return processJobsAsync(player, playerJobs, actionType, event, context);
    }
    
    /**
     * Process jobs de manière asynchrone pour performance maximale.
     */
    private boolean processJobsAsync(Player player, Set<String> playerJobs, ActionType actionType, Event event, ConditionContext context) {
        boolean shouldCancel = false;
        
        // Si debug désactivé, process direct sans logging
        if (!configCache.isDebugEnabled()) {
            for (String jobId : playerJobs) {
                if (processPlayerJobFast(player, jobId, actionType, event, context)) {
                    shouldCancel = true;
                }
            }
        } else {
            // Mode debug avec logging
            for (String jobId : playerJobs) {
                if (processPlayerJob(player, jobId, actionType, event, context)) {
                    shouldCancel = true;
                }
            }
        }
        
        return shouldCancel;
    }
    
    /**
     * Process actions for a specific job.
     */
    private boolean processPlayerJob(Player player, String jobId, ActionType actionType, Event event, ConditionContext context) {
        Job job = jobManager.getJob(jobId);
        if (!isJobValid(job, jobId)) {
            return false;
        }
        
        List<JobAction> actions = job.getActions(actionType);
        debugLog("Job " + jobId + " has " + actions.size() + " actions for type " + actionType);
        
        boolean shouldCancel = false;
        for (JobAction action : actions) {
            if (processJobAction(player, job, action, event, context)) {
                shouldCancel = true;
            }
        }
        return shouldCancel;
    }
    
    /**
     * Check if a job is valid and enabled.
     */
    private boolean isJobValid(Job job, String jobId) {
        if (job == null || !job.isEnabled()) {
            debugLog("Job " + jobId + " is null or disabled");
            return false;
        }
        return true;
    }
    
    /**
     * Log debug message avec cache instantané.
     */
    private void debugLog(String message) {
        if (configCache.isDebugEnabled()) {
            plugin.getLogger().info(message);
        }
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
        if (!validateActionTarget(action, context, job)) {
            return false;
        }
        
        if (!validateInteractType(action, context, job)) {
            return false;
        }
        
        if (!validateProfession(action, context, job)) {
            return false;
        }
        
        if (!validateColor(action, context, job)) {
            return false;
        }
        
        if (!validateNbt(action, context, job)) {
            return false;
        }
        
        if (!validatePotionType(action, context, job)) {
            return false;
        }
        
        boolean shouldCancel = processActionRequirements(player, action, event, context);
        
        processActionRewards(player, job, action, context);
        executeActionEffects(player, action);
        
        return shouldCancel;
    }
    
    /**
     * Version ultra-rapide sans debug logging.
     */
    private boolean processPlayerJobFast(Player player, String jobId, ActionType actionType, Event event, ConditionContext context) {
        Job job = jobManager.getJob(jobId);
        if (job == null || !job.isEnabled()) {
            if (configCache.isDebugEnabled()) {
                plugin.getLogger().info("DEBUG: Job " + jobId + " is null or disabled");
            }
            return false;
        }
        
        // Cache lookup direct des actions
        Set<JobAction> actions = configCache.getActionsForMaterial(context.getTarget());
        if (actions.isEmpty()) {
            if (configCache.isDebugEnabled()) {
                plugin.getLogger().info("DEBUG: No actions found for material " + context.getTarget());
                plugin.getLogger().info("DEBUG: Available cached materials: " + configCache.getCachedMaterials());
            }
            return false;
        }
        
        if (configCache.isDebugEnabled()) {
            plugin.getLogger().info("DEBUG: Found " + actions.size() + " actions for material " + context.getTarget());
        }
        
        boolean shouldCancel = false;
        for (JobAction action : actions) {
            // Validation ultra-rapide
            if (!configCache.isValidTarget(action.getTarget(), context.getTarget())) {
                if (configCache.isDebugEnabled()) {
                    plugin.getLogger().info("DEBUG: Action target " + action.getTarget() + " doesn't match " + context.getTarget());
                }
                continue;
            }
            
            if (configCache.isDebugEnabled()) {
                plugin.getLogger().info("DEBUG: Processing action for " + action.getTarget() + " with " + action.getXp() + " XP");
            }
            
            // Process rewards sans validation lourde
            processActionRewardsFast(player, job, action, context);
            
            if (action.hasRequirements()) {
                ConditionResult result = action.getRequirements().evaluate(player, event, context);
                if (result.shouldCancelEvent()) {
                    shouldCancel = true;
                }
                result.execute(player);
            }
        }
        
        return shouldCancel;
    }
    
    /**
     * Version ultra-rapide des rewards.
     */
    private void processActionRewardsFast(Player player, Job job, JobAction action, ConditionContext context) {
        double xp = action.getXp();
        double money = action.getMoney();
        
        if (xp <= 0 && money <= 0) return;
        
        // Craft multiplier
        Object craftMultiplierObj = context.get("craft_multiplier");
        if (craftMultiplierObj instanceof Integer) {
            int craftMultiplier = (Integer) craftMultiplierObj;
            xp *= craftMultiplier;
            money *= craftMultiplier;
        }
        
        // XP processing avec cache
        if (xp > 0) {
            int currentLevel = playerCache.getPlayerLevel(player.getUniqueId(), job.getId());
            if (currentLevel < job.getMaxLevel()) {
                // Multiplier avec cache
                double multiplier = playerCache.getPlayerMultiplier(player.getUniqueId());
                xp *= multiplier;
                
                // Bonus avec cache
                double bonusMultiplier = bonusManager.getTotalMultiplier(player.getUniqueId(), job.getId());
                xp *= bonusMultiplier;
                
                // Add XP et mise à jour cache
                jobManager.addXp(player, job.getId(), xp);
                int newLevel = jobManager.getLevel(player, job.getId());
                playerCache.updatePlayerXp(player.getUniqueId(), job.getId(), 
                    playerCache.getPlayerXp(player.getUniqueId(), job.getId()) + xp, newLevel);
                
                if (newLevel > currentLevel) {
                    handleLevelUp(player, job, currentLevel, newLevel);
                }
            }
        }
        
        // Money processing
        if (money > 0) {
            double multiplier = playerCache.getPlayerMultiplier(player.getUniqueId());
            money *= multiplier;
            
            double moneyBonusMultiplier = moneyBonusManager.getTotalMultiplier(player.getUniqueId(), job.getId());
            money *= moneyBonusMultiplier;
            
            addPlayerMoney(player, money);
        }
        
        // Message async seulement si activé
        if (configCache.isShowXpGain() && (xp > 0 || money > 0)) {
            fr.ax_dev.universejobs.job.PlayerJobData playerData = jobManager.getPlayerData(player);
            messageSender.sendXpMessage(player, job, xp, money, playerData);
        }
    }
    
    /**
     * Validate target avec cache ultra-rapide.
     */
    private boolean validateActionTarget(JobAction action, ConditionContext context, Job job) {
        String actionTarget = action.getTarget();
        String contextTarget = context.getTarget();
        
        // Cache lookup instantané
        boolean matches = configCache.isValidTarget(actionTarget, contextTarget);
        
        // Debug seulement si activé
        if (configCache.isDebugEnabled()) {
            if (matches) {
                plugin.getLogger().info("Target matched! Processing action for player");
            } else {
                plugin.getLogger().info("Target mismatch - action: " + actionTarget + ", context: " + contextTarget);
            }
        }
        
        return matches;
    }
    
    /**
     * Validate interact type for interaction actions.
     */
    private boolean validateInteractType(JobAction action, ConditionContext context, Job job) {
        ActionType actionType = job.getActionTypeForAction(action);
        if (actionType != ActionType.BLOCK_INTERACT && actionType != ActionType.ENTITY_INTERACT) {
            return true;
        }
        
        String eventInteractType = context.get("interact-type");
        String actionInteractType = action.getInteractType();
        
        debugLog("Interact type check - event: " + eventInteractType + ", action: " + actionInteractType);
        
        if (actionInteractType != null && !actionInteractType.equals("RIGHT_CLICK") && eventInteractType == null) {
            debugLog("Action requires specific interact-type (" + actionInteractType + ") but event doesn't provide interact-type info - skipping");
            return false;
        }
        
        if (eventInteractType != null && actionInteractType != null && 
            !eventInteractType.equalsIgnoreCase(actionInteractType)) {
            debugLog("Interact type mismatch: expected " + actionInteractType + ", got " + eventInteractType);
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate profession requirements for TRADE actions.
     */
    private boolean validateProfession(JobAction action, ConditionContext context, Job job) {
        ActionType actionType = job.getActionTypeForAction(action);
        if (actionType != ActionType.TRADE) {
            return true; // Profession validation only applies to TRADE actions
        }
        
        // If no profession requirements specified, allow all professions
        if (!action.hasProfessionRequirements()) {
            return true;
        }
        
        String villagerProfession = context.get("profession");
        boolean professionMatches = action.matchesProfession(villagerProfession);
        
        debugLog("Profession check - required: " + action.getProfessions() + 
                ", villager: " + villagerProfession + 
                MATCHES_SUFFIX + professionMatches);
        
        return professionMatches;
    }
    
    /**
     * Validate color requirements for SHEAR actions.
     */
    private boolean validateColor(JobAction action, ConditionContext context, Job job) {
        ActionType actionType = job.getActionTypeForAction(action);
        if (actionType != ActionType.SHEAR) {
            return true; // Color validation only applies to SHEAR actions
        }
        
        // If no color requirements specified, allow all colors
        if (!action.hasColorRequirements()) {
            return true;
        }
        
        String sheepColor = context.get("color");
        boolean colorMatches = action.matchesColor(sheepColor);
        
        debugLog("Color check - required: " + action.getColors() + 
                ", sheep: " + sheepColor + 
                MATCHES_SUFFIX + colorMatches);
        
        return colorMatches;
    }
    
    /**
     * Validate NBT requirements for EAT and other item-based actions.
     */
    private boolean validateNbt(JobAction action, ConditionContext context, Job job) {
        ActionType actionType = job.getActionTypeForAction(action);
        if (actionType != ActionType.EAT && actionType != ActionType.POTION) {
            return true; // NBT validation mainly applies to EAT and POTION actions
        }
        
        // If no NBT requirements specified, allow all items
        if (!action.hasNbtRequirements()) {
            return true;
        }
        
        String itemNbt = context.get("nbt");
        boolean nbtMatches = action.matchesNbt(itemNbt);
        
        debugLog("NBT check - required: " + action.getNbtTags() + 
                ", item: " + itemNbt + 
                MATCHES_SUFFIX + nbtMatches);
        
        return nbtMatches;
    }
    
    /**
     * Validate potion-type requirements for POTION actions.
     */
    private boolean validatePotionType(JobAction action, ConditionContext context, Job job) {
        ActionType actionType = job.getActionTypeForAction(action);
        if (actionType != ActionType.POTION) {
            return true; // Potion-type validation only applies to POTION actions
        }
        
        // If no potion-type requirements specified, allow all potions
        if (!action.hasPotionTypeRequirements()) {
            return true;
        }
        
        String potionType = context.get("potion-type");
        boolean potionTypeMatches = action.matchesPotionType(potionType);
        
        debugLog("Potion-type check - required: " + action.getPotionTypes() + 
                ", potion: " + potionType + 
                MATCHES_SUFFIX + potionTypeMatches);
        
        return potionTypeMatches;
    }
    
    /**
     * Process action requirements and return whether event should be cancelled.
     */
    private boolean processActionRequirements(Player player, JobAction action, Event event, ConditionContext context) {
        if (!action.hasRequirements()) {
            return false;
        }
        
        ConditionResult result = action.getRequirements().evaluate(player, event, context);
        boolean shouldCancel = result.shouldCancelEvent();
        
        result.execute(player);
        
        return shouldCancel;
    }
    
    /**
     * Process action rewards (XP and money).
     */
    private void processActionRewards(Player player, Job job, JobAction action, ConditionContext context) {
        double xp = action.getXp();
        double money = action.getMoney();
        
        // Apply craft multiplier if present
        Object craftMultiplierObj = context.get("craft_multiplier");
        if (craftMultiplierObj instanceof Integer) {
            int craftMultiplier = (Integer) craftMultiplierObj;
            xp *= craftMultiplier;
            money *= craftMultiplier;
            
            debugLog("Applied craft multiplier " + craftMultiplier + 
                " - XP: " + action.getXp() + " -> " + xp + 
                ", Money: " + action.getMoney() + " -> " + money);
        }
        
        if (xp > 0 || money > 0) {
            awardRewards(player, job, action, xp, money);
        }
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
            fr.ax_dev.universejobs.job.PlayerJobData playerData = jobManager.getPlayerData(player);
            messageSender.sendXpMessage(player, job, finalXp, finalMoney, playerData);
            
            debugLog("Player " + player.getName() + " earned " + finalXp + " XP and " + finalMoney + " money from job " + job.getId());
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
                String permission = "universejobs.multiplier." + i;
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
                String permission = "universejobs.multiplier." + i;
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
        String command = "eco give {player} {amount}"
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
            String soundName = "ENTITY_PLAYER_LEVELUP";
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
               player.hasPermission("universejobs.*") ||
               player.hasPermission("universejobs.multiplier.*");
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
     * Get the listener instance. This feature has been removed.
     * 
     * @return Always null (feature removed)
     */
    public Object getListener() {
        return null; // Performance listener feature removed
    }
    
}