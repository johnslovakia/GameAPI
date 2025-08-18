package cz.johnslovakia.gameapi.users.stats;

import cz.johnslovakia.gameapi.datastorage.StatsTable;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;

@Getter
public class PlayerStat {

    private final GamePlayer gamePlayer;
    private final Stat stat;
    private int score;
    private boolean updated;

    public PlayerStat(GamePlayer gamePlayer, Stat stat) {
        this.gamePlayer = gamePlayer;
        this.stat = stat;
        this.score = stat.getStatsTable().getStat(gamePlayer.getOnlinePlayer().getName(), stat.getName());
        this.updated = false;
    }

    public void increase(){
        updated = true;
        score++;
    }

    public void decrease(){
        updated = true;
        score--;
    }

    public void setStat(int stat){
        updated = true;
        score = stat;
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
