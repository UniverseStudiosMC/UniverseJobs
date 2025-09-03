package fr.ax_dev.universejobs.placeholder;

import fr.ax_dev.universejobs.UniverseJobs;
import org.bukkit.Bukkit;

public class PlaceholderManager {

    private final UniverseJobs plugin;
    private UniversalPlaceholderExpansion universalPlaceholder;
    private boolean placeholderApiEnabled = false;

    public PlaceholderManager(UniverseJobs plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                universalPlaceholder = new UniversalPlaceholderExpansion(plugin);
                universalPlaceholder.register();

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
                if (universalPlaceholder != null) {
                    universalPlaceholder.unregister();
                }
                // PlaceholderAPI integration disabled
            } catch (Exception e) {
                plugin.getLogger().warning("Error while disabling PlaceholderAPI integration: " + e.getMessage());
            }
        }
    }

    public void clearCache() {
        if (placeholderApiEnabled && universalPlaceholder != null) {
            universalPlaceholder.clearCache();
            plugin.getLogger().info("Placeholder cache cleared.");
        }
    }

    public void clearJobCache(String jobId) {
        if (placeholderApiEnabled && universalPlaceholder != null) {
            universalPlaceholder.clearJobCache(jobId);
            plugin.getLogger().info("Placeholder cache cleared for job: " + jobId);
        }
    }

    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }
}