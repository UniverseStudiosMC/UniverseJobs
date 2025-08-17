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
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
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
            default -> sendHelp(player);
        }
        
        return true;
    }
    
    /**
     * Handle the join subcommand.
     */
    private void handleJoinCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(languageManager.getMessage("commands.join.usage"));
            return;
        }
        
        String jobId = args[1];
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
        
        String jobId = args[1];
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
            String target = args[1];
            
            // Check if target is a job name or player name
            Job job = jobManager.getJob(target);
            if (job != null) {
                showJobInfo(player, job);
            } else {
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
        
        String target = args[1];
        double multiplier;
        long duration;
        
        try {
            multiplier = Double.parseDouble(args[2]);
            duration = Long.parseLong(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid multiplier or duration! Use numbers only.");
            return;
        }
        
        if (multiplier <= 0 || multiplier > 10) {
            sender.sendMessage("§cMultiplier must be between 0.1 and 10.0!");
            return;
        }
        
        if (duration <= 0 || duration > 86400) {
            sender.sendMessage("§cDuration must be between 1 second and 24 hours (86400 seconds)!");
            return;
        }
        
        String jobId = args.length > 4 ? args[4] : null;
        String reason = args.length > 5 ? String.join(" ", Arrays.copyOfRange(args, 5, args.length)) : "Admin bonus";
        
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
        
        String playerName = args[1];
        Player targetPlayer = Bukkit.getPlayer(playerName);
        
        if (targetPlayer == null) {
            sender.sendMessage("§cPlayer '" + playerName + "' is not online!");
            return;
        }
        
        String jobId = args.length > 2 ? args[2] : null;
        
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
}