package cz.johnslovakia.gameapi.task.tasks;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.task.TaskInterface;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Sounds;

public class GameCountdown implements TaskInterface {
    @Override
    public void onStart(Task task) {
        for (GamePlayer gamePlayer : task.getGame().getParticipants()){
            gamePlayer.getPlayerData().saveAll();
        }
    }

    @Override
    public void onCount(Task task) {

    }

    @Override
    public void onEnd(Task task) {
        Game game = task.getGame();

        for (GamePlayer gamePlayer : game.getPlayers()) {
            MessageManager.get(gamePlayer, "title.time_is_up.title")
                    .send();
            gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer().getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 20.0F, 20.0F);
        }
        game.endGame(null);
    }
}
