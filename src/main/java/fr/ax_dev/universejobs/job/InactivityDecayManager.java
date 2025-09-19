package fr.ax_dev.universejobs.job;

import fr.ax_dev.universejobs.UniverseJobs;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class InactivityDecayManager {

    private final UniverseJobs plugin;
    private final JobManager jobManager;
    private volatile boolean isProcessing = false;

    public InactivityDecayManager(UniverseJobs plugin, JobManager jobManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;
    }

    public void processInactivePlayersAsync() {
        if (!plugin.getConfigManager().isInactivityDecayEnabled()) {
            return;
        }

        if (isProcessing) {
            plugin.getLogger().info("Inactivity decay process already running, skipping...");
            return;
        }

        isProcessing = true;

        CompletableFuture.runAsync(() -> {
            try {
                processInactivePlayers();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error processing inactive players", e);
            } finally {
                isProcessing = false;
            }
        });
    }

    private void processInactivePlayers() {
        long currentTime = System.currentTimeMillis();
        int daysBeforeInactive = plugin.getConfigManager().getDaysBeforeInactive();
        int daysBeforeRemoval = plugin.getConfigManager().getDaysBeforeRemoval();

        long inactiveThreshold = TimeUnit.DAYS.toMillis(daysBeforeInactive);
        long removalThreshold = daysBeforeRemoval > 0 ? TimeUnit.DAYS.toMillis(daysBeforeRemoval) : Long.MAX_VALUE;

        Map<UUID, PlayerJobData> allPlayerData = jobManager.getAllPlayerData();
        int processedCount = 0;
        int decayedCount = 0;
        int removedCount = 0;

        for (Map.Entry<UUID, PlayerJobData> entry : allPlayerData.entrySet()) {
            UUID playerUuid = entry.getKey();
            PlayerJobData data = entry.getValue();

            long lastLogin = data.getLastLogin();
            long timeSinceLogin = currentTime - lastLogin;

            if (timeSinceLogin >= removalThreshold && daysBeforeRemoval > 0) {
                jobManager.removePlayerData(playerUuid);
                removedCount++;
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Removed player data for " + playerUuid + " (inactive for " +
                            TimeUnit.MILLISECONDS.toDays(timeSinceLogin) + " days)");
                }
            } else if (timeSinceLogin >= inactiveThreshold) {
                long inactiveDays = TimeUnit.MILLISECONDS.toDays(timeSinceLogin - inactiveThreshold);
                if (inactiveDays > 0) {
                    applyDecay(data, (int) inactiveDays);
                    decayedCount++;
                }
            }

            processedCount++;

            if (processedCount % 100 == 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    plugin.getLogger().warning("Inactivity decay process interrupted");
                    return;
                }
            }
        }

        if (decayedCount > 0 || removedCount > 0) {
            plugin.getLogger().info("Inactivity decay process completed: " +
                    decayedCount + " players decayed, " + removedCount + " players removed");
        }
    }

    private void applyDecay(PlayerJobData data, int inactiveDays) {
        Set<String> jobs = new HashSet<>(data.getJobs());

        for (String jobId : jobs) {
            String decayType = plugin.getConfigManager().getInactivityDecayType(jobId);
            double decayAmount = plugin.getConfigManager().getInactivityDecayAmount(jobId);
            int minLevel = plugin.getConfigManager().getInactivityDecayMinLevel(jobId);

            if (decayAmount <= 0) {
                continue;
            }

            int currentLevel = data.getLevel(jobId);
            double currentXp = data.getXp(jobId);

            for (int day = 0; day < inactiveDays; day++) {
                switch (decayType.toLowerCase()) {
                    case "percentage":
                        if (currentLevel > minLevel) {
                            double levelLoss = currentLevel * decayAmount;
                            int newLevel = Math.max(minLevel, (int) Math.floor(currentLevel - levelLoss));
                            if (newLevel != currentLevel) {
                                currentLevel = newLevel;
                                data.setLevel(jobId, newLevel);

                                Job job = jobManager.getJob(jobId);
                                if (job != null) {
                                    double requiredXp = job.getXpForLevel(newLevel);
                                    if (currentXp > requiredXp) {
                                        data.setXp(jobId, requiredXp);
                                        currentXp = requiredXp;
                                    }
                                }
                            }
                        }
                        break;

                    case "level":
                        int levelsToLose = (int) decayAmount;
                        int newLevelFixed = Math.max(minLevel, currentLevel - levelsToLose);
                        if (newLevelFixed != currentLevel) {
                            currentLevel = newLevelFixed;
                            data.setLevel(jobId, newLevelFixed);

                            Job job = jobManager.getJob(jobId);
                            if (job != null) {
                                double requiredXp = job.getXpForLevel(newLevelFixed);
                                if (currentXp > requiredXp) {
                                    data.setXp(jobId, requiredXp);
                                    currentXp = requiredXp;
                                }
                            }
                        }
                        break;

                    case "xp":
                        double xpToLose = decayAmount;
                        double newXp = Math.max(0, currentXp - xpToLose);
                        if (newXp != currentXp) {
                            data.setXp(jobId, newXp);
                            currentXp = newXp;

                            Job job = jobManager.getJob(jobId);
                            if (job != null) {
                                int calculatedLevel = job.getLevelFromXp(newXp);
                                if (calculatedLevel < currentLevel && calculatedLevel >= minLevel) {
                                    data.setLevel(jobId, calculatedLevel);
                                    currentLevel = calculatedLevel;
                                }
                            }
                        }
                        break;
                }

                if (currentLevel <= minLevel) {
                    break;
                }
            }
        }
    }

    public void updatePlayerActivity(UUID playerUuid) {
        if (Bukkit.isPrimaryThread()) {
            CompletableFuture.runAsync(() -> updatePlayerActivityAsync(playerUuid));
        } else {
            updatePlayerActivityAsync(playerUuid);
        }
    }

    private void updatePlayerActivityAsync(UUID playerUuid) {
        PlayerJobData data = jobManager.getPlayerData(playerUuid);
        if (data != null) {
            data.setLastLogin(System.currentTimeMillis());
            jobManager.markDataDirty(playerUuid);
        }
    }
}