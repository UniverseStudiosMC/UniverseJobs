package fr.ax_dev.universejobs.bonus;

import fr.ax_dev.universejobs.UniverseJobs;
import java.util.UUID;

/**
 * Manages temporary XP bonuses for players.
 */
public class XpBonusManager extends BaseBonusManager<XpBonus> {
    
    /**
     * Create a new XP bonus manager.
     * 
     * @param plugin The plugin instance
     */
    public XpBonusManager(UniverseJobs plugin) {
        super(plugin);
    }
    
    @Override
    protected XpBonus createBonus(UUID playerId, String jobId, double multiplier, long duration, String reason, String grantedBy, String boostId, boolean isGlobal, String actionType, String actionId) {
        return new XpBonus(playerId, jobId, multiplier, duration, reason, grantedBy, boostId, isGlobal, actionType, actionId);
    }
    
    @Override
    protected String getBonusTypeName() {
        return "XP";
    }

    @Override
    protected String getMultiplierPermissionType() {
        return "exp";
    }
}