package cz.johnslovakia.gameapi.users.stats;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.datastorage.StatsTable;
import cz.johnslovakia.gameapi.datastorage.Type;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class StatsManager {

    private final List<Stat> stats = new ArrayList<>();
    private final StatsTable table = new StatsTable();

    public StatsManager(){
    }

    //TODO: .
    public void createDatabaseTable(){
        getTable().createTable();

        registerStat(new Stat("Winstreak").setShowToPlayer(false));
        table.createNewColumn(Type.INT, "Winstreak");
    }

    public void createPlayerStatisticsHologram(Location location, Player player){
        StatsHolograms.createPlayerStatisticsHologram(location, player);
    }

    public void createTOPStatisticsHologram(Location location, Player player){
        StatsHolograms.showTOPStatisticHologram(location, player);
    }

    public void registerStat(Stat... stats){
        this.stats.addAll(Arrays.asList(stats));
    }

    public Stat getStat(String name){
        for (Stat stat : getStats()){
            if (stat.getName().equals(name)) {
                return stat;
            }
        }
        return null;
    }
}
