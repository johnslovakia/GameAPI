package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GameQuitEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private boolean isCancelled;

    @Getter
    private final GameInstance game;
    @Getter
    private final GamePlayer gamePlayer;

    public GameQuitEvent(GameInstance game, GamePlayer gamePlayer){
        this.game = game;
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