package fr.ax_dev.jobsAdventure.command;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.bonus.XpBonus;
import fr.ax_dev.jobsAdventure.bonus.XpBonusManager;
import fr.ax_dev.jobsAdventure.config.LanguageManager;
import fr.ax_dev.jobsAdventure.job.Job;
import fr.ax_dev.jobsAdventure.job.JobManager;
import fr.ax_dev.jobsAdventure.job.PlayerJobData;
import fr.ax_dev.jobsAdventure.reward.Reward;
import fr.ax_dev.jobsAdventure.reward.RewardManager;
import fr.ax_dev.jobsAdventure.reward.gui.RewardGuiManager;
import fr.ax_dev.jobsAdventure.utils.MessageUtils;
import fr.ax_dev.jobsAdventure.utils.LanguageFileMigrator;
import fr.ax_dev.jobsAdventure.utils.LegacyConverterTest;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handles the main job command and its subcommands.
 */
public class JobCommand implements CommandExecutor, TabCompleter {
    
    private final JobsAdventure plugin;
    private final JobManager jobManager;
    private final LanguageManager languageManager;
    private final XpBonusManager bonusManager;
    private final RewardManager rewardManager;
    private final RewardGuiManager rewardGuiManager;
    
    // Security patterns for input validation
    private static final Pattern SAFE_JOB_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,32}$");
    private static final Pattern SAFE_PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");
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
    public JobCommand(JobsAdventure plugin, JobManager jobManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.languageManager = plugin.getLanguageManager();
        this.bonusManager = plugin.getBonusManager();
        this.rewardManager = plugin.getRewardManager();
        this.rewardGuiManager = plugin.getRewardGuiManager();
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
            switch (subCommand) {
                case "join" -> handleJoinCommand((Player) sender, args);
                case "leave" -> handleLeaveCommand((Player) sender, args);
                case "info" -> handleInfoCommand((Player) sender, args);
                case "list" -> handleListCommand((Player) sender);
                case "stats" -> handleStatsCommand((Player) sender, args);
                case "rewards" -> handleRewardsCommand((Player) sender, args);
                case "xpbonus" -> handleXpBonusCommand(sender, args);
                case "migrate" -> handleMigrateCommand(sender, args);
                case "reload" -> handleReloadCommand(sender);
                case "debug" -> handleDebugCommand(sender, args);
                default -> {
                    if (sender instanceof Player player) {
                        sendHelp(player);
                    } else {
                        sendConsoleHelp(sender);
                    }
                }
            }
        } catch (Exception e) {
            String senderName = sender instanceof Player ? sender.getName() : "Console";
            plugin.getLogger().warning("Error executing command for " + senderName + ": " + e.getMessage());
        }
        
        return true;
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
     * Validate job ID format.
     * 
     * @param jobId The job ID to validate
     * @return true if valid
     */
    private boolean isValidJobId(String jobId) {
        return jobId != null && SAFE_JOB_ID_PATTERN.matcher(jobId).matches();
    }
    
    /**
     * Validate player name format.
     * 
     * @param playerName The player name to validate
     * @return true if valid
     */
    private boolean isValidPlayerName(String playerName) {
        return playerName != null && SAFE_PLAYER_NAME_PATTERN.matcher(playerName).matches();
    }
    
    /**
     * Check if subcommand is valid.
     * 
     * @param subCommand The subcommand to validate
     * @return true if valid
     */
    private boolean isValidSubCommand(String subCommand) {
        Set<String> validCommands = Set.of("join", "leave", "info", "list", "stats", "rewards", "xpbonus", "migrate", "reload", "monitor", "debug");
        return validCommands.contains(subCommand);
    }
    
    /**
     * Check if a command requires a player.
     * 
     * @param subCommand The subcommand to check
     * @return true if the command requires a player
     */
    private boolean requiresPlayer(String subCommand) {
        Set<String> playerOnlyCommands = Set.of("join", "leave", "info", "list", "stats", "rewards");
        return playerOnlyCommands.contains(subCommand);
    }
    
    /**
     * Send help message to console.
     * 
     * @param sender The console sender
     */
    private void sendConsoleHelp(CommandSender sender) {
        sender.sendMessage("§6JobsAdventure Console Commands:");
        sender.sendMessage("§e/jobs reload §7- Reload the plugin configuration");
        sender.sendMessage("§e/jobs xpbonus <add|remove|list> §7- Manage XP bonuses");
        sender.sendMessage("§e/jobs migrate §7- Migrate data between storage types");
        sender.sendMessage("§e/jobs monitor <player> §7- Monitor a player's actions");
        sender.sendMessage("§e/jobs debug §7- Toggle debug mode");
    }
    
    /**
     * Handle the join subcommand.
     */
    private void handleJoinCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(languageManager.getMessage("commands.join.usage"));
            return;
        }
        
        String jobId = sanitizeInput(args[1]);
        
        // Validate job ID format
        if (!isValidJobId(jobId)) {
            return;
        }
        
        Job job = jobManager.getJob(jobId);
        
        if (job == null) {
            player.sendMessage(languageManager.getMessage("commands.join.job-not-found", "job", jobId));
            return;
        }
        
        // Check if player already has the job
        if (jobManager.hasJob(player, jobId)) {
            player.sendMessage(languageManager.getMessage("commands.join.already-have", "job", job.getName()));
            return;
        }
        
        // Check max jobs limit
        Set<String> playerJobs = jobManager.getPlayerJobs(player);
        int maxJobs = plugin.getConfigManager().getMaxJobsPerPlayer();
        if (playerJobs.size() >= maxJobs) {
            player.sendMessage(languageManager.getMessage("commands.join.max-jobs-reached", "max", String.valueOf(maxJobs)));
            return;
        }
        
        // Check permission
        if (!player.hasPermission(job.getPermission())) {
            player.sendMessage(languageManager.getMessage("commands.join.no-permission", "job", job.getName()));
            return;
        }
        
        // Join the job
        if (jobManager.joinJob(player, jobId)) {
            player.sendMessage(languageManager.getMessage("commands.join.success", "job", job.getName()));
        } else {
            player.sendMessage(languageManager.getMessage("commands.join.failed", "job", job.getName()));
        }
    }
    
    /**
     * Handle the leave subcommand.
     */
    private void handleLeaveCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(languageManager.getMessage("commands.leave.usage"));
            return;
        }
        
        String jobId = sanitizeInput(args[1]);
        
        // Validate job ID format
        if (!isValidJobId(jobId)) {
            return;
        }
        
        Job job = jobManager.getJob(jobId);
        
        if (job == null) {
            player.sendMessage(languageManager.getMessage("commands.leave.job-not-found", "job", jobId));
            return;
        }
        
        // Check if player has the job
        if (!jobManager.hasJob(player, jobId)) {
            player.sendMessage(languageManager.getMessage("commands.leave.dont-have", "job", job.getName()));
            return;
        }
        
        // Check if this is a default job
        if (plugin.getConfigManager().isDefaultJob(jobId)) {
            player.sendMessage(languageManager.getMessage("commands.leave.default-job", "job", job.getName()));
            return;
        }
        
        // Leave the job
        if (jobManager.leaveJob(player, jobId)) {
            player.sendMessage(languageManager.getMessage("commands.leave.success", "job", job.getName()));
        } else {
            player.sendMessage(languageManager.getMessage("commands.leave.failed", "job", job.getName()));
        }
    }
    
    /**
     * Handle the info subcommand.
     */
    private void handleInfoCommand(Player player, String[] args) {
        if (args.length < 2) {
            // Show player's own job info
            showPlayerJobInfo(player, player);
        } else {
            String target = sanitizeInput(args[1]);
            
            // Validate target format
            if (target.isEmpty() || target.length() > 32) {
                return;
            }
            
            // Check if target is a job name or player name
            Job job = jobManager.getJob(target);
            if (job != null) {
                showJobInfo(player, job);
            } else {
                // Validate player name format before lookup
                if (!isValidPlayerName(target)) {
                    return;
                }
                
                Player targetPlayer = plugin.getServer().getPlayer(target);
                if (targetPlayer != null) {
                    showPlayerJobInfo(player, targetPlayer);
                } else {
                    MessageUtils.sendMessage(player, plugin.getConfigManager().getMessage("job-or-player-not-found", 
                            "&cJob or player '{target}' not found!"), "target", target);
                }
            }
        }
    }
    
    /**
     * Show information about a specific job.
     */
    private void showJobInfo(Player player, Job job) {
        MessageUtils.sendMessage(player, "&6=== " + job.getName() + " ===");
        MessageUtils.sendMessage(player, "&7Description: &f" + job.getDescription());
        MessageUtils.sendMessage(player, "&7Max Level: &f" + job.getMaxLevel());
        MessageUtils.sendMessage(player, "&7Permission: &f" + job.getPermission());
        
        if (!job.getLore().isEmpty()) {
            MessageUtils.sendMessage(player, "&7Lore:");
            for (String line : job.getLore()) {
                MessageUtils.sendMessage(player, "&f  " + line);
            }
        }
        
        MessageUtils.sendMessage(player, "&7Action Types: &f" + 
                job.getActionTypes().stream()
                        .map(Enum::name)
                        .collect(Collectors.joining(", ")));
    }
    
    /**
     * Show a player's job information.
     */
    private void showPlayerJobInfo(Player viewer, Player target) {
        PlayerJobData data = jobManager.getPlayerData(target);
        Set<String> playerJobs = data.getJobs();
        
        if (playerJobs.isEmpty()) {
            if (viewer == target) {
                MessageUtils.sendMessage(viewer, languageManager.getMessage("commands.stats.no-jobs-self"));
            } else {
                MessageUtils.sendMessage(viewer, languageManager.getMessage("commands.stats.no-jobs-other", "player", target.getName()));
            }
            return;
        }
        
        String playerName = viewer == target ? "Your" : target.getName() + "'s";
        MessageUtils.sendMessage(viewer, "&6=== " + playerName + " Jobs ===");
        
        for (String jobId : playerJobs) {
            Job job = jobManager.getJob(jobId);
            if (job != null) {
                int level = jobManager.getLevel(target, jobId); // Use JobManager for accurate level calculation
                double[] progress = data.getXpProgress(jobId);
                
                MessageUtils.sendMessage(viewer, String.format("&e%s &7- Level %d &8(%.1f/%.1f XP)", 
                        job.getName(), level, progress[0], progress[1]));
            }
        }
    }
    
    /**
     * Handle the list subcommand.
     */
    private void handleListCommand(Player player) {
        List<Job> jobs = new ArrayList<>(jobManager.getEnabledJobs());
        
        if (jobs.isEmpty()) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.list.no-jobs"));
            return;
        }
        
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.list.header"));
        for (Job job : jobs) {
            String status = jobManager.hasJob(player, job.getId()) ? 
                languageManager.getMessage("commands.list.status-have") : 
                languageManager.getMessage("commands.list.status-dont-have");
            MessageUtils.sendMessage(player, String.format("%s &e%s &7- %s", status, job.getName(), job.getDescription()));
        }
    }
    
    /**
     * Handle the stats subcommand.
     */
    private void handleStatsCommand(Player player, String[] args) {
        Player target = player;
        
        if (args.length > 1) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                MessageUtils.sendMessage(player, languageManager.getMessage("commands.player-not-found"));
                return;
            }
        }
        
        showPlayerJobInfo(player, target);
    }
    
    /**
     * Handle the xpbonus subcommand.
     */
    private void handleXpBonusCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("jobsadventure.admin.xpbonus")) {
            MessageUtils.sendMessage((Player) sender, languageManager.getMessage("commands.no-permission"));
            return;
        }
        
        if (args.length == 1) {
            sendXpBonusHelp(sender);
            return;
        }
        
        String xpSubCommand = args[1].toLowerCase();
        String[] xpArgs = Arrays.copyOfRange(args, 1, args.length);
        
        switch (xpSubCommand) {
            case "give" -> handleXpBonusGive(sender, xpArgs);
            case "remove" -> handleXpBonusRemove(sender, xpArgs);
            case "list" -> handleXpBonusList(sender, xpArgs);
            case "info" -> handleXpBonusInfo(sender);
            case "cleanup" -> handleXpBonusCleanup(sender);
            default -> sendXpBonusHelp(sender);
        }
    }
    
    /**
     * Handle the xpbonus give subcommand.
     */
    private void handleXpBonusGive(CommandSender sender, String[] args) {
        // /jobs xpbonus give <player|*> <multiplier> <duration> [job] [reason]
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /jobs xpbonus give <player|*> <multiplier> <duration> [job] [reason]");
            sender.sendMessage("§7Examples:");
            sender.sendMessage("§7  /jobs xpbonus give * 2.0 3600 - Give all online players 2x XP for 1 hour");
            sender.sendMessage("§7  /jobs xpbonus give Player123 1.5 1800 miner Mining Event");
            return;
        }
        
        // Validate and sanitize target
        String target = sanitizeInput(args[1]);
        if (!target.equals("*") && !isValidPlayerName(target)) {
            return;
        }
        
        // Validate multiplier with strict bounds checking
        double multiplier;
        try {
            String multiplierStr = sanitizeInput(args[2]);
            if (multiplierStr.isEmpty() || multiplierStr.length() > 10) {
                throw new NumberFormatException("Invalid multiplier format");
            }
            multiplier = Double.parseDouble(multiplierStr);
        } catch (NumberFormatException e) {
            return;
        }
        
        // Strict multiplier bounds checking with security in mind
        if (multiplier <= 0.0 || multiplier > 10.0 || Double.isNaN(multiplier) || Double.isInfinite(multiplier)) {
            return;
        }
        
        // Validate duration with strict bounds checking
        long duration;
        try {
            String durationStr = sanitizeInput(args[3]);
            if (durationStr.isEmpty() || durationStr.length() > 10) {
                throw new NumberFormatException("Invalid duration format");
            }
            duration = Long.parseLong(durationStr);
        } catch (NumberFormatException e) {
            return;
        }
        
        // Strict duration bounds checking (1 second to 24 hours)
        if (duration <= 0 || duration > 86400) {
            return;
        }
        
        // Validate job ID if provided
        String jobId = null;
        if (args.length > 4) {
            jobId = sanitizeInput(args[4]);
            if (!jobId.equals("*") && !isValidJobId(jobId)) {
                return;
            }
        }
        
        // Validate and sanitize reason
        String reason = "Admin bonus";
        if (args.length > 5) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 5; i < args.length && i < 10; i++) { // Limit to prevent abuse
                String part = sanitizeInput(args[i]);
                if (!part.isEmpty()) {
                    if (reasonBuilder.length() > 0) reasonBuilder.append(" ");
                    reasonBuilder.append(part);
                }
            }
            reason = reasonBuilder.length() > 0 ? reasonBuilder.toString() : "Admin bonus";
            
            // Limit reason length
            if (reason.length() > 100) {
                reason = reason.substring(0, 100);
            }
        }
        
        // Validate job if specified
        if (jobId != null && !jobId.equals("*")) {
            Job job = jobManager.getJob(jobId);
            if (job == null) {
                return;
            }
        }
        
        String senderName = sender instanceof Player ? sender.getName() : "Console";
        
        if (target.equals("*")) {
            // Give to all online players
            bonusManager.addGlobalBonus(multiplier, duration, reason, senderName);
        } else {
            // Give to specific player
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null) {
                return;
            }
            
            if (jobId == null || jobId.equals("*")) {
                bonusManager.addPlayerBonus(targetPlayer.getUniqueId(), multiplier, duration, reason, senderName);
            } else {
                bonusManager.addJobBonus(targetPlayer.getUniqueId(), jobId, multiplier, duration, reason, senderName);
            }
            
            // Bonus given successfully
        }
    }
    
    /**
     * Handle the xpbonus remove subcommand.
     */
    private void handleXpBonusRemove(CommandSender sender, String[] args) {
        // /jobs xpbonus remove <player> [job]
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /jobs xpbonus remove <player> [job]");
            return;
        }
        
        // Validate player name
        String playerName = sanitizeInput(args[1]);
        if (!isValidPlayerName(playerName)) {
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            return;
        }
        
        // Validate job ID if provided
        String jobId = null;
        if (args.length > 2) {
            jobId = sanitizeInput(args[2]);
            if (!isValidJobId(jobId)) {
                return;
            }
        }
        
        if (jobId == null) {
            // Remove all bonuses
            bonusManager.removeAllBonuses(targetPlayer.getUniqueId());
        } else {
            // Remove job-specific bonuses
            List<XpBonus> bonuses = bonusManager.getActiveBonuses(targetPlayer.getUniqueId(), jobId);
            for (XpBonus bonus : bonuses) {
                bonusManager.removeBonus(bonus);
            }
        }
    }
    
    /**
     * Handle the xpbonus list subcommand.
     */
    private void handleXpBonusList(CommandSender sender, String[] args) {
        // /jobs xpbonus list [player]
        String playerName = args.length > 1 ? args[1] : (sender instanceof Player ? sender.getName() : null);
        
        if (playerName == null) {
            sender.sendMessage("§cUsage: /jobs xpbonus list [player]");
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            return;
        }
        
        List<XpBonus> bonuses = bonusManager.getActiveBonuses(targetPlayer.getUniqueId());
        
        if (bonuses.isEmpty()) {
            return;
        }
    }
    
    /**
     * Handle the xpbonus info subcommand.
     */
    private void handleXpBonusInfo(CommandSender sender) {
        bonusManager.getStats();
    }
    
    /**
     * Handle the xpbonus cleanup subcommand.
     */
    private void handleXpBonusCleanup(CommandSender sender) {
        bonusManager.cleanupExpiredBonuses();
    }
    
    /**
     * Send XP bonus help message.
     */
    private void sendXpBonusHelp(CommandSender sender) {
        // XP bonus help messages removed
    }
    
    /**
     * Handle the rewards subcommand.
     */
    private void handleRewardsCommand(Player player, String[] args) {
        if (!player.hasPermission("jobsadventure.rewards.use")) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.no-permission"));
            return;
        }
        
        if (args.length < 2) {
            return;
        }
        
        String rewardSubCommand = args[1].toLowerCase();
        
        switch (rewardSubCommand) {
            case "open" -> {
                if (args.length < 3) {
                    MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.open.usage"));
                    return;
                }
                
                String jobId = args[2];
                Job job = jobManager.getJob(jobId);
                
                if (job == null) {
                    MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.job-not-found", "job", jobId));
                    return;
                }
                
                // Check if player has the job
                if (!jobManager.hasJob(player, jobId)) {
                    MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.must-have-job", "job", job.getName()));
                    return;
                }
                
                rewardGuiManager.openRewardsGui(player, jobId);
            }
            
            case "list" -> {
                String jobId = args.length > 2 ? args[2] : null;
                if (jobId == null) {
                    // List all jobs that have rewards
                    for (Job job : jobManager.getAllJobs()) {
                    }
                } else {
                    // List rewards for specific job
                    Job job = jobManager.getJob(jobId);
                    if (job == null) {
                        return;
                    }
                    
                    List<Reward> rewards = rewardManager.getJobRewards(jobId);
                    if (rewards.isEmpty()) {
                        return;
                    }
                }
            }
            
            case "claim" -> {
                if (args.length < 4) {
                    return;
                }
                
                String jobId = args[2];
                String rewardId = args[3];
                
                var reward = rewardManager.getReward(jobId, rewardId);
                if (reward == null) {
                    return;
                }
                
                if (rewardManager.claimReward(player, reward)) {
                    MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.claim.success"));
                } else {
                    MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.claim.failed"));
                }
            }
            
            case "info" -> {
                if (args.length < 4) {
                    return;
                }
                
                String jobId = args[2];
                String rewardId = args[3];
                
                var reward = rewardManager.getReward(jobId, rewardId);
                if (reward == null) {
                    MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.reward-not-found"));
                    return;
                }
            }
            
            case "admin" -> {
                if (!player.hasPermission("jobsadventure.rewards.admin")) {
                    return;
                }
                
                if (args.length < 3) {
                    return;
                }
                
                String adminSubCommand = args[2].toLowerCase();
                switch (adminSubCommand) {
                    case "give" -> {
                        if (args.length < 6) {
                            return;
                        }
                        
                        String targetName = args[3];
                        String jobId = args[4];
                        String rewardId = args[5];
                        
                        Player target = Bukkit.getPlayer(targetName);
                        if (target == null) {
                            return;
                        }
                        
                        var reward = rewardManager.getReward(jobId, rewardId);
                        if (reward == null) {
                            return;
                        }
                        
                        rewardManager.claimReward(target, reward);
                    }
                    
                    case "reset" -> {
                        if (args.length < 5) {
                            return;
                        }
                        
                        String targetName = args[3];
                        String jobId = args[4];
                        
                        Player target = Bukkit.getPlayer(targetName);
                        if (target == null) {
                            return;
                        }
                        
                        rewardManager.resetJobRewards(target, jobId);
                    }
                }
            }
            
            case "reload" -> {
                if (!player.hasPermission("jobsadventure.rewards.admin")) {
                    return;
                }
                
                rewardManager.reloadRewards();
            }
            
            case "debug" -> {
                if (!player.hasPermission("jobsadventure.rewards.admin")) {
                    return;
                }
                
                if (args.length < 3) {
                    return;
                }
                
                String jobId = args[2];
                rewardManager.debugPlayerRewards(player, jobId);
            }
            
            default -> {
                // Unknown rewards command
            }
        }
    }

    /**
     * Handle the migrate subcommand for converting language files.
     */
    private void handleMigrateCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("jobsadventure.admin.migrate")) {
            return;
        }
        
        if (args.length < 2) {
            sendMigrateHelp(sender);
            return;
        }
        
        String migrateSubCommand = args[1].toLowerCase();
        
        switch (migrateSubCommand) {
            case "test" -> handleMigrateTest(sender);
            case "validate" -> handleMigrateValidate(sender);
            case "backup" -> handleMigrateBackup(sender);
            case "convert" -> handleMigrateConvert(sender, args);
            case "restore" -> handleMigrateRestore(sender);
            default -> sendMigrateHelp(sender);
        }
    }
    
    /**
     * Handle migration test command.
     */
    private void handleMigrateTest(CommandSender sender) {
        try {
            LegacyConverterTest.runTests();
        } catch (Exception e) {
            // Test failed
        }
    }
    
    /**
     * Handle migration validate command.
     */
    private void handleMigrateValidate(CommandSender sender) {
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        LanguageFileMigrator migrator = new LanguageFileMigrator(languagesDir, plugin.getLogger());
        migrator.validateFiles();
    }
    
    /**
     * Handle migration backup command.
     */
    private void handleMigrateBackup(CommandSender sender) {
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        LanguageFileMigrator migrator = new LanguageFileMigrator(languagesDir, plugin.getLogger());
        migrator.createBackups();
    }
    
    /**
     * Handle migration convert command.
     */
    private void handleMigrateConvert(CommandSender sender, String[] args) {
        boolean createBackup = true;
        
        // Check for --no-backup flag
        if (args.length > 2 && args[2].equals("--no-backup")) {
            createBackup = false;
        }
        
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        LanguageFileMigrator migrator = new LanguageFileMigrator(languagesDir, plugin.getLogger());
        migrator.migrateAllFiles(createBackup);
    }
    
    /**
     * Handle migration restore command.
     */
    private void handleMigrateRestore(CommandSender sender) {
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        LanguageFileMigrator migrator = new LanguageFileMigrator(languagesDir, plugin.getLogger());
        migrator.restoreFromBackups();
    }
    
    /**
     * Send migration help message.
     */
    private void sendMigrateHelp(CommandSender sender) {
        // Migration help removed
    }
    
    /**
     * Handle the reload subcommand.
     */
    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("jobsadventure.admin")) {
            sender.sendMessage(languageManager.getMessage("commands.no-permission"));
            return;
        }
        
        plugin.getConfigManager().reloadConfig();
        jobManager.reloadJobs();
        rewardManager.reloadRewards();
        sender.sendMessage("§aJobsAdventure configuration reloaded successfully!");
    }
    
    /**
     * Send help message to player.
     */
    private void sendHelp(Player player) {
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.help.header"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.help.join"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.help.leave"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.help.info"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.help.list"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.help.stats"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.help.rewards"));
        
        if (player.hasPermission("jobsadventure.admin")) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.help.reload"));
        }
        
        if (player.hasPermission("jobsadventure.admin.xpbonus")) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.help.xpbonus"));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main subcommands
            List<String> subCommands = new ArrayList<>();
            
            // Commands available to players
            if (player != null) {
                subCommands.addAll(Arrays.asList("join", "leave", "info", "list", "stats"));
                if (sender.hasPermission("jobsadventure.rewards.use")) {
                    subCommands.add("rewards");
                }
            }
            
            // Admin commands available to both console and players
            if (sender.hasPermission("jobsadventure.admin")) {
                subCommands.add("reload");
                subCommands.add("debug");
                subCommands.add("monitor");
            }
            if (sender.hasPermission("jobsadventure.admin.migrate")) {
                subCommands.add("migrate");
            }
            if (sender.hasPermission("jobsadventure.admin.xpbonus")) {
                subCommands.add("xpbonus");
            }
            
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();
            
            switch (subCommand) {
                case "join" -> {
                    // Available jobs that player doesn't have
                    if (player != null) {
                        for (Job job : jobManager.getEnabledJobs()) {
                            if (!jobManager.hasJob(player, job.getId()) && 
                                job.getId().toLowerCase().startsWith(input)) {
                                completions.add(job.getId());
                            }
                        }
                    }
                }
                case "leave" -> {
                    // Jobs that player has
                    if (player != null) {
                        for (String jobId : jobManager.getPlayerJobs(player)) {
                            if (jobId.toLowerCase().startsWith(input)) {
                                completions.add(jobId);
                            }
                        }
                    }
                }
                case "info" -> {
                    // All jobs and online players
                    for (Job job : jobManager.getAllJobs()) {
                        if (job.getId().toLowerCase().startsWith(input)) {
                            completions.add(job.getId());
                        }
                    }
                    for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                        if (onlinePlayer.getName().toLowerCase().startsWith(input)) {
                            completions.add(onlinePlayer.getName());
                        }
                    }
                }
                case "stats" -> {
                    // Online players
                    for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                        if (onlinePlayer.getName().toLowerCase().startsWith(input)) {
                            completions.add(onlinePlayer.getName());
                        }
                    }
                }
                case "rewards" -> {
                    // Rewards subcommands
                    List<String> rewardSubCommands = new ArrayList<>(Arrays.asList("open", "list", "claim", "info"));
                    if (player.hasPermission("jobsadventure.rewards.admin")) {
                        rewardSubCommands.addAll(Arrays.asList("admin", "reload", "debug"));
                    }
                    
                    for (String rewardSubCommand : rewardSubCommands) {
                        if (rewardSubCommand.startsWith(input)) {
                            completions.add(rewardSubCommand);
                        }
                    }
                }
                case "migrate" -> {
                    // Migration subcommands
                    if (player.hasPermission("jobsadventure.admin.migrate")) {
                        List<String> migrateSubCommands = Arrays.asList("test", "validate", "backup", "convert", "restore");
                        for (String migrateSubCommand : migrateSubCommands) {
                            if (migrateSubCommand.startsWith(input)) {
                                completions.add(migrateSubCommand);
                            }
                        }
                    }
                }
                case "xpbonus" -> {
                    // XP Bonus subcommands
                    if (player.hasPermission("jobsadventure.admin.xpbonus")) {
                        List<String> xpSubCommands = Arrays.asList("give", "remove", "list", "info", "cleanup");
                        for (String xpSubCommand : xpSubCommands) {
                            if (xpSubCommand.startsWith(input)) {
                                completions.add(xpSubCommand);
                            }
                        }
                    }
                }
            }
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("rewards")) {
            // Handle Rewards tab completion
            if (player.hasPermission("jobsadventure.rewards.use")) {
                completions.addAll(getRewardsTabCompletions(player, args));
            }
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("migrate")) {
            // Handle Migration tab completion
            if (player.hasPermission("jobsadventure.admin.migrate")) {
                completions.addAll(getMigrateTabCompletions(args));
            }
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("xpbonus")) {
            // Handle XP Bonus tab completion
            if (player.hasPermission("jobsadventure.admin.xpbonus")) {
                completions.addAll(getXpBonusTabCompletions(args));
            }
        }
        
        return completions;
    }
    
    /**
     * Get tab completions for Rewards commands.
     */
    private List<String> getRewardsTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 3) {
            String rewardSubCommand = args[1].toLowerCase();
            String input = args[2].toLowerCase();
            
            switch (rewardSubCommand) {
                case "open" -> {
                    // Jobs that player has
                    for (Job job : jobManager.getAllJobs()) {
                        if (jobManager.hasJob(player, job.getId()) && 
                            job.getId().toLowerCase().startsWith(input)) {
                            completions.add(job.getId());
                        }
                    }
                }
                case "list" -> {
                    // All jobs (optional parameter)
                    for (Job job : jobManager.getAllJobs()) {
                        if (job.getId().toLowerCase().startsWith(input)) {
                            completions.add(job.getId());
                        }
                    }
                }
                case "claim" -> {
                    // Jobs that player has
                    for (Job job : jobManager.getAllJobs()) {
                        if (jobManager.hasJob(player, job.getId()) && 
                            job.getId().toLowerCase().startsWith(input)) {
                            completions.add(job.getId());
                        }
                    }
                }
                case "info" -> {
                    // All jobs
                    for (Job job : jobManager.getAllJobs()) {
                        if (job.getId().toLowerCase().startsWith(input)) {
                            completions.add(job.getId());
                        }
                    }
                }
                case "admin" -> {
                    if (player.hasPermission("jobsadventure.rewards.admin")) {
                        List<String> adminSubCommands = Arrays.asList("give", "reset");
                        for (String adminSubCommand : adminSubCommands) {
                            if (adminSubCommand.startsWith(input)) {
                                completions.add(adminSubCommand);
                            }
                        }
                    }
                }
                case "debug" -> {
                    if (player.hasPermission("jobsadventure.rewards.admin")) {
                        // All jobs for debug command
                        for (Job job : jobManager.getAllJobs()) {
                            if (job.getId().toLowerCase().startsWith(input)) {
                                completions.add(job.getId());
                            }
                        }
                    }
                }
            }
        } else if (args.length == 4) {
            String rewardSubCommand = args[1].toLowerCase();
            String jobId = args[2];
            String input = args[3].toLowerCase();
            
            switch (rewardSubCommand) {
                case "claim", "info" -> {
                    // Reward IDs for the specified job
                    List<Reward> rewards = rewardManager.getJobRewards(jobId);
                    for (Reward reward : rewards) {
                        if (reward.getId().toLowerCase().startsWith(input)) {
                            completions.add(reward.getId());
                        }
                    }
                }
                case "admin" -> {
                    if (player.hasPermission("jobsadventure.rewards.admin")) {
                        String adminSubCommand = args[2].toLowerCase();
                        if ("give".equals(adminSubCommand)) {
                            // Player names
                            return Bukkit.getOnlinePlayers().stream()
                                    .map(Player::getName)
                                    .filter(name -> name.toLowerCase().startsWith(input))
                                    .collect(Collectors.toList());
                        } else if ("reset".equals(adminSubCommand)) {
                            // Player names
                            return Bukkit.getOnlinePlayers().stream()
                                    .map(Player::getName)
                                    .filter(name -> name.toLowerCase().startsWith(input))
                                    .collect(Collectors.toList());
                        }
                    }
                }
            }
        } else if (args.length == 5 && args[1].equalsIgnoreCase("admin")) {
            if (player.hasPermission("jobsadventure.rewards.admin")) {
                String adminSubCommand = args[2].toLowerCase();
                String input = args[4].toLowerCase();
                
                if ("give".equals(adminSubCommand)) {
                    // Job IDs for give command
                    for (Job job : jobManager.getAllJobs()) {
                        if (job.getId().toLowerCase().startsWith(input)) {
                            completions.add(job.getId());
                        }
                    }
                } else if ("reset".equals(adminSubCommand)) {
                    // Job IDs for reset command
                    for (Job job : jobManager.getAllJobs()) {
                        if (job.getId().toLowerCase().startsWith(input)) {
                            completions.add(job.getId());
                        }
                    }
                }
            }
        } else if (args.length == 6 && args[1].equalsIgnoreCase("admin") && "give".equals(args[2].toLowerCase())) {
            if (player.hasPermission("jobsadventure.rewards.admin")) {
                String jobId = args[4];
                String input = args[5].toLowerCase();
                
                // Reward IDs for the specified job
                List<Reward> rewards = rewardManager.getJobRewards(jobId);
                for (Reward reward : rewards) {
                    if (reward.getId().toLowerCase().startsWith(input)) {
                        completions.add(reward.getId());
                    }
                }
            }
        }
        
        return completions;
    }
    
    /**
     * Get tab completions for Migration commands.
     */
    private List<String> getMigrateTabCompletions(String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 3) {
            String migrateSubCommand = args[1].toLowerCase();
            String input = args[2].toLowerCase();
            
            if ("convert".equals(migrateSubCommand)) {
                // Only --no-backup flag for convert command
                if ("--no-backup".startsWith(input)) {
                    completions.add("--no-backup");
                }
            }
        }
        
        return completions;
    }
    
    /**
     * Get tab completions for XP Bonus commands.
     */
    private List<String> getXpBonusTabCompletions(String[] args) {
        List<String> completions = new ArrayList<>();
        String xpSubCommand = args[1].toLowerCase();
        
        if (args.length == 3) {
            String input = args[2].toLowerCase();
            
            switch (xpSubCommand) {
                case "give" -> {
                    // Player names + *
                    completions.add("*");
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(input))
                            .collect(Collectors.toList()));
                }
                case "remove", "list" -> {
                    // Player names only
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(input))
                            .collect(Collectors.toList()));
                }
            }
        } else if (args.length == 4) {
            String input = args[3].toLowerCase();
            
            if (xpSubCommand.equals("give")) {
                // Multiplier suggestions
                List<String> multipliers = Arrays.asList("1.25", "1.5", "2.0", "3.0");
                completions.addAll(multipliers.stream()
                        .filter(mult -> mult.startsWith(input))
                        .collect(Collectors.toList()));
            } else if (xpSubCommand.equals("remove")) {
                // Job names for remove command
                completions.addAll(jobManager.getAllJobs().stream()
                        .map(Job::getId)
                        .filter(jobId -> jobId.toLowerCase().startsWith(input))
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 5 && xpSubCommand.equals("give")) {
            String input = args[4].toLowerCase();
            // Duration suggestions
            List<String> durations = Arrays.asList("300", "600", "1800", "3600", "7200");
            completions.addAll(durations.stream()
                    .filter(dur -> dur.startsWith(input))
                    .collect(Collectors.toList()));
        } else if (args.length == 6 && xpSubCommand.equals("give")) {
            String input = args[5].toLowerCase();
            // Job names
            completions.add("*");
            completions.addAll(jobManager.getAllJobs().stream()
                    .map(Job::getId)
                    .filter(jobId -> jobId.toLowerCase().startsWith(input))
                    .collect(Collectors.toList()));
        }
        
        return completions;
    }
    
    /**
     * Handle the monitor subcommand for performance monitoring.
     */
    private void handleMonitorCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("jobsadventure.admin.monitor")) {
            return;
        }
        
        if (args.length < 2) {
            sendMonitorHelp(sender);
            return;
        }
        
        String monitorSubCommand = sanitizeInput(args[1].toLowerCase());
        
        switch (monitorSubCommand) {
            case "status" -> handleMonitorStatus(sender);
            case "performance" -> handleMonitorPerformance(sender);
            case "memory" -> handleMonitorMemory(sender);
            case "storage" -> handleMonitorStorage(sender);
            case "events" -> handleMonitorEvents(sender);
            case "reset" -> handleMonitorReset(sender);
            default -> sendMonitorHelp(sender);
        }
    }
    
    /**
     * Handle the debug subcommand for detailed debugging information.
     */
    private void handleDebugCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("jobsadventure.admin.debug")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }
        
        // Toggle debug mode if no subcommand
        if (args.length < 2) {
            boolean currentDebug = plugin.getConfig().getBoolean("debug", false);
            plugin.getConfig().set("debug", !currentDebug);
            plugin.saveConfig();
            
            if (!currentDebug) {
                sender.sendMessage("§aDebug mode enabled.");
            } else {
                sender.sendMessage("§cDebug mode disabled.");
            }
            return;
        }
        
        String debugSubCommand = sanitizeInput(args[1].toLowerCase());
        
        switch (debugSubCommand) {
            case "on" -> {
                plugin.getConfig().set("debug", true);
                plugin.saveConfig();
                sender.sendMessage("§aDebug mode enabled.");
            }
            case "off" -> {
                plugin.getConfig().set("debug", false);
                plugin.saveConfig();
                sender.sendMessage("§cDebug mode disabled.");
            }
            case "player" -> handleDebugPlayer(sender, args);
            case "job" -> handleDebugJob(sender, args);
            case "listeners" -> handleDebugListeners(sender);
            case "threads" -> handleDebugThreads(sender);
            case "config" -> handleDebugConfig(sender);
            case "export" -> handleDebugExport(sender);
            default -> sendDebugHelp(sender);
        }
    }
    
    // Monitor subcommand handlers
    
    private void handleMonitorStatus(CommandSender sender) {
        // Status monitoring removed
    }
    
    private void handleMonitorPerformance(CommandSender sender) {
        // Performance monitoring removed
    }
    
    private void handleMonitorMemory(CommandSender sender) {
        // Memory monitoring removed
    }
    
    private void handleMonitorStorage(CommandSender sender) {
        // Storage monitoring removed
    }
    
    private void handleMonitorEvents(CommandSender sender) {
        // Event monitoring removed
    }
    
    private void handleMonitorReset(CommandSender sender) {
        // Force cleanup
        if (plugin.getJobManager() != null) {
            plugin.getJobManager().performCleanup();
        }
        
        // Force garbage collection
        System.gc();
    }
    
    // Debug subcommand handlers
    
    private void handleDebugPlayer(CommandSender sender, String[] args) {
        if (args.length < 3) {
            return;
        }
        
        String playerName = sanitizeInput(args[2]);
        if (!isValidPlayerName(playerName)) {
            return;
        }
        
        Player targetPlayer = plugin.getServer().getPlayer(playerName);
        if (targetPlayer == null) {
            return;
        }
        
        PlayerJobData data = plugin.getJobManager().getPlayerData(targetPlayer);
        if (data != null) {
            data.getDataSnapshot();
        }
    }
    
    private void handleDebugJob(CommandSender sender, String[] args) {
        if (args.length < 3) {
            return;
        }
        
        String jobId = sanitizeInput(args[2]);
        if (!isValidJobId(jobId)) {
            return;
        }
        
        Job job = plugin.getJobManager().getJob(jobId);
        if (job == null) {
            return;
        }
    }
    
    private void handleDebugListeners(CommandSender sender) {
        // Debug listeners removed
    }
    
    private void handleDebugThreads(CommandSender sender) {
        // Debug threads removed
    }
    
    private void handleDebugConfig(CommandSender sender) {
        // Debug config removed
    }
    
    private void handleDebugExport(CommandSender sender) {
        try {
            StringBuilder debugInfo = new StringBuilder();
            debugInfo.append("=== JobsAdventure Debug Export ===\n");
            debugInfo.append("Timestamp: ").append(new java.util.Date()).append("\n");
            debugInfo.append("Plugin Version: ").append(plugin.getDescription().getVersion()).append("\n");
            
            // Save to file
            File debugFile = new File(plugin.getDataFolder(), "debug-export-" + System.currentTimeMillis() + ".txt");
            java.nio.file.Files.write(debugFile.toPath(), debugInfo.toString().getBytes());
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Debug export failed", e);
        }
    }
    
    // Helper methods
    
    private void sendMonitorHelp(CommandSender sender) {
        // Monitor help removed
    }
    
    private void sendDebugHelp(CommandSender sender) {
        sender.sendMessage("§6JobsAdventure Debug Commands:");
        sender.sendMessage("§e/jobs debug §7- Toggle debug mode");
        sender.sendMessage("§e/jobs debug on §7- Enable debug mode");
        sender.sendMessage("§e/jobs debug off §7- Disable debug mode");
        sender.sendMessage("§e/jobs debug player <name> §7- Debug player data");
        sender.sendMessage("§e/jobs debug job <id> §7- Debug job data");
        sender.sendMessage("§e/jobs debug listeners §7- Show registered listeners");
        sender.sendMessage("§e/jobs debug threads §7- Show thread information");
        sender.sendMessage("§e/jobs debug config §7- Show configuration");
        sender.sendMessage("§e/jobs debug export §7- Export debug data");
    }
}