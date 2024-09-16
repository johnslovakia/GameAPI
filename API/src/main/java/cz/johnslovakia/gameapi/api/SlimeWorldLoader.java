package cz.johnslovakia.gameapi.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface SlimeWorldLoader {

    public boolean cloneSlimeArenaWorld(Plugin plugin, String arena, String gameID);

    public boolean loadSlimeWorld(Plugin plugin, String worldName);

    public boolean loadSlimeWorld(Plugin plugin, String worldName, Player player);

    public boolean loadSlimeLobbyWorld(Plugin plugin, String worldName);
}
