package fr.ax_dev.jobsAdventure.reward;

import fr.ax_dev.jobsAdventure.condition.ConditionGroup;
import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a reward that can be claimed by players through the GUI system.
 * Contains all reward data including requirements, items, commands, and metadata.
 */
public class Reward {
    
    private final String id;
    private final String name;
    private final String description;
    private final List<String> lore;
    private final String jobId;
    private final int requiredLevel;
    private final boolean repeatable;
    private final long cooldownHours;
    private final ConditionGroup requirements;
    private final List<RewardItem> items;
    private final List<String> commands;
    private final double economyReward;
    private final String permission;
    private final int guiSlot;
    private final boolean enabled;
    
    /**
     * Create a new Reward from configuration.
     * 
     * @param id The unique identifier for this reward
     * @param jobId The job this reward belongs to
     * @param config The configuration section containing reward data
     */
    public Reward(String id, String jobId, ConfigurationSection config) {
        this.id = id;
        this.jobId = jobId;
        this.name = config.getString("name", id);
        this.description = config.getString("description", "");
        this.lore = config.getStringList("lore");
        this.requiredLevel = config.getInt("required-level", 1);
        this.repeatable = config.getBoolean("repeatable", false);
        this.cooldownHours = config.getLong("cooldown-hours", 0);
        this.economyReward = config.getDouble("economy-reward", 0.0);
        this.permission = config.getString("permission");
        this.guiSlot = config.getInt("gui-slot", -1);
        this.enabled = config.getBoolean("enabled", true);
        
        // Load requirements/conditions
        ConfigurationSection reqSection = config.getConfigurationSection("requirements");
        this.requirements = reqSection != null ? new ConditionGroup(reqSection) : null;
        
        // Load reward items
        this.items = new ArrayList<>();
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                if (itemSection != null) {
                    this.items.add(new RewardItem(itemSection));
                }
            }
        }
        
        // Load commands
        this.commands = config.getStringList("commands");
    }
    
    /**
     * Get the unique identifier of this reward.
     * 
     * @return The reward ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the display name of this reward.
     * 
     * @return The reward name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the description of this reward.
     * 
     * @return The reward description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the lore lines for this reward.
     * 
     * @return List of lore strings
     */
    public List<String> getLore() {
        return new ArrayList<>(lore);
    }
    
    /**
     * Get the job ID this reward belongs to.
     * 
     * @return The job ID
     */
    public String getJobId() {
        return jobId;
    }
    
    /**
     * Get the required level to claim this reward.
     * 
     * @return The required level
     */
    public int getRequiredLevel() {
        return requiredLevel;
    }
    
    /**
     * Check if this reward is repeatable.
     * 
     * @return true if repeatable
     */
    public boolean isRepeatable() {
        return repeatable;
    }
    
    /**
     * Get the cooldown in hours between claims.
     * 
     * @return The cooldown in hours
     */
    public long getCooldownHours() {
        return cooldownHours;
    }
    
    /**
     * Get the requirements for claiming this reward.
     * 
     * @return The condition group or null if no requirements
     */
    public ConditionGroup getRequirements() {
        return requirements;
    }
    
    /**
     * Get the items rewarded when claiming.
     * 
     * @return List of reward items
     */
    public List<RewardItem> getItems() {
        return new ArrayList<>(items);
    }
    
    /**
     * Get the commands executed when claiming.
     * 
     * @return List of commands
     */
    public List<String> getCommands() {
        return new ArrayList<>(commands);
    }
    
    /**
     * Get the economy reward amount.
     * 
     * @return The economy reward
     */
    public double getEconomyReward() {
        return economyReward;
    }
    
    /**
     * Get the permission required to see this reward.
     * 
     * @return The permission or null if none required
     */
    public String getPermission() {
        return permission;
    }
    
    /**
     * Get the GUI slot for this reward.
     * 
     * @return The GUI slot or -1 for auto-assignment
     */
    public int getGuiSlot() {
        return guiSlot;
    }
    
    /**
     * Check if this reward is enabled.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Check if this reward has any items.
     * 
     * @return true if items exist
     */
    public boolean hasItems() {
        return !items.isEmpty();
    }
    
    /**
     * Check if this reward has any commands.
     * 
     * @return true if commands exist
     */
    public boolean hasCommands() {
        return !commands.isEmpty();
    }
    
    /**
     * Check if this reward has economy rewards.
     * 
     * @return true if economy reward > 0
     */
    public boolean hasEconomyReward() {
        return economyReward > 0;
    }
    
    /**
     * Check if this reward has requirements.
     * 
     * @return true if requirements exist
     */
    public boolean hasRequirements() {
        return requirements != null;
    }
    
    @Override
    public String toString() {
        return "Reward{id='" + id + "', name='" + name + "', job='" + jobId + 
               "', level=" + requiredLevel + ", enabled=" + enabled + "}";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Reward reward = (Reward) obj;
        return Objects.equals(id, reward.id) && Objects.equals(jobId, reward.jobId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, jobId);
    }
    
    /**
     * Represents an item reward within a reward.
     */
    public static class RewardItem {
        private final String material;
        private final int amount;
        private final String displayName;
        private final List<String> lore;
        private final int customModelData;
        private final String nexoId;
        private final String itemsAdderId;
        
        /**
         * Create a new RewardItem from configuration.
         * 
         * @param config The configuration section
         */
        public RewardItem(ConfigurationSection config) {
            this.material = config.getString("material", "STONE");
            this.amount = config.getInt("amount", 1);
            this.displayName = config.getString("display-name");
            this.lore = config.getStringList("lore");
            this.customModelData = config.getInt("custom-model-data", -1);
            this.nexoId = config.getString("nexo-id");
            this.itemsAdderId = config.getString("itemsadder-id");
        }
        
        public String getMaterial() { return material; }
        public int getAmount() { return amount; }
        public String getDisplayName() { return displayName; }
        public List<String> getLore() { return new ArrayList<>(lore); }
        public int getCustomModelData() { return customModelData; }
        public String getNexoId() { return nexoId; }
        public String getItemsAdderId() { return itemsAdderId; }
        
        public boolean hasCustomModelData() { return customModelData != -1; }
        public boolean isNexoItem() { return nexoId != null && !nexoId.isEmpty(); }
        public boolean isItemsAdderItem() { return itemsAdderId != null && !itemsAdderId.isEmpty(); }
    }
}