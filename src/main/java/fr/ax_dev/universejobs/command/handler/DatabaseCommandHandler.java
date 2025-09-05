package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.storage.database.DatabaseDataStorage;
import fr.ax_dev.universejobs.storage.migration.DataMigrator;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class DatabaseCommandHandler {

    private final UniverseJobs plugin;

    public DatabaseCommandHandler(UniverseJobs plugin) {
        this.plugin = plugin;
    }

    public boolean handleDatabaseCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /jobs database migrate <old> <new>");
            sender.sendMessage("§cExample: /jobs database migrate sqlite mysql");
            return true;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "migrate":
                return handleMigrateCommand(sender, args);
            default:
                sender.sendMessage("§cUnknown database command: " + subCommand);
                sender.sendMessage("§cAvailable commands: migrate");
                return true;
        }
    }

    private boolean handleMigrateCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /jobs database migrate <old> <new>");
            sender.sendMessage("§cExample: /jobs database migrate sqlite mysql");
            return true;
        }

        String oldType = args[2].toLowerCase();
        String newType = args[3].toLowerCase();

        if (!oldType.equals("sqlite") && !oldType.equals("mysql")) {
            sender.sendMessage("§cInvalid old database type. Use: sqlite or mysql");
            return true;
        }

        if (!newType.equals("sqlite") && !newType.equals("mysql")) {
            sender.sendMessage("§cInvalid new database type. Use: sqlite or mysql");
            return true;
        }

        if (oldType.equals(newType)) {
            sender.sendMessage("§cOld and new database types cannot be the same");
            return true;
        }

        sender.sendMessage("§aStarting database migration from " + oldType + " to " + newType + "...");
        
        plugin.getFoliaManager().runAsync(() -> {
            try {
                if (plugin.isDatabaseEnabled()) {
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
                } else {
                    plugin.getFoliaManager().runNextTick(() -> {
                        sender.sendMessage("§cDatabase is not enabled in configuration");
                    });
                }
                
            } catch (Exception e) {
                plugin.getFoliaManager().runNextTick(() -> {
                    sender.sendMessage("§cMigration failed: " + e.getMessage());
                });
                plugin.getLogger().log(Level.SEVERE, "Migration failed: " + e.getMessage(), e);
            }
        });
        
        return true;
    }

    public List<String> getTabComplete(String[] args) {
        if (args.length == 2) {
            return Collections.singletonList("migrate");
        } else if (args.length == 3 && args[1].equalsIgnoreCase("migrate")) {
            return Arrays.asList("sqlite", "mysql");
        } else if (args.length == 4 && args[1].equalsIgnoreCase("migrate")) {
            return Arrays.asList("sqlite", "mysql");
        }
        return Collections.emptyList();
    }
}