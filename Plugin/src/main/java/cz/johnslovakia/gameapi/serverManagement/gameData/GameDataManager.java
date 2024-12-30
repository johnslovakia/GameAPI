package cz.johnslovakia.gameapi.serverManagement.gameData;

import com.google.gson.JsonObject;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.serverManagement.gameData.implementations.*;
import cz.johnslovakia.gameapi.utils.Logger;
import me.zort.sqllib.internal.query.UpdateQuery;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GameDataManager {

    private final Game game;

    private List<JSONProperty> jsonProperties = new ArrayList<>();


    public GameDataManager(Game game) {
        this.game = game;

        jsonProperties.add(new JSONProperty("GameState", new GameStateValueImple()));
        jsonProperties.add(new JSONProperty("MaxPlayers", new MaxPlayersValueImple()));
        jsonProperties.add(new JSONProperty("Players", new PlayersValueImple()));
        jsonProperties.add(new JSONProperty("Map", new MapValueImple()));
        jsonProperties.add(new JSONProperty("StartingTime", new StartingTimeValueImple()));
    }

    public GameDataManager(Game game, List<JSONProperty> properties) {
        this.game = game;

        jsonProperties.add(new JSONProperty("GameState", new GameStateValueImple()));
        jsonProperties.add(new JSONProperty("MaxPlayers", new MaxPlayersValueImple()));
        jsonProperties.add(new JSONProperty("Players", new PlayersValueImple()));
        jsonProperties.add(new JSONProperty("Map", new MapValueImple()));
        jsonProperties.add(new JSONProperty("StartingTime", new StartingTimeValueImple()));
        if (properties != null) {
            if (!properties.isEmpty()) {
                jsonProperties.addAll(properties);
            }
        }
    }


    public static void  createTableIfNotExists() {
        String query = "CREATE TABLE IF NOT EXISTS games (" +
                //"id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(64) NOT NULL PRIMARY KEY, " +
                "minigame VARCHAR(64) NOT NULL, " +
                "max_players INT NOT NULL, " +
                "data JSON NOT NULL, " +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";

        GameAPI.getInstance().getMinigame().getServerDataMySQL().getConnection().exec(query);
    }

    public void updateGame(){
        Minigame minigame = GameAPI.getInstance().getMinigame();

        if (minigame.useRedisForServerData()){
            new BukkitRunnable(){
                @Override
                public void run() {
                    JsonObject jsonData = new JsonObject();
                    for (JSONProperty property : jsonProperties) {
                        UpdatedValueInterface valueInterface = property.getUpdatedValueInterface();
                        if (valueInterface.getWhat().equalsIgnoreCase("Boolean")) {
                            jsonData.addProperty(property.getProperty(), valueInterface.getBooleanValue(game));
                        } else if (valueInterface.getWhat().equalsIgnoreCase("String")) {
                            jsonData.addProperty(property.getProperty(), valueInterface.getStringValue(game));
                        } else if (valueInterface.getWhat().equalsIgnoreCase("Double")) {
                            jsonData.addProperty(property.getProperty(), valueInterface.getDoubleValue(game));
                        } else if (valueInterface.getWhat().equalsIgnoreCase("Integer")) {
                            jsonData.addProperty(property.getProperty(), valueInterface.getIntegerValue(game));
                        }
                    }

                    minigame.getServerDataRedis().set("minigame." + minigame.getName() + "." + game.getName(), jsonData.toString(), 86400);
                }
            }.runTaskAsynchronously(GameAPI.getInstance());
        }else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    JsonObject jsonData = new JsonObject();
                    for (JSONProperty property : jsonProperties) {
                        UpdatedValueInterface valueInterface = property.getUpdatedValueInterface();
                        if (valueInterface.getWhat().equalsIgnoreCase("Boolean")) {
                            jsonData.addProperty(property.getProperty(), valueInterface.getBooleanValue(game));
                        } else if (valueInterface.getWhat().equalsIgnoreCase("String")) {
                            jsonData.addProperty(property.getProperty(), valueInterface.getStringValue(game));
                        } else if (valueInterface.getWhat().equalsIgnoreCase("Double")) {
                            jsonData.addProperty(property.getProperty(), valueInterface.getDoubleValue(game));
                        } else if (valueInterface.getWhat().equalsIgnoreCase("Integer")) {
                            jsonData.addProperty(property.getProperty(), valueInterface.getIntegerValue(game));
                        }
                    }

                    String query = "INSERT INTO games (name, minigame, max_players, data) VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE name=VALUES(name), minigame=VALUES(minigame), max_players=VALUES(max_players), data=VALUES(data), last_updated=CURRENT_TIMESTAMP";


                    PreparedStatement statement = null;
                    try {
                        statement = minigame.getServerDataMySQL().getConnection().getConnection().prepareStatement(query);
                        statement.setString(1, game.getName());
                        statement.setString(2, minigame.getName());
                        statement.setInt(3, game.getSettings().getMaxPlayers());
                        statement.setString(4, jsonData.toString());
                        statement.executeUpdate();
                        statement.execute();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } finally {
                        if (statement != null) {
                            try {
                                statement.close();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }.runTaskAsynchronously(GameAPI.getInstance());
        }
    }

    public Game getGame() {
        return game;
    }

    public List<JSONProperty> getJsonProperties() {
        return jsonProperties;
    }

    public JSONProperty getJSONProperty(String name){
        for (JSONProperty property : jsonProperties){
            if (property.getProperty().equalsIgnoreCase(name)){
                return property;
            }
        }
        return null;
    }
}
