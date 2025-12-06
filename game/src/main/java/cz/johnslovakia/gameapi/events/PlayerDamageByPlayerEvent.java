package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;

public class PlayerDamageByPlayerEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean isCancelled = false;

    private GameInstance game;
    private GamePlayer damager;
    private GamePlayer damaged;

    private EntityDamageEvent.DamageCause cause;

    public PlayerDamageByPlayerEvent(GameInstance game, GamePlayer damager, GamePlayer damaged, EntityDamageEvent.DamageCause cause){
        this.game = game;
        this.damager = damager;
        this.damaged = damaged;
        this.cause = cause;

    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    public GameInstance getGame() {
        return game;
    }

    public GamePlayer getDamager() {
        return damager;
    }

    public GamePlayer getDamaged() {
        return damaged;
    }

    public EntityDamageEvent.DamageCause getCause() {
        return cause;
    }

    public HandlerList getHandlers() {
        return handlers;
    }



    public static HandlerList getHandlerList(){
        return handlers;
    }

}