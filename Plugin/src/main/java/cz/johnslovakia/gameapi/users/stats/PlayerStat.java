package cz.johnslovakia.gameapi.users.stats;

import cz.johnslovakia.gameapi.datastorage.StatsTable;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Logger;

public class PlayerStat {

    private GamePlayer gamePlayer;
    private Stat stat;
    private int score;
    private boolean updated;

    public PlayerStat(GamePlayer gamePlayer, Stat stat) {
        this.gamePlayer = gamePlayer;
        this.stat = stat;
        this.score = stat.getStatsTable().getStat(gamePlayer.getOnlinePlayer().getName(), stat.getName());
        this.updated = false;
    }

    public PlayerStat increase(){
        updated = true;
        score++;
        return this;
    }

    public PlayerStat decrease(){
        updated = true;
        score--;
        return this;
    }

    public PlayerStat setStat(int stat){
        updated = true;
        score = stat;
        return this;
    }

    public int getStatScore() {
        return score;
    }

    public boolean wasUpdated() {
        return updated;
    }

    public void saveDataToDatabase(){
        StatsTable table = stat.getStatsTable();
        table.setStat(gamePlayer.getOnlinePlayer().getName(), stat.getName(), score);
    }
}
