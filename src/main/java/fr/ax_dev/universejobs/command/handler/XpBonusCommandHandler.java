package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.bonus.XpBonus;
import fr.ax_dev.universejobs.bonus.XpBonusManager;
import org.bukkit.command.CommandSender;

/**
 * Handles XP bonus commands.
 */
public class XpBonusCommandHandler extends BonusCommandHandler<XpBonus, XpBonusManager> {
    
    public XpBonusCommandHandler(UniverseJobs plugin) {
        super(plugin, plugin.getBonusManager(), "xpbonus", "universejobs.admin.xpbonus");
    }
    
    
    
    @Override
    protected void sendBonusHelp(CommandSender sender) {
        sender.sendMessage("§6=== XP Bonus Commands ===");
        sender.sendMessage("§e/jobs xpbonus give <player|*> <multiplier> <duration> [job] [reason]");
        sender.sendMessage("§e/jobs xpbonus remove <player> [job]");
        sender.sendMessage("§e/jobs xpbonus list [player]");
        sender.sendMessage("§e/jobs xpbonus info");
        sender.sendMessage("§e/jobs xpbonus cleanup");
    }
    
}