package cz.johnslovakia.gameapi.users.friends;

import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import me.sk8ingduck.friendsystem.SpigotAPI;
import me.sk8ingduck.friendsystem.manager.FriendManager;
import me.sk8ingduck.friendsystem.util.FriendPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class FriendSystemHook implements FriendsInterface{

    private final PlayerIdentity gamePlayer;
    private final SpigotAPI api;
    private final FriendManager manager;

    public FriendSystemHook(PlayerIdentity gamePlayer) {
        this.gamePlayer = gamePlayer;
        this.api = SpigotAPI.getInstance();
        this.manager = api.getFriendManager();
    }


    @Override
    public boolean hasFriends() {
        FriendPlayer friendPlayer = manager.getFriendPlayer(gamePlayer.getOnlinePlayer().getUniqueId());

        return friendPlayer != null && !friendPlayer.getFriends().isEmpty();
    }

    @Override
    public List<PlayerIdentity> getAllOnlinePlayers() {
        List<PlayerIdentity> online = new ArrayList<>();

        if (hasFriends()){
            FriendPlayer friendPlayer = manager.getFriendPlayer(gamePlayer.getOnlinePlayer().getUniqueId());

            for(String name : friendPlayer.getFriends().keySet()){
                if (Bukkit.getPlayer(name) != null){
                    Player bukkitPlayer = Bukkit.getPlayer(name);
                    if (PlayerIdentityRegistry.exists(bukkitPlayer.getUniqueId())){
                        online.add(PlayerIdentityRegistry.get(bukkitPlayer));
                    }
                }
            }
        }

        return online;
    }

    @Override
    public boolean isFriendWith(PlayerIdentity gamePlayer) {
        if (hasFriends()) {
            return getAllOnlinePlayers().contains(gamePlayer);
        }
        return false;
    }
}
