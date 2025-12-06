package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.scores.Score;
import cz.johnslovakia.gameapi.users.ScoreAction;
import cz.johnslovakia.gameapi.users.GamePlayer;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class PlayerScoreEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final GamePlayer gamePlayer;
    private final Score score;
    private final ScoreAction action;

    public PlayerScoreEvent(GamePlayer gamePlayer, Score score, ScoreAction action) {
        this.gamePlayer = gamePlayer;
        this.score = score;
        this.action = action;
    }

    public GameInstance getGame() {
        return gamePlayer.getGame();
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}