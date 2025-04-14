package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.Winner;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.Map;

@Getter
public class GameStateChangeEvent extends Event {

    static final HandlerList handlers = new HandlerList();

    Game game;
    GameState gameState;

    public GameStateChangeEvent(Game game, GameState gameState){
        this.game = game;
        this.gameState = gameState;
    }


    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}