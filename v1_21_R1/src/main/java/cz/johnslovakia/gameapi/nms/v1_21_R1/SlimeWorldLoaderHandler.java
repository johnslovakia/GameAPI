package cz.johnslovakia.gameapi.nms.v1_21_R1;

import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI;
import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.loaders.file.FileLoader;
import com.infernalsuite.aswm.loaders.mysql.MysqlLoader;
import cz.johnslovakia.gameapi.api.SlimeWorldLoader;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.logging.Logger;

public class SlimeWorldLoaderHandler implements SlimeWorldLoader {

    private final SlimeLoader loader;
    private final AdvancedSlimePaperAPI asp = AdvancedSlimePaperAPI.instance();

    public SlimeWorldLoaderHandler(MysqlLoader loader){
        //loader = new FileLoader("slime_worlds");
        this.loader = loader;
    }

    @Override
    public boolean cloneSlimeArenaWorld(Plugin bukkitPlugin, String arenaID, String gameID) {
        AdvancedSlimePaperAPI asp = AdvancedSlimePaperAPI.instance();

        String newWorldName = arenaID + "_" + gameID;

        if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin"/*"SlimeWorldManager"*/) == null) {
            return false;
        }

        try {
            if (!loader.worldExists(arenaID)){
                if (arenaID.contains(" ")){
                    cloneSlimeArenaWorld(bukkitPlugin, arenaID.replaceAll(" ", "_"), gameID);
                    return true;
                }
                Bukkit.getLogger().warning("I can't upload a World: " + arenaID + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.");
                //TODO: ClassicWorldLoader.loadClassicArenaWorld(arena, game);
                return false;
            }
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }



        SlimePropertyMap properties = new SlimePropertyMap();
        properties.setValue(SlimeProperties.DIFFICULTY, "hard");
        properties.setValue(SlimeProperties.PVP, true);

        Bukkit.getScheduler().runTaskAsynchronously(bukkitPlugin, () -> {
            try {
                SlimeWorld world = asp.readWorld(loader, arenaID, false, properties);

                Bukkit.getScheduler().runTask(bukkitPlugin, () -> {
                    if (/*asp.worldLoaded(world)*/ Bukkit.getWorld(arenaID) != null){
                        asp.loadWorld(world.clone(newWorldName), true);
                    }else{
                        asp.loadWorld(world, true).clone(newWorldName);
                    }

                    World bukkitWorld = Bukkit.getWorld(newWorldName);
                    if (bukkitWorld != null) {
                        bukkitWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                        bukkitWorld.setAutoSave(false);
                    }
                });
            } catch(UnknownWorldException e){
                Bukkit.getLogger().warning("I can't upload a World: " + arenaID + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.");
                throw new RuntimeException(e);
            }catch (IOException | CorruptedWorldException | NewerFormatException e) {
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

        if (Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin"/*"SlimeWorldManager"*/) == null) {
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
        properties.setValue(SlimeProperties.DIFFICULTY, "hard");

        Bukkit.getScheduler().runTaskAsynchronously(bukkitPlugin, () -> {
            try {
                SlimeWorld world = asp.readWorld(loader, worldName, false, properties);

                Bukkit.getScheduler().runTask(bukkitPlugin, () -> {
                    if (!asp.worldLoaded(world)) {
                        asp.loadWorld(world, true);
                    }

                    if (Bukkit.getWorld(worldName) != null) {
                        World bukkitWorld = Bukkit.getWorld(worldName);
                        bukkitWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
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

    @Override
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
                        asp.loadWorld(world, true);
                    }
                    if (Bukkit.getWorld(worldName) != null) {
                        World bukkitWorld = Bukkit.getWorld(worldName);
                        bukkitWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                        bukkitWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                        bukkitWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                        bukkitWorld.setAutoSave(false);
                    }
                });
            } catch(IOException | CorruptedWorldException | NewerFormatException | UnknownWorldException e){
                throw new RuntimeException("I can't upload a World: " + worldName + " using Slime World Manager because the world is not imported! I'll try to load the world in a different way.", e);
            }
        });

        return true;
    }
}
