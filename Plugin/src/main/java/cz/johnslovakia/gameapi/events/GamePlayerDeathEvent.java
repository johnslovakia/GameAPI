package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;

public class GamePlayerDeathEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean isCancelled;

    private Game game;
    private GamePlayer gamePlayer;
    private GamePlayer killer;
    private List<GamePlayer> assists;
    private EntityDamageEvent.DamageCause dmgCause;
    private boolean firstGameKill = false;

    public GamePlayerDeathEvent(Game game, GamePlayer killer, GamePlayer gamePlayer, List<GamePlayer> assists, EntityDamageEvent.DamageCause dmgCause){
        this.game = game;
        this.gamePlayer = gamePlayer;
        this.killer = killer;
        this.dmgCause = dmgCause;
        this.assists = assists;
    }

    public boolean isFirstGameKill() {
        return firstGameKill;
    }

    public void setFirstGameKill(boolean firstGameKill) {
        this.firstGameKill = firstGameKill;
    }

    public boolean isFinalKill(){
        if (gamePlayer.getPlayerData().getGame().getSettings().useTeams()){
            if (gamePlayer.getPlayerData().getTeam().isDead()){
                return true;
            }
            return false;
        }else{
            return true;
        }
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    public Game getGame() {
        return game;
    }


    public GamePlayer getPlayer() {
        return gamePlayer;
    }

    public GamePlayer getKiller() {
        return killer;
    }

    public EntityDamageEvent.DamageCause getDmgCause() {
        return dmgCause;
    }

    public List<GamePlayer> getAssists() {
        return assists;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }

}