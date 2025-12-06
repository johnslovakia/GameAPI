package cz.johnslovakia.gameapi.users.friends;

import cz.johnslovakia.gameapi.users.PlayerIdentity;
import org.bukkit.entity.Player;

import java.util.List;

public interface FriendsInterface {

    boolean hasFriends();
    List<PlayerIdentity> getAllOnlinePlayers();
    boolean isFriendWith(PlayerIdentity playerIdentity);
}
