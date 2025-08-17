package fr.ax_dev.jobsAdventure.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
        
        // First, convert legacy color codes (&) to section symbols (§)
        String processedMessage = translateLegacyColorCodes(message);
        
        // Check if the message contains MiniMessage tags
        if (containsMiniMessageTags(processedMessage)) {
            // Parse as MiniMessage
            return miniMessage.deserialize(processedMessage);
        } else if (processedMessage.contains("§")) {
            // Parse as legacy
            return legacySerializer.deserialize(processedMessage);
        } else {
            // Plain text, but try MiniMessage first in case it has tags
            return miniMessage.deserialize(processedMessage);
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
     * Translate legacy color codes (&) to section symbols (§).
     * 
     * @param message The message to translate
     * @return The translated message
     */
    public static String translateLegacyColorCodes(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        // Translate &x color codes (hex) and standard color codes
        char[] chars = message.toCharArray();
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length) {
                char next = chars[i + 1];
                // Check for valid color codes (0-9, a-f, k-o, r, x)
                if ((next >= '0' && next <= '9') || 
                    (next >= 'a' && next <= 'f') || 
                    (next >= 'k' && next <= 'o') || 
                    next == 'r') {
                    result.append('§').append(next);
                    i++; // Skip the next character
                } else if (next == 'x' && i + 13 < chars.length) {
                    // Check for hex color code (&x&R&R&G&G&B&B)
                    boolean isHex = true;
                    for (int j = 2; j <= 12; j += 2) {
                        if (chars[i + j] != '&' || !isHexChar(chars[i + j + 1])) {
                            isHex = false;
                            break;
                        }
                    }
                    
                    if (isHex) {
                        // Convert to MiniMessage hex format
                        StringBuilder hex = new StringBuilder("<#");
                        for (int j = 3; j <= 13; j += 2) {
                            hex.append(chars[i + j]);
                        }
                        hex.append(">");
                        result.append(hex);
                        i += 13; // Skip the hex code
                    } else {
                        result.append(chars[i]);
                    }
                } else {
                    result.append(chars[i]);
                }
            } else {
                result.append(chars[i]);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Check if a character is a valid hex character.
     */
    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || 
               (c >= 'a' && c <= 'f') || 
               (c >= 'A' && c <= 'F');
    }
    
    /**
     * Check if a message contains MiniMessage tags.
     */
    private static boolean containsMiniMessageTags(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        
        // Common MiniMessage tags
        return message.contains("<") && message.contains(">") && (
            message.contains("<gradient") ||
            message.contains("<rainbow") ||
            message.contains("<color") ||
            message.contains("<#") ||
            message.contains("<bold") ||
            message.contains("<italic") ||
            message.contains("<underlined") ||
            message.contains("<strikethrough") ||
            message.contains("<obfuscated") ||
            message.contains("<click") ||
            message.contains("<hover") ||
            message.contains("<reset") ||
            message.contains("<br>") ||
            message.contains("</")
        );
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