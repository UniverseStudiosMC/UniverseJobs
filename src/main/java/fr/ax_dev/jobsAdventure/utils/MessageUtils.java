package fr.ax_dev.jobsAdventure.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling message formatting with MiniMessage and legacy color codes support.
 */
public class MessageUtils {
    
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");
    
    /**
     * Parse a message string with MiniMessage and legacy color code support.
     * 
     * @param message The message to parse
     * @return The parsed Component
     */
    public static Component parseMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        
        // Check if the message contains legacy codes and convert if needed
        String processedMessage;
        if (LegacyToMiniMessageConverter.containsLegacyCodes(message)) {
            processedMessage = LegacyToMiniMessageConverter.convert(message);
        } else {
            processedMessage = message;
        }
        
        // Parse as MiniMessage (it handles plain text gracefully)
        try {
            return miniMessage.deserialize(processedMessage);
        } catch (Exception e) {
            // Fallback to plain text if parsing fails
            return Component.text(message);
        }
    }
    
    /**
     * Parse a message with placeholders.
     * 
     * @param message The message to parse
     * @param placeholders Map of placeholder keys to values
     * @return The parsed Component with placeholders replaced
     */
    public static Component parseMessage(String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        
        // Replace placeholders
        String processedMessage = replacePlaceholders(message, placeholders);
        
        // Parse the message
        return parseMessage(processedMessage);
    }
    
    /**
     * Parse a message with a single placeholder.
     * 
     * @param message The message to parse
     * @param placeholder The placeholder key
     * @param value The placeholder value
     * @return The parsed Component with placeholder replaced
     */
    public static Component parseMessage(String message, String placeholder, String value) {
        return parseMessage(message, Map.of(placeholder, value));
    }
    
    /**
     * Send a formatted message to a player.
     * 
     * @param player The player to send the message to
     * @param message The message to send
     */
    public static void sendMessage(Player player, String message) {
        if (player != null && message != null && !message.isEmpty()) {
            player.sendMessage(parseMessage(message));
        }
    }
    
    /**
     * Send a formatted message to a command sender.
     * 
     * @param sender The command sender to send the message to
     * @param message The message to send
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(parseMessage(message));
        }
    }
    
    /**
     * Send a formatted message with placeholders to a player.
     * 
     * @param player The player to send the message to
     * @param message The message to send
     * @param placeholders Map of placeholder keys to values
     */
    public static void sendMessage(Player player, String message, Map<String, String> placeholders) {
        if (player != null && message != null && !message.isEmpty()) {
            player.sendMessage(parseMessage(message, placeholders));
        }
    }
    
    /**
     * Send a formatted message with a single placeholder to a player.
     * 
     * @param player The player to send the message to
     * @param message The message to send
     * @param placeholder The placeholder key
     * @param value The placeholder value
     */
    public static void sendMessage(Player player, String message, String placeholder, String value) {
        sendMessage(player, message, Map.of(placeholder, value));
    }
    
    /**
     * Convert legacy color codes to MiniMessage format.
     * 
     * @param message The message to convert
     * @return The converted message in MiniMessage format
     */
    public static String translateLegacyColorCodes(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        // Use the efficient regex-based converter
        return LegacyToMiniMessageConverter.convert(message);
    }
    
    
    /**
     * Replace placeholders in a message.
     */
    private static String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return message;
        }
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = placeholders.getOrDefault(key, "{" + key + "}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Strip all formatting from a message.
     * 
     * @param message The message to strip
     * @return The message without formatting
     */
    public static String stripFormatting(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        Component component = parseMessage(message);
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
    
    /**
     * Send an action bar message to a player.
     * 
     * @param player The player to send the message to
     * @param message The message to send
     */
    public static void sendActionBar(Player player, String message) {
        if (player != null && message != null) {
            player.sendActionBar(parseMessage(message));
        }
    }
    
    /**
     * Send an action bar message with placeholders to a player.
     * 
     * @param player The player to send the message to
     * @param message The message to send
     * @param placeholders Map of placeholder keys to values
     */
    public static void sendActionBar(Player player, String message, Map<String, String> placeholders) {
        if (player != null && message != null) {
            player.sendActionBar(parseMessage(message, placeholders));
        }
    }
    
    /**
     * Colorize a message (convert color codes).
     * 
     * @param message The message to colorize
     * @return The colorized message
     */
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        // Convert legacy codes and return as legacy string
        String processed = translateLegacyColorCodes(message);
        Component component = parseMessage(processed);
        return legacySerializer.serialize(component);
    }
}