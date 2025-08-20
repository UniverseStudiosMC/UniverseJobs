package fr.ax_dev.jobsAdventure;

import fr.ax_dev.jobsAdventure.action.ActionProcessor;
import fr.ax_dev.jobsAdventure.action.ActionLimitManager;
import fr.ax_dev.jobsAdventure.bonus.XpBonusManager;
import fr.ax_dev.jobsAdventure.command.JobCommand;
import fr.ax_dev.jobsAdventure.compatibility.FoliaCompatibilityManager;
import fr.ax_dev.jobsAdventure.config.ConfigManager;
import fr.ax_dev.jobsAdventure.config.LanguageManager;
import fr.ax_dev.jobsAdventure.job.JobManager;
import fr.ax_dev.jobsAdventure.levelup.SimpleLevelUpActionManager;
import fr.ax_dev.jobsAdventure.listener.JobActionListener;
import fr.ax_dev.jobsAdventure.listener.NexoEventListener;
import fr.ax_dev.jobsAdventure.listener.ItemsAdderEventListener;
import fr.ax_dev.jobsAdventure.listener.CustomCropsEventListener;
import fr.ax_dev.jobsAdventure.listener.CustomFishingEventListener;
import fr.ax_dev.jobsAdventure.protection.BlockProtectionManager;
import fr.ax_dev.jobsAdventure.reward.RewardManager;
import fr.ax_dev.jobsAdventure.reward.gui.RewardGuiManager;
import fr.ax_dev.jobsAdventure.utils.XpMessageSender;
import fr.ax_dev.jobsAdventure.placeholder.PlaceholderManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

public final class JobsAdventure extends JavaPlugin implements Listener {
    
    private static JobsAdventure instance;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private FoliaCompatibilityManager foliaManager;
    private JobManager jobManager;
    private SimpleLevelUpActionManager levelUpActionManager;
    private ActionProcessor actionProcessor;
    private ActionLimitManager limitManager;
    private XpBonusManager bonusManager;
    private XpMessageSender messageSender;
    private BlockProtectionManager protectionManager;
    private RewardManager rewardManager;
    private RewardGuiManager rewardGuiManager;
    private PlaceholderManager placeholderManager;
    private BukkitTask saveTask;
    private long startTime;

    @Override
    public void onEnable() {
        instance = this;
        startTime = System.currentTimeMillis();
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.languageManager = new LanguageManager(this);
        this.foliaManager = new FoliaCompatibilityManager(this);
        this.jobManager = new JobManager(this);
        this.levelUpActionManager = new SimpleLevelUpActionManager(this);
        this.limitManager = new ActionLimitManager(this);
        this.bonusManager = new XpBonusManager(this);
        this.messageSender = new XpMessageSender(this);
        this.protectionManager = new BlockProtectionManager(this);
        this.rewardManager = new RewardManager(this);
        this.rewardGuiManager = new RewardGuiManager(this, rewardManager);
        this.placeholderManager = new PlaceholderManager(this);
        this.actionProcessor = new ActionProcessor(this, jobManager, bonusManager, messageSender, limitManager);
        
        // Load configuration
        try {
            configManager.loadConfig();
            // Configuration loaded successfully
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load configuration", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Load jobs
        try {
            jobManager.loadJobs();
            // Jobs loaded successfully
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load jobs", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Load level up actions
        try {
            levelUpActionManager.loadJobActions();
            // Level up actions loaded successfully
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load level up actions", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize reward system
        try {
            rewardManager.initialize();
            // Reward system initialized successfully
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize reward system", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register commands
        JobCommand jobCommand = new JobCommand(this, jobManager);
        getCommand("jobs").setExecutor(jobCommand);
        getCommand("jobs").setTabCompleter(jobCommand);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new JobActionListener(this, actionProcessor, protectionManager), this);
        getServer().getPluginManager().registerEvents(this, this);
        
        // Register Nexo event listener if Nexo is present
        if (getServer().getPluginManager().isPluginEnabled("Nexo")) {
            getServer().getPluginManager().registerEvents(
                new NexoEventListener(this, actionProcessor, protectionManager), 
                this
            );
            // Nexo event listener registered
        }
        
        // Register ItemsAdder event listener if ItemsAdder is present
        if (getServer().getPluginManager().isPluginEnabled("ItemsAdder")) {
            getServer().getPluginManager().registerEvents(
                new ItemsAdderEventListener(this, actionProcessor, protectionManager), 
                this
            );
            // ItemsAdder event listener registered
        }
        
        // Register CustomCrops event listener if CustomCrops is present
        if (getServer().getPluginManager().isPluginEnabled("CustomCrops")) {
            getServer().getPluginManager().registerEvents(
                new CustomCropsEventListener(this, actionProcessor, protectionManager), 
                this
            );
            // CustomCrops event listener registered
        }
        
        // Register CustomFishing event listener if CustomFishing is present
        if (getServer().getPluginManager().isPluginEnabled("CustomFishing")) {
            getServer().getPluginManager().registerEvents(
                new CustomFishingEventListener(this, actionProcessor), 
                this
            );
            // CustomFishing event listener registered
        }
        
        // Load player data for online players
        for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
            foliaManager.runAsync(() -> {
                jobManager.loadPlayerData(player);
                rewardManager.loadPlayerData(player);
            });
        }
        
        // Initialize PlaceholderAPI integration
        try {
            placeholderManager.initialize();
            // PlaceholderAPI integration initialized
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to initialize PlaceholderAPI integration", e);
        }
        
        // Start periodic save task
        startSaveTask();
        
        // Check for optional dependencies
        checkDependencies();
        
        // Plugin enabled successfully
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down JobsAdventure plugin...");
        
        try {
            // Cancel save task first to prevent conflicts
            if (saveTask != null && !saveTask.isCancelled()) {
                saveTask.cancel();
                saveTask = null;
            }
            
            // Save all player data before shutting down managers
            if (jobManager != null) {
                getLogger().info("Saving all player data...");
                jobManager.saveAllPlayerData();
            }
            
            // Shutdown managers in proper order
            getLogger().info("Shutting down managers...");
            
            // Shutdown reward GUI manager first to close any open GUIs
            
            if (rewardGuiManager != null) {
                try {
                    // Close all open GUIs
                    for (Player player : getServer().getOnlinePlayers()) {
                        if (player.getOpenInventory() != null) {
                            player.closeInventory();
                        }
                    }
                    rewardGuiManager = null;
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error shutting down reward GUI manager", e);
                }
            }
            
            // Shutdown reward manager
            if (rewardManager != null) {
                try {
                    rewardManager.shutdown();
                    rewardManager = null;
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error shutting down reward manager", e);
                }
            }
            
            // Shutdown XP message sender
            if (messageSender != null) {
                try {
                    messageSender.cleanup();
                    messageSender = null;
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error shutting down message sender", e);
                }
            }
            
            // Shutdown bonus manager
            if (bonusManager != null) {
                try {
                    bonusManager.shutdown();
                    bonusManager = null;
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error shutting down bonus manager", e);
                }
            }
            
            // Shutdown placeholder manager
            if (placeholderManager != null) {
                try {
                    placeholderManager.shutdown();
                    placeholderManager = null;
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error shutting down placeholder manager", e);
                }
            }
            
            // Shutdown protection manager
            if (protectionManager != null) {
                try {
                    // Protection manager doesn't have explicit shutdown, just clear reference
                    protectionManager = null;
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error cleaning up protection manager", e);
                }
            }
            
            // Shutdown job manager (this should be done after other managers)
            if (jobManager != null) {
                try {
                    jobManager.shutdown();
                    jobManager = null;
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error shutting down job manager", e);
                }
            }
            
            // Shutdown action limit manager
            if (limitManager != null) {
                try {
                    limitManager.clearAllLimits();
                    limitManager = null;
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error shutting down action limit manager", e);
                }
            }
            
            // Shutdown action processor
            if (actionProcessor != null) {
                try {
                    // Action processor doesn't have explicit shutdown, just clear reference
                    actionProcessor = null;
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error cleaning up action processor", e);
                }
            }
            
            // Cancel all Folia tasks
            if (foliaManager != null) {
                try {
                    foliaManager.cancelAllTasks();
                    foliaManager = null;
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error shutting down Folia manager", e);
                }
            }
            
            // Shutdown config managers
            if (languageManager != null) {
                try {
                    // Language manager doesn't have explicit shutdown, just clear reference
                    languageManager = null;
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error cleaning up language manager", e);
                }
            }
            
            if (configManager != null) {
                try {
                    // Config manager doesn't have explicit shutdown, just clear reference
                    configManager = null;
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error cleaning up config manager", e);
                }
            }
            
            getLogger().info("JobsAdventure plugin shutdown completed successfully");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Critical error during plugin shutdown", e);
        } finally {
            // Clear the static instance last
            instance = null;
        }
    }
    
    /**
     * Start the periodic save task.
     */
    private void startSaveTask() {
        int saveInterval = configManager.getSaveInterval();
        if (saveInterval > 0) {
            foliaManager.runTimerAsync(() -> {
                if (configManager.isDebugEnabled()) {
                    getLogger().info("Auto-saving player data...");
                }
                jobManager.saveAllPlayerData();
            }, saveInterval * 20L, saveInterval * 20L);
            
            // Auto-save task started
        }
    }
    
    /**
     * Check for optional dependencies and log their status.
     */
    private void checkDependencies() {
        StringBuilder dependencyStatus = new StringBuilder("Dependency status: ");
        
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            dependencyStatus.append("PlaceholderAPI ✓ ");
        } else {
            dependencyStatus.append("PlaceholderAPI ✗ ");
        }
        
        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            dependencyStatus.append("MythicMobs ✓ ");
        } else {
            dependencyStatus.append("MythicMobs ✗ ");
        }
        
        if (getServer().getPluginManager().isPluginEnabled("MMOItems")) {
            dependencyStatus.append("MMOItems ✓ ");
        } else {
            dependencyStatus.append("MMOItems ✗ ");
        }
        
        if (getServer().getPluginManager().isPluginEnabled("Nexo")) {
            dependencyStatus.append("Nexo ✓ ");
        } else {
            dependencyStatus.append("Nexo ✗ ");
        }
        
        if (getServer().getPluginManager().isPluginEnabled("ItemsAdder")) {
            dependencyStatus.append("ItemsAdder ✓ ");
        } else {
            dependencyStatus.append("ItemsAdder ✗ ");
        }
        
        if (getServer().getPluginManager().isPluginEnabled("CustomCrops")) {
            dependencyStatus.append("CustomCrops ✓ ");
        } else {
            dependencyStatus.append("CustomCrops ✗ ");
        }
        
        if (getServer().getPluginManager().isPluginEnabled("CustomFishing")) {
            dependencyStatus.append("CustomFishing ✓");
        } else {
            dependencyStatus.append("CustomFishing ✗");
        }
        
        // Dependencies checked
    }
    
    /**
     * Handle player join events.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load player data asynchronously
        foliaManager.runAsync(() -> {
            jobManager.loadPlayerData(event.getPlayer());
            rewardManager.loadPlayerData(event.getPlayer());
            if (configManager.isDebugEnabled()) {
                getLogger().info("Loaded data for player: " + event.getPlayer().getName());
            }
        });
    }
    
    /**
     * Handle player quit events.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up XP message sender resources
        if (messageSender != null) {
            messageSender.cleanupPlayer(event.getPlayer());
        }
        
        // Save player data asynchronously
        foliaManager.runAsync(() -> {
            jobManager.savePlayerData(event.getPlayer());
            rewardManager.unloadPlayerData(event.getPlayer());
            if (configManager.isDebugEnabled()) {
                getLogger().info("Saved data for player: " + event.getPlayer().getName());
            }
        });
    }
    
    /**
     * Get the configuration manager.
     * 
     * @return The config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Get the language manager.
     * 
     * @return The language manager
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    /**
     * Get the Folia compatibility manager.
     * 
     * @return The Folia compatibility manager
     */
    public FoliaCompatibilityManager getFoliaManager() {
        return foliaManager;
    }
    
    /**
     * Get the job manager.
     * 
     * @return The job manager
     */
    public JobManager getJobManager() {
        return jobManager;
    }
    
    /**
     * Get the action processor.
     * 
     * @return The action processor
     */
    public ActionProcessor getActionProcessor() {
        return actionProcessor;
    }
    
    /**
     * Get the XP bonus manager.
     * 
     * @return The XP bonus manager
     */
    public XpBonusManager getBonusManager() {
        return bonusManager;
    }
    
    /**
     * Get the XP message sender.
     * 
     * @return The XP message sender
     */
    public XpMessageSender getMessageSender() {
        return messageSender;
    }
    
    /**
     * Get the block protection manager.
     * 
     * @return The block protection manager
     */
    public BlockProtectionManager getProtectionManager() {
        return protectionManager;
    }
    
    /**
     * Get the reward manager.
     * 
     * @return The reward manager
     */
    public RewardManager getRewardManager() {
        return rewardManager;
    }
    
    /**
     * Get the reward GUI manager.
     * 
     * @return The reward GUI manager
     */
    public RewardGuiManager getRewardGuiManager() {
        return rewardGuiManager;
    }
    
    /**
     * Get the placeholder manager.
     * 
     * @return The placeholder manager
     */
    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }
    
    /**
     * Get the action limit manager.
     * 
     * @return The action limit manager
     */
    public ActionLimitManager getLimitManager() {
        return limitManager;
    }
    
    /**
     * Get the level up action manager.
     * 
     * @return The level up action manager
     */
    public SimpleLevelUpActionManager getLevelUpActionManager() {
        return levelUpActionManager;
    }
    
    /**
     * Get the plugin instance.
     * 
     * @return The plugin instance
     */
    public static JobsAdventure getInstance() {
        return instance;
    }
    
    /**
     * Get the plugin start time.
     * 
     * @return The start time in milliseconds
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * Get the performance manager. This is a placeholder implementation.
     * 
     * @return null for now (not implemented yet)
     */
    public Object getPerformanceManager() {
        return null; // TODO: Implement performance manager
    }
}
