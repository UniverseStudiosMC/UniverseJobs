package fr.ax_dev.universejobs;

import fr.ax_dev.universejobs.action.ActionProcessor;
import fr.ax_dev.universejobs.action.ActionLimitManager;
import fr.ax_dev.universejobs.bonus.XpBonusManager;
import fr.ax_dev.universejobs.bonus.MoneyBonusManager;
import fr.ax_dev.universejobs.cache.ConfigurationCache;
import fr.ax_dev.universejobs.cache.PlayerJobCache;
import fr.ax_dev.universejobs.storage.DataStorage;
import fr.ax_dev.universejobs.storage.database.DatabaseDataStorage;
import fr.ax_dev.universejobs.storage.migration.DataMigrator;
import fr.ax_dev.universejobs.command.JobCommand;
import fr.ax_dev.universejobs.compatibility.FoliaCompatibilityManager;
import fr.ax_dev.universejobs.config.ConfigManager;
import fr.ax_dev.universejobs.config.LanguageManager;
import fr.ax_dev.universejobs.integration.MythicMobsHandler;
import fr.ax_dev.universejobs.job.JobManager;
import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.levelup.SimpleLevelUpActionManager;
import fr.ax_dev.universejobs.listener.JobActionListener;
import fr.ax_dev.universejobs.listener.EnchantEventListener;
import fr.ax_dev.universejobs.listener.BrewEventListener;
import fr.ax_dev.universejobs.listener.RepairEventListener;
import fr.ax_dev.universejobs.listener.ExploreEventListener;
import fr.ax_dev.universejobs.protection.BlockProtectionManager;
import fr.ax_dev.universejobs.reward.RewardManager;
import fr.ax_dev.universejobs.reward.gui.RewardGuiManager;
import fr.ax_dev.universejobs.rewards.BatchedRewardManager;
import fr.ax_dev.universejobs.menu.MenuManager;
import fr.ax_dev.universejobs.menu.BoostManagerGui;
import fr.ax_dev.universejobs.utils.AsyncXpMessageSender;
import fr.ax_dev.universejobs.placeholder.PlaceholderManager;
import fr.ax_dev.universejobs.update.UpdateChecker;
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
    private BatchedRewardManager batchedRewardManager;
    private MenuManager menuManager;
    private BoostManagerGui boostManagerGui;
    private PlaceholderManager placeholderManager;
    private MythicMobsHandler mythicMobsHandler;
    private BukkitTask saveTask;
    private BukkitTask dailyTask;
    private long startTime;
    private fr.ax_dev.universejobs.utils.PluginAccessor accessor;
    private UpdateChecker updateChecker;
    private fr.ax_dev.universejobs.job.InactivityDecayManager inactivityDecayManager;
    private ExploreEventListener exploreEventListener;
    
    // ========== STORAGE SYSTEM ==========
    private DataStorage dataStorage;
    private DataMigrator dataMigrator;
    
    // ========== ULTRA-FAST CACHE SYSTEM ==========
    private ConfigurationCache configCache;
    private PlayerJobCache playerCache;

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
        this.limitManager = new ActionLimitManager(this, configCache);
        this.bonusManager = new XpBonusManager(this);
        this.moneyBonusManager = new MoneyBonusManager(this);
        this.messageSender = new AsyncXpMessageSender(this);
        this.protectionManager = new BlockProtectionManager(this);
        this.menuManager = new MenuManager(this);
        this.boostManagerGui = new BoostManagerGui(this);
        this.placeholderManager = new PlaceholderManager(this);
        this.mythicMobsHandler = new MythicMobsHandler(this);
        
        // Load configuration first
        try {
            configManager.loadConfig();
            // Configuration loaded successfully
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load configuration", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize storage system
        try {
            initializeStorageSystem();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize storage system", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Load jobs second
        try {
            jobManager.loadJobs();
            // Jobs loaded successfully
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load jobs", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // ========== INITIALIZE ULTRA-FAST CACHE AFTER JOBS ARE LOADED ==========
        this.configCache = new ConfigurationCache(this);
        this.playerCache = new PlayerJobCache(this);
        
        try {
            configCache.loadAllConfigurations();
            playerCache.preloadOnlinePlayers();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize cache system", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize reward system after database is ready
        this.rewardManager = new RewardManager(this);
        this.rewardGuiManager = new RewardGuiManager(this, rewardManager);
        this.batchedRewardManager = new BatchedRewardManager(this);
        
        // Initialize action processor with loaded cache
        this.actionProcessor = new ActionProcessor(this, jobManager, bonusManager, moneyBonusManager, 
                                                 messageSender, limitManager, configCache, playerCache);
        
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
        
        JobCommand jobCommand = new JobCommand(this, jobManager);

        org.bukkit.command.PluginCommand jobsCommand = getCommand("jobs");
        if (jobsCommand != null) {
            jobsCommand.setExecutor(jobCommand);
            jobsCommand.setTabCompleter(jobCommand);
        }
        
        // Register event listeners avec cache ultra-rapide
        getServer().getPluginManager().registerEvents(
            new JobActionListener(this, actionProcessor, protectionManager, mythicMobsHandler, configCache, playerCache), this);
        getServer().getPluginManager().registerEvents(new EnchantEventListener(this, actionProcessor), this);
        getServer().getPluginManager().registerEvents(new BrewEventListener(this, actionProcessor), this);
        getServer().getPluginManager().registerEvents(new RepairEventListener(this, actionProcessor), this);
        this.exploreEventListener = new ExploreEventListener(this, actionProcessor);
        getServer().getPluginManager().registerEvents(exploreEventListener, this);
        getServer().getPluginManager().registerEvents(this, this);
        
        // Register MythicMobs event listener if available
        if (mythicMobsHandler.isAvailable()) {
            getServer().getPluginManager().registerEvents(mythicMobsHandler, this);
        }
        
        // Register Nexo event listener if Nexo is present
        if (getServer().getPluginManager().isPluginEnabled("Nexo")) {
            try {
                Class<?> nexoListenerClass = Class.forName("fr.ax_dev.universejobs.listener.NexoEventListener");
                Object nexoListener = nexoListenerClass
                    .getConstructor(UniverseJobs.class, ActionProcessor.class, BlockProtectionManager.class)
                    .newInstance(this, actionProcessor, protectionManager);
                getServer().getPluginManager().registerEvents((Listener) nexoListener, this);
                getLogger().info("Nexo event listener registered successfully");
            } catch (Exception e) {
                getLogger().warning("Failed to register Nexo event listener: " + e.getMessage());
            }
        }
        
        // Register ItemsAdder event listener if ItemsAdder is present
        if (getServer().getPluginManager().isPluginEnabled("ItemsAdder")) {
            try {
                Class<?> itemsAdderListenerClass = Class.forName("fr.ax_dev.universejobs.listener.ItemsAdderEventListener");
                Object itemsAdderListener = itemsAdderListenerClass
                    .getConstructor(UniverseJobs.class, ActionProcessor.class, BlockProtectionManager.class)
                    .newInstance(this, actionProcessor, protectionManager);
                getServer().getPluginManager().registerEvents((Listener) itemsAdderListener, this);
                getLogger().info("ItemsAdder event listener registered successfully");
            } catch (Exception e) {
                getLogger().warning("Failed to register ItemsAdder event listener: " + e.getMessage());
            }
        }
        
        // Register Oraxen event listener if Oraxen is present
        if (getServer().getPluginManager().isPluginEnabled("Oraxen")) {
            try {
                Class<?> oraxenListenerClass = Class.forName("fr.ax_dev.universejobs.listener.OraxenEventListener");
                Object oraxenListener = oraxenListenerClass
                    .getConstructor(UniverseJobs.class, ActionProcessor.class, BlockProtectionManager.class)
                    .newInstance(this, actionProcessor, protectionManager);
                getServer().getPluginManager().registerEvents((Listener) oraxenListener, this);
                getLogger().info("Oraxen event listener registered successfully");
            } catch (Exception e) {
                getLogger().warning("Failed to register Oraxen event listener: " + e.getMessage());
            }
        }
        
        // Register CustomCrops event listener if CustomCrops is present
        if (getServer().getPluginManager().isPluginEnabled("CustomCrops")) {
            try {
                Class<?> customCropsListenerClass = Class.forName("fr.ax_dev.universejobs.listener.CustomCropsEventListener");
                Object customCropsListener = customCropsListenerClass
                    .getConstructor(UniverseJobs.class, ActionProcessor.class, BlockProtectionManager.class)
                    .newInstance(this, actionProcessor, protectionManager);
                getServer().getPluginManager().registerEvents((Listener) customCropsListener, this);
                getLogger().info("CustomCrops event listener registered successfully");
            } catch (Exception e) {
                getLogger().warning("Failed to register CustomCrops event listener: " + e.getMessage());
            }
        }
        
        // Register CustomFishing event listener if CustomFishing is present
        if (getServer().getPluginManager().isPluginEnabled("CustomFishing")) {
            try {
                Class<?> customFishingListenerClass = Class.forName("fr.ax_dev.universejobs.listener.CustomFishingEventListener");
                Object customFishingListener = customFishingListenerClass
                    .getConstructor(UniverseJobs.class, ActionProcessor.class)
                    .newInstance(this, actionProcessor);
                getServer().getPluginManager().registerEvents((Listener) customFishingListener, this);
                getLogger().info("CustomFishing event listener registered successfully");
            } catch (Exception e) {
                getLogger().warning("Failed to register CustomFishing event listener: " + e.getMessage());
            }
        }
        
        // Register CraftEngine event listener if CraftEngine is present
        if (getServer().getPluginManager().isPluginEnabled("CraftEngine")) {
            try {
                Class<?> craftEngineListenerClass = Class.forName("fr.ax_dev.universejobs.listener.CraftEngineEventListener");
                Object craftEngineListener = craftEngineListenerClass
                    .getConstructor(UniverseJobs.class, ActionProcessor.class, BlockProtectionManager.class)
                    .newInstance(this, actionProcessor, protectionManager);
                getServer().getPluginManager().registerEvents((Listener) craftEngineListener, this);
                getLogger().info("CraftEngine event listener registered successfully");
            } catch (Exception e) {
                getLogger().warning("Failed to register CraftEngine event listener: " + e.getMessage());
            }
        }
        
        // Load player data for online players et précharge dans le cache
        for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
            foliaManager.runAsync(() -> {
                jobManager.loadPlayerData(player);
                rewardManager.loadPlayerData(player);
                // Précharge dans le cache ultra-rapide
                playerCache.preloadPlayer(player.getUniqueId());
            });
        }
        
        // Initialize PlaceholderAPI integration
        try {
            placeholderManager.initialize();
            // PlaceholderAPI integration initialized
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to initialize PlaceholderAPI integration", e);
        }
        
        // Initialize inactivity decay manager
        this.inactivityDecayManager = new fr.ax_dev.universejobs.job.InactivityDecayManager(this, jobManager);

        // Start periodic save task
        startSaveTask();

        // Start daily task for inactivity decay
        startDailyTask();

        // Check for optional dependencies
        checkDependencies();
        
        // Initialize accessor
        this.accessor = new fr.ax_dev.universejobs.utils.PluginAccessor(this);

        // Check for updates
        this.updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates();

        // Plugin enabled successfully
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down UniverseJobs plugin...");

        try {
            stopSaveTask();

            shutdownManagersExceptJobManager();

            savePlayerData();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            shutdownJobManager();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            shutdownStorageSystem();

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
        if (dailyTask != null && !dailyTask.isCancelled()) {
            dailyTask.cancel();
            dailyTask = null;
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
     * Shutdown all managers except JobManager.
     */
    private void shutdownManagersExceptJobManager() {
        shutdownRewardGuiManager();
        shutdownManagerSilently("menu manager", menuManager, () -> menuManager.closeAllMenus());
        shutdownManagerSilently("reward manager", rewardManager, () -> rewardManager.shutdown());
        shutdownManagerSilently("batched reward manager", batchedRewardManager, () -> batchedRewardManager.shutdown());
        shutdownManagerSilently("message sender", messageSender, () -> messageSender.shutdown());
        shutdownManagerSilently("bonus manager", bonusManager, () -> bonusManager.shutdown());
        shutdownManagerSilently("money bonus manager", moneyBonusManager, () -> moneyBonusManager.shutdown());
        shutdownManagerSilently("placeholder manager", placeholderManager, () -> placeholderManager.shutdown());
        shutdownManagerSilently("action limit manager", limitManager, () -> limitManager.clearAllLimits());
        shutdownManagerSilently("Folia manager", foliaManager, () -> foliaManager.cancelAllTasks());

        // Clear references for managers without explicit shutdown
        protectionManager = null;
        actionProcessor = null;
        languageManager = null;
        configManager = null;
    }

    /**
     * Shutdown JobManager last.
     */
    private void shutdownJobManager() {
        shutdownManagerSilently("job manager", jobManager, () -> jobManager.shutdown());
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
     * Shutdown a manager silently without individual logging.
     */
    private void shutdownManagerSilently(String managerName, Object manager, Runnable shutdownAction) {
        if (manager != null) {
            try {
                shutdownAction.run();

                // Give time for async operations to complete for critical managers
                if ("job manager".equals(managerName) || "reward manager".equals(managerName)) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
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
        }

        foliaManager.runTimerAsync(() -> {
            if (actionProcessor != null) {
                actionProcessor.clearExpiredCaches();
            }
        }, 6000L, 6000L);
    }

    private void startDailyTask() {
        long ticksIn24Hours = 20L * 60L * 60L * 24L;

        long initialDelay = calculateInitialDelayToMidnight();

        foliaManager.runTimerAsync(() -> {
            if (configManager.isDebugEnabled()) {
                getLogger().info("Running daily tasks...");
            }

            if (inactivityDecayManager != null) {
                inactivityDecayManager.processInactivePlayersAsync();
            }

            if (limitManager != null) {
                limitManager.checkAndResetLimits();
            }
        }, initialDelay, ticksIn24Hours);
    }

    private long calculateInitialDelayToMidnight() {
        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar nextMidnight = java.util.Calendar.getInstance();
        nextMidnight.add(java.util.Calendar.DAY_OF_MONTH, 1);
        nextMidnight.set(java.util.Calendar.HOUR_OF_DAY, 0);
        nextMidnight.set(java.util.Calendar.MINUTE, 0);
        nextMidnight.set(java.util.Calendar.SECOND, 0);
        nextMidnight.set(java.util.Calendar.MILLISECOND, 0);

        long millisecondsUntilMidnight = nextMidnight.getTimeInMillis() - now.getTimeInMillis();
        return (millisecondsUntilMidnight / 50L);
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
     * Handle player join events avec cache préloading.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load player data asynchronously et précharge dans le cache
        foliaManager.runAsync(() -> {
            jobManager.loadPlayerData(event.getPlayer());
            rewardManager.loadPlayerData(event.getPlayer());
            // Précharge immédiatement dans le cache
            playerCache.preloadPlayer(event.getPlayer().getUniqueId());

            // Update last login time for inactivity tracking
            if (inactivityDecayManager != null) {
                inactivityDecayManager.updatePlayerActivity(event.getPlayer().getUniqueId());
            }

            if (configCache.isDebugEnabled()) {
                getLogger().info("Loaded data and preloaded cache for player: " + event.getPlayer().getName());
            }
        });
    }
    
    /**
     * Handle player quit events avec cleanup cache.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up XP message sender resources
        if (messageSender != null) {
            messageSender.cleanupPlayer(event.getPlayer());
        }

        // Clear permission cache for this player
        PlayerJobData playerData = jobManager.getPlayerData(event.getPlayer().getUniqueId());
        if (playerData != null) {
            playerData.clearPermissionCache();
        }

        // Cleanup cache immédiatement
        playerCache.cleanupPlayer(event.getPlayer().getUniqueId());

        // Save player data asynchronously
        foliaManager.runAsync(() -> {
            jobManager.savePlayerData(event.getPlayer());
            rewardManager.unloadPlayerData(event.getPlayer());
            if (configCache.isDebugEnabled()) {
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
     * Get the batched reward manager.
     * 
     * @return The batched reward manager
     */
    public BatchedRewardManager getBatchedRewardManager() {
        return batchedRewardManager;
    }
    
    /**
     * Get the menu manager.
     * 
     * @return The menu manager
     */
    public MenuManager getMenuManager() {
        return menuManager;
    }
    
    public BoostManagerGui getBoostManagerGui() {
        return boostManagerGui;
    }
    
    /**
     * Get the configuration cache (ultra-fast).
     * 
     * @return The configuration cache
     */
    public ConfigurationCache getConfigCache() {
        return configCache;
    }
    
    /**
     * Get the player job cache (ultra-fast).
     * 
     * @return The player job cache
     */
    public PlayerJobCache getPlayerCache() {
        return playerCache;
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
     * Get the update checker.
     *
     * @return The update checker
     */
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
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
     * Get the centralized accessor for all plugin components.
     * 
     * @return The plugin accessor
     */
    public fr.ax_dev.universejobs.utils.PluginAccessor getAccessor() {
        return accessor;
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
     * Initialize the storage system based on configuration.
     */
    private void initializeStorageSystem() {
        String databaseType = getConfig().getString("database.type", "");
        boolean databaseEnabled = !databaseType.isEmpty() && (databaseType.equals("sqlite") || databaseType.equals("mysql"));
        
        if (databaseEnabled) {
            getLogger().info("Initializing database storage system...");
            DatabaseDataStorage databaseStorage = new DatabaseDataStorage(this);
            
            try {
                databaseStorage.initializeAsync().join();
                this.dataStorage = databaseStorage;
                this.dataMigrator = new DataMigrator(this, databaseStorage);
                
                if (dataMigrator.shouldMigrate()) {
                    getLogger().info("Legacy YML data detected - starting migration process...");
                    DataMigrator.MigrationResult result = dataMigrator.migrateAllData().join();
                    
                    if (result.isSuccessful()) {
                        getLogger().info("Data migration completed successfully!");
                        getLogger().info("Total records migrated: " + result.getTotalMigrated());
                        dataMigrator.markMigrationComplete();
                    } else {
                        getLogger().severe("Data migration failed: " + result.error);
                        throw new RuntimeException("Migration failed: " + result.error);
                    }
                }
                
                getLogger().info("Database storage system initialized successfully");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to initialize database storage", e);
                throw new RuntimeException("Database initialization failed", e);
            }
        } else {
            getLogger().info("Using file-based storage system");
            this.dataStorage = null;
        }
    }
    
    /**
     * Shutdown the storage system.
     */
    private void shutdownStorageSystem() {
        if (dataStorage != null) {
            try {
                dataStorage.shutdownAsync().join();
                getLogger().info("Storage system shut down successfully");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error shutting down storage system", e);
            }
        }
    }
    
    /**
     * Get the data storage instance.
     * 
     * @return The data storage instance, or null if using file-based storage
     */
    public DataStorage getDataStorage() {
        return dataStorage;
    }
    
    /**
     * Check if database storage is enabled.
     * 
     * @return true if database storage is enabled
     */
    public boolean isDatabaseEnabled() {
        return dataStorage != null;
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
