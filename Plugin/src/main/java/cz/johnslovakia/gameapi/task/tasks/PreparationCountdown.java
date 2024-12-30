package cz.johnslovakia.gameapi.task.tasks;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.task.TaskInterface;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Utils;
import cz.johnslovakia.gameapi.utils.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreparationCountdown implements TaskInterface {

    private void createBossBar(GamePlayer gamePlayer, Task task){
        BossBar bossBar = Bukkit.createBossBar("Battle begins in:", BarColor.WHITE, BarStyle.SOLID);

        bossBar.setTitle(MessageManager.get(gamePlayer, "bossbar.battle_begings_in")
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

        for (GamePlayer gamePlayer : game.getParticipants()){
            BossBar bossBar = gamePlayer.getPlayerData().getCurrentBossBar();
            if(bossBar == null){
                createBossBar(gamePlayer, task);
                bossBar = gamePlayer.getPlayerData().getCurrentBossBar();
            }
            bossBar.setTitle(MessageManager.get(gamePlayer, "bossbar.battle_begings_in")
                    .replace("%time%", Utils.getDurationString(task.getCounter()))
                    .getFontTextComponentJSON("gameapi:bossbar_offset"));
            

            Player player = gamePlayer.getOnlinePlayer();

            if (task.getCounter() <= 5 && task.getCounter() > 0) {
                float volume = 0.3F + (0.8F - 0.3F) * ((float) (5 - task.getCounter()) / 5.0F);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, volume, volume);
            }

            if (task.getCounter() <= 3 && task.getCounter() > 0) {
                ChatColor[] colors = {ChatColor.GREEN, ChatColor.AQUA, ChatColor.YELLOW};
                GameAPI.getInstance().getUserInterface().sendTitle(player, colors[task.getCounter() - 1] + "► " + task.getCounter() + " ◄", MessageManager.get(player, "title.battle_begings_in.subtitle").getTranslated());
            }
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
            Player player = gamePlayer.getOnlinePlayer();
            MessageManager.get(gamePlayer, "title.battle_started")
                    .send();
            player.playSound(player, "custom:gamestart", 20.0F, 20.0F);
        }
        game.startGame();
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
