package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.bonus.MoneyBonus;
import fr.ax_dev.universejobs.bonus.MoneyBonusManager;
import org.bukkit.command.CommandSender;

/**
 * Handles money bonus commands.
 */
public class MoneyBonusCommandHandler extends BonusCommandHandler<MoneyBonus, MoneyBonusManager> {
    
    public MoneyBonusCommandHandler(UniverseJobs plugin) {
        super(plugin, plugin.getMoneyBonusManager(), "moneybonus", "universejobs.admin.moneybonus");
    }
    
    
    
    @Override
    protected void sendBonusHelp(CommandSender sender) {
        sender.sendMessage("§6=== Money Bonus Commands ===");
        sender.sendMessage("§e/jobs moneybonus give <player|*> <multiplier> <duration> [job] [reason]");
        sender.sendMessage("§e/jobs moneybonus remove <player> [job]");
        sender.sendMessage("§e/jobs moneybonus list [player]");
        sender.sendMessage("§e/jobs moneybonus info");
        sender.sendMessage("§e/jobs moneybonus cleanup");
    }
    
}