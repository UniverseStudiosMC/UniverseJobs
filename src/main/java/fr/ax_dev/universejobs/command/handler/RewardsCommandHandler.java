package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.reward.Reward;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles rewards commands with proper API usage and centralized approach.
 * Follows CLAUDE.md rules: no hardcoded messages, proper error handling, centralized code.
 */
public class RewardsCommandHandler extends JobCommandHandler {
    
    private static final String CMD_CLAIM = "claim";
    private static final String CMD_ADMIN = "admin";
    private static final String CMD_DEBUG = "debug";
    private static final String CMD_RESET = "reset";
    private static final String PERM_REWARDS_ADMIN = "universejobs.rewards.admin";
    private static final String PERM_REWARDS_USE = "universejobs.rewards.use";
    
    public RewardsCommandHandler(UniverseJobs plugin) {
        super(plugin);
    }
    
    @Override
    public boolean handleCommand(CommandSender sender, String[] args) {
        Player player = validatePlayerSender(sender);
        if (player == null) {
            return true;
        }
        
        if (!hasPermission(player, PERM_REWARDS_USE)) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sendRewardsHelp(player);
            return true;
        }
        
        String rewardSubCommand = args[1].toLowerCase();
        
        return switch (rewardSubCommand) {
            case "open" -> {
                handleOpenCommand(player, args);
                yield true;
            }
            case CMD_CLAIM -> {
                handleClaimCommand(player, args);
                yield true;
            }
            case CMD_ADMIN -> {
                handleAdminCommand(player, args);
                yield true;
            }
            default -> {
                sendRewardsHelp(player);
                yield true;
            }
        };
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        Player player = getPlayerFromSender(sender);
        
        if (player == null || args.length == 0) {
            return completions;
        }
        
        if (args.length == 2) {
            return getRewardsSubCommands(player, args[1].toLowerCase());
        } else if (args.length >= 3) {
            return getRewardsTabCompletions(player, args);
        }
        
        return completions;
    }
    
    /**
     * Get rewards subcommands based on player permissions.
     */
    private List<String> getRewardsSubCommands(Player player, String input) {
        List<String> rewardSubCommands = new ArrayList<>(Arrays.asList("browse", "menu", "open", "list", CMD_CLAIM, "info"));
        
        if (player.hasPermission(PERM_REWARDS_ADMIN)) {
            rewardSubCommands.addAll(Arrays.asList(CMD_ADMIN, "reload", CMD_DEBUG));
        }
        
        return rewardSubCommands.stream()
            .filter(cmd -> cmd.startsWith(input))
            .collect(Collectors.toList());
    }
    
    /**
     * Handle open command with proper validation and error messages.
     */
    private void handleOpenCommand(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.open.usage"));
            return;
        }
        
        String jobId = args[2];
        Job job = validateAndGetJob(player, jobId);
        if (job == null) {
            return;
        }
        
        // Check if player has the job
        if (!jobManager.hasJob(player, jobId)) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.must-have-job", "job", job.getName()));
            return;
        }
        
        rewardGuiManager.openRewardsGui(player, jobId);
    }
    /**
     * Handle claim command with proper validation.
     */
    private void handleClaimCommand(Player player, String[] args) {
        if (args.length < 4) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.claim.usage"));
            return;
        }
        
        String jobId = args[2];
        String rewardId = args[3];
        
        Reward reward = validateAndGetReward(player, jobId, rewardId);
        if (reward == null) {
            return;
        }
        
        if (rewardManager.claimReward(player, reward)) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.claim.success", 
                "reward", reward.getName()));
        } else {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.claim.failed", 
                "reward", reward.getName()));
        }
    }
    /**
     * Handle admin command with proper permission checking.
     */
    private void handleAdminCommand(Player player, String[] args) {
        if (!hasPermission(player, PERM_REWARDS_ADMIN)) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.admin.no-permission"));
            return;
        }
        
        if (args.length < 3) {
            sendAdminHelp(player);
            return;
        }
        
        String adminSubCommand = args[2].toLowerCase();
        switch (adminSubCommand) {
            case "give" -> handleAdminGive(player, args);
            case CMD_RESET -> handleAdminReset(player, args);
            default -> sendAdminHelp(player);
        }
    }
    
    /**
     * Handle admin give command.
     */
    private void handleAdminGive(Player player, String[] args) {
        if (args.length < 6) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.admin.give.usage"));
            return;
        }
        
        String targetName = args[3];
        String jobId = args[4];
        String rewardId = args[5];
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.player-not-found", "player", targetName));
            return;
        }
        
        Reward reward = validateAndGetReward(player, jobId, rewardId);
        if (reward == null) {
            return;
        }
        
        if (rewardManager.claimReward(target, reward)) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.admin.give.success", 
                "reward", reward.getName(), "player", target.getName()));
            MessageUtils.sendMessage(target, languageManager.getMessage("commands.rewards.admin.give.notify", 
                "reward", reward.getName(), "sender", player.getName()));
        } else {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.admin.give.failed", 
                "reward", reward.getName(), "player", target.getName()));
        }
    }
    
    /**
     * Handle admin reset command.
     */
    private void handleAdminReset(Player player, String[] args) {
        if (args.length < 5) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.admin.reset.usage"));
            return;
        }
        
        String targetName = args[3];
        String jobId = args[4];
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.player-not-found", "player", targetName));
            return;
        }
        
        Job job = validateAndGetJob(player, jobId);
        if (job == null) {
            return;
        }
        
        rewardManager.resetJobRewards(target, jobId);
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.admin.reset.success", 
            "job", job.getName(), "player", target.getName()));
    }
    
    /**
     * Validate and get job with proper error messaging.
     */
    private Job validateAndGetJob(Player player, String jobId) {
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.job-not-found", "job", jobId));
            return null;
        }
        return job;
    }
    
    /**
     * Validate and get reward with proper error messaging.
     */
    private Reward validateAndGetReward(Player player, String jobId, String rewardId) {
        Job job = validateAndGetJob(player, jobId);
        if (job == null) {
            return null;
        }
        
        Reward reward = rewardManager.getReward(jobId, rewardId);
        if (reward == null) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.reward-not-found", 
                "reward", rewardId, "job", job.getName()));
            return null;
        }
        return reward;
    }
    
    /**
     * Get tab completions for Rewards commands using centralized approach.
     */
    private List<String> getRewardsTabCompletions(Player player, String[] args) {
        return switch (args.length) {
            case 3 -> getRewardsCompletions3Args(player, args);
            case 4 -> getRewardsCompletions4Args(player, args);
            case 5 -> getRewardsCompletions5Args(player, args);
            case 6 -> getRewardsCompletions6Args(player, args);
            default -> new ArrayList<>();
        };
    }
    
    /**
     * Get completions for 3-argument commands using centralized approach.
     */
    private List<String> getRewardsCompletions3Args(Player player, String[] args) {
        String rewardSubCommand = args[1].toLowerCase();
        String input = args[2].toLowerCase();
        
        return switch (rewardSubCommand) {
            case "open", CMD_CLAIM -> getPlayerJobCompletions(player, input);
            case "list", "info" -> getAllJobCompletions(input);
            case CMD_ADMIN -> getAdminSubCommandCompletions(player, input);
            case CMD_DEBUG -> hasPermission(player, PERM_REWARDS_ADMIN) ? getAllJobCompletions(input) : new ArrayList<>();
            default -> new ArrayList<>();
        };
    }
    
    /**
     * Get completions for 4-argument commands using centralized approach.
     */
    private List<String> getRewardsCompletions4Args(Player player, String[] args) {
        String rewardSubCommand = args[1].toLowerCase();
        String jobId = args[2];
        String input = args[3].toLowerCase();
        
        return switch (rewardSubCommand) {
            case CMD_CLAIM, "info" -> getRewardCompletions(jobId, input);
            case CMD_ADMIN -> hasPermission(player, PERM_REWARDS_ADMIN) ? getPlayerNameCompletions(input) : new ArrayList<>();
            default -> new ArrayList<>();
        };
    }
    
    /**
     * Get completions for 5-argument commands using centralized approach.
     */
    private List<String> getRewardsCompletions5Args(Player player, String[] args) {
        if (!args[1].equalsIgnoreCase(CMD_ADMIN) || !hasPermission(player, PERM_REWARDS_ADMIN)) {
            return new ArrayList<>();
        }
        
        String adminSubCommand = args[2].toLowerCase();
        String input = args[4].toLowerCase();
        
        if ("give".equals(adminSubCommand) || CMD_RESET.equals(adminSubCommand)) {
            return getAllJobCompletions(input);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Get completions for 6-argument commands using centralized approach.
     */
    private List<String> getRewardsCompletions6Args(Player player, String[] args) {
        if (!args[1].equalsIgnoreCase(CMD_ADMIN) || 
            !args[2].equalsIgnoreCase("give") || 
            !hasPermission(player, PERM_REWARDS_ADMIN)) {
            return new ArrayList<>();
        }
        
        String jobId = args[4];
        String input = args[5].toLowerCase();
        return getRewardCompletions(jobId, input);
    }
    
    /**
     * Get job completions for jobs the player has.
     */
    private List<String> getPlayerJobCompletions(Player player, String input) {
        return jobManager.getAllJobs().stream()
            .filter(job -> jobManager.hasJob(player, job.getId()))
            .map(Job::getId)
            .filter(id -> id.toLowerCase().startsWith(input))
            .collect(Collectors.toList());
    }
    
    /**
     * Get all job completions.
     */
    private List<String> getAllJobCompletions(String input) {
        return jobManager.getAllJobs().stream()
            .map(Job::getId)
            .filter(id -> id.toLowerCase().startsWith(input))
            .collect(Collectors.toList());
    }
    
    /**
     * Get admin subcommand completions.
     */
    private List<String> getAdminSubCommandCompletions(Player player, String input) {
        if (!hasPermission(player, PERM_REWARDS_ADMIN)) {
            return new ArrayList<>();
        }
        
        return Arrays.asList("give", CMD_RESET).stream()
            .filter(cmd -> cmd.startsWith(input))
            .collect(Collectors.toList());
    }
    
    /**
     * Get reward completions for a specific job.
     */
    private List<String> getRewardCompletions(String jobId, String input) {
        return rewardManager.getJobRewards(jobId).stream()
            .map(Reward::getId)
            .filter(id -> id.toLowerCase().startsWith(input))
            .collect(Collectors.toList());
    }
    
    /**
     * Get player name completions.
     */
    private List<String> getPlayerNameCompletions(String input) {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(input))
            .collect(Collectors.toList());
    }
    
    /**
     * Send rewards help message using language system.
     */
    private void sendRewardsHelp(Player player) {
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.help.header"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.help.browse"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.help.menu"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.help.list"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.help.claim"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.help.info"));
        
        if (hasPermission(player, PERM_REWARDS_ADMIN)) {
            sendAdminHelp(player);
        }
    }
    
    /**
     * Send admin help message using language system.
     */
    private void sendAdminHelp(Player player) {
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.admin.help.header"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.admin.help.give"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.admin.help.reset"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.admin.help.reload"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.admin.help.debug"));
    }
}