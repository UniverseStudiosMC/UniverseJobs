package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handles job join and leave commands.
 */
public class JoinLeaveCommandHandler extends JobCommandHandler {
    
    public JoinLeaveCommandHandler(UniverseJobs plugin) {
        super(plugin);
    }
    
    @Override
    public boolean handleCommand(CommandSender sender, String[] args) {
        Player player = validatePlayerSender(sender);
        if (player == null) {
            return true;
        }
        
        if (args.length == 0) {
            return false;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "join" -> handleJoinCommand(player, args);
            case "leave" -> handleLeaveCommand(player, args);
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
        
        String subCommand = args[0].toLowerCase();
        
        if (args.length == 2) {
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
                default -> {
                    // No completions for unknown commands
                }
            }
        }
        
        return completions;
    }
    
    /**
     * Handle the join subcommand.
     */
    private void handleJoinCommand(Player player, String[] args) {
        if (args.length < 2) {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                languageManager.getMessage("commands.join.usage"));
            return;
        }

        String jobId = sanitizeInput(args[1]);

        if (!isValidJobId(jobId)) {
            return;
        }

        Job job = jobManager.getJob(jobId);

        if (job == null) {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                languageManager.getMessage("commands.join.job-not-found", "job", jobId));
            return;
        }

        if (jobManager.hasJob(player, jobId)) {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                languageManager.getMessage("commands.join.already-have", "job", job.getName()));
            return;
        }

        Set<String> playerJobs = jobManager.getPlayerJobs(player);
        int maxJobs = getMaxJobsForPlayer(player);
        if (playerJobs.size() >= maxJobs) {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                languageManager.getMessage("commands.join.max-jobs-reached", "max", String.valueOf(maxJobs)));
            return;
        }

        if (!player.hasPermission(job.getPermission())) {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                languageManager.getMessage("commands.join.no-permission", "job", job.getName()));
            return;
        }
        
        // Join the job
        if (jobManager.joinJob(player, jobId)) {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                languageManager.getMessage("commands.join.success", "job", job.getName()));

            // Refresh any open menu to update button states
            plugin.getMenuManager().refreshPlayerMenu(player);
        } else {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                languageManager.getMessage("commands.join.failed", "job", job.getName()));
        }
    }
    
    /**
     * Handle the leave subcommand.
     */
    private void handleLeaveCommand(Player player, String[] args) {
        if (args.length < 2) {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                languageManager.getMessage("commands.leave.usage"));
            return;
        }

        String jobId = sanitizeInput(args[1]);

        if (!isValidJobId(jobId)) {
            return;
        }

        Job job = jobManager.getJob(jobId);

        if (job == null) {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                languageManager.getMessage("commands.leave.job-not-found", "job", jobId));
            return;
        }

        if (!jobManager.hasJob(player, jobId)) {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                languageManager.getMessage("commands.leave.dont-have", "job", job.getName()));
            return;
        }

        if (plugin.getConfigManager().isDefaultJob(jobId)) {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                languageManager.getMessage("commands.leave.default-job", "job", job.getName()));
            return;
        }
        
        // Leave the job
        if (jobManager.leaveJob(player, jobId)) {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                languageManager.getMessage("commands.leave.success", "job", job.getName()));

            // Refresh any open menu to update button states
            plugin.getMenuManager().refreshPlayerMenu(player);
        } else {
            fr.ax_dev.universejobs.utils.MessageUtils.sendMessage(player,
                languageManager.getMessage("commands.leave.failed", "job", job.getName()));
        }
    }
    
    /**
     * Get the maximum number of jobs a player can have based on their permissions.
     * Checks for universejobs.max_join.X permissions dynamically, where X can be any number.
     * The highest number found will be used.
     * 
     * @param player The player to check
     * @return The maximum number of jobs the player can join
     */
    private int getMaxJobsForPlayer(Player player) {
        int maxJobs = plugin.getConfigManager().getMaxJobsPerPlayer();

        for (org.bukkit.permissions.PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
            String permission = permInfo.getPermission();

            if (permInfo.getValue() && (permission.startsWith("universejobs.max_join.") || permission.startsWith("universejobs.maxjobs."))) {
                try {
                    String numberPart;
                    if (permission.startsWith("universejobs.max_join.")) {
                        numberPart = permission.substring("universejobs.max_join.".length());
                    } else {
                        numberPart = permission.substring("universejobs.maxjobs.".length());
                    }

                    int permissionValue = Integer.parseInt(numberPart);

                    if (permissionValue > maxJobs) {
                        maxJobs = permissionValue;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        return maxJobs;
    }
}