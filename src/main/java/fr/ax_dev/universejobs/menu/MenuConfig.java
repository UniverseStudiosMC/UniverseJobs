package fr.ax_dev.universejobs.menu;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.menu.config.SingleMenuConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Configuration manager for all menu configurations.
 */
public class MenuConfig {
    
    private final UniverseJobs plugin;
    private final File menusFolder;
    
    private SingleMenuConfig mainMenuConfig;
    private SingleMenuConfig jobMenuConfig;
    private SingleMenuConfig actionsMenuConfig;
    private SingleMenuConfig rankingsMenuConfig;
    
    public MenuConfig(UniverseJobs plugin) {
        this.plugin = plugin;
        this.menusFolder = new File(plugin.getDataFolder(), "menus");
        
        createMenusFolder();
    }
    
    /**
     * Create the menus folder and copy default configurations.
     */
    private void createMenusFolder() {
        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
        }
        
        // Copy default configuration files if they don't exist
        String[] defaultConfigs = {
            "main-menu.yml",
            "job-menu.yml", 
            "actions-menu.yml",
            "rankings-menu.yml"
        };
        
        for (String configName : defaultConfigs) {
            File configFile = new File(menusFolder, configName);
            if (!configFile.exists()) {
                copyDefaultConfig(configName, configFile);
            }
        }
    }
    
    /**
     * Copy a default configuration file from resources.
     */
    private void copyDefaultConfig(String resourceName, File target) {
        try (InputStream inputStream = plugin.getResource("menus/" + resourceName)) {
            if (inputStream != null) {
                Files.copy(inputStream, target.toPath());
                plugin.getLogger().info("Created default menu configuration: " + resourceName);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to copy default menu configuration '" + resourceName + "': " + e.getMessage());
        }
    }
    
    /**
     * Load all menu configurations.
     */
    public void loadConfigurations() {
        try {
            // Load main menu configuration
            File mainMenuFile = new File(menusFolder, "main-menu.yml");
            if (mainMenuFile.exists()) {
                FileConfiguration mainConfig = YamlConfiguration.loadConfiguration(mainMenuFile);
                this.mainMenuConfig = new SingleMenuConfig(mainConfig);
            } else {
                plugin.getLogger().warning("Main menu configuration not found, using defaults");
                this.mainMenuConfig = SingleMenuConfig.getDefaultMainMenu();
            }
            
            // Load job menu configuration
            File jobMenuFile = new File(menusFolder, "job-menu.yml");
            if (jobMenuFile.exists()) {
                FileConfiguration jobConfig = YamlConfiguration.loadConfiguration(jobMenuFile);
                this.jobMenuConfig = new SingleMenuConfig(jobConfig);
            } else {
                plugin.getLogger().warning("Job menu configuration not found, using defaults");
                this.jobMenuConfig = SingleMenuConfig.getDefaultJobMenu();
            }
            
            // Load actions menu configuration
            File actionsMenuFile = new File(menusFolder, "actions-menu.yml");
            if (actionsMenuFile.exists()) {
                FileConfiguration actionsConfig = YamlConfiguration.loadConfiguration(actionsMenuFile);
                this.actionsMenuConfig = new SingleMenuConfig(actionsConfig);
            } else {
                plugin.getLogger().warning("Actions menu configuration not found, using defaults");
                this.actionsMenuConfig = SingleMenuConfig.getDefaultActionsMenu();
            }
            
            // Load rankings menu configuration
            File rankingsMenuFile = new File(menusFolder, "rankings-menu.yml");
            if (rankingsMenuFile.exists()) {
                FileConfiguration rankingsConfig = YamlConfiguration.loadConfiguration(rankingsMenuFile);
                this.rankingsMenuConfig = new SingleMenuConfig(rankingsConfig);
            } else {
                plugin.getLogger().warning("Rankings menu configuration not found, using defaults");
                this.rankingsMenuConfig = SingleMenuConfig.getDefaultRankingsMenu();
            }
            
            plugin.getLogger().info("Menu configurations loaded successfully");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load menu configurations: " + e.getMessage());
            
            // Use defaults if loading failed
            this.mainMenuConfig = SingleMenuConfig.getDefaultMainMenu();
            this.jobMenuConfig = SingleMenuConfig.getDefaultJobMenu();
            this.actionsMenuConfig = SingleMenuConfig.getDefaultActionsMenu();
            this.rankingsMenuConfig = SingleMenuConfig.getDefaultRankingsMenu();
        }
    }
    
    /**
     * Save a configuration file.
     */
    public void saveConfiguration(String fileName, FileConfiguration config) {
        try {
            File configFile = new File(menusFolder, fileName);
            config.save(configFile);
            plugin.getLogger().info("Saved menu configuration: " + fileName);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save menu configuration '" + fileName + "': " + e.getMessage());
        }
    }
    
    // Getters
    public SingleMenuConfig getMainMenuConfig() {
        return mainMenuConfig;
    }
    
    public SingleMenuConfig getJobMenuConfig() {
        return jobMenuConfig;
    }
    
    public SingleMenuConfig getActionsMenuConfig() {
        return actionsMenuConfig;
    }
    
    public SingleMenuConfig getRankingsMenuConfig() {
        return rankingsMenuConfig;
    }
    
    public File getMenusFolder() {
        return menusFolder;
    }
}