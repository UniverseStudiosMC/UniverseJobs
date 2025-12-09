package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.JobManager;
import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.storage.database.DatabaseDataStorage;
import fr.ax_dev.universejobs.storage.migration.DataMigrator;
import fr.ax_dev.universejobs.storage.migration.JobsRebornConverter;
import fr.ax_dev.universejobs.storage.migration.JobsRebornDataMigrator;
import fr.ax_dev.universejobs.utils.MessageUtils;
import net.milkbowl.vault.economy.Economy;

import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AdminJobCommandHandler extends JobCommandHandler {
    
    private final JobManager jobManager;
    private final BoostCommandHandler boostHandler;
    
    // Confirmation cache for reset commands
    private final Map<String, Long> resetConfirmations = new ConcurrentHashMap<>();
    private static final long CONFIRMATION_TIMEOUT = 10000L; // 10 seconds
    
    public AdminJobCommandHandler(UniverseJobs plugin, JobManager jobManager) {
        super(plugin);
        this.jobManager = jobManager;
        this.boostHandler = new BoostCommandHandler(plugin);
    }
    
    private void sendMessage(CommandSender sender, String messageKey, String... replacements) {
        String message = languageManager.getMessage("commands.admin." + messageKey);
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = "{" + replacements[i] + "}";
                message = message.replace(placeholder, replacements[i + 1]);
            }
        }
        
        MessageUtils.sendMessage(sender, message);
    }
    
    @Override
    public boolean handleCommand(CommandSender sender, String[] args) {
        return false;
    }
    
    public boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendAdminHelp(sender);
            return true;
        }
        
        String subCommand = args[1].toLowerCase();
        
        return switch (subCommand) {
            case "give" -> handleGiveCommand(sender, args);
            case "forcejoin" -> handleForceJoin(sender, args);
            case "forceleave" -> handleForceLeave(sender, args);
            case "reset" -> handleReset(sender, args);
            case "info" -> handlePlayerInfo(sender, args);
            case "reload" -> handleReload(sender, args);
            case "boost" -> {
                boostHandler.handleCommand(sender, args);
                yield true;
            }
            case "migrate" -> handleMigrate(sender, args);
            default -> {
                sendAdminHelp(sender);
                yield true;
            }
        };
    }
    
    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendGiveHelp(sender);
            return true;
        }
        
        String giveType = args[2].toLowerCase();
        
        return switch (giveType) {
            case "xp" -> handleXpCommand(sender, args);
            case "level" -> handleLevelCommand(sender, args);
            case "custom" -> handleGiveCustom(sender, args);
            default -> {
                sendGiveHelp(sender);
                yield true;
            }
        };
    }
    
    private void sendGiveHelp(CommandSender sender) {
        sendMessage(sender, "give-help-header");
        sendMessage(sender, "give-help-xp");
        sendMessage(sender, "give-help-level");
        sendMessage(sender, "give-help-custom");
    }
    
    private boolean handleGiveCustom(CommandSender sender, String[] args) {
        if (args.length < 7) {
            MessageUtils.sendMessage(sender, "&cUsage: /jobs admin give custom <action> <player> <job> <exp> [money] [silent]");
            return true;
        }


        if (!sender.hasPermission("universejobs.admin.givecustom")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        final String action = args[3];
        final String playerName = args[4];
        final String jobId = args[5];

        final double xp;
        double tempMoney = 0;
        boolean tempSilent = false;

        try {
            String xpStr = args[6];
            if (xpStr.contains("-")) {
                String[] parts = xpStr.split("-");
                if (parts.length != 2) {
                    MessageUtils.sendMessage(sender, "&cInvalid XP range format. Use: min-max");
                    return true;
                }

                double min = Double.parseDouble(parts[0]);
                double max = Double.parseDouble(parts[1]);

                if (min > max) {
                    MessageUtils.sendMessage(sender, "&cInvalid XP range. Min cannot be greater than max.");
                    return true;
                }

                xp = min + (Math.random() * (max - min));
            } else {
                xp = Double.parseDouble(xpStr);
            }

            if (args.length >= 8) {
                String moneyStr = args[7];
                try {
                    if (moneyStr.contains("-")) {
                        String[] parts = moneyStr.split("-");
                        if (parts.length != 2) {
                            MessageUtils.sendMessage(sender, "&cInvalid money range format. Use: min-max");
                            return true;
                        }

                        double min = Double.parseDouble(parts[0]);
                        double max = Double.parseDouble(parts[1]);

                        if (min > max) {
                            MessageUtils.sendMessage(sender, "&cInvalid money range. Min cannot be greater than max.");
                            return true;
                        }

                        tempMoney = min + (Math.random() * (max - min));
                    } else {
                        tempMoney = Double.parseDouble(moneyStr);
                    }
                    tempSilent = args.length >= 9 && "silent".equalsIgnoreCase(args[8]);
                } catch (NumberFormatException e) {
                    if ("silent".equalsIgnoreCase(args[7])) {
                        tempMoney = 0;
                        tempSilent = true;
                    } else {
                        tempMoney = 0;
                        tempSilent = false;
                    }
                }
            }
        } catch (NumberFormatException e) {
            MessageUtils.sendMessage(sender, "&cInvalid amounts. Use numbers or ranges (min-max).");
            return true;
        }

        final double money = tempMoney;
        final boolean silent = tempSilent;
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            if (!silent) {
                MessageUtils.sendMessage(sender, "&cPlayer " + playerName + " not found.");
            }
            return true;
        }
        
        // Debug info
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Give custom command - Player: " + playerName + ", Job: " + jobId + ", XP: " + xp + ", Money: " + money + ", Silent: " + silent);
            plugin.getLogger().info("Target player UUID: " + target.getUniqueId() + ", HasPlayedBefore: " + target.hasPlayedBefore());
        }
        
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            if (!silent) {
                sendMessage(sender, "job-not-found", "job", jobId);
            }
            return true;
        }
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                
                // Check if player has the job - if not, don't give anything
                if (!playerData.hasJob(jobId)) {
                    if (!silent) {
                        plugin.getFoliaManager().runNextTick(() -> 
                            sendMessage(sender, "player-no-job", "player", playerName, "job", jobId));
                    }
                    return;
                }
                
                // Apply multipliers if player is online - using arrays to make final
                final double[] finalValues = {xp, money}; // [0] = finalXp, [1] = finalMoney
                
                if (target.isOnline() && target.getPlayer() != null) {
                    Player onlinePlayer = target.getPlayer();
                    finalValues[0] = applyXpMultipliers(onlinePlayer, job, xp);
                    finalValues[1] = applyMoneyMultipliers(onlinePlayer, job, money);
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Applied multipliers - Original XP: " + xp + ", Final XP: " + finalValues[0] + 
                                               ", Original Money: " + money + ", Final Money: " + finalValues[1]);
                    }
                }
                
                // Handle different actions
                switch (action.toLowerCase()) {
                    case "give" -> {
                        if (finalValues[0] > 0) {
                            playerData.addXp(jobId, finalValues[0]);
                        }
                        if (finalValues[1] > 0) {
                            addPlayerMoney(target, finalValues[1]);
                        }
                    }
                    case "set" -> {
                        // For set action, don't apply multipliers - set exact value
                        if (xp > 0) {
                            playerData.setXp(jobId, xp);
                        }
                        // Money setting not supported for set action
                        finalValues[0] = xp; // Reset to original for message
                        finalValues[1] = 0;
                    }
                    case "remove" -> {
                        // For remove action, use original values (no multipliers for removal)
                        if (xp > 0) {
                            double currentXp = playerData.getXp(jobId);
                            double newXp = Math.max(0, currentXp - xp); // Don't go below 0
                            playerData.setXp(jobId, newXp);
                        }
                        if (money > 0) {
                            removePlayerMoney(target, money);
                        }
                        finalValues[0] = xp; // Reset to original for message
                        finalValues[1] = money;
                    }
                    default -> {
                        if (!silent) {
                            plugin.getFoliaManager().runNextTick(() -> 
                                MessageUtils.sendMessage(sender, "&cInvalid action. Use: give, set, remove"));
                        }
                        return;
                    }
                }
                
                plugin.getFoliaManager().runNextTick(() -> {
                    // Show final values (with multipliers) in admin message (only if not silent)
                    if (!silent) {
                        sendMessage(sender, "givecustom-success", "player", target.getName(), 
                                   "job", job.getName(), "xp", String.valueOf(finalValues[0]), "money", String.valueOf(finalValues[1]));
                    }
                    
                    // Always send XP message to player (even in silent mode)
                    if (target.isOnline() && "give".equalsIgnoreCase(action)) {
                        Player onlinePlayer = target.getPlayer();
                        
                        // Send XP message to player with final values
                        String messageText = job.getXpMessageSettings().processMessage(finalValues[0], finalValues[1]);
                        messageText = messageText.replace("{job}", job.getDisplayName());
                        
                        plugin.getMessageSender().sendXpMessage(onlinePlayer, job, finalValues[0], finalValues[1], playerData);
                    }
                });
                
            } catch (Exception e) {
                plugin.getLogger().warning("Error during givecustom: " + e.getMessage());
                if (!silent) {
                    plugin.getFoliaManager().runNextTick(() -> 
                        MessageUtils.sendMessage(sender, "&cError while giving rewards."));
                }
            }
        });
        
        return true;
    }
    
    private void addPlayerMoney(OfflinePlayer player, double amount) {
        if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            try {
                net.milkbowl.vault.economy.Economy economy = getVaultEconomy();
                if (economy != null) {
                    economy.depositPlayer(player, amount);
                    return;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to use Vault for money reward: " + e.getMessage());
            }
        }
    }
    
    private void removePlayerMoney(OfflinePlayer player, double amount) {
        if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            try {
                net.milkbowl.vault.economy.Economy economy = getVaultEconomy();
                if (economy != null) {
                    economy.withdrawPlayer(player, amount);
                    return;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to use Vault for money removal: " + e.getMessage());
            }
        }
    }
    
    private Economy getVaultEconomy() {
        try {
            if (plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class) != null) {
                return plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class).getProvider();
            }
        } catch (Exception e) {
            // Class not found or other error
        }
        return null;
    }
    
    private boolean handleForceJoin(CommandSender sender, String[] args) {
        return handleForceJobAction(sender, args, "forcejoin", true);
    }
    
    private boolean handleForceLeave(CommandSender sender, String[] args) {
        return handleForceJobAction(sender, args, "forceleave", false);
    }
    
    private boolean handleForceJobAction(CommandSender sender, String[] args, String action, boolean isJoin) {
        if (args.length < 4) {
            sendMessage(sender, "usage." + action);
            return true;
        }
        
        if (!sender.hasPermission("universejobs.admin." + action)) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        String playerName = args[2];
        String jobId = args[3];
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            sendMessage(sender, "player-not-found", "player", playerName);
            return true;
        }
        
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            sendMessage(sender, "job-not-found", "job", jobId);
            return true;
        }
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                if (isJoin && target.isOnline()) {
                    plugin.getPlayerCache().preloadPlayer(target.getUniqueId());
                }
                
                boolean success = isJoin ? 
                    jobManager.joinJob(target.getUniqueId(), jobId) :
                    jobManager.leaveJob(target.getUniqueId(), jobId);
                
                plugin.getFoliaManager().runNextTick(() -> {
                    if (success) {
                        if (target.isOnline()) {
                            if (isJoin) {
                                plugin.getPlayerCache().addPlayerJob(target.getUniqueId(), jobId);
                            } else {
                                plugin.getPlayerCache().removePlayerJob(target.getUniqueId(), jobId);
                            }
                        }
                        
                        sendMessage(sender, action + "-success", "player", target.getName(), "job", job.getName());
                        
                        if (target.isOnline()) {
                            MessageUtils.sendMessage(target.getPlayer(), 
                                languageManager.getMessage("commands.admin." + action + "-notify", "job", job.getName()));
                        }
                    } else {
                        sendMessage(sender, action + "-failed");
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Error during " + action + ": " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() -> sendMessage(sender, action + "-failed"));
            }
        });
        
        return true;
    }
    
    private boolean handleReset(CommandSender sender, String[] args) {
        // Clean up expired confirmations
        cleanupExpiredConfirmations();
        
        if (args.length < 3) {
            sendMessage(sender, "usage.reset");
            return true;
        }
        
        if (!sender.hasPermission("universejobs.admin.reset")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        String playerName = args[2];
        String jobId = args.length > 3 ? args[3] : null;
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            sendMessage(sender, "player-not-found", "player", playerName);
            return true;
        }
        
        final String finalJobId;
        if (jobId == null) {
            // Check if there's a pending confirmation
            String confirmationKey = sender.getName() + ":" + playerName;
            long currentTime = System.currentTimeMillis();
            Long lastConfirmation = resetConfirmations.get(confirmationKey);
            
            if (lastConfirmation != null && (currentTime - lastConfirmation) <= CONFIRMATION_TIMEOUT) {
                // User confirmed within 10 seconds, proceed with reset
                resetConfirmations.remove(confirmationKey);
                finalJobId = "ALL"; // Reset all jobs
            } else {
                // First time or expired, ask for confirmation
                resetConfirmations.put(confirmationKey, currentTime);
                sendMessage(sender, "reset-confirmation", "player", playerName);
                return true;
            }
        } else {
            finalJobId = jobId;
        }
        
        if ("ALL".equalsIgnoreCase(finalJobId)) {
            plugin.getFoliaManager().runAsync(() -> {
                try {
                    PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                    Set<String> jobs = playerData.getJobs();
                    
                    for (String jobToReset : jobs) {
                        playerData.setXp(jobToReset, 0.0);
                        playerData.setLevel(jobToReset, 0);
                        playerData.leaveJob(jobToReset);
                        
                        if (plugin.getRewardManager() != null) {
                            plugin.getRewardManager().resetJobRewards(target.getUniqueId(), jobToReset);
                        }
                    }
                    
                    jobManager.savePlayerData(target.getUniqueId());
                    
                    if (target.isOnline()) {
                        plugin.getPlayerCache().cleanupPlayer(target.getUniqueId());
                        plugin.getPlayerCache().preloadPlayer(target.getUniqueId());
                    }
                    
                    plugin.getFoliaManager().runNextTick(() -> {
                        sendMessage(sender, "reset-success", "player", target.getName(), "cleanedJobs", String.valueOf(jobs.size()), "totalJobs", String.valueOf(jobs.size()));
                        
                        if (target.isOnline()) {
                            MessageUtils.sendMessage(target.getPlayer(), 
                                languageManager.getMessage("commands.admin.reset-notify"));
                        }
                    });
                } catch (Exception e) {
                    plugin.getLogger().warning("Erreur lors du reset: " + e.getMessage());
                    plugin.getFoliaManager().runNextTick(() -> sendMessage(sender, "reset-error"));
                }
            });
        } else {
            Job job = jobManager.getJob(finalJobId);
            if (job == null) {
                sendMessage(sender, "job-not-found", "job", finalJobId);
                return true;
            }
            
            plugin.getFoliaManager().runAsync(() -> {
                try {
                    PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                    
                    playerData.setXp(finalJobId, 0);
                    playerData.setLevel(finalJobId, 0);
                    
                    if (plugin.getRewardManager() != null) {
                        plugin.getRewardManager().resetJobRewards(target.getUniqueId(), finalJobId);
                    }
                    
                    jobManager.savePlayerData(target.getUniqueId());
                    
                    if (target.isOnline()) {
                        plugin.getPlayerCache().updatePlayerXp(target.getUniqueId(), finalJobId, 0, 0);
                    }
                    
                    plugin.getFoliaManager().runNextTick(() -> {
                        sendMessage(sender, "reset-job-success", "job", job.getName(), "player", target.getName());
                        
                        if (target.isOnline()) {
                            MessageUtils.sendMessage(target.getPlayer(), 
                                languageManager.getMessage("commands.admin.reset-job-notify", "job", job.getName()));
                        }
                    });
                } catch (Exception e) {
                    plugin.getLogger().warning("Erreur lors du reset du métier: " + e.getMessage());
                    plugin.getFoliaManager().runNextTick(() -> sendMessage(sender, "reset-error"));
                }
            });
        }
        
        return true;
    }
    
    private boolean handleXpCommand(CommandSender sender, String[] args) {
        if (args.length < 7) {
            sendMessage(sender, "usage.xp");
            return true;
        }

        String subType = args[2].toLowerCase();
        String action = args[3].toLowerCase();
        String playerName = args[4];
        String jobId = args[5];
        String amountStr = args[6];

        double amount;
        boolean isRange = amountStr.contains("-");

        try {
            if (isRange) {
                String[] parts = amountStr.split("-");
                if (parts.length != 2) {
                    sendMessage(sender, "invalid-amount", "amount", amountStr);
                    return true;
                }

                double min = Double.parseDouble(parts[0]);
                double max = Double.parseDouble(parts[1]);

                if (min > max) {
                    sendMessage(sender, "invalid-amount", "amount", amountStr);
                    return true;
                }

                if ((min < 0 || max < 0) && !action.equals("remove")) {
                    sendMessage(sender, "invalid-amount", "amount", amountStr);
                    return true;
                }

                amount = min + (Math.random() * (max - min));
            } else {
                amount = Double.parseDouble(amountStr);
                if (amount < 0 && !action.equals("remove")) {
                    sendMessage(sender, "invalid-amount", "amount", amountStr);
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            sendMessage(sender, "invalid-amount", "amount", amountStr);
            return true;
        }

        return switch (action) {
            case "give" -> handleGiveXp(sender, playerName, jobId, amount);
            case "set" -> handleSetXpDirect(sender, playerName, jobId, amount);
            case "remove" -> handleRemoveXp(sender, playerName, jobId, amount);
            default -> {
                sendMessage(sender, "invalid-action", "actions", "give, set, remove");
                yield true;
            }
        };
    }
    
    private boolean handleLevelCommand(CommandSender sender, String[] args) {
        if (args.length < 7) {
            sendMessage(sender, "usage.level");
            return true;
        }

        String subType = args[2].toLowerCase();
        String action = args[3].toLowerCase();
        String playerName = args[4];
        String jobId = args[5];

        int amount;
        try {
            amount = Integer.parseInt(args[6]);
            if (amount < 0 && !action.equals("remove")) {
                sendMessage(sender, "invalid-amount", "amount", args[6]);
                return true;
            }
        } catch (NumberFormatException e) {
            sendMessage(sender, "invalid-amount", "amount", args[6]);
            return true;
        }

        return switch (action) {
            case "give" -> handleAddLevel(sender, playerName, jobId, amount);
            case "set" -> handleSetLevel(sender, new String[]{"admin", "setlevel", playerName, jobId, String.valueOf(amount)});
            case "remove" -> handleRemoveLevel(sender, playerName, jobId, amount);
            default -> {
                sendMessage(sender, "invalid-action", "actions", "give, set, remove");
                yield true;
            }
        };
    }
    
    private boolean handleRemoveXp(CommandSender sender, String playerName, String jobId, double amount) {
        if (!sender.hasPermission("universejobs.admin.setxp")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            sendMessage(sender, "player-not-found", "player", playerName);
            return true;
        }
        
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            sendMessage(sender, "job-not-found", "job", jobId);
            return true;
        }
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                
                if (!playerData.hasJob(jobId)) {
                    plugin.getFoliaManager().runNextTick(() -> 
                        sendMessage(sender, "player-no-job", "player", target.getName(), "job", job.getName()));
                    return;
                }
                
                double currentXp = playerData.getXp(jobId);
                double newXp = Math.max(0, currentXp - amount);
                
                playerData.setXp(jobId, newXp);
                int newLevel = jobManager.getLevel(target.getPlayer(), jobId);
                
                jobManager.savePlayerData(target.getUniqueId());
                
                if (target.isOnline()) {
                    plugin.getPlayerCache().updatePlayerXp(target.getUniqueId(), jobId, newXp, newLevel);
                }
                
                plugin.getFoliaManager().runNextTick(() -> {
                    sendMessage(sender, "xp-taken", "amount", String.format("%.1f", amount), "player", target.getName(), "job", job.getName());
                    
                    if (target.isOnline()) {
                        MessageUtils.sendMessage(target.getPlayer(), 
                            languageManager.getMessage("commands.admin.xp-lost", "amount", String.format("%.1f", amount), "job", job.getName()));
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors de la suppression d'XP: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() -> 
                    sendMessage(sender, "xp-error", "error", e.getMessage()));
            }
        });
        
        return true;
    }
    
    private boolean handleAddLevel(CommandSender sender, String playerName, String jobId, int levels) {
        if (!sender.hasPermission("universejobs.admin.setlevel")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            sendMessage(sender, "player-not-found", "player", playerName);
            return true;
        }

        Job job = jobManager.getJob(jobId);
        if (job == null) {
            sendMessage(sender, "job-not-found", "job", jobId);
            return true;
        }

        plugin.getFoliaManager().runAsync(() -> {
            try {
                PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());

                if (!playerData.hasJob(jobId)) {
                    plugin.getFoliaManager().runNextTick(() ->
                        sendMessage(sender, "player-no-job", "player", target.getName(), "job", job.getName()));
                    return;
                }

                int currentLevel = playerData.getLevel(jobId);
                int effectiveMaxLevel = playerData.getMaxLevel(jobId);
                int newLevel = Math.min(effectiveMaxLevel, currentLevel + levels);
                double requiredXp = jobManager.getXpRequiredForLevel(jobId, newLevel);

                playerData.setLevel(jobId, newLevel);
                playerData.setXp(jobId, requiredXp);

                jobManager.savePlayerData(target.getUniqueId());

                if (target.isOnline()) {
                    plugin.getPlayerCache().updatePlayerXp(target.getUniqueId(), jobId, requiredXp, newLevel);
                }

                plugin.getFoliaManager().runNextTick(() -> {
                    sendMessage(sender, "level-given", "amount", String.valueOf(levels), "player", target.getName(), "job", job.getName(), "level", String.valueOf(newLevel));

                    if (target.isOnline()) {
                        MessageUtils.sendMessage(target.getPlayer(),
                            languageManager.getMessage("commands.admin.level-received", "amount", String.valueOf(levels), "job", job.getName()));
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors de l'ajout de niveau: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() ->
                    sendMessage(sender, "level-error", "error", e.getMessage()));
            }
        });

        return true;
    }
    
    private boolean handleRemoveLevel(CommandSender sender, String playerName, String jobId, int levels) {
        if (!sender.hasPermission("universejobs.admin.setlevel")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            sendMessage(sender, "player-not-found", "player", playerName);
            return true;
        }
        
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            sendMessage(sender, "job-not-found", "job", jobId);
            return true;
        }
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                
                if (!playerData.hasJob(jobId)) {
                    plugin.getFoliaManager().runNextTick(() ->
                        sendMessage(sender, "player-no-job", "player", target.getName(), "job", job.getName()));
                    return;
                }
                
                int currentLevel = playerData.getLevel(jobId);
                int newLevel = Math.max(0, currentLevel - levels);
                double requiredXp = jobManager.getXpRequiredForLevel(jobId, newLevel);
                
                playerData.setLevel(jobId, newLevel);
                playerData.setXp(jobId, requiredXp);
                
                jobManager.savePlayerData(target.getUniqueId());
                
                if (target.isOnline()) {
                    plugin.getPlayerCache().updatePlayerXp(target.getUniqueId(), jobId, requiredXp, newLevel);
                }
                
                plugin.getFoliaManager().runNextTick(() -> {
                    sendMessage(sender, "level-taken", "amount", String.valueOf(levels), "player", target.getName(), "job", job.getName(), "level", String.valueOf(newLevel));
                    
                    if (target.isOnline()) {
                        MessageUtils.sendMessage(target.getPlayer(), 
                            languageManager.getMessage("commands.admin.level-lost", "amount", String.valueOf(levels), "job", job.getName()));
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors de la suppression de niveau: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() ->
                    sendMessage(sender, "level-error", "error", e.getMessage()));
            }
        });
        
        return true;
    }
    
    private boolean handleSetLevel(CommandSender sender, String[] args) {
        if (args.length >= 5) {
            String[] newArgs = {"admin", "level", "set", args[2], args[3], args[4]};
            return handleLevelCommand(sender, newArgs);
        }
        sendMessage(sender, "usage.level");
        return true;
    }
    
    private boolean handleGiveXp(CommandSender sender, String playerName, String jobId, double amount) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            sendMessage(sender, "player-not-found", "player", playerName);
            return true;
        }

        Job job = jobManager.getJob(jobId);
        if (job == null) {
            sendMessage(sender, "job-not-found", "job", jobId);
            return true;
        }

        plugin.getFoliaManager().runAsync(() -> {
            try {
                PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                if (!playerData.hasJob(jobId)) {
                    plugin.getFoliaManager().runNextTick(() ->
                        sendMessage(sender, "player-no-job", "player", playerName, "job", job.getName()));
                    return;
                }

                double currentXp = playerData.getXp(jobId);
                double newXp = currentXp + amount;
                playerData.setXp(jobId, newXp);

                int newLevel = jobManager.getLevel(target.getPlayer() != null ? target.getPlayer() : null, jobId);
                if (target.getPlayer() == null) {
                    int effectiveMaxLevel = playerData.getMaxLevel(jobId);
                    newLevel = (job.getXpCurve() != null)
                        ? job.getXpCurve().getLevelForXp(newXp, effectiveMaxLevel)
                        : playerData.getLevel(jobId);
                }

                jobManager.savePlayerData(target.getUniqueId());

                if (target.isOnline()) {
                    plugin.getPlayerCache().updatePlayerXp(target.getUniqueId(), jobId, newXp, newLevel);
                }

                plugin.getFoliaManager().runNextTick(() -> {
                    sendMessage(sender, "xp-given", "amount", String.valueOf(amount), "player", playerName, "job", job.getName());

                    if (target.isOnline()) {
                        Player onlinePlayer = target.getPlayer();
                        MessageUtils.sendMessage(onlinePlayer,
                            languageManager.getMessage("commands.admin.xp-received", "amount", String.valueOf(amount), "job", job.getName()));
                    }
                });
            } catch (Exception e) {
                plugin.getFoliaManager().runNextTick(() ->
                    sendMessage(sender, "xp-error", "error", e.getMessage()));
            }
        });

        return true;
    }
    
    private boolean handleSetXpDirect(CommandSender sender, String playerName, String jobId, double amount) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            sendMessage(sender, "player-not-found", "player", playerName);
            return true;
        }

        Job job = jobManager.getJob(jobId);
        if (job == null) {
            sendMessage(sender, "job-not-found", "job", jobId);
            return true;
        }

        plugin.getFoliaManager().runAsync(() -> {
            try {
                PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                if (!playerData.hasJob(jobId)) {
                    plugin.getFoliaManager().runNextTick(() ->
                        sendMessage(sender, "player-no-job", "player", playerName, "job", job.getName()));
                    return;
                }

                playerData.setXp(jobId, amount);

                int effectiveMaxLevel = playerData.getMaxLevel(jobId);
                int newLevel = (job.getXpCurve() != null)
                    ? job.getXpCurve().getLevelForXp(amount, effectiveMaxLevel)
                    : playerData.getLevel(jobId);

                playerData.setLevel(jobId, newLevel);

                jobManager.savePlayerData(target.getUniqueId());

                if (target.isOnline()) {
                    plugin.getPlayerCache().updatePlayerXp(target.getUniqueId(), jobId, amount, newLevel);
                }

                plugin.getFoliaManager().runNextTick(() -> {
                    sendMessage(sender, "xp-set", "amount", String.valueOf(amount), "player", playerName, "job", job.getName());

                    if (target.isOnline()) {
                        Player onlinePlayer = target.getPlayer();
                        MessageUtils.sendMessage(onlinePlayer,
                            languageManager.getMessage("commands.admin.xp-set-notify", "amount", String.valueOf(amount), "job", job.getName()));
                    }
                });
            } catch (Exception e) {
                plugin.getFoliaManager().runNextTick(() ->
                    sendMessage(sender, "xp-error", "error", e.getMessage()));
            }
        });

        return true;
    }
    
    private boolean handlePlayerInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "usage.info");
            return true;
        }
        
        if (!sender.hasPermission("universejobs.admin.info")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        String playerName = args[2];
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            sendMessage(sender, "player-not-found", "player", playerName);
            return true;
        }
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                Set<String> jobs = playerData.getJobs();
                
                plugin.getFoliaManager().runNextTick(() -> {
                    sendMessage(sender, "debug-header", "player", target.getName());
                    sendMessage(sender, "debug-jobs", "count", String.valueOf(jobs.size()));
                    
                    if (jobs.isEmpty()) {
                        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.stats.no-jobs-other", "player", target.getName()));
                    } else {
                        for (String jobId : jobs) {
                            Job job = jobManager.getJob(jobId);
                            if (job != null) {
                                double xp = playerData.getXp(jobId);
                                int level = playerData.getLevel(jobId);
                                MessageUtils.sendMessage(sender, 
                                    languageManager.getMessage("commands.admin.debug-job-line", 
                                        "job", job.getName(), "level", String.valueOf(level), "xp", String.format("%.1f", xp)));
                            }
                        }
                    }
                    
                    if (target.isOnline()) {
                        var cacheStats = plugin.getPlayerCache().getStats();
                        MessageUtils.sendMessage(sender, 
                            languageManager.getMessage("commands.admin.cache-stats-hitrate", 
                                "rate", String.valueOf(cacheStats.getOrDefault("hit_rate", "N/A"))));
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors de la récupération des infos: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() ->
                    sendMessage(sender, "info-error", "error", e.getMessage()));
            }
        });
        
        return true;
    }

    
    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("universejobs.admin.reload")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        sendMessage(sender, "reload-start");
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                plugin.getConfigManager().reloadConfig();

                plugin.getJobManager().reloadJobs();

                plugin.getLanguageManager().reload();

                plugin.getMenuManager().reloadConfigurations();

                plugin.getRewardManager().reloadRewards();

                plugin.getConfigCache().reload();
                plugin.getPlayerCache().preloadOnlinePlayers();
                
                plugin.getFoliaManager().runNextTick(() ->
                    sendMessage(sender, "reload-success"));
            } catch (Exception e) {
                plugin.getLogger().severe("Error during reload: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() ->
                    sendMessage(sender, "reload-failed", "error", e.getMessage()));
            }
        });
        
        return true;
    }
    
    
    private void sendAdminHelp(CommandSender sender) {
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.header"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.give"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.boost"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.forcejoin"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.forceleave"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.reset"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.info"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.reload"));
    }
    
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("give", "boost", "forcejoin", "forceleave", "reset", "info", "reload", "migrate");
        }
        
        if (args.length == 3) {
            String subCommand = args[1].toLowerCase();
            if ("give".equals(subCommand)) {
                return Arrays.asList("xp", "level", "custom");
            }
            
            if (Arrays.asList("forcejoin", "forceleave", "reset", "info").contains(subCommand)) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            }


            if ("migrate".equals(subCommand)) {
                return Arrays.asList("sqlite", "mysql", "JobsReborn");
            }
            
            if ("boost".equals(subCommand)) {
                return boostHandler.getTabCompletions(sender, args);
            }
        }
        
        // Délégation boost pour toutes les longueurs supérieures à 3
        if (args.length > 3 && "boost".equalsIgnoreCase(args[1])) {
            return boostHandler.getTabCompletions(sender, args);
        }
        
        if (args.length == 4) {
            String subCommand = args[1].toLowerCase();
            
            if ("give".equals(subCommand)) {
                String giveType = args[2].toLowerCase();
                if ("xp".equals(giveType) || "level".equals(giveType) || "custom".equals(giveType)) {
                    return Arrays.asList("give", "set", "remove");
                }
            }
            
            if (Arrays.asList("forcejoin", "forceleave").contains(subCommand)) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            }
            
            if ("reset".equals(subCommand)) {
                List<String> options = jobManager.getAllJobs().stream()
                    .map(Job::getId)
                    .collect(Collectors.toList());
                options.add("ALL");
                return options;
            }

            if ("migrate".equals(subCommand)) {
                String migrationType = args[2];
                if ("JobsReborn".equalsIgnoreCase(migrationType)) {
                    return Arrays.asList("jobs", "data", "all");
                } else {
                    return Arrays.asList("sqlite", "mysql");
                }
            }
        }
        
        if (args.length == 5) {
            String subCommand = args[1].toLowerCase();

            if ("give".equals(subCommand)) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            }

            if (Arrays.asList("forcejoin", "forceleave").contains(subCommand)) {
                return jobManager.getAllJobs().stream()
                    .map(Job::getId)
                    .collect(Collectors.toList());
            }

            if ("migrate".equals(subCommand)) {
                String migrationType = args[2];
                if ("JobsReborn".equalsIgnoreCase(migrationType)) {
                    String migrationCommand = args[3].toLowerCase();
                    if ("jobs".equals(migrationCommand)) {
                        // Return available JobsReborn jobs for tab completion
                        try {
                            JobsRebornConverter converter = new JobsRebornConverter(plugin);
                            return converter.getAvailableJobNames();
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error getting JobsReborn job names: " + e.getMessage());
                            return new ArrayList<>();
                        }
                    }
                }
            }
        }
        
        if (args.length == 6) {
            String subCommand = args[1].toLowerCase();
            
            if ("give".equals(subCommand)) {
                return jobManager.getAllJobs().stream()
                    .map(Job::getId)
                    .collect(Collectors.toList());
            }
        }
        
        if (args.length == 7) {
            String subCommand = args[1].toLowerCase();
            
            if ("give".equals(subCommand)) {
                String giveType = args[2].toLowerCase();
                if ("xp".equals(giveType) || "level".equals(giveType)) {
                    return Arrays.asList("100", "500", "1000", "5000", "10000");
                } else if ("custom".equals(giveType)) {
                    return Arrays.asList("0", "10", "50", "100", "500", "1000");
                }
            }
        }
        
        if (args.length == 8) {
            String subCommand = args[1].toLowerCase();
            
            if ("give".equals(subCommand)) {
                String giveType = args[2].toLowerCase();
                if ("custom".equals(giveType)) {
                    return Arrays.asList("0", "10", "50", "100", "500", "1000");
                }
            }
            
            if ("migrate".equals(subCommand)) {
                String migrationType = args[2];
                if ("JobsReborn".equalsIgnoreCase(migrationType)) {
                    return Arrays.asList("jobs", "data", "all");
                } else {
                    return Arrays.asList("sqlite", "mysql");
                }
            }
        }
        
        return new ArrayList<>();
    }
    
    private boolean handleMigrate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage:");
            sender.sendMessage("§c/jobs admin migrate <old> <new> - Migrate between database types");
            sender.sendMessage("§c/jobs admin migrate JobsReborn <jobs|data|all> - Migrate from JobsReborn");
            return true;
        }

        String migrationType = args[2].toLowerCase();

        if ("jobsreborn".equalsIgnoreCase(migrationType)) {
            return handleJobsRebornMigration(sender, args);
        }

        if (args.length < 4) {
            sender.sendMessage("§cUsage: /jobs admin migrate <old> <new>");
            sender.sendMessage("§cExample: /jobs admin migrate sqlite mysql");
            return true;
        }

        String oldType = migrationType;
        String newType = args[3].toLowerCase();

        if (!oldType.equals("sqlite") && !oldType.equals("mysql")) {
            sender.sendMessage("§cInvalid old database type. Use: sqlite or mysql");
            return true;
        }

        if (!newType.equals("sqlite") && !newType.equals("mysql")) {
            sender.sendMessage("§cInvalid new database type. Use: sqlite or mysql");
            return true;
        }

        if (oldType.equals(newType)) {
            sender.sendMessage("§cOld and new database types cannot be the same");
            return true;
        }

        sender.sendMessage("§aStarting database migration from " + oldType + " to " + newType + "...");

        plugin.getFoliaManager().runAsync(() -> {
            try {
                if (plugin.isDatabaseEnabled()) {
                    DatabaseDataStorage storage = (DatabaseDataStorage) plugin.getDataStorage();
                    DataMigrator migrator = new DataMigrator(plugin, storage);

                    DataMigrator.MigrationResult result = migrator.migrateAllData().join();

                    plugin.getFoliaManager().runNextTick(() -> {
                        if (result.isSuccessful()) {
                            sender.sendMessage("§aMigration completed successfully!");
                            sender.sendMessage("§aPlayer data migrated: " + result.playerDataMigrated);
                            sender.sendMessage("§aReward data migrated: " + result.rewardDataMigrated);
                            sender.sendMessage("§aTotal records migrated: " + result.getTotalMigrated());
                            migrator.markMigrationComplete();
                        } else {
                            sender.sendMessage("§cMigration failed: " + result.error);
                        }
                    });
                } else {
                    plugin.getFoliaManager().runNextTick(() -> {
                        sender.sendMessage("§cDatabase is not enabled in configuration");
                    });
                }

            } catch (Exception e) {
                plugin.getFoliaManager().runNextTick(() -> {
                    sender.sendMessage("§cMigration failed: " + e.getMessage());
                });
                plugin.getLogger().log(Level.SEVERE, "Migration failed: " + e.getMessage(), e);
            }
        });

        return true;
    }

    private boolean handleJobsRebornMigration(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage:");
            sender.sendMessage("§c/jobs admin migrate JobsReborn jobs [job-name] - Convert job configurations");
            sender.sendMessage("§c/jobs admin migrate JobsReborn data - Migrate player data");
            sender.sendMessage("§c/jobs admin migrate JobsReborn all - Convert jobs and migrate data");
            return true;
        }

        String subCommand = args[3].toLowerCase();

        switch (subCommand) {
            case "jobs":
                String specificJob = args.length > 4 ? args[4] : null;
                return handleJobsRebornJobsConversion(sender, specificJob);
            case "data":
                return handleJobsRebornDataMigration(sender);
            case "all":
                return handleJobsRebornFullMigration(sender);
            default:
                sender.sendMessage("§cUnknown JobsReborn migration command: " + subCommand);
                sender.sendMessage("§cAvailable commands: jobs, data, all");
                return true;
        }
    }

    private boolean handleJobsRebornJobsConversion(CommandSender sender, String specificJob) {
        if (specificJob != null) {
            sender.sendMessage("§aStarting JobsReborn job conversion for: " + specificJob);
        } else {
            sender.sendMessage("§aStarting JobsReborn jobs conversion...");
        }

        plugin.getFoliaManager().runAsync(() -> {
            try {
                JobsRebornConverter converter = new JobsRebornConverter(plugin);
                JobsRebornConverter.ConversionResult result = converter.convertJobs(specificJob);

                plugin.getFoliaManager().runNextTick(() -> {
                    if (result.isSuccessful()) {
                        if (specificJob != null) {
                            sender.sendMessage("§aJob conversion completed successfully!");
                        } else {
                            sender.sendMessage("§aJobs conversion completed successfully!");
                        }
                        sender.sendMessage("§aConverted jobs: " + result.jobsConverted);

                        if (!result.convertedJobs.isEmpty()) {
                            sender.sendMessage("§aConverted jobs: §f" + String.join("§a, §f", result.convertedJobs));
                        }

                        if (!result.failedJobs.isEmpty()) {
                            sender.sendMessage("§cFailed to convert some jobs:");
                            for (String failed : result.failedJobs) {
                                sender.sendMessage("§c- " + failed);
                            }
                        }

                        sender.sendMessage("§e/jobs admin reload to load the new job configurations.");
                    } else {
                        sender.sendMessage("§cJobs conversion failed: " + result.error);
                    }
                });

            } catch (Exception e) {
                plugin.getFoliaManager().runNextTick(() -> {
                    sender.sendMessage("§cJobs conversion failed: " + e.getMessage());
                });
                plugin.getLogger().log(Level.SEVERE, "Jobs conversion failed", e);
            }
        });

        return true;
    }

    private boolean handleJobsRebornDataMigration(CommandSender sender) {
        if (!plugin.isDatabaseEnabled()) {
            sender.sendMessage("§cDatabase is not enabled. Data migration requires database storage.");
            return true;
        }

        DatabaseDataStorage storage = (DatabaseDataStorage) plugin.getDataStorage();
        JobsRebornDataMigrator migrator = new JobsRebornDataMigrator(plugin, storage);

        if (!migrator.shouldMigrate()) {
            sender.sendMessage("§cNo JobsReborn data found or migration already completed.");
            return true;
        }

        sender.sendMessage("§aStarting JobsReborn player data migration...");

        plugin.getFoliaManager().runAsync(() -> {
            try {
                JobsRebornDataMigrator.MigrationResult result = migrator.migrateAllData().join();

                plugin.getFoliaManager().runNextTick(() -> {
                    if (result.isSuccessful()) {
                        sender.sendMessage("§aData migration completed successfully!");
                        sender.sendMessage("§aPlayer data migrated: " + result.playerDataMigrated);
                        migrator.markMigrationComplete();
                    } else {
                        sender.sendMessage("§cData migration failed: " + result.error);
                    }
                });

            } catch (Exception e) {
                plugin.getFoliaManager().runNextTick(() -> {
                    sender.sendMessage("§cData migration failed: " + e.getMessage());
                });
                plugin.getLogger().log(Level.SEVERE, "Data migration failed", e);
            }
        });

        return true;
    }

    private boolean handleJobsRebornFullMigration(CommandSender sender) {
        sender.sendMessage("§aStarting full JobsReborn migration (jobs + data)...");

        plugin.getFoliaManager().runAsync(() -> {
            try {
                JobsRebornConverter converter = new JobsRebornConverter(plugin);
                JobsRebornConverter.ConversionResult jobsResult = converter.convertJobs(null); // null = all jobs

                CompletableFuture<JobsRebornDataMigrator.MigrationResult> dataFuture;

                if (plugin.isDatabaseEnabled()) {
                    DatabaseDataStorage storage = (DatabaseDataStorage) plugin.getDataStorage();
                    JobsRebornDataMigrator migrator = new JobsRebornDataMigrator(plugin, storage);

                    if (migrator.shouldMigrate()) {
                        dataFuture = migrator.migrateAllData();
                    } else {
                        dataFuture = CompletableFuture.completedFuture(null);
                    }
                } else {
                    dataFuture = CompletableFuture.completedFuture(null);
                }

                JobsRebornDataMigrator.MigrationResult dataResult = dataFuture.join();

                plugin.getFoliaManager().runNextTick(() -> {
                    sender.sendMessage("§a---- JobsReborn Migration Results ----");

                    if (jobsResult.isSuccessful()) {
                        sender.sendMessage("§aJobs conversion: §2SUCCESS");
                        sender.sendMessage("§aConverted jobs: " + jobsResult.jobsConverted);

                        if (!jobsResult.convertedJobs.isEmpty()) {
                            sender.sendMessage("§aJob IDs: §f" + String.join("§a, §f", jobsResult.convertedJobs));
                        }
                    } else {
                        sender.sendMessage("§cJobs conversion: §4FAILED");
                        sender.sendMessage("§cError: " + jobsResult.error);
                    }

                    if (dataResult != null) {
                        if (dataResult.isSuccessful()) {
                            sender.sendMessage("§aData migration: §2SUCCESS");
                            sender.sendMessage("§aPlayer data migrated: " + dataResult.playerDataMigrated);

                            DatabaseDataStorage storage = (DatabaseDataStorage) plugin.getDataStorage();
                            JobsRebornDataMigrator migrator = new JobsRebornDataMigrator(plugin, storage);
                            migrator.markMigrationComplete();
                        } else {
                            sender.sendMessage("§cData migration: §4FAILED");
                            sender.sendMessage("§cError: " + dataResult.error);
                        }
                    } else {
                        sender.sendMessage("§eData migration: §6SKIPPED");
                        sender.sendMessage("§e(Database not enabled or no data found)");
                    }

                    if (!jobsResult.failedJobs.isEmpty()) {
                        sender.sendMessage("§cSome jobs failed to convert:");
                        for (String failed : jobsResult.failedJobs) {
                            sender.sendMessage("§c- " + failed);
                        }
                    }

                    sender.sendMessage("§eRestart the server to load the new configurations.");
                });

            } catch (Exception e) {
                plugin.getFoliaManager().runNextTick(() -> {
                    sender.sendMessage("§cFull migration failed: " + e.getMessage());
                });
                plugin.getLogger().log(Level.SEVERE, "Full migration failed", e);
            }
        });

        return true;
    }

    /**
     * Apply multipliers to XP amount (permissions + boosts).
     */
    private double applyXpMultipliers(Player player, Job job, double baseXp) {
        if (player == null || job == null || baseXp <= 0) {
            return baseXp;
        }
        
        // Apply permission multipliers
        double multiplier = getPermissionMultiplier(player);
        double xpWithPermissions = baseXp * multiplier;
        
        // Apply bonus multipliers (boosts)
        if (plugin.getBonusManager() != null) {
            double bonusMultiplier = plugin.getBonusManager().getTotalMultiplier(player.getUniqueId(), job.getId());
            xpWithPermissions *= bonusMultiplier;
        }
        
        return xpWithPermissions;
    }
    
    /**
     * Apply multipliers to money amount (boosts only, no permission multipliers for money).
     */
    private double applyMoneyMultipliers(Player player, Job job, double baseMoney) {
        if (player == null || job == null || baseMoney <= 0) {
            return baseMoney;
        }
        
        // Apply money bonus multipliers (boosts)
        if (plugin.getMoneyBonusManager() != null) {
            double moneyBonusMultiplier = plugin.getMoneyBonusManager().getTotalMultiplier(player.getUniqueId(), job.getId());
            return baseMoney * moneyBonusMultiplier;
        }
        
        return baseMoney;
    }
    
    /**
     * Get permission multiplier for a player (simplified version without caching).
     */
    private double getPermissionMultiplier(Player player) {
        double multiplier = 1.0;
        
        // Skip if player is OP or has wildcard permission to avoid overpowered bonuses
        if (!player.isOp() && !player.hasPermission("*")) {
            for (int i = 10; i >= 1; i--) {
                String permission = "universejobs.multiplier.exp." + i;
                if (player.hasPermission(permission)) {
                    multiplier = i;
                    break;
                }
            }
        }
        
        return multiplier;
    }
    
    /**
     * Clean up expired reset confirmations to prevent memory leaks.
     */
    private void cleanupExpiredConfirmations() {
        long currentTime = System.currentTimeMillis();
        resetConfirmations.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > CONFIRMATION_TIMEOUT);
    }
}