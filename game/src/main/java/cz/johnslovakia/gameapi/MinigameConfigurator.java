package cz.johnslovakia.gameapi;

import cz.johnslovakia.gameapi.database.Database;
import cz.johnslovakia.gameapi.database.MinigameTable;
import cz.johnslovakia.gameapi.database.RedisManager;
import cz.johnslovakia.gameapi.modules.serverManagement.IMinigame;
import cz.johnslovakia.gameapi.modules.serverManagement.ServerRegistry;
import cz.johnslovakia.gameapi.modules.serverManagement.gameData.JSONProperty;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.scoreboard.ScoreboardSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
public final class MinigameConfigurator {

    private final Minigame minigame;

    MinigameConfigurator(@NotNull Minigame minigame) {
        this.minigame = Objects.requireNonNull(minigame);
    }

    public MinigameConfigurator fullname(@Nullable String fullName) {
        minigame.setFullName(fullName);
        return this;
    }

    public MinigameConfigurator descriptionKey(@NotNull String key) {
        minigame.setDescriptionTranslateKey(Objects.requireNonNull(key));
        return this;
    }

    public MinigameConfigurator minigameTable(@NotNull String namespace) {
        minigame.setMinigameTable(new MinigameTable(Objects.requireNonNull(namespace)));
        return this;
    }

    public MinigameConfigurator settings(@NotNull MinigameSettings settings) {
        minigame.setSettings(Objects.requireNonNull(settings));
        return this;
    }

    public MinigameConfigurator scoreboard(@NotNull ScoreboardSettings settings) {
        minigame.setScoreboardSettings(Objects.requireNonNull(settings));
        return this;
    }

    public MinigameConfigurator scoreboard(@NotNull Consumer<ScoreboardSettings.Builder> updater) {
        ScoreboardSettings.Builder builder = ScoreboardSettings.builder();
        updater.accept(builder);
        return scoreboard(builder.build());
    }

    public MinigameConfigurator scoreboard() {
        return scoreboard(ScoreboardSettings.defaults());
    }

    public MinigameConfigurator updateSettings(@NotNull Consumer<MinigameSettings.Builder> updater) {
        if (minigame.getSettings() == null) {
            throw new IllegalStateException(
                    "Call settings(MinigameSettings) before updateSettings(Consumer).");
        }
        minigame.updateSettings(updater);
        return this;
    }

    public MinigameConfigurator winCondition(@NotNull WinCondition condition) {
        Objects.requireNonNull(condition, "winCondition must not be null");
        minigame.setWinCondition(condition);
        return this;
    }


    public MinigameConfigurator database(@Nullable Database database) {
        minigame.setDatabase(database);
        return this;
    }

    public MinigameConfigurator bungeeRedis(@NotNull RedisManager redis) {
        ServerRegistry registry = minigame.getModuleManager().registerModule(new ServerRegistry(redis));
        registry.addMinigame(new IMinigame(registry, minigame.getFullName()));
        return this;
    }

    public MinigameConfigurator bungeeMySQL(@NotNull Database db) {
        ServerRegistry registry = minigame.getModuleManager().registerModule(new ServerRegistry(db));
        registry.addMinigame(new IMinigame(registry, minigame.getFullName()));
        return this;
    }

    public MinigameConfigurator gameProperty(@NotNull JSONProperty<GameInstance> property) {
        minigame.getProperties().add(Objects.requireNonNull(property));
        return this;
    }

    @NotNull
    public Minigame minigame() {
        return minigame;
    }
}
