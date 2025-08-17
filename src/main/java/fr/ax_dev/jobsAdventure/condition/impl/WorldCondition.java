package fr.ax_dev.jobsAdventure.condition.impl;

import fr.ax_dev.jobsAdventure.condition.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

/**
 * Condition that checks the world the player is in.
 */
public class WorldCondition extends AbstractCondition {
    
    private final List<String> allowedWorlds;
    private final boolean blacklist;
    
    /**
     * Create a world condition from configuration.
     * 
     * @param config The configuration section
     */
    public WorldCondition(ConfigurationSection config) {
        super(config);
        this.allowedWorlds = config.getStringList("worlds");
        this.blacklist = config.getBoolean("blacklist", false);
    }
    
    @Override
    public boolean isMet(Player player, Event event, ConditionContext context) {
        String playerWorld = player.getWorld().getName();
        boolean inList = allowedWorlds.contains(playerWorld);
        
        return blacklist ? !inList : inList;
    }
    
    @Override
    public ConditionType getType() {
        return ConditionType.WORLD;
    }
    
    
    @Override
    public String toString() {
        return "WorldCondition{worlds=" + allowedWorlds.size() + ", blacklist=" + blacklist + "}";
    }
}