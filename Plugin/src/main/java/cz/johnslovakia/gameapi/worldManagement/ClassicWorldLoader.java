package cz.johnslovakia.gameapi.worldManagement;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.utils.FileManager;
import cz.johnslovakia.gameapi.utils.Logger;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class ClassicWorldLoader {

    public static World loadClassicWorld(String worldName){
        return loadClassicWorld(worldName, null);
    }

    public static World loadClassicWorld(String worldName, Player player){
        File sourceWorldFolder = new File(GameAPI.getInstance().getMinigameDataFolder() + "/maps/", worldName);
        File activeWorldFolder = new File(Bukkit.getWorldContainer().getParentFile(), worldName);

        World world = loadClassicWorld(worldName, sourceWorldFolder, activeWorldFolder, true);
        if (player != null && world != null){
            Location location = new Location(world, 0, 90, 0);
            player.teleport(location);
        }

        return world;
    }

    public static World loadClassicWorld(String worldName, File source, File active, boolean canLoadWithoutSource){

        try {

            if (!source.exists() && source.getName().contains(" ")) {
                String parent = source.getParent();
                String fixedName = source.getName().replace(" ", "_");
                source = new File(parent, fixedName);
            }


            if (!source.exists()) {
                File fallbackFolder = new File(Bukkit.getWorldContainer(), worldName);
                if (fallbackFolder.exists() && !canLoadWithoutSource) {
                    source = fallbackFolder;
                } else if (canLoadWithoutSource){
                    Logger.log("Can't load Classic Arena World — no map file found! I'm trying to find another way.", Logger.LogType.ERROR);
                    Logger.log(source.getAbsolutePath() + " (" + worldName + ")", Logger.LogType.INFO);

                    World bukkitWorld = new WorldCreator(worldName).createWorld();
                    bukkitWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                    bukkitWorld.setGameRule(GameRule.LOCATOR_BAR, false);
                    bukkitWorld.setAutoSave(false);
                    return bukkitWorld;
                }else {
                    Logger.log("Can't load Classic Arena World — no map file found!", Logger.LogType.ERROR);
                    Logger.log(source.getAbsolutePath() + " (" + worldName + ")", Logger.LogType.INFO);
                    return null;
                }
            }


            boolean schem = source.getName().contains("schem");
            /*if (active.exists()){
                new WorldCreator(active.getName()).createWorld();
                return Bukkit.getWorld(active.getName());
            }*/
            try {
                Files.deleteIfExists(new File(source, "uid.dat").toPath());
            } catch (IOException ignored) {}

            if (!schem) {
                FileManager.copyFolder(source, active);
            }
            if (Bukkit.getWorld(active.getName()) != null){
                WorldManager.unload(Bukkit.getWorld(active.getName()));
            }


            //World bukkitWorld = Bukkit.createWorld(new WorldCreator(active.getName()));
            WorldCreator worldCreator = new WorldCreator(active.getName());
            if (schem) worldCreator.generator(new EmptyChunkGenerator());
            World bukkitWorld = Bukkit.createWorld(worldCreator);

            if (bukkitWorld == null)
                return null;

            if (schem){
                Location location = new Location(bukkitWorld, 0, 70, 0);
                GameAPI.getInstance().getSchematicHandler().pasteSchematic(Minigame.getInstance().getPlugin(), location, worldName, "maps");
            }

            bukkitWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            bukkitWorld.setGameRule(GameRule.LOCATOR_BAR, false);
            bukkitWorld.setAutoSave(false);
            WorldManager.addLoadedWorld(active.getName());

            return bukkitWorld;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static World loadClassicArenaWorld(GameMap arena, Game game){
        File sourceWorldFolder = new File(GameAPI.getInstance().getMinigameDataFolder() + "/maps/", arena.getName());

        if (!sourceWorldFolder.exists() && sourceWorldFolder.getName().contains(" ")) {
            String parent = sourceWorldFolder.getParent();
            String fixedName = sourceWorldFolder.getName().replace(" ", "_");
            sourceWorldFolder = new File(parent, fixedName);
        }

        if (!sourceWorldFolder.exists()) {
            File fallbackFolder = new File(Bukkit.getWorldContainer(), arena.getName());
            if (fallbackFolder.exists()) {
                sourceWorldFolder = fallbackFolder;
            } else {
                Logger.log("Can't load Classic Arena World — no map file found!", Logger.LogType.ERROR);
                Logger.log(sourceWorldFolder.getAbsolutePath() + " (" + arena.getName() + ")", Logger.LogType.INFO);
                return null;
            }
        }

        String worldName = sourceWorldFolder.getName() + "_" + game.getID();
        File activeWorldFolder = new File(Bukkit.getWorldContainer().getParentFile(), worldName);

        if (WorldManager.isLoaded(worldName)){
            return Bukkit.getWorld(worldName);
        }

        try {
            Files.deleteIfExists(new File(sourceWorldFolder, "uid.dat").toPath());
        } catch (IOException ignored) {}


        World world = loadClassicWorld(arena.getName(), sourceWorldFolder, activeWorldFolder, false);

        if (world == null)
            return null;

        world.setAutoSave(false);
        if (!arena.getSettings().isAllowTimeChange()) {
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            world.setGameRule(GameRule.LOCATOR_BAR, false);
        }
        arena.setWorld(world);
        WorldManager.addLoadedWorld(worldName);
        return world;
    }
}
