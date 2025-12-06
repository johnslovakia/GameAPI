package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class GameStateChangeEvent extends Event {

    static final HandlerList handlers = new HandlerList();

    GameInstance game;
    GameState gameState;

    public GameStateChangeEvent(GameInstance game, GameState gameState){
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