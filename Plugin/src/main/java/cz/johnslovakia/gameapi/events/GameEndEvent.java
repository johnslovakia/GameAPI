package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.Winner;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class GameEndEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private Game game;
    private Winner winner;

    public GameEndEvent(Game game, Winner winner){
        this.game = game;
        this.winner = winner;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}