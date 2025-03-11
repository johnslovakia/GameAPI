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

import java.util.HashMap;
import java.util.Map;

public class EndCountdown implements TaskInterface {

    private  void createBossBar(GamePlayer gamePlayer, Task task){
        if (gamePlayer.getPlayerData().getCurrentBossBar() != null){
            gamePlayer.getPlayerData().getCurrentBossBar().removeAll();
        }

        BossBar bossBar = Bukkit.createBossBar("Game starting in:", BarColor.WHITE,BarStyle.SOLID);

        bossBar.setTitle(MessageManager.get(gamePlayer, "bossbar.finding_new_game_in")
                .replace("%time%", Utils.getDurationString(task.getCounter()))
                .getFontTextComponentJSON("gameapi:bossbar_offset"));

        bossBar.setVisible(true);
        bossBar.addPlayer(gamePlayer.getOnlinePlayer());
        gamePlayer.getPlayerData().setCurrentBossBar(bossBar);
    }
    
    @Override
    public void onStart(Task task) {
        for (GamePlayer gamePlayer : task.getGame().getParticipants()){
            createBossBar(gamePlayer, task);
        }
    }

    @Override
    public void onCount(Task task) {
        Game game = task.getGame();

        for (GamePlayer gamePlayer : game.getParticipants()) {
            BossBar bossBar = gamePlayer.getPlayerData().getCurrentBossBar();
            if(bossBar == null){
                createBossBar(gamePlayer, task);
                bossBar = gamePlayer.getPlayerData().getCurrentBossBar();
            }
            bossBar.setTitle(MessageManager.get(gamePlayer, "bossbar.finding_new_game_in")
                    .replace("%time%", Utils.getDurationString(task.getCounter()))
                    .getFontTextComponentJSON("gameapi:bossbar_offset"));
            
        }
    }

    @Override
    public void onEnd(Task task) {
        Game game = task.getGame();

        for (GamePlayer gamePlayer : game.getParticipants()) {
            BossBar bossBar = gamePlayer.getPlayerData().getCurrentBossBar();
            if (bossBar != null) {
                bossBar.removeAll();
            }
        }

        GameManager.resetGame(game);
        Task.cancelAll(game);
    }

    @Override
    public void onCancel(Task task) {
        for (GamePlayer gamePlayer : task.getGame().getParticipants()) {
            BossBar bossBar = gamePlayer.getPlayerData().getCurrentBossBar();
            if (bossBar != null) {
                bossBar.removeAll();
            }
        }
    }
}
