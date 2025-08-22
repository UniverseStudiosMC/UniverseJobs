package fr.ax_dev.universejobs.utils;

import java.util.Arrays;
import java.util.List;

/**
 * Simple test class to verify the LegacyToMiniMessageConverter functionality.
 * This class can be used to test conversions before applying them to production files.
 */
public class LegacyConverterTest {
    
    /**
     * Test method that demonstrates various conversion scenarios.
     * This can be called from a command or during plugin initialization for testing.
     */
    public static void runTests() {
        System.out.println("=== Legacy to MiniMessage Converter Tests ===");
        
        // Test cases covering various scenarios
        List<String> testCases = Arrays.asList(
            // Basic color codes
            "&aHello World",
            "&6This is &lgold and bold&r text",
            "&cRed &4Dark Red &fWhite",
            
            // Format codes
            "&lBold &oItalic &nUnderlined &mStrikethrough &kObfuscated",
            "&l&nBold and Underlined",
            
            // Reset codes
            "&6Gold &lBold &rReset to normal",
            
            // Hex colors (&#RRGGBB format)
            "&#FF0000Red &#00FF00Green &#0000FFBlue",
            "&#FFD700Golden text with &#FFFFFF white",
            
            // Legacy hex format (&x&R&R&G&G&B&B)
            "&x&F&F&0&0&0&0Red &x&0&0&F&F&0&0Green &x&0&0&0&0&F&FBlue",
            
            // Section symbol codes (should also work)
            "§aGreen §6Gold §lBold",
            
            // Mixed formats
            "&6&lBold Gold &r&cNormal Red",
            "&#FF5500Orange &lwith bold",
            
            // Already MiniMessage (should pass through unchanged)
            "<gold>Already MiniMessage</gold>",
            "<#FF0000>Hex in MiniMessage</#FF0000>",
            
            // Edge cases
            "&",  // Single ampersand
            "&&a", // Double ampersand
            "&zInvalid", // Invalid code
            "", // Empty string
            "Normal text without codes",
            
            // Real examples from the language files
            "&e+{xp} XP ({job})",
            "&aCongratulations! You reached level {level} in {job}!",
            "&e&lBONUS XP! &6All players received &e{bonus}% &6bonus XP for &e{duration}&6!",
            "§cThis command can only be used by players!",
            "&6=== {job} ===",
            "&7Description: &f{description}"
        );
        
        System.out.println("\nTesting " + testCases.size() + " cases:");
        System.out.println("----------------------------------------");
        
        int passed = 0;
        int failed = 0;
        
        for (int i = 0; i < testCases.size(); i++) {
            String input = testCases.get(i);
            try {
                String output = LegacyToMiniMessageConverter.convert(input);
                boolean hasLegacy = LegacyToMiniMessageConverter.containsLegacyCodes(input);
                System.out.printf("Test %2d: %-40s -> %s%n", 
                    i + 1, 
                    truncate(input, 40), 
                    truncate(output, 50));
                
                // Validate the conversion
                if (hasLegacy && input.equals(output)) {
                    System.out.println("  WARNING: Legacy codes detected but no conversion occurred");
                    failed++;
                } else {
                    passed++;
                }
                
            } catch (Exception e) {
                System.out.printf("Test %2d: FAILED - %s (Error: %s)%n", i + 1, input, e.getMessage());
                failed++;
            }
        }
        
        System.out.println("----------------------------------------");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        
        // Test performance
        testPerformance();
        
        System.out.println("=== Test Complete ===");
    }
    
    /**
     * Tests the performance of the converter with a large number of conversions.
     */
    private static void testPerformance() {
        System.out.println("\n=== Performance Test ===");
        
        String testMessage = "&6&lJobsAdventure &r&eYou gained &a{xp} XP &ein &b{job}&e! &#FF0000Red &#00FF00Green";
        int iterations = 10000;
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            LegacyToMiniMessageConverter.convert(testMessage);
        }
        
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        double milliseconds = duration / 1_000_000.0;
        double averagePerConversion = duration / (double) iterations;
        
        System.out.printf("Converted %d messages in %.2f ms%n", iterations, milliseconds);
        System.out.printf("Average: %.2f nanoseconds per conversion%n", averagePerConversion);
        System.out.printf("Rate: %.0f conversions per second%n", 1_000_000_000.0 / averagePerConversion);
    }
    
    /**
     * Utility method to truncate strings for display.
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) return "null";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Tests specific conversion scenarios that are common in Minecraft plugins.
     */
    public static void testMinecraftScenarios() {
        System.out.println("=== Minecraft Plugin Scenarios ===");
        
        String[] scenarios = {
            // Chat prefixes
            "&7[&6JobsAdventure&7] &a",
            
            // Progress bars
            "&a████████&7██ &e80%",
            
            // XP messages
            "&e+{xp} XP &8(&7{job}&8)",
            
            // Level up messages
            "&6&l*** &eLEVEL UP! &6&l***",
            
            // Command usage
            "&cUsage: &e/job join <jobname>",
            
            // GUI titles
            "&6&lRewards &8- &e{job}",
            
            // Status indicators
            "&aOnline &8| &7{count} players",
            
            // Error messages
            "&cError: &7{message}",
            
            // Placeholders with colors
            "&7Balance: &a${balance} &8| &7Level: &e{level}"
        };
        
        for (String scenario : scenarios) {
            String converted = LegacyToMiniMessageConverter.convert(scenario);
            System.out.printf("Original:  %s%n", scenario);
            System.out.printf("Converted: %s%n", converted);
            System.out.println();
        }
    }
}