package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
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
 * Handles admin commands: exp, migrate, reload, debug.
 */
public class AdminCommandHandler extends JobCommandHandler {
    
    // Command constants
    private static final String PERM_ADMIN_EXP = "universejobs.admin.exp";
    private static final String PERM_ADMIN_DEBUG = "universejobs.admin.debug";
    private static final String NO_PERMISSION_KEY = "commands.no-permission";
    private static final String PLAYER_PREFIX = "&cPlayer ";
    private static final String JOB_SUFFIX = " for job ";
    
    public AdminCommandHandler(UniverseJobs plugin) {
        super(plugin);
    }
    
    @Override
    public boolean handleCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return false;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "exp" -> handleExpCommand(sender, args);
            case "migrate" -> handleMigrateCommand(sender, args);
            case "reload" -> handleReloadCommand(sender);
            case "debug" -> handleDebugCommand(sender, args);
            default -> {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 0) {
            return completions;
        }
        
        String subCommand = args[0].toLowerCase();
        
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            
            switch (subCommand) {
                case "exp" -> {
                    if (hasPermission(sender, PERM_ADMIN_EXP)) {
                        List<String> expSubCommands = Arrays.asList("give", "take", "set");
                        for (String expSubCommand : expSubCommands) {
                            if (expSubCommand.startsWith(input)) {
                                completions.add(expSubCommand);
                            }
                        }
                    }
                }
                case "debug" -> {
                    if (hasPermission(sender, PERM_ADMIN_DEBUG)) {
                        // Player names for debug command
                        completions.addAll(Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> name.toLowerCase().startsWith(input))
                                .collect(Collectors.toList()));
                    }
                }
                default -> {
                    // No completions for unknown commands
                }
            }
        } else if (args.length >= 3) {
            completions.addAll(getAdminTabCompletions(sender, args));
        }
        
        return completions;
    }
    
    /**
     * Handle the exp admin command.
     */
    private void handleExpCommand(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN_EXP)) {
            sender.sendMessage(languageManager.getMessage(NO_PERMISSION_KEY));
            return;
        }
        
        // /jobs exp <give|take|set> <player> <job> <amount>
        if (args.length < 5) {
            MessageUtils.sendMessage(sender, "&cUsage: /jobs exp <give|take|set> <player> <job> <amount>");
            return;
        }
        
        String expAction = sanitizeInput(args[1]);
        String playerName = sanitizeInput(args[2]);
        String jobId = sanitizeInput(args[3]);
        String amountStr = sanitizeInput(args[4]);
        
        // Validate inputs
        if (!Arrays.asList("give", "take", "set").contains(expAction.toLowerCase())) {
            MessageUtils.sendMessage(sender, "&cInvalid action. Use: give, take, or set");
            return;
        }
        
        if (!isValidPlayerName(playerName)) {
            return;
        }
        
        if (!isValidJobId(jobId)) {
            return;
        }
        
        // Validate amount with strict bounds
        double amount;
        try {
            if (amountStr.isEmpty() || amountStr.length() > 15) {
                throw new NumberFormatException("Invalid amount format");
            }
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            MessageUtils.sendMessage(sender, "&cInvalid amount: " + amountStr);
            return;
        }
        
        // Strict amount bounds checking (prevent abuse)
        if (amount <= 0 || amount > 1000000 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            MessageUtils.sendMessage(sender, "&cAmount must be between 1 and 1,000,000");
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            MessageUtils.sendMessage(sender, PLAYER_PREFIX + playerName + " not found or not online.");
            return;
        }
        
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            MessageUtils.sendMessage(sender, "&cJob " + jobId + " not found.");
            return;
        }
        
        // Check if player has the job
        if (!jobManager.hasJob(targetPlayer, jobId)) {
            MessageUtils.sendMessage(sender, PLAYER_PREFIX + playerName + " doesn't have the job " + job.getName());
            return;
        }
        
        String senderName = sender instanceof Player ? sender.getName() : "Console";
        
        switch (expAction.toLowerCase()) {
            case "give" -> {
                jobManager.addXp(targetPlayer, jobId, amount);
                MessageUtils.sendMessage(sender, "&aGave " + amount + " XP to " + playerName + JOB_SUFFIX + job.getName());
                MessageUtils.sendMessage(targetPlayer, "&aReceived " + amount + " XP for " + job.getName() + " from " + senderName);
            }
            case "take" -> {
                // Get current XP and subtract the amount (minimum 0)
                double currentXp = jobManager.getXp(targetPlayer, jobId);
                double newXp = Math.max(0, currentXp - amount);
                // Set the new XP by adding the difference
                jobManager.addXp(targetPlayer, jobId, newXp - currentXp);
                MessageUtils.sendMessage(sender, "&aTook " + amount + " XP from " + playerName + JOB_SUFFIX + job.getName());
                MessageUtils.sendMessage(targetPlayer, "&cLost " + amount + " XP for " + job.getName() + " (Admin action)");
            }
            case "set" -> {
                // Get current XP and calculate difference to set
                double currentXp = jobManager.getXp(targetPlayer, jobId);
                double difference = amount - currentXp;
                jobManager.addXp(targetPlayer, jobId, difference);
                MessageUtils.sendMessage(sender, "&aSet " + playerName + "'s XP to " + amount + JOB_SUFFIX + job.getName());
                MessageUtils.sendMessage(targetPlayer, "&aYour XP for " + job.getName() + " has been set to " + amount + " by " + senderName);
            }
            default -> {
                MessageUtils.sendMessage(sender, "&cInvalid exp action. Use: give, take, or set");
            }
        }
    }
    
    /**
     * Handle the migrate admin command.
     */
    private void handleMigrateCommand(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "universejobs.admin.migrate")) {
            sender.sendMessage(languageManager.getMessage(NO_PERMISSION_KEY));
            return;
        }
        
        // /jobs migrate [from_version] [to_version]
        String fromVersion = args.length > 1 ? sanitizeInput(args[1]) : "auto";
        String toVersion = args.length > 2 ? sanitizeInput(args[2]) : plugin.getDescription().getVersion();
        
        MessageUtils.sendMessage(sender, "&6Starting migration from " + fromVersion + " to " + toVersion + "...");
        
        // Perform migration
        MessageUtils.sendMessage(sender, "&cMigration feature not yet implemented.");
        plugin.getLogger().info("Migration requested from " + fromVersion + " to " + toVersion + " by " + sender.getName());
    }
    
    /**
     * Handle the reload admin command.
     */
    private void handleReloadCommand(CommandSender sender) {
        if (!hasPermission(sender, "universejobs.admin.reload")) {
            sender.sendMessage(languageManager.getMessage(NO_PERMISSION_KEY));
            return;
        }
        
        MessageUtils.sendMessage(sender, "&6Reloading UniverseJobs...");
        
        try {
            // Reload individual components
            plugin.getConfigManager().reloadConfig();
            plugin.getLanguageManager().reload();
            jobManager.reloadJobs();
            rewardManager.reloadRewards();
            
            MessageUtils.sendMessage(sender, "&aUniverseJobs reloaded successfully!");
        } catch (Exception e) {
            MessageUtils.sendMessage(sender, "&cFailed to reload: " + e.getMessage());
            plugin.getLogger().severe("Failed to reload plugin: " + e.getMessage());
        }
    }
    
    /**
     * Handle the debug admin command.
     */
    private void handleDebugCommand(CommandSender sender, String[] args) {
        if (!hasPermission(sender, PERM_ADMIN_DEBUG)) {
            sender.sendMessage(languageManager.getMessage(NO_PERMISSION_KEY));
            return;
        }
        
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "&cUsage: /jobs debug <player>");
            return;
        }
        
        String playerName = sanitizeInput(args[1]);
        
        if (!isValidPlayerName(playerName)) {
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            MessageUtils.sendMessage(sender, PLAYER_PREFIX + playerName + " not found or not online.");
            return;
        }
        
        MessageUtils.sendMessage(sender, "&6=== Debug Info for " + targetPlayer.getName() + " ===");
        
        // Show player's jobs and levels
        var playerJobs = jobManager.getPlayerJobs(targetPlayer);
        MessageUtils.sendMessage(sender, "&eJobs: " + playerJobs.size());
        
        for (String jobId : playerJobs) {
            Job job = jobManager.getJob(jobId);
            if (job != null) {
                int level = jobManager.getLevel(targetPlayer, jobId);
                double xp = jobManager.getXp(targetPlayer, jobId);
                MessageUtils.sendMessage(sender, "&7  " + job.getName() + " - Level " + level + " (" + xp + " XP)");
            }
        }
        
        // Show active bonuses
        if (bonusManager != null) {
            var bonuses = bonusManager.getActiveBonuses(targetPlayer.getUniqueId());
            MessageUtils.sendMessage(sender, "&eActive Bonuses: " + bonuses.size());
            for (var bonus : bonuses) {
                MessageUtils.sendMessage(sender, "&7  " + bonus.getMultiplier() + "x - " + bonus.getReason());
            }
        }
        
        // Show memory usage
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        
        MessageUtils.sendMessage(sender, "&eMemory Usage: " + usedMemory + "/" + totalMemory + " MB");
    }
    
    /**
     * Get tab completions for admin commands.
     */
    private List<String> getAdminTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        String subCommand = args[0].toLowerCase();
        
        if (args.length == 3) {
            String input = args[2].toLowerCase();
            
            if ("exp".equals(subCommand) && hasPermission(sender, PERM_ADMIN_EXP)) {
                // Player names for exp command
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 4) {
            String input = args[3].toLowerCase();
            
            if ("exp".equals(subCommand) && hasPermission(sender, PERM_ADMIN_EXP)) {
                // Job IDs for exp command
                completions.addAll(jobManager.getAllJobs().stream()
                        .map(Job::getId)
                        .filter(jobId -> jobId.toLowerCase().startsWith(input))
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 5) {
            String input = args[4].toLowerCase();
            
            if ("exp".equals(subCommand) && hasPermission(sender, PERM_ADMIN_EXP)) {
                // Amount suggestions for exp command
                List<String> amounts = Arrays.asList("100", "500", "1000", "5000", "10000");
                completions.addAll(amounts.stream()
                        .filter(amount -> amount.startsWith(input))
                        .collect(Collectors.toList()));
            }
        }
        
        return completions;
    }
}