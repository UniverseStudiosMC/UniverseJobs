package fr.ax_dev.universejobs;

import fr.ax_dev.universejobs.action.ActionProcessor;
import fr.ax_dev.universejobs.action.ActionLimitManager;
import fr.ax_dev.universejobs.bonus.XpBonusManager;
import fr.ax_dev.universejobs.bonus.MoneyBonusManager;
import fr.ax_dev.universejobs.command.JobCommand;
import fr.ax_dev.universejobs.compatibility.FoliaCompatibilityManager;
import fr.ax_dev.universejobs.config.ConfigManager;
import fr.ax_dev.universejobs.config.LanguageManager;
import fr.ax_dev.universejobs.integration.MythicMobsHandler;
import fr.ax_dev.universejobs.job.JobManager;
import fr.ax_dev.universejobs.levelup.SimpleLevelUpActionManager;
import fr.ax_dev.universejobs.listener.JobActionListener;
import fr.ax_dev.universejobs.listener.NexoEventListener;
import fr.ax_dev.universejobs.listener.ItemsAdderEventListener;
import fr.ax_dev.universejobs.listener.CustomCropsEventListener;
import fr.ax_dev.universejobs.listener.CustomFishingEventListener;
import fr.ax_dev.universejobs.listener.EnchantEventListener;
import fr.ax_dev.universejobs.protection.BlockProtectionManager;
import fr.ax_dev.universejobs.reward.RewardManager;
import fr.ax_dev.universejobs.reward.gui.RewardGuiManager;
import fr.ax_dev.universejobs.menu.MenuManager;
import fr.ax_dev.universejobs.utils.AsyncXpMessageSender;
import fr.ax_dev.universejobs.placeholder.PlaceholderManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

public final class UniverseJobs extends JavaPlugin implements Listener {
    
    private static volatile UniverseJobs instance;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private FoliaCompatibilityManager foliaManager;
    private JobManager jobManager;
    private SimpleLevelUpActionManager levelUpActionManager;
    private ActionProcessor actionProcessor;
    private ActionLimitManager limitManager;
    private XpBonusManager bonusManager;
    private MoneyBonusManager moneyBonusManager;
    private AsyncXpMessageSender messageSender;
    private BlockProtectionManager protectionManager;
    private RewardManager rewardManager;
    private RewardGuiManager rewardGuiManager;
    private MenuManager menuManager;
    private PlaceholderManager placeholderManager;
    private MythicMobsHandler mythicMobsHandler;
    private BukkitTask saveTask;
    private long startTime;

    @Override
    public void onEnable() {
        setInstance(this);
        startTime = System.currentTimeMillis();
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.languageManager = new LanguageManager(this);
        this.foliaManager = new FoliaCompatibilityManager(this);
        this.jobManager = new JobManager(this);
        this.levelUpActionManager = new SimpleLevelUpActionManager(this);
        this.limitManager = new ActionLimitManager(this);
        this.bonusManager = new XpBonusManager(this);
        this.moneyBonusManager = new MoneyBonusManager(this);
        this.messageSender = new AsyncXpMessageSender(this);
        this.protectionManager = new BlockProtectionManager(this);
        this.rewardManager = new RewardManager(this);
        this.rewardGuiManager = new RewardGuiManager(this, rewardManager);
        this.menuManager = new MenuManager(this);
        this.placeholderManager = new PlaceholderManager(this);
        this.mythicMobsHandler = new MythicMobsHandler(this);
        this.actionProcessor = new ActionProcessor(this, jobManager, bonusManager, moneyBonusManager, messageSender, limitManager);
        
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
        getServer().getPluginManager().registerEvents(new JobActionListener(this, actionProcessor, protectionManager, mythicMobsHandler), this);
        getServer().getPluginManager().registerEvents(new EnchantEventListener(this, actionProcessor), this);
        getServer().getPluginManager().registerEvents(this, this);
        
        // Register MythicMobs event listener if available
        if (mythicMobsHandler.isAvailable()) {
            getServer().getPluginManager().registerEvents(mythicMobsHandler, this);
        }
        
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
            getLogger().info("CustomFishing plugin detected - registering event listener");
            getServer().getPluginManager().registerEvents(
                new CustomFishingEventListener(this, actionProcessor), 
                this
            );
            getLogger().info("CustomFishing event listener registered successfully");
        } else {
            getLogger().info("CustomFishing plugin not found - skipping CustomFishing integration");
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
        getLogger().info("Shutting down UniverseJobs plugin...");
        
        try {
            stopSaveTask();
            savePlayerData();
            shutdownManagers();
            getLogger().info("UniverseJobs plugin shutdown completed successfully");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Critical error during plugin shutdown", e);
        } finally {
            setInstance(null);
        }
    }
    
    /**
     * Stop the periodic save task.
     */
    private void stopSaveTask() {
        if (saveTask != null && !saveTask.isCancelled()) {
            saveTask.cancel();
            saveTask = null;
        }
    }
    
    /**
     * Save all player data before shutdown.
     */
    private void savePlayerData() {
        if (jobManager != null) {
            getLogger().info("Saving all player data...");
            jobManager.saveAllPlayerData();
        }
    }
    
    /**
     * Shutdown all managers in proper order.
     */
    private void shutdownManagers() {
        getLogger().info("Shutting down managers...");
        
        shutdownRewardGuiManager();
        shutdownManagerSafely("menu manager", menuManager, () -> menuManager.closeAllMenus());
        shutdownManagerSafely("reward manager", rewardManager, () -> rewardManager.shutdown());
        shutdownManagerSafely("message sender", messageSender, () -> messageSender.shutdown());
        shutdownManagerSafely("bonus manager", bonusManager, () -> bonusManager.shutdown());
        shutdownManagerSafely("money bonus manager", moneyBonusManager, () -> moneyBonusManager.shutdown());
        shutdownManagerSafely("placeholder manager", placeholderManager, () -> placeholderManager.shutdown());
        shutdownManagerSafely("job manager", jobManager, () -> jobManager.shutdown());
        shutdownManagerSafely("action limit manager", limitManager, () -> limitManager.clearAllLimits());
        shutdownManagerSafely("Folia manager", foliaManager, () -> foliaManager.cancelAllTasks());
        
        // Clear references for managers without explicit shutdown
        protectionManager = null;
        actionProcessor = null;
        languageManager = null;
        configManager = null;
    }
    
    /**
     * Shutdown reward GUI manager and close all open GUIs.
     */
    private void shutdownRewardGuiManager() {
        if (rewardGuiManager != null) {
            try {
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
    }
    
    /**
     * Safely shutdown a manager with error handling.
     */
    private void shutdownManagerSafely(String managerName, Object manager, Runnable shutdownAction) {
        if (manager != null) {
            try {
                shutdownAction.run();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error shutting down " + managerName, e);
            }
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
     * Get the money bonus manager.
     * 
     * @return The money bonus manager
     */
    public MoneyBonusManager getMoneyBonusManager() {
        return moneyBonusManager;
    }
    
    /**
     * Get the XP message sender.
     * 
     * @return The XP message sender
     */
    public AsyncXpMessageSender getMessageSender() {
        return messageSender;
    }
    
    /**
     * Get the XP message sender (legacy method name for compatibility).
     * 
     * @return The XP message sender
     */
    public AsyncXpMessageSender getXpMessageSender() {
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
     * Get the menu manager.
     * 
     * @return The menu manager
     */
    public MenuManager getMenuManager() {
        return menuManager;
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
     * Get the MythicMobs integration handler.
     * 
     * @return The MythicMobs handler
     */
    public MythicMobsHandler getMythicMobsHandler() {
        return mythicMobsHandler;
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
    public static synchronized UniverseJobs getInstance() {
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
     * Get the performance manager. This feature has been removed as it was not needed.
     * 
     * @return Always null (feature removed)
     */
    public Object getPerformanceManager() {
        return null; // Performance manager feature removed
    }
    
    /**
     * Set the plugin instance (thread-safe).
     * 
     * @param newInstance The new instance
     */
    private static synchronized void setInstance(UniverseJobs newInstance) {
        instance = newInstance;
    }
}
