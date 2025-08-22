import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeFormatTest {
    public static void main(String[] args) {
        testTimeFormats();
    }
    
    private static void testTimeFormats() {
        String[] testTimes = {
            "00:00",
            "12:30",
            "9:15",
            "23:45", 
            "2:30 PM",
            "2:30PM",
            "02:30 AM",
            "02:30AM",
            "12:00 AM", // Midnight
            "12:00 PM"  // Noon
        };
        
        System.out.println("Testing time formats:");
        
        for (String timeString : testTimes) {
            try {
                LocalDateTime result = parseTime(timeString);
                System.out.println("✓ '" + timeString + "' -> " + result.getHour() + ":" + 
                                 String.format("%02d", result.getMinute()));
            } catch (Exception e) {
                System.out.println("✗ '" + timeString + "' -> Failed: " + e.getMessage());
            }
        }
    }
    
    private static LocalDateTime parseTime(String timeString) {
        String normalizedTime = timeString.trim();
        LocalDateTime baseDate = LocalDateTime.now().withSecond(0).withNano(0);
        
        // Try different time formats
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("HH:mm"),     // 24-hour format (e.g., "14:30")
            DateTimeFormatter.ofPattern("H:mm"),      // 24-hour format single digit (e.g., "9:30")
            DateTimeFormatter.ofPattern("h:mm a"),    // 12-hour format with AM/PM (e.g., "2:30 PM")
            DateTimeFormatter.ofPattern("hh:mm a"),   // 12-hour format with leading zero (e.g., "02:30 PM")
            DateTimeFormatter.ofPattern("h:mma"),     // 12-hour format without space (e.g., "2:30PM")
            DateTimeFormatter.ofPattern("hh:mma")     // 12-hour format without space and leading zero (e.g., "02:30PM")
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                // For 24-hour formats, parse directly
                if (formatter.toString().contains("HH") || formatter.toString().contains("H:")) {
                    java.time.LocalTime time = java.time.LocalTime.parse(normalizedTime, formatter);
                    return baseDate.with(time);
                } else {
                    // For 12-hour formats with AM/PM
                    java.time.LocalTime time = java.time.LocalTime.parse(normalizedTime.toUpperCase(), formatter);
                    return baseDate.with(time);
                }
            } catch (Exception ignored) {
                // Try next formatter
            }
        }
        
        throw new IllegalArgumentException("Unable to parse time: " + timeString);
    }
}