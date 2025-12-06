package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GamePreparationEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private GameInstance game;

    public GamePreparationEvent(GameInstance game){
        this.game = game;
    }

    public GameInstance getGame() {
        return game;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}