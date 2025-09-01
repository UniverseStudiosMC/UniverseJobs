package fr.ax_dev.universejobs.utils;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.action.ActionLimitManager;
import fr.ax_dev.universejobs.action.ActionProcessor;
import fr.ax_dev.universejobs.bonus.MoneyBonusManager;
import fr.ax_dev.universejobs.bonus.XpBonusManager;
import fr.ax_dev.universejobs.cache.ConfigurationCache;
import fr.ax_dev.universejobs.cache.PlayerJobCache;
import fr.ax_dev.universejobs.config.ConfigManager;
import fr.ax_dev.universejobs.config.LanguageManager;
import fr.ax_dev.universejobs.integration.MythicMobsHandler;
import fr.ax_dev.universejobs.job.JobManager;
import fr.ax_dev.universejobs.levelup.SimpleLevelUpActionManager;
import fr.ax_dev.universejobs.menu.MenuManager;
import fr.ax_dev.universejobs.placeholder.PlaceholderManager;
import fr.ax_dev.universejobs.protection.BlockProtectionManager;
import fr.ax_dev.universejobs.reward.RewardManager;
import fr.ax_dev.universejobs.reward.gui.RewardGuiManager;
import fr.ax_dev.universejobs.compatibility.FoliaCompatibilityManager;

/**
 * Centralized accessor for all plugin managers and utilities.
 * Provides thread-safe access to all plugin components.
 */
public final class PluginAccessor {
    
    private final UniverseJobs plugin;
    
    public PluginAccessor(UniverseJobs plugin) {
        this.plugin = plugin;
    }
    
    // Core managers
    public ConfigManager getConfigManager() { return plugin.getConfigManager(); }
    public LanguageManager getLanguageManager() { return plugin.getLanguageManager(); }
    public JobManager getJobManager() { return plugin.getJobManager(); }
    public MenuManager getMenuManager() { return plugin.getMenuManager(); }
    
    // Action system
    public ActionProcessor getActionProcessor() { return plugin.getActionProcessor(); }
    public ActionLimitManager getLimitManager() { return plugin.getLimitManager(); }
    
    // Bonus system
    public XpBonusManager getBonusManager() { return plugin.getBonusManager(); }
    public MoneyBonusManager getMoneyBonusManager() { return plugin.getMoneyBonusManager(); }
    
    // Reward system
    public RewardManager getRewardManager() { return plugin.getRewardManager(); }
    public RewardGuiManager getRewardGuiManager() { return plugin.getRewardGuiManager(); }
    
    // Cache system
    public ConfigurationCache getConfigCache() { return plugin.getConfigCache(); }
    public PlayerJobCache getPlayerCache() { return plugin.getPlayerCache(); }
    
    // Utility managers
    public AsyncXpMessageSender getMessageSender() { return plugin.getMessageSender(); }
    public BlockProtectionManager getProtectionManager() { return plugin.getProtectionManager(); }
    public PlaceholderManager getPlaceholderManager() { return plugin.getPlaceholderManager(); }
    
    // Integration handlers
    public MythicMobsHandler getMythicMobsHandler() { return plugin.getMythicMobsHandler(); }
    
    // Level up system
    public SimpleLevelUpActionManager getLevelUpActionManager() { return plugin.getLevelUpActionManager(); }
    
    // Compatibility
    public FoliaCompatibilityManager getFoliaManager() { return plugin.getFoliaManager(); }
    
    // Plugin info
    public long getStartTime() { return plugin.getStartTime(); }
    
    // Utility methods
    public boolean isDebugEnabled() { return getConfigCache().isDebugEnabled(); }
    public void logInfo(String message) { plugin.getLogger().info(message); }
    public void logWarning(String message) { plugin.getLogger().warning(message); }
    public void logSevere(String message) { plugin.getLogger().severe(message); }
}