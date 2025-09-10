package fr.ax_dev.universejobs.utils;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Method;

/**
 * Utility class for safe enum parsing that works across all Minecraft/Paper versions.
 * Avoids IncompatibleClassChangeError on Paper 1.21+ by using reflection.
 */
public class EnumUtils {
    
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
     * Safely parse a Material enum value.
     */
    public static Material parseMaterial(String materialName, Material defaultMaterial) {
        return parseEnumSafely(Material.class, materialName, defaultMaterial);
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
     * Generic safe enum parsing using reflection to avoid valueOf() issues.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T parseEnumSafely(Class<T> enumClass, String value, T defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        
        try {
            // Method 1: Try using valueOf via reflection
            try {
                Method valueOfMethod = enumClass.getMethod("valueOf", String.class);
                Object result = valueOfMethod.invoke(null, value.toUpperCase());
                if (enumClass.isInstance(result)) {
                    return (T) result;
                }
            } catch (Exception e) {
                // valueOf failed, try next method
            }
            
            // Method 2: Try getting field directly
            try {
                java.lang.reflect.Field field = enumClass.getField(value.toUpperCase());
                if (field.getType() == enumClass) {
                    return (T) field.get(null);
                }
            } catch (Exception e) {
                // Field access failed, try next method
            }
            
            // Method 3: Iterate through values() via reflection
            try {
                Method valuesMethod = enumClass.getMethod("values");
                T[] values = (T[]) valuesMethod.invoke(null);
                for (T enumConstant : values) {
                    if (enumConstant.name().equalsIgnoreCase(value)) {
                        return enumConstant;
                    }
                }
            } catch (Exception e) {
                // values() failed
            }
            
        } catch (Exception e) {
            // All methods failed
        }
        
        return defaultValue;
    }
}