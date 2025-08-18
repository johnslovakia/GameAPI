package cz.johnslovakia.gameapi.worldManagement;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameManager;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.utils.FileManager;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WorldManager {


    public static List<String> loadedWorlds = new ArrayList<>();


    /*public static void unload(GameMap arena){
        if (arena.getWorld() == null){
            return;
        }

        String world_name = arena.getWorld().getName();
        Bukkit.unloadWorld(arena.getWorld(), false);
        arena.setWorld(null);

        File activeWorldFolder = new File(Bukkit.getWorldContainer(), world_name);
        if (activeWorldFolder.exists()) FileManager.deleteFile(activeWorldFolder);
    }*/

    public static void unload(World world) {
        if (world == null) return;

        String worldName = world.getName();

        if (!world.getPlayers().isEmpty()){
            world.getPlayers().forEach(player -> {
                player.sendMessage("§cAn error occurred, you are being sent to the lobby");
                Utils.sendToLobby(player, false);
            });
        }

        Bukkit.getScheduler().runTask(Minigame.getInstance().getPlugin(), () -> {
            try {
                boolean success = Bukkit.unloadWorld(world, false);
                if (!success) {
                    Logger.log("Failed to unload world: " + worldName, Logger.LogType.ERROR);
                }else{
                    loadedWorlds.remove(worldName);
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), () -> {
                File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
                if (worldFolder.exists()) {
                    try {
                        Files.walk(worldFolder.toPath())
                                .sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                    } catch (IOException e) {
                        Logger.log("Failed to delete world folder: " + worldName, Logger.LogType.ERROR);
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    public static void delete(File file){
        if (file.isDirectory()){
            File[] files = file.listFiles();
            if (files == null) return;
            for (File child : files){
                delete(child);
            }
        }

        file.delete();
    }


    public static void loadLobbyWorld(String worldName){
        ClassicWorldLoader.loadClassicWorld(worldName);

        World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld != null){
            //TODO: potřeba přepsat a udělat lepší system při podpoře více verzí!
            if (GameAPI.getInstance().getVersion().contains("1_20")){
                bukkitWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            }else{
                bukkitWorld.setGameRuleValue("doDaylightCycle", "false");
            }
        }

    }

    public static void loadArenaWorld(GameMap arena, Game game){
        if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin"/*"SlimeWorldManager"*/) != null) {
            if (GameAPI.getInstance().getSlimeWorldLoader().cloneSlimeArenaWorld(Minigame.getInstance().getPlugin(), arena.getName(), game.getID())) {
                return;
            }
        }
        ClassicWorldLoader.loadClassicArenaWorld(arena, game);
    }

    public static void loadWorld(String worldName){
        loadWorld(worldName, null);
    }

    public static void loadWorld(String worldName, Player player){
        if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin"/*"SlimeWorldManager"*/) != null) {
            if (GameAPI.getInstance().getSlimeWorldLoader().loadSlimeWorld(Minigame.getInstance().getPlugin(), worldName, player)) {
                return;
            }
        }
        ClassicWorldLoader.loadClassicWorld(worldName, player);
    }

    public static void cloneWorld(String worldName, String id){
        if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin"/*"SlimeWorldManager"*/) != null) {
            if (GameAPI.getInstance().getSlimeWorldLoader().cloneSlimeArenaWorld(Minigame.getInstance().getPlugin(), worldName, id)) {
                return;
            }
        }
        File sourceWorldFolder = new File(GameAPI.getInstance().getMinigameDataFolder() + "/maps/", worldName);
        File activeWorldFolder = new File(Bukkit.getWorldContainer().getParentFile(), worldName + "_" + id);
        ClassicWorldLoader.loadClassicWorld(worldName, sourceWorldFolder, activeWorldFolder, false);
    }


    public static void addLoadedWorld(String worldName){
        if (loadedWorlds.contains(worldName)){
            return;
        }
        loadedWorlds.add(worldName);
    }

    public static boolean isLoaded(String worldName){
        return loadedWorlds.contains(worldName);
    }
}
