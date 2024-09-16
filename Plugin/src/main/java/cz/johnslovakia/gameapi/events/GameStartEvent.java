package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.Game;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GameStartEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private Game game;

    public GameStartEvent(Game game){
        this.game = game;

    }

    public Game getGame() {
        return game;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}