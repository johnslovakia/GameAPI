package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NewArenaEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private boolean isCancelled = false;

    private final GamePlayer gamePlayer;

    public NewArenaEvent(GamePlayer gamePlayer){
        this.gamePlayer = gamePlayer;
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