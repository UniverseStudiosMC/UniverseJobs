package fr.ax_dev.jobsAdventure.placeholder;

import fr.ax_dev.jobsAdventure.JobsAdventure;
import fr.ax_dev.jobsAdventure.job.JobManager;
import fr.ax_dev.jobsAdventure.job.PlayerJobData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalLeaderboardPlaceholder extends PlaceholderExpansion {

    private final JobsAdventure plugin;
    private final JobManager jobManager;
    private final Map<String, List<GlobalLeaderboardEntry>> cachedGlobalLeaderboards = new ConcurrentHashMap<>();
    private final Map<String, Long> lastCacheUpdate = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 60000; // 1 minute pour les classements globaux

    public GlobalLeaderboardPlaceholder(JobsAdventure plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    @Override
    public String getIdentifier() {
        return "jobsglobal";
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

        String type = args[0];
        
        switch (type.toLowerCase()) {
            case "totallevels":
                return handleTotalLevelsLeaderboard(args);
            case "totaljobs":
                return handleTotalJobsLeaderboard(args);
            case "totalxp":
                return handleTotalXpLeaderboard(args);
            case "player":
                return handleGlobalPlayerStats(player, args);
            default:
                return null;
        }
    }

    private String handleTotalLevelsLeaderboard(String[] args) {
        // Format: jobsglobal_totallevels_<position>_<info>
        if (args.length < 3) return null;

        try {
            int position = Integer.parseInt(args[1]);
            String info = args.length > 2 ? args[2] : "formatted";

            List<GlobalLeaderboardEntry> leaderboard = getGlobalLeaderboard("totallevels");
            if (position < 1 || position > leaderboard.size()) {
                return getEmptyGlobalValue(info);
            }

            GlobalLeaderboardEntry entry = leaderboard.get(position - 1);
            return formatGlobalLeaderboardInfo(entry, info, position, "levels");

        } catch (NumberFormatException e) {
            return "Invalid Position";
        }
    }

    private String handleTotalJobsLeaderboard(String[] args) {
        // Format: jobsglobal_totaljobs_<position>_<info>
        if (args.length < 3) return null;

        try {
            int position = Integer.parseInt(args[1]);
            String info = args.length > 2 ? args[2] : "formatted";

            List<GlobalLeaderboardEntry> leaderboard = getGlobalLeaderboard("totaljobs");
            if (position < 1 || position > leaderboard.size()) {
                return getEmptyGlobalValue(info);
            }

            GlobalLeaderboardEntry entry = leaderboard.get(position - 1);
            return formatGlobalLeaderboardInfo(entry, info, position, "jobs");

        } catch (NumberFormatException e) {
            return "Invalid Position";
        }
    }

    private String handleTotalXpLeaderboard(String[] args) {
        // Format: jobsglobal_totalxp_<position>_<info>
        if (args.length < 3) return null;

        try {
            int position = Integer.parseInt(args[1]);
            String info = args.length > 2 ? args[2] : "formatted";

            List<GlobalLeaderboardEntry> leaderboard = getGlobalLeaderboard("totalxp");
            if (position < 1 || position > leaderboard.size()) {
                return getEmptyGlobalValue(info);
            }

            GlobalLeaderboardEntry entry = leaderboard.get(position - 1);
            return formatGlobalLeaderboardInfo(entry, info, position, "XP");

        } catch (NumberFormatException e) {
            return "Invalid Position";
        }
    }

    private String handleGlobalPlayerStats(OfflinePlayer player, String[] args) {
        if (player == null || args.length < 2) return null;

        String stat = args[1];
        PlayerJobData playerData = jobManager.getPlayerData(player.getUniqueId());

        switch (stat.toLowerCase()) {
            case "totallevels":
                return String.valueOf(calculateTotalLevels(playerData));
            case "totaljobs":
                return String.valueOf(playerData.getJobs().size());
            case "totalxp":
                return String.format("%.1f", calculateTotalXp(playerData));
            case "rank":
                if (args.length < 3) return null;
                String rankType = args[2];
                return String.valueOf(getGlobalPlayerRank(player.getUniqueId(), rankType));
            case "avgLevel":
                return String.format("%.1f", calculateAverageLevel(playerData));
            default:
                return null;
        }
    }

    private List<GlobalLeaderboardEntry> getGlobalLeaderboard(String type) {
        String cacheKey = type;
        Long lastUpdate = lastCacheUpdate.get(cacheKey);
        long currentTime = System.currentTimeMillis();

        if (lastUpdate != null && (currentTime - lastUpdate) < CACHE_DURATION) {
            return cachedGlobalLeaderboards.getOrDefault(cacheKey, new ArrayList<>());
        }

        List<GlobalLeaderboardEntry> leaderboard = calculateGlobalLeaderboard(type);
        cachedGlobalLeaderboards.put(cacheKey, leaderboard);
        lastCacheUpdate.put(cacheKey, currentTime);

        return leaderboard;
    }

    private List<GlobalLeaderboardEntry> calculateGlobalLeaderboard(String type) {
        List<GlobalLeaderboardEntry> entries = new ArrayList<>();
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
                
                if (playerData.getJobs().isEmpty()) {
                    continue; // Skip players without jobs
                }

                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
                String playerName = offlinePlayer.getName();
                
                if (playerName == null) {
                    playerName = "Unknown Player";
                }

                double value = 0;
                switch (type) {
                    case "totallevels":
                        value = calculateTotalLevels(playerData);
                        break;
                    case "totaljobs":
                        value = playerData.getJobs().size();
                        break;
                    case "totalxp":
                        value = calculateTotalXp(playerData);
                        break;
                }

                entries.add(new GlobalLeaderboardEntry(playerUuid, playerName, value));

            } catch (Exception e) {
                plugin.getLogger().warning("Error processing global data file: " + dataFile.getName());
            }
        }

        // Trier par valeur décroissante
        entries.sort((a, b) -> Double.compare(b.value, a.value));

        return entries;
    }

    private int calculateTotalLevels(PlayerJobData playerData) {
        int totalLevels = 0;
        for (String jobId : playerData.getJobs()) {
            totalLevels += playerData.getLevel(jobId);
        }
        return totalLevels;
    }

    private double calculateTotalXp(PlayerJobData playerData) {
        double totalXp = 0.0;
        for (String jobId : playerData.getJobs()) {
            totalXp += playerData.getXp(jobId);
        }
        return totalXp;
    }

    private double calculateAverageLevel(PlayerJobData playerData) {
        Set<String> jobs = playerData.getJobs();
        if (jobs.isEmpty()) {
            return 0.0;
        }

        int totalLevels = calculateTotalLevels(playerData);
        return (double) totalLevels / jobs.size();
    }

    private String formatGlobalLeaderboardInfo(GlobalLeaderboardEntry entry, String info, int position, String unit) {
        switch (info.toLowerCase()) {
            case "player":
            case "name":
                return entry.playerName;
            case "value":
                if ("XP".equals(unit)) {
                    return String.format("%.1f", entry.value);
                } else {
                    return String.valueOf((int) entry.value);
                }
            case "position":
            case "rank":
                return String.valueOf(position);
            case "formatted":
                if ("XP".equals(unit)) {
                    return String.format("#%d %s - %.1f %s", 
                        position, entry.playerName, entry.value, unit);
                } else {
                    return String.format("#%d %s - %d %s", 
                        position, entry.playerName, (int) entry.value, unit);
                }
            default:
                return "Invalid Info";
        }
    }

    private String getEmptyGlobalValue(String info) {
        switch (info.toLowerCase()) {
            case "player":
            case "name":
                return "No Player";
            case "value":
                return "0";
            case "position":
            case "rank":
                return "0";
            case "formatted":
                return "No player in ranking";
            default:
                return "N/A";
        }
    }

    private int getGlobalPlayerRank(UUID playerUuid, String rankType) {
        List<GlobalLeaderboardEntry> leaderboard = getGlobalLeaderboard(rankType);
        
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).playerUuid.equals(playerUuid)) {
                return i + 1;
            }
        }
        
        return 0; // Player not found in ranking
    }

    public void clearCache() {
        cachedGlobalLeaderboards.clear();
        lastCacheUpdate.clear();
    }

    private static class GlobalLeaderboardEntry {
        final UUID playerUuid;
        final String playerName;
        final double value;

        GlobalLeaderboardEntry(UUID playerUuid, String playerName, double value) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.value = value;
        }
    }
}