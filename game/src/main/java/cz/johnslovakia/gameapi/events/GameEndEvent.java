package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.Placement;
import cz.johnslovakia.gameapi.modules.game.Winner;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.Map;

@Getter
public class GameEndEvent extends Event {

    static final HandlerList handlers = new HandlerList();

    GameInstance game;
    Winner winner;
    Map<GamePlayer, Integer> ranking;

    public GameEndEvent(GameInstance game, Winner winner, Map<GamePlayer, Integer> ranking){
        this.game = game;
        this.winner = winner;
        this.ranking = ranking;
    }

    public List<GamePlayer> getLossers(){
        if (winner == null){
            return List.of();
        }

        return game.getParticipants().stream().filter(gamePlayer ->
                (game.getSettings().isUseTeams() ?
                        !((GameTeam)winner).getAllMembers().contains(gamePlayer)
                        : winner != gamePlayer
                )
        ).toList();
    }

    public List<Placement<?>> getPlacements(){
        return game.getPlacements();
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}