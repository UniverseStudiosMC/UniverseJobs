package fr.ax_dev.universejobs.command;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.command.handler.*;
import fr.ax_dev.universejobs.config.LanguageManager;
import fr.ax_dev.universejobs.job.JobManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Handles the main job command and its subcommands using modular handlers.
 */
public class JobCommand implements CommandExecutor, TabCompleter {
    
    private final UniverseJobs plugin;
    private final LanguageManager languageManager;
    
    // Command handlers
    private final JoinLeaveCommandHandler joinLeaveHandler;
    private final InfoStatsCommandHandler infoStatsHandler;
    private final RewardsCommandHandler rewardsHandler;
    private final XpBonusCommandHandler xpBonusHandler;
    private final MoneyBonusCommandHandler moneyBonusHandler;
    private final ActionLimitCommandHandler actionLimitHandler;
    private final AdminCommandHandler adminHandler;
    
    // Command constants
    private static final String CMD_JOIN = "join";
    private static final String CMD_LEAVE = "leave";
    private static final String CMD_INFO = "info";
    private static final String CMD_LIST = "list";
    private static final String CMD_STATS = "stats";
    private static final String CMD_REWARDS = "rewards";
    private static final String CMD_XP_BONUS = "xpbonus";
    private static final String CMD_MONEY_BONUS = "moneybonus";
    private static final String CMD_ACTION_LIMIT = "actionlimit";
    private static final String CMD_EXP = "exp";
    private static final String CMD_MIGRATE = "migrate";
    private static final String CMD_RELOAD = "reload";
    private static final String CMD_DEBUG = "debug";
    
    // Security patterns for input validation
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile("[;&|`$(){}\\[\\]<>\"'\\\\]");
    
    // Rate limiting for commands (per player)
    private final Map<UUID, Long> lastCommandTime = new HashMap<>();
    private static final long COMMAND_COOLDOWN_MS = 100; // 100ms between commands
    
    /**
     * Create a new JobCommand.
     * 
     * @param plugin The plugin instance
     * @param jobManager The job manager
     */
    public JobCommand(UniverseJobs plugin, JobManager jobManager) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        
        // Initialize command handlers
        this.joinLeaveHandler = new JoinLeaveCommandHandler(plugin);
        this.infoStatsHandler = new InfoStatsCommandHandler(plugin);
        this.rewardsHandler = new RewardsCommandHandler(plugin);
        this.xpBonusHandler = new XpBonusCommandHandler(plugin);
        this.moneyBonusHandler = new MoneyBonusCommandHandler(plugin);
        this.actionLimitHandler = new ActionLimitCommandHandler(plugin);
        this.adminHandler = new AdminCommandHandler(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Basic argument validation
        if (!validateCommandStructure(args)) {
            return true; // Silently ignore invalid command structure
        }
        
        if (args.length == 0) {
            if (sender instanceof Player player) {
                sendHelp(player);
            } else {
                sendConsoleHelp(sender);
            }
            return true;
        }
        
        String subCommand = sanitizeInput(args[0].toLowerCase());
        
        // Validate subcommand
        if (!isValidSubCommand(subCommand)) {
            if (sender instanceof Player player) {
                sendHelp(player);
            } else {
                sendConsoleHelp(sender);
            }
            return true;
        }
        
        // Check if command requires a player
        boolean requiresPlayer = requiresPlayer(subCommand);
        if (requiresPlayer && !(sender instanceof Player)) {
            sender.sendMessage(languageManager.getMessage("commands.players-only"));
            return true;
        }
        
        // Rate limiting check for players only
        if (sender instanceof Player player && !checkRateLimit(player)) {
            return true; // Silently ignore rapid commands
        }
        
        try {
            boolean handled = false;
            
            switch (subCommand) {
                case CMD_JOIN, CMD_LEAVE -> handled = joinLeaveHandler.handleCommand(sender, args);
                case CMD_INFO, CMD_LIST, CMD_STATS -> handled = infoStatsHandler.handleCommand(sender, args);
                case CMD_REWARDS -> handled = rewardsHandler.handleCommand(sender, args);
                case CMD_XP_BONUS -> handled = xpBonusHandler.handleCommand(sender, args);
                case CMD_MONEY_BONUS -> handled = moneyBonusHandler.handleCommand(sender, args);
                case CMD_ACTION_LIMIT -> handled = actionLimitHandler.handleCommand(sender, args);
                case CMD_EXP, CMD_MIGRATE, CMD_RELOAD, CMD_DEBUG -> handled = adminHandler.handleCommand(sender, args);
                default -> handled = false;
            }
            
            if (!handled) {
                if (sender instanceof Player player) {
                    sendHelp(player);
                } else {
                    sendConsoleHelp(sender);
                }
            }
        } catch (Exception e) {
            String senderName = sender instanceof Player ? sender.getName() : "Console";
            plugin.getLogger().warning("Error executing command for " + senderName + ": " + e.getMessage());
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main subcommands
            List<String> subCommands = new ArrayList<>();
            
            // Commands available to players
            if (sender instanceof Player) {
                subCommands.addAll(Arrays.asList(CMD_JOIN, CMD_LEAVE, CMD_INFO, CMD_LIST, CMD_STATS));
                if (sender.hasPermission("universejobs.rewards.use")) {
                    subCommands.add(CMD_REWARDS);
                }
            }
            
            // Admin commands available to both console and players
            if (sender.hasPermission("universejobs.admin")) {
                subCommands.add(CMD_RELOAD);
                subCommands.add(CMD_DEBUG);
            }
            if (sender.hasPermission("universejobs.admin.migrate")) {
                subCommands.add(CMD_MIGRATE);
            }
            if (sender.hasPermission("universejobs.admin.xpbonus")) {
                subCommands.add(CMD_XP_BONUS);
            }
            if (sender.hasPermission("universejobs.admin.moneybonus")) {
                subCommands.add(CMD_MONEY_BONUS);
            }
            if (sender.hasPermission("universejobs.admin.exp")) {
                subCommands.add(CMD_EXP);
            }
            if (sender.hasPermission("universejobs.admin.actionlimits")) {
                subCommands.add(CMD_ACTION_LIMIT);
            }
            
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        } else {
            // Delegate to appropriate handler for tab completion
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case CMD_JOIN, CMD_LEAVE -> completions.addAll(joinLeaveHandler.getTabCompletions(sender, args));
                case CMD_INFO, CMD_LIST, CMD_STATS -> completions.addAll(infoStatsHandler.getTabCompletions(sender, args));
                case CMD_REWARDS -> completions.addAll(rewardsHandler.getTabCompletions(sender, args));
                case CMD_XP_BONUS -> completions.addAll(xpBonusHandler.getTabCompletions(sender, args));
                case CMD_MONEY_BONUS -> completions.addAll(moneyBonusHandler.getTabCompletions(sender, args));
                case CMD_ACTION_LIMIT -> completions.addAll(actionLimitHandler.getTabCompletions(sender, args));
                case CMD_EXP, CMD_MIGRATE, CMD_RELOAD, CMD_DEBUG -> completions.addAll(adminHandler.getTabCompletions(sender, args));
            }
        }
        
        return completions;
    }
    
    /**
     * Check rate limiting for command execution.
     * 
     * @param player The player executing the command
     * @return true if command should be processed, false if rate limited
     */
    private boolean checkRateLimit(Player player) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastCommandTime.get(player.getUniqueId());
        
        if (lastTime != null && (currentTime - lastTime) < COMMAND_COOLDOWN_MS) {
            return false;
        }
        
        lastCommandTime.put(player.getUniqueId(), currentTime);
        return true;
    }
    
    /**
     * Validate the basic structure of command arguments.
     * 
     * @param args The command arguments
     * @return true if structure is valid
     */
    private boolean validateCommandStructure(String[] args) {
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
     * Sanitize input string to prevent injection attacks.
     * 
     * @param input The input string
     * @return Sanitized string
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        
        // Remove dangerous characters and limit length
        return input.replaceAll("[^a-zA-Z0-9_-]", "").substring(0, Math.min(input.length(), 64));
    }
    
    /**
     * Check if subcommand is valid.
     * 
     * @param subCommand The subcommand to validate
     * @return true if valid
     */
    private boolean isValidSubCommand(String subCommand) {
        Set<String> validCommands = Set.of(CMD_JOIN, CMD_LEAVE, CMD_INFO, CMD_LIST, CMD_STATS, CMD_REWARDS, CMD_XP_BONUS, CMD_MONEY_BONUS, CMD_ACTION_LIMIT, CMD_EXP, CMD_MIGRATE, CMD_RELOAD, CMD_DEBUG);
        return validCommands.contains(subCommand);
    }
    
    /**
     * Check if a command requires a player.
     * 
     * @param subCommand The subcommand to check
     * @return true if the command requires a player
     */
    private boolean requiresPlayer(String subCommand) {
        Set<String> playerOnlyCommands = Set.of(CMD_JOIN, CMD_LEAVE, CMD_INFO, CMD_LIST, CMD_STATS, CMD_REWARDS);
        return playerOnlyCommands.contains(subCommand);
    }
    
    /**
     * Send help message to a player.
     * 
     * @param player The player to send help to
     */
    private void sendHelp(Player player) {
        player.sendMessage("§6=== UniverseJobs Commands ===");
        player.sendMessage("§e/jobs join <job> §7- Join a job");
        player.sendMessage("§e/jobs leave <job> §7- Leave a job");
        player.sendMessage("§e/jobs list §7- List all available jobs");
        player.sendMessage("§e/jobs info [job/player] §7- Show job or player information");
        player.sendMessage("§e/jobs stats [player] §7- Show job statistics");
        
        if (player.hasPermission("universejobs.rewards.use")) {
            player.sendMessage("§e/jobs rewards §7- Access job rewards");
        }
        
        if (player.hasPermission("universejobs.admin.xpbonus")) {
            player.sendMessage("§e/jobs xpbonus §7- Manage XP bonuses");
        }
        
        if (player.hasPermission("universejobs.admin.actionlimits")) {
            player.sendMessage("§e/jobs actionlimit §7- Manage action limits");
        }
    }
    
    /**
     * Send help message to console.
     * 
     * @param sender The console sender
     */
    private void sendConsoleHelp(CommandSender sender) {
        sender.sendMessage("§6UniverseJobs Console Commands:");
        sender.sendMessage("§e/jobs reload §7- Reload the plugin configuration");
        sender.sendMessage("§e/jobs xpbonus <give|remove|list> §7- Manage XP bonuses");
        sender.sendMessage("§e/jobs actionlimit <restore|status> §7- Manage action limits");
        sender.sendMessage("§e/jobs exp <give|take|set> <player> <job> <amount> §7- Manage player XP");
        sender.sendMessage("§e/jobs migrate §7- Migrate data between storage types");
        sender.sendMessage("§e/jobs debug <player> §7- Show debug information");
    }
}