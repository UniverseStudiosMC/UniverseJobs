package fr.ax_dev.jobsAdventure.reward.gui;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Loader for GUI configurations from the gui/ directory.
 */
public class GuiConfigLoader {
    
    private final JobsAdventure plugin;
    private final Map<String, GuiConfig> guiConfigs;
    
    /**
     * Create a new GuiConfigLoader.
     * 
     * @param plugin The plugin instance
     */
    public GuiConfigLoader(JobsAdventure plugin) {
        this.plugin = plugin;
        this.guiConfigs = new ConcurrentHashMap<>();
    }
    
    /**
     * Load all GUI configurations from the gui/ directory.
     */
    public void loadGuiConfigs() {
        guiConfigs.clear();
        
        File guiFolder = new File(plugin.getDataFolder(), "gui");
        if (!guiFolder.exists()) {
            if (guiFolder.mkdirs()) {
                // Created gui folder
            } else {
                plugin.getLogger().severe("Failed to create gui folder: " + guiFolder.getPath());
                return;
            }
        }
        
        // Always try to create example files if they don't exist
        createExampleGuiFiles();
        
        // Load GUI files
        File[] files = guiFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            plugin.getLogger().warning("No GUI files found in gui folder");
            return;
        }
        
        for (File file : files) {
            loadGuiFile(file);
        }
        
        // GUI configurations loaded
    }
    
    /**
     * Load a GUI configuration from a specific file.
     * 
     * @param file The GUI file
     */
    private void loadGuiFile(File file) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String fileName = file.getName().replace(".yml", "");
            
            GuiConfig guiConfig = new GuiConfig(config);
            guiConfigs.put(fileName, guiConfig);
            
            // GUI configuration loaded
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load GUI file " + file.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Get a GUI configuration by name.
     * 
     * @param name The GUI configuration name
     * @return The GUI configuration, or null if not found
     */
    public GuiConfig getGuiConfig(String name) {
        return guiConfigs.get(name);
    }
    
    /**
     * Get all loaded GUI configurations.
     * 
     * @return Map of all GUI configurations
     */
    public Map<String, GuiConfig> getAllGuiConfigs() {
        return new ConcurrentHashMap<>(guiConfigs);
    }
    
    /**
     * Create example GUI files for demonstration.
     */
    private void createExampleGuiFiles() {
        createExampleRewardsGuiFile();
    }
    
    /**
     * Create an example rewards GUI configuration file by copying from resources.
     */
    private void createExampleRewardsGuiFile() {
        File guiFile = new File(plugin.getDataFolder(), "gui/example_rewards_gui.yml");
        if (guiFile.exists()) return;
        
        try {
            plugin.saveResource("gui/example_rewards_gui.yml", false);
            // Created example GUI file
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create example GUI file from resources", e);
        }
    }
    
    /**
     * Reload all GUI configurations.
     */
    public void reloadGuiConfigs() {
        loadGuiConfigs();
    }
}