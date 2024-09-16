package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.game.team.TeamScore;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TeamScoreEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private GameTeam team;
    private TeamScore score;


    public TeamScoreEvent(GameTeam gameTeam, TeamScore score) {
        this.team = gameTeam;
        this.score = score;
    }

    public GameTeam getTeam() {
        return team;
    }

    public TeamScore getScore() {
        return score;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}