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

/**
 * Handler for administrative job commands.
 * Allows managing player jobs and data.
 */
public class AdminJobCommandHandler extends JobCommandHandler {
    
    private final JobManager jobManager;
    
    public AdminJobCommandHandler(UniverseJobs plugin, JobManager jobManager) {
        super(plugin);
        this.jobManager = jobManager;
    }
    
    /**
     * Helper to send a message to sender using language manager.
     */
    private void sendMessage(CommandSender sender, String messageKey, String... replacements) {
        String message = languageManager.getMessage("commands.admin." + messageKey);
        
        // Replace placeholders
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = "{" + replacements[i] + "}";
                message = message.replace(placeholder, replacements[i + 1]);
            }
        }
        
        MessageUtils.sendMessage(sender, message);
    }
    
    /**
     * Handles admin commands.
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled
     */
    public boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendAdminHelp(sender);
            return true;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "xp":
                return handleXpCommand(sender, args);
            case "exp":
                return handleExpCommand(sender, args);
            case "level":
                return handleLevelCommand(sender, args);
            case "forcejoin":
                return handleForceJoin(sender, args);
            case "forceleave":
                return handleForceLeave(sender, args);
            case "reset":
                return handleReset(sender, args);
            case "info":
                return handlePlayerInfo(sender, args);
            case "cache":
                return handleCacheCommand(sender, args);
            case "reload":
                return handleReload(sender, args);
            case "migrate":
                return handleMigrate(sender, args);
            case "cleanup":
                return handleCleanup(sender, args);
            case "debug":
                return handleDebug(sender, args);
            default:
                sendAdminHelp(sender);
                return true;
        }
    }
    
    /**
     * Force un joueur à rejoindre un métier.
     */
    private boolean handleForceJoin(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtils.sendMessage(sender, "&cUsage: /jobs admin forcejoin <joueur> <métier>");
            return true;
        }
        
        if (!sender.hasPermission("universejobs.admin.forcejoin")) {
            MessageUtils.sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        
        String playerName = args[2];
        String jobId = args[3];
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            MessageUtils.sendMessage(sender, "&cJoueur introuvable: " + playerName);
            return true;
        }
        
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            MessageUtils.sendMessage(sender, "&cMétier introuvable: " + jobId);
            return true;
        }
        
        // Force join asynchrone
        plugin.getFoliaManager().runAsync(() -> {
            try {
                if (target.isOnline()) {
                    // Précharge le cache si le joueur est en ligne
                    plugin.getPlayerCache().preloadPlayer(target.getUniqueId());
                }
                
                boolean success = jobManager.joinJob(target.getUniqueId(), jobId);
                
                plugin.getFoliaManager().runNextTick(() -> {
                    if (success) {
                        // Met à jour le cache
                        if (target.isOnline()) {
                            plugin.getPlayerCache().addPlayerJob(target.getUniqueId(), jobId);
                        }
                        
                        MessageUtils.sendMessage(sender, "&aLe joueur &e" + target.getName() + 
                            "&a a été forcé à rejoindre le métier &e" + job.getName() + "&a.");
                        
                        if (target.isOnline()) {
                            MessageUtils.sendMessage(target.getPlayer(), 
                                "&aVous avez été ajouté au métier &e" + job.getName() + "&a par un administrateur.");
                        }
                    } else {
                        MessageUtils.sendMessage(sender, "&cEchec de l'ajout du joueur au métier. " +
                            "Le joueur a peut-être déjà ce métier.");
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du force join: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() -> {
                    MessageUtils.sendMessage(sender, "&cErreur lors de l'ajout du métier.");
                });
            }
        });
        
        return true;
    }
    
    /**
     * Force un joueur à quitter un métier.
     */
    private boolean handleForceLeave(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtils.sendMessage(sender, "&cUsage: /jobs admin forceleave <joueur> <métier>");
            return true;
        }
        
        if (!sender.hasPermission("universejobs.admin.forceleave")) {
            MessageUtils.sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        
        String playerName = args[2];
        String jobId = args[3];
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            MessageUtils.sendMessage(sender, "&cJoueur introuvable: " + playerName);
            return true;
        }
        
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            MessageUtils.sendMessage(sender, "&cMétier introuvable: " + jobId);
            return true;
        }
        
        // Force leave asynchrone
        plugin.getFoliaManager().runAsync(() -> {
            try {
                boolean success = jobManager.leaveJob(target.getUniqueId(), jobId);
                
                plugin.getFoliaManager().runNextTick(() -> {
                    if (success) {
                        // Met à jour le cache
                        if (target.isOnline()) {
                            plugin.getPlayerCache().removePlayerJob(target.getUniqueId(), jobId);
                        }
                        
                        MessageUtils.sendMessage(sender, "&aLe joueur &e" + target.getName() + 
                            "&a a été forcé à quitter le métier &e" + job.getName() + "&a.");
                        
                        if (target.isOnline()) {
                            MessageUtils.sendMessage(target.getPlayer(), 
                                "&cVous avez été retiré du métier &e" + job.getName() + "&c par un administrateur.");
                        }
                    } else {
                        MessageUtils.sendMessage(sender, "&cEchec de la suppression du métier. " +
                            "Le joueur n'a peut-être pas ce métier.");
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du force leave: " + e.getMessage());
                plugin.getFoliaManager().runAsync(() -> {
                    MessageUtils.sendMessage(sender, "&cErreur lors de la suppression du métier.");
                });
            }
        });
        
        return true;
    }
    
    /**
     * Reset complètement les données d'un joueur.
     */
    private boolean handleReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.sendMessage(sender, "&cUsage: /jobs admin reset <joueur> [métier]");
            return true;
        }
        
        if (!sender.hasPermission("universejobs.admin.reset")) {
            MessageUtils.sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        
        String playerName = args[2];
        String jobId = args.length > 3 ? args[3] : null;
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            MessageUtils.sendMessage(sender, "&cJoueur introuvable: " + playerName);
            return true;
        }
        
        // Confirmation pour reset complet
        if (jobId == null) {
            MessageUtils.sendMessage(sender, "&c⚠️ ATTENTION: Ceci va supprimer TOUTES les données de métier de " + target.getName());
            MessageUtils.sendMessage(sender, "&cPour confirmer, utilisez: /jobs admin reset " + playerName + " ALL");
            return true;
        }
        
        if ("ALL".equalsIgnoreCase(jobId)) {
            // Reset complet
            plugin.getFoliaManager().runAsync(() -> {
                try {
                    PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                    Set<String> jobs = playerData.getJobs();
                    
                    // Supprime tous les métiers et reset XP/niveaux
                    for (String jobToReset : jobs) {
                        playerData.setXp(jobToReset, 0.0);
                        playerData.setLevel(jobToReset, 0);
                        playerData.leaveJob(jobToReset);
                    }
                    
                    // Sauvegarde
                    jobManager.savePlayerData(target.getUniqueId());
                    
                    // Nettoie le cache
                    if (target.isOnline()) {
                        plugin.getPlayerCache().cleanupPlayer(target.getUniqueId());
                        plugin.getPlayerCache().preloadPlayer(target.getUniqueId());
                    }
                    
                    plugin.getFoliaManager().runNextTick(() -> {
                        MessageUtils.sendMessage(sender, "&aToutes les données de métier de &e" + target.getName() + 
                            "&a ont été supprimées.");
                        
                        if (target.isOnline()) {
                            MessageUtils.sendMessage(target.getPlayer(), 
                                "&cToutes vos données de métier ont été supprimées par un administrateur.");
                        }
                    });
                } catch (Exception e) {
                    plugin.getLogger().warning("Erreur lors du reset: " + e.getMessage());
                    plugin.getFoliaManager().runAsync(() -> {
                        MessageUtils.sendMessage(sender, "&cErreur lors du reset des données.");
                    });
                }
            });
        } else {
            // Reset d'un métier spécifique
            Job job = jobManager.getJob(jobId);
            if (job == null) {
                MessageUtils.sendMessage(sender, "&cMétier introuvable: " + jobId);
                return true;
            }
            
            plugin.getFoliaManager().runAsync(() -> {
                try {
                    PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                    
                    // Reset XP et niveau du métier
                    playerData.setXp(jobId, 0);
                    playerData.setLevel(jobId, 0);
                    
                    // Sauvegarde
                    jobManager.savePlayerData(target.getUniqueId());
                    
                    // Met à jour le cache
                    if (target.isOnline()) {
                        plugin.getPlayerCache().updatePlayerXp(target.getUniqueId(), jobId, 0, 0);
                    }
                    
                    plugin.getFoliaManager().runAsync(() -> {
                        MessageUtils.sendMessage(sender, "&aLe métier &e" + job.getName() + 
                            "&a de &e" + target.getName() + "&a a été reset.");
                        
                        if (target.isOnline()) {
                            MessageUtils.sendMessage(target.getPlayer(), 
                                "&cVotre métier &e" + job.getName() + "&c a été reset par un administrateur.");
                        }
                    });
                } catch (Exception e) {
                    plugin.getLogger().warning("Erreur lors du reset du métier: " + e.getMessage());
                    plugin.getFoliaManager().runAsync(() -> {
                        MessageUtils.sendMessage(sender, "&cErreur lors du reset du métier.");
                    });
                }
            });
        }
        
        return true;
    }
    
    /**
     * Gère les commandes XP.
     * Usage: /jobs admin xp <give|set|remove> <joueur> <métier> <montant>
     */
    private boolean handleXpCommand(CommandSender sender, String[] args) {
        if (args.length < 6) {
            MessageUtils.sendMessage(sender, "&cUsage: /jobs admin xp <give|set|remove> <joueur> <métier> <montant>");
            return true;
        }
        
        String action = args[2].toLowerCase();
        String playerName = args[3];
        String jobId = args[4];
        
        double amount;
        try {
            amount = Double.parseDouble(args[5]);
            if (amount < 0 && !action.equals("remove")) {
                MessageUtils.sendMessage(sender, "&cLe montant ne peut pas être négatif.");
                return true;
            }
        } catch (NumberFormatException e) {
            MessageUtils.sendMessage(sender, "&cMontant invalide: " + args[5]);
            return true;
        }
        
        switch (action) {
            case "give":
                return handleAddXp(sender, new String[]{"admin", "addxp", playerName, jobId, String.valueOf(amount)});
            case "set":
                return handleSetXp(sender, new String[]{"admin", "setxp", playerName, jobId, String.valueOf(amount)});
            case "remove":
                return handleRemoveXp(sender, playerName, jobId, amount);
            default:
                MessageUtils.sendMessage(sender, "&cAction invalide. Utilisez: give, set, ou remove");
                return true;
        }
    }
    
    /**
     * Gère les commandes de niveau.
     * Usage: /jobs admin level <give|set|remove> <joueur> <métier> <montant>
     */
    private boolean handleLevelCommand(CommandSender sender, String[] args) {
        if (args.length < 6) {
            MessageUtils.sendMessage(sender, "&cUsage: /jobs admin level <give|set|remove> <joueur> <métier> <montant>");
            return true;
        }
        
        String action = args[2].toLowerCase();
        String playerName = args[3];
        String jobId = args[4];
        
        int amount;
        try {
            amount = Integer.parseInt(args[5]);
            if (amount < 0 && !action.equals("remove")) {
                MessageUtils.sendMessage(sender, "&cLe montant ne peut pas être négatif.");
                return true;
            }
        } catch (NumberFormatException e) {
            MessageUtils.sendMessage(sender, "&cMontant invalide: " + args[5]);
            return true;
        }
        
        switch (action) {
            case "give":
                return handleAddLevel(sender, playerName, jobId, amount);
            case "set":
                return handleSetLevel(sender, new String[]{"admin", "setlevel", playerName, jobId, String.valueOf(amount)});
            case "remove":
                return handleRemoveLevel(sender, playerName, jobId, amount);
            default:
                MessageUtils.sendMessage(sender, "&cAction invalide. Utilisez: give, set, ou remove");
                return true;
        }
    }
    
    /**
     * Retire de l'XP à un joueur.
     */
    private boolean handleRemoveXp(CommandSender sender, String playerName, String jobId, double amount) {
        if (!sender.hasPermission("universejobs.admin.setxp")) {
            MessageUtils.sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            MessageUtils.sendMessage(sender, "&cJoueur introuvable: " + playerName);
            return true;
        }
        
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            MessageUtils.sendMessage(sender, "&cMétier introuvable: " + jobId);
            return true;
        }
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                
                if (!playerData.hasJob(jobId)) {
                    plugin.getFoliaManager().runNextTick(() -> {
                        MessageUtils.sendMessage(sender, "&cLe joueur n'a pas ce métier.");
                    });
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
                    MessageUtils.sendMessage(sender, "&c" + String.format("%.1f", amount) + " XP&c retirée du métier &e" + 
                        job.getName() + "&c pour le joueur &e" + target.getName() + "&c.");
                    
                    if (target.isOnline()) {
                        MessageUtils.sendMessage(target.getPlayer(), 
                            "&c" + String.format("%.1f", amount) + " XP&c retirée de votre métier &e" + job.getName() + "&c.");
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors de la suppression d'XP: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() -> {
                    MessageUtils.sendMessage(sender, "&cErreur lors de la suppression d'XP.");
                });
            }
        });
        
        return true;
    }
    
    /**
     * Ajoute des niveaux à un joueur.
     */
    private boolean handleAddLevel(CommandSender sender, String playerName, String jobId, int levels) {
        if (!sender.hasPermission("universejobs.admin.setlevel")) {
            MessageUtils.sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            MessageUtils.sendMessage(sender, "&cJoueur introuvable: " + playerName);
            return true;
        }
        
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            MessageUtils.sendMessage(sender, "&cMétier introuvable: " + jobId);
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
                    MessageUtils.sendMessage(sender, "&a" + levels + " niveau(x)&a ajouté(s) au métier &e" + 
                        job.getName() + "&a pour le joueur &e" + target.getName() + "&a (niveau &e" + newLevel + "&a).");
                    
                    if (target.isOnline()) {
                        MessageUtils.sendMessage(target.getPlayer(), 
                            "&a" + levels + " niveau(x)&a ajouté(s) à votre métier &e" + job.getName() + "&a!");
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors de l'ajout de niveau: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() -> {
                    MessageUtils.sendMessage(sender, "&cErreur lors de l'ajout de niveau.");
                });
            }
        });
        
        return true;
    }
    
    /**
     * Retire des niveaux à un joueur.
     */
    private boolean handleRemoveLevel(CommandSender sender, String playerName, String jobId, int levels) {
        if (!sender.hasPermission("universejobs.admin.setlevel")) {
            MessageUtils.sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            MessageUtils.sendMessage(sender, "&cJoueur introuvable: " + playerName);
            return true;
        }
        
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            MessageUtils.sendMessage(sender, "&cMétier introuvable: " + jobId);
            return true;
        }
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                
                if (!playerData.hasJob(jobId)) {
                    plugin.getFoliaManager().runNextTick(() -> {
                        MessageUtils.sendMessage(sender, "&cLe joueur n'a pas ce métier.");
                    });
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
                    MessageUtils.sendMessage(sender, "&c" + levels + " niveau(x)&c retiré(s) du métier &e" + 
                        job.getName() + "&c pour le joueur &e" + target.getName() + "&c (niveau &e" + newLevel + "&c).");
                    
                    if (target.isOnline()) {
                        MessageUtils.sendMessage(target.getPlayer(), 
                            "&c" + levels + " niveau(x)&c retiré(s) de votre métier &e" + job.getName() + "&c.");
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors de la suppression de niveau: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() -> {
                    MessageUtils.sendMessage(sender, "&cErreur lors de la suppression de niveau.");
                });
            }
        });
        
        return true;
    }
    
    /**
     * Définit le niveau d'un joueur dans un métier (legacy method for backward compatibility).
     */
    private boolean handleSetLevel(CommandSender sender, String[] args) {
        // Redirect to new format
        if (args.length >= 5) {
            String[] newArgs = {"admin", "level", "set", args[2], args[3], args[4]};
            return handleLevelCommand(sender, newArgs);
        }
        MessageUtils.sendMessage(sender, "&cUsage: /jobs admin level set <joueur> <métier> <niveau>");
        return true;
    }
    
    /**
     * Définit l'XP d'un joueur dans un métier (legacy method for backward compatibility).
     */
    private boolean handleSetXp(CommandSender sender, String[] args) {
        // Redirect to new format  
        if (args.length >= 5) {
            String[] newArgs = {"admin", "xp", "set", args[2], args[3], args[4]};
            return handleXpCommand(sender, newArgs);
        }
        MessageUtils.sendMessage(sender, "&cUsage: /jobs admin xp set <joueur> <métier> <xp>");
        return true;
    }
    
    /**
     * Ajoute de l'XP à un joueur dans un métier (legacy method for backward compatibility).
     */
    private boolean handleAddXp(CommandSender sender, String[] args) {
        // Redirect to new format
        if (args.length >= 5) {
            String[] newArgs = {"admin", "xp", "give", args[2], args[3], args[4]};
            return handleXpCommand(sender, newArgs);
        }
        MessageUtils.sendMessage(sender, "&cUsage: /jobs admin xp give <joueur> <métier> <xp>");
        return true;
    }
    
    /**
     * Affiche les informations d'un joueur.
     */
    private boolean handlePlayerInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.sendMessage(sender, "&cUsage: /jobs admin info <joueur>");
            return true;
        }
        
        if (!sender.hasPermission("universejobs.admin.info")) {
            MessageUtils.sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        
        String playerName = args[2];
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            MessageUtils.sendMessage(sender, "&cJoueur introuvable: " + playerName);
            return true;
        }
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                PlayerJobData playerData = jobManager.getPlayerData(target.getUniqueId());
                Set<String> jobs = playerData.getJobs();
                
                plugin.getFoliaManager().runAsync(() -> {
                    MessageUtils.sendMessage(sender, "&6=== Informations de " + target.getName() + " ===");
                    MessageUtils.sendMessage(sender, "&eEn ligne: " + (target.isOnline() ? "&aOui" : "&cNon"));
                    MessageUtils.sendMessage(sender, "&eNombre de métiers: &a" + jobs.size());
                    
                    if (jobs.isEmpty()) {
                        MessageUtils.sendMessage(sender, "&cAucun métier.");
                    } else {
                        MessageUtils.sendMessage(sender, "&eMétiers:");
                        for (String jobId : jobs) {
                            Job job = jobManager.getJob(jobId);
                            if (job != null) {
                                double xp = playerData.getXp(jobId);
                                int level = playerData.getLevel(jobId);
                                MessageUtils.sendMessage(sender, "&f  - &e" + job.getName() + 
                                    " &7(niveau &a" + level + "&7, XP: &a" + String.format("%.1f", xp) + "&7)");
                            }
                        }
                    }
                    
                    // Stats du cache si le joueur est en ligne
                    if (target.isOnline()) {
                        MessageUtils.sendMessage(sender, "&e=== Cache Stats ===");
                        var cacheStats = plugin.getPlayerCache().getStats();
                        MessageUtils.sendMessage(sender, "&eCache Hit Rate: &a" + 
                            cacheStats.getOrDefault("hit_rate", "N/A") + "%");
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors de la récupération des infos: " + e.getMessage());
                plugin.getFoliaManager().runAsync(() -> {
                    MessageUtils.sendMessage(sender, "&cErreur lors de la récupération des informations.");
                });
            }
        });
        
        return true;
    }
    
    /**
     * Gère les commandes de cache.
     */
    private boolean handleCacheCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.sendMessage(sender, "&cUsage: /jobs admin cache <reload|stats|clear>");
            return true;
        }
        
        if (!sender.hasPermission("universejobs.admin.cache")) {
            MessageUtils.sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        
        String cacheAction = args[2].toLowerCase();
        
        switch (cacheAction) {
            case "reload":
                plugin.getFoliaManager().runAsync(() -> {
                    try {
                        plugin.getConfigCache().reload();
                        plugin.getPlayerCache().preloadOnlinePlayers();
                        
                        plugin.getFoliaManager().runAsync(() -> {
                            MessageUtils.sendMessage(sender, "&aCache rechargé avec succès!");
                        });
                    } catch (Exception e) {
                        plugin.getLogger().warning("Erreur lors du rechargement du cache: " + e.getMessage());
                        plugin.getFoliaManager().runAsync(() -> {
                            MessageUtils.sendMessage(sender, "&cErreur lors du rechargement du cache.");
                        });
                    }
                });
                break;
                
            case "stats":
                MessageUtils.sendMessage(sender, "&6=== Statistiques du Cache ===");
                var configStats = plugin.getConfigCache().getCacheStats();
                MessageUtils.sendMessage(sender, "&eConfiguration Cache: &a" + configStats);
                
                var playerStats = plugin.getPlayerCache().getStats();
                playerStats.forEach((key, value) -> {
                    MessageUtils.sendMessage(sender, "&e" + key + ": &a" + value);
                });
                break;
                
            case "clear":
                MessageUtils.sendMessage(sender, "&cCette action va vider tout le cache des joueurs.");
                MessageUtils.sendMessage(sender, "&cPour confirmer: /jobs admin cache clear CONFIRM");
                
                if (args.length > 3 && "CONFIRM".equals(args[3])) {
                    // Clear cache pour tous les joueurs offline
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        plugin.getPlayerCache().cleanupPlayer(player.getUniqueId());
                        plugin.getPlayerCache().preloadPlayer(player.getUniqueId());
                    }
                    MessageUtils.sendMessage(sender, "&aCache des joueurs vidé et rechargé.");
                }
                break;
                
            default:
                MessageUtils.sendMessage(sender, "&cAction inconnue: " + cacheAction);
        }
        
        return true;
    }
    
    /**
     * Recharge les configurations et jobs.
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("universejobs.admin.reload")) {
            MessageUtils.sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        
        MessageUtils.sendMessage(sender, "&eRechargement d'UniverseJobs...");
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                // Reload config
                plugin.getConfigManager().loadConfig();
                
                // Reload jobs
                plugin.getJobManager().reloadJobs();
                
                // Reload cache
                plugin.getConfigCache().reload();
                plugin.getPlayerCache().preloadOnlinePlayers();
                
                plugin.getFoliaManager().runAsync(() -> {
                    MessageUtils.sendMessage(sender, "&aUniverseJobs rechargé avec succès!");
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du rechargement: " + e.getMessage());
                plugin.getFoliaManager().runAsync(() -> {
                    MessageUtils.sendMessage(sender, "&cErreur lors du rechargement: " + e.getMessage());
                });
            }
        });
        
        return true;
    }
    
    /**
     * Nettoie manuellement les métiers inexistants.
     */
    private boolean handleCleanup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("universejobs.admin.cleanup")) {
            MessageUtils.sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        
        MessageUtils.sendMessage(sender, "&eNettoyage des métiers inexistants en cours...");
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                // Trigger the cleanup manually
                plugin.getJobManager().cleanupInvalidJobs();
                
                plugin.getFoliaManager().runNextTick(() -> {
                    MessageUtils.sendMessage(sender, "&aNettoyage terminé! Vérifiez les logs pour les détails.");
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du nettoyage: " + e.getMessage());
                plugin.getFoliaManager().runNextTick(() -> {
                    MessageUtils.sendMessage(sender, "&cErreur lors du nettoyage: " + e.getMessage());
                });
            }
        });
        
        return true;
    }
    
    /**
     * Commande de debug pour diagnostiquer les problèmes.
     */
    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("universejobs.admin.debug")) {
            MessageUtils.sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        
        if (args.length < 3) {
            MessageUtils.sendMessage(sender, "&cUsage: /jobs admin debug <xp|cache|config>");
            return true;
        }
        
        String debugType = args[2].toLowerCase();
        
        switch (debugType) {
            case "xp":
                debugXpSystem(sender);
                break;
            case "cache":
                debugCacheSystem(sender);
                break;
            case "config":
                debugConfiguration(sender);
                break;
            default:
                MessageUtils.sendMessage(sender, "&cType de debug invalide: " + debugType);
                MessageUtils.sendMessage(sender, "&eTypes disponibles: xp, cache, config");
        }
        
        return true;
    }
    
    private void debugXpSystem(CommandSender sender) {
        MessageUtils.sendMessage(sender, "&6=== Debug XP System ===");
        
        // Check jobs
        var jobs = jobManager.getAllJobs();
        MessageUtils.sendMessage(sender, "&eJobs chargés: &a" + jobs.size());
        
        for (var job : jobs) {
            MessageUtils.sendMessage(sender, "&f  - &e" + job.getId() + "&f (&aactivé: " + job.isEnabled() + "&f)");
            var breakActions = job.getActions(fr.ax_dev.universejobs.action.ActionType.BREAK);
            MessageUtils.sendMessage(sender, "&f    Actions BREAK: &a" + breakActions.size());
        }
        
        // Check cache
        if (plugin.getConfigCache() != null) {
            MessageUtils.sendMessage(sender, "&eCache configuré: &aOui");
            MessageUtils.sendMessage(sender, "&eDebug activé: &a" + plugin.getConfigCache().isDebugEnabled());
        } else {
            MessageUtils.sendMessage(sender, "&cCache non configuré!");
        }
    }
    
    private void debugCacheSystem(CommandSender sender) {
        MessageUtils.sendMessage(sender, "&6=== Debug Cache System ===");
        
        if (plugin.getConfigCache() != null) {
            var stats = plugin.getConfigCache().getCacheStats();
            MessageUtils.sendMessage(sender, "&eStats du cache: &a" + stats);
        }
        
        if (plugin.getPlayerCache() != null) {
            var playerStats = plugin.getPlayerCache().getStats();
            MessageUtils.sendMessage(sender, "&eStats des joueurs:");
            playerStats.forEach((key, value) -> {
                MessageUtils.sendMessage(sender, "&f  " + key + ": &a" + value);
            });
        }
    }
    
    private void debugConfiguration(CommandSender sender) {
        MessageUtils.sendMessage(sender, "&6=== Debug Configuration ===");
        
        MessageUtils.sendMessage(sender, "&eDebug: &a" + plugin.getConfig().getBoolean("debug", false));
        MessageUtils.sendMessage(sender, "&eShow XP: &a" + plugin.getConfig().getBoolean("messages.show-xp-gain", true));
    }
    
    /**
     * Handle the exp admin command (legacy from AdminCommandHandler).
     * Usage: /jobs admin exp <give|take|set> <player> <job> <amount>
     */
    private boolean handleExpCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("universejobs.admin.exp")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        // /jobs admin exp <give|take|set> <player> <job> <amount>
        if (args.length < 6) {
            sendMessage(sender, "usage.exp");
            return true;
        }
        
        String expAction = args[2].toLowerCase();
        String playerName = args[3];
        String jobId = args[4];
        String amountStr = args[5];
        
        // Validate inputs
        if (!Arrays.asList("give", "take", "set").contains(expAction)) {
            sendMessage(sender, "invalid-action", "actions", "give, take, set");
            return true;
        }
        
        // Validate amount with strict bounds
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
        
        // Strict amount bounds checking (prevent abuse)
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
        
        // Execute async
        plugin.getFoliaManager().runAsync(() -> {
            try {
                // Check if player has the job
                PlayerJobData data = jobManager.getPlayerData(target.getUniqueId());
                if (!data.hasJob(jobId)) {
                    plugin.getFoliaManager().runNextTick(() -> {
                        sendMessage(sender, "player-no-job", "player", target.getName(), "job", job.getName());
                    });
                    return;
                }
                
                String senderName = sender instanceof Player ? sender.getName() : "Console";
                
                // Need to check if player is online for JobManager API
                if (!target.isOnline()) {
                    plugin.getFoliaManager().runNextTick(() -> {
                        sendMessage(sender, "player-offline", "player", target.getName());
                    });
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
                        // Get current XP and subtract the amount (minimum 0)
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
                        // Get current XP and calculate difference to set
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
                plugin.getFoliaManager().runNextTick(() -> {
                    MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.xp-error", "error", e.getMessage()));
                });
            }
        });
        
        return true;
    }
    
    /**
     * Handle the migrate admin command.
     */
    private boolean handleMigrate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("universejobs.admin.migrate")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        // /jobs admin migrate [from_version] [to_version]
        String fromVersion = args.length > 2 ? args[2] : "auto";
        String toVersion = args.length > 3 ? args[3] : plugin.getDescription().getVersion();
        
        sendMessage(sender, "migrate-start", "from", fromVersion, "to", toVersion);
        
        // Perform migration (placeholder for now)
        sendMessage(sender, "migrate-not-implemented");
        plugin.getLogger().info("Migration requested from " + fromVersion + " to " + toVersion + " by " + sender.getName());
        
        return true;
    }
    
    /**
     * Shows admin command help.
     */
    private void sendAdminHelp(CommandSender sender) {
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.header"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.xp"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.exp"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.level"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.forcejoin"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.forceleave"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.reset"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.info"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.cache"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.reload"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.migrate"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.debug"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.cleanup"));
    }
    
    /**
     * Tab completion for admin commands.
     */
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("xp", "exp", "level", "forcejoin", "forceleave", "reset", "info", "cache", "debug", "cleanup", "reload", "migrate");
        }
        
        if (args.length == 3) {
            String subCommand = args[1].toLowerCase();
            if ("xp".equals(subCommand) || "level".equals(subCommand)) {
                return Arrays.asList("give", "set", "remove");
            }
            
            if ("exp".equals(subCommand)) {
                return Arrays.asList("give", "take", "set");
            }
            
            if (Arrays.asList("forcejoin", "forceleave", "reset", "info").contains(subCommand)) {
                // Liste des joueurs en ligne
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            }
            
            if ("cache".equals(subCommand)) {
                return Arrays.asList("reload", "stats", "clear");
            }
            
            if ("debug".equals(subCommand)) {
                return Arrays.asList("xp", "cache", "config");
            }
            
        }
        
        if (args.length == 4) {
            String subCommand = args[1].toLowerCase();
            
            if ("xp".equals(subCommand) || "level".equals(subCommand)) {
                // Pour xp/level, args[3] devrait être le joueur
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            }
            
            if ("exp".equals(subCommand)) {
                // Pour exp, args[3] est le joueur
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            }
            
            if (Arrays.asList("forcejoin", "forceleave").contains(subCommand)) {
                // Liste des métiers
                return jobManager.getAllJobs().stream()
                    .map(Job::getId)
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
        
        if (args.length == 5) {
            String subCommand = args[1].toLowerCase();
            
            if ("xp".equals(subCommand) || "level".equals(subCommand)) {
                // Pour xp/level, args[4] devrait être le métier
                return jobManager.getAllJobs().stream()
                    .map(Job::getId)
                    .collect(Collectors.toList());
            }
            
            if ("exp".equals(subCommand)) {
                // Pour exp, args[4] est le métier
                return jobManager.getAllJobs().stream()
                    .map(Job::getId)
                    .collect(Collectors.toList());
            }
        }
        
        if (args.length == 6) {
            String subCommand = args[1].toLowerCase();
            
            if ("xp".equals(subCommand) || "level".equals(subCommand) || "exp".equals(subCommand)) {
                // Suggestions pour le montant
                return Arrays.asList("100", "500", "1000", "5000", "10000");
            }
        }
        
        return new ArrayList<>();
    }
}