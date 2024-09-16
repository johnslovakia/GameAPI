package cz.johnslovakia.gameapi.game.map;

import cz.johnslovakia.gameapi.utils.Logger;
import org.bukkit.Location;
import org.bukkit.World;

public class MapLocation {

    private String id;

    private String worldName;
    private double x;
    private double y;
    private double z;

    private float yaw;
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


    public String getId() {
        return id;
    }

    //public Arena getArena() {
        //return arena;
    //}

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

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

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
}
