package cz.johnslovakia.gameapi.modules.scoreboard;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class PlayerScoreboard {

    private final ScoreboardModule module;
    private final Player player;
    private final FastBoard board;
    private final String gameId;

    PlayerScoreboard(ScoreboardModule module, Player player, GamePlayer gamePlayer) {
        this.module = module;
        this.player = player;
        this.board = new FastBoard(player);
        GameInstance game = gamePlayer.getGame();
        this.gameId = game != null ? game.getID() : null;
    }

    public void update() {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        GameInstance game = gamePlayer.getGame();
        if (game == null) {
            return;
        }

        GameState state = game.getState();
        ScoreboardTemplate template = module.getTemplate(gamePlayer);

        String title = template != null ? template.getTitle(state) : null;
        if (title == null) {
            title = module.getDefaultTitle();
        }
        board.updateTitle(module.renderComponent(title, new ScoreboardContext(player, gamePlayer, game, state, title, true)));

        List<String> rawLines = template != null ? template.getLines(state) : null;
        if (rawLines == null) {
            rawLines = List.of("");
        }

        List<Component> renderedLines = new ArrayList<>(rawLines.size());
        for (String line : rawLines) {
            renderedLines.add(module.renderComponent(line, new ScoreboardContext(player, gamePlayer, game, state, line, false)));
        }
        board.updateLines(renderedLines);
    }

    public void delete() {
        board.delete();
    }
}
