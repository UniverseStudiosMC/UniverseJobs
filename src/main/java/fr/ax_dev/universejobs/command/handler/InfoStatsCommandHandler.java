package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles info, list, and stats commands.
 */
public class InfoStatsCommandHandler extends JobCommandHandler {
    
    public InfoStatsCommandHandler(UniverseJobs plugin) {
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
            case "info" -> handleInfoCommand(player, args);
            case "list" -> handleListCommand(player);
            case "stats" -> handleStatsCommand(player, args);
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
                default -> {
                    // No completions for unknown commands
                }
            }
        }
        
        return completions;
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
                    MessageUtils.sendMessage(player, languageManager.getMessage("commands.info.not-found", "target", target));
                }
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
     * Show information about a specific job.
     */
    private void showJobInfo(Player player, Job job) {
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.info.job.header", "job", job.getName()));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.info.job.description", "description", job.getDescription()));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.info.job.max-level", "level", String.valueOf(job.getMaxLevel())));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.info.job.permission", "permission", job.getPermission()));
        
        if (!job.getLore().isEmpty()) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.info.job.lore-header"));
            for (String line : job.getLore()) {
                MessageUtils.sendMessage(player, languageManager.getMessage("commands.info.job.lore-line", "line", line));
            }
        }
        
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.info.job.action-types", 
                "types", job.getActionTypes().stream()
                        .map(Enum::name)
                        .collect(Collectors.joining(", "))));
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
        
        if (viewer == target) {
            MessageUtils.sendMessage(viewer, languageManager.getMessage("commands.stats.header-self"));
        } else {
            MessageUtils.sendMessage(viewer, languageManager.getMessage("commands.stats.header-other", "player", target.getName()));
        }
        
        for (String jobId : playerJobs) {
            Job job = jobManager.getJob(jobId);
            if (job != null) {
                int level = jobManager.getLevel(target, jobId);
                double[] progress = data.getXpProgress(jobId);
                
                MessageUtils.sendMessage(viewer, languageManager.getMessage("commands.stats.job-entry", 
                        "job", job.getName(), 
                        "level", String.valueOf(level), 
                        "current_xp", String.valueOf(Math.round(progress[0])), 
                        "required_xp", String.valueOf(Math.round(progress[1]))));
            }
        }
    }
}