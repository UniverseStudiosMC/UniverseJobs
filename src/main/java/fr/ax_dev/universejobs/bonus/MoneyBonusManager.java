package fr.ax_dev.universejobs.bonus;

import fr.ax_dev.universejobs.UniverseJobs;
import java.util.UUID;

/**
 * Manages temporary money bonuses for players.
 */
public class MoneyBonusManager extends BaseBonusManager<MoneyBonus> {
    
    /**
     * Create a new money bonus manager.
     * 
     * @param plugin The plugin instance
     */
    public MoneyBonusManager(UniverseJobs plugin) {
        super(plugin);
    }
    
    
    @Override
    protected MoneyBonus createBonus(UUID playerId, String jobId, double multiplier, long duration, String reason, String grantedBy) {
        return new MoneyBonus(playerId, jobId, multiplier, duration, reason, grantedBy);
    }
    
    @Override
    protected String getBonusTypeName() {
        return "Money";
    }
    
}