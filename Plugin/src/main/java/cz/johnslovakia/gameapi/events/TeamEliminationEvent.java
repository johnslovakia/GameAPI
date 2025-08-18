package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerScore;
import cz.johnslovakia.gameapi.users.ScoreAction;
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