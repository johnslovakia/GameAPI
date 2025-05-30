package cz.johnslovakia.gameapi.game.map;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
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

    /**
     * -- GETTER --
     *  Get the settings for this area.
     *
     * @return ArenaSettings for this area.
     */
    @Getter @Setter
    private AreaSettings settings;

    /**
     * Define an area to make checking for certain events easier.
     * You can also define areas to have their own settings (Area.getSettings()).
     * Area settings will not take effect unless useSettings() is set to true.
     * Loc1 and loc2 will be re-written so that loc1 is always the "lowest" position, and loc2 to be the "highest"
     *
     * @param loc1 First corner of the area
     * @param loc2 Second corner of the area
     * @param name The name for this area
     */
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

    /**
     * Checks whether or not a player is in the area.
     * @return True if player is in area, false otherwise.
     */
    public boolean isInArea(Location location){
        return isInArea(location, 1);
    }

    /**
     * Checks whether or not a player is in the area.
     * @return True if player is in area, false otherwise.
     */
    public boolean isInArea(Location location, double tolerance) {
        double buffer = tolerance / 2;

        if (!Objects.equals(location.getWorld(), getWorld())){
            return false;
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
        for (Game game : GameManager.getGames()) {
            for (GamePlayer gamePlayer : game.getPlayers()) {
                Player player = gamePlayer.getOnlinePlayer();

                if (isInArea(player.getLocation())) {
                    list.add(player);
                }
            }
        }
        return list;
    }

    /**
     * Get the first (corner) location of this area.
     * @return ArenaLocation object.
     */
    public MapLocation getArenaLocation1(){
        return loc1;
    }

    /**
     * Get the first (corner) location of this area.
     * @return Location object.
     */
    public Location getLocation1(){
        return loc1.getLocation();
    }

    /**
     * Get the second (corner) location of this area.
     * @return ArenaLocation object.
     */
    public MapLocation getArenaLocation2(){
        return loc2;
    }

    /**
     * Get the second (corner) location of this area.
     * @return Location object.
     */
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