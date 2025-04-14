package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.kit.Kit;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class KitGiveContentEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private boolean isCancelled;

    private final Kit kit;
    private final GamePlayer gamePlayer;

    public KitGiveContentEvent(GamePlayer gamePlayer, Kit kit){
        this.kit = kit;
        this.gamePlayer = gamePlayer;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }

}