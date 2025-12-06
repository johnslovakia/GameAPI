package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class TeamEliminationEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final GameTeam gameTeam;
    private final int placement;

    public TeamEliminationEvent(GameTeam gameTeam, int placement) {
        this.gameTeam = gameTeam;
        this.placement = placement;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}