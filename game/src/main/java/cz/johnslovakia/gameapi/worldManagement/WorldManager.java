package cz.johnslovakia.gameapi.worldManagement;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.map.GameMap;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.utils.GameUtils;
import cz.johnslovakia.gameapi.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorldManager {

    static final List<String> loadedWorlds = Collections.synchronizedList(new ArrayList<>());

    public static void unload(World world) {
        if (world == null) return;

        String worldName = world.getName();

        boolean hasPlayers = !world.getPlayers().isEmpty();
        if (hasPlayers) {
            world.getPlayers().forEach(player -> {
                player.sendMessage("§cAn error occurred — you are being sent to the lobby.");
                GameUtils.sendToLobby(player, false);
            });
        }

        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
            try {
                boolean success = Bukkit.unloadWorld(world, false);
                if (!success) {
                    Logger.log("Failed to unload world: " + worldName, Logger.LogType.ERROR);
                    return;
                }
                loadedWorlds.remove(worldName);
            } catch (Exception e) {
                Logger.log("Exception while unloading world: " + worldName, Logger.LogType.ERROR);
                e.printStackTrace();
                return;
            }

            Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), () ->
                    WorldFileUtils.deleteFolder(new File(Bukkit.getWorldContainer(), worldName))
            );

        }, hasPlayers ? 65L : 0L);
    }

    public static void loadLobbyWorld(String worldName) {
        ClassicWorldLoader.loadClassicWorld(worldName);

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        }
    }

    public static void loadArenaWorld(GameMap arena, GameInstance game) {
        if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin") != null) {
            if (GameAPI.getInstance().getSlimeWorldLoader()
                    .cloneSlimeArenaWorld(Minigame.getInstance().getPlugin(), arena.getName(), game.getID())) {
                return;
            }
        }
        ClassicWorldLoader.loadClassicArenaWorld(arena, game);
    }

    public static void loadWorld(String worldName) {
        loadWorld(worldName, null);
    }

    public static void loadWorld(String worldName, Player player) {
        if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin") != null) {
            if (GameAPI.getInstance().getSlimeWorldLoader()
                    .loadSlimeWorld(Minigame.getInstance().getPlugin(), worldName, player)) {
                return;
            }
        }
        ClassicWorldLoader.loadClassicWorld(worldName, player);
    }

    public static void cloneWorld(String worldName, String id) {
        if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin") != null) {
            if (GameAPI.getInstance().getSlimeWorldLoader()
                    .cloneSlimeArenaWorld(Minigame.getInstance().getPlugin(), worldName, id)) {
                return;
            }
        }

        File source = new File(GameAPI.getInstance().getMinigameDataFolder() + "/maps/", worldName);
        File active = new File(Bukkit.getWorldContainer(), worldName + "_" + id);
        ClassicWorldLoader.loadClassicWorld(worldName, source, active, false);
    }

    public static void addLoadedWorld(String worldName) {
        if (!loadedWorlds.contains(worldName)) {
            loadedWorlds.add(worldName);
        }
    }

    public static boolean isLoaded(String worldName) {
        return loadedWorlds.contains(worldName);
    }
}