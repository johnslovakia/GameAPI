package cz.johnslovakia.gameapi.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.johnslovakia.gameapi.GameAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class UpdateChecker {

    private final String URL; //= "https://example.com/version.json"; // Odkaz na tvůj JSON soubor
    private final String currentVersion;

    private boolean outdated = false;

    public UpdateChecker(Plugin plugin, String URL) {
        this.URL = URL;
        currentVersion = plugin.getDescription().getVersion();
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
                JsonObject json = JsonParser.parseReader(new InputStreamReader(connection.getInputStream())).getAsJsonObject();

                String latestVersion = json.get("latest_version").getAsString();
                String updateMessage = json.has("update_message") ? json.get("update_message").getAsString() : "";

                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    outdated = true;
                    Bukkit.getLogger().log(Level.WARNING, "Your version of the " + GameAPI.getInstance().getMinigame().getName() + " plugin is outdated! We recommend updating to the latest version! Latest Version: " + latestVersion + " Your Current version: " + currentVersion);
                    if (!updateMessage.isEmpty()) {
                        Bukkit.getLogger().log(Level.INFO,"Update Message: " + updateMessage);
                    }
                }
            } else {
                System.out.println("Failed to check the version. HTTP code: " + connection.getResponseCode());
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
