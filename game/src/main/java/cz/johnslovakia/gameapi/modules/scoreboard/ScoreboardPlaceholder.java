package cz.johnslovakia.gameapi.modules.scoreboard;

@FunctionalInterface
public interface ScoreboardPlaceholder {

    String replace(ScoreboardContext context);
}
