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
 * Handler pour les commandes administratives des jobs.
 * Permet de gérer les métiers des joueurs de force.
 */
public class AdminJobCommandHandler {
    
    private final UniverseJobs plugin;
    private final JobManager jobManager;
    
    public AdminJobCommandHandler(UniverseJobs plugin, JobManager jobManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;
    }
    
    /**
     * Helper pour envoyer un message de manière simple.
     */
    private void sendMessageToSender(CommandSender sender, String message) {
        MessageUtils.sendMessage(sender, message);
    }
    
    /**
     * Traite les commandes admin.
     * 
     * @param sender L'expéditeur de la commande
     * @param args Les arguments de la commande
     * @return true si la commande a été traitée
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
     * Affiche l'aide des commandes admin.
     */
    private void sendAdminHelp(CommandSender sender) {
        MessageUtils.sendMessage(sender, "&6=== Commandes Admin UniverseJobs ===");
        MessageUtils.sendMessage(sender, "&e/jobs admin xp <give|set|remove> <joueur> <métier> <montant> &7- Gère l'XP d'un joueur");
        MessageUtils.sendMessage(sender, "&e/jobs admin level <give|set|remove> <joueur> <métier> <montant> &7- Gère le niveau d'un joueur");
        MessageUtils.sendMessage(sender, "&e/jobs admin forcejoin <joueur> <métier> &7- Force un joueur à rejoindre un métier");
        MessageUtils.sendMessage(sender, "&e/jobs admin forceleave <joueur> <métier> &7- Force un joueur à quitter un métier");
        MessageUtils.sendMessage(sender, "&e/jobs admin reset <joueur> [métier|ALL] &7- Reset les données d'un joueur");
        MessageUtils.sendMessage(sender, "&e/jobs admin info <joueur> &7- Affiche les infos d'un joueur");
        MessageUtils.sendMessage(sender, "&e/jobs admin cache <reload|stats|clear> &7- Gère le cache");
        MessageUtils.sendMessage(sender, "&e/jobs admin debug <xp|cache|config> &7- Debug système XP/cache/config");
        MessageUtils.sendMessage(sender, "&e/jobs admin cleanup &7- Nettoie les métiers inexistants des données joueurs");
        MessageUtils.sendMessage(sender, "&e/jobs admin reload &7- Recharge la configuration et les métiers");
    }
    
    /**
     * Auto-complétion pour les commandes admin.
     */
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("xp", "level", "forcejoin", "forceleave", "reset", "info", "cache", "debug", "cleanup", "reload");
        }
        
        if (args.length == 3) {
            String subCommand = args[1].toLowerCase();
            if ("xp".equals(subCommand) || "level".equals(subCommand)) {
                return Arrays.asList("give", "set", "remove");
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
        }
        
        return new ArrayList<>();
    }
}