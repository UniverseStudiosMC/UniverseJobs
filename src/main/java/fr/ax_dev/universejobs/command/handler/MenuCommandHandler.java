package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.menu.MenuManager;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
            MessageUtils.sendMessage((Player) sender, languageManager.getMessage("commands.menu.invalid-format"));
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
            // Rankings menu
            case "rankings":
                openRankingsMenu(player);
                return true;
                
            // Actions menu - requires job id as second argument
            case "actions":
                if (args.length < 2) {
                    MessageUtils.sendMessage(player, languageManager.getMessage("commands.menu.actions.usage"));
                    return false;
                }
                return openActionsMenu(player, args[1]);
                
            // Admin commands
            case "reload":
                if (!hasPermission(player, "universejobs.admin.menu.reload")) {
                    return false;
                }
                return reloadMenus(player);
                
            default:
                // Try to interpret as direct job name - most intuitive approach
                if (isValidJobName(subCommand)) {
                    return openJobMenu(player, subCommand);
                }
                
                MessageUtils.sendMessage(player, languageManager.getMessage("commands.menu.unknown"));
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
     * Open a specific job actions menu.
     */
    private boolean openActionsMenu(Player player, String jobId) {
        Job job = validateJobAccess(player, jobId);
        if (job == null) return false;
        
        try {
            menuManager.openJobActionsMenu(player, jobId);
            return true;
        } catch (Exception e) {
            return handleMenuError(player, "actions menu", e);
        }
    }
    
    /**
     * Reload menu configurations.
     */
    private boolean reloadMenus(Player player) {
        try {
            menuManager.reloadConfigurations();
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.menu.reload.success"));
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
            List<String> completions = new ArrayList<>(Arrays.asList(
                "rankings", // Rankings only
                "actions"   // Actions menu
            ));
            
            // Add admin commands if player has permission
            if (sender.hasPermission("universejobs.admin.menu.reload")) {
                completions.add("reload");
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
        
        // For actions command, show job completions as second argument
        if (args.length == 2 && args[0].equalsIgnoreCase("actions")) {
            return jobManager.getJobs().values().stream()
                .filter(Job::isEnabled)
                .filter(job -> job.getPermission() == null || sender.hasPermission(job.getPermission()))
                .map(Job::getId)
                .filter(jobId -> jobId.toLowerCase().startsWith(args[1].toLowerCase()))
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
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.menu.invalid-job-id"));
            return null;
        }
        
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.menu.job-not-found", "job", jobId));
            return null;
        }
        
        if (!job.isEnabled()) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.menu.job-disabled"));
            return null;
        }
        
        // Check permission if job requires one
        if (job.getPermission() != null && !player.hasPermission(job.getPermission())) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.menu.no-permission"));
            return null;
        }
        
        return job;
    }
    
    /**
     * Handle menu opening errors consistently.
     */
    private boolean handleMenuError(Player player, String menuType, Exception e) {
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.menu.failed-open", "type", menuType));
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
     * Show quick help for invalid commands.
     */
    private void showQuickHelp(Player player) {
        List<String> jobExamples = jobManager.getJobs().values().stream()
            .filter(Job::isEnabled)
            .filter(job -> job.getPermission() == null || player.hasPermission(job.getPermission()))
            .map(Job::getId)
            .limit(3)
            .collect(Collectors.toList());
        
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.menu.quick-help.header"));
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.menu.quick-help.main"));
        
        for (String jobId : jobExamples) {
            MessageUtils.sendMessage(player, languageManager.getMessage("commands.menu.quick-help.job-example", "job", jobId));
        }
        
        MessageUtils.sendMessage(player, languageManager.getMessage("commands.menu.quick-help.rankings"));
    }
}