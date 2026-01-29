package cz.johnslovakia.gameapi.modules.game.session;

import cz.johnslovakia.gameapi.modules.game.GameModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameSessionModule extends GameModule {

    private Map<PlayerIdentity, PlayerGameSession> playerSessions = new HashMap<>();

    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {
        playerSessions = null;
    }

    public PlayerGameSession createPlayerSession(PlayerIdentity playerIdentity){
        if (playerSessions.containsKey(playerIdentity)) return getPlayerSession(playerIdentity);

        PlayerGameSession session = new PlayerGameSession(playerIdentity, getGame());
        playerSessions.put(playerIdentity, session);
        return session;
    }

    public PlayerGameSession getPlayerSession(PlayerIdentity playerIdentity){
        return playerSessions.get(playerIdentity);
    }

    public PlayerGameSession getPlayerSession(Player player){
        return getPlayerSession(PlayerIdentityRegistry.get(player));
    }

    public PlayerGameSession getPlayerSession(UUID uuid){
        return getPlayerSession(PlayerIdentityRegistry.get(uuid));
    }
}
