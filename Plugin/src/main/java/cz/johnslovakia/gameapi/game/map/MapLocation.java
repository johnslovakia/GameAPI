package cz.johnslovakia.gameapi.game.map;

import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;

@Setter @Getter
public class MapLocation {

    private final GameMap gameMap;
    private final String id;

    private double x;
    private double y;
    private double z;

    private float yaw;
    private float pitch;

    public MapLocation(GameMap gameMap, String id, double x, double y, double z, float yaw, float pitch) {
        this.gameMap = gameMap;
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public MapLocation(GameMap gameMap, String id, double x, double y, double z) {
        this.gameMap = gameMap;
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public World getWorld(){
        World world = gameMap.getWorld();
        if (world == null) Logger.log("GameLocation (getLocation): World is null!", Logger.LogType.ERROR);
        return world;
    }

    public Location getLocation(){
        World world = gameMap.getWorld();
        if (world == null) Logger.log("GameLocation (getLocation): World is null!", Logger.LogType.ERROR);

        Location location = new Location(world, x, y, z);
        if (getYaw() != 0 && getPitch() != 0){
            location.setYaw(getYaw());
            location.setPitch(getPitch());
        }
        return location;
    }
}
