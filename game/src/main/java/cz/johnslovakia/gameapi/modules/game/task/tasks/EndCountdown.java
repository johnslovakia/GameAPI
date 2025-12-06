package cz.johnslovakia.gameapi.modules.game.task.tasks;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameService;
import cz.johnslovakia.gameapi.modules.game.task.Task;
import cz.johnslovakia.gameapi.modules.game.task.TaskInterface;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.PlayerBossBar;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.utils.Utils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public class EndCountdown implements TaskInterface {

    @Override
    public void onCount(Task task) {
        GameInstance game = task.getGame();

        for (GamePlayer gamePlayer : game.getParticipants()) {
            PlayerBossBar playerBossBar = PlayerBossBar.getOrCreateBossBar(gamePlayer.getOnlinePlayer().getUniqueId(), Component.text(""));

            Component component = ModuleManager.getModule(MessageModule.class).get(gamePlayer, "bossbar.finding_new_game_in")
                        .replace("%time%", Utils.getDurationString(task.getCounter())).getTranslated()
                    .font(Key.key("jsplugins", "bossbar_offset"));
            playerBossBar.setName(component);
        }
    }

    @Override
    public void onEnd(Task task) {
        GameInstance game = task.getGame();

        ModuleManager.getModule(GameService.class).resetGame(game);
    }
}
