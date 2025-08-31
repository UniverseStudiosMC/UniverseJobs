package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionLimitManager;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.utils.MessageUtils;
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
    
    // Command constants
    private static final String CMD_RESTORE = "restore";
    private static final String CMD_STATUS = "status";
    
    public ActionLimitCommandHandler(UniverseJobs plugin) {
        super(plugin);
    }
    
    @Override
    public boolean handleCommand(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "universejobs.admin.actionlimits")) {
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sendActionLimitHelp(sender);
            return true;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case CMD_RESTORE -> handleActionLimitRestore(sender, args);
            case CMD_STATUS -> handleActionLimitStatus(sender, args);
            default -> sendActionLimitHelp(sender);
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!hasPermission(sender, "universejobs.admin.actionlimits")) {
            return completions;
        }
        
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            
            List<String> actionLimitSubCommands = Arrays.asList(CMD_RESTORE, CMD_STATUS);
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
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.restore.usage"));
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
            
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.restore.all-success", "count", String.valueOf(totalRestored)));
        } else {
            Player targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer == null) {
                MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.player-not-found", "player", playerName));
                return;
            }
            
            int restored = limitManager.restorePlayerLimit(targetPlayer, jobId, target);
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.restore.player-success", "count", String.valueOf(restored), "player", targetPlayer.getName()));
            
            // Notify the player
            MessageUtils.sendMessage(targetPlayer, languageManager.getMessage("commands.actionlimit.restore.player-notification"));
        }
    }
    
    private void handleActionLimitStatus(CommandSender sender, String[] args) {
        if (args.length < 5) {
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.status.usage"));
            return;
        }
        
        String playerName = args[2];
        Player targetPlayer = Bukkit.getPlayer(playerName);
        
        if (targetPlayer == null) {
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.player-not-found", "player", playerName));
            return;
        }
        
        String jobId = args[3];
        String target = args[4];
        
        ActionLimitManager.ActionLimitStatus status = limitManager.getPlayerLimitStatus(targetPlayer, jobId, target);
        
        if (status == null) {
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.status.no-limits", "job", jobId, "target", target));
            return;
        }
        
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.status.header"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.status.player", "player", targetPlayer.getName()));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.status.job", "job", jobId));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.status.target", "target", target));
        
        if (status.isOnCooldown()) {
            long remainingSeconds = status.getRemainingCooldownSeconds();
            long minutes = remainingSeconds / 60;
            long seconds = remainingSeconds % 60;
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.status.cooldown", "minutes", String.valueOf(minutes), "seconds", String.valueOf(seconds)));
        } else {
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.status.available"));
        }
        
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.status.actions", 
            "current", String.valueOf(status.getCurrentActionsPerformed()),
            "max", String.valueOf(status.getLimit().getMaxActionsPerPeriod()),
            "remaining", String.valueOf(status.getRemainingActions())));
        
        if (status.getLimit().isBlockExp() && status.getLimit().isBlockMoney()) {
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.status.blocking-both"));
        } else if (status.getLimit().isBlockExp()) {
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.status.blocking-xp"));
        } else if (status.getLimit().isBlockMoney()) {
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.status.blocking-money"));
        } else {
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.status.blocking-none"));
        }
    }
    
    private void sendActionLimitHelp(CommandSender sender) {
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.actionlimit.help"));
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
                case CMD_RESTORE -> {
                    // Player names + asterisk
                    completions.add("*");
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(input))
                            .collect(Collectors.toList()));
                }
                case CMD_STATUS -> {
                    // Player names only
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(input))
                            .collect(Collectors.toList()));
                }
                default -> {
                    // No completions for unknown subcommands
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