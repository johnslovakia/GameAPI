package cz.johnslovakia.gameapi;

import cz.johnslovakia.gameapi.database.Database;
import cz.johnslovakia.gameapi.utils.SlotHighlightSystem;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class Shared {

    @Getter
    private static Shared instance;

    private final JavaPlugin plugin;
    private final Database database;

    public Shared(JavaPlugin plugin, Database database) {
        instance = this;

        this.plugin = plugin;
        this.database = database;
    }
}
