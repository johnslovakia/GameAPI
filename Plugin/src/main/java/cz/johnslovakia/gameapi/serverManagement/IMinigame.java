package cz.johnslovakia.gameapi.serverManagement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.GameState;
import lombok.Getter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Getter
public class IMinigame {

    private final DataManager dataManager;
    private final String name;

    private final List<IGame> games = new ArrayList<>();

    public IMinigame(DataManager dataManager, String name) {
        this.dataManager = dataManager;
        this.name = name;

        load();
    }

    public void load() {
        if (dataManager.useRedisForServerData()){
            Set<String> keys = dataManager.getServerDataRedis().getPool().getResource().keys("minigame." + name + ".*");

            if (!keys.isEmpty()) {
                for (String key : keys) {
                    games.add(new IGame(this, key.split("\\.")[2]));
                }
            }
        }else {
            dataManager.getServerDataMySQL().getConnection().connect();

            String query = "SELECT * FROM games WHERE minigame = ?";

            PreparedStatement statement = null;
            try {
                statement = Objects.requireNonNull(dataManager.getServerDataMySQL().getConnection().getConnection()).prepareStatement(query);
                statement.setString(1, Minigame.getInstance().getName());

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String arenaName = resultSet.getString("name");

                        Timestamp lastUpdate = resultSet.getTimestamp("last_updated");
                        long differenceInHours = (System.currentTimeMillis() - lastUpdate.getTime()) / 1000 / 60 / 60;
                        if (differenceInHours >= 24) {
                            dataManager.getServerDataMySQL().getConnection()
                                    .delete()
                                    .from("games")
                                    .where().isEqual("name", arenaName)
                                    .execute();
                            continue;
                        }

                        games.add(new IGame(this, arenaName));
                    }
                }
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
    }

    public IGame getBestServer(){
        GameData highestServerData = null;
        for (GameData serverData : getServersData()){
            if (highestServerData == null){
                highestServerData = serverData;
            }

            if (serverData.getPlayers() > highestServerData.getPlayers()) {
                highestServerData = serverData;
            }
        }
        if (highestServerData == null){
            return null;
        }
        return highestServerData.getServer();
    }

    public List<GameData> getServersData() {
        List<GameData> list = new ArrayList<>();

        for (IGame arena : games){
            list.add(getGameDataByGame(arena));
        }
        return list;
    }

    public boolean isThereFreeGame(){
        for (IGame server : games){
            if (server.isOpen()) return true;
        }
        return false;
    }

    public GameData getGameDataByGame(IGame server) {
        GameData arenaData = server.getGameData();

        if (arenaData != null){
            if (!arenaData.shouldUpdate()){
                return arenaData;
            }
        }

        if (dataManager.useRedisForServerData()){
            String data = dataManager.getServerDataRedis().getPool().getResource().get("minigame." + name + "." + server.getName());
            if (data == null) return null;

            JsonObject jsonData = new JsonParser().parse(data).getAsJsonObject();

            GameData newData = new GameData(server);
            newData.setGameState(GameState.valueOf(jsonData.get("GameState").getAsString()));
            newData.setPlayers(jsonData.get("Players").getAsInt());
            newData.setMaxPlayers(jsonData.get("MaxPlayers").getAsInt());
            newData.setJsonObject(jsonData);
            newData.setLastUpdate(LocalTime.now());

            server.setGameData(newData);

            return newData;
        }else {
            String query = "SELECT * FROM games WHERE name = ? LIMIT 1";

            PreparedStatement statement = null;
            try {
                statement = dataManager.getServerDataMySQL().getConnection().getConnection().prepareStatement(query);
                statement.setString(1, server.getName());

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        int maxPlayers = resultSet.getInt("max_players");

                        JsonObject a = new JsonObject();
                        a.addProperty("name", resultSet.getString("name"));
                        a.addProperty("max_players", maxPlayers);
                        a.addProperty("minigame", resultSet.getString("minigame"));

                        String data = resultSet.getString("data");
                        JsonObject jsonData = new JsonParser().parse(data).getAsJsonObject();
                        a.add("data", jsonData);


                        GameData newData = new GameData(server);
                        newData.setGameState(GameState.valueOf(jsonData.get("GameState").getAsString()));
                        newData.setPlayers(jsonData.get("Players").getAsInt());
                        newData.setMaxPlayers(maxPlayers);
                        newData.setJsonObject(jsonData);
                        newData.setLastUpdate(LocalTime.now());

                        server.setGameData(newData);

                        return newData;
                    }
                }
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
        return null;
    }
}
