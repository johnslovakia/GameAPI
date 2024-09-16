package cz.johnslovakia.gameapi.users.friends;

import cz.johnslovakia.gameapi.users.GamePlayer;

import java.util.List;

public interface FriendsInterface {

    boolean hasFriends();
    List<GamePlayer> getAllOnlinePlayers();
    boolean isFriendWith(GamePlayer gamePlayer);
}
