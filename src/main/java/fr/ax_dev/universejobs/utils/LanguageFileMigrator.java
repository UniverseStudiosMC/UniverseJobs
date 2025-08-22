package fr.ax_dev.universejobs.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Utility for migrating language files from legacy color codes to MiniMessage format.
 * 
 * This tool provides:
 * - Batch conversion of all language files in a directory
 * - Backup creation before conversion
 * - Detailed logging of conversion process
 * - Rollback functionality if needed
 * - Conversion validation and reporting
 */
public class LanguageFileMigrator {
    
    private final Logger logger;
    private final File languagesDirectory;
    private final List<String> conversionReport;
    
    public LanguageFileMigrator(File languagesDirectory, Logger logger) {
        this.languagesDirectory = languagesDirectory;
        this.logger = logger;
        this.conversionReport = new ArrayList<>();
    }
    
    /**
     * Migrates all YAML language files in the languages directory.
     * 
     * @param createBackup Whether to create backup files before conversion
     * @return true if migration was successful
     */
    public boolean migrateAllFiles(boolean createBackup) {
        conversionReport.clear();
        
        if (!languagesDirectory.exists() || !languagesDirectory.isDirectory()) {
            logger.severe("Languages directory not found: " + languagesDirectory.getPath());
            return false;
        }
        
        File[] yamlFiles = languagesDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (yamlFiles == null || yamlFiles.length == 0) {
            logger.info("No YAML language files found to migrate.");
            return true;
        }
        
        logger.info("Starting migration of " + yamlFiles.length + " language file(s)...");
        
        boolean allSuccessful = true;
        int convertedFiles = 0;
        int totalConversions = 0;
        
        for (File file : yamlFiles) {
            try {
                MigrationResult result = migrateFile(file, createBackup);
                if (result.isSuccessful()) {
                    convertedFiles++;
                    totalConversions += result.getConversionCount();
                    logger.info("Successfully migrated " + file.getName() + " (" + result.getConversionCount() + " messages converted)");
                } else {
                    allSuccessful = false;
                    logger.warning("Failed to migrate " + file.getName() + ": " + result.getErrorMessage());
                }
                
                conversionReport.addAll(result.getDetails());
                
            } catch (Exception e) {
                allSuccessful = false;
                logger.severe("Error migrating " + file.getName() + ": " + e.getMessage());
                conversionReport.add("ERROR: " + file.getName() + " - " + e.getMessage());
            }
        }
        
        logger.info("Migration complete. Files converted: " + convertedFiles + "/" + yamlFiles.length + 
                   ", Total message conversions: " + totalConversions);
        
        return allSuccessful;
    }
    
    /**
     * Migrates a single language file.
     * 
     * @param file The YAML file to migrate
     * @param createBackup Whether to create a backup before conversion
     * @return MigrationResult containing the results
     */
    public MigrationResult migrateFile(File file, boolean createBackup) {
        try {
            // Create backup if requested
            if (createBackup) {
                File backupFile = new File(file.getParent(), file.getName() + ".backup");
                Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Load the YAML file
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            // Convert all string values
            int conversions = convertConfigurationSection(config, "");
            
            // Save the converted file
            config.save(file);
            
            return new MigrationResult(true, conversions, "Migration successful", new ArrayList<>());
            
        } catch (Exception e) {
            return new MigrationResult(false, 0, e.getMessage(), new ArrayList<>());
        }
    }
    
    /**
     * Recursively converts all string values in a configuration section.
     */
    private int convertConfigurationSection(ConfigurationSection section, String path) {
        int conversions = 0;
        Set<String> keys = section.getKeys(false);
        
        for (String key : keys) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            
            if (section.isConfigurationSection(key)) {
                // Recursively process nested sections
                conversions += convertConfigurationSection(section.getConfigurationSection(key), fullPath);
            } else if (section.isString(key)) {
                // Convert string values
                String originalValue = section.getString(key);
                if (originalValue != null && LegacyToMiniMessageConverter.containsLegacyCodes(originalValue)) {
                    String convertedValue = LegacyToMiniMessageConverter.convert(originalValue);
                    section.set(key, convertedValue);
                    conversions++;
                    
                    conversionReport.add("CONVERTED: " + fullPath + " | " + originalValue + " -> " + convertedValue);
                }
            }
        }
        
        return conversions;
    }
    
    /**
     * Creates backup files for all language files.
     * 
     * @return true if all backups were created successfully
     */
    public boolean createBackups() {
        if (!languagesDirectory.exists() || !languagesDirectory.isDirectory()) {
            logger.severe("Languages directory not found: " + languagesDirectory.getPath());
            return false;
        }
        
        File[] yamlFiles = languagesDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (yamlFiles == null || yamlFiles.length == 0) {
            logger.info("No YAML language files found to backup.");
            return true;
        }
        
        boolean allSuccessful = true;
        for (File file : yamlFiles) {
            try {
                File backupFile = new File(file.getParent(), file.getName() + ".backup");
                Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.info("Created backup: " + backupFile.getName());
            } catch (IOException e) {
                allSuccessful = false;
                logger.warning("Failed to create backup for " + file.getName() + ": " + e.getMessage());
            }
        }
        
        return allSuccessful;
    }
    
    /**
     * Restores all language files from their backup files.
     * 
     * @return true if all files were restored successfully
     */
    public boolean restoreFromBackups() {
        if (!languagesDirectory.exists() || !languagesDirectory.isDirectory()) {
            logger.severe("Languages directory not found: " + languagesDirectory.getPath());
            return false;
        }
        
        File[] backupFiles = languagesDirectory.listFiles((dir, name) -> name.endsWith(".yml.backup"));
        if (backupFiles == null || backupFiles.length == 0) {
            logger.info("No backup files found to restore.");
            return true;
        }
        
        boolean allSuccessful = true;
        for (File backupFile : backupFiles) {
            try {
                String originalName = backupFile.getName().replace(".backup", "");
                File originalFile = new File(backupFile.getParent(), originalName);
                Files.copy(backupFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.info("Restored from backup: " + originalName);
            } catch (IOException e) {
                allSuccessful = false;
                logger.warning("Failed to restore " + backupFile.getName() + ": " + e.getMessage());
            }
        }
        
        return allSuccessful;
    }
    
    /**
     * Validates that all string values in language files are properly formatted.
     * 
     * @return ValidationResult containing validation details
     */
    public ValidationResult validateFiles() {
        if (!languagesDirectory.exists() || !languagesDirectory.isDirectory()) {
            return new ValidationResult(false, "Languages directory not found: " + languagesDirectory.getPath());
        }
        
        File[] yamlFiles = languagesDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (yamlFiles == null || yamlFiles.length == 0) {
            return new ValidationResult(true, "No YAML language files found.");
        }
        
        List<String> issues = new ArrayList<>();
        int filesChecked = 0;
        int totalMessages = 0;
        int legacyMessages = 0;
        
        for (File file : yamlFiles) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                ValidationFileResult fileResult = validateConfigurationSection(config, "", file.getName());
                
                filesChecked++;
                totalMessages += fileResult.totalMessages;
                legacyMessages += fileResult.legacyMessages;
                issues.addAll(fileResult.issues);
                
            } catch (Exception e) {
                issues.add("ERROR reading " + file.getName() + ": " + e.getMessage());
            }
        }
        
        String summary = String.format("Validation complete: %d files, %d messages, %d still using legacy codes", 
                                      filesChecked, totalMessages, legacyMessages);
        
        return new ValidationResult(legacyMessages == 0, summary, issues);
    }
    
    /**
     * Validates a configuration section for legacy codes.
     */
    private ValidationFileResult validateConfigurationSection(ConfigurationSection section, String path, String fileName) {
        ValidationFileResult result = new ValidationFileResult();
        Set<String> keys = section.getKeys(false);
        
        for (String key : keys) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            
            if (section.isConfigurationSection(key)) {
                ValidationFileResult nestedResult = validateConfigurationSection(section.getConfigurationSection(key), fullPath, fileName);
                result.totalMessages += nestedResult.totalMessages;
                result.legacyMessages += nestedResult.legacyMessages;
                result.issues.addAll(nestedResult.issues);
            } else if (section.isString(key)) {
                result.totalMessages++;
                String value = section.getString(key);
                if (value != null && LegacyToMiniMessageConverter.containsLegacyCodes(value)) {
                    result.legacyMessages++;
                    result.issues.add("LEGACY CODE in " + fileName + " at " + fullPath + ": " + value);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Gets the conversion report from the last migration.
     */
    public List<String> getConversionReport() {
        return new ArrayList<>(conversionReport);
    }
    
    /**
     * Result of a file migration operation.
     */
    public static class MigrationResult {
        private final boolean successful;
        private final int conversionCount;
        private final String errorMessage;
        private final List<String> details;
        
        public MigrationResult(boolean successful, int conversionCount, String errorMessage, List<String> details) {
            this.successful = successful;
            this.conversionCount = conversionCount;
            this.errorMessage = errorMessage;
            this.details = details;
        }
        
        public boolean isSuccessful() { return successful; }
        public int getConversionCount() { return conversionCount; }
        public String getErrorMessage() { return errorMessage; }
        public List<String> getDetails() { return details; }
    }
    
    /**
     * Result of file validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String summary;
        private final List<String> issues;
        
        public ValidationResult(boolean valid, String summary) {
            this(valid, summary, new ArrayList<>());
        }
        
        public ValidationResult(boolean valid, String summary, List<String> issues) {
            this.valid = valid;
            this.summary = summary;
            this.issues = issues;
        }
        
        public boolean isValid() { return valid; }
        public String getSummary() { return summary; }
        public List<String> getIssues() { return issues; }
    }
    
    /**
     * Internal result for file validation.
     */
    private static class ValidationFileResult {
        int totalMessages = 0;
        int legacyMessages = 0;
        List<String> issues = new ArrayList<>();
    }
}