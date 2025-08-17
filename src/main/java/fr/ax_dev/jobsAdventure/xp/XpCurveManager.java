package fr.ax_dev.jobsAdventure.xp;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages XP curves for jobs, loading them from files or equations.
 */
public class XpCurveManager {
    
    private final JobsAdventure plugin;
    private final File curvesFolder;
    private final Map<String, XpCurve> curves = new HashMap<>();
    
    /**
     * Create a new XP curve manager.
     * 
     * @param plugin The plugin instance
     */
    public XpCurveManager(JobsAdventure plugin) {
        this.plugin = plugin;
        this.curvesFolder = new File(plugin.getDataFolder(), "xp-curves");
        
        // Create curves folder if it doesn't exist
        if (!curvesFolder.exists()) {
            curvesFolder.mkdirs();
            // Created xp-curves folder
        }
        
        // Check if folder is empty and create examples
        File[] existingFiles = curvesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (existingFiles == null || existingFiles.length == 0) {
            // No XP curve files found, creating examples
            createExampleCurves();
        }
        
        loadAllCurves();
    }
    
    /**
     * Load all XP curves from the curves folder.
     */
    private void loadAllCurves() {
        File[] files = curvesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files != null) {
            for (File file : files) {
                String curveName = file.getName().replace(".yml", "");
                try {
                    XpCurve curve = XpCurve.fromFile(curveName, file);
                    curves.put(curveName, curve);
                    // Loaded XP curve
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load XP curve: " + file.getName(), e);
                }
            }
        }
        
        // Add default curve if no curves loaded
        if (curves.isEmpty()) {
            XpCurve defaultCurve = XpCurve.createDefault();
            curves.put("default", defaultCurve);
            // Created default XP curve
        }
    }
    
    /**
     * Create example XP curve files.
     */
    private void createExampleCurves() {
        // Creating example XP curve files
        
        // Create example linear curve
        createLinearExample();
        
        // Create example steep curve
        createSteepExample();
        
        // Create example gentle curve
        createGentleExample();
        
        // Create combat-focused curve
        createCombatExample();
        
        // Create mining-focused curve
        createMiningExample();
        
        // Created example XP curve files
    }
    
    /**
     * Create a linear progression example.
     */
    private void createLinearExample() {
        File file = new File(curvesFolder, "linear.yml");
        YamlConfiguration config = new YamlConfiguration();
        
        // Add header comment
        config.options().header("""
            Linear XP Curve Example
            Each level requires 1000 more XP than the previous
            Level 1: 0 XP (starting level)
            Level 2: 1000 XP
            Level 3: 2000 XP
            etc...
            """);
        
        // Generate linear progression
        for (int level = 1; level <= 100; level++) {
            config.set(String.valueOf(level), (level - 1) * 1000.0);
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create linear.yml example", e);
        }
    }
    
    /**
     * Create a steep progression example.
     */
    private void createSteepExample() {
        File file = new File(curvesFolder, "steep.yml");
        YamlConfiguration config = new YamlConfiguration();
        
        config.options().header("""
            Steep XP Curve Example
            XP requirement increases exponentially
            Early levels are easy, later levels are very hard
            """);
        
        // Generate exponential progression
        for (int level = 1; level <= 100; level++) {
            double xp = Math.pow(level - 1, 2.5) * 50;
            config.set(String.valueOf(level), xp);
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create steep.yml example", e);
        }
    }
    
    /**
     * Create a gentle progression example.
     */
    private void createGentleExample() {
        File file = new File(curvesFolder, "gentle.yml");
        YamlConfiguration config = new YamlConfiguration();
        
        config.options().header("""
            Gentle XP Curve Example
            XP requirement increases slowly
            Good for casual gameplay
            """);
        
        // Generate gentle progression
        for (int level = 1; level <= 100; level++) {
            double xp = Math.sqrt(level - 1) * 500;
            config.set(String.valueOf(level), xp);
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create gentle.yml example", e);
        }
    }
    
    /**
     * Create a combat-focused progression example.
     */
    private void createCombatExample() {
        File file = new File(curvesFolder, "combat.yml");
        YamlConfiguration config = new YamlConfiguration();
        
        config.options().header("""
            Combat XP Curve Example
            Designed for fighting jobs (hunter, warrior, etc.)
            Fast early progression, moderate late game
            """);
        
        // Generate combat progression - fast early, moderate late
        for (int level = 1; level <= 100; level++) {
            double xp;
            if (level <= 20) {
                // Fast early levels
                xp = Math.pow(level - 1, 1.3) * 80;
            } else if (level <= 50) {
                // Moderate mid levels
                xp = Math.pow(level - 1, 1.6) * 60;
            } else {
                // Slower high levels
                xp = Math.pow(level - 1, 2.0) * 45;
            }
            config.set(String.valueOf(level), Math.round(xp));
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create combat.yml example", e);
        }
    }
    
    /**
     * Create a mining-focused progression example.
     */
    private void createMiningExample() {
        File file = new File(curvesFolder, "mining.yml");
        YamlConfiguration config = new YamlConfiguration();
        
        config.options().header("""
            Mining XP Curve Example
            Designed for resource gathering jobs (miner, lumberjack, etc.)
            Steady progression with milestone levels
            """);
        
        // Generate mining progression with milestone bonuses
        for (int level = 1; level <= 100; level++) {
            double baseXp = Math.pow(level - 1, 1.7) * 70;
            
            // Milestone levels (every 10 levels) require slightly less XP
            if (level % 10 == 0 && level > 1) {
                baseXp *= 0.9;
            }
            
            config.set(String.valueOf(level), Math.round(baseXp));
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create mining.yml example", e);
        }
    }
    
    /**
     * Get an XP curve by name.
     * 
     * @param name The curve name
     * @return The XP curve or default if not found
     */
    public XpCurve getCurve(String name) {
        return curves.getOrDefault(name, getDefaultCurve());
    }
    
    /**
     * Get or create an XP curve from an equation.
     * 
     * @param equation The mathematical equation
     * @return The XP curve
     */
    public XpCurve getCurveFromEquation(String equation) {
        // Check if this equation already exists
        String equationKey = "equation_" + equation.hashCode();
        
        if (!curves.containsKey(equationKey)) {
            XpCurve curve = XpCurve.fromEquation(equationKey, equation);
            curves.put(equationKey, curve);
        }
        
        return curves.get(equationKey);
    }
    
    /**
     * Get the default XP curve.
     * 
     * @return The default curve
     */
    public XpCurve getDefaultCurve() {
        if (curves.containsKey("default")) {
            return curves.get("default");
        }
        
        XpCurve defaultCurve = XpCurve.createDefault();
        curves.put("default", defaultCurve);
        return defaultCurve;
    }
    
    /**
     * Reload all XP curves.
     */
    public void reload() {
        curves.clear();
        loadAllCurves();
    }
    
    /**
     * Get all loaded curve names.
     * 
     * @return Set of curve names
     */
    public Map<String, XpCurve> getAllCurves() {
        return new HashMap<>(curves);
    }
}