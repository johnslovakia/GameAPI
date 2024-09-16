package cz.johnslovakia.gameapi.events;

import cz.johnslovakia.gameapi.users.ScoreAction;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerScore;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerScoreEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private Game game;
    private GamePlayer gamePlayer;
    private PlayerScore score;
    private ScoreAction action;


    public PlayerScoreEvent(GamePlayer gamePlayer, PlayerScore score, ScoreAction action) {
        this.game = gamePlayer.getPlayerData().getGame();
        this.gamePlayer = gamePlayer;
        this.score = score;
        this.action = action;
    }

    public GamePlayer getGamePlayer() {
        return gamePlayer;
    }

    public PlayerScore getScore() {
        return score;
    }

    public ScoreAction getAction() {
        return action;
    }

    public Game getGame() {
        return this.game;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}