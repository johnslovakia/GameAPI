package cz.johnslovakia.gameapi.task.tasks;

import cz.johnslovakia.gameapi.MinigameSettings;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.task.TaskInterface;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.List;

public class StartCountdownCommand implements TaskInterface {

    private void createBossBar(GamePlayer gamePlayer, Task task){
        if (gamePlayer.getPlayerData().getCurrentBossBar() != null){
            gamePlayer.getPlayerData().getCurrentBossBar().removeAll();
        }

        BossBar bossBar = Bukkit.createBossBar("Game starting in:", BarColor.WHITE, BarStyle.SOLID);

        if (gamePlayer.getMetadata().get("bossbar.waiting_for_players") != null) {
            ((BossBar) gamePlayer.getMetadata().get("bossbar.waiting_for_players")).removeAll();
            gamePlayer.getMetadata().remove("bossbar.waiting_for_players");
        }

        bossBar.setTitle(MessageManager.get(gamePlayer, "bossbar.game_starting_in")
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

        MinigameSettings settings = game.getSettings();
        List<GamePlayer> players = game.getParticipants();

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

        for (GamePlayer gamePlayer : game.getParticipants()){
            BossBar bossBar = gamePlayer.getPlayerData().getCurrentBossBar();
            if(bossBar == null){
                createBossBar(gamePlayer, task);
                bossBar = gamePlayer.getPlayerData().getCurrentBossBar();
            }
            bossBar.setTitle(MessageManager.get(gamePlayer, "bossbar.game_starting_in")
                    .replace("%time%", Utils.getDurationString(task.getCounter()))
                    .getFontTextComponentJSON("gameapi:bossbar_offset"));

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

        if (game.getServerDataManager() != null) {
            game.getServerDataManager().getJSONProperty("StartingTIme").update(game, task.getCounter());
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
        if (game.getSettings().usePreperationTask()) {
            game.getStartingProcessHandler().startPreparation();
        }else{
            for (GamePlayer gamePlayer : game.getParticipants()) {
                gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "custom:gamestart", 20.0F, 20.0F);
            }
            game.getStartingProcessHandler().startGame();
        }
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
