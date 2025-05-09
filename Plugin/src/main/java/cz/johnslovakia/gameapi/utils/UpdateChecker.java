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
    private String latestVersion, updateMessage;

    private boolean outdated = false;

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

                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    outdated = true;
                    Bukkit.getLogger().log(Level.WARNING, "Your version of the " + minigame.getName() + " plugin is outdated! We recommend updating to the latest version! Latest Version: " + latestVersion + ", Your Current version: " + currentVersion);
                    if (!updateMessage.isEmpty()) {
                        Bukkit.getLogger().log(Level.INFO,"Update Message: " + updateMessage);
                        this.updateMessage = updateMessage;
                    }
                }
            } else {
                Bukkit.getLogger().log(Level.WARNING, "Failed to check the version. HTTP code: " + connection.getResponseCode());
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
