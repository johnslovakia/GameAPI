package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class GameJoinEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private boolean isCancelled;

    private Game game;
    private GamePlayer gamePlayer;
    private JoinType joinType;


    public GameJoinEvent(Game game, GamePlayer gamePlayer, JoinType joinType){
        this.game = game;
        this.gamePlayer = gamePlayer;
        this.joinType = joinType;
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


    public enum JoinType{
        LOBBY, SPECTATOR, REJOIN, JOIN_AFTER_START;
    }

}