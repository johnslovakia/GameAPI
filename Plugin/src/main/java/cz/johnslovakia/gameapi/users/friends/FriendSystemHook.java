package cz.johnslovakia.gameapi.users.friends;

import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import me.sk8ingduck.friendsystem.SpigotAPI;
import me.sk8ingduck.friendsystem.manager.FriendManager;
import me.sk8ingduck.friendsystem.util.FriendPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class FriendSystemHook implements FriendsInterface{

    private final GamePlayer gamePlayer;
    private final SpigotAPI api;
    private final FriendManager manager;

    public FriendSystemHook(GamePlayer gamePlayer) {
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
    public List<GamePlayer> getAllOnlinePlayers() {
        List<GamePlayer> online = new ArrayList<>();

        if (hasFriends()){
            FriendPlayer friendPlayer = manager.getFriendPlayer(gamePlayer.getOnlinePlayer().getUniqueId());

            for(String name : friendPlayer.getFriends().keySet()){
                if (Bukkit.getPlayer(name) != null){
                    Player bukkitPlayer = Bukkit.getPlayer(name);
                    if (PlayerManager.exists(bukkitPlayer.getUniqueId())){
                        online.add(PlayerManager.getGamePlayer(bukkitPlayer));
                    }
                }
            }
        }

        return online;
    }

    @Override
    public boolean isFriendWith(GamePlayer gamePlayer) {
        if (hasFriends()) {
            return getAllOnlinePlayers().contains(gamePlayer);
        }
        return false;
    }
}
