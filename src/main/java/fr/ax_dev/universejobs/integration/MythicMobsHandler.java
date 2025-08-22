package fr.ax_dev.universejobs.integration;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.condition.ConditionContext;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.api.mobs.MythicMob;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Handles MythicMobs integration.
 */
public class MythicMobsHandler implements Listener {
    
    private final UniverseJobs plugin;
    private final boolean isAvailable;
    
    /**
     * Create a new MythicMobsHandler.
     * 
     * @param plugin The plugin instance
     */
    public MythicMobsHandler(UniverseJobs plugin) {
        this.plugin = plugin;
        this.isAvailable = checkMythicMobsAvailability();
        
        if (isAvailable) {
            plugin.getLogger().info("MythicMobs integration initialized successfully");
        } else {
            plugin.getLogger().info("MythicMobs not available or incompatible version");
        }
    }
    
    /**
     * Check if MythicMobs is available and compatible.
     * 
     * @return true if MythicMobs is available
     */
    private boolean checkMythicMobsAvailability() {
        try {
            Plugin mythicPlugin = plugin.getServer().getPluginManager().getPlugin("MythicMobs");
            if (mythicPlugin == null || !mythicPlugin.isEnabled()) {
                return false;
            }
            
            // Test API access
            if (MythicBukkit.inst().getMobManager() == null) {
                return false;
            }
            
            plugin.getLogger().info("MythicMobs detected - Version: " + mythicPlugin.getDescription().getVersion());
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize MythicMobs integration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if MythicMobs integration is available.
     * 
     * @return true if available
     */
    public boolean isAvailable() {
        return isAvailable;
    }
    
    /**
     * Check if an entity is a MythicMob and populate context with MythicMob data.
     * 
     * @param entity The entity to check
     * @param context The context to populate
     * @return true if the entity is a MythicMob
     */
    public boolean populateMythicMobContext(Entity entity, ConditionContext context) {
        if (!isAvailable || entity == null || context == null) {
            return false;
        }
        
        try {
            java.util.Optional<ActiveMob> optionalActiveMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId());
            if (optionalActiveMob.isPresent()) {
                ActiveMob activeMob = optionalActiveMob.get();
                MythicMob mythicMob = activeMob.getType();
                String internalName = mythicMob.getInternalName();
                
                // Populate context with MythicMob information
                context.set("target", "MYTHICMOB:" + internalName);
                context.set("mythicmob", true);
                context.set("mythicmob_type", internalName);
                context.set("mythicmob_display_name", mythicMob.getDisplayName().get());
                context.set("mythicmob_level", activeMob.getLevel());
                
                // Add additional MythicMob properties if available
                if (activeMob.getEntity() instanceof LivingEntity livingEntity) {
                    context.set("mythicmob_health", livingEntity.getHealth());
                    context.set("mythicmob_max_health", livingEntity.getMaxHealth());
                }
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("MythicMob detected: " + internalName + " (Level " + activeMob.getLevel() + ")");
                }
                
                return true;
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Failed to check MythicMob data for entity: " + e.getMessage());
            }
        }
        
        return false;
    }
    
    /**
     * Get the MythicMob internal name for an entity.
     * 
     * @param entity The entity to check
     * @return The internal name or null if not a MythicMob
     */
    public String getMythicMobInternalName(Entity entity) {
        if (!isAvailable || entity == null) {
            return null;
        }
        
        try {
            java.util.Optional<ActiveMob> optionalActiveMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId());
            if (optionalActiveMob.isPresent()) {
                return optionalActiveMob.get().getType().getInternalName();
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Failed to get MythicMob internal name: " + e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * Check if an entity is a MythicMob.
     * 
     * @param entity The entity to check
     * @return true if it's a MythicMob
     */
    public boolean isMythicMob(Entity entity) {
        if (!isAvailable || entity == null) {
            return false;
        }
        
        try {
            java.util.Optional<ActiveMob> optionalActiveMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId());
            return optionalActiveMob.isPresent();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Handle MythicMob spawn events.
     * This can be used for additional job-related logic when MythicMobs spawn.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        if (!isAvailable) {
            return;
        }
        
        try {
            ActiveMob activeMob = event.getMob();
            if (activeMob != null) {
                String internalName = activeMob.getType().getInternalName();
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("MythicMob spawned: " + internalName + 
                        " at " + activeMob.getEntity().getLocation());
                }
                
                // Future: Add spawn-based job rewards or tracking here
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling MythicMob spawn event: " + e.getMessage());
        }
    }
    
    /**
     * Handle MythicMob death events.
     * This provides additional context beyond the standard EntityDeathEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        if (!isAvailable) {
            return;
        }
        
        try {
            ActiveMob activeMob = event.getMob();
            Player killer = null;
            
            // Try to get the killer
            if (activeMob.getEntity() instanceof LivingEntity livingEntity) {
                killer = livingEntity.getKiller();
            }
            
            if (killer != null && activeMob != null) {
                String internalName = activeMob.getType().getInternalName();
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("MythicMob killed: " + internalName + 
                        " by " + killer.getName() + 
                        " (Level " + activeMob.getLevel() + ")");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling MythicMob death event: " + e.getMessage());
        }
    }
    
    /**
     * Get performance and status information about MythicMobs integration.
     * 
     * @return Status information
     */
    public String getStatusInfo() {
        if (!isAvailable) {
            return "MythicMobs: Not available";
        }
        
        try {
            int activeMobs = MythicBukkit.inst().getMobManager().getActiveMobs().size();
            return String.format("MythicMobs: Active (%d mobs)", activeMobs);
        } catch (Exception e) {
            return "MythicMobs: Error - " + e.getMessage();
        }
    }
}