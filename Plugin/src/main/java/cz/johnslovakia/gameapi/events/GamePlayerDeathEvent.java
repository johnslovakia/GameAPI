package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;

@Getter
public class GamePlayerDeathEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean isCancelled;

    private Game game;
    private GamePlayer gamePlayer;
    private GamePlayer killer;
    private List<GamePlayer> assists;
    private EntityDamageEvent.DamageCause dmgCause;
    @Setter
    private boolean firstGameKill = false;

    public GamePlayerDeathEvent(Game game, GamePlayer killer, GamePlayer gamePlayer, List<GamePlayer> assists, EntityDamageEvent.DamageCause dmgCause){
        this.game = game;
        this.gamePlayer = gamePlayer;
        this.killer = killer;
        this.dmgCause = dmgCause;
        this.assists = assists;
    }

    public boolean isFinalKill(){
        if (gamePlayer.getPlayerData().getGame().getSettings().useTeams()){
            return gamePlayer.getPlayerData().getTeam().isDead();
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

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }

}