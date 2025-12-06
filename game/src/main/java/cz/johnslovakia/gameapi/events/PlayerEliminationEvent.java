package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class PlayerEliminationEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final GamePlayer gamePlayer;
    private final int placement;

    public PlayerEliminationEvent(GamePlayer gamePlayer, int placement) {
        this.gamePlayer = gamePlayer;
        this.placement = placement;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}