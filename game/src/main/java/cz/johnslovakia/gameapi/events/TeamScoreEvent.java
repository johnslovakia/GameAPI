package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.game.team.TeamScore;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class TeamScoreEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private GameTeam team;
    private TeamScore score;


    public TeamScoreEvent(GameTeam gameTeam, TeamScore score) {
        this.team = gameTeam;
        this.score = score;
    }

    public GameInstance getGame() {
        return team.getGame();
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}