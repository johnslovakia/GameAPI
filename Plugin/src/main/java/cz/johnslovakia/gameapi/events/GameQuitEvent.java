package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GameQuitEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private boolean isCancelled;

    private Game game;
    private GamePlayer gamePlayer;

    public GameQuitEvent(Game game, GamePlayer gamePlayer){
        this.game = game;
        this.gamePlayer = gamePlayer;
    }


    public Game getGame() {
        return game;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    public GamePlayer getGamePlayer() {
        return gamePlayer;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }

}