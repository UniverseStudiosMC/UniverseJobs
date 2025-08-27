package fr.ax_dev.universejobs.menu.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Simple implementation of ConfigurationSection for creating menu items programmatically.
 */
public class SimpleConfigurationSection extends MemoryConfiguration {
    
    public SimpleConfigurationSection() {
        super();
    }
    
    public SimpleConfigurationSection(Map<String, Object> values) {
        super();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }
    
    @Override
    public String getString(String path) {
        Object value = get(path);
        return value != null ? value.toString() : null;
    }
    
    @Override
    public String getString(String path, String def) {
        String value = getString(path);
        return value != null ? value : def;
    }
    
    @Override
    public int getInt(String path) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
    
    @Override
    public int getInt(String path, int def) {
        try {
            return getInt(path);
        } catch (Exception e) {
            return def;
        }
    }
    
    @Override
    public boolean getBoolean(String path) {
        Object value = get(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }
    
    @Override
    public boolean getBoolean(String path, boolean def) {
        try {
            return getBoolean(path);
        } catch (Exception e) {
            return def;
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String path) {
        Object value = get(path);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return List.of();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<Integer> getIntegerList(String path) {
        Object value = get(path);
        if (value instanceof List) {
            return (List<Integer>) value;
        }
        return List.of();
    }
    
    @Override
    public ConfigurationSection getConfigurationSection(String path) {
        Object value = get(path);
        if (value instanceof ConfigurationSection) {
            return (ConfigurationSection) value;
        }
        return null;
    }
}