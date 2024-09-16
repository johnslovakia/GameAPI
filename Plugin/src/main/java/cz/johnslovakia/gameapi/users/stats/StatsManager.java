package cz.johnslovakia.gameapi.users.stats;

import cz.johnslovakia.gameapi.datastorage.StatsTable;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StatsManager {

    private List<Stat> stats = new ArrayList<>();
    private StatsTable table = new StatsTable();

    public StatsTable getStatsTable() {
        return table;
    }

    public void createDatabaseTable(){
        getStatsTable().createTable();
    }


    public void createPlayerStatisticsHologram(Location location, Player player){
        StatsHolograms.createPlayerStatisticsHologram(this, location, player);
    }

    public void createTOPStatisticsHologram(Location location, Player player){
        StatsHolograms.createTOPStatisticsHologram(this, location, player);
    }



    public void registerStat(Stat... stat){
        if (!stats.contains(stat)){
            stats.addAll(Arrays.asList(stat));
        }
    }

    public Stat getStat(String name){
        for (Stat stat : getStats()){
            if (stat.getName().equals(name)) {
                return stat;
            }
        }
        return null;
    }

    public List<Stat> getStats() {
        return stats;
    }
}
