package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.Set;

@Getter
public class PlayerEliminationEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final GamePlayer gamePlayer;
    private final GamePlayer killer;
    private final List<GamePlayer> assists;
    private final int placement;

    public PlayerEliminationEvent(GamePlayer gamePlayer, GamePlayer killer, List<GamePlayer> assists, int placement) {
        this.gamePlayer = gamePlayer;
        this.killer = killer;
        this.assists = assists;
        this.placement = placement;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}