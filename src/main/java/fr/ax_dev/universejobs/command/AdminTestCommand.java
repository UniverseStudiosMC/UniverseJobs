package fr.ax_dev.universejobs.command;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande de test pour démontrer les nouvelles fonctionnalités admin.
 */
public class AdminTestCommand implements CommandExecutor {
    
    private final UniverseJobs plugin;
    
    public AdminTestCommand(UniverseJobs plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("universejobs.admin.test")) {
            MessageUtils.sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        
        MessageUtils.sendMessage(sender, "&6=== UniverseJobs - Test Commandes Admin ===");
        MessageUtils.sendMessage(sender, "");
        MessageUtils.sendMessage(sender, "&eCommandes disponibles:");
        MessageUtils.sendMessage(sender, "&a/jobs admin forcejoin <joueur> <métier> &7- Force rejoindre un métier");
        MessageUtils.sendMessage(sender, "&a/jobs admin forceleave <joueur> <métier> &7- Force quitter un métier");
        MessageUtils.sendMessage(sender, "&a/jobs admin reset <joueur> [métier|ALL] &7- Reset données");
        MessageUtils.sendMessage(sender, "&a/jobs admin setlevel <joueur> <métier> <niveau> &7- Définir niveau");
        MessageUtils.sendMessage(sender, "&a/jobs admin setxp <joueur> <métier> <xp> &7- Définir XP");
        MessageUtils.sendMessage(sender, "&a/jobs admin addxp <joueur> <métier> <xp> &7- Ajouter XP");
        MessageUtils.sendMessage(sender, "&a/jobs admin info <joueur> &7- Infos joueur + stats cache");
        MessageUtils.sendMessage(sender, "&a/jobs admin cache <reload|stats|clear> &7- Gestion cache");
        MessageUtils.sendMessage(sender, "&a/jobs admin reload &7- Recharger tout");
        MessageUtils.sendMessage(sender, "");
        
        // Statistiques du cache si disponible
        if (plugin.getPlayerCache() != null) {
            var cacheStats = plugin.getPlayerCache().getStats();
            MessageUtils.sendMessage(sender, "&6=== Statistiques Cache Ultra-Rapide ===");
            cacheStats.forEach((key, value) -> {
                MessageUtils.sendMessage(sender, "&e" + key + ": &a" + value);
            });
            
            MessageUtils.sendMessage(sender, "");
            String configStats = plugin.getConfigCache().getCacheStats();
            MessageUtils.sendMessage(sender, "&eConfiguration Cache: &a" + configStats);
        }
        
        if (sender instanceof Player player) {
            MessageUtils.sendMessage(sender, "");
            MessageUtils.sendMessage(sender, "&bExemple d'utilisation:");
            MessageUtils.sendMessage(sender, "&f/jobs admin forcejoin " + player.getName() + " miner");
            MessageUtils.sendMessage(sender, "&f/jobs admin setlevel " + player.getName() + " miner 50");
            MessageUtils.sendMessage(sender, "&f/jobs admin info " + player.getName());
        }
        
        return true;
    }
}