package fr.ax_dev.universejobs.condition.impl;

import fr.ax_dev.universejobs.condition.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Condition that checks if a player has specific permissions.
 */
public class PermissionCondition extends AbstractCondition {
    
    private final String permission;
    private final boolean require;
    
    /**
     * Create a permission condition from configuration.
     * 
     * @param config The configuration section
     */
    public PermissionCondition(ConfigurationSection config) {
        super(config);
        this.permission = config.getString("permission", "");
        this.require = config.getBoolean("require", true);
    }
    
    @Override
    public boolean isMet(Player player, Event event, ConditionContext context) {
        boolean hasPermission = player.hasPermission(permission);
        return require ? hasPermission : !hasPermission;
    }
    
    @Override
    public ConditionType getType() {
        return ConditionType.PERMISSION;
    }
    
    @Override
    public String toString() {
        return "PermissionCondition{permission='" + permission + "', require=" + require + "}";
    }
}