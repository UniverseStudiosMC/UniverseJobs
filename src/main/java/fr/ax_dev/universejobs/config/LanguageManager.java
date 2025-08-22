package fr.ax_dev.universejobs.config;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages language files and provides localized messages for the plugin.
 * Supports multiple languages with fallback to English if a key is missing.
 */
public class LanguageManager {
    
    // Language constants
    private static final String DEFAULT_LOCALE = "en_US";
    private static final String MISSING_MESSAGE_PREFIX = "<red>[Missing message: ";
    private static final String MISSING_MESSAGE_SUFFIX = "]";
    
    private final UniverseJobs plugin;
    private final Map<String, FileConfiguration> languageFiles;
    private String currentLocale;
    private FileConfiguration currentLanguage;
    private FileConfiguration fallbackLanguage; // en_US as fallback
    
    public LanguageManager(UniverseJobs plugin) {
        this.plugin = plugin;
        this.languageFiles = new HashMap<>();
        this.currentLocale = DEFAULT_LOCALE; // Default locale
        
        setupLanguageFiles();
        loadLanguage();
    }
    
    /**
     * Sets up the language files by copying defaults from the JAR if they don't exist.
     */
    private void setupLanguageFiles() {
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        if (!languagesDir.exists()) {
            languagesDir.mkdirs();
        }
        
        // Copy default language files from JAR
        String[] defaultLanguages = {"en_US.yml", "fr_FR.yml"};
        for (String langFile : defaultLanguages) {
            File file = new File(languagesDir, langFile);
            if (!file.exists()) {
                try (InputStream in = plugin.getResource("languages/" + langFile)) {
                    if (in != null) {
                        Files.copy(in, file.toPath());
                        // Created default language file
                    }
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to create language file " + langFile + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Loads all available language files.
     */
    public void loadLanguageFiles() {
        languageFiles.clear();
        
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        if (!languagesDir.exists()) {
            setupLanguageFiles();
        }
        
        File[] files = languagesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    String locale = file.getName().replace(".yml", "");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    languageFiles.put(locale, config);
                    // Loaded language file
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load language file: " + file.getName(), e);
                }
            }
        }
        
        // Ensure en_US is always available as fallback
        if (!languageFiles.containsKey(DEFAULT_LOCALE)) {
            plugin.getLogger().warning("en_US language file not found! Creating fallback...");
            languageFiles.put(DEFAULT_LOCALE, new YamlConfiguration());
        }
    }
    
    /**
     * Loads the current language based on the configuration.
     */
    public void loadLanguage() {
        this.currentLocale = plugin.getConfig().getString("language.locale", DEFAULT_LOCALE);
        
        loadLanguageFiles();
        
        this.currentLanguage = languageFiles.get(currentLocale);
        this.fallbackLanguage = languageFiles.get(DEFAULT_LOCALE);
        
        if (currentLanguage == null) {
            plugin.getLogger().warning("Language " + currentLocale + " not found! Falling back to en_US");
            this.currentLanguage = fallbackLanguage;
            this.currentLocale = DEFAULT_LOCALE;
        }
        
        // Language loaded
    }
    
    /**
     * Gets a localized message by key with placeholder replacement.
     *
     * @param key The message key (e.g., "messages.level-up")
     * @param placeholders Key-value pairs for placeholder replacement
     * @return The localized message with placeholders replaced
     */
    public String getMessage(String key, Object... placeholders) {
        String message = getRawMessage(key);
        
        if (message == null) {
            return MISSING_MESSAGE_PREFIX + key + MISSING_MESSAGE_SUFFIX;
        }
        
        // Replace placeholders
        if (placeholders.length > 0) {
            for (int i = 0; i < placeholders.length - 1; i += 2) {
                String placeholder = "{" + placeholders[i] + "}";
                String value = String.valueOf(placeholders[i + 1]);
                message = message.replace(placeholder, value);
            }
        }
        
        // Parse the message and convert to legacy string for backward compatibility
        Component component = MessageUtils.parseMessage(message);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
    
    /**
     * Gets a raw message without color code translation.
     *
     * @param key The message key
     * @return The raw message or null if not found
     */
    private String getRawMessage(String key) {
        // Try current language first
        String message = currentLanguage.getString(key);
        
        // Fall back to English if not found
        if (message == null && fallbackLanguage != null && !currentLocale.equals(DEFAULT_LOCALE)) {
            message = fallbackLanguage.getString(key);
            if (message != null) {
                plugin.getLogger().warning("Missing translation for key '" + key + "' in " + currentLocale + ", using English fallback");
            }
        }
        
        return message;
    }
    
    /**
     * Gets a localized message with a map of placeholders.
     *
     * @param key The message key
     * @param placeholders Map of placeholder keys and values
     * @return The localized message with placeholders replaced
     */
    public String getMessage(String key, Map<String, Object> placeholders) {
        String message = getRawMessage(key);
        
        if (message == null) {
            return MISSING_MESSAGE_PREFIX + key + MISSING_MESSAGE_SUFFIX;
        }
        
        // Replace placeholders
        if (placeholders != null) {
            for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = String.valueOf(entry.getValue());
                message = message.replace(placeholder, value);
            }
        }
        
        // Parse the message and convert to legacy string for backward compatibility
        Component component = MessageUtils.parseMessage(message);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
    
    /**
     * Gets a simple message without placeholders.
     *
     * @param key The message key
     * @return The localized message
     */
    public String getMessage(String key) {
        return getMessage(key, new Object[0]);
    }
    
    /**
     * Checks if a message key exists in the current language.
     *
     * @param key The message key to check
     * @return true if the key exists
     */
    public boolean hasMessage(String key) {
        return getRawMessage(key) != null;
    }
    
    /**
     * Gets the current locale.
     *
     * @return The current locale code
     */
    public String getCurrentLocale() {
        return currentLocale;
    }
    
    /**
     * Gets all available locales.
     *
     * @return Set of available locale codes
     */
    public java.util.Set<String> getAvailableLocales() {
        return languageFiles.keySet();
    }
    
    /**
     * Gets a localized message as a Component (for direct MiniMessage usage).
     *
     * @param key The message key
     * @param placeholders Key-value pairs for placeholder replacement
     * @return The Component with formatting applied
     */
    public Component getMessageComponent(String key, Object... placeholders) {
        String message = getRawMessage(key);
        
        if (message == null) {
            return MessageUtils.parseMessage(MISSING_MESSAGE_PREFIX + key + MISSING_MESSAGE_SUFFIX);
        }
        
        // Replace placeholders
        if (placeholders.length > 0) {
            for (int i = 0; i < placeholders.length - 1; i += 2) {
                String placeholder = "{" + placeholders[i] + "}";
                String value = String.valueOf(placeholders[i + 1]);
                message = message.replace(placeholder, value);
            }
        }
        
        return MessageUtils.parseMessage(message);
    }
    
    /**
     * Gets a localized message as a Component with a map of placeholders.
     *
     * @param key The message key
     * @param placeholders Map of placeholder keys and values
     * @return The Component with formatting applied
     */
    public Component getMessageComponent(String key, Map<String, Object> placeholders) {
        String message = getRawMessage(key);
        
        if (message == null) {
            return MessageUtils.parseMessage(MISSING_MESSAGE_PREFIX + key + MISSING_MESSAGE_SUFFIX);
        }
        
        // Replace placeholders
        if (placeholders != null) {
            for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = String.valueOf(entry.getValue());
                message = message.replace(placeholder, value);
            }
        }
        
        return MessageUtils.parseMessage(message);
    }
    
    /**
     * Reloads all language files and the current language.
     */
    public void reload() {
        plugin.getLogger().info("Reloading language files...");
        loadLanguage();
    }
}