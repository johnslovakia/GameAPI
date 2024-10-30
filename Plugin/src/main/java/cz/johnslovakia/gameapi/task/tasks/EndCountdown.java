package cz.johnslovakia.gameapi.task.tasks;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.GameManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.task.TaskInterface;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class EndCountdown implements TaskInterface {


    private BossBar bossBar;

    @Override
    public void onStart(Task task) {
        this.bossBar = Bukkit.createBossBar("Game starting in:", BarColor.YELLOW ,BarStyle.SOLID);

        for (GamePlayer gamePlayer : task.getGame().getPlayers()){
            bossBar.setTitle(MessageManager.get(gamePlayer, "bossbar.finding_new_game_in")
                    .replace("%time%", Utils.getDurationString(task.getCounter()))
                    .getTranslated());
            bossBar.setProgress(task.getCounter() / (double) task.getStartCounter());
            bossBar.setVisible(true);
            bossBar.addPlayer(gamePlayer.getOnlinePlayer());
        }
    }

    @Override
    public void onCount(Task task) {
        Game game = task.getGame();

        for (GamePlayer gamePlayer : game.getPlayers()) {
            bossBar.setTitle(MessageManager.get(gamePlayer, "bossbar.finding_new_game_in")
                    .replace("%time%", Utils.getDurationString(task.getCounter()))
                    .getTranslated());
            bossBar.setProgress(task.getCounter() / (double) task.getStartCounter());
        }
    }

    @Override
    public void onEnd(Task task) {
        Game game = task.getGame();

        bossBar.removeAll();

        GameManager.resetGame(game);
        Task.cancelAll(game);
    }
}
