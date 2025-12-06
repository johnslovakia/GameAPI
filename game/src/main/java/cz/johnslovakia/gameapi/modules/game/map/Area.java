package cz.johnslovakia.gameapi.modules.game.map;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameService;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Area {

    /**
     * -- GETTER --
     *  Get the set name for this area.
     *
     * @return String name.
     */
    @Getter
    private final String name;
    @Getter
    private final GameMap map;
    @Setter
    private MapLocation loc1;
    @Setter
    private MapLocation loc2;

    private boolean isBorder = false;

    @Getter @Setter
    private AreaSettings settings;

    public Area(GameMap map, MapLocation loc1, MapLocation loc2, String name){
        this.map = map;
        this.loc1 = loc1;
        this.loc2 = loc2;
        this.name = name;
        this.settings = new AreaSettings();
    }

    public Location getCenter(){
        return getCenter(true);
    }

    public Location getCenter(boolean withY){
        double x = loc1.getX() + 0.5 + (loc2.getX() - loc1.getX()) / 2;
        double y = (withY ? loc1.getY() + (loc2.getY() - loc1.getY()) / 2 : loc1.getY());
        double z = loc1.getZ() + 0.5 + (loc2.getZ() - loc1.getZ()) / 2;
        return new Location(getWorld(), x, y, z);
    }

    public World getWorld(){
        return map.getWorld();
    }

    public boolean isInArea(Location location){
        return isInArea(location, 1);
    }

    public boolean isInArea(Location location, double tolerance) {
        double buffer = tolerance / 2;

        if (!Objects.equals(location.getWorld(), getWorld())){
            return false;
        }

        if (loc2 == null){
            Logger.log("Area.isInArea(): loc2 is null! (" + map.getName() + " " + map.getGame().getName() + " " + map.getGame().getID() + ")", Logger.LogType.WARNING);
            return true;
        }else if (loc1 == null){
            Logger.log("Area.isInArea(): loc1 is null! (" + map.getName() + " " + map.getGame().getName() + " " + map.getGame().getID() + ")", Logger.LogType.WARNING);
            return true;
        }

        double x1 = Math.min(loc1.getX(), loc2.getX()) - buffer;
        double y1 = Math.min(loc1.getY(), loc2.getY()) - buffer;
        double z1 = Math.min(loc1.getZ(), loc2.getZ()) - buffer;

        double x2 = Math.max(loc1.getX(), loc2.getX()) + buffer;
        double y2 = Math.max(loc1.getY(), loc2.getY()) + buffer;
        double z2 = Math.max(loc1.getZ(), loc2.getZ()) + buffer;

        return location.getX() >= x1 && location.getX() <= x2 &&
                location.getY() >= y1 && location.getY() <= y2 &&
                location.getZ() >= z1 && location.getZ() <= z2;
    }

    /**
     * Returns a list of players in the area.
     * @return List<GamePlayer> players.
     */
    public List<Player> getPlayersInArea(){
        List<Player> list = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()){

            if (isInArea(player.getLocation())){
                list.add(player);
            }
        }
        return list;
    }

    public List<Player> getAlivePlayersInArea(){
        List<Player> list = new ArrayList<>();
        for (GamePlayer gamePlayer : getMap().getGame().getPlayers()) {
            Player player = gamePlayer.getOnlinePlayer();

            if (isInArea(player.getLocation())) {
                list.add(player);
            }
        }
        return list;
    }

    public MapLocation getMapLocation1(){
        return loc1;
    }

    public Location getLocation1(){
        return loc1.getLocation();
    }

    public MapLocation getMapLocation2(){
        return loc2;
    }

    public Location getLocation2(){
        return loc2.getLocation();
    }

    public boolean isBorder() {
        return isBorder;
    }

    public void setBorder(boolean border) {
        isBorder = border;
    }

}