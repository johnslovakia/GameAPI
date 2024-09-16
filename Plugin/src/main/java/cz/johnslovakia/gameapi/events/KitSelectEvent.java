package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.kit.Kit;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class KitSelectEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private boolean isCancelled;

    private Kit kit;
    private GamePlayer gamePlayer;

    public KitSelectEvent(GamePlayer gamePlayer, Kit kit){
        this.kit = kit;
        this.gamePlayer = gamePlayer;

    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    public Kit getKit() {
        return kit;
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