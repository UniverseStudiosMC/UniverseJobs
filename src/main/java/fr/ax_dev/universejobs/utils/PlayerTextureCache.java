package fr.ax_dev.universejobs.utils;

import org.bukkit.Bukkit;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PlayerTextureCache {

    private static final Map<UUID, PlayerProfile> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CACHE_TIMESTAMPS = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = TimeUnit.HOURS.toMillis(24);
    private static final int MAX_CACHE_SIZE = 500;

    public static CompletableFuture<PlayerProfile> getPlayerProfile(UUID playerId, String playerName) {
        Long timestamp = CACHE_TIMESTAMPS.get(playerId);
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
            PlayerProfile cached = PROFILE_CACHE.get(playerId);
            if (cached != null) {
                return CompletableFuture.completedFuture(cached);
            }
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerProfile profile = Bukkit.createPlayerProfile(playerId, playerName);
                profile.update().get(5, TimeUnit.SECONDS);

                if (PROFILE_CACHE.size() >= MAX_CACHE_SIZE) {
                    cleanOldestEntries();
                }

                PROFILE_CACHE.put(playerId, profile);
                CACHE_TIMESTAMPS.put(playerId, System.currentTimeMillis());

                return profile;
            } catch (Exception e) {
                PlayerProfile fallback = Bukkit.createPlayerProfile(playerId, playerName);
                return fallback;
            }
        });
    }

    private static void cleanOldestEntries() {
        long oldestTime = System.currentTimeMillis();
        UUID oldestUuid = null;

        for (Map.Entry<UUID, Long> entry : CACHE_TIMESTAMPS.entrySet()) {
            if (entry.getValue() < oldestTime) {
                oldestTime = entry.getValue();
                oldestUuid = entry.getKey();
            }
        }

        if (oldestUuid != null) {
            PROFILE_CACHE.remove(oldestUuid);
            CACHE_TIMESTAMPS.remove(oldestUuid);
        }
    }

    public static void clearCache() {
        PROFILE_CACHE.clear();
        CACHE_TIMESTAMPS.clear();
    }

    public static void removeFromCache(UUID playerId) {
        PROFILE_CACHE.remove(playerId);
        CACHE_TIMESTAMPS.remove(playerId);
    }
}
