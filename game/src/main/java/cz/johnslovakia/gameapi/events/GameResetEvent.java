package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class GameResetEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final GameInstance game;
    private final GameInstance newGame;

    public GameResetEvent(GameInstance game, GameInstance newGame){
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