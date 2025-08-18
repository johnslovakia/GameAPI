package cz.johnslovakia.gameapi.task.tasks;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.task.TaskInterface;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.PlayerBossBar;
import cz.johnslovakia.gameapi.utils.Utils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public class EndCountdown implements TaskInterface {

    @Override
    public void onCount(Task task) {
        Game game = task.getGame();

        for (GamePlayer gamePlayer : game.getParticipants()) {
            PlayerBossBar playerBossBar = PlayerBossBar.getOrCreateBossBar(gamePlayer.getOnlinePlayer().getUniqueId(), Component.text(""));

            Component component = MessageManager.get(gamePlayer, "bossbar.finding_new_game_in")
                        .replace("%time%", Utils.getDurationString(task.getCounter())).getTranslated()
                    .font(Key.key("jsplugins", "bossbar_offset"));
            playerBossBar.setName(component);
        }
    }

    @Override
    public void onEnd(Task task) {
        Game game = task.getGame();

        GameManager.resetGame(game);
        Task.cancelAll(game);
    }
}
