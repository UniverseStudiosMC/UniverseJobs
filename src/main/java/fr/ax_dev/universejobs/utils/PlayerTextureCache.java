package fr.ax_dev.universejobs.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.profile.PlayerProfile;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class PlayerTextureCache {

    private static final Map<UUID, PlayerProfile> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, UUID> NAME_TO_UUID_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, String> UUID_TO_NAME_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CACHE_TIMESTAMPS = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = TimeUnit.HOURS.toMillis(24);
    private static final int MAX_CACHE_SIZE = 1000;

    private static volatile boolean userCacheLoaded = false;
    private static volatile long lastUserCacheLoad = 0;
    private static final long USER_CACHE_RELOAD_INTERVAL = TimeUnit.MINUTES.toMillis(5);

    private static volatile Boolean hasPaperProfileApi = null;
    private static Method paperCreateProfileMethod = null;
    private static Method paperFillFromCacheMethod = null;

    public static void init() {
        loadUserCache();
        detectPaperProfileApi();
    }

    private static void detectPaperProfileApi() {
        if (hasPaperProfileApi != null) return;
        try {
            Class<?> serverClass = Bukkit.getServer().getClass();
            paperCreateProfileMethod = serverClass.getMethod("createProfile", UUID.class, String.class);
            Class<?> profileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
            paperFillFromCacheMethod = profileClass.getMethod("complete", boolean.class);
            hasPaperProfileApi = true;
            Bukkit.getLogger().info("[UniverseJobs] Paper PlayerProfile API detected - using optimized player head caching");
        } catch (Exception e) {
            hasPaperProfileApi = false;
        }
    }

    private static PlayerProfile tryPaperCacheFirst(UUID playerId, String playerName) {
        if (hasPaperProfileApi == null) detectPaperProfileApi();
        if (!Boolean.TRUE.equals(hasPaperProfileApi)) return null;
        try {
            Object paperProfile = paperCreateProfileMethod.invoke(Bukkit.getServer(), playerId, playerName);
            Boolean completed = (Boolean) paperFillFromCacheMethod.invoke(paperProfile, false);
            if (completed) {
                Method getTexturesMethod = paperProfile.getClass().getMethod("getTextures");
                Object textures = getTexturesMethod.invoke(paperProfile);
                Method getSkinMethod = textures.getClass().getMethod("getSkin");
                Object skin = getSkinMethod.invoke(textures);
                if (skin != null) {
                    PlayerProfile bukkitProfile = Bukkit.createPlayerProfile(playerId, playerName);
                    bukkitProfile.getTextures().setSkin((URL) skin);
                    return bukkitProfile;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void loadUserCache() {
        if (System.currentTimeMillis() - lastUserCacheLoad < USER_CACHE_RELOAD_INTERVAL && userCacheLoaded) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                File userCacheFile = new File(Bukkit.getWorldContainer(), "usercache.json");
                if (!userCacheFile.exists()) {
                    userCacheFile = new File("usercache.json");
                }

                if (userCacheFile.exists()) {
                    try (FileReader reader = new FileReader(userCacheFile)) {
                        JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
                        for (JsonElement element : array) {
                            JsonObject entry = element.getAsJsonObject();
                            if (entry.has("uuid") && entry.has("name")) {
                                String uuidStr = entry.get("uuid").getAsString();
                                String name = entry.get("name").getAsString();
                                try {
                                    UUID uuid = UUID.fromString(uuidStr);
                                    NAME_TO_UUID_CACHE.put(name.toLowerCase(), uuid);
                                    UUID_TO_NAME_CACHE.put(uuid, name);
                                } catch (IllegalArgumentException ignored) {}
                            }
                        }
                    }
                    userCacheLoaded = true;
                    lastUserCacheLoad = System.currentTimeMillis();
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to load usercache.json", e);
            }
        });
    }

    public static PlayerProfile getProfileFromCacheOnly(UUID playerId, String playerName) {
        Long timestamp = CACHE_TIMESTAMPS.get(playerId);
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
            PlayerProfile cached = PROFILE_CACHE.get(playerId);
            if (cached != null && cached.getTextures().getSkin() != null) {
                return cached;
            }
        }

        PlayerProfile paperCached = tryPaperCacheFirst(playerId, playerName);
        if (paperCached != null && paperCached.getTextures().getSkin() != null) {
            cacheProfile(playerId, paperCached);
            return paperCached;
        }

        return null;
    }

    public static void preloadProfileAsync(UUID playerId, String playerName) {
        Long timestamp = CACHE_TIMESTAMPS.get(playerId);
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
            PlayerProfile cached = PROFILE_CACHE.get(playerId);
            if (cached != null && cached.getTextures().getSkin() != null) {
                return;
            }
        }

        getPlayerProfile(playerId, playerName);
    }

    public static CompletableFuture<PlayerProfile> getPlayerProfile(UUID playerId, String playerName) {
        loadUserCache();

        Long timestamp = CACHE_TIMESTAMPS.get(playerId);
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
            PlayerProfile cached = PROFILE_CACHE.get(playerId);
            if (cached != null && cached.getTextures().getSkin() != null) {
                return CompletableFuture.completedFuture(cached);
            }
        }

        PlayerProfile paperCached = tryPaperCacheFirst(playerId, playerName);
        if (paperCached != null && paperCached.getTextures().getSkin() != null) {
            cacheProfile(playerId, paperCached);
            return CompletableFuture.completedFuture(paperCached);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerProfile profile = Bukkit.createPlayerProfile(playerId, playerName);
                profile.update().get(5, TimeUnit.SECONDS);

                if (profile.getTextures().getSkin() != null) {
                    cacheProfile(playerId, profile);
                    return profile;
                }

                return fetchFromMojangApi(playerId, playerName);
            } catch (Exception e) {
                return fetchFromMojangApi(playerId, playerName);
            }
        });
    }

    public static PlayerProfile getProfileByNameFromCacheOnly(String playerName) {
        UUID cachedUuid = NAME_TO_UUID_CACHE.get(playerName.toLowerCase());
        if (cachedUuid != null) {
            return getProfileFromCacheOnly(cachedUuid, playerName);
        }
        return null;
    }

    public static void preloadProfileByNameAsync(String playerName) {
        getPlayerProfileByName(playerName);
    }

    public static CompletableFuture<PlayerProfile> getPlayerProfileByName(String playerName) {
        loadUserCache();

        UUID cachedUuid = NAME_TO_UUID_CACHE.get(playerName.toLowerCase());
        if (cachedUuid != null) {
            Long timestamp = CACHE_TIMESTAMPS.get(cachedUuid);
            if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
                PlayerProfile cached = PROFILE_CACHE.get(cachedUuid);
                if (cached != null && cached.getTextures().getSkin() != null) {
                    return CompletableFuture.completedFuture(cached);
                }
            }
            return getPlayerProfile(cachedUuid, playerName);
        }

        return CompletableFuture.supplyAsync(() -> fetchFromMojangApiByName(playerName));
    }

    public static String getPlayerName(UUID playerId) {
        loadUserCache();
        return UUID_TO_NAME_CACHE.get(playerId);
    }

    public static UUID getPlayerUuid(String playerName) {
        loadUserCache();
        return NAME_TO_UUID_CACHE.get(playerName.toLowerCase());
    }

    private static PlayerProfile fetchFromMojangApi(UUID playerId, String playerName) {
        try {
            UUID mojangUuid = NAME_TO_UUID_CACHE.get(playerName.toLowerCase());
            if (mojangUuid == null) {
                mojangUuid = fetchMojangUuid(playerName);
            }

            if (mojangUuid != null) {
                NAME_TO_UUID_CACHE.put(playerName.toLowerCase(), mojangUuid);
                UUID_TO_NAME_CACHE.put(mojangUuid, playerName);
                return fetchProfileWithTextures(mojangUuid, playerName);
            }
        } catch (Exception ignored) {}

        PlayerProfile fallback = Bukkit.createPlayerProfile(playerId, playerName);
        cacheProfile(playerId, fallback);
        return fallback;
    }

    private static PlayerProfile fetchFromMojangApiByName(String playerName) {
        try {
            UUID mojangUuid = NAME_TO_UUID_CACHE.get(playerName.toLowerCase());
            if (mojangUuid == null) {
                mojangUuid = fetchMojangUuid(playerName);
            }

            if (mojangUuid != null) {
                NAME_TO_UUID_CACHE.put(playerName.toLowerCase(), mojangUuid);
                UUID_TO_NAME_CACHE.put(mojangUuid, playerName);
                return fetchProfileWithTextures(mojangUuid, playerName);
            }
        } catch (Exception ignored) {}

        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
        return Bukkit.createPlayerProfile(offlineUuid, playerName);
    }

    private static UUID fetchMojangUuid(String playerName) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            if (connection.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    String uuidStr = json.get("id").getAsString();
                    return parseUuidWithoutDashes(uuidStr);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static PlayerProfile fetchProfileWithTextures(UUID mojangUuid, String playerName) {
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" +
                mojangUuid.toString().replace("-", "") + "?unsigned=false");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            if (connection.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                    if (json.has("properties")) {
                        for (var prop : json.getAsJsonArray("properties")) {
                            JsonObject propObj = prop.getAsJsonObject();
                            if ("textures".equals(propObj.get("name").getAsString())) {
                                String textureValue = propObj.get("value").getAsString();
                                return createProfileFromTexture(mojangUuid, playerName, textureValue);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        PlayerProfile profile = Bukkit.createPlayerProfile(mojangUuid, playerName);
        cacheProfile(mojangUuid, profile);
        return profile;
    }

    private static PlayerProfile createProfileFromTexture(UUID uuid, String name, String textureValue) {
        try {
            String decoded = new String(java.util.Base64.getDecoder().decode(textureValue));
            JsonObject textureJson = JsonParser.parseString(decoded).getAsJsonObject();
            JsonObject textures = textureJson.getAsJsonObject("textures");

            if (textures.has("SKIN")) {
                String skinUrl = textures.getAsJsonObject("SKIN").get("url").getAsString();
                PlayerProfile profile = Bukkit.createPlayerProfile(uuid, name);
                profile.getTextures().setSkin(new URL(skinUrl));
                cacheProfile(uuid, profile);
                return profile;
            }
        } catch (Exception ignored) {}

        PlayerProfile profile = Bukkit.createPlayerProfile(uuid, name);
        cacheProfile(uuid, profile);
        return profile;
    }

    private static UUID parseUuidWithoutDashes(String uuidStr) {
        String withDashes = uuidStr.replaceFirst(
            "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
            "$1-$2-$3-$4-$5"
        );
        return UUID.fromString(withDashes);
    }

    private static void cacheProfile(UUID playerId, PlayerProfile profile) {
        if (PROFILE_CACHE.size() >= MAX_CACHE_SIZE) {
            cleanOldestEntries();
        }
        PROFILE_CACHE.put(playerId, profile);
        CACHE_TIMESTAMPS.put(playerId, System.currentTimeMillis());
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
        NAME_TO_UUID_CACHE.clear();
        UUID_TO_NAME_CACHE.clear();
        userCacheLoaded = false;
    }

    public static void removeFromCache(UUID playerId) {
        PROFILE_CACHE.remove(playerId);
        CACHE_TIMESTAMPS.remove(playerId);
    }

    public static int getCacheSize() {
        return PROFILE_CACHE.size();
    }

    public static int getNameCacheSize() {
        return NAME_TO_UUID_CACHE.size();
    }
}
