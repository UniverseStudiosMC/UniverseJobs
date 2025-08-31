package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.JobManager;
import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminJobCommandHandler extends JobCommandHandler {
    
    private final JobManager jobManager;
    
    public AdminJobCommandHandler(UniverseJobs plugin, JobManager jobManager) {
        super(plugin);
        this.jobManager = jobManager;
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
            case "xp" -> handleXpCommand(sender, args);
            case "level" -> handleLevelCommand(sender, args);
            case "givecustom" -> handleGiveCustom(sender, args);
            case "forcejoin" -> handleForceJoin(sender, args);
            case "forceleave" -> handleForceLeave(sender, args);
            case "reset" -> handleReset(sender, args);
            case "info" -> handlePlayerInfo(sender, args);
            case "reload" -> handleReload(sender, args);
            case "debug" -> handleDebug(sender, args);
            default -> {
                sendAdminHelp(sender);
                yield true;
            }
        };
    }
    
    private boolean handleGiveCustom(CommandSender sender, String[] args) {
        if (args.length < 6) {
            MessageUtils.sendMessage(sender, "&cUsage: /jobs admin givecustom <player> <job> <exp> <money>");
            return true;
        }
        
        if (!sender.hasPermission("universejobs.admin.givecustom")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        String playerName = args[2];
        String jobId = args[3];
        
        double xp, money;
        try {
            xp = Double.parseDouble(args[4]);
            money = Double.parseDouble(args[5]);
        } catch (NumberFormatException e) {
            MessageUtils.sendMessage(sender, "&cMontants invalides. Utilisez des nombres.");
            return true;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null || !target.hasPlayedBefore()) {
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
                        sendMessage(sender, "player-no-job", "player", playerName, "job", jobId));
                    return;
                }
                
                if (xp > 0) {
                    playerData.addXp(jobId, xp);
                }
                
                if (money > 0) {
                    addPlayerMoney(target, money);
                }
                
                plugin.getFoliaManager().runNextTick(() -> {
                    sendMessage(sender, "givecustom-success", "player", target.getName(), 
                               "job", job.getName(), "xp", String.valueOf(xp), "money", String.valueOf(money));
                    
                    if (target.isOnline()) {
                        Player onlinePlayer = target.getPlayer();
                        
                        String messageText = job.getXpMessageSettings().processMessage(xp, money);
                        messageText = messageText.replace("{job}", job.getDisplayName());
                        
                        plugin.getMessageSender().sendXpMessage(onlinePlayer, job, xp, money, playerData);
                    }
                });
                
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du givecustom: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() -> 
                    MessageUtils.sendMessage(sender, "&cErreur lors de l'attribution des récompenses."));
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
        
        if (player.isOnline()) {
            String command = "eco give " + player.getName() + " " + amount;
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
        }
    }
    
    private net.milkbowl.vault.economy.Economy getVaultEconomy() {
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
        if (args.length < 4) {
            sendMessage(sender, "usage.forcejoin");
            return true;
        }
        
        if (!sender.hasPermission("universejobs.admin.forcejoin")) {
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
                if (target.isOnline()) {
                    plugin.getPlayerCache().preloadPlayer(target.getUniqueId());
                }
                
                boolean success = jobManager.joinJob(target.getUniqueId(), jobId);
                
                plugin.getFoliaManager().runNextTick(() -> {
                    if (success) {
                        if (target.isOnline()) {
                            plugin.getPlayerCache().addPlayerJob(target.getUniqueId(), jobId);
                        }
                        
                        sendMessage(sender, "forcejoin-success", "player", target.getName(), "job", job.getName());
                        
                        if (target.isOnline()) {
                            MessageUtils.sendMessage(target.getPlayer(), 
                                languageManager.getMessage("commands.admin.forcejoin-notify", "job", job.getName()));
                        }
                    } else {
                        sendMessage(sender, "forcejoin-failed");
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du force join: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() -> sendMessage(sender, "forcejoin-failed"));
            }
        });
        
        return true;
    }
    
    private boolean handleForceLeave(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendMessage(sender, "usage.forceleave");
            return true;
        }
        
        if (!sender.hasPermission("universejobs.admin.forceleave")) {
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
                boolean success = jobManager.leaveJob(target.getUniqueId(), jobId);
                
                plugin.getFoliaManager().runNextTick(() -> {
                    if (success) {
                        if (target.isOnline()) {
                            plugin.getPlayerCache().removePlayerJob(target.getUniqueId(), jobId);
                        }
                        
                        sendMessage(sender, "forceleave-success", "player", target.getName(), "job", job.getName());
                        
                        if (target.isOnline()) {
                            MessageUtils.sendMessage(target.getPlayer(), 
                                languageManager.getMessage("commands.admin.forceleave-notify", "job", job.getName()));
                        }
                    } else {
                        sendMessage(sender, "forceleave-failed");
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du force leave: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() -> sendMessage(sender, "forceleave-failed"));
            }
        });
        
        return true;
    }
    
    private boolean handleReset(CommandSender sender, String[] args) {
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
        
        if (jobId == null) {
            sendMessage(sender, "reset-confirmation", "player", playerName);
            return true;
        }
        
        if ("ALL".equalsIgnoreCase(jobId)) {
            plugin.getFoliaManager().runAsync(() -> {
                try {
                    PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                    Set<String> jobs = playerData.getJobs();
                    
                    for (String jobToReset : jobs) {
                        playerData.setXp(jobToReset, 0.0);
                        playerData.setLevel(jobToReset, 0);
                        playerData.leaveJob(jobToReset);
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
            Job job = jobManager.getJob(jobId);
            if (job == null) {
                sendMessage(sender, "job-not-found", "job", jobId);
                return true;
            }
            
            plugin.getFoliaManager().runAsync(() -> {
                try {
                    PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                    
                    playerData.setXp(jobId, 0);
                    playerData.setLevel(jobId, 0);
                    
                    jobManager.savePlayerData(target.getUniqueId());
                    
                    if (target.isOnline()) {
                        plugin.getPlayerCache().updatePlayerXp(target.getUniqueId(), jobId, 0, 0);
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
        if (args.length < 6) {
            sendMessage(sender, "usage.xp");
            return true;
        }
        
        String action = args[2].toLowerCase();
        String playerName = args[3];
        String jobId = args[4];
        
        double amount;
        try {
            amount = Double.parseDouble(args[5]);
            if (amount < 0 && !action.equals("remove")) {
                sendMessage(sender, "invalid-amount", "amount", args[5]);
                return true;
            }
        } catch (NumberFormatException e) {
            sendMessage(sender, "invalid-amount", "amount", args[5]);
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
        if (args.length < 6) {
            sendMessage(sender, "usage.level");
            return true;
        }
        
        String action = args[2].toLowerCase();
        String playerName = args[3];
        String jobId = args[4];
        
        int amount;
        try {
            amount = Integer.parseInt(args[5]);
            if (amount < 0 && !action.equals("remove")) {
                sendMessage(sender, "invalid-amount", "amount", args[5]);
                return true;
            }
        } catch (NumberFormatException e) {
            sendMessage(sender, "invalid-amount", "amount", args[5]);
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
                    playerData.joinJob(jobId);
                    if (target.isOnline()) {
                        plugin.getPlayerCache().addPlayerJob(target.getUniqueId(), jobId);
                    }
                }
                
                int currentLevel = playerData.getLevel(jobId);
                int newLevel = Math.min(job.getMaxLevel(), currentLevel + levels);
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
    
    private boolean handleSetXp(CommandSender sender, String[] args) {
        if (args.length >= 5) {
            String[] newArgs = {"admin", "xp", "set", args[2], args[3], args[4]};
            return handleXpCommand(sender, newArgs);
        }
        sendMessage(sender, "usage.xp");
        return true;
    }
    
    private boolean handleAddXp(CommandSender sender, String[] args) {
        if (args.length >= 5) {
            String[] newArgs = {"admin", "xp", "give", args[2], args[3], args[4]};
            return handleXpCommand(sender, newArgs);
        }
        sendMessage(sender, "usage.xp");
        return true;
    }
    
    private boolean handleGiveXp(CommandSender sender, String playerName, String jobId, double amount) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            sendMessage(sender, "player-not-found", "player", playerName);
            return true;
        }
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                if (!playerData.hasJob(jobId)) {
                    sendMessage(sender, "player-no-job", "player", playerName, "job", jobId);
                    return;
                }
                
                double currentXp = playerData.getXp(jobId);
                playerData.setXp(jobId, currentXp + amount);
                jobManager.savePlayerData(target.getUniqueId());
                
                sendMessage(sender, "xp-given", "amount", String.valueOf(amount), "player", playerName, "job", jobId);
                
                if (target.isOnline()) {
                    Player onlinePlayer = target.getPlayer();
                    MessageUtils.sendMessage(onlinePlayer, 
                        languageManager.getMessage("commands.admin.xp-received", "amount", String.valueOf(amount), "job", jobId));
                }
            } catch (Exception e) {
                sendMessage(sender, "xp-error", "error", e.getMessage());
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
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                if (!playerData.hasJob(jobId)) {
                    sendMessage(sender, "player-no-job", "player", playerName, "job", jobId);
                    return;
                }
                
                playerData.setXp(jobId, amount);
                jobManager.savePlayerData(target.getUniqueId());
                
                sendMessage(sender, "xp-set", "amount", String.valueOf(amount), "player", playerName, "job", jobId);
                
                if (target.isOnline()) {
                    Player onlinePlayer = target.getPlayer();
                    MessageUtils.sendMessage(onlinePlayer, 
                        languageManager.getMessage("commands.admin.xp-set-notify", "amount", String.valueOf(amount), "job", jobId));
                }
            } catch (Exception e) {
                sendMessage(sender, "xp-error", "error", e.getMessage());
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
    
    private boolean handleCacheCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "usage.cache");
            return true;
        }
        
        if (!sender.hasPermission("universejobs.admin.cache")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        String cacheAction = args[2].toLowerCase();
        
        switch (cacheAction) {
            case "reload" -> {
                plugin.getFoliaManager().runAsync(() -> {
                    try {
                        plugin.getConfigCache().reload();
                        plugin.getPlayerCache().preloadOnlinePlayers();
                        
                        plugin.getFoliaManager().runNextTick(() ->
                            sendMessage(sender, "cache-reloaded"));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Erreur lors du rechargement du cache: " + e.getMessage());
                        plugin.getFoliaManager().runNextTick(() ->
                            sendMessage(sender, "cache-reload-error", "error", e.getMessage()));
                    }
                });
            }
            case "stats" -> {
                sendMessage(sender, "cache-stats-header");
                var configStats = plugin.getConfigCache().getCacheStats();
                MessageUtils.sendMessage(sender, 
                    languageManager.getMessage("commands.admin.cache-stats-hits", "hits", String.valueOf(configStats)));
                
                var playerStats = plugin.getPlayerCache().getStats();
                playerStats.forEach((key, value) ->
                    MessageUtils.sendMessage(sender, 
                        languageManager.getMessage("commands.admin.cache-stats-entry", "key", key, "value", String.valueOf(value))));
            }
            case "clear" -> {
                if (args.length > 3 && "CONFIRM".equals(args[3])) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        plugin.getPlayerCache().cleanupPlayer(player.getUniqueId());
                        plugin.getPlayerCache().preloadPlayer(player.getUniqueId());
                    }
                    sendMessage(sender, "cache-cleared");
                } else {
                    sendMessage(sender, "cache-clear-confirm");
                }
            }
            default -> sendMessage(sender, "cache-unknown-action", "action", cacheAction);
        }
        
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
                plugin.getLogger().info("Reloading main configuration...");
                plugin.getConfigManager().loadConfig();
                
                plugin.getLogger().info("Reloading jobs and XP curves...");
                plugin.getJobManager().reloadJobs();
                
                plugin.getLogger().info("Reloading language files...");
                plugin.getLanguageManager().reload();
                
                plugin.getLogger().info("Reloading menu configurations...");
                plugin.getMenuManager().reloadConfigurations();
                
                plugin.getLogger().info("Reloading reward configurations...");
                plugin.getRewardManager().reloadRewards();
                
                plugin.getLogger().info("Reloading cache system...");
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
    
    private boolean handleCleanup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("universejobs.admin.cleanup")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        sendMessage(sender, "cleanup-start");
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                plugin.getJobManager().cleanupInvalidJobs();
                
                plugin.getFoliaManager().runNextTick(() ->
                    sendMessage(sender, "cleanup-complete", "players", "N/A", "jobs", "N/A"));
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du nettoyage: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() ->
                    sendMessage(sender, "cleanup-error", "error", e.getMessage()));
            }
        });
        
        return true;
    }
    
    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("universejobs.admin.debug")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        if (args.length < 3) {
            sendMessage(sender, "usage.debug");
            return true;
        }
        
        String debugType = args[2].toLowerCase();
        
        switch (debugType) {
            case "xp" -> debugXpSystem(sender);
            case "cache" -> debugCacheSystem(sender);
            case "config" -> debugConfiguration(sender);
            default -> {
                sendMessage(sender, "debug-invalid-type", "type", debugType);
                sendMessage(sender, "debug-available-types");
            }
        }
        
        return true;
    }
    
    private void debugXpSystem(CommandSender sender) {
        sendMessage(sender, "debug-xp-header");
        
        var jobs = jobManager.getAllJobs();
        sendMessage(sender, "debug-jobs", "count", String.valueOf(jobs.size()));
        
        for (var job : jobs) {
            MessageUtils.sendMessage(sender, 
                languageManager.getMessage("commands.admin.debug-job-enabled", 
                    "job", job.getId(), "enabled", String.valueOf(job.isEnabled())));
            var breakActions = job.getActions(fr.ax_dev.universejobs.action.ActionType.BREAK);
            MessageUtils.sendMessage(sender, 
                languageManager.getMessage("commands.admin.debug-actions-break", 
                    "count", String.valueOf(breakActions.size())));
        }
        
        if (plugin.getConfigCache() != null) {
            sendMessage(sender, "debug-cache-configured");
            sendMessage(sender, "debug-enabled", "enabled", String.valueOf(plugin.getConfigCache().isDebugEnabled()));
        } else {
            sendMessage(sender, "debug-cache-not-configured");
        }
    }
    
    private void debugCacheSystem(CommandSender sender) {
        sendMessage(sender, "debug-cache-header");
        
        if (plugin.getConfigCache() != null) {
            var stats = plugin.getConfigCache().getCacheStats();
            MessageUtils.sendMessage(sender, 
                languageManager.getMessage("commands.admin.debug-cache-stats", "stats", String.valueOf(stats)));
        }
        
        if (plugin.getPlayerCache() != null) {
            var playerStats = plugin.getPlayerCache().getStats();
            playerStats.forEach((key, value) ->
                MessageUtils.sendMessage(sender, 
                    languageManager.getMessage("commands.admin.debug-stat-entry", "key", key, "value", String.valueOf(value))));
        }
    }
    
    private void debugConfiguration(CommandSender sender) {
        sendMessage(sender, "debug-config-header");
        
        sendMessage(sender, "debug-enabled", "enabled", String.valueOf(plugin.getConfig().getBoolean("debug", false)));
        sendMessage(sender, "debug-show-xp", "enabled", String.valueOf(plugin.getConfig().getBoolean("messages.show-xp-gain", true)));
    }
    
    private boolean handleExpCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("universejobs.admin.exp")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        if (args.length < 6) {
            sendMessage(sender, "usage.exp");
            return true;
        }
        
        String expAction = args[2].toLowerCase();
        String playerName = args[3];
        String jobId = args[4];
        String amountStr = args[5];
        
        if (!Arrays.asList("give", "take", "set").contains(expAction)) {
            sendMessage(sender, "invalid-action", "actions", "give, take, set");
            return true;
        }
        
        double amount;
        try {
            if (amountStr.isEmpty() || amountStr.length() > 15) {
                throw new NumberFormatException("Invalid amount format");
            }
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            sendMessage(sender, "invalid-amount", "amount", amountStr);
            return true;
        }
        
        if (amount <= 0 || amount > 1000000 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            sendMessage(sender, "amount-bounds");
            return true;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null || target.getName() == null) {
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
                PlayerJobData data = jobManager.getPlayerData(target.getUniqueId());
                if (!data.hasJob(jobId)) {
                    plugin.getFoliaManager().runNextTick(() ->
                        sendMessage(sender, "player-no-job", "player", target.getName(), "job", job.getName()));
                    return;
                }
                
                String senderName = sender instanceof Player ? sender.getName() : "Console";
                
                if (!target.isOnline()) {
                    plugin.getFoliaManager().runNextTick(() ->
                        sendMessage(sender, "player-offline", "player", target.getName()));
                    return;
                }
                
                Player onlinePlayer = target.getPlayer();
                
                switch (expAction) {
                    case "give" -> {
                        jobManager.addXp(onlinePlayer, jobId, amount);
                        plugin.getFoliaManager().runNextTick(() -> {
                            sendMessage(sender, "xp-given", "amount", String.valueOf(amount), "player", target.getName(), "job", job.getName());
                            MessageUtils.sendMessage(onlinePlayer, languageManager.getMessage("commands.admin.xp-received", 
                                "amount", String.valueOf(amount), "job", job.getName(), "sender", senderName));
                        });
                    }
                    case "take" -> {
                        double currentXp = data.getXp(jobId);
                        double newXp = Math.max(0, currentXp - amount);
                        double difference = newXp - currentXp;
                        jobManager.addXp(onlinePlayer, jobId, difference);
                        plugin.getFoliaManager().runNextTick(() -> {
                            sendMessage(sender, "xp-taken", "amount", String.valueOf(amount), "player", target.getName(), "job", job.getName());
                            MessageUtils.sendMessage(onlinePlayer, languageManager.getMessage("commands.admin.xp-lost", 
                                "amount", String.valueOf(amount), "job", job.getName()));
                        });
                    }
                    case "set" -> {
                        double currentXp = data.getXp(jobId);
                        double difference = amount - currentXp;
                        jobManager.addXp(onlinePlayer, jobId, difference);
                        plugin.getFoliaManager().runNextTick(() -> {
                            sendMessage(sender, "xp-set", "amount", String.valueOf(amount), "player", target.getName(), "job", job.getName());
                            MessageUtils.sendMessage(onlinePlayer, languageManager.getMessage("commands.admin.xp-set-notify", 
                                "amount", String.valueOf(amount), "job", job.getName(), "sender", senderName));
                        });
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("Error during exp command: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() ->
                    sendMessage(sender, "xp-error", "error", e.getMessage()));
            }
        });
        
        return true;
    }
    
    private boolean handleMigrate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("universejobs.admin.migrate")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        String fromVersion = args.length > 2 ? args[2] : "auto";
        String toVersion = args.length > 3 ? args[3] : plugin.getDescription().getVersion();
        
        sendMessage(sender, "migrate-start", "from", fromVersion, "to", toVersion);
        sendMessage(sender, "migrate-not-implemented");
        plugin.getLogger().info("Migration requested from " + fromVersion + " to " + toVersion + " by " + sender.getName());
        
        return true;
    }
    
    private boolean handleValidateConfig(CommandSender sender, String[] args) {
        if (!sender.hasPermission("universejobs.admin.validateconfig")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        sendMessage(sender, "validate-start");
        
        try {
            plugin.getConfigManager().reloadConfig();
            sendMessage(sender, "validate-success");
        } catch (Exception e) {
            sendMessage(sender, "validate-failed", "error", e.getMessage());
            plugin.getLogger().warning("Configuration validation failed: " + e.getMessage());
        }
        
        return true;
    }
    
    private void sendAdminHelp(CommandSender sender) {
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.header"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.xp"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.level"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.givecustom"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.forcejoin"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.forceleave"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.reset"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.info"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.reload"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.debug"));
    }
    
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("xp", "level", "givecustom", "forcejoin", "forceleave", "reset", "info", "reload", "debug");
        }
        
        if (args.length == 3) {
            String subCommand = args[1].toLowerCase();
            if ("xp".equals(subCommand) || "level".equals(subCommand)) {
                return Arrays.asList("give", "set", "remove");
            }
            
            if (Arrays.asList("forcejoin", "forceleave", "reset", "info").contains(subCommand)) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            }
            
            if ("debug".equals(subCommand)) {
                return Arrays.asList("xp", "cache", "config");
            }
        }
        
        if (args.length == 4) {
            String subCommand = args[1].toLowerCase();
            
            if ("xp".equals(subCommand) || "level".equals(subCommand)) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            }
            
            if (Arrays.asList("forcejoin", "forceleave", "givecustom").contains(subCommand)) {
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
        }
        
        if (args.length == 4) {
            String subCommand = args[1].toLowerCase();
            
            if (Arrays.asList("forcejoin", "forceleave").contains(subCommand)) {
                return jobManager.getAllJobs().stream()
                    .map(Job::getId)
                    .collect(Collectors.toList());
            }
            
            if ("givecustom".equals(subCommand)) {
                return jobManager.getAllJobs().stream()
                    .map(Job::getId)
                    .collect(Collectors.toList());
            }
        }
        
        if (args.length == 5) {
            String subCommand = args[1].toLowerCase();
            
            if ("xp".equals(subCommand) || "level".equals(subCommand)) {
                return jobManager.getAllJobs().stream()
                    .map(Job::getId)
                    .collect(Collectors.toList());
            }
            
            if ("exp".equals(subCommand)) {
                return jobManager.getAllJobs().stream()
                    .map(Job::getId)
                    .collect(Collectors.toList());
            }
            
            if ("givecustom".equals(subCommand)) {
                return Arrays.asList("0", "10", "50", "100", "500", "1000");
            }
        }
        
        if (args.length == 6) {
            String subCommand = args[1].toLowerCase();
            
            if ("xp".equals(subCommand) || "level".equals(subCommand)) {
                return Arrays.asList("100", "500", "1000", "5000", "10000");
            }
            
            if ("givecustom".equals(subCommand)) {
                return Arrays.asList("0", "10", "50", "100", "500", "1000");
            }
        }
        
        return new ArrayList<>();
    }
}