package fr.ax_dev.jobsAdventure.action;

import fr.ax_dev.jobsAdventure.condition.ConditionGroup;
import fr.ax_dev.jobsAdventure.config.MessageConfig;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * Represents a specific action within a job that can award XP when performed.
 * Contains target criteria, XP rewards, and requirement conditions.
 */
public class JobAction {
    
    private final String target;
    private final double xp;
    private final ConditionGroup requirements;
    private final String name;
    private final String description;
    private final MessageConfig message;
    private final List<String> commands;
    
    /**
     * Create a new JobAction from configuration.
     * 
     * @param config The configuration section containing action data
     */
    public JobAction(ConfigurationSection config) {
        this.target = config.getString("target", "");
        this.xp = config.getDouble("xp", 0.0);
        this.name = config.getString("name", "");
        this.description = config.getString("description", "");
        
        // Load message configuration
        ConfigurationSection messageSection = config.getConfigurationSection("message");
        this.message = messageSection != null ? new MessageConfig(messageSection) : null;
        
        // Load commands
        this.commands = MessageConfig.parseCommands(config, "commands");
        
        // Load requirements
        ConfigurationSection requirementsSection = config.getConfigurationSection("requirements");
        if (requirementsSection != null) {
            this.requirements = new ConditionGroup(requirementsSection);
        } else {
            this.requirements = null;
        }
    }
    
    /**
     * Get the target for this action (e.g., block type, entity type, etc.).
     * 
     * @return The target string
     */
    public String getTarget() {
        return target;
    }
    
    /**
     * Get the XP reward for this action.
     * 
     * @return The XP amount
     */
    public double getXp() {
        return xp;
    }
    
    /**
     * Get the requirements that must be met for this action.
     * 
     * @return The condition group, or null if no requirements
     */
    public ConditionGroup getRequirements() {
        return requirements;
    }
    
    /**
     * Get the display name for this action.
     * 
     * @return The action name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the description for this action.
     * 
     * @return The action description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this action has requirements.
     * 
     * @return true if requirements exist
     */
    public boolean hasRequirements() {
        return requirements != null;
    }
    
    /**
     * Get the message configuration for this action.
     * 
     * @return The message config or null
     */
    public MessageConfig getMessage() {
        return message;
    }
    
    /**
     * Get the commands to execute for this action.
     * 
     * @return The list of commands
     */
    public List<String> getCommands() {
        return commands;
    }
    
    /**
     * Check if this action has a message configuration.
     * 
     * @return true if message exists
     */
    public boolean hasMessage() {
        return message != null && message.hasContent();
    }
    
    /**
     * Check if this action has commands to execute.
     * 
     * @return true if commands exist
     */
    public boolean hasCommands() {
        return commands != null && !commands.isEmpty();
    }
    
    /**
     * Check if the target matches the given string or Nexo block ID.
     * Supports wildcards, case-insensitive matching, and Nexo block IDs.
     * 
     * @param targetToCheck The target to check against (Material name or Nexo ID)
     * @param nexoBlockId The Nexo block ID if applicable (can be null)
     * @return true if it matches
     */
    public boolean matchesTarget(String targetToCheck, String nexoBlockId) {
        if (target == null || targetToCheck == null) {
            return false;
        }
        
        // Check for Nexo target (format: "nexo:block_id")
        if (target.startsWith("nexo:")) {
            String nexoTargetId = target.substring(5); // Remove "nexo:" prefix
            return nexoBlockId != null && nexoTargetId.equalsIgnoreCase(nexoBlockId);
        }
        
        // Standard matching for vanilla blocks
        return matchesTarget(targetToCheck);
    }
    
    /**
     * Check if the target matches the given string.
     * Supports wildcards and case-insensitive matching.
     * 
     * @param targetToCheck The target to check against
     * @return true if it matches
     */
    public boolean matchesTarget(String targetToCheck) {
        if (target == null || targetToCheck == null) {
            return false;
        }
        
        // Exact match (case-insensitive)
        if (target.equalsIgnoreCase(targetToCheck)) {
            return true;
        }
        
        // Wildcard support
        if (target.equals("*")) {
            return true;
        }
        
        // Prefix wildcard (e.g., "STONE_*")
        if (target.endsWith("*")) {
            String prefix = target.substring(0, target.length() - 1);
            return targetToCheck.toUpperCase().startsWith(prefix.toUpperCase());
        }
        
        // Suffix wildcard (e.g., "*_ORE")
        if (target.startsWith("*")) {
            String suffix = target.substring(1);
            return targetToCheck.toUpperCase().endsWith(suffix.toUpperCase());
        }
        
        return false;
    }
    
    /**
     * Check if this action targets a Nexo custom block.
     * 
     * @return true if the target starts with "nexo:"
     */
    public boolean isNexoTarget() {
        return target != null && target.startsWith("nexo:");
    }
    
    @Override
    public String toString() {
        return "JobAction{target='" + target + "', xp=" + xp + ", hasRequirements=" + hasRequirements() + "}";
    }
}