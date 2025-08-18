package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.kit.Kit;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class KitActiveEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private boolean isCancelled;
    private boolean giveItems = true;

    private final Kit kit;
    private final GamePlayer gamePlayer;

    public KitActiveEvent(GamePlayer gamePlayer, Kit kit){
        this.kit = kit;
        this.gamePlayer = gamePlayer;

    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    public boolean isGiveItems() {
        return giveItems;
    }

    public void setGiveItems(boolean giveItems) {
        this.giveItems = giveItems;
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