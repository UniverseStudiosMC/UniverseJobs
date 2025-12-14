package fr.ax_dev.universejobs.command;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.command.handler.*;
import fr.ax_dev.universejobs.config.LanguageManager;
import fr.ax_dev.universejobs.job.JobManager;
import fr.ax_dev.universejobs.utils.MessageUtils;
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
    private final ActionLimitCommandHandler actionLimitHandler;
    private final AdminJobCommandHandler adminJobHandler;
    private final MenuCommandHandler menuHandler;
    private final DatabaseCommandHandler databaseHandler;
    // Command constants
    private static final String CMD_JOIN = "join";
    private static final String CMD_LEAVE = "leave";
    private static final String CMD_INFO = "info";
    private static final String CMD_LIST = "list";
    private static final String CMD_STATS = "stats";
    private static final String CMD_REWARDS = "rewards";
    private static final String CMD_ACTION_LIMIT = "actionlimit";
    private static final String CMD_MENU = "menu";
    private static final String CMD_ADMIN = "admin";
    private static final String CMD_DATABASE = "database";

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
        this.actionLimitHandler = new ActionLimitCommandHandler(plugin);
        this.adminJobHandler = new AdminJobCommandHandler(plugin, jobManager);
        this.menuHandler = new MenuCommandHandler(plugin);
        this.databaseHandler = new DatabaseCommandHandler(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!validateCommandStructure(args)) {
            return true;
        }

        if (args.length == 0) {
            // If sender is a player, open main menu; otherwise show help
            if (sender instanceof Player player) {
                return menuHandler.handleCommand(sender, new String[0]);
            } else {
                sendConsoleHelp(sender);
                return true;
            }
        }

        String subCommand = sanitizeInput(args[0].toLowerCase());

        if (!isValidSubCommand(subCommand)) {
            sendHelpBasedOnSender(sender);
            return true;
        }

        if (!validatePlayerRequirement(sender, subCommand)) {
            return true;
        }

        if (sender instanceof Player player && !checkRateLimit(player)) {
            return true;
        }

        try {
            boolean handled = false;

            switch (subCommand) {
                case CMD_JOIN, CMD_LEAVE -> handled = joinLeaveHandler.handleCommand(sender, args);
                case CMD_INFO, CMD_LIST, CMD_STATS -> handled = infoStatsHandler.handleCommand(sender, args);
                case CMD_REWARDS -> handled = rewardsHandler.handleCommand(sender, args);
                case CMD_ACTION_LIMIT -> handled = actionLimitHandler.handleCommand(sender, args);
                case CMD_ADMIN -> handled = adminJobHandler.handleAdminCommand(sender, args);
                case CMD_MENU -> handled = menuHandler.handleCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                case CMD_DATABASE -> handled = this.databaseHandler.handleDatabaseCommand(sender, args);
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
                subCommands.addAll(Arrays.asList(CMD_JOIN, CMD_LEAVE, CMD_INFO, CMD_LIST, CMD_STATS, CMD_MENU));
                if (sender.hasPermission("universejobs.rewards.use")) {
                    subCommands.add(CMD_REWARDS);
                }
            }

            // Admin commands available to both console and players
            if (sender.hasPermission("universejobs.admin")) {
                subCommands.add(CMD_ADMIN); // All admin commands under /jobs admin
                subCommands.add(CMD_DATABASE); // Database management commands
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
                case CMD_ACTION_LIMIT -> completions.addAll(actionLimitHandler.getTabCompletions(sender, args));
                case CMD_ADMIN -> completions.addAll(adminJobHandler.getTabCompletions(sender, args));
                case CMD_MENU -> completions.addAll(menuHandler.getTabCompletions(sender, Arrays.copyOfRange(args, 1, args.length)));
                default -> {
                    // Unknown subcommand - no additional completions
                }
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

        String sanitized = input.replaceAll("[;&|`$(){}\\[\\]<>\"'\\\\]", "");
        return sanitized.substring(0, Math.min(sanitized.length(), 64));
    }

    /**
     * Check if subcommand is valid.
     *
     * @param subCommand The subcommand to validate
     * @return true if valid
     */
    private boolean isValidSubCommand(String subCommand) {
        Set<String> validCommands = Set.of(CMD_JOIN, CMD_LEAVE, CMD_INFO, CMD_LIST, CMD_STATS, CMD_REWARDS, CMD_ACTION_LIMIT, CMD_MENU, CMD_ADMIN, CMD_DATABASE);
        return validCommands.contains(subCommand);
    }

    /**
     * Check if a command requires a player.
     *
     * @param subCommand The subcommand to check
     * @return true if the command requires a player
     */
    private boolean requiresPlayer(String subCommand) {
        Set<String> playerOnlyCommands = Set.of(CMD_JOIN, CMD_LEAVE, CMD_INFO, CMD_LIST, CMD_STATS, CMD_REWARDS, CMD_MENU);
        return playerOnlyCommands.contains(subCommand);
    }

    /**
     * Send help message to a player.
     *
     * @param player The player to send help to
     */
    private void sendHelp(Player player) {
        for (String line : languageManager.getMessageList("commands.help.player")) {
            MessageUtils.sendMessage(player, line);
        }

        if (player.hasPermission("universejobs.rewards.use")) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.help.rewards"));
        }

        if (player.hasPermission("universejobs.admin")) {
            for (String line : languageManager.getMessageList("commands.help.admin")) {
                MessageUtils.sendMessage(player, line);
            }
        }
    }

    /**
     * Send help message to console.
     *
     * @param sender The console sender
     */
    private void sendConsoleHelp(CommandSender sender) {
        for (String line : languageManager.getMessageList("commands.help.console")) {
            sender.sendMessage(line);
        }
    }

    /**
     * Send help message based on sender type.
     */
    private void sendHelpBasedOnSender(CommandSender sender) {
        if (sender instanceof Player player) {
            sendHelp(player);
        } else {
            sendConsoleHelp(sender);
        }
    }

    /**
     * Validate player requirement for subcommand.
     */
    private boolean validatePlayerRequirement(CommandSender sender, String subCommand) {
        boolean requiresPlayer = requiresPlayer(subCommand);
        if (requiresPlayer && !(sender instanceof Player)) {
            sender.sendMessage(languageManager.getMessage("commands.players-only"));
            return false;
        }
        return true;
    }
}
