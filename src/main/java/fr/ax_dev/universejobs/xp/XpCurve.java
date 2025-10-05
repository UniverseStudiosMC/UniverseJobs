package fr.ax_dev.universejobs.xp;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents an XP curve for a job, supporting both linear (file-based) and equation-based curves.
 */
public class XpCurve {
    
    private final String name;
    private final CurveType type;
    private final TreeMap<Integer, Double> linearCurve;
    private final String equation;
    private final Map<Integer, Double> cache = new HashMap<>();
    
    /**
     * Curve type enum.
     */
    public enum CurveType {
        LINEAR,    // Defined in a YAML file with level -> xp mapping
        EQUATION   // Defined by a mathematical equation
    }
    
    /**
     * Create a linear XP curve from a file.
     * 
     * @param name The curve name
     * @param file The YAML file containing the curve data
     * @return The XP curve
     */
    public static XpCurve fromFile(String name, File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        TreeMap<Integer, Double> perLevelXp = new TreeMap<>();

        for (String key : config.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                double xp = config.getDouble(key);
                perLevelXp.put(level, xp);
            } catch (NumberFormatException e) {
            }
        }

        if (!perLevelXp.containsKey(1)) {
            perLevelXp.put(1, 0.0);
        }

        TreeMap<Integer, Double> cumulativeCurve = new TreeMap<>();
        double totalXp = 0.0;

        for (Map.Entry<Integer, Double> entry : perLevelXp.entrySet()) {
            cumulativeCurve.put(entry.getKey(), totalXp);
            totalXp += entry.getValue();
        }

        return new XpCurve(name, cumulativeCurve);
    }
    
    /**
     * Create an equation-based XP curve.
     * 
     * @param name The curve name
     * @param equation The mathematical equation (using 'level' as variable)
     * @return The XP curve
     */
    public static XpCurve fromEquation(String name, String equation) {
        return new XpCurve(name, equation);
    }
    
    /**
     * Create a default XP curve with a standard progression.
     * 
     * @return The default XP curve
     */
    public static XpCurve createDefault() {
        // Default equation: 100 * level^2
        return new XpCurve("default", "100 * Math.pow(level, 2)");
    }
    
    /**
     * Constructor for linear curve.
     * 
     * @param name The curve name
     * @param linearCurve The level -> xp mappings
     */
    private XpCurve(String name, TreeMap<Integer, Double> linearCurve) {
        this.name = name;
        this.type = CurveType.LINEAR;
        this.linearCurve = linearCurve;
        this.equation = null;
    }
    
    /**
     * Constructor for equation-based curve.
     * 
     * @param name The curve name
     * @param equation The mathematical equation
     */
    private XpCurve(String name, String equation) {
        this.name = name;
        this.type = CurveType.EQUATION;
        this.linearCurve = null;
        this.equation = equation;
    }
    
    /**
     * Get the XP required for a specific level.
     * 
     * @param level The level
     * @return The XP required to reach that level
     */
    public double getXpForLevel(int level) {
        if (level <= 1) return 0;
        
        // Check cache first
        if (cache.containsKey(level)) {
            return cache.get(level);
        }
        
        double xp;
        if (type == CurveType.LINEAR) {
            xp = getLinearXp(level);
        } else {
            xp = getEquationXp(level);
        }
        
        // Cache the result
        cache.put(level, xp);
        return xp;
    }
    
    /**
     * Get XP from linear curve.
     * 
     * @param level The level
     * @return The XP required
     */
    private double getLinearXp(int level) {
        // If exact level exists, return it
        if (linearCurve.containsKey(level)) {
            return linearCurve.get(level);
        }
        
        // Find the surrounding levels for interpolation
        Map.Entry<Integer, Double> lower = linearCurve.floorEntry(level);
        Map.Entry<Integer, Double> higher = linearCurve.ceilingEntry(level);
        
        if (lower == null) {
            // Level is below minimum, use first entry
            return linearCurve.firstEntry().getValue();
        }
        
        if (higher == null) {
            // Level is above maximum, extrapolate from last two points
            Map.Entry<Integer, Double> lastEntry = linearCurve.lastEntry();
            Map.Entry<Integer, Double> secondLastEntry = linearCurve.lowerEntry(lastEntry.getKey());
            
            if (secondLastEntry == null) {
                // Only one point, can't extrapolate
                return lastEntry.getValue();
            }
            
            // Linear extrapolation
            double slope = (lastEntry.getValue() - secondLastEntry.getValue()) / 
                          (lastEntry.getKey() - secondLastEntry.getKey());
            return lastEntry.getValue() + slope * (level - lastEntry.getKey());
        }
        
        // Linear interpolation between two points
        double ratio = (double)(level - lower.getKey()) / (higher.getKey() - lower.getKey());
        return lower.getValue() + ratio * (higher.getValue() - lower.getValue());
    }
    
    
    /**
     * Get XP from equation.
     * 
     * @param level The level
     * @return The XP required
     * @throws RuntimeException if equation cannot be evaluated
     */
    private double getEquationXp(int level) {
        try {
            // Use our custom expression evaluator
            return ExpressionEvaluator.evaluate(equation, "level", level);
        } catch (Exception e) {
            throw new RuntimeException("Error evaluating XP equation '" + equation + "': " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the level for a given amount of XP.
     * 
     * @param xp The XP amount
     * @param maxLevel The maximum level to check
     * @return The level
     */
    public int getLevelForXp(double xp, int maxLevel) {
        // Binary search for efficiency
        int low = 1;
        int high = maxLevel;
        int result = 1;
        
        while (low <= high) {
            int mid = (low + high) / 2;
            double requiredXp = getXpForLevel(mid);
            
            if (requiredXp <= xp) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        
        return result;
    }
    
    /**
     * Get the XP required to go from one level to the next.
     *
     * @param currentLevel The current level
     * @return The XP required for next level
     */
    public double getXpToNextLevel(int currentLevel) {
        if (currentLevel <= 0) return getXpForLevel(2);
        return getXpForLevel(currentLevel + 1) - getXpForLevel(currentLevel);
    }

    /**
     * Get the level from a given amount of XP (alias for getLevelForXp with default max level).
     *
     * @param xp The XP amount
     * @return The level
     */
    public int getLevelFromXp(double xp) {
        return getLevelForXp(xp, 100);
    }
    
    /**
     * Get the curve name.
     * 
     * @return The curve name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the curve type.
     * 
     * @return The curve type
     */
    public CurveType getType() {
        return type;
    }
    
    /**
     * Get the equation (if equation-based).
     * 
     * @return The equation or null
     */
    public String getEquation() {
        return equation;
    }
    
    @Override
    public String toString() {
        return "XpCurve{name='" + name + "', type=" + type + 
               (type == CurveType.EQUATION ? ", equation='" + equation + "'" : "") + "}";
    }
}