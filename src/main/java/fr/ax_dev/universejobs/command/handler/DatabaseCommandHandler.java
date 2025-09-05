package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.storage.database.DatabaseDataStorage;
import fr.ax_dev.universejobs.storage.database.DatabaseTest;
import fr.ax_dev.universejobs.storage.migration.DataMigrator;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public class DatabaseCommandHandler {
    
    private final UniverseJobs plugin;

    public DatabaseCommandHandler(UniverseJobs plugin) {
        this.plugin = plugin;
    }

    public boolean handleDatabaseCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /jobs database <test|migrate|stats|health>");
            return true;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "test":
                return handleTestCommand(sender, args);
            case "migrate":
                return handleMigrateCommand(sender, args);
            case "stats":
                return handleStatsCommand(sender, args);
            case "health":
                return handleHealthCommand(sender, args);
            default:
                sender.sendMessage("§cUnknown database command: " + subCommand);
                sender.sendMessage("§cAvailable commands: test, migrate, stats, health");
                return true;
        }
    }

    private boolean handleTestCommand(CommandSender sender, String[] args) {
        if (!plugin.isDatabaseEnabled()) {
            sender.sendMessage("§cDatabase is not enabled in configuration");
            return true;
        }

        sender.sendMessage("§aStarting database integration test...");
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                DatabaseTest test = new DatabaseTest(plugin);
                test.testDatabaseIntegration();
                
                plugin.getFoliaManager().runNextTick(() -> {
                    sender.sendMessage("§aDatabase integration test completed successfully!");
                });
                
                if (args.length > 2 && args[2].equalsIgnoreCase("performance")) {
                    test.performanceTest();
                    plugin.getFoliaManager().runNextTick(() -> {
                        sender.sendMessage("§aDatabase performance test completed!");
                    });
                }
                
            } catch (Exception e) {
                plugin.getFoliaManager().runNextTick(() -> {
                    sender.sendMessage("§cDatabase test failed: " + e.getMessage());
                });
                plugin.getLogger().severe("Database test failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        return true;
    }

    private boolean handleMigrateCommand(CommandSender sender, String[] args) {
        if (!plugin.isDatabaseEnabled()) {
            sender.sendMessage("§cDatabase is not enabled in configuration");
            return true;
        }

        sender.sendMessage("§aStarting data migration...");
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                DatabaseDataStorage storage = (DatabaseDataStorage) plugin.getDataStorage();
                DataMigrator migrator = new DataMigrator(plugin, storage);
                
                DataMigrator.MigrationResult result = migrator.migrateAllData().join();
                
                plugin.getFoliaManager().runNextTick(() -> {
                    if (result.isSuccessful()) {
                        sender.sendMessage("§aMigration completed successfully!");
                        sender.sendMessage("§aPlayer data migrated: " + result.playerDataMigrated);
                        sender.sendMessage("§aReward data migrated: " + result.rewardDataMigrated);
                        sender.sendMessage("§aTotal records migrated: " + result.getTotalMigrated());
                        migrator.markMigrationComplete();
                    } else {
                        sender.sendMessage("§cMigration failed: " + result.error);
                    }
                });
                
            } catch (Exception e) {
                plugin.getFoliaManager().runNextTick(() -> {
                    sender.sendMessage("§cMigration failed: " + e.getMessage());
                });
                plugin.getLogger().severe("Migration failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        return true;
    }

    private boolean handleStatsCommand(CommandSender sender, String[] args) {
        if (!plugin.isDatabaseEnabled()) {
            sender.sendMessage("§cDatabase is not enabled in configuration");
            return true;
        }

        try {
            DatabaseDataStorage storage = (DatabaseDataStorage) plugin.getDataStorage();
            
            sender.sendMessage("§6=== Database Statistics ===");
            
            var cacheStats = storage.getCacheStats();
            sender.sendMessage("§aCached players: §f" + cacheStats.get("cached_players"));
            sender.sendMessage("§aCached rewards: §f" + cacheStats.get("cached_rewards"));
            sender.sendMessage("§aCache hits: §f" + cacheStats.get("cache_hits"));
            sender.sendMessage("§aCache misses: §f" + cacheStats.get("cache_misses"));
            sender.sendMessage("§aHit ratio: §f" + String.format("%.2f%%", (Double) cacheStats.get("hit_ratio") * 100));
            
            var perfMetrics = storage.getPerformanceMetrics();
            sender.sendMessage("§bTotal operations: §f" + perfMetrics.get("total_operations"));
            sender.sendMessage("§bActive connections: §f" + perfMetrics.get("pool_active"));
            sender.sendMessage("§bIdle connections: §f" + perfMetrics.get("pool_idle"));
            
        } catch (Exception e) {
            sender.sendMessage("§cFailed to retrieve database stats: " + e.getMessage());
        }
        
        return true;
    }

    private boolean handleHealthCommand(CommandSender sender, String[] args) {
        if (!plugin.isDatabaseEnabled()) {
            sender.sendMessage("§cDatabase is not enabled in configuration");
            return true;
        }

        try {
            DatabaseDataStorage storage = (DatabaseDataStorage) plugin.getDataStorage();
            
            sender.sendMessage("§6=== Database Health ===");
            
            var healthInfo = storage.getHealthInfo();
            boolean isHealthy = storage.isHealthy();
            
            sender.sendMessage("§aOverall status: " + (isHealthy ? "§aHealthy" : "§cUnhealthy"));
            sender.sendMessage("§aInitialized: §f" + healthInfo.get("initialized"));
            sender.sendMessage("§aShutdown: §f" + healthInfo.get("shutdown"));
            sender.sendMessage("§aPool initialized: §f" + healthInfo.get("pool_initialized"));
            sender.sendMessage("§aDatabase type: §f" + healthInfo.get("database_type"));
            sender.sendMessage("§aCache size: §f" + healthInfo.get("cache_size"));
            
        } catch (Exception e) {
            sender.sendMessage("§cFailed to retrieve database health: " + e.getMessage());
        }
        
        return true;
    }

    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("test", "migrate", "stats", "health");
        } else if (args.length == 3 && args[1].equalsIgnoreCase("test")) {
            return Arrays.asList("performance");
        }
        return Arrays.asList();
    }
}