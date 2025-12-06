package cz.johnslovakia.gameapi.modules.game.task.tasks;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.session.PlayerGameSession;
import cz.johnslovakia.gameapi.modules.game.task.Task;
import cz.johnslovakia.gameapi.modules.game.task.TaskInterface;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.PlayerBossBar;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.StringUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

public class GameCountdown implements TaskInterface {

    @Override
    public void onStart(Task task) {

        for (GamePlayer gamePlayer : task.getGame().getParticipants()){
            PlayerData data = gamePlayer.getPlayerData();
            new BukkitRunnable(){
                @Override
                public void run() {
                    data.saveAll();

                    data.getKitInventories().clear();
                }
            }.runTaskAsynchronously(Minigame.getInstance().getPlugin());

        }
    }

    @Override
    public void onCount(Task task) {
        int counter = task.getCounter();

        for (GamePlayer gamePlayer : task.getGame().getParticipants()) {
            PlayerGameSession gameSession = gamePlayer.getGameSession();
            boolean respawning = gamePlayer.getGame().getSettings().isEnabledRespawning();

            PlayerBossBar playerBossBar = PlayerBossBar.getOrCreateBossBar(gamePlayer.getOnlinePlayer().getUniqueId(), Component.text(""));

            playerBossBar.setName(null,
                    Component.text((counter <= 15 ? (counter % 2 == 0 || counter > 8 ? "§c" : "§4") : "§f") + StringUtils.getDurationString(counter)).font(Key.key("jsplugins", "bossbar_offset")),
                    Component.text(gameSession.getScore("Kill") + " ẍ"
                            + (respawning ? /*"\uDB00\uDC0F"*/ "   "
                            + gameSession.getScore("Death") + " Ẍ" : "")).font(Key.key("jsplugins", "bossbar_offset")));



            if (counter == 26){
                gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer().getLocation(), "jsplugins:ending", 0.25F, 1F);
            }
        }
    }

    boolean timeIsUp = false;
    @Override
    public void onEnd(Task task) {
        GameInstance game = task.getGame();

        for (GamePlayer gamePlayer : game.getParticipants()) {
            PlayerBossBar playerBossBar = PlayerBossBar.getOrCreateBossBar(gamePlayer.getOnlinePlayer().getUniqueId(), Component.text(""));

            Component component = ModuleManager.getModule(MessageModule.class).get(gamePlayer, "bossbar.time_is_up").getTranslated()
                    .font(Key.key("jsplugins", "bossbar_offset"));
            playerBossBar.setName(component);


            ModuleManager.getModule(MessageModule.class).get(gamePlayer, "title.time_is_up.title")
                    .send();
            gamePlayer.getOnlinePlayer().stopSound("jsplugins:ending");
            gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer().getLocation(), Sound.BLOCK_ANVIL_BREAK, 20.0F, 20.0F);
        }
        game.endGame(null);

        timeIsUp = true;
    }

    private static final int CHARACTER_WIDTH = 6;
}
