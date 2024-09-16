package cz.johnslovakia.gameapi.users.stats;

import cz.johnslovakia.gameapi.datastorage.StatsTable;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;

import java.util.*;

public class Stat{

    private String name;
    private Map<GamePlayer, PlayerStat> playerStats = new HashMap<>();
    private StatsManager statsManager;

    private final String translate_key;

    public Stat(StatsManager statsManager, String name) {
        this.statsManager = statsManager;
        this.name = name;

        this.translate_key = "stat." + name.toLowerCase().replace(" ", "_");
    }

    public PlayerStat getPlayerStat(GamePlayer gamePlayer){
        if (playerStats.containsKey(gamePlayer)){
            return playerStats.get(gamePlayer);
        }else{
            PlayerStat stat = new PlayerStat(gamePlayer, this);
            playerStats.put(gamePlayer, stat);
            return stat;
        }
    }

    public StatsTable getStatsTable() {
        return statsManager.getStatsTable();
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public String getName() {
        return name;
    }

    public String getTranslated(GamePlayer gamePlayer){
        if (MessageManager.existMessage(gamePlayer.getLanguage(), translate_key)){
            return MessageManager.get(gamePlayer, translate_key).getTranslated();
        }else{
            return getName();
        }
    }

    public Map<GamePlayer, PlayerStat> getPlayerStats() {
        return playerStats;
    }


}
