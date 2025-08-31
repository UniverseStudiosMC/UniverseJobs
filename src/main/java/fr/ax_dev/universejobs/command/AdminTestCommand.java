package fr.ax_dev.universejobs.command;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.config.LanguageManager;
import fr.ax_dev.universejobs.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminTestCommand implements CommandExecutor {
    
    private final UniverseJobs plugin;
    private final LanguageManager languageManager;
    
    public AdminTestCommand(UniverseJobs plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("universejobs.admin.test")) {
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.no-permission"));
            return true;
        }
        
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.test.help"));
        
        displayCacheStats(sender);
        
        if (sender instanceof Player player) {
            displayExampleCommands(sender, player.getName());
        }
        
        return true;
    }
    
    private void displayCacheStats(CommandSender sender) {
        if (plugin.getPlayerCache() != null) {
            var cacheStats = plugin.getPlayerCache().getStats();
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.test.cache-stats.header"));
            
            cacheStats.forEach((key, value) -> {
                MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.test.cache-stats.entry", "key", key, "value", value.toString()));
            });
            
            String configStats = plugin.getConfigCache().getCacheStats();
            MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.test.config-stats", "stats", configStats));
        }
    }
    
    private void displayExampleCommands(CommandSender sender, String playerName) {
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.test.examples.header"));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.test.examples.forcejoin", "player", playerName));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.test.examples.setlevel", "player", playerName));
        MessageUtils.sendMessage(sender, languageManager.getMessage("commands.admin.test.examples.info", "player", playerName));
    }
}