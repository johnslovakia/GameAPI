package cz.johnslovakia.gameapi.task.tasks;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.task.TaskInterface;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitRunnable;

public class GameCountdown implements TaskInterface {

    private BossBar bossBar;

    @Override
    public void onStart(Task task) {
        this.bossBar = Bukkit.createBossBar("", BarColor.WHITE , BarStyle.SOLID);

        for (GamePlayer gamePlayer : task.getGame().getParticipants()){
            new BukkitRunnable(){
                @Override
                public void run() {
                    gamePlayer.getPlayerData().saveAll();
                }
            }.runTaskAsynchronously(GameAPI.getInstance());

            bossBar.setTitle(StringUtils.getDurationString(task.getCounter()));
            bossBar.setProgress(task.getCounter() / (double) task.getStartCounter());
            bossBar.setVisible(true);
            bossBar.addPlayer(gamePlayer.getOnlinePlayer());
        }
    }

    @Override
    public void onCount(Task task) {
        bossBar.setTitle(StringUtils.getDurationString(task.getCounter()));
        bossBar.setProgress(task.getCounter() / (double) task.getStartCounter());
    }

    @Override
    public void onEnd(Task task) {
        Game game = task.getGame();

        bossBar.removeAll();

        for (GamePlayer gamePlayer : game.getPlayers()) {
            MessageManager.get(gamePlayer, "title.time_is_up.title")
                    .send();
            gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer().getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 20.0F, 20.0F);
        }
        game.endGame(null);
    }
}
