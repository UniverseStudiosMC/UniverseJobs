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

        if (params.equalsIgnoreCase("equippedjobs")) {
            if (player == null) return "0";
            return String.valueOf(getEquippedJobsCount(player));
        }

        if (params.equalsIgnoreCase("currentjobs")) {
            if (player == null) return "No Jobs";
            return getCurrentJobs(player);
        }

        String[] args = params.split("_");
        if (args.length < 1) return null;

        if (args[0].equalsIgnoreCase("global")) {
            return globalLeaderboardPlaceholder.onRequest(player, params);
        }

        if (args[0].equalsIgnoreCase("boost")) {
            return boostPlaceholder.onRequest(player, params);
        }

        if (args[0].equalsIgnoreCase("equippedjobs") && args.length == 2) {
            if (player == null) return "0";
            return String.valueOf(getEquippedJobsOfTypeCount(player, args[1]));
        }

        if (args[0].equalsIgnoreCase("equipped") && args.length >= 2) {
            if (player == null) return "";
            try {
                int index = Integer.parseInt(args[1]);
                var playerData = plugin.getDataStorage().getPlayerData(player.getUniqueId());
                if (playerData == null) return "";
                var jobs = new java.util.ArrayList<>(playerData.getJobs());
                if (index < 1 || index > jobs.size()) return "";
                String jobId = jobs.get(index - 1);
                var job = plugin.getJobManager().getJob(jobId);
                if (job == null) return "";

                if (args.length == 2) {
                    return job.getName();
                }
                if (args.length == 3 && args[2].equalsIgnoreCase("level")) {
                    return String.valueOf(playerData.getLevel(jobId));
                }
                if (args.length == 3 && args[2].equalsIgnoreCase("xp")) {
                    return String.valueOf((long) playerData.getXp(jobId));
                }
                if (args.length == 3 && args[2].equalsIgnoreCase("id")) {
                    return jobId;
                }
            } catch (NumberFormatException e) {
                return "";
            }
            return "";
        }

        if (args[0].equalsIgnoreCase("multiplier") && args.length == 2) {
            if (player == null) return "1.0";
            return String.valueOf(getUsageMultiplier(args[1]));
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

    private int getEquippedJobsCount(OfflinePlayer player) {
        try {
            return plugin.getDataStorage().getPlayerData(player.getUniqueId()).getJobs().size();
        } catch (Exception e) {
            return 0;
        }
    }

    private int getEquippedJobsOfTypeCount(OfflinePlayer player, String jobType) {
        try {
            return (int) plugin.getDataStorage().getPlayerData(player.getUniqueId())
                    .getJobs().stream()
                    .filter(jobId -> jobType.equalsIgnoreCase(jobId))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    private double getUsageMultiplier(String jobId) {
        try {
            double userCount = plugin.getDataStorage().getJobUserCount(jobId);
            return plugin.getJobManager().calculateUsageMultiplier(jobId, userCount);
        } catch (Exception e) {
            return 1.0;
        }
    }

    private String getCurrentJobs(OfflinePlayer player) {
        try {
            var playerData = plugin.getDataStorage().getPlayerData(player.getUniqueId());
            if (playerData == null || playerData.getJobs().isEmpty()) {
                return "No Jobs";
            }

            java.util.List<String> jobNames = new java.util.ArrayList<>();
            for (String jobId : playerData.getJobs()) {
                var job = plugin.getJobManager().getJob(jobId);
                if (job != null) {
                    jobNames.add(job.getName());
                }
            }

            if (jobNames.isEmpty()) {
                return "No Jobs";
            }

            return String.join(", ", jobNames);
        } catch (Exception e) {
            return "No Jobs";
        }
    }
}