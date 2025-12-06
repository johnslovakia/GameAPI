package cz.johnslovakia.gameapi.modules.game.lobby;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

@Setter @Getter
public class LobbyLocation {

    private final GameInstance game;
    private final String worldName;

    private final double x;
    private final double y;
    private final double z;

    private float yaw;
    private float pitch;

    public LobbyLocation(GameInstance game, String worldName, double x, double y, double z, float yaw, float pitch) {
        this.game = game;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public World getWorld(){
        World world = Bukkit.getWorld(worldName + "_" + game.getID());
        if (world == null){
            Logger.log("LobbyLocation (getLocation): World is null!", Logger.LogType.ERROR);
            return null;
        }
        return world;
    }

    public Location getLocation(){
        World world = getWorld();
        if (world == null) return null;

        Location location = new Location(world, x, y, z);
        if (getYaw() != 0 && getPitch() != 0){
            location.setYaw(getYaw());
            location.setPitch(getPitch());
        }
        return location;
    }
}
