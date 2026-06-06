package cz.johnslovakia.gameapi.modules.game.session;

import cz.johnslovakia.gameapi.modules.game.GameModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import org.bukkit.entity.Player;

import java.util.*;

public class GameSessionModule extends GameModule {

    private Map<GamePlayer, PlayerGameSession> playerSessions = new HashMap<>();

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

    public PlayerGameSession createPlayerSession(GamePlayer gamePlayer){
        if (playerSessions.containsKey(gamePlayer)) return getPlayerSession(gamePlayer);

        PlayerGameSession session = new PlayerGameSession(gamePlayer, getGame());
        playerSessions.put(gamePlayer, session);
        return session;
    }

    public void removePlayerSession(GamePlayer gamePlayer){
        playerSessions.remove(gamePlayer);
    }

    public PlayerGameSession getPlayerSession(GamePlayer gamePlayer){
        return playerSessions.get(gamePlayer);
    }

    public PlayerGameSession getPlayerSession(Player player){
        return getPlayerSession(PlayerManager.getGamePlayer(player));
    }
}
