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
    private MapLocation mapLocation1;
    @Setter
    private MapLocation mapLocation2;

    private boolean isBorder = false;

    @Getter @Setter
    private AreaSettings settings;

    public Area(GameMap map, MapLocation mapLocation1, MapLocation mapLocation2, String name){
        this.map = map;
        this.mapLocation1 = mapLocation1;
        this.mapLocation2 = mapLocation2;
        this.name = name;
        this.settings = new AreaSettings();
    }

    public Location getCenter(){
        return getCenter(true);
    }

    public Location getCenter(boolean withY){
        double x = mapLocation1.getX() + 0.5 + (mapLocation2.getX() - mapLocation1.getX()) / 2;
        double y = (withY ? mapLocation1.getY() + (mapLocation2.getY() - mapLocation1.getY()) / 2 : mapLocation1.getY());
        double z = mapLocation1.getZ() + 0.5 + (mapLocation2.getZ() - mapLocation1.getZ()) / 2;
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

        if (mapLocation2 == null){
            Logger.log("Area.isInArea(): mapLocation2 is null! (" + map.getName() + " " + map.getGame().getName() + " " + map.getGame().getID() + ")", Logger.LogType.WARNING);
            return true;
        }else if (mapLocation1 == null){
            Logger.log("Area.isInArea(): mapLocation1 is null! (" + map.getName() + " " + map.getGame().getName() + " " + map.getGame().getID() + ")", Logger.LogType.WARNING);
            return true;
        }

        double x1 = Math.min(mapLocation1.getX(), mapLocation2.getX()) - buffer;
        double y1 = Math.min(mapLocation1.getY(), mapLocation2.getY()) - buffer;
        double z1 = Math.min(mapLocation1.getZ(), mapLocation2.getZ()) - buffer;

        double x2 = Math.max(mapLocation1.getX(), mapLocation2.getX()) + buffer;
        double y2 = Math.max(mapLocation1.getY(), mapLocation2.getY()) + buffer;
        double z2 = Math.max(mapLocation1.getZ(), mapLocation2.getZ()) + buffer;

        return location.getX() >= x1 && location.getX() <= x2 &&
                location.getY() >= y1 && location.getY() <= y2 &&
                location.getZ() >= z1 && location.getZ() <= z2;
    }

    public boolean isInArea(Player player){
        return isInArea(player.getLocation());
    }

    public boolean isInArea(Player player, int tolerance){
        return isInArea(player.getLocation(), tolerance);
    }

    public boolean isInArea(GamePlayer gamePlayer){
        return isInArea(gamePlayer.getOnlinePlayer().getLocation());
    }

    public boolean isInArea(GamePlayer gamePlayer, int tolerance){
        return isInArea(gamePlayer.getOnlinePlayer().getLocation(), tolerance);
    }

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
        return mapLocation1;
    }

    public Location getLocation1(){
        return mapLocation1.getLocation();
    }

    public MapLocation getMapLocation2(){
        return mapLocation2;
    }

    public Location getLocation2(){
        return mapLocation2.getLocation();
    }

    public boolean isBorder() {
        return isBorder;
    }

    public void setBorder(boolean border) {
        isBorder = border;
    }

}