package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.damage.DamageType;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

@Getter
public class GamePlayerDeathEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean isCancelled;

    private final GameInstance game;
    private final GamePlayer gamePlayer;
    private final GamePlayer killer;
    private final List<GamePlayer> assists;
    private final DamageType dmgType;
    @Setter
    private boolean firstGameKill = false;

    public GamePlayerDeathEvent(GameInstance game, GamePlayer killer, GamePlayer gamePlayer, List<GamePlayer> assists, DamageType dmgType){
        this.game = game;
        this.gamePlayer = gamePlayer;
        this.killer = killer;
        this.dmgType = dmgType;
        this.assists = assists;
    }

    public boolean isFinalKill(){
        if (gamePlayer.getGame().getSettings().isUseTeams()){
            return gamePlayer.getGameSession().getTeam().isDead();
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