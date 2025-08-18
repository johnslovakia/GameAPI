package cz.johnslovakia.gameapi.task.tasks;

import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.kit.KitManager;
import cz.johnslovakia.gameapi.guis.KitInventory;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.task.TaskInterface;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.PlayerBossBar;
import cz.johnslovakia.gameapi.utils.Utils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;


public class StartCountdown implements TaskInterface {
    @Override
    public void onCount(Task task) {
        Game game = task.getGame();


        if (task.getCounter() == 10) {
            if (!game.getSettings().isChooseRandomMap()) {
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

            KitManager kitManager = KitManager.getKitManager(game);
            if (kitManager != null){
                game.getParticipants().stream()
                        .filter(gamePlayer -> gamePlayer.getKit() == null || gamePlayer.getKit().equals(kitManager.getDefaultKit())).toList()
                        .forEach(KitInventory::openKitInventory);
            }
        }


        for (GamePlayer gamePlayer : game.getParticipants()){
            PlayerBossBar playerBossBar = PlayerBossBar.getOrCreateBossBar(gamePlayer.getOnlinePlayer().getUniqueId(), Component.text(""));

            Component component = MessageManager.get(gamePlayer, "bossbar.game_starting_in")
                        .replace("%time%", Utils.getDurationString(task.getCounter())).getTranslated()
                    .font(Key.key("jsplugins", "bossbar_offset"));
            playerBossBar.setName(component);
        }


        if (game.getServerDataManager() != null) {
            game.getServerDataManager().getJSONProperty("StartingTime").update(game, task.getCounter());
        }
    }

    @Override
    public void onEnd(Task task) {
        Game game = task.getGame();

        if (game.getSettings().usePreperationTask()) {
            game.getStartingProcessHandler().startPreparation();
        }else{
            for (GamePlayer gamePlayer : game.getPlayers()) {
                gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "jsplugins:gamestart", 20.0F, 20.0F);
            }
            game.getStartingProcessHandler().startGame();
        }
    }
}
