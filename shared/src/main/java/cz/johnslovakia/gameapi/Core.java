package cz.johnslovakia.gameapi;

import cz.johnslovakia.gameapi.database.Database;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class Core {

    @Getter
    private static Core instance;

    private final PluginContext context;
    private final JavaPlugin plugin;
    private final Database database;

    public Core(PluginContext context, JavaPlugin plugin, Database database) {
        instance = this;

        this.context = context;
        this.plugin = plugin;
        this.database = database;
    }

    public boolean isMinigame(){
        return context.equals(PluginContext.MINIGAME);
    }

    public boolean isLobby(){
        return context.equals(PluginContext.LOBBY);
    }

    public enum PluginContext {
        MINIGAME,
        LOBBY,
        OTHER
    }
}
