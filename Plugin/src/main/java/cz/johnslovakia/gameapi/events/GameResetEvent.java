package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.Game;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class GameResetEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Game game;
    private final Game newGame;

    public GameResetEvent(Game game, Game newGame){
        this.game = game;
        this.newGame = newGame;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}