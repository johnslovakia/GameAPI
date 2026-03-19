package cz.johnslovakia.gameapi.modules.resources;

import cz.johnslovakia.gameapi.users.PlayerIdentity;
import org.bukkit.OfflinePlayer;

@FunctionalInterface
public interface ResourceChangeListener {
    void onChange(OfflinePlayer offlinePlayer, int amount, ResourceChangeType type);

}