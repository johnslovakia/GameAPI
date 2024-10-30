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
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class PreparationCountdown implements TaskInterface {

    private BossBar bossBar;

    @Override
    public void onStart(Task task) {
        this.bossBar = Bukkit.createBossBar("Battle begins in:", BarColor.YELLOW , BarStyle.SOLID);

        for (GamePlayer gamePlayer : task.getGame().getPlayers()){
            bossBar.setTitle(MessageManager.get(gamePlayer, "bossbar.battle_begings_in")
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
        List<GamePlayer> players = game.getPlayers();

        for (GamePlayer gamePlayer : game.getPlayers()){
            bossBar.setTitle(MessageManager.get(gamePlayer, "bossbar.battle_begings_in")
                    .replace("%time%", Utils.getDurationString(task.getCounter()))
                    .getTranslated());
            bossBar.setProgress(task.getCounter() / (double) task.getStartCounter());
        }

        
        if (task.getCounter() == 3) {
            for (GamePlayer gamePlayer : players) {
                Player player = gamePlayer.getOnlinePlayer();

                GameAPI.getInstance().getUserInterface().sendTitle(player, "§a➌", MessageManager.get(player, "title.battle_begings_in.subtitle").getTranslated());
                player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 20.0F, 20.0F);
            }
        } else if (task.getCounter() == 2) {
            for (GamePlayer gamePlayer : players) {
                Player player = gamePlayer.getOnlinePlayer();
                GameAPI.getInstance().getUserInterface().sendTitle(player, "§a➋", MessageManager.get(player, "title.battle_begings_in.subtitle").getTranslated());
                player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 20.0F, 20.0F);
            }
        } else if (task.getCounter() == 1) {
            for (GamePlayer gamePlayer : players) {
                Player player = gamePlayer.getOnlinePlayer();
                GameAPI.getInstance().getUserInterface().sendTitle(player, "§a➊", MessageManager.get(player, "title.battle_begings_in.subtitle").getTranslated());
                player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 20.0F, 20.0F);
            }
        }
    }

    @Override
    public void onEnd(Task task) {
        Game game = task.getGame();

        bossBar.removeAll();

        for (GamePlayer gamePlayer : game.getPlayers()) {
            Player player = gamePlayer.getOnlinePlayer();
            player.playSound(player, "custom:gamestart", 20.0F, 20.0F);
            GameAPI.getInstance().getUserInterface().sendTitle(player, MessageManager.get(player, "title.battle_started.subtitle").getTranslated(), "");
            gamePlayer.setEnabledMovement(true);
        }
        game.startGame();
    }
}
