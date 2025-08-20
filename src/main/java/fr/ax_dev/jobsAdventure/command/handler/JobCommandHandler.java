package fr.ax_dev.jobsAdventure.command.handler;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.action.ActionLimitManager;
import fr.ax_dev.jobsAdventure.bonus.XpBonusManager;
import fr.ax_dev.jobsAdventure.bonus.MoneyBonusManager;
import fr.ax_dev.jobsAdventure.config.LanguageManager;
import fr.ax_dev.jobsAdventure.job.JobManager;
import fr.ax_dev.jobsAdventure.reward.RewardManager;
import fr.ax_dev.jobsAdventure.reward.gui.RewardGuiManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Base class for job command handlers providing common functionality.
 */
public abstract class JobCommandHandler {
    
    protected final JobsAdventure plugin;
    protected final JobManager jobManager;
    protected final LanguageManager languageManager;
    protected final XpBonusManager bonusManager;
    protected final MoneyBonusManager moneyBonusManager;
    protected final RewardManager rewardManager;
    protected final RewardGuiManager rewardGuiManager;
    protected final ActionLimitManager limitManager;
    
    // Security patterns for input validation
    protected static final Pattern SAFE_JOB_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,32}$");
    protected static final Pattern SAFE_PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");
    protected static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile("[;&|`$(){}\\[\\]<>\"'\\\\]");
    
    public JobCommandHandler(JobsAdventure plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.languageManager = plugin.getLanguageManager();
        this.bonusManager = plugin.getBonusManager();
        this.moneyBonusManager = plugin.getMoneyBonusManager();
        this.rewardManager = plugin.getRewardManager();
        this.rewardGuiManager = plugin.getRewardGuiManager();
        this.limitManager = plugin.getLimitManager();
    }
    
    /**
     * Handle the command execution.
     * 
     * @param sender The command sender
     * @param args The command arguments (without the main command)
     * @return true if command was handled successfully
     */
    public abstract boolean handleCommand(CommandSender sender, String[] args);
    
    /**
     * Get tab completions for this command.
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return List of possible completions
     */
    public abstract List<String> getTabCompletions(CommandSender sender, String[] args);
    
    /**
     * Sanitize input string to prevent injection attacks.
     * 
     * @param input The input string
     * @return Sanitized string
     */
    protected String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        
        // Remove dangerous characters and limit length
        return input.replaceAll("[^a-zA-Z0-9_-]", "").substring(0, Math.min(input.length(), 64));
    }
    
    /**
     * Validate job ID format.
     * 
     * @param jobId The job ID to validate
     * @return true if valid
     */
    protected boolean isValidJobId(String jobId) {
        return jobId != null && SAFE_JOB_ID_PATTERN.matcher(jobId).matches();
    }
    
    /**
     * Validate player name format.
     * 
     * @param playerName The player name to validate
     * @return true if valid
     */
    protected boolean isValidPlayerName(String playerName) {
        return playerName != null && SAFE_PLAYER_NAME_PATTERN.matcher(playerName).matches();
    }
    
    /**
     * Validate command structure.
     * 
     * @param args The command arguments
     * @return true if structure is valid
     */
    protected boolean validateCommandStructure(String[] args) {
        if (args.length > 10) {
            return false; // Too many arguments
        }
        
        for (String arg : args) {
            if (arg == null || arg.length() > 256) {
                return false; // Null or excessively long argument
            }
            
            // Check for potential command injection
            if (COMMAND_INJECTION_PATTERN.matcher(arg).find()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if sender has the required permission.
     * 
     * @param sender The command sender
     * @param permission The permission to check
     * @return true if sender has permission
     */
    protected boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }
    
    /**
     * Get player from sender if sender is a player.
     * 
     * @param sender The command sender
     * @return Player if sender is a player, null otherwise
     */
    protected Player getPlayerFromSender(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }
}