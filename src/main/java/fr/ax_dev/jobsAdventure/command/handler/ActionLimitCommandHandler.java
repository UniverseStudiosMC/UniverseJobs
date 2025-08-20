package fr.ax_dev.jobsAdventure.command.handler;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.action.ActionLimitManager;
import fr.ax_dev.jobsAdventure.job.Job;
import fr.ax_dev.jobsAdventure.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles action limit commands.
 */
public class ActionLimitCommandHandler extends JobCommandHandler {
    
    public ActionLimitCommandHandler(JobsAdventure plugin) {
        super(plugin);
    }
    
    @Override
    public boolean handleCommand(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "jobsadventure.admin.actionlimits")) {
            MessageUtils.sendMessage(sender, "&cYou don't have permission to use this command.");
            return true;
        }
        
        if (args.length < 2) {
            sendActionLimitHelp(sender);
            return true;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "restore" -> handleActionLimitRestore(sender, args);
            case "status" -> handleActionLimitStatus(sender, args);
            default -> sendActionLimitHelp(sender);
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!hasPermission(sender, "jobsadventure.admin.actionlimits")) {
            return completions;
        }
        
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            
            List<String> actionLimitSubCommands = Arrays.asList("restore", "status");
            for (String actionLimitSubCommand : actionLimitSubCommands) {
                if (actionLimitSubCommand.startsWith(input)) {
                    completions.add(actionLimitSubCommand);
                }
            }
        } else if (args.length >= 3) {
            completions.addAll(getActionLimitTabCompletions(args));
        }
        
        return completions;
    }
    
    private void handleActionLimitRestore(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.sendMessage(sender, "&cUsage: /jobs actionlimit restore <player|*> [job] [target]");
            return;
        }
        
        String playerName = args[2];
        String jobId = args.length > 3 ? args[3] : "*";
        String target = args.length > 4 ? args[4] : "*";
        
        int totalRestored = 0;
        
        if ("*".equals(playerName)) {
            // Restore for all online players
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                int restored = limitManager.restorePlayerLimit(onlinePlayer, jobId, target);
                totalRestored += restored;
            }
            
            MessageUtils.sendMessage(sender, "&aRestored &e" + totalRestored + "&a action limits for all online players.");
        } else {
            Player targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer == null) {
                MessageUtils.sendMessage(sender, "&cPlayer &e" + playerName + "&c not found or not online.");
                return;
            }
            
            int restored = limitManager.restorePlayerLimit(targetPlayer, jobId, target);
            MessageUtils.sendMessage(sender, "&aRestored &e" + restored + "&a action limits for player &e" + targetPlayer.getName() + "&a.");
            
            // Notify the player
            MessageUtils.sendMessage(targetPlayer, "&aYour action limits have been restored by an administrator.");
        }
    }
    
    private void handleActionLimitStatus(CommandSender sender, String[] args) {
        if (args.length < 5) {
            MessageUtils.sendMessage(sender, "&cUsage: /jobs actionlimit status <player> <job> <target>");
            return;
        }
        
        String playerName = args[2];
        Player targetPlayer = Bukkit.getPlayer(playerName);
        
        if (targetPlayer == null) {
            MessageUtils.sendMessage(sender, "&cPlayer &e" + playerName + "&c not found or not online.");
            return;
        }
        
        String jobId = args[3];
        String target = args[4];
        
        ActionLimitManager.ActionLimitStatus status = limitManager.getPlayerLimitStatus(targetPlayer, jobId, target);
        
        if (status == null) {
            MessageUtils.sendMessage(sender, "&cNo limits configured for job &e" + jobId + "&c and target &e" + target + "&c.");
            return;
        }
        
        MessageUtils.sendMessage(sender, "&6=== Action Limit Status ===");
        MessageUtils.sendMessage(sender, "&ePlayer: &f" + targetPlayer.getName());
        MessageUtils.sendMessage(sender, "&eJob: &f" + jobId);
        MessageUtils.sendMessage(sender, "&eTarget: &f" + target);
        
        if (status.isOnCooldown()) {
            long remainingSeconds = status.getRemainingCooldownSeconds();
            long minutes = remainingSeconds / 60;
            long seconds = remainingSeconds % 60;
            MessageUtils.sendMessage(sender, "&cStatus: &4On Cooldown &c(Remaining: " + minutes + "m " + seconds + "s)");
        } else {
            MessageUtils.sendMessage(sender, "&aStatus: &2Available");
        }
        
        MessageUtils.sendMessage(sender, "&eActions: &f" + status.getCurrentActionsPerformed() + "&7/&f" + 
                status.getLimit().getMaxActionsPerPeriod() + " &7(Remaining: &f" + status.getRemainingActions() + "&7)");
        
        String blockingStatus = "";
        if (status.getLimit().isBlockExp() && status.getLimit().isBlockMoney()) {
            blockingStatus = "&cBlocking: XP & Money";
        } else if (status.getLimit().isBlockExp()) {
            blockingStatus = "&cBlocking: XP only";
        } else if (status.getLimit().isBlockMoney()) {
            blockingStatus = "&cBlocking: Money only";
        } else {
            blockingStatus = "&aBlocking: None";
        }
        MessageUtils.sendMessage(sender, blockingStatus);
    }
    
    private void sendActionLimitHelp(CommandSender sender) {
        MessageUtils.sendMessage(sender, "&6=== Action Limit Commands ===");
        MessageUtils.sendMessage(sender, "&e/jobs actionlimit restore <player|*> [job] [target] &7- Restore action limits");
        MessageUtils.sendMessage(sender, "&e/jobs actionlimit status <player> <job> <target> &7- Check limit status");
        MessageUtils.sendMessage(sender, "&7Use '*' for player to restore all online players");
        MessageUtils.sendMessage(sender, "&7Use '*' for job/target to restore all jobs/targets");
    }
    
    /**
     * Get tab completions for Action Limit commands.
     */
    private List<String> getActionLimitTabCompletions(String[] args) {
        List<String> completions = new ArrayList<>();
        String actionLimitSubCommand = args[1].toLowerCase();
        
        if (args.length == 3) {
            String input = args[2].toLowerCase();
            
            switch (actionLimitSubCommand) {
                case "restore" -> {
                    // Player names + asterisk
                    completions.add("*");
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(input))
                            .collect(Collectors.toList()));
                }
                case "status" -> {
                    // Player names only
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(input))
                            .collect(Collectors.toList()));
                }
            }
        } else if (args.length == 4) {
            String input = args[3].toLowerCase();
            
            if (actionLimitSubCommand.equals("restore")) {
                // Job IDs + asterisk
                completions.add("*");
                completions.addAll(jobManager.getAllJobs().stream()
                        .map(Job::getId)
                        .filter(jobId -> jobId.toLowerCase().startsWith(input))
                        .collect(Collectors.toList()));
            } else if (actionLimitSubCommand.equals("status")) {
                // Job IDs only
                completions.addAll(jobManager.getAllJobs().stream()
                        .map(Job::getId)
                        .filter(jobId -> jobId.toLowerCase().startsWith(input))
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 5) {
            String input = args[4].toLowerCase();
            
            // Target completions
            if (actionLimitSubCommand.equals("restore")) {
                // Common targets + asterisk
                completions.add("*");
                completions.addAll(Arrays.asList("STONE", "DIAMOND_ORE", "COAL_ORE", "IRON_ORE", "WHEAT", "ZOMBIE", "CREEPER").stream()
                        .filter(target -> target.toLowerCase().startsWith(input.toUpperCase()))
                        .collect(Collectors.toList()));
            } else if (actionLimitSubCommand.equals("status")) {
                // Common targets only
                completions.addAll(Arrays.asList("STONE", "DIAMOND_ORE", "COAL_ORE", "IRON_ORE", "WHEAT", "ZOMBIE", "CREEPER").stream()
                        .filter(target -> target.toLowerCase().startsWith(input.toUpperCase()))
                        .collect(Collectors.toList()));
            }
        }
        
        return completions;
    }
}