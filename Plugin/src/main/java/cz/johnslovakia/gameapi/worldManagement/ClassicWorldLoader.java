package cz.johnslovakia.gameapi.worldManagement;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.utils.FileManager;
import cz.johnslovakia.gameapi.utils.Logger;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;


public class ClassicWorldLoader {

    public static World loadClassicWorld(String worldName){
        return loadClassicWorld(worldName, null);
    }

    public static World loadClassicWorld(String worldName, Player player){
        File sourceWorldFolder = new File(GameAPI.getInstance().getMinigameDataFolder() + "/maps/", worldName);
        File activeWorldFolder = new File(Bukkit.getWorldContainer().getParentFile(), worldName);

        World world = loadClassicWorld(worldName, sourceWorldFolder, activeWorldFolder, true);
        if (player != null){
            Location location = new Location(Bukkit.getWorld(worldName), 0, 90, 0);
            player.teleport(location);
        }

        if (GameAPI.getInstance().getVersion().contains("1_20")){
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule announceAdvancements false");
        }else {
            world.setGameRuleValue("announceAdvancements", "false");
        }

        return world;
    }

    public static World loadClassicWorld(String worldName, File source, File active, boolean canLoadWithoutSource){

        try {
            if (!source.exists()){
                if (!source.exists() && canLoadWithoutSource) {
                    Logger.log("ClassicWorldLoader: I can't make a copy of the World: " + worldName + " because it's not in the x/maps folder, I'm creating a new world.", Logger.LogType.WARNING);
                    return new WorldCreator(worldName).createWorld();
                }else{
                    Logger.log("ClassicWorldLoader: I can't make a copy of the World: " + worldName + " because it's not in the x/maps folder!", Logger.LogType.ERROR);
                    return null;
                }
            }

            /*if (active.exists()){
                new WorldCreator(active.getName()).createWorld();
                return Bukkit.getWorld(active.getName());
            }*/

            FileManager.copyFolder(source, active);
            World bukkitWorld = Bukkit.createWorld(new WorldCreator(active.getName()));

            //bukkitWorld.setGameRule("ANNOUNCE_ADVANCEMENTS", "false");
            if (GameAPI.getInstance().getVersion().contains("1_20")){
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule announceAdvancements false");
            }else {
                bukkitWorld.setGameRuleValue("announceAdvancements", "false");
            }
            bukkitWorld.setAutoSave(false);
            WorldManager.addLoadedWorld(active.getName());

            return bukkitWorld;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static World loadClassicArenaWorld(GameMap arena, Game game){
        File sourceWorldFolder = new File(GameAPI.getInstance().getMinigameDataFolder() + "/maps/", arena.getName());
        if (!sourceWorldFolder.exists() && new File(Bukkit.getWorldContainer() + "/", arena.getName()).exists()){
            sourceWorldFolder = new File(Bukkit.getWorldContainer() + "/", arena.getName());
        }else{
            Logger.log("Can't load Classic Arena World! No map file!", Logger.LogType.ERROR);
            return null;
        }

        String worldName = sourceWorldFolder.getName() + "_" + game.getID();

        File activeWorldFolder = new File(Bukkit.getWorldContainer().getParentFile(), worldName);

        if (WorldManager.isLoaded(worldName)){
            return Bukkit.getWorld(worldName);
        }

        World world;
        if (sourceWorldFolder.getName().contains("schem")){
            world = Bukkit.createWorld(new WorldCreator(worldName));
            world.setAutoSave(false);

            if (GameAPI.getInstance().getVersion().contains("1_20")){
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule announceAdvancements false");
            }else {
                world.setGameRuleValue("announceAdvancements", "false");
            }

            Location location = new Location(world, 0, 70, 0);
            GameAPI.getInstance().getSchematicHandler().pasteSchematic(GameAPI.getInstance(), location, arena.getName(), "maps");
        }else{
            world = loadClassicWorld(arena.getName(), sourceWorldFolder, activeWorldFolder, false);
        }


        if (!arena.getSettings().isAllowTimeChange()) {
            if (GameAPI.getInstance().getVersion().contains("1_20")){
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            }else{
                world.setGameRuleValue("doDaylightCycle", "false");
            }
        }
        arena.setWorld(world);
        WorldManager.addLoadedWorld(worldName);
        return world;
    }
}
