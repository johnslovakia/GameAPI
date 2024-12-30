package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.worldManagement.WorldManager;
import cz.johnslovakia.gameapi.game.map.MapLocation;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class ConfigAPI {

    private File file;
    private FileConfiguration fileConfig;

    /** Creates a new config at the path, with the fileName, with a configCreate method caller, and uses the Plugin */
    public ConfigAPI(String path, String fileName, Runnable callback, Plugin plugin) {
        if (!fileName.contains(".yml")) {
            fileName = fileName + ".yml";
        }
        file = new File(path, fileName);
        fileConfig = YamlConfiguration.loadConfiguration(file);

        if (!file.exists()) {
            fileConfig.options().copyDefaults(true);
            callback.run();
            try {
                fileConfig.save(file);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    /** Creates a new config at the path, with the fileName, and uses the Plugin */
    public ConfigAPI(String path, String fileName, Plugin plugin) {
        if (!fileName.contains(".yml")) {
            fileName = fileName + ".yml";
        }
        file = new File(path, fileName);
        fileConfig = YamlConfiguration.loadConfiguration(file);

        if (!file.exists()) {
            fileConfig.options().copyDefaults(true);
            try {
                fileConfig.save(file);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    /** Get the Configuration section */
    public FileConfiguration getConfig() {
        return fileConfig;
    }

    /** Save the config */
    public void saveConfig() {
        try {
            fileConfig.save(file);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void setLocation(String path, Location location){
        setLocation(path, location, false);
    }

    /** Set a location in the config */
    public void setLocation(String path, Location location, boolean PitchAndYaw) {
        /*fileConfig.set(path + ".World", location.getWorld().getName());
        fileConfig.set(path + ".X", location.getX());
        fileConfig.set(path + ".Y", location.getY());
        fileConfig.set(path + ".Z", location.getZ());
        if (PitchAndYaw) {
            fileConfig.set(path + ".Pitch", location.getPitch());
            fileConfig.set(path + ".Yaw", location.getYaw());
        }*/
        fileConfig.set(path, Utils.getStringLocation(location, true));
        saveConfig();
    }

    /*public Location getLocation(String path){
        return getLocation(path, false);
    }*/


    public Location getLocation(String path) {
        if (fileConfig.getString(path) == null) {
            return null;
        }

        return Utils.getLocationString(fileConfig.getString(path));
    }

    public MapLocation getMapLocation(GameMap gameMap, String id, String path){
        return getMapLocation(gameMap, id, path, false);
    }

    public MapLocation getMapLocation(GameMap gameMap, String id, String path, boolean yaw_and_pitch){
        if (fileConfig.get(path) == null) {
            Logger.log("getMapLocation: Path '" + path + "' is null!", Logger.LogType.ERROR);
            return null;
        }
        return Utils.getMapLocationFromString(gameMap, id, fileConfig.getString(path), yaw_and_pitch);
    }
}