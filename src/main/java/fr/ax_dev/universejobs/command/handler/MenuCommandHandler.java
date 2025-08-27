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
            case "main":
            case "browse":
                openMainMenu(player);
                return true;
                
            case "job":
                if (args.length < 2) {
                    MessageUtils.sendMessage(player, "&cUsage: /jobs menu job <jobId>");
                    return false;
                }
                return openJobMenu(player, args[1]);
                
            case "actions":
            case "rewards":
                if (args.length < 2) {
                    MessageUtils.sendMessage(player, "&cUsage: /jobs menu actions <jobId>");
                    return false;
                }
                return openActionsMenu(player, args[1]);
                
            case "rankings":
            case "leaderboard":
                openRankingsMenu(player);
                return true;
                
            case "reload":
                if (!validatePermission(player, "universejobs.admin.menu.reload")) {
                    return false;
                }
                return reloadMenus(player);
                
            default:
                MessageUtils.sendMessage(player, "&cUnknown menu command. Available: main, job, actions, rankings");
                return false;
        }
    }
    
    /**
     * Open the main jobs menu.
     */
    private void openMainMenu(Player player) {
        try {
            menuManager.openJobsMainMenu(player);
            MessageUtils.sendMessage(player, "&aOpening jobs menu...");
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
            MessageUtils.sendMessage(player, "&aOpening " + job.getName() + " menu...");
            return true;
        } catch (Exception e) {
            return handleMenuError(player, "job menu", e);
        }
    }
    
    /**
     * Open a job's actions menu (redirect to existing rewards GUI).
     */
    private boolean openActionsMenu(Player player, String jobId) {
        Job job = validateJobAccess(player, jobId);
        if (job == null) return false;
        
        // Use the existing reward GUI system instead of duplicating
        try {
            rewardGuiManager.openRewardsGui(player, jobId);
            MessageUtils.sendMessage(player, "&aOpening " + job.getName() + " rewards...");
            return true;
        } catch (Exception e) {
            return handleMenuError(player, "rewards menu", e);
        }
    }
    
    /**
     * Open the global rankings menu.
     */
    private void openRankingsMenu(Player player) {
        try {
            menuManager.openGlobalRankingsMenu(player);
            MessageUtils.sendMessage(player, "&aOpening global rankings...");
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
            // Main subcommands
            List<String> completions = Arrays.asList("main", "browse", "job", "actions", "rewards", "rankings", "leaderboard");
            
            // Add admin commands if player has permission
            if (sender.hasPermission("universejobs.admin.menu.reload")) {
                completions = Arrays.asList("main", "browse", "job", "actions", "rewards", "rankings", "leaderboard", "reload");
            }
            
            return completions.stream()
                .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
                
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("job") || subCommand.equals("actions") || subCommand.equals("rewards")) {
                // Return available job IDs
                return jobManager.getJobs().values().stream()
                    .filter(Job::isEnabled)
                    .filter(job -> job.getPermission() == null || sender.hasPermission(job.getPermission()))
                    .map(Job::getId)
                    .filter(jobId -> jobId.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
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
}