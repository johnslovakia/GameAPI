package cz.johnslovakia.gameapi.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.listeners.UpdateCheckerListener;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class UpdateChecker {

    private final Minigame minigame;
    private final String URL;

    private final String currentVersion;
    private String latestVersion;
    private List<String> updateMessages = new ArrayList<>();
    private String announcement;

    private boolean outdated, unreleased = false;
    private boolean allSuppressed = false;

    public UpdateChecker(Minigame minigame, String URL) {
        this.minigame = minigame;
        this.URL = URL;
        currentVersion = minigame.getPlugin().getDescription().getVersion();
        checkVersion();

        Bukkit.getPluginManager().registerEvents(new UpdateCheckerListener(), minigame.getPlugin());
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

                announcement = json.has("announcement") ? json.get("announcement").getAsString() : null;

                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    if (isNewerVersion(currentVersion, latestVersion)) {
                        unreleased = true;
                        Logger.log("[" + minigame.getName() + "] You are running an unreleased version (" + currentVersion + ")! Latest public version: " + latestVersion, Logger.LogType.WARNING);
                        Logger.log("[" + minigame.getName() + "] This version may not be stable.", Logger.LogType.WARNING);
                    } else {
                        outdated = true;
                        Logger.log("[" + minigame.getName() + "] Your version is outdated! Current: " + currentVersion + ", Latest: " + latestVersion, Logger.LogType.WARNING);

                        updateMessages = getRelevantUpdateMessages(json, currentVersion, latestVersion);

                        if (!updateMessages.isEmpty()) {
                            Logger.log("=== Update Messages ===", Logger.LogType.INFO);
                            for (String msg : updateMessages) {
                                Logger.log(msg, Logger.LogType.INFO);
                            }
                            Logger.log("======================", Logger.LogType.INFO);
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

    private List<String> getRelevantUpdateMessages(JsonObject json, String current, String latest) {
        List<String> messages = new ArrayList<>();

        if (!json.has("update_messages")) {
            if (json.has("update_message")) {
                String msg = json.get("update_message").getAsString();
                if (!msg.isEmpty()) {
                    messages.add(msg);
                }
            }
            return messages;
        }

        JsonObject updateMessagesObj = json.getAsJsonObject("update_messages");

        List<String> relevantVersions = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : updateMessagesObj.entrySet()) {
            String version = entry.getKey();

            if (isNewerVersion(version, current) && !isNewerVersion(version, latest)) {
                relevantVersions.add(version);
            }
        }

        relevantVersions.sort((a, b) -> {
            if (isNewerVersion(a, b)) return 1;
            if (isNewerVersion(b, a)) return -1;
            return 0;
        });

        boolean hasNonSuppressedVersion = false;

        for (String version : relevantVersions) {
            JsonElement element = updateMessagesObj.get(version);

            if (element.isJsonObject()) {
                JsonObject versionObj = element.getAsJsonObject();
                boolean suppressed = versionObj.has("suppress_message") && versionObj.get("suppress_message").getAsBoolean();
                if (!suppressed) {
                    hasNonSuppressedVersion = true;
                    break;
                }
            } else {
                hasNonSuppressedVersion = true;
                break;
            }
        }

        if (!hasNonSuppressedVersion) {
            allSuppressed = true;
            return messages;
        }

        for (String version : relevantVersions) {
            JsonElement element = updateMessagesObj.get(version);

            if (element.isJsonObject()) {
                JsonObject versionObj = element.getAsJsonObject();
                if (versionObj.has("message")) {
                    String message = versionObj.get("message").getAsString();
                    if (!message.isEmpty()) {
                        messages.add("[" + version + "] " + message);
                    }
                }
            } else {
                String message = element.getAsString();
                if (!message.isEmpty()) {
                    messages.add("[" + version + "] " + message);
                }
            }
        }

        return messages;
    }

    public boolean hasUpdateMessages() {
        return !updateMessages.isEmpty();
    }

    public String getUpdateMessage() {
        return String.join("\n", updateMessages);
    }

    public String getFormattedUpdateMessagesForHover() {
        if (updateMessages.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§e§lUpdate History:");
        for (String msg : updateMessages) {
            sb.append("\n§7• §f").append(msg);
        }
        return sb.toString();
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