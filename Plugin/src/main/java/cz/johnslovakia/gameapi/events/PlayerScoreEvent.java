package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.users.ScoreAction;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerScore;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class PlayerScoreEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final GamePlayer gamePlayer;
    private final PlayerScore score;
    private final ScoreAction action;


    public PlayerScoreEvent(GamePlayer gamePlayer, PlayerScore score, ScoreAction action) {
        this.gamePlayer = gamePlayer;
        this.score = score;
        this.action = action;
    }

    public Game getGame() {
        return gamePlayer.getGame();
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}