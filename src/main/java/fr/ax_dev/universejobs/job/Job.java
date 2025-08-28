package fr.ax_dev.universejobs.job;

import fr.ax_dev.universejobs.action.ActionType;
import fr.ax_dev.universejobs.action.JobAction;
import fr.ax_dev.universejobs.xp.XpCurve;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Represents a job that players can join.
 * Contains all job data including actions, requirements, and metadata.
 */
public class Job {
    
    private static final String XP_TYPE_CURVE = "CURVE";
    
    private final String id;
    private final String name;
    private final String description;
    private final List<String> lore;
    private final String permission;
    private final int maxLevel;
    private final Map<ActionType, List<JobAction>> actions;
    private final String iconMaterial;
    private final int customModelData;
    private final boolean enabled;
    private final String xpType; // CURVE or EQUATION
    private final String xpValue; // curve name or equation
    private XpCurve xpCurve;
    private boolean xpCurveError = false;
    private String xpCurveErrorMessage;
    private final XpMessageSettings xpMessageSettings;
    private final String guiReward;
    private final String rewardsFile;
    private final ConfigurationSection config;
    
    /**
     * Create a new Job instance.
     * 
     * @param id The unique identifier for this job
     * @param config The configuration section containing job data
     */
    public Job(String id, ConfigurationSection config) {
        this.id = id;
        this.config = config; // Store the config reference
        this.name = config.getString("name", id);
        this.description = config.getString("description", "");
        this.lore = config.getStringList("lore");
        this.permission = config.getString("permission", "universejobs.job." + id.toLowerCase());
        this.maxLevel = config.getInt("max-level", 100);
        
        // Load icon configuration
        ConfigurationSection iconSection = config.getConfigurationSection("icon");
        if (iconSection != null) {
            this.iconMaterial = iconSection.getString("material", "STONE");
            String customModelStr = iconSection.getString("custom-model-data", "");
            this.customModelData = customModelStr.isEmpty() ? 0 : 
                Integer.parseInt(customModelStr.replaceAll("[^0-9]", "0"));
        } else {
            // Fallback for old format
            this.iconMaterial = config.getString("icon", "STONE");
            this.customModelData = 0;
        }
        
        this.enabled = config.getBoolean("enabled", true);
        // Load new XP format
        ConfigurationSection xpSection = config.getConfigurationSection("xp");
        if (xpSection != null) {
            this.xpType = xpSection.getString("type", XP_TYPE_CURVE);
            this.xpValue = xpSection.getString("xp");
            this.xpMessageSettings = new XpMessageSettings(xpSection.getConfigurationSection("message"));
        } else {
            // Fallback for old format
            if (config.contains("xp-equation")) {
                this.xpType = "EQUATION";
                this.xpValue = config.getString("xp-equation");
            } else {
                this.xpType = XP_TYPE_CURVE;
                this.xpValue = config.getString("xp-curve", "default");
            }
            this.xpMessageSettings = new XpMessageSettings(config.getConfigurationSection("xp-message"));
        }
        this.xpCurve = null; // Will be set by JobManager
        this.guiReward = config.getString("gui-reward");
        this.rewardsFile = config.getString("rewards");
        
        this.actions = new EnumMap<>(ActionType.class);
        loadActions(config.getConfigurationSection("actions"));
    }
    
    /**
     * Load actions from the configuration.
     * 
     * @param actionsSection The actions configuration section
     */
    private void loadActions(ConfigurationSection actionsSection) {
        if (actionsSection == null) return;
        
        for (String actionTypeStr : actionsSection.getKeys(false)) {
            ActionType actionType = ActionType.fromString(actionTypeStr);
            if (actionType == null) continue;
            
            ConfigurationSection actionSection = actionsSection.getConfigurationSection(actionTypeStr);
            if (actionSection == null) continue;
            
            List<JobAction> jobActions = new ArrayList<>();
            
            // Handle both single action and multiple actions
            if (actionSection.contains("target")) {
                // Single action
                jobActions.add(new JobAction(actionSection));
            } else {
                // Multiple actions
                for (String actionKey : actionSection.getKeys(false)) {
                    ConfigurationSection singleActionSection = actionSection.getConfigurationSection(actionKey);
                    if (singleActionSection != null) {
                        jobActions.add(new JobAction(singleActionSection));
                    }
                }
            }
            
            this.actions.put(actionType, jobActions);
        }
    }
    
    /**
     * Get the unique identifier of this job.
     * 
     * @return The job ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the display name of this job.
     * 
     * @return The job name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the description of this job.
     * 
     * @return The job description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the lore lines for this job.
     * 
     * @return List of lore strings
     */
    public List<String> getLore() {
        return new ArrayList<>(lore);
    }
    
    /**
     * Get the permission required to join this job.
     * 
     * @return The permission string
     */
    public String getPermission() {
        return permission;
    }
    
    /**
     * Get the maximum level for this job.
     * 
     * @return The max level
     */
    public int getMaxLevel() {
        return maxLevel;
    }
    
    /**
     * Get the icon material for this job.
     * 
     * @return The icon material name
     */
    public String getIconMaterial() {
        return iconMaterial;
    }
    
    /**
     * Get the custom model data for this job's icon.
     * 
     * @return The custom model data value, or 0 if not set
     */
    public int getCustomModelData() {
        return customModelData;
    }
    
    /**
     * Get the icon material for this job (backward compatibility).
     * 
     * @deprecated Use {@link #getIconMaterial()} instead
     * @return The icon material name
     */
    @Deprecated
    public String getIcon() {
        return iconMaterial;
    }
    
    /**
     * Check if this job is enabled.
     * 
     * @return true if enabled and XP curve is working
     */
    public boolean isEnabled() {
        return enabled && !xpCurveError;
    }
    
    /**
     * Check if this job is enabled in configuration.
     * 
     * @return true if enabled in config (ignoring XP curve errors)
     */
    public boolean isEnabledInConfig() {
        return enabled;
    }
    
    /**
     * Get all actions for a specific action type.
     * 
     * @param actionType The action type
     * @return List of job actions, or empty list if none
     */
    public List<JobAction> getActions(ActionType actionType) {
        return actions.getOrDefault(actionType, new ArrayList<>());
    }
    
    /**
     * Get all action types configured for this job.
     * 
     * @return Set of action types
     */
    public Set<ActionType> getActionTypes() {
        return actions.keySet();
    }
    
    /**
     * Get all actions for this job.
     * 
     * @return Map of all actions grouped by action type
     */
    public Map<ActionType, List<JobAction>> getActions() {
        return new HashMap<>(actions);
    }
    
    /**
     * Check if this job has any actions for the given action type.
     * 
     * @param actionType The action type to check
     * @return true if actions exist
     */
    public boolean hasActions(ActionType actionType) {
        List<JobAction> jobActions = actions.get(actionType);
        return jobActions != null && !jobActions.isEmpty();
    }
    
    /**
     * Get the XP type for this job (CURVE or EQUATION).
     * 
     * @return The XP type
     */
    public String getXpType() {
        return xpType;
    }
    
    /**
     * Get the XP value for this job (curve name or equation).
     * 
     * @return The XP value
     */
    public String getXpValue() {
        return xpValue;
    }
    
    /**
     * Get the XP curve name for this job (for backward compatibility).
     * 
     * @return The XP curve name or null if using equation
     */
    public String getXpCurveName() {
        return XP_TYPE_CURVE.equals(xpType) ? xpValue : null;
    }
    
    /**
     * Get the XP equation for this job (for backward compatibility).
     * 
     * @return The XP equation or null if using curve file
     */
    public String getXpEquation() {
        return "EQUATION".equals(xpType) ? xpValue : null;
    }
    
    /**
     * Get the XP curve for this job.
     * 
     * @return The XP curve
     */
    public XpCurve getXpCurve() {
        return xpCurve;
    }
    
    /**
     * Set the XP curve for this job.
     * This is called by JobManager during initialization.
     * 
     * @param xpCurve The XP curve
     */
    public void setXpCurve(XpCurve xpCurve) {
        this.xpCurve = xpCurve;
    }
    
    /**
     * Check if this job has a custom XP curve.
     * 
     * @return true if using custom curve or equation
     */
    public boolean hasCustomXpCurve() {
        return xpValue != null;
    }
    
    /**
     * Check if there's an XP curve error.
     * 
     * @return true if XP curve has an error
     */
    public boolean hasXpCurveError() {
        return xpCurveError;
    }
    
    /**
     * Get the XP curve error message.
     * 
     * @return The error message or null
     */
    public String getXpCurveErrorMessage() {
        return xpCurveErrorMessage;
    }
    
    /**
     * Set XP curve error status.
     * 
     * @param error true if there's an error
     * @param message The error message
     */
    public void setXpCurveError(boolean error, String message) {
        this.xpCurveError = error;
        this.xpCurveErrorMessage = message;
    }
    
    /**
     * Get the XP message settings for this job.
     * 
     * @return The XP message settings
     */
    public XpMessageSettings getXpMessageSettings() {
        return xpMessageSettings;
    }
    
    /**
     * Get the GUI reward configuration name for this job.
     * 
     * @return The GUI reward configuration name, or null if not set
     */
    public String getGuiReward() {
        return guiReward;
    }
    
    /**
     * Get the rewards file name for this job.
     * 
     * @return The rewards file name, or null if not set
     */
    public String getRewardsFile() {
        return rewardsFile;
    }
    
    /**
     * Get the action type for a specific job action.
     * This is used to determine which action type contains the given action.
     * 
     * @param jobAction The job action to find
     * @return The action type, or null if not found
     */
    public ActionType getActionTypeForAction(JobAction jobAction) {
        for (Map.Entry<ActionType, List<JobAction>> entry : actions.entrySet()) {
            if (entry.getValue().contains(jobAction)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Get the display name of this job (alias for getName()).
     * 
     * @return The job display name
     */
    public String getDisplayName() {
        return name;
    }
    
    /**
     * Get the configuration section for this job.
     * 
     * @return The configuration section
     */
    public ConfigurationSection getConfig() {
        return config;
    }
    
    /**
     * Check if auto-restore is enabled for action limits.
     * 
     * @return true if auto-restore is enabled
     */
    public boolean isAutoRestoreEnabled() {
        return config.getBoolean("action-limits.auto-restore.enabled", false);
    }
    
    /**
     * Get the auto-restore time for action limits.
     * 
     * @return The restore time in HH:mm format, or null if not set
     */
    public String getAutoRestoreTime() {
        if (!isAutoRestoreEnabled()) {
            return null;
        }
        return config.getString("action-limits.auto-restore.time", "00:00");
    }
    
    @Override
    public String toString() {
        return "Job{id='" + id + "', name='" + name + "', enabled=" + enabled + 
               ", xpType='" + xpType + "', xpValue='" + (xpValue != null ? xpValue : "default") + "'}";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Job job = (Job) obj;
        return Objects.equals(id, job.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}