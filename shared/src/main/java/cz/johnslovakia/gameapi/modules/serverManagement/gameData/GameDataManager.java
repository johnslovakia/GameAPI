package cz.johnslovakia.gameapi.modules.serverManagement.gameData;

import com.google.gson.JsonObject;
import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.database.Database;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.serverManagement.ServerRegistry;
import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages reading and writing game state data for a single game server.
 * Register properties via {@link #addProperty} and call {@link #updateGame} to push data.
 */
public class GameDataManager<T> {

    @Getter
    private final String minigameName;
    @Getter
    private final T gameContext;
    private final String gameName;
    private final int maxPlayers;

    @Getter
    private final List<JSONProperty<T>> jsonProperties = new ArrayList<>();

    public GameDataManager(String minigameName, T gameContext, String gameName, int maxPlayers) {
        this.minigameName = minigameName;
        this.gameContext = gameContext;
        this.gameName = gameName;
        this.maxPlayers = maxPlayers;
    }

    /** Registers a single JSON property that will be included in game data updates. */
    public void addProperty(String propertyName, UpdatedValueInterface<T> valueInterface) {
        jsonProperties.add(new JSONProperty<>(propertyName, valueInterface));
    }

    public void addProperties(List<JSONProperty<T>> properties) {
        jsonProperties.addAll(properties);
    }

    public JSONProperty<T> getJSONProperty(String name) {
        return jsonProperties.stream()
                .filter(prop -> prop.getProperty().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /** Creates the games table if it doesn't already exist. Called during {@link ServerRegistry#initialize()}. */
    public static void createTableIfNotExists(Database serverDataMySQL) {
        String query = "CREATE TABLE IF NOT EXISTS games (" +
                "name VARCHAR(64) NOT NULL PRIMARY KEY, " +
                "minigame VARCHAR(64) NOT NULL, " +
                "max_players INT NOT NULL, " +
                "data JSON NOT NULL, " +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "INDEX idx_minigame (minigame), " +
                "INDEX idx_last_updated (last_updated)" +
                ")";
        try (SQLDatabaseConnection connection = serverDataMySQL.getConnection()) {
            if (connection != null) {
                QueryResult result = connection.exec(query);
                if (!result.isSuccessful()) {
                    Logger.log("Failed to create 'games' table: " + result.getRejectMessage(), Logger.LogType.ERROR);
                }
            } else {
                Logger.log("Failed to get a database connection!", Logger.LogType.ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Pushes all registered properties to the database/Redis asynchronously. */
    public void updateGame() {
        if (ModuleManager.getModule(ServerRegistry.class).useRedisForServerData()) {
            updateGameRedis();
        } else {
            updateGameMySQL();
        }
    }

    private void updateGameRedis() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    JsonObject jsonData = buildGameDataJson();
                    String key = "minigame." + minigameName + "." + gameName;
                    ModuleManager.getModule(ServerRegistry.class).getServerDataRedis().set(key, jsonData.toString(), 60);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Shared.getInstance().getPlugin());
    }

    private void updateGameMySQL() {
        new BukkitRunnable() {
            @Override
            public void run() {
                JsonObject jsonData = buildGameDataJson();
                String query = "INSERT INTO games (name, minigame, max_players, data) VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE minigame=?, max_players=?, data=?, last_updated=CURRENT_TIMESTAMP";
                try (SQLDatabaseConnection connection = ModuleManager.getModule(ServerRegistry.class).getServerDataMySQL().getConnection()) {
                    if (connection == null) {
                        Logger.log("Failed to get connection for updating game: " + gameName, Logger.LogType.ERROR);
                        return;
                    }
                    try (PreparedStatement statement = connection.getConnection().prepareStatement(query)) {
                        statement.setString(1, gameName);
                        statement.setString(2, minigameName);
                        statement.setInt(3, maxPlayers);
                        statement.setString(4, jsonData.toString());
                        statement.setString(5, minigameName);
                        statement.setInt(6, maxPlayers);
                        statement.setString(7, jsonData.toString());
                        statement.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Shared.getInstance().getPlugin());
    }

    private JsonObject buildGameDataJson() {
        JsonObject jsonData = new JsonObject();
        for (JSONProperty<T> property : jsonProperties) {
            UpdatedValueInterface<T> valueInterface = property.getUpdatedValueInterface();
            switch (valueInterface.getWhat().toLowerCase()) {
                case "string":
                    String strValue = valueInterface.getStringValue(gameContext);
                    if (strValue != null) jsonData.addProperty(property.getProperty(), strValue);
                    break;
                case "integer":
                    Integer intValue = valueInterface.getIntegerValue(gameContext);
                    if (intValue != null) jsonData.addProperty(property.getProperty(), intValue);
                    break;
                case "double":
                    Double doubleValue = valueInterface.getDoubleValue(gameContext);
                    if (doubleValue != null) jsonData.addProperty(property.getProperty(), doubleValue);
                    break;
                case "boolean":
                    Boolean boolValue = valueInterface.getBooleanValue(gameContext);
                    if (boolValue != null) jsonData.addProperty(property.getProperty(), boolValue);
                    break;
            }
        }
        return jsonData;
    }

    /**
     * Updates a single property in the database without rebuilding the full JSON.
     * For Redis, falls back to a full {@link #updateGame()} call.
     */
    public void updateProperty(String propertyName, Object value) {
        if (ModuleManager.getModule(ServerRegistry.class).useRedisForServerData()) {
            updateGame();
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                String query = "UPDATE games SET data = JSON_SET(data, '$." + propertyName + "', ?), " +
                        "last_updated = CURRENT_TIMESTAMP WHERE name = ?";
                try (SQLDatabaseConnection connection = ModuleManager.getModule(ServerRegistry.class).getServerDataMySQL().getConnection()) {
                    if (connection == null) {
                        Logger.log("Failed to get connection for updating property: " + propertyName, Logger.LogType.ERROR);
                        return;
                    }
                    try (PreparedStatement statement = connection.getConnection().prepareStatement(query)) {
                        if (value instanceof String) statement.setString(1, (String) value);
                        else if (value instanceof Integer) statement.setInt(1, (Integer) value);
                        else if (value instanceof Double) statement.setDouble(1, (Double) value);
                        else if (value instanceof Boolean) statement.setBoolean(1, (Boolean) value);
                        else statement.setString(1, value.toString());
                        statement.setString(2, gameName);
                        statement.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Shared.getInstance().getPlugin());
    }
}