package fr.ax_dev.universejobs.storage.migration;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.storage.database.DatabaseDataStorage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class JobsRebornDataMigrator {

    private final UniverseJobs plugin;
    private final DatabaseDataStorage databaseStorage;
    private final File jobsRebornFolder;
    private final File backupFolder;

    public JobsRebornDataMigrator(UniverseJobs plugin, DatabaseDataStorage databaseStorage) {
        this.plugin = plugin;
        this.databaseStorage = databaseStorage;

        File jobsPluginFolder = null;
        if (plugin.getServer().getPluginManager().getPlugin("Jobs") != null) {
            jobsPluginFolder = plugin.getServer().getPluginManager().getPlugin("Jobs").getDataFolder();
        } else {
            jobsPluginFolder = new File(plugin.getDataFolder().getParent(), "Jobs");
        }
        this.jobsRebornFolder = jobsPluginFolder;

        this.backupFolder = new File(plugin.getDataFolder(), "jobsreborn-migration-backup");
    }

    public CompletableFuture<MigrationResult> migrateAllData() {
        return CompletableFuture.supplyAsync(() -> {
            plugin.getLogger().info("Starting data migration from JobsReborn...");

            MigrationResult result = new MigrationResult();

            try {
                createBackup();

                if (hasJobsRebornDatabase()) {
                    result.playerDataMigrated = migrateDatabaseData();
                } else {
                    result.playerDataMigrated = migrateFileData();
                }

                result.success = true;

                plugin.getLogger().info("JobsReborn data migration completed successfully!");
                plugin.getLogger().info("Player data migrated: " + result.playerDataMigrated);

            } catch (Exception e) {
                result.success = false;
                result.error = e.getMessage();
                plugin.getLogger().log(Level.SEVERE, "JobsReborn data migration failed", e);
            }

            return result;
        });
    }

    private void createBackup() {
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        File backupDataFolder = new File(backupFolder, "jobsreborn-data-" + System.currentTimeMillis());
        if (jobsRebornFolder.exists()) {
            copyDirectory(jobsRebornFolder, backupDataFolder);
            plugin.getLogger().info("Created JobsReborn backup at: " + backupDataFolder.getAbsolutePath());
        }
    }

    private boolean hasJobsRebornDatabase() {
        File configFile = new File(jobsRebornFolder, "generalConfig.yml");
        if (!configFile.exists()) {
            configFile = new File(jobsRebornFolder, "config.yml");
        }

        if (configFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            String storageMethod = config.getString("storage-method", "file");
            return storageMethod.equalsIgnoreCase("mysql") || storageMethod.equalsIgnoreCase("sqlite");
        }

        return false;
    }

    private int migrateDatabaseData() {
        AtomicInteger migratedCount = new AtomicInteger(0);

        try {
            Connection connection = getJobsRebornConnection();
            if (connection == null) {
                plugin.getLogger().warning("Could not connect to JobsReborn database");
                return 0;
            }

            String query = "SELECT * FROM jobs_users";
            try (PreparedStatement stmt = connection.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                List<CompletableFuture<Void>> futures = new ArrayList<>();

                while (rs.next()) {
                    UUID playerId = getPlayerUUID(rs);
                    if (playerId == null) continue;

                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            PlayerJobData playerData = convertDatabasePlayerData(rs, playerId);
                            databaseStorage.savePlayerDataAsync(playerId, playerData).join();
                            migratedCount.incrementAndGet();
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Failed to migrate player data for: " + playerId, e);
                        }
                    });

                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            } finally {
                connection.close();
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Database migration failed", e);
        }

        return migratedCount.get();
    }

    private int migrateFileData() {
        AtomicInteger migratedCount = new AtomicInteger(0);

        File playersFolder = new File(jobsRebornFolder, "players");
        if (!playersFolder.exists() || !playersFolder.isDirectory()) {
            plugin.getLogger().info("No JobsReborn player data folder found");
            return 0;
        }

        File[] playerFiles = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles == null || playerFiles.length == 0) {
            plugin.getLogger().info("No JobsReborn player files found");
            return 0;
        }

        plugin.getLogger().info("Migrating " + playerFiles.length + " JobsReborn player files...");

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (File playerFile : playerFiles) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String fileName = playerFile.getName();
                    String uuidString = fileName.substring(0, fileName.length() - 4);
                    UUID playerId = UUID.fromString(uuidString);

                    FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
                    PlayerJobData playerData = convertFilePlayerData(config, playerId);

                    databaseStorage.savePlayerDataAsync(playerId, playerData).join();
                    migratedCount.incrementAndGet();

                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to migrate player file: " + playerFile.getName(), e);
                }
            });

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return migratedCount.get();
    }

    private Connection getJobsRebornConnection() {
        File configFile = new File(jobsRebornFolder, "generalConfig.yml");
        if (!configFile.exists()) {
            configFile = new File(jobsRebornFolder, "config.yml");
        }

        if (!configFile.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String storageMethod = config.getString("storage-method", "file");

        try {
            if (storageMethod.equalsIgnoreCase("mysql")) {
                String host = config.getString("mysql-hostname", "localhost");
                int port = config.getInt("mysql-port", 3306);
                String database = config.getString("mysql-database", "minecraft");
                String username = config.getString("mysql-username", "root");
                String password = config.getString("mysql-password", "");

                String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true";
                return DriverManager.getConnection(url, username, password);

            } else if (storageMethod.equalsIgnoreCase("sqlite")) {
                File sqliteFile = new File(jobsRebornFolder, "jobs.sqlite.db");
                if (!sqliteFile.exists()) {
                    sqliteFile = new File(jobsRebornFolder, "jobs.db");
                }

                if (sqliteFile.exists()) {
                    String url = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
                    return DriverManager.getConnection(url);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to JobsReborn database", e);
        }

        return null;
    }

    private UUID getPlayerUUID(ResultSet rs) throws SQLException {
        try {
            String uuidString = rs.getString("player_uuid");
            if (uuidString != null && !uuidString.isEmpty()) {
                return UUID.fromString(uuidString);
            }
        } catch (Exception e) {
        }

        try {
            String username = rs.getString("username");
            if (username != null && !username.isEmpty()) {
                return plugin.getServer().getOfflinePlayer(username).getUniqueId();
            }
        } catch (Exception e) {
        }

        return null;
    }

    private PlayerJobData convertDatabasePlayerData(ResultSet rs, UUID playerId) throws SQLException {
        PlayerJobData playerData = new PlayerJobData(playerId);

        try {
            ConfigurationSection jobsSection = null;
            String jobsData = rs.getString("jobs");
            if (jobsData != null && !jobsData.isEmpty()) {
                FileConfiguration tempConfig = new YamlConfiguration();
                tempConfig.loadFromString(jobsData);
                jobsSection = tempConfig.getConfigurationSection("jobs");
            }

            if (jobsSection != null) {
                convertJobsData(jobsSection, playerData);
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing jobs data for player: " + playerId, e);
        }

        return playerData;
    }

    private PlayerJobData convertFilePlayerData(FileConfiguration config, UUID playerId) {
        PlayerJobData playerData = new PlayerJobData(playerId);

        ConfigurationSection jobsSection = config.getConfigurationSection("jobs");
        if (jobsSection != null) {
            convertJobsData(jobsSection, playerData);
        }

        return playerData;
    }

    private void convertJobsData(ConfigurationSection jobsSection, PlayerJobData playerData) {
        for (String jobName : jobsSection.getKeys(false)) {
            ConfigurationSection jobSection = jobsSection.getConfigurationSection(jobName);
            if (jobSection == null) continue;

            String jobId = jobName.toLowerCase();
            int level = jobSection.getInt("level", 1);
            double experience = jobSection.getDouble("experience", 0.0);

            playerData.joinJob(jobId);
            playerData.setLevel(jobId, level);
            playerData.setXp(jobId, experience);
        }
    }

    private void copyDirectory(File source, File destination) {
        try {
            if (source.isDirectory()) {
                destination.mkdirs();

                File[] files = source.listFiles();
                if (files != null) {
                    for (File file : files) {
                        File destFile = new File(destination, file.getName());
                        copyDirectory(file, destFile);
                    }
                }
            } else {
                java.nio.file.Files.copy(source.toPath(), destination.toPath());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to copy file: " + source.getName(), e);
        }
    }

    public boolean shouldMigrate() {
        File migrationMarker = new File(plugin.getDataFolder(), ".jobsreborn-migrated");
        if (migrationMarker.exists()) {
            return false;
        }

        return jobsRebornFolder.exists() &&
               (hasJobsRebornDatabase() || new File(jobsRebornFolder, "players").exists());
    }

    public void markMigrationComplete() {
        try {
            File migrationMarker = new File(plugin.getDataFolder(), ".jobsreborn-migrated");
            migrationMarker.createNewFile();
            plugin.getLogger().info("JobsReborn migration marked as complete");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create JobsReborn migration marker", e);
        }
    }

    public static class MigrationResult {
        public boolean success = false;
        public int playerDataMigrated = 0;
        public String error = null;

        public boolean isSuccessful() {
            return success;
        }

        public int getTotalMigrated() {
            return playerDataMigrated;
        }
    }
}