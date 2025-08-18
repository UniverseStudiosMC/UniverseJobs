package fr.ax_dev.jobsAdventure.levelup;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.condition.ConditionResult;
import fr.ax_dev.jobsAdventure.job.Job;
import fr.ax_dev.jobsAdventure.utils.XpMessageSender;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Simplified level up action manager that reuses existing components.
 * Uses ConditionResult for basic actions and existing managers for complex ones.
 */
public class SimpleLevelUpActionManager {
    
    private final JobsAdventure plugin;
    private final Map<String, List<LevelUpActionConfig>> jobActions = new ConcurrentHashMap<>();
    
    /**
     * Configuration for a level up action.
     */
    public static class LevelUpActionConfig {
        private final String type;
        private final ConfigurationSection config;
        private final int minLevel;
        private final int maxLevel;
        private final Set<Integer> specificLevels;
        private final int levelInterval;
        
        public LevelUpActionConfig(String type, ConfigurationSection config) {
            this.type = type;
            this.config = config;
            this.minLevel = config.getInt("min-level", 1);
            this.maxLevel = config.getInt("max-level", Integer.MAX_VALUE);
            this.levelInterval = config.getInt("level-interval", 0);
            
            this.specificLevels = new HashSet<>();
            if (config.contains("levels")) {
                if (config.isList("levels")) {
                    for (int level : config.getIntegerList("levels")) {
                        this.specificLevels.add(level);
                    }
                } else {
                    this.specificLevels.add(config.getInt("levels"));
                }
            }
        }
        
        public boolean shouldExecuteForLevel(int level) {
            if (!specificLevels.isEmpty()) {
                return specificLevels.contains(level);
            }
            
            if (level < minLevel || level > maxLevel) {
                return false;
            }
            
            if (levelInterval > 0) {
                return (level - minLevel) % levelInterval == 0;
            }
            
            return true;
        }
        
        public String getType() { return type; }
        public ConfigurationSection getConfig() { return config; }
    }
    
    public SimpleLevelUpActionManager(JobsAdventure plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load level up actions for all jobs.
     */
    public void loadJobActions() {
        jobActions.clear();
        
        for (Job job : plugin.getJobManager().getAllJobs()) {
            loadJobActions(job);
        }
    }
    
    /**
     * Load level up actions for a specific job.
     */
    public void loadJobActions(Job job) {
        List<LevelUpActionConfig> actions = new ArrayList<>();
        
        ConfigurationSection levelUpSection = job.getConfig().getConfigurationSection("levelup-actions");
        if (levelUpSection == null) {
            jobActions.put(job.getId(), actions);
            return;
        }
        
        for (String actionKey : levelUpSection.getKeys(false)) {
            ConfigurationSection actionSection = levelUpSection.getConfigurationSection(actionKey);
            if (actionSection == null) continue;
            
            String actionType = actionSection.getString("type");
            if (actionType == null) {
                plugin.getLogger().warning("No type specified for level up action '" + actionKey + "' in job " + job.getId());
                continue;
            }
            
            try {
                LevelUpActionConfig actionConfig = new LevelUpActionConfig(actionType.toLowerCase(), actionSection);
                actions.add(actionConfig);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load level up action '" + actionKey + "' for job " + job.getId(), e);
            }
        }
        
        jobActions.put(job.getId(), actions);
        plugin.getLogger().info("Loaded " + actions.size() + " level up actions for job " + job.getId());
    }
    
    /**
     * Execute level up actions for a player.
     */
    public void executeLevelUpActions(Player player, String jobId, int oldLevel, int newLevel, double totalXp, double xpGained) {
        List<LevelUpActionConfig> actions = jobActions.get(jobId);
        if (actions == null || actions.isEmpty()) {
            return;
        }
        
        Job job = plugin.getJobManager().getJob(jobId);
        if (job == null) {
            return;
        }
        
        // Execute actions for each level gained
        for (int level = oldLevel + 1; level <= newLevel; level++) {
            executeActionsForLevel(player, job, level, oldLevel, totalXp, xpGained, actions);
        }
    }
    
    /**
     * Execute actions for a specific level using existing components.
     */
    private void executeActionsForLevel(Player player, Job job, int level, int oldLevel, 
                                       double totalXp, double xpGained, List<LevelUpActionConfig> actions) {
        for (LevelUpActionConfig actionConfig : actions) {
            if (!actionConfig.shouldExecuteForLevel(level)) {
                continue;
            }
            
            try {
                switch (actionConfig.getType()) {
                    case "message":
                        executeMessageAction(player, job, level, oldLevel, actionConfig);
                        break;
                    case "command":
                        executeCommandAction(player, job, level, oldLevel, actionConfig);
                        break;
                    case "sound":
                        executeSoundAction(player, job, level, oldLevel, actionConfig);
                        break;
                    case "bossbar":
                        executeBossBarAction(player, job, level, oldLevel, actionConfig);
                        break;
                    case "broadcast":
                        executeBroadcastAction(player, job, level, oldLevel, actionConfig);
                        break;
                    case "title":
                        executeTitleAction(player, job, level, oldLevel, actionConfig);
                        break;
                    case "particle":
                        executeParticleAction(player, job, level, oldLevel, actionConfig);
                        break;
                    case "reward":
                        executeRewardAction(player, job, level, oldLevel, actionConfig);
                        break;
                    default:
                        plugin.getLogger().warning("Unknown level up action type: " + actionConfig.getType());
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, 
                    "Failed to execute level up action '" + actionConfig.getType() + 
                    "' for player " + player.getName() + " in job " + job.getId(), e);
            }
        }
    }
    
    /**
     * Execute message action using existing ConditionResult.
     */
    private void executeMessageAction(Player player, Job job, int level, int oldLevel, LevelUpActionConfig config) {
        List<String> messages = config.getConfig().getStringList("messages");
        if (messages.isEmpty()) {
            String singleMessage = config.getConfig().getString("message");
            if (singleMessage != null && !singleMessage.isEmpty()) {
                messages = List.of(singleMessage);
            }
        }
        
        for (String message : messages) {
            String processedMessage = processPlaceholders(message, player, job, level, oldLevel);
            ConditionResult.allow(processedMessage, null, null).execute(player);
        }
    }
    
    /**
     * Execute command action using existing ConditionResult.
     */
    private void executeCommandAction(Player player, Job job, int level, int oldLevel, LevelUpActionConfig config) {
        List<String> commands = config.getConfig().getStringList("commands");
        if (commands.isEmpty()) {
            String singleCommand = config.getConfig().getString("command");
            if (singleCommand != null && !singleCommand.isEmpty()) {
                commands = List.of(singleCommand);
            }
        }
        
        List<String> processedCommands = new ArrayList<>();
        for (String command : commands) {
            processedCommands.add(processPlaceholders(command, player, job, level, oldLevel));
        }
        
        ConditionResult.allow(null, null, processedCommands).execute(player);
    }
    
    /**
     * Execute sound action using existing ConditionResult.
     */
    private void executeSoundAction(Player player, Job job, int level, int oldLevel, LevelUpActionConfig config) {
        String soundName = config.getConfig().getString("sound", "ENTITY_PLAYER_LEVELUP");
        
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            ConditionResult.allow(null, sound, null).execute(player);
        } catch (IllegalArgumentException e) {
            ConditionResult.allow(null, Sound.ENTITY_PLAYER_LEVELUP, null).execute(player);
        }
    }
    
    /**
     * Execute boss bar action using existing XpMessageSender.
     */
    private void executeBossBarAction(Player player, Job job, int level, int oldLevel, LevelUpActionConfig config) {
        // Use existing XpMessageSender for boss bar functionality
        // This would require extending XpMessageSender to accept custom boss bar messages
        // For now, we can create a simple implementation
        String title = processPlaceholders(config.getConfig().getString("title", "Level Up!"), player, job, level, oldLevel);
        
        // Simple title as fallback (since we're simplifying)
        if (title != null && !title.isEmpty()) {
            player.sendTitle("§6§lLEVEL UP!", title, 10, 70, 20);
        }
    }
    
    /**
     * Execute other action types with simple implementations.
     */
    private void executeBroadcastAction(Player player, Job job, int level, int oldLevel, LevelUpActionConfig config) {
        List<String> messages = config.getConfig().getStringList("messages");
        if (messages.isEmpty()) {
            String singleMessage = config.getConfig().getString("message");
            if (singleMessage != null) {
                messages = List.of(singleMessage);
            }
        }
        
        for (String message : messages) {
            String processedMessage = processPlaceholders(message, player, job, level, oldLevel);
            plugin.getServer().broadcastMessage(processedMessage);
        }
    }
    
    private void executeTitleAction(Player player, Job job, int level, int oldLevel, LevelUpActionConfig config) {
        String title = processPlaceholders(config.getConfig().getString("title", ""), player, job, level, oldLevel);
        String subtitle = processPlaceholders(config.getConfig().getString("subtitle", ""), player, job, level, oldLevel);
        int fadeIn = config.getConfig().getInt("fade-in", 10);
        int stay = config.getConfig().getInt("stay", 70);
        int fadeOut = config.getConfig().getInt("fade-out", 20);
        
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }
    
    private void executeParticleAction(Player player, Job job, int level, int oldLevel, LevelUpActionConfig config) {
        // Simple particle implementation - could be enhanced
        try {
            org.bukkit.Particle particle = org.bukkit.Particle.valueOf(config.getConfig().getString("particle", "FLAME").toUpperCase());
            int count = config.getConfig().getInt("count", 20);
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count, 0.5, 1.0, 0.5, 0.1);
        } catch (Exception e) {
            // Fallback particle
            player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, player.getLocation().add(0, 1, 0), 20, 0.5, 1.0, 0.5, 0.1);
        }
    }
    
    private void executeRewardAction(Player player, Job job, int level, int oldLevel, LevelUpActionConfig config) {
        // Use existing RewardManager - this would require integration with the reward system
        // For now, we'll use a command-based approach
        String rewardId = config.getConfig().getString("reward-id");
        if (rewardId != null) {
            List<String> commands = List.of("jobs reward give " + player.getName() + " " + rewardId);
            ConditionResult.allow(null, null, commands).execute(player);
        }
    }
    
    /**
     * Process placeholders in text.
     */
    private String processPlaceholders(String text, Player player, Job job, int level, int oldLevel) {
        return text
            .replace("{player}", player.getName())
            .replace("{job}", job.getName())
            .replace("{oldlevel}", String.valueOf(oldLevel))
            .replace("{newlevel}", String.valueOf(level))
            .replace("{level}", String.valueOf(level));
    }
    
    /**
     * Reload all job actions.
     */
    public void reload() {
        loadJobActions();
    }
}