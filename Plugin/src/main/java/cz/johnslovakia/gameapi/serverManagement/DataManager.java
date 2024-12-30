package cz.johnslovakia.gameapi.serverManagement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.GameState;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.*;

public class DataManager {

    private static List<GameData> dataList = new ArrayList<>();
    private Minigame minigame = null;

    public DataManager(Minigame minigame){
        if (GameAPI.getInstance().getMinigame() != null){
            this.minigame = minigame;
        }else{
            return;
        }
        load();
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

        for (IGame arena : IGame.getServers()){
            list.add(getGameDataByGame(arena));
        }
        return list;
    }

    public boolean isThereFreeGame(){
        for (IGame server : IGame.getServers()){
            if (server.isOpen()) return true;
        }
        return false;
    }


    public void load() {
        if (minigame.useRedisForServerData()){
            Set<String> keys = minigame.getServerDataRedis().getPool().getResource().keys("minigame." + minigame.getName() + ".*");

            if (!keys.isEmpty()) {
                for (String key : keys) {
                    IGame.addGame(new IGame(minigame.getName(), key.split("\\.")[2]));
                }
            }
        }else {
            minigame.getServerDataMySQL().getConnection().connect();

            String query = "SELECT * FROM games WHERE minigame = ?";

            PreparedStatement statement = null;
            try {
                statement = Objects.requireNonNull(minigame.getServerDataMySQL().getConnection().getConnection()).prepareStatement(query);
                statement.setString(1, GameAPI.getInstance().getName());

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String arenaName = resultSet.getString("name");

                        Timestamp lastUpdate = resultSet.getTimestamp("last_updated");
                        long differenceInHours = (System.currentTimeMillis() - lastUpdate.getTime()) / 1000 / 60 / 60;
                        if (differenceInHours >= 24) {
                            minigame.getServerDataMySQL().getConnection()
                                    .delete()
                                    .from("games")
                                    .where().isEqual("name", arenaName)
                                    .execute();
                            continue;
                        }

                        IGame.addGame(new IGame(minigame.getName(), arenaName));
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

    public GameData getGameDataByGame(IGame server) {
        GameData arenaData = null;
        for(GameData data : dataList){
            if (data.getServer().equals(server)){
                arenaData = data;
                break;
            }
        }

        if (arenaData != null){
            if (!arenaData.shouldUpdate()){
                return arenaData;
            }
        }

        if (minigame.useRedisForServerData()){
            String data = minigame.getServerDataRedis().getPool().getResource().get("minigame." + minigame.getName() + "." + server.getName());
            if (data == null) return null;

            JsonObject jsonData = new JsonParser().parse(data).getAsJsonObject();

            GameData newData = new GameData(server);
            newData.setGameState(GameState.valueOf(jsonData.get("GameState").getAsString()));
            newData.setPlayers(jsonData.get("Players").getAsInt());
            newData.setMaxPlayers(jsonData.get("MaxPlayers").getAsInt());
            newData.setJsonObject(jsonData);
            newData.setLastUpdate(LocalTime.now());

            if (arenaData != null) {
                dataList.remove(arenaData);
            }
            dataList.add(newData);

            return newData;
        }else {

            String query = "SELECT * FROM games WHERE name = ? LIMIT 1";

            PreparedStatement statement = null;
            try {
                statement = minigame.getServerDataMySQL().getConnection().getConnection().prepareStatement(query);
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

                        if (arenaData != null) {
                            dataList.remove(arenaData);
                        }
                        dataList.add(newData);

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
