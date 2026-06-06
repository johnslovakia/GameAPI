package cz.johnslovakia.gameapi.utils.chatHead;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.OfflinePlayer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

/**
 * SkinSource implementation to retrieve heads from Mojang.
 */
//https://github.com/OGminso/ChatHeadFont/tree/main
public class MojangSource extends SkinSource {

    private static final String FALLBACK_SKIN_UUID = "9cb6a52c55bc456b9513f4cf19cdf9e3";
    private static final String UNAVAILABLE_SKIN = "Unable to retrieve player skin URL.";

    public MojangSource(boolean useUUIDWhenRetrieve) {
        super(SkinSourceEnum.MOJANG, true, useUUIDWhenRetrieve);
    }

    public MojangSource() {
        super(SkinSourceEnum.MOJANG, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseComponent[] getHead(OfflinePlayer player, boolean overlay) {

        String lookupId = useUUIDWhenRetrieve() ? player.getUniqueId().toString().replace("-", "") : getUUIDFromName(player);
        String skin = getPlayerSkinFromMojang(lookupId);
        if (UNAVAILABLE_SKIN.equals(skin)) {
            skin = getPlayerSkinFromMojang(FALLBACK_SKIN_UUID, false);
        }
        return toBaseComponent(getPixelColorsFromSkin(skin, overlay));

    }

    /**
     * Get the id by knowing the player's name.
     *
     * @param offlinePlayer The player.
     * @return the id by knowing the player's name.
     */
    public String getUUIDFromName(OfflinePlayer offlinePlayer) {
        try {
            // Construct the URL for fetching player's profile information from Mojang's session server
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + offlinePlayer.getName());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return FALLBACK_SKIN_UUID;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                // Read the response from the connection and append it to the StringBuilder
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close(); // Close the reader
                // Parse the JSON response
                String jsonResponse = response.toString();
                JSONObject jsonObject = new JSONObject(jsonResponse);
                return jsonObject.getString("id");

            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return FALLBACK_SKIN_UUID;
        }

    }


    /**
     * Retrieves the URL of the players skin hosted on Mojangs session server.
     * The function sends a GET request to Mojangs session server with the provided players UUID,
     * parses the JSON response to extract the skin URL, and returns it.
     *
     * @param uuid The UUID of the player whose skin URL is to be retrieved.
     * @return A string representing the URL of the player's skin.
     */
    private String getPlayerSkinFromMojang(String uuid) {
        return getPlayerSkinFromMojang(uuid, true);
    }

    private String getPlayerSkinFromMojang(String uuid, boolean allowFallback) {
        if (uuid == null || uuid.isBlank()) {
            return UNAVAILABLE_SKIN;
        }

        try {
            // Construct the URL for fetching player's profile information from Mojang's session server
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                if (allowFallback && !FALLBACK_SKIN_UUID.equals(uuid)) {
                    return getPlayerSkinFromMojang(FALLBACK_SKIN_UUID, false);
                }
                return UNAVAILABLE_SKIN;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                // Read the response from the connection and append it to the StringBuilder
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close(); // Close the reader
                // Parse the JSON response
                String jsonResponse = response.toString();
                JSONObject jsonObject = new JSONObject(jsonResponse);
                JSONArray propertiesArray = jsonObject.getJSONArray("properties");

                // Iterate through the properties array to find the textures property
                for (int i = 0; i < propertiesArray.length(); i++) {
                    JSONObject property = propertiesArray.getJSONObject(i);
                    if (property.getString("name").equals("textures")) {
                        String value = property.getString("value");
                        // Decode the Base64 encoded value
                        byte[] decodedBytes = Base64.getDecoder().decode(value);
                        String decodedValue = new String(decodedBytes);
                        JSONObject textureJson = new JSONObject(decodedValue);
                        // Extract and return the URL of the player's skin
                        return textureJson.getJSONObject("textures").getJSONObject("SKIN").getString("url");
                    }
                }
            }
        } catch (IOException | JSONException ignored) {
            //e.printStackTrace();
        }
        return UNAVAILABLE_SKIN; //TODO Add error handling
    }


}
