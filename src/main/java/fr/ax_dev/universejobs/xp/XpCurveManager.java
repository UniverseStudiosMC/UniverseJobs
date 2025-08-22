package fr.ax_dev.universejobs.xp;

import fr.ax_dev.universejobs.UniverseJobs;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages XP curves for jobs, loading them from files or equations.
 */
public class XpCurveManager {
    
    private final UniverseJobs plugin;
    private final File curvesFolder;
    private final Map<String, XpCurve> curves = new HashMap<>();
    
    /**
     * Create a new XP curve manager.
     * 
     * @param plugin The plugin instance
     */
    public XpCurveManager(UniverseJobs plugin) {
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
        // Copy example curve file from resources
        plugin.saveResource("xp-curves/example.yml", false);
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