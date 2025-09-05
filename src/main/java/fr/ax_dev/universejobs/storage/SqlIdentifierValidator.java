package fr.ax_dev.universejobs.storage;

import java.util.regex.Pattern;

public final class SqlIdentifierValidator {
    
    private static final Pattern SAFE_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final int MAX_IDENTIFIER_LENGTH = 64;
    
    private SqlIdentifierValidator() {
        throw new AssertionError("Utility class");
    }
    
    public static String validateAndSanitizeIdentifier(String identifier, String context) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException(context + " cannot be null or empty");
        }
        
        String trimmed = identifier.trim();
        
        if (trimmed.length() > MAX_IDENTIFIER_LENGTH) {
            throw new IllegalArgumentException(context + " is too long (max " + MAX_IDENTIFIER_LENGTH + " characters): " + trimmed);
        }
        
        if (!SAFE_IDENTIFIER_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(context + " contains invalid characters. Only alphanumeric characters and underscores are allowed, must start with letter or underscore: " + trimmed);
        }
        
        return trimmed;
    }
    
    public static String buildSafeTableName(String prefix, String tableName) {
        String safePrefix = validateAndSanitizeIdentifier(prefix, "Table prefix");
        String safeSuffix = validateAndSanitizeIdentifier(tableName, "Table name");
        return safePrefix + safeSuffix;
    }
    
    public static String buildSafeIndexName(String prefix, String indexName) {
        String safePrefix = validateAndSanitizeIdentifier(prefix, "Index prefix");
        String safeSuffix = validateAndSanitizeIdentifier(indexName, "Index name");
        return "idx_" + safePrefix + safeSuffix;
    }
}