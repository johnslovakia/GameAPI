package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.map.Area;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;

@Getter
public class AreaEvent extends Event implements Cancellable {

    private boolean isCancelled = false;
    private static final HandlerList handlers = new HandlerList();
    private final GamePlayer player;
    private final Area area;
    private PlayerMoveEvent moveEvent;


    public AreaEvent(GamePlayer player, Area area){
        this.player = player;
        this.area = area;
    }

    public AreaEvent(GamePlayer player, Area area, PlayerMoveEvent moveEvent){
        this.player = player;
        this.area = area;
        this.moveEvent = moveEvent;
    }

    public GamePlayer getGamePlayer(){
        return this.player;
    }


    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    /*@Override
    public HandlerList getHandlers() {
        for(RegisteredListener listener : handlers.getRegisteredListeners()){
            if(!listener.getPlugin().equals(GameAPI.getInstance())){ //GameAPI.getPlugin()
                handlers.unregister(listener);
            }
        }
        return handlers;
    }*/

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }

}