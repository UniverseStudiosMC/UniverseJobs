package fr.ax_dev.universejobs.placeholder;

import fr.ax_dev.universejobs.UniverseJobs;
import org.bukkit.Bukkit;

public class PlaceholderManager {

    private final UniverseJobs plugin;
    private JobsLeaderboardPlaceholder jobsLeaderboardPlaceholder;
    private GlobalLeaderboardPlaceholder globalLeaderboardPlaceholder;
    private boolean placeholderApiEnabled = false;

    public PlaceholderManager(UniverseJobs plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                jobsLeaderboardPlaceholder = new JobsLeaderboardPlaceholder(plugin);
                globalLeaderboardPlaceholder = new GlobalLeaderboardPlaceholder(plugin);

                jobsLeaderboardPlaceholder.register();
                globalLeaderboardPlaceholder.register();

                placeholderApiEnabled = true;
                // PlaceholderAPI integration enabled

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to initialize PlaceholderAPI integration: " + e.getMessage());
            }
        } else {
            plugin.getLogger().warning("PlaceholderAPI not found. Placeholder features will be disabled.");
        }
    }

    public void shutdown() {
        if (placeholderApiEnabled) {
            try {
                if (jobsLeaderboardPlaceholder != null) {
                    jobsLeaderboardPlaceholder.unregister();
                }
                if (globalLeaderboardPlaceholder != null) {
                    globalLeaderboardPlaceholder.unregister();
                }
                // PlaceholderAPI integration disabled
            } catch (Exception e) {
                plugin.getLogger().warning("Error while disabling PlaceholderAPI integration: " + e.getMessage());
            }
        }
    }

    public void clearCache() {
        if (placeholderApiEnabled) {
            if (jobsLeaderboardPlaceholder != null) {
                jobsLeaderboardPlaceholder.clearCache();
            }
            if (globalLeaderboardPlaceholder != null) {
                globalLeaderboardPlaceholder.clearCache();
            }
            plugin.getLogger().info("Placeholder cache cleared.");
        }
    }

    public void clearJobCache(String jobId) {
        if (placeholderApiEnabled && jobsLeaderboardPlaceholder != null) {
            jobsLeaderboardPlaceholder.clearJobCache(jobId);
            plugin.getLogger().info("Placeholder cache cleared for job: " + jobId);
        }
    }

    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }

    public JobsLeaderboardPlaceholder getJobsLeaderboardPlaceholder() {
        return jobsLeaderboardPlaceholder;
    }

    public GlobalLeaderboardPlaceholder getGlobalLeaderboardPlaceholder() {
        return globalLeaderboardPlaceholder;
    }
}