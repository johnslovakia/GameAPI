package cz.johnslovakia.gameapi.nms.v1_21_R1;

import com.infernalsuite.aswm.api.SlimePlugin;
import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.exceptions.WorldLockedException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import cz.johnslovakia.gameapi.api.SlimeWorldLoader;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;

public class SlimeWorldLoaderHandler implements SlimeWorldLoader {

    @Override
    public boolean cloneSlimeArenaWorld(Plugin bukkitPlugin, String arenaID, String gameID) {

        SlimePlugin plugin;
        SlimeLoader loader;

        String worldName = arenaID;
        String newWorldName = arenaID + "_" + gameID;

        if (Bukkit.getPluginManager().getPlugin("SlimeWorldManager") != null) {
            plugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
            loader = plugin.getLoader("mysql");
            try {
                if (loader == null || !loader.worldExists(worldName)){
                    plugin.getLoader("files");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else{
            return false;
        }



        try {
            if (!loader.worldExists(worldName)){
                Bukkit.getLogger().warning("I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.");
                //TODO: ClassicWorldLoader.loadClassicArenaWorld(arena, game);
                return false;
            }
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }



        SlimePropertyMap properties = new SlimePropertyMap();
        properties.setValue(SlimeProperties.DIFFICULTY, "normal");


        Bukkit.getScheduler().runTaskAsynchronously(bukkitPlugin, () -> {
            try {
                SlimeWorld world = plugin.loadWorld(loader, worldName, true, properties).clone(newWorldName);

                Bukkit.getScheduler().runTask(bukkitPlugin, () -> {
                    try {
                        plugin.loadWorld(world, true);

                        World bukkitWorld = Bukkit.getWorld(newWorldName);
                        if (bukkitWorld != null) {
                            bukkitWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                            bukkitWorld.setAutoSave(false);
                            //TODO: arena.setWorld(bukkitWorld);
                        }
                    } catch(UnknownWorldException e){
                        Bukkit.getLogger().warning("I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.");
                    }catch (WorldLockedException | IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch(UnknownWorldException e){
                Bukkit.getLogger().warning("I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.");
                throw new RuntimeException(e);
            }catch (WorldLockedException | IOException | CorruptedWorldException | NewerFormatException e) {
                throw new RuntimeException(e);
            }
        });

        return true;
    }

    @Override
    public boolean loadSlimeWorld(Plugin bukkitPlugin, String worldName) {
        return loadSlimeWorld(bukkitPlugin, worldName, null);
    }

    @Override
    public boolean loadSlimeWorld(Plugin bukkitPlugin, String worldName, Player player) {
        SlimePlugin plugin;
        SlimeLoader loader;

        if (Bukkit.getPluginManager().getPlugin("SlimeWorldManager") != null) {
            plugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
            loader = plugin.getLoader("mysql");
            try {
                if (loader == null || !loader.worldExists(worldName)){
                    plugin.getLoader("files");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else{
            return false;
        }
        

        try {
            if (!loader.worldExists(worldName)){
                Bukkit.getLogger().warning("SlimeWorldLoader: I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.");
                //TODO: ClassicWorldLoader.loadClassicWorld(worldName, player);
                return false;
            }
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }


        SlimePropertyMap properties = new SlimePropertyMap();
        properties.setValue(SlimeProperties.DIFFICULTY, "normal");


        Bukkit.getScheduler().runTaskAsynchronously(bukkitPlugin, () -> {
            try {
                SlimeWorld world = plugin.loadWorld(loader, worldName, true, properties);

                Bukkit.getScheduler().runTask(bukkitPlugin, () -> {
                    try {
                        plugin.loadWorld(world, true);

                        if (Bukkit.getWorld(worldName) != null) {
                            World bukkitWorld = Bukkit.getWorld(worldName);
                            bukkitWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                            bukkitWorld.setAutoSave(false);

                            if (player != null){
                                Location location = new Location(bukkitWorld, 0, 90, 0);
                                player.teleport(location);
                            }
                        }
                    } catch(UnknownWorldException e){
                        Bukkit.getLogger().warning("I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.");
                    }catch (WorldLockedException | IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch(UnknownWorldException e){
                Bukkit.getLogger().warning("I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.");
            } catch (IOException | CorruptedWorldException | NewerFormatException |
                     WorldLockedException exception) {
                exception.printStackTrace();
            }
        });
        return true;
    }

    @Override
    public boolean loadSlimeLobbyWorld(Plugin bukkitPlugin, String worldName) {
        SlimePlugin plugin;
        SlimeLoader loader;

        if (Bukkit.getPluginManager().getPlugin("SlimeWorldManager") != null) {
            plugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
            loader = plugin.getLoader("mysql");
            try {
                if (loader == null || !loader.worldExists(worldName)){
                    plugin.getLoader("files");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else{
            return false;
        }
        

        try {
            if (!loader.worldExists(worldName)){
                Bukkit.getLogger().warning("SlimeWorldLoader: I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.");
                //TODO: World bukkitWorld = ClassicWorldLoader.loadClassicWorld(worldName);
                return false;
            }
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }


        SlimePropertyMap properties = new SlimePropertyMap();
        properties.setValue(SlimeProperties.DIFFICULTY, "normal");


        Bukkit.getScheduler().runTaskAsynchronously(bukkitPlugin, () -> {
            try {
                SlimeWorld world = plugin.loadWorld(loader, worldName, true, properties);

                Bukkit.getScheduler().runTask(bukkitPlugin, () -> {
                    try {
                        plugin.loadWorld(world, true);
                        if (Bukkit.getWorld(worldName) != null) {
                            World bukkitWorld = Bukkit.getWorld(worldName);
                            bukkitWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                            bukkitWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                            bukkitWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                            bukkitWorld.setAutoSave(false);
                        }
                    } catch(UnknownWorldException e){
                        Bukkit.getLogger().warning("I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.");
                    }catch (WorldLockedException | IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch(UnknownWorldException e){
                Bukkit.getLogger().warning("I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.");
            } catch (IOException | CorruptedWorldException | NewerFormatException |
                     WorldLockedException exception) {
                exception.printStackTrace();
            }
        });
        return true;
    }
}
