package cz.johnslovakia.gameapi.modules.serverManagement;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.johnslovakia.gameapi.modules.game.GameState;
import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.*;

@Getter
public class IMinigame {

    private final DataManager dataManager;
    private final String name;
    private final Map<String, IGame> games = new HashMap<>();

    public IMinigame(DataManager dataManager, String name) {
        this.dataManager = dataManager;
        this.name = name;
        load();
    }

    //TODO: refresh
    public void load() {
        if (dataManager.useRedisForServerData()) {
            loadFromRedis();
        } else {
            loadFromMySQL();
        }
    }

    private void loadFromRedis() {
        Set<String> keys = dataManager.getServerDataRedis().scanKeys("minigame." + name + ".*");

        if (!keys.isEmpty()) {
            for (String key : keys) {
                String[] parts = key.split("\\.");
                if (parts.length >= 3) {
                    games.put(parts[2], new IGame(this, parts[2]));
                }
            }
        }
    }

    private void loadFromMySQL() {
        try (SQLDatabaseConnection connection = dataManager.getServerDataMySQL().getConnection()) {
            if (connection == null) return;

            String query = "SELECT * FROM games WHERE minigame = ?";
            try (PreparedStatement statement = Objects.requireNonNull(connection.getConnection()).prepareStatement(query)) {
                statement.setString(1, getName());

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String arenaName = resultSet.getString("name");
                        Timestamp lastUpdate = resultSet.getTimestamp("last_updated");
                        long differenceInHours = (System.currentTimeMillis() - lastUpdate.getTime()) / 1000 / 60 / 60;

                        if (differenceInHours >= 24) {
                            connection.delete()
                                    .from("games")
                                    .where().isEqual("name", arenaName)
                                    .execute();
                            continue;
                        }

                        if (!games.containsKey(arenaName))
                            games.put(arenaName, new IGame(this, arenaName));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public IGame getBestServer() {
        return getServersData().stream()
                .filter(data -> data.getServer().isOpen())
                .max((d1, d2) -> Integer.compare(d1.getPlayers(), d2.getPlayers()))
                .map(GameData::getServer)
                .orElse(null);
    }

    public List<GameData> getServersData() {
        List<GameData> list = new ArrayList<>();
        for (IGame arena : games.values()) {
            GameData data = getGameDataByGame(arena);
            if (data != null) {
                list.add(data);
            }
        }
        return list;
    }

    public boolean isThereFreeGame() {
        return games.values().stream().anyMatch(IGame::isOpen);
    }

    public GameData getGameDataByGame(IGame server) {
        GameData arenaData = server.getGameData();

        if (arenaData != null && !arenaData.shouldUpdate()) {
            return arenaData;
        }

        if (dataManager.useRedisForServerData()) {
            return fetchGameDataFromRedis(server);
        } else {
            return fetchGameDataFromMySQL(server);
        }
    }

    private GameData fetchGameDataFromRedis(IGame server) {
        try {
            String key = "minigame." + name + "." + server.getName();
            String data = dataManager.getServerDataRedis().get(key);

            if (data == null) {
                return createDefaultGameData(server);
            }

            JsonObject jsonData = JsonParser.parseString(data).getAsJsonObject();
            return parseGameData(server, jsonData, jsonData);

        } catch (Exception e) {
            e.printStackTrace();
            return server.getGameData() != null ? server.getGameData() : createDefaultGameData(server);
        }
    }

    private GameData fetchGameDataFromMySQL(IGame server) {
        String query = "SELECT * FROM games WHERE name = ? LIMIT 1";

        try (SQLDatabaseConnection connection = dataManager.getServerDataMySQL().getConnection()) {
            if (connection == null) return createDefaultGameData(server);

            try (PreparedStatement statement = Objects.requireNonNull(connection.getConnection()).prepareStatement(query)) {
                statement.setString(1, server.getName());

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        int maxPlayers = resultSet.getInt("max_players");
                        String data = resultSet.getString("data");
                        JsonObject jsonData = JsonParser.parseString(data).getAsJsonObject();

                        return parseGameData(server, jsonData, jsonData, maxPlayers);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return createDefaultGameData(server);
    }

    private GameData parseGameData(IGame server, JsonObject jsonData, JsonObject fullData) {
        return parseGameData(server, jsonData, fullData,
                jsonData.has("MaxPlayers") ? jsonData.get("MaxPlayers").getAsInt() : -1);
    }

    private GameData parseGameData(IGame server, JsonObject jsonData, JsonObject fullData, int maxPlayers) {
        GameData newData = new GameData(server);

        try {
            newData.setGameState(GameState.valueOf(jsonData.get("GameState").getAsString()));
            newData.setPlayers(jsonData.get("Players").getAsInt());
            newData.setMaxPlayers(maxPlayers);
            newData.setJsonObject(fullData);
            newData.setLastUpdate(LocalTime.now());

            server.setGameData(newData);
        } catch (Exception e) {
            e.printStackTrace();
            return createDefaultGameData(server);
        }

        return newData;
    }

    private GameData createDefaultGameData(IGame server) {
        GameData data = new GameData(server);
        data.setGameState(GameState.LOADING);
        data.setPlayers(0);
        data.setMaxPlayers(-1);
        data.setLastUpdate(LocalTime.now());
        return data;
    }
}