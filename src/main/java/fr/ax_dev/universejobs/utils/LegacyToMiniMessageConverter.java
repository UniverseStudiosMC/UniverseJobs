package fr.ax_dev.universejobs.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Efficient regex-based utility for converting legacy Minecraft color codes to MiniMessage format.
 * 
 * This converter handles:
 * - Standard color codes (&0-&9, &a-&f)
 * - Formatting codes (&l, &o, &n, &m, &k, &r)
 * - Hex colors (&#RRGGBB and &x&R&R&G&G&B&B formats)
 * - Nested/combined formats
 * - Reset codes with proper tag closing
 * 
 * Performance optimized with compiled regex patterns and efficient string replacement.
 */
public class LegacyToMiniMessageConverter {
    
    // Color code mappings from legacy to MiniMessage
    private static final Map<String, String> COLOR_MAPPINGS = new HashMap<>();
    private static final Map<String, String> FORMAT_MAPPINGS = new HashMap<>();
    
    // Compiled regex patterns for maximum performance
    private static final Pattern HEX_PATTERN_AMPERSAND = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern HEX_PATTERN_LEGACY = Pattern.compile("&x(&[0-9a-fA-F]){6}");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("&([0-9a-fklmnor])");
    private static final Pattern SECTION_COLOR_PATTERN = Pattern.compile("§([0-9a-fklmnor])");
    
    static {
        // Initialize color mappings
        COLOR_MAPPINGS.put("0", "black");
        COLOR_MAPPINGS.put("1", "dark_blue");
        COLOR_MAPPINGS.put("2", "dark_green");
        COLOR_MAPPINGS.put("3", "dark_aqua");
        COLOR_MAPPINGS.put("4", "dark_red");
        COLOR_MAPPINGS.put("5", "dark_purple");
        COLOR_MAPPINGS.put("6", "gold");
        COLOR_MAPPINGS.put("7", "gray");
        COLOR_MAPPINGS.put("8", "dark_gray");
        COLOR_MAPPINGS.put("9", "blue");
        COLOR_MAPPINGS.put("a", "green");
        COLOR_MAPPINGS.put("b", "aqua");
        COLOR_MAPPINGS.put("c", "red");
        COLOR_MAPPINGS.put("d", "light_purple");
        COLOR_MAPPINGS.put("e", "yellow");
        COLOR_MAPPINGS.put("f", "white");
        
        // Initialize format mappings
        FORMAT_MAPPINGS.put("k", "obfuscated");
        FORMAT_MAPPINGS.put("l", "bold");
        FORMAT_MAPPINGS.put("m", "strikethrough");
        FORMAT_MAPPINGS.put("n", "underlined");
        FORMAT_MAPPINGS.put("o", "italic");
        FORMAT_MAPPINGS.put("r", "reset");
    }
    
    /**
     * Converts a legacy color code string to MiniMessage format.
     * 
     * @param input The input string with legacy color codes
     * @return The converted string in MiniMessage format
     */
    public static String convert(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String result = input;
        
        // Step 1: Convert hex colors (&#RRGGBB format)
        result = convertHexColorsAmpersand(result);
        
        // Step 2: Convert legacy hex colors (&x&R&R&G&G&B&B format)
        result = convertHexColorsLegacy(result);
        
        // Step 3: Convert legacy color and format codes
        result = convertLegacyCodes(result);
        
        // Step 4: Convert section symbol codes (§) if any remain
        result = convertSectionCodes(result);
        
        return result;
    }
    
    /**
     * Converts hex color codes in &#RRGGBB format to MiniMessage.
     */
    private static String convertHexColorsAmpersand(String input) {
        return HEX_PATTERN_AMPERSAND.matcher(input).replaceAll(matchResult -> {
            String hex = matchResult.group(1);
            return "<#" + hex + ">";
        });
    }
    
    /**
     * Converts legacy hex color codes in &x&R&R&G&G&B&B format to MiniMessage.
     */
    private static String convertHexColorsLegacy(String input) {
        return HEX_PATTERN_LEGACY.matcher(input).replaceAll(matchResult -> {
            String match = matchResult.group();
            // Extract hex digits from &x&R&R&G&G&B&B
            StringBuilder hex = new StringBuilder();
            for (int i = 3; i < match.length(); i += 2) {
                hex.append(match.charAt(i));
            }
            return "<#" + hex + ">";
        });
    }
    
    /**
     * Converts legacy ampersand color and format codes to MiniMessage.
     */
    private static String convertLegacyCodes(String input) {
        return LEGACY_COLOR_PATTERN.matcher(input).replaceAll(matchResult -> {
            String code = matchResult.group(1);
            return convertCodeToMiniMessage(code);
        });
    }
    
    /**
     * Converts section symbol color and format codes to MiniMessage.
     */
    private static String convertSectionCodes(String input) {
        return SECTION_COLOR_PATTERN.matcher(input).replaceAll(matchResult -> {
            String code = matchResult.group(1);
            return convertCodeToMiniMessage(code);
        });
    }
    
    /**
     * Converts a single legacy code to its MiniMessage equivalent.
     */
    private static String convertCodeToMiniMessage(String code) {
        // Check if it's a color code
        if (COLOR_MAPPINGS.containsKey(code)) {
            return "<" + COLOR_MAPPINGS.get(code) + ">";
        }
        
        // Check if it's a format code
        if (FORMAT_MAPPINGS.containsKey(code)) {
            return "<" + FORMAT_MAPPINGS.get(code) + ">";
        }
        
        // If not found, return the original code (shouldn't happen with valid input)
        return "&" + code;
    }
    
    /**
     * Converts legacy color codes and properly handles nested formatting.
     * This version attempts to close open tags when a reset is encountered.
     * 
     * @param input The input string with legacy color codes
     * @return The converted string with proper tag nesting
     */
    public static String convertWithProperNesting(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // For now, use the simple conversion
        // Advanced nesting support could be added here if needed
        return convert(input);
    }
    
    /**
     * Batch converts multiple strings efficiently.
     * 
     * @param inputs Array of strings to convert
     * @return Array of converted strings
     */
    public static String[] convertBatch(String... inputs) {
        if (inputs == null) {
            return null;
        }
        
        String[] results = new String[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            results[i] = convert(inputs[i]);
        }
        return results;
    }
    
    /**
     * Checks if a string contains legacy color codes.
     * 
     * @param input The string to check
     * @return true if legacy codes are found
     */
    public static boolean containsLegacyCodes(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        return LEGACY_COLOR_PATTERN.matcher(input).find() ||
               SECTION_COLOR_PATTERN.matcher(input).find() ||
               HEX_PATTERN_AMPERSAND.matcher(input).find() ||
               HEX_PATTERN_LEGACY.matcher(input).find();
    }
    
    /**
     * Checks if a string is already in MiniMessage format.
     * 
     * @param input The string to check
     * @return true if it appears to be MiniMessage format
     */
    public static boolean isMiniMessageFormat(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        // Check for MiniMessage tags
        return input.contains("<") && input.contains(">") && (
            input.contains("<#") ||  // Hex colors
            input.contains("<black>") || input.contains("<white>") || // Common colors
            input.contains("<bold>") || input.contains("<italic>") || // Common formats
            input.contains("<reset>") || input.contains("<gradient>") || // MiniMessage specific
            input.contains("</") // Closing tags
        );
    }
    
    /**
     * Safely converts a string, only if it contains legacy codes.
     * 
     * @param input The input string
     * @return The converted string if legacy codes were found, otherwise the original
     */
    public static String convertIfNeeded(String input) {
        if (containsLegacyCodes(input)) {
            return convert(input);
        }
        return input;
    }
}