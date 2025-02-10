package cz.johnslovakia.gameapi.task.tasks;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.task.TaskInterface;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
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


    private void createBossbar(GamePlayer gamePlayer, Task task){
        BossBar bossBar = Bukkit.createBossBar("§c00:00", BarColor.WHITE , BarStyle.SOLID);

        bossBar.setTitle(StringUtils.getFontTextComponentJSON(StringUtils.getDurationString(task.getCounter()), "gameapi:bossbar_offset"));
        bossBar.setVisible(true);
        bossBar.addPlayer(gamePlayer.getOnlinePlayer());
        gamePlayer.getPlayerData().setCurrentBossBar(bossBar);
    }
    
    @Override
    public void onStart(Task task) {

        for (GamePlayer gamePlayer : task.getGame().getParticipants()){
            createBossbar(gamePlayer, task);

            new BukkitRunnable(){
                @Override
                public void run() {
                    gamePlayer.getPlayerData().saveAll();
                }
            }.runTaskAsynchronously(GameAPI.getInstance());

        }
    }

    @Override
    public void onCount(Task task) {
        int counter = task.getCounter();

        for (GamePlayer gamePlayer : task.getGame().getParticipants()) {
            BossBar bossBar = gamePlayer.getPlayerData().getCurrentBossBar();

            if(bossBar == null){
                createBossbar(gamePlayer, task);
                bossBar = gamePlayer.getPlayerData().getCurrentBossBar();
            }
            
            boolean respawning = gamePlayer.getPlayerData().getGame().getSettings().isEnabledRespawning();

            int kills = gamePlayer.getScoreByName("Kill").getScore();
            int deaths = gamePlayer.getScoreByName("Death").getScore();
            String killText = kills + " ẋ";
            String deathText = "";
            if (respawning){
                deathText = deaths + " Ẍ";
            }

            int width = (((killText.length()) * CHARACTER_WIDTH) / 2) + ((deathText.length() * CHARACTER_WIDTH) / 2) + 5;
            
            bossBar.setTitle(StringUtils.getFontTextComponentJSON(StringUtils.colorizer("\uDB00\uDC02".repeat(width)
                    + "\uDB00\uDC96" + (counter <= 15 ? (counter % 2 == 0 || counter > 8 ? "§c" : "§4") : "§f") + StringUtils.getDurationString(counter)
                    + "\uDB00\uDC96§f"
                    + gamePlayer.getScoreByName("Kill").getScore() + " ẍ"
                    + (respawning ? "\uDB00\uDC0F"
                    + gamePlayer.getScoreByName("Death").getScore() + " Ẍ" : "")), "gameapi:bossbar_offset"));

            if (counter == 26){
                gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer().getLocation(), "custom:ending", 0.25F, 1F);
            }
        }
    }

    boolean timeIsUp = false;
    @Override
    public void onEnd(Task task) {
        Game game = task.getGame();

        for (GamePlayer gamePlayer : game.getParticipants()) {
            BossBar bossBar = gamePlayer.getPlayerData().getCurrentBossBar();
            bossBar.setTitle(MessageManager.get(gamePlayer, "bossbar.time_is_up").getFontTextComponentJSON("gameapi:bossbar_offset"));

            MessageManager.get(gamePlayer, "title.time_is_up.title")
                    .send();
            gamePlayer.getOnlinePlayer().stopSound("custom:ending");
            gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer().getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 20.0F, 20.0F);
        }
        game.endGame(null);

        timeIsUp = true;
        Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), t -> {
            for (GamePlayer gamePlayer : game.getParticipants()) {
                BossBar bossBar = gamePlayer.getPlayerData().getCurrentBossBar();
                if (bossBar != null) {
                    bossBar.removeAll();
                }
            }
        }, 25L);
    }

    private static final int CHARACTER_WIDTH = 6;


    @Override
    public void onCancel(Task task) {
        for (GamePlayer gamePlayer : task.getGame().getParticipants()) {
            if (timeIsUp) {
                Bukkit.getScheduler().runTaskLater(GameAPI.getInstance(), t -> {
                    BossBar bossBar = gamePlayer.getPlayerData().getCurrentBossBar();
                    if (bossBar != null) {
                        bossBar.removeAll();
                    }
                }, 25L);
            } else {
                BossBar bossBar = gamePlayer.getPlayerData().getCurrentBossBar();
                if (bossBar != null) {
                    bossBar.removeAll();
                }
            }
        }
    }
}
