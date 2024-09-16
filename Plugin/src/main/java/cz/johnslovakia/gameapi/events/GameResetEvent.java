package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.Game;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GameResetEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private Game game;
    private Game newGame;

    public GameResetEvent(Game game, Game newGame){
        this.game = game;
        this.newGame = newGame;
    }

    public Game getGame() {
        return game;
    }

    public Game getNewGame() {
        return newGame;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}