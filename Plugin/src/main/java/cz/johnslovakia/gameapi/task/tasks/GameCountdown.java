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
import eu.decentsoftware.holograms.api.utils.scheduler.S;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class GameCountdown implements TaskInterface {

    private Map<GamePlayer, BossBar> bossBars = new HashMap<>();

    @Override
    public void onStart(Task task) {

        for (GamePlayer gamePlayer : task.getGame().getParticipants()){
            new BukkitRunnable(){
                @Override
                public void run() {
                    gamePlayer.getPlayerData().saveAll();
                }
            }.runTaskAsynchronously(GameAPI.getInstance());

            BossBar bossBar = Bukkit.createBossBar("", BarColor.WHITE , BarStyle.SOLID);

            bossBar.setTitle(StringUtils.getDurationString(task.getCounter()));
            //bossBar.setProgress(task.getCounter() / (double) task.getStartCounter());
            bossBar.setVisible(true);
            bossBar.addPlayer(gamePlayer.getOnlinePlayer());

            bossBars.put(gamePlayer, bossBar);
        }
    }

    @Override
    public void onCount(Task task) {
        for (GamePlayer gamePlayer : task.getGame().getParticipants()) {
            BossBar bossBar = bossBars.get(gamePlayer);
            boolean respawning = gamePlayer.getPlayerData().getGame().getSettings().isEnabledRespawning();

            int kills = gamePlayer.getScoreByName("Kill").getScore();
            int deaths = gamePlayer.getScoreByName("Death").getScore();
            String killText = kills + " ẁ";
            String deathText = "";
            if (respawning){
                deathText = deaths + " ẃ";
            }

            int width = (((killText.length()) * CHARACTER_WIDTH) / 2) + ((deathText.length() * CHARACTER_WIDTH) / 2) + 5;

            bossBar.setTitle("\uDB00\uDC02".repeat(width)
                    + "\uDB00\uDC96" + StringUtils.getDurationString(task.getCounter())
                    + "\uDB00\uDC96"
                    + gamePlayer.getScoreByName("Kill").getScore() + " ẁ"
                    + (respawning ? "\uDB00\uDC0F"
                    + gamePlayer.getScoreByName("Death").getScore() + " ẃ" : ""));
            /*bossBar.setTitle(createTitleWithCenteredTime(
                    StringUtils.getDurationString(task.getCounter()),
                    gamePlayer.getScoreByName("Kill").getScore(),
                    gamePlayer.getScoreByName("Death").getScore()
            ));*/
            //bossBar.setProgress(task.getCounter() / (double) task.getStartCounter());
        }
    }

    @Override
    public void onEnd(Task task) {
        Game game = task.getGame();

        bossBars.values().forEach(BossBar::removeAll);

        for (GamePlayer gamePlayer : game.getPlayers()) {
            MessageManager.get(gamePlayer, "title.time_is_up.title")
                    .send();
            gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer().getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 20.0F, 20.0F);
        }
        game.endGame(null);
    }

    private static final int CHARACTER_WIDTH = 6; // Odhadovaná šířka jednoho znaku


    @Override
    public void onCancel(Task task) {
        bossBars.forEach((gamePlayer, bossBar) -> bossBar.removeAll());
    }
}
