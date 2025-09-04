package fr.ax_dev.universejobs.update;

import fr.ax_dev.universejobs.UniverseJobs;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Checks GitHub for plugin updates and downloads new versions automatically.
 */
public class UpdateChecker {

    private static final String API_URL = "https://api.github.com/repos/UniverseStudiosMC/UniverseJobs/releases/latest";

    private final UniverseJobs plugin;

    public UpdateChecker(UniverseJobs plugin) {
        this.plugin = plugin;
    }

    /**
     * Check GitHub for updates and download the newest jar if available.
     */
    public void checkForUpdates() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Accept", "application/vnd.github+json")
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    plugin.getLogger().warning("Update check failed: HTTP " + response.statusCode());
                    return;
                }

                String body = response.body();
                String latestTag = extractValue(body, "\"tag_name\":\"");
                if (latestTag == null) {
                    plugin.getLogger().warning("Could not parse latest version from GitHub response.");
                    return;
                }

                String currentVersion = plugin.getDescription().getVersion();
                if (currentVersion.equalsIgnoreCase(latestTag)) {
                    return; // already up to date
                }

                plugin.getLogger().info("New version available: " + latestTag + " (current: " + currentVersion + ")");

                String downloadUrl = extractValue(body, "\"browser_download_url\":\"");
                if (downloadUrl == null) {
                    plugin.getLogger().warning("No download URL found for latest release.");
                    return;
                }

                Path pluginsDir = plugin.getDataFolder().getParentFile().toPath();
                Path target = pluginsDir.resolve("UniverseJobs-" + latestTag + ".jar");

                HttpRequest downloadRequest = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).build();
                HttpResponse<Path> downloadResponse = client.send(downloadRequest, HttpResponse.BodyHandlers.ofFile(target));
                if (downloadResponse.statusCode() == 200) {
                    plugin.getLogger().info("Downloaded update to " + target.getFileName() + ". Please restart the server to apply the update.");
                } else {
                    plugin.getLogger().warning("Failed to download update: HTTP " + downloadResponse.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                plugin.getLogger().log(Level.WARNING, "Error while checking for updates", e);
            }
        });
    }

    private String extractValue(String json, String key) {
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = json.indexOf('"', start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}
