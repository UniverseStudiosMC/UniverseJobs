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
 * Handles rewards commands.
 */
public class RewardsCommandHandler extends JobCommandHandler {
    
    private static final String CMD_CLAIM = "claim";
    private static final String CMD_ADMIN = "admin";
    private static final String CMD_DEBUG = "debug";
    private static final String CMD_RESET = "reset";
    private static final String PERM_REWARDS_ADMIN = "universejobs.rewards.admin";
    
    public RewardsCommandHandler(UniverseJobs plugin) {
        super(plugin);
    }
    
    @Override
    public boolean handleCommand(CommandSender sender, String[] args) {
        Player player = validatePlayerSender(sender);
        if (player == null) {
            return true;
        }
        
        if (!hasPermission(player, "universejobs.rewards.use")) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.rewards.no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            return false;
        }
        
        String rewardSubCommand = args[1].toLowerCase();
        
        switch (rewardSubCommand) {
            case "open" -> handleOpenCommand(player, args);
            case "list" -> handleListCommand(player, args);
            case CMD_CLAIM -> handleClaimCommand(player, args);
            case "info" -> handleInfoCommand(player, args);
            case CMD_ADMIN -> handleAdminCommand(player, args);
            case "reload" -> handleReloadCommand(player);
            case CMD_DEBUG -> handleDebugCommand(player, args);
            default -> {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        Player player = getPlayerFromSender(sender);
        
        if (player == null || args.length == 0) {
            return completions;
        }
        
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            
            // Rewards subcommands
            List<String> rewardSubCommands = new ArrayList<>(Arrays.asList("open", "list", CMD_CLAIM, "info"));
            if (player.hasPermission(PERM_REWARDS_ADMIN)) {
                rewardSubCommands.addAll(Arrays.asList(CMD_ADMIN, "reload", CMD_DEBUG));
            }
            
            for (String rewardSubCommand : rewardSubCommands) {
                if (rewardSubCommand.startsWith(input)) {
                    completions.add(rewardSubCommand);
                }
            }
        } else if (args.length >= 3) {
            completions.addAll(getRewardsTabCompletions(player, args));
        }
        
        return completions;
    }
    
    private void handleOpenCommand(Player player, String[] args) {
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
    
    private void handleListCommand(Player player, String[] args) {
        String jobId = args.length > 2 ? args[2] : null;
        if (jobId == null) {
            // List all jobs that have rewards
            for (Job job : jobManager.getAllJobs()) {
                // Implementation for listing jobs with rewards
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
            // Implementation for listing specific job rewards
        }
    }
    
    private void handleClaimCommand(Player player, String[] args) {
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
    
    private void handleInfoCommand(Player player, String[] args) {
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
        // Display reward info
    }
    
    private void handleAdminCommand(Player player, String[] args) {
        if (!hasPermission(player, PERM_REWARDS_ADMIN)) {
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
            
            case CMD_RESET -> {
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
            
            default -> {
                // Unknown admin subcommand
            }
        }
    }
    
    private void handleReloadCommand(Player player) {
        if (!hasPermission(player, PERM_REWARDS_ADMIN)) {
            return;
        }
        
        rewardManager.reloadRewards();
    }
    
    private void handleDebugCommand(Player player, String[] args) {
        if (!hasPermission(player, PERM_REWARDS_ADMIN)) {
            return;
        }
        
        if (args.length < 3) {
            return;
        }
        
        String jobId = args[2];
        rewardManager.debugPlayerRewards(player, jobId);
    }
    
    /**
     * Get tab completions for Rewards commands.
     */
    private List<String> getRewardsTabCompletions(Player player, String[] args) {
        if (args.length == 3) {
            return getRewardsCompletions3Args(player, args);
        } else if (args.length == 4) {
            return getRewardsCompletions4Args(player, args);
        } else if (args.length == 5) {
            return getRewardsCompletions5Args(player, args);
        } else if (args.length == 6) {
            return getRewardsCompletions6Args(player, args);
        }
        return new ArrayList<>();
    }
    
    /**
     * Get completions for 3-argument commands.
     */
    private List<String> getRewardsCompletions3Args(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        String rewardSubCommand = args[1].toLowerCase();
        String input = args[2].toLowerCase();
        
        switch (rewardSubCommand) {
            case "open", CMD_CLAIM -> addPlayerJobCompletions(player, input, completions);
            case "list", "info" -> addAllJobCompletions(input, completions);
            case CMD_ADMIN -> addAdminSubCommandCompletions(player, input, completions);
            case CMD_DEBUG -> addDebugJobCompletions(player, input, completions);
            default -> {
                // Unknown subcommand - no completions
            }
        }
        return completions;
    }
    
    /**
     * Get completions for 4-argument commands.
     */
    private List<String> getRewardsCompletions4Args(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        String rewardSubCommand = args[1].toLowerCase();
        String jobId = args[2];
        String input = args[3].toLowerCase();
        
        switch (rewardSubCommand) {
            case CMD_CLAIM, "info" -> addRewardCompletions(jobId, input, completions);
            case CMD_ADMIN -> {
                if (player.hasPermission(PERM_REWARDS_ADMIN)) {
                    return getPlayerNameCompletions(input);
                }
            }
            default -> {
                // Unknown subcommand - no completions
            }
        }
        return completions;
    }
    
    /**
     * Get completions for 5-argument commands.
     */
    private List<String> getRewardsCompletions5Args(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!args[1].equalsIgnoreCase(CMD_ADMIN) || !player.hasPermission(PERM_REWARDS_ADMIN)) {
            return completions;
        }
        
        String adminSubCommand = args[2].toLowerCase();
        String input = args[4].toLowerCase();
        
        if ("give".equals(adminSubCommand) || CMD_RESET.equals(adminSubCommand)) {
            addAllJobCompletions(input, completions);
        }
        return completions;
    }
    
    /**
     * Get completions for 6-argument commands.
     */
    private List<String> getRewardsCompletions6Args(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!args[1].equalsIgnoreCase(CMD_ADMIN) || 
            !args[2].equalsIgnoreCase("give") || 
            !player.hasPermission(PERM_REWARDS_ADMIN)) {
            return completions;
        }
        
        String jobId = args[4];
        String input = args[5].toLowerCase();
        addRewardCompletions(jobId, input, completions);
        return completions;
    }
    
    /**
     * Add job completions for jobs the player has.
     */
    private void addPlayerJobCompletions(Player player, String input, List<String> completions) {
        for (Job job : jobManager.getAllJobs()) {
            if (jobManager.hasJob(player, job.getId()) && 
                job.getId().toLowerCase().startsWith(input)) {
                completions.add(job.getId());
            }
        }
    }
    
    /**
     * Add all job completions.
     */
    private void addAllJobCompletions(String input, List<String> completions) {
        for (Job job : jobManager.getAllJobs()) {
            if (job.getId().toLowerCase().startsWith(input)) {
                completions.add(job.getId());
            }
        }
    }
    
    /**
     * Add admin subcommand completions.
     */
    private void addAdminSubCommandCompletions(Player player, String input, List<String> completions) {
        if (player.hasPermission(PERM_REWARDS_ADMIN)) {
            List<String> adminSubCommands = Arrays.asList("give", CMD_RESET);
            for (String adminSubCommand : adminSubCommands) {
                if (adminSubCommand.startsWith(input)) {
                    completions.add(adminSubCommand);
                }
            }
        }
    }
    
    /**
     * Add debug job completions.
     */
    private void addDebugJobCompletions(Player player, String input, List<String> completions) {
        if (player.hasPermission(PERM_REWARDS_ADMIN)) {
            addAllJobCompletions(input, completions);
        }
    }
    
    /**
     * Add reward completions for a specific job.
     */
    private void addRewardCompletions(String jobId, String input, List<String> completions) {
        List<Reward> rewards = rewardManager.getJobRewards(jobId);
        for (Reward reward : rewards) {
            if (reward.getId().toLowerCase().startsWith(input)) {
                completions.add(reward.getId());
            }
        }
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
}