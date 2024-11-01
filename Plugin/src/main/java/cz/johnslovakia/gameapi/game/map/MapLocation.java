package cz.johnslovakia.gameapi.game.map;

import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;

public class MapLocation {

    @Getter
    private final String id;

    @Setter
    private String worldName;
    @Getter
    private double x;
    @Getter
    private double y;
    @Getter
    private double z;

    @Getter
    private float yaw;
    @Getter
    private float pitch;

    public MapLocation(String id, double x, double y, double z, float yaw, float pitch) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public MapLocation(String id, double x, double y, double z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public MapLocation(String id, String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = worldName;
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public MapLocation(String id, String worldName, double x, double y, double z) {
        this.worldName = worldName;
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public World getWorld(GameMap map){
        if (map.getWorld() == null){
            Logger.log("GameLocation (getWorld): World is null!", Logger.LogType.ERROR);
        }
        return map.getWorld();
    }

    public Location getLocationForMap(GameMap map){
        return getLocation(map.getWorld());
    }

    public Location getLocation(World world){
        if (world == null){
            Logger.log("GameLocation (getLocation): World is null!", Logger.LogType.ERROR);
        }
        Location location = new Location(world, x, y, z);
        if (getYaw() != 0 && getPitch() != 0){
            location.setYaw(getYaw());
            location.setPitch(getPitch());
        }
        return location;
    }


    //public Arena getArena() {
        //return arena;
    //}

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public String getWorldName() {
        if (worldName == null){
            return id;
        }
        return worldName;
    }

}
