package cz.johnslovakia.gameapi.serverManagement;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.datastorage.RedisManager;
import cz.johnslovakia.gameapi.serverManagement.gameData.GameDataManager;
import lombok.Getter;

import java.util.*;

@Getter
public class DataManager {

    @Getter
    private static DataManager instance;

    private Minigame.Database serverDataMySQL;
    private RedisManager serverDataRedis;

    private final List<IMinigame> minigames = new ArrayList<>();

    public DataManager(Minigame.Database serverDataMySQL) {
        instance = this;
        this.serverDataMySQL = serverDataMySQL;
        GameDataManager.createTableIfNotExists(serverDataMySQL);
    }

    public DataManager(RedisManager serverDataRedis) {
        instance = this;
        this.serverDataRedis = serverDataRedis;
    }

    public void addMinigame(IMinigame iMinigame){
        if (minigames.contains(iMinigame)) return;
        minigames.add(iMinigame);
    }

    public Optional<IMinigame> getMinigame(String name){
        return minigames.stream().filter(iMinigame -> iMinigame.getName().equalsIgnoreCase(name)).findFirst();
    }


    public boolean useRedisForServerData(){
        return serverDataRedis != null;
    }

}
