package fr.ax_dev.universejobs.placeholder;

import fr.ax_dev.universejobs.UniverseJobs;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class UniversalPlaceholderExpansion extends PlaceholderExpansion {

    private final UniverseJobs plugin;
    private final JobsLeaderboardPlaceholder jobsLeaderboardPlaceholder;
    private final GlobalLeaderboardPlaceholder globalLeaderboardPlaceholder;
    private final BoostPlaceholder boostPlaceholder;

    public UniversalPlaceholderExpansion(UniverseJobs plugin) {
        this.plugin = plugin;
        this.jobsLeaderboardPlaceholder = new JobsLeaderboardPlaceholder(plugin);
        this.globalLeaderboardPlaceholder = new GlobalLeaderboardPlaceholder(plugin);
        this.boostPlaceholder = new BoostPlaceholder(plugin);
    }

    @Override
    public String getIdentifier() {
        return "universejobs";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) return null;

        if (params.equalsIgnoreCase("totaljobs")) {
            return String.valueOf(plugin.getJobManager().getAllJobs().size());
        }

        String[] args = params.split("_");
        if (args.length < 1) return null;

        if (args[0].equalsIgnoreCase("global")) {
            return globalLeaderboardPlaceholder.onRequest(player, params);
        }
        
        if (args[0].equalsIgnoreCase("boost")) {
            return boostPlaceholder.onRequest(player, params);
        }

        return jobsLeaderboardPlaceholder.onRequest(player, params);
    }

    public void clearCache() {
        jobsLeaderboardPlaceholder.clearCache();
        globalLeaderboardPlaceholder.clearCache();
    }

    public void clearJobCache(String jobId) {
        jobsLeaderboardPlaceholder.clearJobCache(jobId);
    }
}