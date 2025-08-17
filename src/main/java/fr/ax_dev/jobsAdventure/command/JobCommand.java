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
import fr.ax_dev.jobsAdventure.reward.RewardStatus;
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
    private static final Pattern SAFE_REWARD_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(languageManager.getMessage("commands.players-only"));
            return true;
        }
        
        // Rate limiting check
        if (!checkRateLimit(player)) {
            return true; // Silently ignore rapid commands
        }
        
        // Basic argument validation
        if (!validateCommandStructure(args)) {
            MessageUtils.sendMessage(player, "&cInvalid command format!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = sanitizeInput(args[0].toLowerCase());
        
        // Validate subcommand
        if (!isValidSubCommand(subCommand)) {
            MessageUtils.sendMessage(player, "&cInvalid command!");
            return true;
        }
        
        try {
            switch (subCommand) {
                case "join" -> handleJoinCommand(player, args);
                case "leave" -> handleLeaveCommand(player, args);
                case "info" -> handleInfoCommand(player, args);
                case "list" -> handleListCommand(player);
                case "stats" -> handleStatsCommand(player, args);
                case "rewards" -> handleRewardsCommand(player, args);
                case "xpbonus" -> handleXpBonusCommand(sender, args);
                case "migrate" -> handleMigrateCommand(sender, args);
                case "reload" -> handleReloadCommand(player);
                case "monitor" -> handleMonitorCommand(sender, args);
                case "debug" -> handleDebugCommand(sender, args);
                default -> sendHelp(player);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error executing command for player " + player.getName() + ": " + e.getMessage());
            MessageUtils.sendMessage(player, "&cAn error occurred while processing your command.");
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
     * Validate reward ID format.
     * 
     * @param rewardId The reward ID to validate
     * @return true if valid
     */
    private boolean isValidRewardId(String rewardId) {
        return rewardId != null && SAFE_REWARD_ID_PATTERN.matcher(rewardId).matches();
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
            MessageUtils.sendMessage(player, "&cInvalid job name format!");
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
            MessageUtils.sendMessage(player, "&cInvalid job name format!");
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
                MessageUtils.sendMessage(player, "&cInvalid target format!");
                return;
            }
            
            // Check if target is a job name or player name
            Job job = jobManager.getJob(target);
            if (job != null) {
                showJobInfo(player, job);
            } else {
                // Validate player name format before lookup
                if (!isValidPlayerName(target)) {
                    MessageUtils.sendMessage(player, "&cInvalid player name format!");
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
                MessageUtils.sendMessage(viewer, "&cYou don't have any jobs!");
            } else {
                MessageUtils.sendMessage(viewer, "&c" + target.getName() + " doesn't have any jobs!");
            }
            return;
        }
        
        String playerName = viewer == target ? "Your" : target.getName() + "'s";
        MessageUtils.sendMessage(viewer, "&6=== " + playerName + " Jobs ===");
        
        for (String jobId : playerJobs) {
            Job job = jobManager.getJob(jobId);
            if (job != null) {
                int level = jobManager.getLevel(target, jobId); // Use JobManager for accurate level calculation
                double xp = data.getXp(jobId);
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
            MessageUtils.sendMessage(player, "&cNo jobs are available!");
            return;
        }
        
        MessageUtils.sendMessage(player, "&6=== Available Jobs ===");
        for (Job job : jobs) {
            String status = jobManager.hasJob(player, job.getId()) ? "§a✓" : "§c✗";
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
                MessageUtils.sendMessage(player, "&cPlayer not found!");
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
            MessageUtils.sendMessage((Player) sender, "&cYou don't have permission to use this command!");
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
            sender.sendMessage("§cInvalid player name format!");
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
            sender.sendMessage("§cInvalid multiplier! Must be a valid number.");
            return;
        }
        
        // Strict multiplier bounds checking with security in mind
        if (multiplier <= 0.0 || multiplier > 10.0 || Double.isNaN(multiplier) || Double.isInfinite(multiplier)) {
            sender.sendMessage("§cMultiplier must be between 0.1 and 10.0!");
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
            sender.sendMessage("§cInvalid duration! Must be a valid number of seconds.");
            return;
        }
        
        // Strict duration bounds checking (1 second to 24 hours)
        if (duration <= 0 || duration > 86400) {
            sender.sendMessage("§cDuration must be between 1 second and 24 hours (86400 seconds)!");
            return;
        }
        
        // Validate job ID if provided
        String jobId = null;
        if (args.length > 4) {
            jobId = sanitizeInput(args[4]);
            if (!jobId.equals("*") && !isValidJobId(jobId)) {
                sender.sendMessage("§cInvalid job ID format!");
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
                sender.sendMessage("§cJob '" + jobId + "' does not exist!");
                return;
            }
        }
        
        String senderName = sender instanceof Player ? sender.getName() : "Console";
        
        if (target.equals("*")) {
            // Give to all online players
            int count = bonusManager.addGlobalBonus(multiplier, duration, reason, senderName);
            sender.sendMessage(String.format("§aGave %dx XP bonus to %d online players for %s", 
                    (int)((multiplier - 1) * 100), count, XpBonusManager.formatDuration(duration)));
        } else {
            // Give to specific player
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null) {
                sender.sendMessage("§cPlayer '" + target + "' is not online!");
                return;
            }
            
            if (jobId == null || jobId.equals("*")) {
                bonusManager.addPlayerBonus(targetPlayer.getUniqueId(), multiplier, duration, reason, senderName);
            } else {
                bonusManager.addJobBonus(targetPlayer.getUniqueId(), jobId, multiplier, duration, reason, senderName);
            }
            
            String jobText = (jobId == null || jobId.equals("*")) ? "all jobs" : "job " + jobId;
            sender.sendMessage(String.format("§aGave %s %d%% XP bonus for %s (%s)", 
                    targetPlayer.getName(), (int)((multiplier - 1) * 100), jobText, XpBonusManager.formatDuration(duration)));
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
            sender.sendMessage("§cInvalid player name format!");
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage("§cPlayer '" + playerName + "' is not online!");
            return;
        }
        
        // Validate job ID if provided
        String jobId = null;
        if (args.length > 2) {
            jobId = sanitizeInput(args[2]);
            if (!isValidJobId(jobId)) {
                sender.sendMessage("§cInvalid job ID format!");
                return;
            }
        }
        
        if (jobId == null) {
            // Remove all bonuses
            int removed = bonusManager.removeAllBonuses(targetPlayer.getUniqueId());
            sender.sendMessage(String.format("§aRemoved %d bonuses from %s", removed, targetPlayer.getName()));
        } else {
            // Remove job-specific bonuses
            List<XpBonus> bonuses = bonusManager.getActiveBonuses(targetPlayer.getUniqueId(), jobId);
            int removed = 0;
            for (XpBonus bonus : bonuses) {
                if (bonusManager.removeBonus(bonus)) {
                    removed++;
                }
            }
            sender.sendMessage(String.format("§aRemoved %d %s bonuses from %s", removed, jobId, targetPlayer.getName()));
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
            sender.sendMessage("§cPlayer '" + playerName + "' is not online!");
            return;
        }
        
        List<XpBonus> bonuses = bonusManager.getActiveBonuses(targetPlayer.getUniqueId());
        
        if (bonuses.isEmpty()) {
            sender.sendMessage("§e" + targetPlayer.getName() + " has no active XP bonuses.");
            return;
        }
        
        sender.sendMessage("§6=== XP Bonuses for " + targetPlayer.getName() + " ===");
        for (int i = 0; i < bonuses.size(); i++) {
            XpBonus bonus = bonuses.get(i);
            String jobText = bonus.getJobId() != null ? bonus.getJobId() : "All Jobs";
            sender.sendMessage(String.format("§7%d. §e+%d%% §7(%s) - §a%s §7- %s", 
                    i + 1,
                    bonus.getBonusPercentage(),
                    jobText,
                    bonus.getRemainingTimeFormatted(),
                    bonus.getReason()));
        }
    }
    
    /**
     * Handle the xpbonus info subcommand.
     */
    private void handleXpBonusInfo(CommandSender sender) {
        Map<String, Object> stats = bonusManager.getStats();
        
        sender.sendMessage("§6=== XP Bonus System Info ===");
        sender.sendMessage("§7Players with bonuses: §e" + stats.get("total_players"));
        sender.sendMessage("§7Total bonuses: §e" + stats.get("total_bonuses"));
        sender.sendMessage("§7Active bonuses: §e" + stats.get("active_bonuses"));
    }
    
    /**
     * Handle the xpbonus cleanup subcommand.
     */
    private void handleXpBonusCleanup(CommandSender sender) {
        bonusManager.cleanupExpiredBonuses();
        sender.sendMessage("§aForced cleanup of expired bonuses completed.");
    }
    
    /**
     * Send XP bonus help message.
     */
    private void sendXpBonusHelp(CommandSender sender) {
        sender.sendMessage("§6=== XP Bonus Commands ===");
        sender.sendMessage("§e/jobs xpbonus give <player|*> <multiplier> <duration> [job] [reason]");
        sender.sendMessage("§7  Give XP bonus to player(s). Use * for all online players.");
        sender.sendMessage("§e/jobs xpbonus remove <player> [job]");
        sender.sendMessage("§7  Remove XP bonuses from a player.");
        sender.sendMessage("§e/jobs xpbonus list [player]");
        sender.sendMessage("§7  List active bonuses for a player.");
        sender.sendMessage("§e/jobs xpbonus info");
        sender.sendMessage("§7  Show bonus system statistics.");
        sender.sendMessage("§e/jobs xpbonus cleanup");
        sender.sendMessage("§7  Force cleanup of expired bonuses.");
        sender.sendMessage("§7");
        sender.sendMessage("§7Examples:");
        sender.sendMessage("§7  /jobs xpbonus give * 2.0 3600 - 2x XP for all players, 1 hour");
        sender.sendMessage("§7  /jobs xpbonus give Steve 1.5 1800 miner Mining Event");
    }
    
    /**
     * Handle the rewards subcommand.
     */
    private void handleRewardsCommand(Player player, String[] args) {
        if (!player.hasPermission("jobsadventure.rewards.use")) {
            MessageUtils.sendMessage(player, "&cYou don't have permission to use rewards!");
            return;
        }
        
        if (args.length < 2) {
            // Show help for rewards command
            MessageUtils.sendMessage(player, "&6=== Jobs Rewards Commands ===");
            MessageUtils.sendMessage(player, "&e/jobs rewards open <job> &7- Open rewards GUI for a job");
            MessageUtils.sendMessage(player, "&e/jobs rewards list [job] &7- List available rewards");
            MessageUtils.sendMessage(player, "&e/jobs rewards claim <job> <reward> &7- Claim a specific reward");
            MessageUtils.sendMessage(player, "&e/jobs rewards info <job> <reward> &7- Show reward details");
            
            if (player.hasPermission("jobsadventure.rewards.admin")) {
                MessageUtils.sendMessage(player, "&e/jobs rewards admin ... &7- Admin commands");
                MessageUtils.sendMessage(player, "&e/jobs rewards reload &7- Reload reward configs");
            }
            return;
        }
        
        String rewardSubCommand = args[1].toLowerCase();
        
        switch (rewardSubCommand) {
            case "open" -> {
                if (args.length < 3) {
                    MessageUtils.sendMessage(player, "&cUsage: /jobs rewards open <job>");
                    return;
                }
                
                String jobId = args[2];
                Job job = jobManager.getJob(jobId);
                
                if (job == null) {
                    MessageUtils.sendMessage(player, "&cJob '{job}' not found!", "job", jobId);
                    return;
                }
                
                // Check if player has the job
                if (!jobManager.hasJob(player, jobId)) {
                    MessageUtils.sendMessage(player, "&cYou must have the {job} job to view its rewards!", "job", job.getName());
                    return;
                }
                
                rewardGuiManager.openRewardsGui(player, jobId);
            }
            
            case "list" -> {
                String jobId = args.length > 2 ? args[2] : null;
                if (jobId == null) {
                    // List all jobs that have rewards
                    MessageUtils.sendMessage(player, "&6Jobs with rewards:");
                    boolean foundJobs = false;
                    
                    for (Job job : jobManager.getAllJobs()) {
                        List<Reward> jobRewards = rewardManager.getJobRewards(job.getId());
                        if (!jobRewards.isEmpty()) {
                            MessageUtils.sendMessage(player, "&e- {job}", "job", job.getName());
                            foundJobs = true;
                        }
                    }
                    
                    if (!foundJobs) {
                        MessageUtils.sendMessage(player, "&cNo jobs have rewards configured.");
                    }
                } else {
                    // List rewards for specific job
                    Job job = jobManager.getJob(jobId);
                    if (job == null) {
                        MessageUtils.sendMessage(player, "&cJob '{job}' not found!", "job", jobId);
                        return;
                    }
                    
                    List<Reward> rewards = rewardManager.getJobRewards(jobId);
                    if (rewards.isEmpty()) {
                        MessageUtils.sendMessage(player, "&cNo rewards configured for {job}.", "job", job.getName());
                        return;
                    }
                    
                    MessageUtils.sendMessage(player, "&6Rewards for {job}:", "job", job.getName());
                    for (Reward reward : rewards) {
                        MessageUtils.sendMessage(player, "&e- " + reward.getName() + " &7(Level " + reward.getRequiredLevel() + ")");
                    }
                }
            }
            
            case "claim" -> {
                if (args.length < 4) {
                    MessageUtils.sendMessage(player, "&cUsage: /jobs rewards claim <job> <reward>");
                    return;
                }
                
                String jobId = args[2];
                String rewardId = args[3];
                
                var reward = rewardManager.getReward(jobId, rewardId);
                if (reward == null) {
                    MessageUtils.sendMessage(player, "&cReward not found!");
                    return;
                }
                
                if (rewardManager.claimReward(player, reward)) {
                    MessageUtils.sendMessage(player, "&aReward claimed successfully!");
                } else {
                    MessageUtils.sendMessage(player, "&cFailed to claim reward. Check requirements and availability.");
                }
            }
            
            case "info" -> {
                if (args.length < 4) {
                    MessageUtils.sendMessage(player, "&cUsage: /jobs rewards info <job> <reward>");
                    return;
                }
                
                String jobId = args[2];
                String rewardId = args[3];
                
                var reward = rewardManager.getReward(jobId, rewardId);
                if (reward == null) {
                    MessageUtils.sendMessage(player, "&cReward not found!");
                    return;
                }
                
                MessageUtils.sendMessage(player, "&6=== Reward Info ===");
                MessageUtils.sendMessage(player, "&eReward: &f{name}", "name", reward.getName());
                MessageUtils.sendMessage(player, "&eDescription: &f{desc}", "desc", reward.getDescription());
                MessageUtils.sendMessage(player, "&eRequired Level: &f{level}", "level", String.valueOf(reward.getRequiredLevel()));
                MessageUtils.sendMessage(player, "&eRepeatable: &f{repeat}", "repeat", reward.isRepeatable() ? "Yes" : "No");
                
                RewardStatus status = rewardManager.getRewardStatus(player, reward);
                String statusText = switch (status) {
                    case RETRIEVABLE -> "&aReady to claim";
                    case BLOCKED -> "&cLocked";
                    case RETRIEVED -> "&7Already claimed";
                    default -> "&7Unknown";
                };
                MessageUtils.sendMessage(player, "&eStatus: {status}", "status", statusText);
            }
            
            case "admin" -> {
                if (!player.hasPermission("jobsadventure.rewards.admin")) {
                    MessageUtils.sendMessage(player, "&cYou don't have permission for admin commands!");
                    return;
                }
                
                if (args.length < 3) {
                    MessageUtils.sendMessage(player, "&6=== Rewards Admin Commands ===");
                    MessageUtils.sendMessage(player, "&e/jobs rewards admin give <player> <job> <reward> &7- Give reward");
                    MessageUtils.sendMessage(player, "&e/jobs rewards admin reset <player> <job> &7- Reset job rewards");
                    return;
                }
                
                String adminSubCommand = args[2].toLowerCase();
                switch (adminSubCommand) {
                    case "give" -> {
                        if (args.length < 6) {
                            MessageUtils.sendMessage(player, "&cUsage: /jobs rewards admin give <player> <job> <reward>");
                            return;
                        }
                        
                        String targetName = args[3];
                        String jobId = args[4];
                        String rewardId = args[5];
                        
                        Player target = Bukkit.getPlayer(targetName);
                        if (target == null) {
                            MessageUtils.sendMessage(player, "&cPlayer not found!");
                            return;
                        }
                        
                        var reward = rewardManager.getReward(jobId, rewardId);
                        if (reward == null) {
                            MessageUtils.sendMessage(player, "&cReward not found!");
                            return;
                        }
                        
                        if (rewardManager.claimReward(target, reward)) {
                            MessageUtils.sendMessage(player, "&aReward given to {player}!", "player", target.getName());
                            MessageUtils.sendMessage(target, "&aYou received a reward from an admin!");
                        } else {
                            MessageUtils.sendMessage(player, "&cFailed to give reward!");
                        }
                    }
                    
                    case "reset" -> {
                        if (args.length < 5) {
                            MessageUtils.sendMessage(player, "&cUsage: /jobs rewards admin reset <player> <job>");
                            return;
                        }
                        
                        String targetName = args[3];
                        String jobId = args[4];
                        
                        Player target = Bukkit.getPlayer(targetName);
                        if (target == null) {
                            MessageUtils.sendMessage(player, "&cPlayer not found!");
                            return;
                        }
                        
                        rewardManager.resetJobRewards(target, jobId);
                        MessageUtils.sendMessage(player, "&aReset rewards for " + target.getName() + " in job " + jobId + "!");
                    }
                    
                    default -> MessageUtils.sendMessage(player, "&cUnknown admin command!");
                }
            }
            
            case "reload" -> {
                if (!player.hasPermission("jobsadventure.rewards.admin")) {
                    MessageUtils.sendMessage(player, "&cYou don't have permission to reload!");
                    return;
                }
                
                rewardManager.reloadRewards();
                MessageUtils.sendMessage(player, "&aRewards configuration reloaded!");
            }
            
            case "debug" -> {
                if (!player.hasPermission("jobsadventure.rewards.admin")) {
                    MessageUtils.sendMessage(player, "&cYou don't have permission to use debug!");
                    return;
                }
                
                if (args.length < 3) {
                    MessageUtils.sendMessage(player, "&cUsage: /jobs rewards debug <job>");
                    return;
                }
                
                String jobId = args[2];
                rewardManager.debugPlayerRewards(player, jobId);
                MessageUtils.sendMessage(player, "&aDebug info printed to console!");
            }
            
            default -> {
                MessageUtils.sendMessage(player, "&cUnknown rewards command! Use /jobs rewards for help.");
            }
        }
    }

    /**
     * Handle the migrate subcommand for converting language files.
     */
    private void handleMigrateCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("jobsadventure.admin.migrate")) {
            sender.sendMessage("§cYou don't have permission to use migration commands!");
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
        sender.sendMessage("§6Running legacy to MiniMessage converter tests...");
        
        // Capture console output by redirecting System.out temporarily
        try {
            LegacyConverterTest.runTests();
            sender.sendMessage("§aConverter tests completed! Check console for detailed results.");
        } catch (Exception e) {
            sender.sendMessage("§cError running tests: " + e.getMessage());
        }
    }
    
    /**
     * Handle migration validate command.
     */
    private void handleMigrateValidate(CommandSender sender) {
        sender.sendMessage("§6Validating language files for legacy color codes...");
        
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        LanguageFileMigrator migrator = new LanguageFileMigrator(languagesDir, plugin.getLogger());
        
        LanguageFileMigrator.ValidationResult result = migrator.validateFiles();
        
        sender.sendMessage("§6" + result.getSummary());
        
        if (!result.getIssues().isEmpty()) {
            sender.sendMessage("§eFound " + result.getIssues().size() + " files with legacy codes:");
            for (String issue : result.getIssues()) {
                sender.sendMessage("§7- " + issue);
            }
        }
        
        if (result.isValid()) {
            sender.sendMessage("§aAll language files are using MiniMessage format!");
        } else {
            sender.sendMessage("§eUse '/jobs migrate convert' to convert legacy codes to MiniMessage format.");
        }
    }
    
    /**
     * Handle migration backup command.
     */
    private void handleMigrateBackup(CommandSender sender) {
        sender.sendMessage("§6Creating backups of all language files...");
        
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        LanguageFileMigrator migrator = new LanguageFileMigrator(languagesDir, plugin.getLogger());
        
        if (migrator.createBackups()) {
            sender.sendMessage("§aSuccessfully created backups for all language files!");
        } else {
            sender.sendMessage("§cFailed to create some backups. Check console for details.");
        }
    }
    
    /**
     * Handle migration convert command.
     */
    private void handleMigrateConvert(CommandSender sender, String[] args) {
        boolean createBackup = true;
        
        // Check for --no-backup flag
        if (args.length > 2 && args[2].equals("--no-backup")) {
            createBackup = false;
            sender.sendMessage("§eSkipping backup creation as requested.");
        }
        
        sender.sendMessage("§6Converting all language files from legacy to MiniMessage format...");
        if (createBackup) {
            sender.sendMessage("§7Creating backups automatically...");
        }
        
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        LanguageFileMigrator migrator = new LanguageFileMigrator(languagesDir, plugin.getLogger());
        
        boolean success = migrator.migrateAllFiles(createBackup);
        
        if (success) {
            sender.sendMessage("§aSuccessfully converted all language files to MiniMessage format!");
            
            // Show conversion report
            List<String> report = migrator.getConversionReport();
            if (!report.isEmpty()) {
                sender.sendMessage("§6Conversion summary:");
                int conversions = 0;
                for (String line : report) {
                    if (line.startsWith("CONVERTED:")) {
                        conversions++;
                    }
                }
                sender.sendMessage("§7Total messages converted: §e" + conversions);
                
                if (conversions > 0) {
                    sender.sendMessage("§7Use '/jobs migrate validate' to verify the conversion.");
                    sender.sendMessage("§eRecommended: Reload the plugin to apply changes with '/jobs reload'");
                }
            }
        } else {
            sender.sendMessage("§cSome files failed to convert. Check console for details.");
            sender.sendMessage("§7You can use '/jobs migrate restore' to revert changes if needed.");
        }
    }
    
    /**
     * Handle migration restore command.
     */
    private void handleMigrateRestore(CommandSender sender) {
        sender.sendMessage("§6Restoring language files from backups...");
        
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        LanguageFileMigrator migrator = new LanguageFileMigrator(languagesDir, plugin.getLogger());
        
        if (migrator.restoreFromBackups()) {
            sender.sendMessage("§aSuccessfully restored all language files from backups!");
            sender.sendMessage("§eRecommended: Reload the plugin with '/jobs reload' to apply changes.");
        } else {
            sender.sendMessage("§cFailed to restore some files. Check console for details.");
        }
    }
    
    /**
     * Send migration help message.
     */
    private void sendMigrateHelp(CommandSender sender) {
        sender.sendMessage("§6=== Language File Migration Commands ===");
        sender.sendMessage("§e/jobs migrate test §7- Run converter tests");
        sender.sendMessage("§e/jobs migrate validate §7- Check files for legacy color codes");
        sender.sendMessage("§e/jobs migrate backup §7- Create backup files");
        sender.sendMessage("§e/jobs migrate convert [--no-backup] §7- Convert legacy to MiniMessage");
        sender.sendMessage("§e/jobs migrate restore §7- Restore files from backups");
        sender.sendMessage("§7");
        sender.sendMessage("§7Migration process:");
        sender.sendMessage("§71. Run §e/jobs migrate validate §7to see what needs converting");
        sender.sendMessage("§72. Run §e/jobs migrate convert §7to perform the conversion");
        sender.sendMessage("§73. Use §e/jobs reload §7to apply the changes");
        sender.sendMessage("§74. If issues occur, use §e/jobs migrate restore §7to revert");
    }
    
    /**
     * Handle the reload subcommand.
     */
    private void handleReloadCommand(Player player) {
        if (!player.hasPermission("jobsadventure.admin")) {
            MessageUtils.sendMessage(player, "&cYou don't have permission to use this command!");
            return;
        }
        
        plugin.getConfigManager().reloadConfig();
        jobManager.reloadJobs();
        rewardManager.reloadRewards();
        MessageUtils.sendMessage(player, "&aJobsAdventure configuration reloaded (including rewards and GUI configs)!");
    }
    
    /**
     * Send help message to player.
     */
    private void sendHelp(Player player) {
        MessageUtils.sendMessage(player, "&6=== JobsAdventure Commands ===");
        MessageUtils.sendMessage(player, "&e/jobs join <job> &7- Join a job");
        MessageUtils.sendMessage(player, "&e/jobs leave <job> &7- Leave a job");
        MessageUtils.sendMessage(player, "&e/jobs info [job/player] &7- Show job or player info");
        MessageUtils.sendMessage(player, "&e/jobs list &7- List available jobs");
        MessageUtils.sendMessage(player, "&e/jobs stats [player] &7- Show job statistics");
        MessageUtils.sendMessage(player, "&e/jobs rewards <subcommand> &7- Manage job rewards");
        
        if (player.hasPermission("jobsadventure.admin")) {
            MessageUtils.sendMessage(player, "&e/jobs reload &7- Reload configuration");
        }
        
        if (player.hasPermission("jobsadventure.admin.migrate")) {
            MessageUtils.sendMessage(player, "&e/jobs migrate &7- Language file migration tools");
        }
        
        if (player.hasPermission("jobsadventure.admin.xpbonus")) {
            MessageUtils.sendMessage(player, "&e/jobs xpbonus &7- Manage XP bonuses");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main subcommands
            List<String> subCommands = new ArrayList<>(Arrays.asList("join", "leave", "info", "list", "stats"));
            if (player.hasPermission("jobsadventure.rewards.use")) {
                subCommands.add("rewards");
            }
            if (player.hasPermission("jobsadventure.admin")) {
                subCommands.add("reload");
            }
            if (player.hasPermission("jobsadventure.admin.migrate")) {
                subCommands.add("migrate");
            }
            if (player.hasPermission("jobsadventure.admin.xpbonus")) {
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
                    for (Job job : jobManager.getEnabledJobs()) {
                        if (!jobManager.hasJob(player, job.getId()) && 
                            job.getId().toLowerCase().startsWith(input)) {
                            completions.add(job.getId());
                        }
                    }
                }
                case "leave" -> {
                    // Jobs that player has
                    for (String jobId : jobManager.getPlayerJobs(player)) {
                        if (jobId.toLowerCase().startsWith(input)) {
                            completions.add(jobId);
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
            MessageUtils.sendMessage(sender, "&cYou don't have permission to use monitoring commands!");
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
            MessageUtils.sendMessage(sender, "&cYou don't have permission to use debug commands!");
            return;
        }
        
        if (args.length < 2) {
            sendDebugHelp(sender);
            return;
        }
        
        String debugSubCommand = sanitizeInput(args[1].toLowerCase());
        
        switch (debugSubCommand) {
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
        sender.sendMessage("§6=== JobsAdventure System Status ===");
        
        // Plugin status
        sender.sendMessage("§ePlugin Version: §f" + plugin.getDescription().getVersion());
        sender.sendMessage("§eUptime: §f" + formatUptime(System.currentTimeMillis() - plugin.getStartTime()));
        
        // Manager status
        boolean jobManagerHealthy = plugin.getJobManager() != null;
        sender.sendMessage("§eJob Manager: " + (jobManagerHealthy ? "§aHealthy" : "§cNot Available"));
        
        // Performance manager status
        sender.sendMessage("§ePerformance Manager: §cNot Implemented Yet");
        
        // Server info
        Runtime runtime = Runtime.getRuntime();
        int maxMemoryMB = (int) (runtime.maxMemory() / (1024 * 1024));
        int usedMemoryMB = (int) ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        double memoryUsage = (double) usedMemoryMB / maxMemoryMB * 100;
        
        sender.sendMessage("§eMemory Usage: §f" + usedMemoryMB + "MB / " + maxMemoryMB + "MB (§e" + String.format("%.1f%%", memoryUsage) + "§f)");
        sender.sendMessage("§eOnline Players: §f" + plugin.getServer().getOnlinePlayers().size());
    }
    
    private void handleMonitorPerformance(CommandSender sender) {
        sender.sendMessage("§6=== Performance Metrics ===");
        sender.sendMessage("§cPerformance monitoring not implemented yet");
    }
    
    private void handleMonitorMemory(CommandSender sender) {
        sender.sendMessage("§6=== Memory Information ===");
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        sender.sendMessage("§eMax Memory: §f" + formatBytes(maxMemory));
        sender.sendMessage("§eAllocated Memory: §f" + formatBytes(totalMemory));
        sender.sendMessage("§eUsed Memory: §f" + formatBytes(usedMemory));
        sender.sendMessage("§eFree Memory: §f" + formatBytes(freeMemory));
        sender.sendMessage("§eMemory Usage: §f" + String.format("%.1f%%", (usedMemory * 100.0) / maxMemory));
        
        // JobManager memory stats if available
        if (plugin.getJobManager() != null) {
            Map<String, Object> jobManagerStats = plugin.getJobManager().getMemoryStats();
            sender.sendMessage("§6JobManager Memory:");
            sender.sendMessage("§eCached Players: §f" + jobManagerStats.get("cached_players"));
            sender.sendMessage("§eLoaded Jobs: §f" + jobManagerStats.get("loaded_jobs"));
            sender.sendMessage("§eTracked References: §f" + jobManagerStats.get("tracked_references"));
        }
        
        // Suggest GC
        sender.sendMessage("§7Use '/jobs monitor reset' to force garbage collection");
    }
    
    private void handleMonitorStorage(CommandSender sender) {
        sender.sendMessage("§6=== Storage Information ===");
        sender.sendMessage("§cStorage monitoring not implemented yet");
    }
    
    private void handleMonitorEvents(CommandSender sender) {
        sender.sendMessage("§6=== Event Monitoring ===");
        sender.sendMessage("§cEvent monitoring not implemented yet");
    }
    
    private void handleMonitorReset(CommandSender sender) {
        sender.sendMessage("§6Resetting performance metrics and forcing cleanup...");
        
        // Force cleanup
        if (plugin.getJobManager() != null) {
            plugin.getJobManager().performCleanup();
        }
        
        // Force garbage collection
        System.gc();
        
        sender.sendMessage("§aPerformance metrics reset and cleanup completed!");
    }
    
    // Debug subcommand handlers
    
    private void handleDebugPlayer(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /jobs debug player <playername>");
            return;
        }
        
        String playerName = sanitizeInput(args[2]);
        if (!isValidPlayerName(playerName)) {
            sender.sendMessage("§cInvalid player name format!");
            return;
        }
        
        Player targetPlayer = plugin.getServer().getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }
        
        sender.sendMessage("§6=== Debug Info for " + targetPlayer.getName() + " ===");
        
        PlayerJobData data = plugin.getJobManager().getPlayerData(targetPlayer);
        if (data != null) {
            Map<String, Object> snapshot = data.getDataSnapshot();
            sender.sendMessage("§ePlayer UUID: §f" + snapshot.get("playerUuid"));
            sender.sendMessage("§eJob Count: §f" + snapshot.get("jobCount"));
            sender.sendMessage("§eTotal XP: §f" + String.format("%.2f", snapshot.get("totalXp")));
            sender.sendMessage("§eLast Modified: §f" + snapshot.get("lastModified"));
            sender.sendMessage("§eLoading: §f" + snapshot.get("isLoading"));
            
            sender.sendMessage("§6Jobs:");
            for (String jobId : data.getJobs()) {
                double xp = data.getXp(jobId);
                int level = data.getLevel(jobId);
                sender.sendMessage("§7- §e" + jobId + "§7: Level §f" + level + "§7, XP §f" + String.format("%.2f", xp));
            }
        } else {
            sender.sendMessage("§cNo player data found!");
        }
    }
    
    private void handleDebugJob(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /jobs debug job <jobid>");
            return;
        }
        
        String jobId = sanitizeInput(args[2]);
        if (!isValidJobId(jobId)) {
            sender.sendMessage("§cInvalid job ID format!");
            return;
        }
        
        Job job = plugin.getJobManager().getJob(jobId);
        if (job == null) {
            sender.sendMessage("§cJob not found!");
            return;
        }
        
        sender.sendMessage("§6=== Debug Info for Job: " + job.getId() + " ===");
        sender.sendMessage("§eID: §f" + job.getId());
        sender.sendMessage("§eEnabled: §f" + job.isEnabled());
        sender.sendMessage("§ePermission: §f" + job.getPermission());
        sender.sendMessage("§eMax Level: §f" + job.getMaxLevel());
        sender.sendMessage("§eActions: §f" + job.getActions().size());
        
        for (fr.ax_dev.jobsAdventure.action.ActionType actionType : job.getActions().keySet()) {
            sender.sendMessage("§7- §e" + actionType.name() + "§7: §f" + job.getActions().get(actionType).size() + " entries");
        }
    }
    
    private void handleDebugListeners(CommandSender sender) {
        sender.sendMessage("§6=== Event Listener Debug ===");
        sender.sendMessage("§cListener debugging not implemented yet");
    }
    
    private void handleDebugThreads(CommandSender sender) {
        sender.sendMessage("§6=== Thread Information ===");
        
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        
        Thread[] threads = new Thread[rootGroup.activeCount()];
        int threadCount = rootGroup.enumerate(threads);
        
        int jobsAdventureThreads = 0;
        for (int i = 0; i < threadCount; i++) {
            if (threads[i] != null && threads[i].getName().contains("JobsAdventure")) {
                jobsAdventureThreads++;
                sender.sendMessage("§7- §e" + threads[i].getName() + "§7: §f" + threads[i].getState());
            }
        }
        
        sender.sendMessage("§eTotal Threads: §f" + threadCount);
        sender.sendMessage("§eJobsAdventure Threads: §f" + jobsAdventureThreads);
        
        // Memory per thread (rough estimate)
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryPerThread = threadCount > 0 ? usedMemory / threadCount : 0;
        sender.sendMessage("§eAvg Memory/Thread: §f" + formatBytes(memoryPerThread));
    }
    
    private void handleDebugConfig(CommandSender sender) {
        sender.sendMessage("§6=== Configuration Debug ===");
        
        if (plugin.getConfigManager() != null) {
            sender.sendMessage("§eDebug Mode: §f" + plugin.getConfigManager().isDebugEnabled());
            sender.sendMessage("§eData Folder: §f" + plugin.getDataFolder().getAbsolutePath());
            sender.sendMessage("§eConfig File Exists: §f" + new File(plugin.getDataFolder(), "config.yml").exists());
            sender.sendMessage("§eLanguages Loaded: §f" + (plugin.getLanguageManager() != null ? "Yes" : "No"));
        } else {
            sender.sendMessage("§cConfiguration manager not available");
        }
        
        sender.sendMessage("§ePlugin Enabled: §f" + plugin.isEnabled());
        sender.sendMessage("§eServer Version: §f" + plugin.getServer().getVersion());
        sender.sendMessage("§eBukkit Version: §f" + plugin.getServer().getBukkitVersion());
    }
    
    private void handleDebugExport(CommandSender sender) {
        sender.sendMessage("§6Exporting debug information...");
        
        try {
            StringBuilder debugInfo = new StringBuilder();
            debugInfo.append("=== JobsAdventure Debug Export ===\n");
            debugInfo.append("Timestamp: ").append(new java.util.Date()).append("\n");
            debugInfo.append("Plugin Version: ").append(plugin.getDescription().getVersion()).append("\n");
            debugInfo.append("Server Version: ").append(plugin.getServer().getVersion()).append("\n");
            debugInfo.append("\n");
            
            // Performance stats (placeholder)
            debugInfo.append("=== Performance Stats ===\n");
            debugInfo.append("Performance monitoring not implemented yet\n");
            debugInfo.append("\n");
            
            // Add memory info
            Runtime runtime = Runtime.getRuntime();
            debugInfo.append("=== Memory Info ===\n");
            debugInfo.append("Max Memory: ").append(formatBytes(runtime.maxMemory())).append("\n");
            debugInfo.append("Used Memory: ").append(formatBytes(runtime.totalMemory() - runtime.freeMemory())).append("\n");
            debugInfo.append("\n");
            
            // Save to file
            File debugFile = new File(plugin.getDataFolder(), "debug-export-" + System.currentTimeMillis() + ".txt");
            java.nio.file.Files.write(debugFile.toPath(), debugInfo.toString().getBytes());
            
            sender.sendMessage("§aDebug information exported to: §f" + debugFile.getName());
            
        } catch (Exception e) {
            sender.sendMessage("§cFailed to export debug information: " + e.getMessage());
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Debug export failed", e);
        }
    }
    
    // Helper methods
    
    private void sendMonitorHelp(CommandSender sender) {
        sender.sendMessage("§6=== Monitoring Commands ===");
        sender.sendMessage("§e/jobs monitor status §7- Overall system status");
        sender.sendMessage("§e/jobs monitor performance §7- Performance metrics");
        sender.sendMessage("§e/jobs monitor memory §7- Memory usage information");
        sender.sendMessage("§e/jobs monitor storage §7- Storage system status");
        sender.sendMessage("§e/jobs monitor events §7- Event processing statistics");
        sender.sendMessage("§e/jobs monitor reset §7- Reset metrics and force cleanup");
    }
    
    private void sendDebugHelp(CommandSender sender) {
        sender.sendMessage("§6=== Debug Commands ===");
        sender.sendMessage("§e/jobs debug player <name> §7- Player data debug info");
        sender.sendMessage("§e/jobs debug job <id> §7- Job configuration debug info");
        sender.sendMessage("§e/jobs debug listeners §7- Event listener debug info");
        sender.sendMessage("§e/jobs debug threads §7- Thread information");
        sender.sendMessage("§e/jobs debug config §7- Configuration debug info");
        sender.sendMessage("§e/jobs debug export §7- Export debug info to file");
    }
    
    private String formatUptime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes >= 1024 * 1024 * 1024) {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        } else if (bytes >= 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else if (bytes >= 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return bytes + " B";
        }
    }
}