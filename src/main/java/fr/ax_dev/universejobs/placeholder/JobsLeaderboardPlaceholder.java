package fr.ax_dev.universejobs.placeholder;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.job.Job;
import fr.ax_dev.universejobs.job.JobManager;
import fr.ax_dev.universejobs.job.PlayerJobData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JobsLeaderboardPlaceholder extends PlaceholderExpansion {

    private final UniverseJobs plugin;
    private final JobManager jobManager;
    private final Map<String, List<LeaderboardEntry>> cachedLeaderboards = new ConcurrentHashMap<>();
    private final Map<String, Long> lastCacheUpdate = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 30000; // 30 seconds

    public JobsLeaderboardPlaceholder(UniverseJobs plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    @Override
    public String getIdentifier() {
        return "UniverseJobs";
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

        String[] args = params.split("_");
        if (args.length < 2) return null;

        // Format: UniverseJobs_<job>_<type>_<position/info>
        String jobId = args[0];
        String type = args[1];

        Job job = jobManager.getJob(jobId);
        if (job == null) return "Invalid Job";

        switch (type.toLowerCase()) {
            case "leaderboard":
                return handleLeaderboardPlaceholder(jobId, args);
            case "player":
                return handlePlayerPlaceholder(player, jobId, args);
            case "job":
                return handleJobPlaceholder(job, args);
            default:
                return null;
        }
    }

    private String handleLeaderboardPlaceholder(String jobId, String[] args) {
        // Format: UniverseJobs_<job>_leaderboard_<position>_<info>
        if (args.length < 4) return null;

        try {
            int position = Integer.parseInt(args[2]);
            String info = args[3];

            List<LeaderboardEntry> leaderboard = getJobLeaderboard(jobId);
            if (position < 1 || position > leaderboard.size()) {
                return getEmptyLeaderboardValue(info);
            }

            LeaderboardEntry entry = leaderboard.get(position - 1);
            return formatLeaderboardInfo(entry, info, position);

        } catch (NumberFormatException e) {
            return "Invalid Position";
        }
    }

    private String handlePlayerPlaceholder(OfflinePlayer player, String jobId, String[] args) {
        if (player == null || args.length < 3) return null;

        String info = args[2];
        PlayerJobData playerData = jobManager.getPlayerData(player.getUniqueId());

        switch (info.toLowerCase()) {
            case "level":
                return String.valueOf(playerData.getLevel(jobId));
            case "xp":
                return String.format("%.1f", playerData.getXp(jobId));
            case "rank":
                return String.valueOf(getPlayerRank(player.getUniqueId(), jobId));
            case "progress":
                double[] progress = playerData.getXpProgress(jobId);
                return String.format("%.1f/%.1f", progress[0], progress[1]);
            case "progresspercent":
                double[] progressPercent = playerData.getXpProgress(jobId);
                double percentage = (progressPercent[0] / progressPercent[1]) * 100;
                return String.format("%.1f%%", percentage);
            case "hasjob":
                return String.valueOf(playerData.hasJob(jobId));
            default:
                return null;
        }
    }

    private String handleJobPlaceholder(Job job, String[] args) {
        if (args.length < 3) return null;

        String info = args[2];
        switch (info.toLowerCase()) {
            case "name":
                return job.getName();
            case "description":
                return job.getDescription();
            case "maxlevel":
                return String.valueOf(job.getMaxLevel());
            case "enabled":
                return String.valueOf(job.isEnabled());
            case "playercount":
                return String.valueOf(getJobPlayerCount(job.getId()));
            default:
                return null;
        }
    }

    private List<LeaderboardEntry> getJobLeaderboard(String jobId) {
        String cacheKey = jobId;
        Long lastUpdate = lastCacheUpdate.get(cacheKey);
        long currentTime = System.currentTimeMillis();

        if (lastUpdate != null && (currentTime - lastUpdate) < CACHE_DURATION) {
            return cachedLeaderboards.getOrDefault(cacheKey, new ArrayList<>());
        }

        List<LeaderboardEntry> leaderboard = calculateJobLeaderboard(jobId);
        cachedLeaderboards.put(cacheKey, leaderboard);
        lastCacheUpdate.put(cacheKey, currentTime);

        return leaderboard;
    }

    private List<LeaderboardEntry> calculateJobLeaderboard(String jobId) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        File dataFolder = new File(plugin.getDataFolder(), "data");

        if (!dataFolder.exists()) {
            return entries;
        }

        File[] dataFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (dataFiles == null) {
            return entries;
        }

        for (File dataFile : dataFiles) {
            try {
                String uuidString = dataFile.getName().replace(".yml", "");
                UUID playerUuid = UUID.fromString(uuidString);
                
                PlayerJobData playerData = jobManager.getPlayerData(playerUuid);
                
                if (playerData.hasJob(jobId)) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
                    String playerName = offlinePlayer.getName();
                    
                    if (playerName == null) {
                        playerName = "Unknown Player";
                    }

                    double xp = playerData.getXp(jobId);
                    int level = playerData.getLevel(jobId);

                    entries.add(new LeaderboardEntry(playerUuid, playerName, xp, level));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error processing data file: " + dataFile.getName());
            }
        }

        // Sort by descending level, then by descending XP
        entries.sort((a, b) -> {
            int levelCompare = Integer.compare(b.level, a.level);
            if (levelCompare != 0) {
                return levelCompare;
            }
            return Double.compare(b.xp, a.xp);
        });

        return entries;
    }

    private String formatLeaderboardInfo(LeaderboardEntry entry, String info, int position) {
        switch (info.toLowerCase()) {
            case "player":
            case "name":
                return entry.playerName;
            case "level":
                return String.valueOf(entry.level);
            case "xp":
                return String.format("%.1f", entry.xp);
            case "position":
            case "rank":
                return String.valueOf(position);
            case "formatted":
                return String.format("#%d %s - Level %d (%.1f XP)", 
                    position, entry.playerName, entry.level, entry.xp);
            default:
                return "Invalid Info";
        }
    }

    private String getEmptyLeaderboardValue(String info) {
        switch (info.toLowerCase()) {
            case "player":
            case "name":
                return "No Player";
            case "level":
                return "0";
            case "xp":
                return "0.0";
            case "position":
            case "rank":
                return "0";
            case "formatted":
                return "No player in ranking";
            default:
                return "N/A";
        }
    }

    private int getPlayerRank(UUID playerUuid, String jobId) {
        List<LeaderboardEntry> leaderboard = getJobLeaderboard(jobId);
        
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).playerUuid.equals(playerUuid)) {
                return i + 1;
            }
        }
        
        return 0; // Player not found in ranking
    }

    private int getJobPlayerCount(String jobId) {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            return 0;
        }

        File[] dataFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (dataFiles == null) {
            return 0;
        }

        int count = 0;
        for (File dataFile : dataFiles) {
            try {
                String uuidString = dataFile.getName().replace(".yml", "");
                UUID playerUuid = UUID.fromString(uuidString);
                
                PlayerJobData playerData = jobManager.getPlayerData(playerUuid);
                if (playerData.hasJob(jobId)) {
                    count++;
                }
            } catch (Exception e) {
                // Ignore errors and continue
            }
        }

        return count;
    }

    public void clearCache() {
        cachedLeaderboards.clear();
        lastCacheUpdate.clear();
    }

    public void clearJobCache(String jobId) {
        cachedLeaderboards.remove(jobId);
        lastCacheUpdate.remove(jobId);
    }

    private static class LeaderboardEntry {
        final UUID playerUuid;
        final String playerName;
        final double xp;
        final int level;

        LeaderboardEntry(UUID playerUuid, String playerName, double xp, int level) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.xp = xp;
            this.level = level;
        }
    }
}