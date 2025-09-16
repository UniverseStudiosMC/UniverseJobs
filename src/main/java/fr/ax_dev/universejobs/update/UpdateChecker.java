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
    private static final int RESOURCE_ID = 128572;

    private final UniverseJobs plugin;

    public UpdateChecker(UniverseJobs plugin) {
        this.plugin = plugin;
    }

    /**
     * Check Spigot for updates using the Spiget API.
     */
    public void checkForUpdates() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
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
                String latestVersion = extractValue(responseBody, "\"name\":\"");

                if (latestVersion == null) {
                    plugin.getLogger().warning("Could not parse latest version from Spigot response.");
                    return;
                }

                String currentVersion = plugin.getDescription().getVersion();

                // Clean version strings for comparison
                String cleanLatest = cleanVersionString(latestVersion);
                String cleanCurrent = cleanVersionString(currentVersion);

                if (cleanCurrent.equals(cleanLatest)) {
                    plugin.getLogger().info("UniverseJobs is up to date! (Version: " + currentVersion + ")");
                    return;
                }

                plugin.getLogger().info("═══════════════════════════════════════");
                plugin.getLogger().info("    NEW UPDATE AVAILABLE!");
                plugin.getLogger().info("    Current: " + currentVersion);
                plugin.getLogger().info("    Latest:  " + latestVersion);
                plugin.getLogger().info("    Download: https://www.spigotmc.org/resources/128572/");
                plugin.getLogger().info("═══════════════════════════════════════");

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
     * Clean version string for comparison by removing common prefixes/suffixes.
     */
    private String cleanVersionString(String version) {
        if (version == null) return "";

        // Remove common prefixes like "v", "version-", etc.
        version = version.replaceAll("^(v|version-?)", "");

        // Remove common suffixes like "-dev", "-SNAPSHOT", etc.
        version = version.replaceAll("(-dev|-SNAPSHOT|-beta|-alpha).*$", "");

        return version.trim();
    }

    /**
     * Extract value from JSON response.
     */
    private String extractValue(String json, String key) {
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = json.indexOf('"', start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}
