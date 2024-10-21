package cz.johnslovakia.gameapi.task.tasks;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.MinigameSettings;
import cz.johnslovakia.gameapi.game.Game;
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

import java.util.List;

public class StartCountdown implements TaskInterface {

    private BossBar bossBar;

    @Override
    public void onStart(Task task) {
        this.bossBar = Bukkit.createBossBar("Game starting in:", BarColor.YELLOW ,BarStyle.SOLID);

        for (GamePlayer gamePlayer : task.getGame().getPlayers()){
            bossBar.setTitle(MessageManager.get(gamePlayer, "bossbar.game_starting_in")
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

        MinigameSettings settings = game.getSettings();
        List<GamePlayer> players = game.getPlayers();

        if (!settings.isChooseRandomMap()) {
            if (task.getCounter() == 10) {
                if (game.getMapManager().isEnabledVoting()) {
                    game.winMap();
                }

                for (GamePlayer gamePlayer : game.getParticipants()) {
                    Player player = gamePlayer.getOnlinePlayer();
                    if (player.getOpenInventory().getTitle().toLowerCase().contains("map")) {
                        player.closeInventory();
                    }
                }
            }
        }

        if (players.size() == settings.getReducedPlayers()) {
            if (task.getCounter() > settings.getReducedTime()) {
                task.setCounter(settings.getReducedTime());
                for (GamePlayer player : players) {
                    MessageManager.get(player, "chat.time_reduced")
                            .send();
                }
            }
        }

        for (GamePlayer gamePlayer : game.getPlayers()){
            bossBar.setTitle(MessageManager.get(gamePlayer, "bossbar.game_starting_in")
                    .replace("%time%", Utils.getDurationString(task.getCounter()))
                    .getTranslated());
            bossBar.setProgress(task.getCounter() / (double) task.getStartCounter());
        }



        if (players.isEmpty() || (players.size() <= settings.getMinPlayers() - 2 && settings.getMinPlayers() >= 5)
                || (settings.getMinPlayers() <= 4 && players.size() < settings.getMinPlayers())) {
            task.cancel();
        }
    }

    @Override
    public void onEnd(Task task) {
        Game game = task.getGame();

        new BukkitRunnable(){
            @Override
            public void run() {
                bossBar.removeAll();
            }
        }.runTaskLater(GameAPI.getInstance(), 1L);

        for (GamePlayer players : game.getPlayers()) {
            Player player = players.getOnlinePlayer();
                    /*if (time <= 5) {
                        Messages.send(player, "chat.game_starts");
                    }*/
            player.setLevel(task.getCounter());
            //player.playSound(player, "custom:gamestart", 20.0F, 20.0F); //v PreparationCountdown
        }
        if (game.getSettings().usePreperationTask()) {
            game.startPreparation();
        }else{
            for (GamePlayer gamePlayer : game.getPlayers()) {
                gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "custom:gamestart", 20.0F, 20.0F);
            }
            game.startGame();
        }
    }
}
