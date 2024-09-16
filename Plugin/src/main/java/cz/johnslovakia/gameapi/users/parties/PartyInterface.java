package cz.johnslovakia.gameapi.users.parties;

import cz.johnslovakia.gameapi.users.GamePlayer;

import java.util.List;

public interface PartyInterface {

    boolean isInParty();
    List<GamePlayer> getAllOnlinePlayers();
    GamePlayer getLeader();
}
