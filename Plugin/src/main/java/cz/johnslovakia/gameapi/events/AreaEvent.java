package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.map.Area;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.RegisteredListener;


public class AreaEvent extends Event implements Cancellable {

    private boolean cancelled = false;
    private static final HandlerList handlers = new HandlerList();
    private GamePlayer player;
    private Area area;
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

    public Area getArea(){
        return this.area;
    }

    public PlayerMoveEvent getMoveEvent() {
        return moveEvent;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        for(RegisteredListener listener : handlers.getRegisteredListeners()){
            if(!listener.getPlugin().equals(GameAPI.getInstance())){ //GameAPI.getPlugin()
                handlers.unregister(listener);
            }
        }
        return handlers;
    }
    public static HandlerList getHandlerList(){
        return handlers;
    }

}