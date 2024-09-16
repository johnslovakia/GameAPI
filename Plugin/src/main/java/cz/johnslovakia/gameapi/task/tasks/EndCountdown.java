package cz.johnslovakia.gameapi.task.tasks;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.GameManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.task.TaskInterface;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.bukkit.entity.Player;

public class EndCountdown implements TaskInterface {
    @Override
    public void onStart(Task task) {

    }

    @Override
    public void onCount(Task task) {
        Game game = task.getGame();

        for (GamePlayer players : game.getPlayers()) {
            Player player = players.getOnlinePlayer();

            if (GameManager.getGames().size() > 1) {
                if (task.getCounter() <= 3 && task.getCounter() != 0) {
                    MessageManager.get(player, "chat.searching_game")
                            .replace("%time%", "" + task.getCounter())
                            .send();
                }
            }

        }
    }

    @Override
    public void onEnd(Task task) {
        Game game = task.getGame();

        game.setState(GameState.ENDING);
        GameManager.resetGame(game);
        Task.cancelAll(game);
    }
}
