package cz.johnslovakia.gameapi.modules.game.task.tasks;

import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.map.MapModule;
import cz.johnslovakia.gameapi.modules.game.task.Task;
import cz.johnslovakia.gameapi.modules.game.task.TaskInterface;
import cz.johnslovakia.gameapi.modules.kits.KitManager;
import cz.johnslovakia.gameapi.modules.serverManagement.gameData.GameDataManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.PlayerBossBar;
import cz.johnslovakia.gameapi.guis.KitInventory;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.utils.Utils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;


public class StartCountdown implements TaskInterface {
    @Override
    public void onCount(Task task) {
        GameInstance game = task.getGame();


        if (task.getCounter() == 10) {
            if (!game.getSettings().isChooseRandomMap()) {
                if (game.getModule(MapModule.class).isEnabledVoting()) {
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
                        .filter(gamePlayer -> gamePlayer.getGameSession().getSelectedKit() == null || gamePlayer.getGameSession().getSelectedKit().equals(kitManager.getDefaultKit())).toList()
                        .forEach(KitInventory::openKitInventory);
            }
        }


        for (GamePlayer gamePlayer : game.getParticipants()){
            PlayerBossBar playerBossBar = PlayerBossBar.getOrCreateBossBar(gamePlayer.getOnlinePlayer().getUniqueId(), Component.text(""));

            Component component = ModuleManager.getModule(MessageModule.class).get(gamePlayer, "bossbar.game_starting_in")
                        .replace("%time%", Utils.getDurationString(task.getCounter())).getTranslated()
                    .font(Key.key("jsplugins", "bossbar_offset"));
            playerBossBar.setName(component);
        }


        GameDataManager<GameInstance> gameDataManager = game.getServerDataManager();
        if (gameDataManager != null) {
            gameDataManager.updateGame();
        }
    }

    @Override
    public void onEnd(Task task) {
        GameInstance game = task.getGame();

        if (game.getSettings().isUsePreperationTask()) {
            game.getGameStartHandler().startPreparation();
        }else{
            for (GamePlayer gamePlayer : game.getPlayers()) {
                gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer(), "jsplugins:gamestart", 20.0F, 20.0F);
            }
            game.getGameStartHandler().startGame();
        }
    }
}
