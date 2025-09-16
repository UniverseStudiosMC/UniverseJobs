package fr.ax_dev.universejobs.reward.gui;

import fr.ax_dev.universejobs.UniverseJobs;
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
    
    private final UniverseJobs plugin;
    private final Map<String, GuiConfig> guiConfigs;
    
    /**
     * Create a new GuiConfigLoader.
     * 
     * @param plugin The plugin instance
     */
    public GuiConfigLoader(UniverseJobs plugin) {
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
                // Created gui folder - generate all default GUI files
                createDefaultGuiFiles();
            } else {
                plugin.getLogger().severe("Failed to create gui folder: " + guiFolder.getPath());
                return;
            }
        }
        
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
        GuiConfig config = guiConfigs.get(name);
        if (config == null) {
            plugin.getLogger().warning("GUI configuration '" + name + "' not found");
        }
        return config;
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
     * Create default GUI files from resources.
     * Only called when gui folder is created for the first time.
     */
    private void createDefaultGuiFiles() {
        String[] defaultGuiFiles = {
            "miner_rewards_gui.yml",
            "farmer_rewards_gui.yml",
            "hunter_rewards_gui.yml",
            "lumberjack_rewards_gui.yml"
        };
        int createdCount = 0;

        for (String guiFile : defaultGuiFiles) {
            try {
                plugin.saveResource("gui/" + guiFile, false);
                createdCount++;
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Could not create GUI file " + guiFile + ": " + e.getMessage());
            }
        }

        if (createdCount > 0) {
            plugin.getLogger().info("Created " + createdCount + " default GUI files");
        }
    }

    /**
     * Reload all GUI configurations.
     */
    public void reloadGuiConfigs() {
        loadGuiConfigs();
    }
}