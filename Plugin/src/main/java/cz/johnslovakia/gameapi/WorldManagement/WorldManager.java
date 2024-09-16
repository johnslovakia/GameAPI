package cz.johnslovakia.gameapi.WorldManagement;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameManager;
import cz.johnslovakia.gameapi.game.map.GameMap;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WorldManager {


    public static List<String> loadedWorlds = new ArrayList<>();


    public static void unload(GameMap arena, Game game){
        if (arena.getWorld() == null){
            return;
        }
        String world_name = arena.getWorld().getName();
        Bukkit.unloadWorld(arena.getWorld(), false);

        File activeWorldFolder = new File(Bukkit.getWorldContainer().getParentFile(), world_name);
        if (activeWorldFolder.exists()) delete(activeWorldFolder);



        /*File sourceWorldFolder = new File(GameAPI.getPlugin().getDataFolder() + "/maps/", arena.getID());
        if (sourceWorldFolder.exists()) {
            File activeWorldFolder = new File(Bukkit.getWorldContainer().getParentFile(), sourceWorldFolder.getName() + "_" + game.getID());
            if (activeWorldFolder.exists()) FileManager.deleteFile(activeWorldFolder);
        }*/

        arena.setWorld(null);
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
        if (Bukkit.getPluginManager().getPlugin("SlimeWorldManager") != null) {
            if (GameAPI.getInstance().getSlimeWorldLoader().cloneSlimeArenaWorld(GameAPI.getInstance(), arena.getName(), game.getID())) {
                return;
            }
        }
        ClassicWorldLoader.loadClassicArenaWorld(arena, game);
    }

    public static void loadWorld(String worldName){
        loadWorld(worldName, null);
    }

    public static void loadWorld(String worldName, Player player){
        if (Bukkit.getPluginManager().getPlugin("SlimeWorldManager") != null) {
            if (GameAPI.getInstance().getSlimeWorldLoader().loadSlimeWorld(GameAPI.getInstance(), worldName, player)) {
                return;
            }
        }
        ClassicWorldLoader.loadClassicWorld(worldName, player);
    }





    public static List<String> getLoadedWorlds() {
        return loadedWorlds;
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
