package fr.ax_dev.universejobs.storage.migration;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.PlayerJobData;
import fr.ax_dev.universejobs.storage.database.DatabaseDataStorage;
import fr.ax_dev.universejobs.reward.storage.FileRewardStorage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class DataMigrator {
    
    private final UniverseJobs plugin;
    private final DatabaseDataStorage databaseStorage;
    private final File dataFolder;
    private final File backupFolder;

    public DataMigrator(UniverseJobs plugin, DatabaseDataStorage databaseStorage) {
        this.plugin = plugin;
        this.databaseStorage = databaseStorage;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        this.backupFolder = new File(plugin.getDataFolder(), "migration-backup");
    }

    public CompletableFuture<MigrationResult> migrateAllData() {
        return CompletableFuture.supplyAsync(() -> {
            plugin.getLogger().info("Starting data migration from YML to database...");
            
            MigrationResult result = new MigrationResult();
            
            try {
                createBackup();
                
                result.playerDataMigrated = migratePlayerData();
                result.rewardDataMigrated = migrateRewardData();
                
                result.success = true;
                
                plugin.getLogger().info("Data migration completed successfully!");
                plugin.getLogger().info("Player data migrated: " + result.playerDataMigrated);
                plugin.getLogger().info("Reward data migrated: " + result.rewardDataMigrated);
                
            } catch (Exception e) {
                result.success = false;
                result.error = e.getMessage();
                plugin.getLogger().log(Level.SEVERE, "Data migration failed", e);
            }
            
            return result;
        });
    }

    private void createBackup() {
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
        
        File backupDataFolder = new File(backupFolder, "data-" + System.currentTimeMillis());
        if (dataFolder.exists()) {
            copyDirectory(dataFolder, backupDataFolder);
            plugin.getLogger().info("Created backup at: " + backupDataFolder.getAbsolutePath());
        }
    }

    private int migratePlayerData() {
        AtomicInteger migratedCount = new AtomicInteger(0);
        
        if (!dataFolder.exists()) {
            return 0;
        }
        
        File[] playerFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles == null || playerFiles.length == 0) {
            return 0;
        }
        
        plugin.getLogger().info("Migrating " + playerFiles.length + " player data files...");
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (File playerFile : playerFiles) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String fileName = playerFile.getName();
                    String uuidString = fileName.substring(0, fileName.length() - 4);
                    UUID playerId = UUID.fromString(uuidString);
                    
                    FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
                    PlayerJobData data = new PlayerJobData(playerId);
                    data.load(config);
                    
                    databaseStorage.savePlayerDataAsync(playerId, data).join();
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

    private int migrateRewardData() {
        AtomicInteger migratedCount = new AtomicInteger(0);
        
        try {
            FileRewardStorage fileRewardStorage = new FileRewardStorage(plugin);
            fileRewardStorage.initialize();
            
            Set<UUID> allPlayerIds = getAllPlayerIds();
            
            for (UUID playerId : allPlayerIds) {
                Set<String> allClaimedRewards = fileRewardStorage.getAllClaimedRewards(playerId);
                
                for (String rewardKey : allClaimedRewards) {
                    String[] parts = rewardKey.split(":", 2);
                    if (parts.length == 2) {
                        String jobId = parts[0];
                        String rewardId = parts[1];
                        
                        long claimTime = fileRewardStorage.getClaimTime(playerId, jobId, rewardId);
                        if (claimTime == -1) {
                            claimTime = System.currentTimeMillis();
                        }
                        
                        databaseStorage.claimReward(playerId, jobId, rewardId, claimTime);
                        migratedCount.incrementAndGet();
                    }
                }
            }
            
            fileRewardStorage.shutdown();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to migrate reward data", e);
        }
        
        return migratedCount.get();
    }

    private Set<UUID> getAllPlayerIds() {
        Set<UUID> playerIds = new HashSet<>();
        
        if (dataFolder.exists()) {
            File[] playerFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (playerFiles != null) {
                for (File playerFile : playerFiles) {
                    try {
                        String fileName = playerFile.getName();
                        String uuidString = fileName.substring(0, fileName.length() - 4);
                        UUID playerId = UUID.fromString(uuidString);
                        playerIds.add(playerId);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Invalid player file name: " + playerFile.getName(), e);
                    }
                }
            }
        }
        
        return playerIds;
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
        if (!databaseStorage.isHealthy()) {
            return false;
        }
        
        File migrationMarker = new File(plugin.getDataFolder(), ".migrated");
        if (migrationMarker.exists()) {
            return false;
        }
        
        return dataFolder.exists() && dataFolder.listFiles((dir, name) -> name.endsWith(".yml")) != null;
    }

    public void markMigrationComplete() {
        try {
            File migrationMarker = new File(plugin.getDataFolder(), ".migrated");
            migrationMarker.createNewFile();
            plugin.getLogger().info("Migration marked as complete");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create migration marker", e);
        }
    }

    public static class MigrationResult {
        public boolean success = false;
        public int playerDataMigrated = 0;
        public int rewardDataMigrated = 0;
        public String error = null;
        
        public boolean isSuccessful() {
            return success;
        }
        
        public int getTotalMigrated() {
            return playerDataMigrated + rewardDataMigrated;
        }
    }
}