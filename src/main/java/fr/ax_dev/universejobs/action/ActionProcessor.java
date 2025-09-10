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
import fr.ax_dev.universejobs.rewards.BatchedRewardManager;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private final BatchedRewardManager batchManager;
    
    // Permission cache for performance (cleared every 30 seconds)
    private static final Map<UUID, Integer> PERMISSION_MULTIPLIER_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PERMISSION_CACHE_TIMESTAMPS = new ConcurrentHashMap<>();
    private static final long PERMISSION_CACHE_DURATION = 30000L; // 30 seconds
    
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
        this.batchManager = new BatchedRewardManager(plugin);
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
        
        if (!validateEnchantLevel(action, context, job)) {
            return false;
        }
        
        if (!validateAge(action, context, job)) {
            return false;
        }
        
        ActionType actionType = job.getActionTypeForAction(action);
        if (!validateFurnaceType(action, context, actionType)) {
            return false;
        }
        
        boolean shouldCancel = false;
        boolean conditionMet = true;
        
        // Check requirements first
        if (action.hasRequirements()) {
            ConditionResult result = action.getRequirements().evaluate(player, event, context);
            conditionMet = result.isAllowed();
            shouldCancel = result.shouldCancelEvent();
            result.execute(player);
        }
        
        // Only process rewards and effects if conditions are met
        if (conditionMet) {
            processActionRewardsFast(player, job, action, context);
            executeActionEffects(player, action);
        }
        
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
        
        // Always use job's action list directly to ensure ALL actions are checked
        // The cache may miss actions with complex targets or conditions
        List<JobAction> actionsList = job.getActions(actionType);
        
        if (actionsList.isEmpty()) {
            if (configCache.isDebugEnabled()) {
                plugin.getLogger().info("DEBUG: No actions found for " + actionType);
            }
            return false;
        }
        
        if (configCache.isDebugEnabled()) {
            plugin.getLogger().info("DEBUG: Found " + actionsList.size() + " actions for " + actionType + " with target " + context.getTarget());
        }
        
        boolean shouldCancel = false;
        for (JobAction action : actionsList) {
            // Validation ultra-rapide avec toutes les conditions
            if (!validateActionTargetFast(action, context)) continue;
            if (!validateInteractTypeFast(action, context, job)) continue;
            if (!validateProfessionFast(action, context)) continue;
            if (!validateColorFast(action, context)) continue;
            if (!validateNbtFast(action, context)) continue;
            if (!validatePotionTypeFast(action, context)) continue;
            if (!validateEnchantLevelFast(action, context, job)) continue;
            if (!validateAgeFast(action, context)) continue;
            if (!validateFurnaceTypeFast(action, context, actionType)) continue;
            
            if (configCache.isDebugEnabled()) {
                plugin.getLogger().info("DEBUG: Processing action for " + action.getTarget() + " with " + action.getXp() + " XP");
            }
            
            // Check requirements first before giving rewards
            boolean conditionMet = true;
            if (action.hasRequirements()) {
                ConditionResult result = action.getRequirements().evaluate(player, event, context);
                conditionMet = result.isAllowed();
                
                if (result.shouldCancelEvent()) {
                    shouldCancel = true;
                }
                result.execute(player);
            }
            
            // Only process rewards if conditions are met
            if (conditionMet) {
                processActionRewardsFast(player, job, action, context);
            }
        }
        
        return shouldCancel;
    }
    
    // Fast validation methods (without debug logging for performance)
    private boolean validateActionTargetFast(JobAction action, ConditionContext context) {
        return configCache.isValidTarget(action.getTarget(), context.getTarget());
    }
    
    private boolean validateInteractTypeFast(JobAction action, ConditionContext context, Job job) {
        ActionType actionType = job.getActionTypeForAction(action);
        if (actionType != ActionType.BLOCK_INTERACT && actionType != ActionType.ENTITY_INTERACT) {
            return true;
        }
        
        String eventInteractType = context.get("interact-type");
        String actionInteractType = action.getInteractType();
        
        if (actionInteractType != null && !actionInteractType.equals("RIGHT_CLICK") && eventInteractType == null) {
            return false;
        }
        
        if (eventInteractType != null && actionInteractType != null && 
            !eventInteractType.equalsIgnoreCase(actionInteractType)) {
            return false;
        }
        
        return true;
    }
    
    private boolean validateProfessionFast(JobAction action, ConditionContext context) {
        if (!action.hasProfessionRequirements()) {
            return true;
        }
        
        String villagerProfession = context.get("profession");
        return action.matchesProfession(villagerProfession);
    }
    
    private boolean validateColorFast(JobAction action, ConditionContext context) {
        if (!action.hasColorRequirements()) {
            return true;
        }
        
        String sheepColor = context.get("color");
        return action.matchesColor(sheepColor);
    }
    
    private boolean validateNbtFast(JobAction action, ConditionContext context) {
        if (!action.hasNbtRequirements()) {
            return true;
        }
        
        String itemNbt = context.get("nbt");
        return action.matchesNbt(itemNbt);
    }
    
    private boolean validatePotionTypeFast(JobAction action, ConditionContext context) {
        if (!action.hasPotionTypeRequirements()) {
            return true;
        }
        
        String potionType = context.get("potion-type");
        return action.matchesPotionType(potionType);
    }
    
    private boolean validateEnchantLevelFast(JobAction action, ConditionContext context, Job job) {
        ActionType actionType = job.getActionTypeForAction(action);
        if (actionType != ActionType.ENCHANT) {
            return true;
        }
        
        if (action.getEnchantLevel() == null || action.getEnchantLevel().isEmpty()) {
            return true;
        }
        
        String enchantLevelStr = context.get("enchantment_level");
        if (enchantLevelStr == null) {
            return false;
        }
        
        try {
            int enchantLevel = Integer.parseInt(enchantLevelStr);
            return matchesEnchantLevel(action.getEnchantLevel(), enchantLevel);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean validateAgeFast(JobAction action, ConditionContext context) {
        if (!action.hasAgeRequirements()) {
            return true;
        }
        
        String ageStr = context.get("age");
        if (ageStr == null) {
            return false;
        }
        
        try {
            int currentAge = Integer.parseInt(ageStr);
            return action.matchesAge(currentAge);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean validateFurnaceTypeFast(JobAction action, ConditionContext context, ActionType actionType) {
        if (actionType != ActionType.SMELT) {
            return true;
        }
        
        String furnaceType = context.get("furnace_type");
        if (furnaceType == null || action.getBlacklistedFurnaces() == null || action.getBlacklistedFurnaces().isEmpty()) {
            return true;
        }
        
        return !action.getBlacklistedFurnaces().contains(furnaceType);
    }
    
    /**
     * Process action rewards with optimal performance and all features.
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
        
        // Check action limits first (if any)
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
        
        // XP processing (same logic as awardRewards)
        if (xp > 0) {
            int currentLevel = jobManager.getLevel(player, job.getId());
            if (currentLevel < job.getMaxLevel()) {
                // Apply any XP multipliers
                double finalXp = applyMultipliers(player, job, xp);
                
                // Apply bonus multipliers
                double bonusMultiplier = bonusManager.getTotalMultiplier(player.getUniqueId(), job.getId());
                finalXp *= bonusMultiplier;
                
                // Add XP to batch for optimized processing
                batchManager.batchXp(player, job.getId(), finalXp);
                
                // Check for level up
                int newLevel = jobManager.getLevel(player, job.getId());
                if (newLevel > currentLevel) {
                    handleLevelUp(player, job, currentLevel, newLevel);
                }
            }
        }
        
        // Money processing (same logic as awardRewards)
        if (money > 0) {
            double moneyBonusMultiplier = moneyBonusManager.getTotalMultiplier(player.getUniqueId(), job.getId());
            double finalMoney = money * moneyBonusMultiplier;
            
            // Add money to the player
            addPlayerMoney(player, finalMoney);
        }
        
        // Message async seulement si activé (et si pas supprimé) - use final values
        boolean suppressMessage = "true".equals(context.get("suppress_message"));
        double finalXp = xp > 0 ? applyMultipliers(player, job, xp) * bonusManager.getTotalMultiplier(player.getUniqueId(), job.getId()) : 0;
        double finalMoney = money > 0 ? money * moneyBonusManager.getTotalMultiplier(player.getUniqueId(), job.getId()) : 0;
        
        if (configCache.isShowXpGain() && (finalXp > 0 || finalMoney > 0) && !suppressMessage) {
            fr.ax_dev.universejobs.job.PlayerJobData playerData = jobManager.getPlayerData(player);
            messageSender.sendXpMessage(player, job, finalXp, finalMoney, playerData);
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
     * Validate enchant-level requirements for ENCHANT actions.
     */
    private boolean validateEnchantLevel(JobAction action, ConditionContext context, Job job) {
        ActionType actionType = job.getActionTypeForAction(action);
        if (actionType != ActionType.ENCHANT) {
            return true; // Enchant-level validation only applies to ENCHANT actions
        }
        
        // If no enchant-level requirements specified, allow all levels
        if (action.getEnchantLevel() == null || action.getEnchantLevel().isEmpty()) {
            return true;
        }
        
        String enchantLevelStr = context.get("enchantment_level");
        if (enchantLevelStr == null) {
            debugLog("Enchant-level check - no enchantment level in context");
            return false;
        }
        
        try {
            int enchantLevel = Integer.parseInt(enchantLevelStr);
            boolean levelMatches = matchesEnchantLevel(action.getEnchantLevel(), enchantLevel);
            
            debugLog("Enchant-level check - required: " + action.getEnchantLevel() + 
                    ", actual: " + enchantLevel + 
                    MATCHES_SUFFIX + levelMatches);
            
            return levelMatches;
        } catch (NumberFormatException e) {
            debugLog("Enchant-level check - invalid level format: " + enchantLevelStr);
            return false;
        }
    }
    
    private boolean validateFurnaceType(JobAction action, ConditionContext context, ActionType actionType) {
        if (actionType != ActionType.SMELT) {
            return true;
        }
        
        String furnaceType = context.get("furnace_type");
        if (furnaceType == null || action.getBlacklistedFurnaces() == null || action.getBlacklistedFurnaces().isEmpty()) {
            return true;
        }
        
        boolean isBlacklisted = action.getBlacklistedFurnaces().contains(furnaceType);
        debugLog("Furnace-type check - type: " + furnaceType + 
                ", blacklisted: " + action.getBlacklistedFurnaces() + 
                ", blocked: " + isBlacklisted);
        
        return !isBlacklisted;
    }
    
    private boolean validateAge(JobAction action, ConditionContext context, Job job) {
        // If no age requirements specified, allow all ages
        if (!action.hasAgeRequirements()) {
            return true;
        }
        
        String ageStr = context.get("age");
        if (ageStr == null) {
            debugLog("Age check - no age in context");
            return false;
        }
        
        try {
            int currentAge = Integer.parseInt(ageStr);
            boolean ageMatches = action.matchesAge(currentAge);
            
            debugLog("Age check - required: " + action.getAge() + 
                    ", actual: " + currentAge + 
                    MATCHES_SUFFIX + ageMatches);
            
            return ageMatches;
        } catch (NumberFormatException e) {
            debugLog("Age check - invalid age format: " + ageStr);
            return false;
        }
    }
    
    /**
     * Check if an enchantment level matches the requirement.
     * Supports ranges like "3-10" and single values like "5".
     */
    private boolean matchesEnchantLevel(String requirement, int actualLevel) {
        if (requirement == null || requirement.isEmpty()) {
            return true;
        }
        
        requirement = requirement.trim();
        
        // Check for range (e.g., "3-10")
        if (requirement.contains("-")) {
            String[] parts = requirement.split("-", 2);
            if (parts.length == 2) {
                try {
                    int minLevel = Integer.parseInt(parts[0].trim());
                    int maxLevel = Integer.parseInt(parts[1].trim());
                    return actualLevel >= minLevel && actualLevel <= maxLevel;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        
        // Single value (e.g., "5")
        try {
            int requiredLevel = Integer.parseInt(requirement);
            return actualLevel == requiredLevel;
        } catch (NumberFormatException e) {
            return false;
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
        
        // Check for permission-based multipliers (cached for performance)
        multiplier = getCachedPermissionMultiplier(player);
        
        // Could add other multipliers here:
        // - Time-based bonuses
        // - World-based bonuses
        // - Job-level bonuses
        // - Item-based bonuses
        
        return baseXp * multiplier;
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
                // Add money to batch for optimized processing
                batchManager.batchMoney(player, amount);
                return;
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
     * Get cached permission multiplier for player (optimized for performance).
     * 
     * @param player The player to check
     * @return The permission multiplier (1.0 = no multiplier)
     */
    private double getCachedPermissionMultiplier(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check if we have a cached result that's still valid
        Long cacheTime = PERMISSION_CACHE_TIMESTAMPS.get(playerId);
        if (cacheTime != null && (currentTime - cacheTime) < PERMISSION_CACHE_DURATION) {
            Integer cachedMultiplier = PERMISSION_MULTIPLIER_CACHE.get(playerId);
            if (cachedMultiplier != null) {
                return cachedMultiplier;
            }
        }
        
        // Calculate multiplier (expensive operation)
        double multiplier = 1.0;
        
        // Skip if player is OP or has wildcard permission to avoid overpowered bonuses
        if (!player.isOp() && !player.hasPermission("*")) {
            for (int i = 10; i >= 1; i--) {
                String permission = "universejobs.multiplier.exp." + i;
                // Check if player has the specific permission (not through wildcard)
                if (player.hasPermission(permission) && !hasWildcardPermission(player)) {
                    multiplier = i;
                    break;
                }
            }
        }
        
        // Cache the result
        PERMISSION_MULTIPLIER_CACHE.put(playerId, (int) multiplier);
        PERMISSION_CACHE_TIMESTAMPS.put(playerId, currentTime);
        
        return multiplier;
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
               player.hasPermission("universejobs.multiplier.*") ||
               player.hasPermission("universejobs.multiplier.money.*") ||
               player.hasPermission("universejobs.multiplier.exp.*");
    }
    
    /**
     * Clear permission cache for a player (call on disconnect).
     * 
     * @param playerId The player UUID
     */
    public static void clearPermissionCache(UUID playerId) {
        PERMISSION_MULTIPLIER_CACHE.remove(playerId);
        PERMISSION_CACHE_TIMESTAMPS.remove(playerId);
    }
    
    /**
     * Clear all expired permission cache entries (call periodically).
     */
    public static void cleanupExpiredPermissionCache() {
        long currentTime = System.currentTimeMillis();
        PERMISSION_CACHE_TIMESTAMPS.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) >= PERMISSION_CACHE_DURATION);
        PERMISSION_MULTIPLIER_CACHE.keySet().retainAll(PERMISSION_CACHE_TIMESTAMPS.keySet());
    }
    
    /**
     * Shutdown the ActionProcessor and flush all pending batches.
     */
    public void shutdown() {
        if (batchManager != null) {
            batchManager.shutdown();
        }
    }
    
    /**
     * Get current batch statistics for monitoring.
     */
    public BatchedRewardManager.BatchStatistics getBatchStatistics() {
        return batchManager != null ? batchManager.getStatistics() : null;
    }
    
    /**
     * Force flush all pending batches immediately.
     */
    public void forceFlushBatches() {
        if (batchManager != null) {
            batchManager.forceFlushAll();
        }
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