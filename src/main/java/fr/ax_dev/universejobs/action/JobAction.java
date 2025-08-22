package fr.ax_dev.universejobs.action;

import fr.ax_dev.universejobs.condition.ConditionGroup;
import fr.ax_dev.universejobs.config.MessageConfig;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a specific action within a job that can award XP and money when performed.
 * Contains target criteria, XP and money rewards, and requirement conditions.
 */
public class JobAction {
    
    private final String target;
    private final double xp;
    private final double money;
    private final ConditionGroup requirements;
    private final String name;
    private final String description;
    private final MessageConfig message;
    private final List<String> commands;
    private final String interactType;
    private final ActionLimitManager.ActionLimit actionLimit;
    private final String enchantLevel;
    private final List<String> professions;
    private final List<String> colors;
    private final List<String> nbtTags;
    
    /**
     * Create a new JobAction from configuration.
     * 
     * @param config The configuration section containing action data
     */
    public JobAction(ConfigurationSection config) {
        this.target = config.getString("target", "");
        this.xp = config.getDouble("xp", 0.0);
        this.money = config.getDouble("money", 0.0);
        this.name = config.getString("name", "");
        this.description = config.getString("description", "");
        this.interactType = config.getString("interact-type", "RIGHT_CLICK").toUpperCase();
        this.enchantLevel = config.getString("enchant-level", null);
        
        // Load profession requirements for TRADE actions
        this.professions = loadProfessions(config);
        
        // Load color requirements for SHEAR actions
        this.colors = loadColors(config);
        
        // Load NBT requirements for EAT and other actions
        this.nbtTags = loadNbtTags(config);
        
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
        
        // Load action limits
        ConfigurationSection limitsSection = config.getConfigurationSection("limits");
        if (limitsSection != null) {
            int maxActionsPerPeriod = limitsSection.getInt("max-action-per-period", 0);
            int cooldownMinutes = limitsSection.getInt("cooldown-minutes", 60);
            boolean blockExp = limitsSection.getBoolean("block-exp", false);
            boolean blockMoney = limitsSection.getBoolean("block-money", false);
            
            if (maxActionsPerPeriod > 0) {
                this.actionLimit = new ActionLimitManager.ActionLimit(maxActionsPerPeriod, cooldownMinutes, blockExp, blockMoney);
            } else {
                this.actionLimit = null;
            }
        } else {
            this.actionLimit = null;
        }
    }
    
    /**
     * Load profession requirements from configuration.
     * Supports both single profession and list of professions.
     * 
     * @param config The configuration section
     * @return List of required professions (uppercase), or empty list if none specified
     */
    private List<String> loadProfessions(ConfigurationSection config) {
        List<String> result = new ArrayList<>();
        
        if (config.isList("profession")) {
            // Handle list format: profession: ["ARMORER", "CLERIC"]
            for (String prof : config.getStringList("profession")) {
                if (prof != null && !prof.trim().isEmpty()) {
                    result.add(prof.trim().toUpperCase());
                }
            }
        } else if (config.isString("profession")) {
            // Handle single string format: profession: "ARMORER"
            String prof = config.getString("profession");
            if (prof != null && !prof.trim().isEmpty()) {
                result.add(prof.trim().toUpperCase());
            }
        }
        
        return result;
    }
    
    /**
     * Load color requirements from configuration.
     * Supports both single color and list of colors.
     * 
     * @param config The configuration section
     * @return List of required colors (uppercase), or empty list if none specified
     */
    private List<String> loadColors(ConfigurationSection config) {
        List<String> result = new ArrayList<>();
        
        if (config.isList("color")) {
            // Handle list format: color: ["RED", "BLUE"]
            for (String color : config.getStringList("color")) {
                if (color != null && !color.trim().isEmpty()) {
                    result.add(color.trim().toUpperCase());
                }
            }
        } else if (config.isString("color")) {
            // Handle single string format: color: "RED"
            String color = config.getString("color");
            if (color != null && !color.trim().isEmpty()) {
                result.add(color.trim().toUpperCase());
            }
        }
        
        return result;
    }
    
    /**
     * Load NBT requirements from configuration.
     * Supports both single NBT tag and list of NBT tags.
     * 
     * @param config The configuration section
     * @return List of required NBT tags, or empty list if none specified
     */
    private List<String> loadNbtTags(ConfigurationSection config) {
        List<String> result = new ArrayList<>();
        
        if (config.isList("nbt")) {
            // Handle list format: nbt: ["MMOITEMS:CONSUMABLE:APPLE", "customcrops:apple"]
            for (String nbt : config.getStringList("nbt")) {
                if (nbt != null && !nbt.trim().isEmpty()) {
                    result.add(nbt.trim());
                }
            }
        } else if (config.isString("nbt")) {
            // Handle single string format: nbt: "MMOITEMS:CONSUMABLE:APPLE"
            String nbt = config.getString("nbt");
            if (nbt != null && !nbt.trim().isEmpty()) {
                result.add(nbt.trim());
            }
        }
        
        return result;
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
     * Get the money reward for this action.
     * 
     * @return The money amount
     */
    public double getMoney() {
        return money;
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
     * Get the interact type for this action (LEFT_CLICK, LEFT_SHIFT_CLICK, RIGHT_CLICK, RIGHT_SHIFT_CLICK).
     * 
     * @return The interact type
     */
    public String getInteractType() {
        return interactType;
    }
    
    /**
     * Get the enchant level requirement for this action.
     * Supports ranges like "3-5" or single values like "3".
     * 
     * @return The enchant level requirement or null if not specified
     */
    public String getEnchantLevel() {
        return enchantLevel;
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
     * Check if the target matches the given enchantment with namespace support.
     * Supports namespaces like "excellentenchants:tunnel", "advancedenchantments:harvest".
     * Default namespace is "minecraft" if none specified.
     * 
     * @param enchantmentKey The enchantment key to check (e.g., "minecraft:sharpness" or "tunnel")
     * @param enchantmentLevel The current enchantment level (can be null if not specified)
     * @return true if it matches
     */
    public boolean matchesEnchantTarget(String enchantmentKey, String enchantmentLevel) {
        if (target == null || enchantmentKey == null) {
            return false;
        }
        
        // Parse target namespace and key
        String targetNamespace = "minecraft"; // default
        String targetKey = target;
        
        if (target.contains(":")) {
            String[] targetParts = target.split(":", 2);
            targetNamespace = targetParts[0];
            targetKey = targetParts[1];
        }
        
        // Parse enchantment namespace and key
        String enchantNamespace = "minecraft"; // default
        String enchantKey = enchantmentKey;
        
        if (enchantmentKey.contains(":")) {
            String[] enchantParts = enchantmentKey.split(":", 2);
            enchantNamespace = enchantParts[0];
            enchantKey = enchantParts[1];
        }
        
        // First check if the enchantment type matches
        boolean enchantTypeMatches = false;
        
        // Check for exact match (case-insensitive)
        if (targetNamespace.equalsIgnoreCase(enchantNamespace) && targetKey.equalsIgnoreCase(enchantKey)) {
            enchantTypeMatches = true;
        }
        // Support wildcards in the key part
        else if (targetKey.equals("*")) {
            enchantTypeMatches = targetNamespace.equalsIgnoreCase(enchantNamespace);
        }
        // Prefix wildcard (e.g., "excellentenchants:tunnel_*")
        else if (targetKey.endsWith("*")) {
            String prefix = targetKey.substring(0, targetKey.length() - 1);
            enchantTypeMatches = targetNamespace.equalsIgnoreCase(enchantNamespace) && 
                   enchantKey.toUpperCase().startsWith(prefix.toUpperCase());
        }
        // Suffix wildcard (e.g., "excellentenchants:*_tunnel")
        else if (targetKey.startsWith("*")) {
            String suffix = targetKey.substring(1);
            enchantTypeMatches = targetNamespace.equalsIgnoreCase(enchantNamespace) && 
                   enchantKey.toUpperCase().endsWith(suffix.toUpperCase());
        }
        
        // If enchantment type doesn't match, return false
        if (!enchantTypeMatches) {
            return false;
        }
        
        // If no enchant-level requirement specified, just check type match
        if (enchantLevel == null || enchantLevel.trim().isEmpty()) {
            return true;
        }
        
        // Check enchantment level requirement
        return matchesEnchantLevel(enchantmentLevel);
    }
    
    /**
     * Check if the current enchantment level matches the required level.
     * Supports ranges like "3-5" or single values like "3".
     * 
     * @param currentLevelStr The current enchantment level as string
     * @return true if the level matches the requirement
     */
    private boolean matchesEnchantLevel(String currentLevelStr) {
        if (enchantLevel == null || currentLevelStr == null) {
            return true; // No level requirement
        }
        
        try {
            int currentLevel = Integer.parseInt(currentLevelStr.trim());
            String levelReq = enchantLevel.trim();
            
            // Check for range (e.g., "3-5")
            if (levelReq.contains("-")) {
                String[] parts = levelReq.split("-", 2);
                if (parts.length == 2) {
                    int minLevel = Integer.parseInt(parts[0].trim());
                    int maxLevel = Integer.parseInt(parts[1].trim());
                    return currentLevel >= minLevel && currentLevel <= maxLevel;
                }
            }
            
            // Check for exact level (e.g., "3")
            int requiredLevel = Integer.parseInt(levelReq);
            return currentLevel == requiredLevel;
            
        } catch (NumberFormatException e) {
            // Invalid format, ignore level requirement
            return true;
        }
    }
    
    /**
     * Check if the target matches the given enchantment with namespace support.
     * Compatibility method that calls the main method with null level.
     * 
     * @param enchantmentKey The enchantment key to check
     * @return true if it matches
     */
    public boolean matchesEnchantTarget(String enchantmentKey) {
        return matchesEnchantTarget(enchantmentKey, null);
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
    
    /**
     * Get the action limit configuration for this action.
     * 
     * @return The action limit or null if no limits configured
     */
    public ActionLimitManager.ActionLimit getActionLimit() {
        return actionLimit;
    }
    
    /**
     * Check if this action has limits configured.
     * 
     * @return true if limits exist
     */
    public boolean hasLimits() {
        return actionLimit != null;
    }
    
    /**
     * Get the list of required professions for this action.
     * Only applicable for TRADE actions.
     * 
     * @return List of profession names (uppercase), empty if no profession requirements
     */
    public List<String> getProfessions() {
        return professions;
    }
    
    /**
     * Check if this action has profession requirements.
     * 
     * @return true if profession requirements exist
     */
    public boolean hasProfessionRequirements() {
        return professions != null && !professions.isEmpty();
    }
    
    /**
     * Check if a villager profession matches the requirements for this action.
     * If no profession requirements are specified, all professions are accepted.
     * 
     * @param villagerProfession The villager's profession name (case-insensitive)
     * @return true if the profession matches or no requirements exist
     */
    public boolean matchesProfession(String villagerProfession) {
        // If no profession requirements, accept any profession
        if (!hasProfessionRequirements()) {
            return true;
        }
        
        // Check if the villager's profession matches any required profession
        if (villagerProfession != null) {
            String professionUpper = villagerProfession.toUpperCase();
            return professions.contains(professionUpper);
        }
        
        return false;
    }
    
    /**
     * Get the list of required colors for this action.
     * Only applicable for SHEAR actions.
     * 
     * @return List of color names (uppercase), empty if no color requirements
     */
    public List<String> getColors() {
        return colors;
    }
    
    /**
     * Check if this action has color requirements.
     * 
     * @return true if color requirements exist
     */
    public boolean hasColorRequirements() {
        return colors != null && !colors.isEmpty();
    }
    
    /**
     * Check if a sheep color matches the requirements for this action.
     * If no color requirements are specified, all colors are accepted.
     * 
     * @param sheepColor The sheep's wool color name (case-insensitive)
     * @return true if the color matches or no requirements exist
     */
    public boolean matchesColor(String sheepColor) {
        // If no color requirements, accept any color
        if (!hasColorRequirements()) {
            return true;
        }
        
        // Check if the sheep's color matches any required color
        if (sheepColor != null) {
            String colorUpper = sheepColor.toUpperCase();
            return colors.contains(colorUpper);
        }
        
        return false;
    }
    
    /**
     * Get the list of required NBT tags for this action.
     * Applicable for EAT and other actions that check item NBT.
     * 
     * @return List of NBT tags, empty if no NBT requirements
     */
    public List<String> getNbtTags() {
        return nbtTags;
    }
    
    /**
     * Check if this action has NBT requirements.
     * 
     * @return true if NBT requirements exist
     */
    public boolean hasNbtRequirements() {
        return nbtTags != null && !nbtTags.isEmpty();
    }
    
    /**
     * Check if an item's NBT matches the requirements for this action.
     * If no NBT requirements are specified, all items are accepted.
     * 
     * @param itemNbt The item's NBT/custom identifier
     * @return true if the NBT matches or no requirements exist
     */
    public boolean matchesNbt(String itemNbt) {
        // If no NBT requirements, accept any item
        if (!hasNbtRequirements()) {
            return true;
        }
        
        // Check if the item's NBT matches any required NBT
        if (itemNbt != null) {
            return nbtTags.contains(itemNbt);
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return "JobAction{target='" + target + "', xp=" + xp + ", money=" + money + ", hasRequirements=" + hasRequirements() + "}";
    }
}