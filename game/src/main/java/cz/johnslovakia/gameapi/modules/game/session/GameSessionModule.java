package cz.johnslovakia.gameapi.modules.game.session;

import cz.johnslovakia.gameapi.modules.game.GameModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import org.bukkit.entity.Player;

import java.util.*;

public class GameSessionModule extends GameModule {

    private Map<PlayerIdentity, PlayerGameSession> playerSessions = new HashMap<>();

    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {
        playerSessions = null;
    }

    public List<PlayerGameSession> getPlayerSessions(){
        return playerSessions.values().stream().toList();
    }

    public PlayerGameSession createPlayerSession(PlayerIdentity playerIdentity){
        if (playerSessions.containsKey(playerIdentity)) return getPlayerSession(playerIdentity);

        PlayerGameSession session = new PlayerGameSession(playerIdentity, getGame());
        playerSessions.put(playerIdentity, session);
        return session;
    }

    public PlayerGameSession removePlayerSession(PlayerIdentity playerIdentity){
        return playerSessions.remove(playerIdentity);
    }

    public PlayerGameSession getPlayerSession(PlayerIdentity playerIdentity){
        return playerSessions.get(playerIdentity);
    }

    public PlayerGameSession getPlayerSession(Player player){
        return getPlayerSession(PlayerIdentityRegistry.get(player));
    }
}
