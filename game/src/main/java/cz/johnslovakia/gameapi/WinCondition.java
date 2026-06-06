package cz.johnslovakia.gameapi;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.game.team.TeamModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class WinCondition {

    private final Predicate<GameInstance> validator;
    private final Consumer<GameInstance> handler;

    private WinCondition(@NotNull Predicate<GameInstance> validator, @NotNull Consumer<GameInstance> handler) {
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.handler = Objects.requireNonNull(handler, "handler must not be null");
    }

    public static WinCondition lastTeamStanding() {
        return custom(
                game -> aliveTeams(game).size() <= 1,
                game -> {
                    List<GameTeam> alive = aliveTeams(game);
                    game.endGame(alive.isEmpty() ? null : alive.get(0));
                }
        );
    }
    public static WinCondition lastPlayerStanding(int minimumAlive) {
        if (minimumAlive < 1) throw new IllegalArgumentException("minimumAlive must be >= 1");
        return custom(
                game -> game.getPlayers().size() <= minimumAlive,
                game -> {
                    List<GamePlayer> alive = game.getPlayers();
                    game.endGame(alive.isEmpty() ? null : alive.get(0));
                }
        );
    }

    public static WinCondition lastPlayerStanding() {
        return lastPlayerStanding(1);
    }

    public static WinCondition custom(@NotNull Predicate<GameInstance> validator, @NotNull Consumer<GameInstance> handler) {
        return new WinCondition(validator, handler);
    }

    public boolean shouldEnd(@NotNull GameInstance game) {
        return validator.test(Objects.requireNonNull(game));
    }

    public void resolve(@NotNull GameInstance game) {
        handler.accept(Objects.requireNonNull(game));
    }

    private static List<GameTeam> aliveTeams(GameInstance game) {
        return game.getModule(TeamModule.class)
                   .getTeams().values().stream()
                   .filter(team -> !team.getAliveMembers().isEmpty())
                   .toList();
    }

    @Override
    public String toString() {
        return "WinCondition{validator=" + validator + ", handler=" + handler + "}";
    }
}
