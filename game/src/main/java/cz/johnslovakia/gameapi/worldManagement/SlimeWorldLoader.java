package cz.johnslovakia.gameapi.worldManagement;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.loaders.mysql.MysqlLoader;
import cz.johnslovakia.gameapi.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;


//TODO: přepsat
public class SlimeWorldLoader {
    private final SlimeLoader loader;
    private final AdvancedSlimePaperAPI asp = AdvancedSlimePaperAPI.instance();

    public SlimeWorldLoader(MysqlLoader loader){
        //loader = new FileLoader("slime_worlds");
        this.loader = loader;
    }

    //TODO: přepsat
    public boolean cloneSlimeArenaWorld(Plugin bukkitPlugin, String worldName, String gameID) {
        AdvancedSlimePaperAPI asp = AdvancedSlimePaperAPI.instance();

        String newWorldName = worldName + "_" + gameID;

        if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin"/*"SlimeWorldManager"*/) == null) {
            return false;
        }

        try {
            if (!loader.worldExists(worldName)){
                if (worldName.contains(" ")){
                    cloneSlimeArenaWorld(bukkitPlugin, worldName.replaceAll(" ", "_"), gameID);
                    return true;
                }
                Logger.log("I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported!", Logger.LogType.ERROR);
                //TODO: ClassicWorldLoader.loadClassicArenaWorld(arena, game);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }



        SlimePropertyMap properties = new SlimePropertyMap();
        properties.setValue(SlimeProperties.DIFFICULTY, "hard");
        properties.setValue(SlimeProperties.PVP, true);

        Bukkit.getScheduler().runTaskAsynchronously(bukkitPlugin, () -> {
            try {
                SlimeWorld world = asp.readWorld(loader, worldName, false, properties);

                Bukkit.getScheduler().runTask(bukkitPlugin, () -> {
                    SlimeWorldInstance worldInstance;
                    if (/*asp.worldLoaded(world)*/ Bukkit.getWorld(worldName) != null){
                        worldInstance = asp.loadWorld(world.clone(newWorldName), true);
                    }else{
                        worldInstance = asp.loadWorld(world, true);
                        worldInstance.clone(newWorldName);
                    }

                    World bukkitWorld = worldInstance.getBukkitWorld();
                    applyDefaultGameRules(bukkitWorld);
                    bukkitWorld.setAutoSave(false);
                });
            } catch(UnknownWorldException e){
                Bukkit.getLogger().warning("I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.");
                throw new RuntimeException(e);
            }catch (IOException | CorruptedWorldException | NewerFormatException e) {
                throw new RuntimeException(e);
            }
        });

        return true;
    }

    public boolean loadSlimeWorld(Plugin bukkitPlugin, String worldName) {
        return loadSlimeWorld(bukkitPlugin, worldName, null);
    }

    public boolean loadSlimeWorld(Plugin bukkitPlugin, String worldName, Player player) {

        if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin"/*"SlimeWorldManager"*/) == null) {
            return false;
        }

        try {
            if (!loader.worldExists(worldName)){
                Bukkit.getLogger().warning("SlimeWorldLoader: I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported!");
                //TODO: ClassicWorldLoader.loadClassicWorld(worldName, player);
                return false;
            }
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }


        SlimePropertyMap properties = new SlimePropertyMap();
        properties.setValue(SlimeProperties.DIFFICULTY, "hard");

        Bukkit.getScheduler().runTaskAsynchronously(bukkitPlugin, () -> {
            try {
                SlimeWorld world = asp.readWorld(loader, worldName, false, properties);

                Bukkit.getScheduler().runTask(bukkitPlugin, () -> {
                    if (!asp.worldLoaded(world)) {
                        SlimeWorldInstance worldInstance = asp.loadWorld(world, true);

                        World bukkitWorld = worldInstance.getBukkitWorld();
                        applyDefaultGameRules(bukkitWorld);
                        bukkitWorld.setAutoSave(false);

                        if (player != null) {
                            Location location = new Location(bukkitWorld, 0, 90, 0);
                            player.teleport(location);
                        }
                    }else if (Bukkit.getWorld(worldName) != null) {
                        World bukkitWorld = Bukkit.getWorld(worldName);
                        applyDefaultGameRules(bukkitWorld);
                        bukkitWorld.setAutoSave(false);

                        if (player != null) {
                            Location location = new Location(bukkitWorld, 0, 90, 0);
                            player.teleport(location);
                        }
                    }
                });
            } catch(UnknownWorldException | IOException | CorruptedWorldException | NewerFormatException ex){
                throw new RuntimeException(
                        "I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.",
                        ex
                );
            }
        });

        return true;
    }

    public boolean loadSlimeLobbyWorld(Plugin bukkitPlugin, String worldName) {
        if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin"/*"SlimeWorldManager"*/) == null) {
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
        properties.setValue(SlimeProperties.DIFFICULTY, "hard");

        Bukkit.getScheduler().runTaskAsynchronously(bukkitPlugin, () -> {
            try {
                SlimeWorld world = asp.readWorld(loader, worldName, true, properties);

                Bukkit.getScheduler().runTask(bukkitPlugin, () -> {
                    if (!asp.worldLoaded(world)) {
                        SlimeWorldInstance worldInstance = asp.loadWorld(world, true);

                        World bukkitWorld = worldInstance.getBukkitWorld();
                        applyDefaultGameRules(bukkitWorld);
                        bukkitWorld.setAutoSave(false);
                    }else if (Bukkit.getWorld(worldName) != null) {
                        World bukkitWorld = Bukkit.getWorld(worldName);
                        applyDefaultGameRules(bukkitWorld);
                        bukkitWorld.setAutoSave(false);
                    }
                });
            } catch(IOException | CorruptedWorldException | NewerFormatException | UnknownWorldException e){
                throw new RuntimeException("I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.", e);
            }
        });

        return true;
    }

    static void applyDefaultGameRules(World world) {
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.LOCATOR_BAR, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setAutoSave(false);
    }
}
