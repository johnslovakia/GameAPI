package cz.johnslovakia.npcapi.util;

import cz.johnslovakia.npcapi.api.SkinData;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Utility for fetching player skins from the Mojang API asynchronously.
 *
 * <pre>{@code
 * SkinFetcher.fetchByName("Notch", plugin)
 *     .thenAccept(skin -> {
 *         npc.setSkin(skin);
 *     })
 *     .exceptionally(ex -> { ex.printStackTrace(); return null; });
 * }</pre>
 */
public final class SkinFetcher {

    private static final String PROFILE_BY_NAME =
            "https://api.mojang.com/users/profiles/minecraft/";
    private static final String PROFILE_BY_UUID =
            "https://sessionserver.mojang.com/session/minecraft/profile/";

    private SkinFetcher() {}

    /**
     * Fetches a skin by player name asynchronously.
     *
     * @param name   Minecraft username
     * @param plugin Plugin used for scheduling
     */
    @NotNull
    public static CompletableFuture<SkinData> fetchByName(@NotNull String name,
                                                          @NotNull Plugin plugin) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String uuidJson = httpGet(PROFILE_BY_NAME + name);
                if (uuidJson == null) throw new RuntimeException("Player not found: " + name);

                String rawUuid = extractJson(uuidJson, "id");
                if (rawUuid == null) throw new RuntimeException("Could not parse UUID for: " + name);

                return fetchByRawUuid(rawUuid);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to fetch skin for " + name, e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Fetches a skin by player UUID asynchronously.
     */
    @NotNull
    public static CompletableFuture<SkinData> fetchByUUID(@NotNull UUID uuid,
                                                          @NotNull Plugin plugin) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetchByRawUuid(uuid.toString().replace("-", ""));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to fetch skin for UUID " + uuid, e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Convenience method that fetches a skin and calls a callback on the main thread.
     */
    public static void fetchByNameAsync(@NotNull String name,
                                        @NotNull Plugin plugin,
                                        @NotNull Consumer<SkinData> onSuccess,
                                        @NotNull Consumer<Throwable> onError) {
        fetchByName(name, plugin)
                .thenAccept(skin -> Bukkit.getScheduler().runTask(plugin, () -> onSuccess.accept(skin)))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () -> onError.accept(ex));
                    return null;
                });
    }

    @NotNull
    private static SkinData fetchByRawUuid(@NotNull String rawUuid) throws Exception {
        String profileJson = httpGet(PROFILE_BY_UUID + rawUuid + "?unsigned=false");
        if (profileJson == null) throw new RuntimeException("Empty profile response for UUID: " + rawUuid);

        String value     = extractPropertyField(profileJson, "value");
        String signature = extractPropertyField(profileJson, "signature");

        if (value == null) throw new RuntimeException("Could not extract texture value from profile");

        return new SkinData(value, signature);
    }

    private static String httpGet(@NotNull String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent", "NpcAPI/1.0");

        int responseCode = conn.getResponseCode();
        if (responseCode == 204 || responseCode == 404) return null;
        if (responseCode != 200) throw new RuntimeException("HTTP " + responseCode + " for " + urlString);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    /**
     * Minimal JSON field extractor — avoids a full JSON library dependency.
     * Only handles simple string fields at the top level.
     */
    private static String extractJson(@NotNull String json, @NotNull String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf('"', start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    /**
     * Extracts a field from inside the "properties" array of a Mojang profile response.
     */
    private static String extractPropertyField(@NotNull String json, @NotNull String field) {
        int propIndex = json.indexOf("\"properties\"");
        if (propIndex == -1) return null;
        String sub = json.substring(propIndex);
        return extractJson(sub, field);
    }
}
