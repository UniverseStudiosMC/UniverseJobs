package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.menu.MenuManager;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles menu-related commands.
 */
public class MenuCommandHandler extends JobCommandHandler {
    
    private final MenuManager menuManager;
    
    public MenuCommandHandler(UniverseJobs plugin) {
        super(plugin);
        this.menuManager = plugin.getMenuManager();
    }
    
    @Override
    public boolean handleCommand(CommandSender sender, String[] args) {
        if (!validateCommandStructure(args)) {
            MessageUtils.sendMessage((Player) sender, "&cInvalid command format.");
            return false;
        }
        
        Player player = validatePlayerSender(sender);
        if (player == null) return false;
        
        if (args.length == 0) {
            // Open main jobs menu
            openMainMenu(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            // Main menu commands - more intuitive names
            case "main":
            case "browse":
            case "list":
            case "all":
                openMainMenu(player);
                return true;
            
            // Rankings - multiple intuitive aliases
            case "rankings":
            case "leaderboard":
            case "top":
            case "rank":
                openRankingsMenu(player);
                return true;
                
            // Admin commands
            case "reload":
                if (!validatePermission(player, "universejobs.admin.menu.reload")) {
                    return false;
                }
                return reloadMenus(player);
                
            // Help command
            case "help":
            case "?":
                showMenuHelp(player);
                return true;
                
            default:
                // Try to interpret as direct job name - most intuitive approach
                if (isValidJobName(subCommand)) {
                    return openJobMenu(player, subCommand);
                }
                
                MessageUtils.sendMessage(player, "&cUnknown command. Type '&e/jobs menu help&c' for help.");
                showQuickHelp(player);
                return false;
        }
    }
    
    /**
     * Open the main jobs menu.
     */
    private void openMainMenu(Player player) {
        try {
            menuManager.openJobsMainMenu(player);
        } catch (Exception e) {
            handleMenuError(player, "jobs menu", e);
        }
    }
    
    /**
     * Open a specific job menu.
     */
    private boolean openJobMenu(Player player, String jobId) {
        Job job = validateJobAccess(player, jobId);
        if (job == null) return false;
        
        try {
            menuManager.openJobMenu(player, jobId);
            return true;
        } catch (Exception e) {
            return handleMenuError(player, "job menu", e);
        }
    }
    
    /**
     * Open the global rankings menu.
     */
    private void openRankingsMenu(Player player) {
        try {
            menuManager.openGlobalRankingsMenu(player);
        } catch (Exception e) {
            handleMenuError(player, "rankings menu", e);
        }
    }
    
    /**
     * Reload menu configurations.
     */
    private boolean reloadMenus(Player player) {
        try {
            menuManager.reloadConfigurations();
            MessageUtils.sendMessage(player, "&aMenu configurations reloaded successfully!");
            plugin.getLogger().info("Menu configurations reloaded by " + player.getName());
            return true;
        } catch (Exception e) {
            return handleMenuError(player, "reload menu configurations", e);
        }
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return Arrays.asList();
        }
        
        if (args.length == 1) {
            // Base commands + all job names for direct access
            List<String> completions = Arrays.asList(
                "main", "browse", "list", "all", // Main menu
                "rankings", "leaderboard", "top", "rank", // Rankings
                "help" // Help
            );
            
            // Add admin commands if player has permission
            if (sender.hasPermission("universejobs.admin.menu.reload")) {
                completions = Arrays.asList(
                    "main", "browse", "list", "all",
                    "rankings", "leaderboard", "top", "rank",
                    "help", "reload"
                );
            }
            
            // Add all job names for direct access
            List<String> jobNames = jobManager.getJobs().values().stream()
                .filter(Job::isEnabled)
                .filter(job -> job.getPermission() == null || sender.hasPermission(job.getPermission()))
                .map(Job::getId)
                .collect(Collectors.toList());
            
            completions.addAll(jobNames);
            
            return completions.stream()
                .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
        }
        
        return Arrays.asList();
    }
    
    /**
     * Validate job access for a player.
     */
    private Job validateJobAccess(Player player, String jobId) {
        if (!isValidJobId(jobId)) {
            MessageUtils.sendMessage(player, "&cInvalid job ID format.");
            return null;
        }
        
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            MessageUtils.sendMessage(player, "&cJob not found: " + jobId);
            return null;
        }
        
        if (!job.isEnabled()) {
            MessageUtils.sendMessage(player, "&cThis job is currently disabled.");
            return null;
        }
        
        // Check permission if job requires one
        if (job.getPermission() != null && !player.hasPermission(job.getPermission())) {
            MessageUtils.sendMessage(player, "&cYou don't have permission to view this job.");
            return null;
        }
        
        return job;
    }
    
    /**
     * Handle menu opening errors consistently.
     */
    private boolean handleMenuError(Player player, String menuType, Exception e) {
        MessageUtils.sendMessage(player, "&cFailed to open " + menuType + ".");
        plugin.getLogger().warning("Failed to open " + menuType + " for " + player.getName() + ": " + e.getMessage());
        return false;
    }
    
    /**
     * Check if a string is a valid job name.
     */
    private boolean isValidJobName(String jobName) {
        if (!isValidJobId(jobName)) {
            return false;
        }
        
        Job job = jobManager.getJob(jobName);
        return job != null && job.isEnabled();
    }
    
    /**
     * Show comprehensive menu help to the player.
     */
    private void showMenuHelp(Player player) {
        MessageUtils.sendMessage(player, "&6&l=== Jobs Menu Help ===");
        MessageUtils.sendMessage(player, "&e/jobs menu &7- Open main jobs menu");
        MessageUtils.sendMessage(player, "&e/jobs menu <jobname> &7- Open specific job menu directly");
        MessageUtils.sendMessage(player, "&e/jobs menu main &7- Open main jobs menu");
        MessageUtils.sendMessage(player, "&e/jobs menu rankings &7- View job rankings");
        MessageUtils.sendMessage(player, "");
        MessageUtils.sendMessage(player, "&6Examples:");
        MessageUtils.sendMessage(player, "&7- &e/jobs menu miner &7→ Open miner job menu");
        MessageUtils.sendMessage(player, "&7- &e/jobs menu farmer &7→ Open farmer job menu");
        MessageUtils.sendMessage(player, "&7- &e/jobs menu top &7→ View leaderboards");
        
        if (player.hasPermission("universejobs.admin.menu.reload")) {
            MessageUtils.sendMessage(player, "");
            MessageUtils.sendMessage(player, "&cAdmin:");
            MessageUtils.sendMessage(player, "&e/jobs menu reload &7- Reload menu configurations");
        }
    }
    
    /**
     * Show quick help for invalid commands.
     */
    private void showQuickHelp(Player player) {
        MessageUtils.sendMessage(player, "&6Quick Examples:");
        MessageUtils.sendMessage(player, "&e/jobs menu main &7→ Browse all jobs");
        
        // Show first 3 available jobs as examples
        List<String> jobExamples = jobManager.getJobs().values().stream()
            .filter(Job::isEnabled)
            .filter(job -> job.getPermission() == null || player.hasPermission(job.getPermission()))
            .map(Job::getId)
            .limit(3)
            .collect(Collectors.toList());
            
        for (String jobId : jobExamples) {
            MessageUtils.sendMessage(player, "&e/jobs menu " + jobId + " &7→ Open " + jobId + " menu");
        }
        
        MessageUtils.sendMessage(player, "&e/jobs menu rankings &7→ View top players");
    }
}