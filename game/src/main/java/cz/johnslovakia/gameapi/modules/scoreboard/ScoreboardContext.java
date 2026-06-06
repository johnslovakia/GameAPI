package cz.johnslovakia.gameapi.modules.scoreboard;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.task.Task;
import cz.johnslovakia.gameapi.modules.game.task.TaskModule;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.StringUtils;
import lombok.Getter;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Optional;

@Getter
public final class ScoreboardContext {

    private final Player player;
    private final GamePlayer gamePlayer;
    private final GameInstance game;
    private final GameState gameState;
    private final String source;
    private final boolean title;

    public ScoreboardContext(Player player, GamePlayer gamePlayer, GameInstance game, GameState gameState, String source, boolean title) {
        this.player = player;
        this.gamePlayer = gamePlayer;
        this.game = game;
        this.gameState = gameState;
        this.source = source;
        this.title = title;
    }

    public String translate(String key) {
        MessageModule messageModule = ModuleManager.getModule(MessageModule.class);
        if (messageModule == null || gamePlayer == null) {
            return key;
        }
        return LegacyComponentSerializer.legacySection()
                .serialize(messageModule.getMessage(gamePlayer, key).toComponent());
    }

    public boolean hasTranslation(String key) {
        MessageModule messageModule = ModuleManager.getModule(MessageModule.class);
        return messageModule != null && gamePlayer != null && messageModule.hasMessage(gamePlayer, key);
    }

    public Optional<Task> getTask(String taskId) {
        if (game == null || game.getModule(TaskModule.class) == null) {
            return Optional.empty();
        }
        return game.getModule(TaskModule.class).getTask(taskId);
    }

    public String formatDuration(int seconds) {
        return StringUtils.getDurationString(seconds);
    }
}
