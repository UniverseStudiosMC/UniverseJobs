package fr.ax_dev.jobsAdventure.config;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for message display settings.
 * Supports CHAT, ACTIONBAR, and BOSSBAR display types.
 */
public class MessageConfig {
    
    public enum MessageType {
        CHAT,
        ACTIONBAR,
        BOSSBAR
    }
    
    private final MessageType type;
    private final String text;
    private final int duration; // For ACTIONBAR and BOSSBAR
    private final BarStyle bossbarStyle;
    private final BarColor bossbarColor;
    
    /**
     * Create a message configuration from a configuration section.
     * 
     * @param config The configuration section
     */
    public MessageConfig(ConfigurationSection config) {
        if (config == null) {
            // Default settings
            this.type = MessageType.CHAT;
            this.text = "";
            this.duration = 60;
            this.bossbarStyle = BarStyle.SOLID;
            this.bossbarColor = BarColor.GREEN;
        } else {
            // Parse type
            String typeStr = config.getString("type", "CHAT").toUpperCase();
            MessageType tempType;
            try {
                tempType = MessageType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                tempType = MessageType.CHAT;
            }
            this.type = tempType;
            
            // Get text (support both old "message" key and direct text)
            this.text = config.getString("text", config.getString("message", ""));
            
            // Duration for ACTIONBAR and BOSSBAR
            this.duration = config.getInt("duration", 60);
            
            // BOSSBAR specific settings
            String styleStr = config.getString("style", "SOLID").toUpperCase();
            if (styleStr.startsWith("SEGMENT_")) {
                // Convert old format: segment_0 -> SEGMENTED_6, segment_10 -> SEGMENTED_10, etc.
                styleStr = styleStr.replace("SEGMENT_", "SEGMENTED_");
                if (styleStr.equals("SEGMENTED_0")) {
                    styleStr = "SEGMENTED_6";
                }
            }
            BarStyle tempStyle;
            try {
                tempStyle = BarStyle.valueOf(styleStr);
            } catch (IllegalArgumentException e) {
                tempStyle = BarStyle.SOLID;
            }
            this.bossbarStyle = tempStyle;
            
            String colorStr = config.getString("color", "GREEN").toUpperCase();
            BarColor tempColor;
            try {
                tempColor = BarColor.valueOf(colorStr);
            } catch (IllegalArgumentException e) {
                tempColor = BarColor.GREEN;
            }
            this.bossbarColor = tempColor;
        }
    }
    
    /**
     * Create a message configuration from a simple string (legacy support).
     * 
     * @param message The message text
     */
    public MessageConfig(String message) {
        this.type = MessageType.CHAT;
        this.text = message != null ? message : "";
        this.duration = 60;
        this.bossbarStyle = BarStyle.SOLID;
        this.bossbarColor = BarColor.GREEN;
    }
    
    /**
     * Get the message type.
     * 
     * @return The message type
     */
    public MessageType getType() {
        return type;
    }
    
    /**
     * Get the message text.
     * 
     * @return The message text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Get the duration in ticks.
     * 
     * @return The duration
     */
    public int getDuration() {
        return duration;
    }
    
    /**
     * Get the bossbar style.
     * 
     * @return The bossbar style
     */
    public BarStyle getBossbarStyle() {
        return bossbarStyle;
    }
    
    /**
     * Get the bossbar color.
     * 
     * @return The bossbar color
     */
    public BarColor getBossbarColor() {
        return bossbarColor;
    }
    
    /**
     * Check if this message config has content.
     * 
     * @return true if there is text to display
     */
    public boolean hasContent() {
        return text != null && !text.isEmpty();
    }
    
    /**
     * Parse a list of command strings from configuration.
     * 
     * @param config The configuration section
     * @param key The key to look for
     * @return List of commands or empty list
     */
    public static List<String> parseCommands(ConfigurationSection config, String key) {
        if (config == null || !config.contains(key)) {
            return new ArrayList<>();
        }
        
        Object value = config.get(key);
        if (value instanceof List<?>) {
            List<String> commands = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item != null) {
                    commands.add(item.toString());
                }
            }
            return commands;
        } else if (value instanceof String) {
            List<String> commands = new ArrayList<>();
            commands.add((String) value);
            return commands;
        }
        
        return new ArrayList<>();
    }
}