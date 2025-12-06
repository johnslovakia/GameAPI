package cz.johnslovakia.gameapi.users.parties;

import cz.johnslovakia.gameapi.users.PlayerIdentity;

import java.util.List;

public interface PartyInterface {

    boolean isInParty();
    List<PlayerIdentity> getAllOnlinePlayers();
    PlayerIdentity getLeader();
}
