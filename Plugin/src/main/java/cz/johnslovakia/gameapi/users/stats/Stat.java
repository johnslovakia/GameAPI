package cz.johnslovakia.gameapi.users.stats;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.datastorage.StatsTable;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;

import java.util.*;

@Getter
public class Stat{

    private String name;
    private Map<GamePlayer, PlayerStat> playerStats = new HashMap<>();

    private final String translate_key;

    public Stat(String name) {
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
        return GameAPI.getInstance().getStatsManager().getStatsTable();
    }

    public String getTranslated(GamePlayer gamePlayer){
        if (MessageManager.existMessage(gamePlayer.getLanguage(), translate_key)){
            return MessageManager.get(gamePlayer, translate_key).getTranslated();
        }else{
            return getName();
        }
    }
}
