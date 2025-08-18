package cz.johnslovakia.gameapi.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import cz.johnslovakia.gameapi.Minigame;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

@Getter
public class UpdateChecker {

    private final Minigame minigame;
    private final String URL; //= "https://example.com/version.json";

    private final String currentVersion;
    private String latestVersion, updateMessage, announcement;

    private boolean outdated, unreleased = false;

    public UpdateChecker(Minigame minigame, String URL) {
        this.minigame = minigame;
        this.URL = URL;
        currentVersion = minigame.getPlugin().getDescription().getVersion();
        checkVersion();
    }

    public void checkVersion() {
        try {
            URL url = new URL(URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == 200) {
                JsonReader reader = new JsonReader(new InputStreamReader(connection.getInputStream()));
                reader.setLenient(true);
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                String latestVersion = json.get("latest_version").getAsString();
                this.latestVersion = latestVersion;

                String updateMessage = json.has("update_message") ? json.get("update_message").getAsString() : "";
                this.updateMessage = updateMessage;

                announcement = json.has("announcement") ? json.get("announcement").getAsString() : null;

                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    if (isNewerVersion(currentVersion, latestVersion)) {
                        unreleased = true;
                        Logger.log("[" + minigame.getName() + "] You are running an unreleased version (" + currentVersion + ")! Latest public version: " + latestVersion, Logger.LogType.WARNING);
                        Logger.log("[" + minigame.getName() + "] This version may not be stable.", Logger.LogType.WARNING);
                    } else {
                        outdated = true;
                        Logger.log("[" + minigame.getName() + "] Your version is outdated! Current: " + currentVersion + ", Latest: " + latestVersion, Logger.LogType.WARNING);
                        if (!updateMessage.isEmpty()) {
                            Logger.log("Update Message: " + updateMessage, Logger.LogType.INFO);
                        }
                    }
                }
            } else {
                Logger.log("Failed to check the version. HTTP code: " + connection.getResponseCode(), Logger.LogType.WARNING);
            }

            connection.disconnect();
        } catch (Exception e) {
            Logger.log("Error while checking for updates: " + e.getMessage(), Logger.LogType.ERROR);
            e.printStackTrace();
        }
    }
    
    private boolean isNewerVersion(String current, String latest) {
        try {
            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");

            for (int i = 0; i < Math.max(currentParts.length, latestParts.length); i++) {
                int currentVal = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestVal = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

                if (currentVal > latestVal) return true;
                if (currentVal < latestVal) return false;
            }
        } catch (NumberFormatException ignored) {
        }

        return false;
    }
}
