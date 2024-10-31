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

            int kills = gamePlayer.getScoreByName("Kill").getScore();
            int deaths = gamePlayer.getScoreByName("Death").getScore();
            String killText = kills + " ẁ";
            String deathText = deaths + " ẃ";

            int width = (((killText.length()) * CHARACTER_WIDTH) / 2) + ((deathText.length() * CHARACTER_WIDTH) / 2) + 5;

            bossBar.setTitle("\uDB00\uDC02".repeat(width)
                    + "\uDB00\uDC96" + StringUtils.getDurationString(task.getCounter())
                    + "\uDB00\uDC96"
                    + gamePlayer.getScoreByName("Kill").getScore() + " ẁ"
                    + "\uDB00\uDC0F"
                    + gamePlayer.getScoreByName("Death").getScore() + " ẃ");
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

    // Metoda pro vytvoření titulu s vycentrovaným časem
    public static String createTitleWithCenteredTime(String duration, int kills, int deaths) {
        // Texty pro kills a deaths
        String killText = kills + " ẁ"; // Kills text
        String deathText = deaths + " ẃ"; // Deaths text

        // Odhadneme šířku textu
        int durationWidth = duration.length() * CHARACTER_WIDTH;
        int killWidth = killText.length() * CHARACTER_WIDTH;
        int deathWidth = deathText.length() * CHARACTER_WIDTH;

        // Celková šířka boss baru (např. 150, podle vašich potřeb)
        int totalWidth = 150;
        int rightSideWidth = killWidth + deathWidth; // Šířka pravé strany

        // Vypočítáme, kolik "negative space" je potřeba pro vycentrování času
        int spaceWidth = (totalWidth - rightSideWidth - durationWidth) / 2;

        // Sestavíme řetězec s negativními mezerami
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spaceWidth / CHARACTER_WIDTH; i++) {
            sb.append("\uDB00\uDC00"); // Nebo jiný znak pro prázdný prostor
        }

        // Vytvoříme finální text s vycentrovaným časem
        return sb.toString() + duration + " " + "\uDB00\uDC96" + killText + "\uDB00\uDC0A" + deathText;
    }
}
