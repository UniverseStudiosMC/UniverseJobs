package fr.ax_dev.universejobs.update;

import fr.ax_dev.universejobs.UniverseJobs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

/**
 * Checks Spigot for plugin updates using the Spiget API.
 */
public class UpdateChecker {

    private static final String SPIGOT_API_URL = "https://api.spiget.org/v2/resources/128572/versions/latest";
    private final UniverseJobs plugin;

    public UpdateChecker(UniverseJobs plugin) {
        this.plugin = plugin;
    }

    /**
     * Check Spigot for updates using the Spiget API.
     */
    public void checkForUpdates() {
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            try {
                URL url = new URL(SPIGOT_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "UniverseJobs-UpdateChecker");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    plugin.getLogger().warning("Update check failed: HTTP " + responseCode);
                    return;
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                String responseBody = response.toString();
                String latestVersion = extractValue(responseBody, "\"name\"");

                if (latestVersion == null) {
                    plugin.getLogger().warning("Could not parse latest version from Spigot response.");
                    return;
                }

                String currentVersion = plugin.getDescription().getVersion();

                int comparison = compareVersions(currentVersion, latestVersion);

                if (comparison == 0) {
                    plugin.getLogger().info("UniverseJobs is up to date! (Version: " + currentVersion + ")");
                } else if (comparison > 0) {
                    plugin.getLogger().info("You are running a newer version than the latest on SpigotMC!");
                    plugin.getLogger().info("    Current: " + currentVersion);
                    plugin.getLogger().info("    SpigotMC: " + latestVersion);
                } else {
                    plugin.getLogger().info("═══════════════════════════════════════");
                    plugin.getLogger().info("    NEW UPDATE AVAILABLE!");
                    plugin.getLogger().info("    Current: " + currentVersion);
                    plugin.getLogger().info("    Latest:  " + latestVersion);
                    plugin.getLogger().info("    Download: https://www.spigotmc.org/resources/128572/");
                    plugin.getLogger().info("═══════════════════════════════════════");
                }

            } catch (IOException e) {
                if (plugin.getServer().getPluginManager().getPlugin("UniverseJobs").isEnabled()) {
                    plugin.getLogger().log(Level.WARNING, "Could not check for updates: " + e.getMessage());
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error while checking for updates", e);
            }
        });
    }

    /**
     * Compare two version strings.
     * @return positive if current > latest, 0 if equal, negative if current < latest
     */
    private int compareVersions(String current, String latest) {
        String cleanCurrent = extractVersionNumber(current);
        String cleanLatest = extractVersionNumber(latest);

        String[] currentParts = cleanCurrent.split("\\.");
        String[] latestParts = cleanLatest.split("\\.");

        int maxLength = Math.max(currentParts.length, latestParts.length);

        for (int i = 0; i < maxLength; i++) {
            int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;

            if (currentPart != latestPart) {
                return currentPart - latestPart;
            }
        }

        return 0;
    }

    /**
     * Extract version number from a version string.
     */
    private String extractVersionNumber(String version) {
        if (version == null) return "0.0.0";

        version = version.toUpperCase();
        version = version.replaceAll("^(ALPHA-|BETA-|RC-|RELEASE-|V)", "");
        version = version.replaceAll("(-DEV|-SNAPSHOT|-BETA|-ALPHA).*$", "");

        return version.trim();
    }

    /**
     * Parse a single version part to integer.
     */
    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Extract value from JSON response.
     */
    private String extractValue(String json, String key) {
        int start = json.indexOf(key);
        if (start == -1) return null;

        start = json.indexOf(':', start);
        if (start == -1) return null;

        start = json.indexOf('"', start);
        if (start == -1) return null;
        start++;

        int end = json.indexOf('"', start);
        if (end == -1) return null;

        return json.substring(start, end);
    }
}
