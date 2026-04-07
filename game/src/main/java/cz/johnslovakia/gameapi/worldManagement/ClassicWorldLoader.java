package cz.johnslovakia.gameapi.worldManagement;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.map.Area;
import cz.johnslovakia.gameapi.modules.game.map.GameMap;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.utils.FileManager;
import cz.johnslovakia.gameapi.utils.Logger;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


public class ClassicWorldLoader {

    public static World loadClassicWorld(String worldName) {
        return loadClassicWorld(worldName, (Player) null);
    }

    public static World loadClassicWorld(String worldName, Player player) {
        File source = new File(GameAPI.getInstance().getMinigameDataFolder() + "/maps/", worldName);
        File active = new File(Bukkit.getWorldContainer(), worldName);

        World world = loadClassicWorld(worldName, source, active, true);

        if (player != null && world != null) {
            player.teleport(new Location(world, 0, 90, 0));
        }

        return world;
    }

    public static World loadClassicWorld(String worldName, File source, File active, boolean canLoadWithoutSource) {
        try {
            if (!source.exists() && source.getName().contains(" ")) {
                source = new File(source.getParent(), source.getName().replace(" ", "_"));
            }

            World existing = Bukkit.getWorld(active.getName());
            if (existing != null) {
                applyDefaultGameRules(existing);
                return existing;
            }

            if (!source.exists()) {
                File fallback = new File(Bukkit.getWorldContainer(), worldName);

                if (fallback.exists() && !canLoadWithoutSource) {
                    return loadClassicWorld(worldName, fallback, active, false);
                }

                if (canLoadWithoutSource) {
                    Logger.log("Cannot load Classic World — no map file found, attempting Bukkit fallback. Path: "
                            + source.getAbsolutePath() + " (" + worldName + ")", Logger.LogType.INFO);
                    World world = new WorldCreator(worldName).createWorld();
                    if (world != null) applyDefaultGameRules(world);
                    return world;
                }

                Logger.log("Cannot load Classic World — no map file found. Path: "
                        + source.getAbsolutePath() + " (" + worldName + ")", Logger.LogType.ERROR);
                return null;
            }

            boolean isSchem = source.getName().contains("schem");

            try { Files.deleteIfExists(new File(source, "uid.dat").toPath()); } catch (IOException ignored) {}

            if (!isSchem) {
                WorldFileUtils.deleteFolder(active);
                FileManager.copyFolder(source, active);
            }

            World stale = Bukkit.getWorld(active.getName());
            if (stale != null) {
                if (!stale.getPlayers().isEmpty()) {
                    Logger.log("Cannot reload world '" + active.getName() + "' — players are still inside.", Logger.LogType.ERROR);
                    return null;
                }
                boolean unloaded = Bukkit.unloadWorld(stale, false);
                if (!unloaded) {
                    Logger.log("Failed to synchronously unload stale world: " + active.getName(), Logger.LogType.ERROR);
                    return null;
                }
                WorldManager.loadedWorlds.remove(active.getName());
            }

            WorldCreator creator = new WorldCreator(active.getName());
            if (isSchem) creator.generator(new EmptyChunkGenerator());

            World world = Bukkit.createWorld(creator);
            if (world == null) {
                Logger.log("Bukkit.createWorld returned null for: " + active.getName(), Logger.LogType.ERROR);
                return null;
            }

            if (isSchem) {
                SchematicUtils.pasteSchematic(
                        Minigame.getInstance().getPlugin(),
                        new Location(world, 0, 70, 0),
                        worldName,
                        "maps"
                );
            }

            applyDefaultGameRules(world);
            WorldManager.addLoadedWorld(active.getName());

            return world;

        } catch (IOException e) {
            throw new RuntimeException("IOException while loading world: " + worldName, e);
        }
    }

    public static World loadClassicArenaWorld(GameMap gameMap, GameInstance game) {
        File source = new File(GameAPI.getInstance().getMinigameDataFolder() + "/maps/", gameMap.getName());

        if (!source.exists() && source.getName().contains(" ")) {
            source = new File(source.getParent(), source.getName().replace(" ", "_"));
        }

        if (!source.exists()) {
            File fallback = new File(Bukkit.getWorldContainer(), gameMap.getName());
            if (fallback.exists()) {
                source = fallback;
            } else {
                Logger.log("Cannot load Classic Arena World — no map file found. Path: "
                        + source.getAbsolutePath() + " (" + gameMap.getName() + ")", Logger.LogType.ERROR);
                return null;
            }
        }

        String worldName = source.getName() + "_" + game.getID();
        File active = new File(Bukkit.getWorldContainer(), worldName);

        if (WorldManager.isLoaded(worldName)) {
            return Bukkit.getWorld(worldName);
        }

        try { Files.deleteIfExists(new File(source, "uid.dat").toPath()); } catch (IOException ignored) {}

        World world = loadClassicWorld(gameMap.getName(), source, active, false);
        if (world == null) return null;

        applyDefaultGameRules(world);
        world.setVoidDamageEnabled(true);
        gameMap.setWorld(world);

        Area borderArea = gameMap.getMainArea();
        if (borderArea != null) {
            double lowestY = Math.min(borderArea.getLocation1().getY(), borderArea.getLocation2().getY());
            world.setVoidDamageMinBuildHeightOffset((lowestY - 20) - world.getMinHeight());
        }
        world.setVoidDamageAmount(gameMap.getSettings().isAllowedInstantVoidKill() ? 50.0f : 4.0f);

        WorldManager.addLoadedWorld(worldName);
        return world;
    }

    static void applyDefaultGameRules(World world) {
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.LOCATOR_BAR, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setAutoSave(false);
    }
}