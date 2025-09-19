package fr.ax_dev.universejobs.utils;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Utility class for safe enum parsing that works across all Minecraft/Paper versions.
 * Avoids IncompatibleClassChangeError on Paper 1.21+ by using reflection.
 */
public class EnumUtils {

    // Cache for enum parsing results to avoid repeated reflection calls
    private static final Map<String, Object> enumCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 500;

    // Pre-cache Material values since they're used frequently
    private static Material[] cachedMaterialValues = null;

    static {
        try {
            Method valuesMethod = Material.class.getMethod("values");
            cachedMaterialValues = (Material[]) valuesMethod.invoke(null);
        } catch (Exception e) {
            // Fallback to runtime fetching if initialization fails
        }
    }
    
    /**
     * Safely parse a Sound enum value using reflection to avoid version conflicts.
     */
    public static Sound parseSound(String soundName, Sound defaultSound) {
        if (soundName == null || soundName.isEmpty()) {
            return defaultSound;
        }
        
        try {
            // Method 1: Try using Registry.SOUNDS (Paper 1.20+)
            try {
                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(
                    soundName.toLowerCase().replace("_", ".")
                );
                
                java.lang.reflect.Field soundsField = org.bukkit.Registry.class.getField("SOUNDS");
                Object soundsRegistry = soundsField.get(null);
                java.lang.reflect.Method getMethod = soundsRegistry.getClass().getMethod("get", org.bukkit.NamespacedKey.class);
                Object result = getMethod.invoke(soundsRegistry, key);
                
                if (result instanceof Sound) {
                    return (Sound) result;
                }
            } catch (NoSuchFieldException e) {
                // Registry.SOUNDS doesn't exist, try fallback
            } catch (Exception e) {
                // Registry method failed, try fallback
            }
            
            // Method 2: Try using valueOf via reflection
            try {
                java.lang.reflect.Method valueOfMethod = Sound.class.getMethod("valueOf", String.class);
                Object result = valueOfMethod.invoke(null, soundName.toUpperCase());
                if (result instanceof Sound) {
                    return (Sound) result;
                }
            } catch (Exception e) {
                // valueOf failed, try next method
            }
            
            // Method 3: Try getting field directly
            try {
                java.lang.reflect.Field field = Sound.class.getField(soundName.toUpperCase());
                if (field.getType() == Sound.class) {
                    return (Sound) field.get(null);
                }
            } catch (Exception e) {
                // Field access failed, try next method
            }
            
            // Method 4: Iterate through values() via reflection
            try {
                java.lang.reflect.Method valuesMethod = Sound.class.getMethod("values");
                Sound[] values = (Sound[]) valuesMethod.invoke(null);
                for (Sound enumConstant : values) {
                    if (enumConstant.name().equalsIgnoreCase(soundName)) {
                        return enumConstant;
                    }
                }
            } catch (Exception e) {
                // values() failed
            }
            
        } catch (Exception e) {
            // All methods failed
        }
        
        return defaultSound;
    }
    
    /**
     * Safely parse a Material enum value with flexible name matching.
     * Supports both HONEY_BOTTLE and HONEYBOTTLE formats.
     */
    public static Material parseMaterial(String materialName, Material defaultMaterial) {
        if (materialName == null || materialName.isEmpty()) {
            return defaultMaterial;
        }

        // Check cache first
        String cacheKey = "Material:" + materialName.toUpperCase();
        Object cached = enumCache.get(cacheKey);
        if (cached != null) {
            return cached == NullMarker.INSTANCE ? defaultMaterial : (Material) cached;
        }

        // Try the original name first
        Material result = parseEnumSafely(Material.class, materialName, null);
        if (result != null) {
            addToCache(cacheKey, result);
            return result;
        }

        // If not found, try flexible material name matching
        result = parseMaterialFlexible(materialName, defaultMaterial);
        addToCache(cacheKey, result != null ? result : NullMarker.INSTANCE);
        return result;
    }

    // Marker object for null values in cache
    private static class NullMarker {
        static final NullMarker INSTANCE = new NullMarker();
    }

    private static void addToCache(String key, Object value) {
        if (enumCache.size() >= MAX_CACHE_SIZE) {
            // Simple eviction: clear entire cache when full
            enumCache.clear();
        }
        enumCache.put(key, value);
    }
    
    /**
     * Safely parse a Particle enum value.
     */
    public static Particle parseParticle(String particleName, Particle defaultParticle) {
        return parseEnumSafely(Particle.class, particleName, defaultParticle);
    }
    
    /**
     * Safely parse an EntityType enum value.
     */
    public static EntityType parseEntityType(String entityName, EntityType defaultType) {
        return parseEnumSafely(EntityType.class, entityName, defaultType);
    }
    
    /**
     * Safely parse a BarColor enum value.
     */
    public static BarColor parseBarColor(String colorName, BarColor defaultColor) {
        return parseEnumSafely(BarColor.class, colorName, defaultColor);
    }
    
    /**
     * Safely parse a BarStyle enum value.
     */
    public static BarStyle parseBarStyle(String styleName, BarStyle defaultStyle) {
        return parseEnumSafely(BarStyle.class, styleName, defaultStyle);
    }
    
    /**
     * Flexible material parsing that handles underscore variations.
     * Examples: HONEYBOTTLE -> HONEY_BOTTLE, honey_bottle -> HONEY_BOTTLE
     */
    public static Material parseMaterialFlexible(String materialName, Material defaultMaterial) {
        if (materialName == null || materialName.isEmpty()) {
            return defaultMaterial;
        }

        String normalizedName = materialName.trim().toUpperCase();

        // Use cached Material values if available
        Material[] values = cachedMaterialValues;
        if (values == null) {
            try {
                // Fallback to runtime fetching if cache is not available
                Method valuesMethod = Material.class.getMethod("values");
                values = (Material[]) valuesMethod.invoke(null);
            } catch (Exception e) {
                return defaultMaterial;
            }
        }

        for (Material material : values) {
            String materialNameStr = material.name();

            // Exact match (case insensitive)
            if (materialNameStr.equalsIgnoreCase(normalizedName)) {
                return material;
            }

            // Match without underscores (HONEY_BOTTLE matches HONEYBOTTLE)
            String withoutUnderscores = materialNameStr.replace("_", "");
            String inputWithoutUnderscores = normalizedName.replace("_", "");
            if (withoutUnderscores.equalsIgnoreCase(inputWithoutUnderscores)) {
                return material;
            }
        }

        return defaultMaterial;
    }

    /**
     * Generic safe enum parsing using reflection to avoid valueOf() issues.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T parseEnumSafely(Class<T> enumClass, String value, T defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        // Check cache first
        String cacheKey = enumClass.getSimpleName() + ":" + value.toUpperCase();
        Object cached = enumCache.get(cacheKey);
        if (cached != null) {
            return cached == NullMarker.INSTANCE ? defaultValue : (T) cached;
        }

        T result = null;

        try {
            // Method 1: Try using valueOf via reflection
            try {
                Method valueOfMethod = enumClass.getMethod("valueOf", String.class);
                Object enumResult = valueOfMethod.invoke(null, value.toUpperCase());
                if (enumClass.isInstance(enumResult)) {
                    result = (T) enumResult;
                }
            } catch (Exception e) {
                // valueOf failed, try next method
            }

            if (result == null) {
                // Method 2: Try getting field directly
                try {
                    java.lang.reflect.Field field = enumClass.getField(value.toUpperCase());
                    if (field.getType() == enumClass) {
                        result = (T) field.get(null);
                    }
                } catch (Exception e) {
                    // Field access failed, try next method
                }
            }

            if (result == null) {
                // Method 3: Iterate through values() via reflection
                try {
                    Method valuesMethod = enumClass.getMethod("values");
                    T[] values = (T[]) valuesMethod.invoke(null);
                    for (T enumConstant : values) {
                        if (enumConstant.name().equalsIgnoreCase(value)) {
                            result = enumConstant;
                            break;
                        }
                    }
                } catch (Exception e) {
                    // values() failed
                }
            }

        } catch (Exception e) {
            // All methods failed
        }

        // Cache the result
        addToCache(cacheKey, result != null ? result : NullMarker.INSTANCE);
        return result != null ? result : defaultValue;
    }
}