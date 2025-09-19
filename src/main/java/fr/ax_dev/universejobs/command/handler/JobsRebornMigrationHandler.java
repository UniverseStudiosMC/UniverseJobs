package fr.ax_dev.universejobs.command.handler;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.storage.database.DatabaseDataStorage;
import fr.ax_dev.universejobs.storage.migration.JobsRebornConverter;
import fr.ax_dev.universejobs.storage.migration.JobsRebornDataMigrator;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class JobsRebornMigrationHandler {

    private final UniverseJobs plugin;

    public JobsRebornMigrationHandler(UniverseJobs plugin) {
        this.plugin = plugin;
    }

    public boolean handleMigrationCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage:");
            sender.sendMessage("§c/jobs migrate jobs [job-name] - Convert job configurations from JobsReborn");
            sender.sendMessage("§c/jobs migrate data - Migrate player data from JobsReborn");
            sender.sendMessage("§c/jobs migrate all - Convert jobs and migrate data");
            return true;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "jobs":
                String specificJob = args.length > 2 ? args[2] : null;
                return handleJobsMigration(sender, specificJob);
            case "data":
                return handleDataMigration(sender);
            case "all":
                return handleFullMigration(sender);
            default:
                sender.sendMessage("§cUnknown migration command: " + subCommand);
                sender.sendMessage("§cAvailable commands: jobs, data, all");
                return true;
        }
    }

    private boolean handleJobsMigration(CommandSender sender, String specificJob) {
        if (specificJob != null) {
            sender.sendMessage("§aStarting JobsReborn job conversion for: " + specificJob);
        } else {
            sender.sendMessage("§aStarting JobsReborn jobs conversion...");
        }

        plugin.getFoliaManager().runAsync(() -> {
            try {
                JobsRebornConverter converter = new JobsRebornConverter(plugin);
                JobsRebornConverter.ConversionResult result = converter.convertJobs(specificJob);

                plugin.getFoliaManager().runNextTick(() -> {
                    if (result.isSuccessful()) {
                        if (specificJob != null) {
                            sender.sendMessage("§aJob conversion completed successfully!");
                        } else {
                            sender.sendMessage("§aJobs conversion completed successfully!");
                        }
                        sender.sendMessage("§aConverted jobs: " + result.jobsConverted);

                        if (!result.convertedJobs.isEmpty()) {
                            sender.sendMessage("§aConverted jobs: §f" + String.join("§a, §f", result.convertedJobs));
                        }

                        if (!result.failedJobs.isEmpty()) {
                            sender.sendMessage("§cFailed to convert some jobs:");
                            for (String failed : result.failedJobs) {
                                sender.sendMessage("§c- " + failed);
                            }
                        }

                        sender.sendMessage("§e/jobs admin reload to load the new job configurations.");
                    } else {
                        sender.sendMessage("§cJobs conversion failed: " + result.error);
                    }
                });

            } catch (Exception e) {
                plugin.getFoliaManager().runNextTick(() -> {
                    sender.sendMessage("§cJobs conversion failed: " + e.getMessage());
                });
                plugin.getLogger().log(Level.SEVERE, "Jobs conversion failed", e);
            }
        });

        return true;
    }

    private boolean handleDataMigration(CommandSender sender) {
        if (!plugin.isDatabaseEnabled()) {
            sender.sendMessage("§cDatabase is not enabled. Data migration requires database storage.");
            return true;
        }

        DatabaseDataStorage storage = (DatabaseDataStorage) plugin.getDataStorage();
        JobsRebornDataMigrator migrator = new JobsRebornDataMigrator(plugin, storage);

        if (!migrator.shouldMigrate()) {
            sender.sendMessage("§cNo JobsReborn data found or migration already completed.");
            return true;
        }

        sender.sendMessage("§aStarting JobsReborn player data migration...");

        plugin.getFoliaManager().runAsync(() -> {
            try {
                JobsRebornDataMigrator.MigrationResult result = migrator.migrateAllData().join();

                plugin.getFoliaManager().runNextTick(() -> {
                    if (result.isSuccessful()) {
                        sender.sendMessage("§aData migration completed successfully!");
                        sender.sendMessage("§aPlayer data migrated: " + result.playerDataMigrated);
                        migrator.markMigrationComplete();
                    } else {
                        sender.sendMessage("§cData migration failed: " + result.error);
                    }
                });

            } catch (Exception e) {
                plugin.getFoliaManager().runNextTick(() -> {
                    sender.sendMessage("§cData migration failed: " + e.getMessage());
                });
                plugin.getLogger().log(Level.SEVERE, "Data migration failed", e);
            }
        });

        return true;
    }

    private boolean handleFullMigration(CommandSender sender) {
        sender.sendMessage("§aStarting full JobsReborn migration (jobs + data)...");

        plugin.getFoliaManager().runAsync(() -> {
            try {
                JobsRebornConverter converter = new JobsRebornConverter(plugin);
                JobsRebornConverter.ConversionResult jobsResult = converter.convertJobs(null); // null = all jobs

                CompletableFuture<JobsRebornDataMigrator.MigrationResult> dataFuture;

                if (plugin.isDatabaseEnabled()) {
                    DatabaseDataStorage storage = (DatabaseDataStorage) plugin.getDataStorage();
                    JobsRebornDataMigrator migrator = new JobsRebornDataMigrator(plugin, storage);

                    if (migrator.shouldMigrate()) {
                        dataFuture = migrator.migrateAllData();
                    } else {
                        dataFuture = CompletableFuture.completedFuture(null);
                    }
                } else {
                    dataFuture = CompletableFuture.completedFuture(null);
                }

                JobsRebornDataMigrator.MigrationResult dataResult = dataFuture.join();

                plugin.getFoliaManager().runNextTick(() -> {
                    sender.sendMessage("§a=== JobsReborn Migration Results ===");

                    if (jobsResult.isSuccessful()) {
                        sender.sendMessage("§aJobs conversion: §2SUCCESS");
                        sender.sendMessage("§aConverted jobs: " + jobsResult.jobsConverted);

                        if (!jobsResult.convertedJobs.isEmpty()) {
                            sender.sendMessage("§aJob IDs: §f" + String.join("§a, §f", jobsResult.convertedJobs));
                        }
                    } else {
                        sender.sendMessage("§cJobs conversion: §4FAILED");
                        sender.sendMessage("§cError: " + jobsResult.error);
                    }

                    if (dataResult != null) {
                        if (dataResult.isSuccessful()) {
                            sender.sendMessage("§aData migration: §2SUCCESS");
                            sender.sendMessage("§aPlayer data migrated: " + dataResult.playerDataMigrated);

                            DatabaseDataStorage storage = (DatabaseDataStorage) plugin.getDataStorage();
                            JobsRebornDataMigrator migrator = new JobsRebornDataMigrator(plugin, storage);
                            migrator.markMigrationComplete();
                        } else {
                            sender.sendMessage("§cData migration: §4FAILED");
                            sender.sendMessage("§cError: " + dataResult.error);
                        }
                    } else {
                        sender.sendMessage("§eData migration: §6SKIPPED");
                        sender.sendMessage("§e(Database not enabled or no data found)");
                    }

                    if (!jobsResult.failedJobs.isEmpty()) {
                        sender.sendMessage("§cSome jobs failed to convert:");
                        for (String failed : jobsResult.failedJobs) {
                            sender.sendMessage("§c- " + failed);
                        }
                    }

                    sender.sendMessage("§eRestart the server to load the new configurations.");
                });

            } catch (Exception e) {
                plugin.getFoliaManager().runNextTick(() -> {
                    sender.sendMessage("§cFull migration failed: " + e.getMessage());
                });
                plugin.getLogger().log(Level.SEVERE, "Full migration failed", e);
            }
        });

        return true;
    }

    public List<String> getTabComplete(String[] args) {
        if (args.length == 2) {
            return Arrays.asList("jobs", "data", "all");
        }
        return Collections.emptyList();
    }
}