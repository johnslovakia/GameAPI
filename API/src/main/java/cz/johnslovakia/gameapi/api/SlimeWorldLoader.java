package cz.johnslovakia.gameapi.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface SlimeWorldLoader {

    boolean cloneSlimeArenaWorld(Plugin plugin, String arena, String gameID);
    boolean loadSlimeWorld(Plugin plugin, String worldName);
    boolean loadSlimeWorld(Plugin plugin, String worldName, Player player);
    boolean loadSlimeLobbyWorld(Plugin plugin, String worldName);
}
