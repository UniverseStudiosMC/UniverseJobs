package fr.ax_dev.jobsAdventure.command.handler;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.job.Job;
import fr.ax_dev.jobsAdventure.reward.Reward;
import fr.ax_dev.jobsAdventure.utils.MessageUtils;
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
    
    public RewardsCommandHandler(JobsAdventure plugin) {
        super(plugin);
    }
    
    @Override
    public boolean handleCommand(CommandSender sender, String[] args) {
        Player player = getPlayerFromSender(sender);
        
        if (player == null) {
            sender.sendMessage(languageManager.getMessage("commands.players-only"));
            return true;
        }
        
        if (!hasPermission(player, "jobsadventure.rewards.use")) {
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
            case "claim" -> handleClaimCommand(player, args);
            case "info" -> handleInfoCommand(player, args);
            case "admin" -> handleAdminCommand(player, args);
            case "reload" -> handleReloadCommand(player);
            case "debug" -> handleDebugCommand(player, args);
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
            List<String> rewardSubCommands = new ArrayList<>(Arrays.asList("open", "list", "claim", "info"));
            if (player.hasPermission("jobsadventure.rewards.admin")) {
                rewardSubCommands.addAll(Arrays.asList("admin", "reload", "debug"));
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
        if (!hasPermission(player, "jobsadventure.rewards.admin")) {
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
    
    private void handleReloadCommand(Player player) {
        if (!hasPermission(player, "jobsadventure.rewards.admin")) {
            return;
        }
        
        rewardManager.reloadRewards();
    }
    
    private void handleDebugCommand(Player player, String[] args) {
        if (!hasPermission(player, "jobsadventure.rewards.admin")) {
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
}