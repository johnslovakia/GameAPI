package cz.johnslovakia.gameapi.task.tasks;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.task.TaskInterface;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.PlayerBossBar;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.Utils;
import eu.decentsoftware.holograms.api.utils.scheduler.S;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class GameCountdown implements TaskInterface {

    @Override
    public void onStart(Task task) {

        for (GamePlayer gamePlayer : task.getGame().getParticipants()){
            new BukkitRunnable(){
                @Override
                public void run() {
                    gamePlayer.getPlayerData().saveAll();
                }
            }.runTaskAsynchronously(Minigame.getInstance().getPlugin());

        }
    }

    @Override
    public void onCount(Task task) {
        int counter = task.getCounter();

        for (GamePlayer gamePlayer : task.getGame().getParticipants()) {
            boolean respawning = gamePlayer.getGame().getSettings().isEnabledRespawning();

            PlayerBossBar playerBossBar = PlayerBossBar.getOrCreateBossBar(gamePlayer.getOnlinePlayer().getUniqueId(), Component.text(""));

            playerBossBar.setName(null,
                    Component.text((counter <= 15 ? (counter % 2 == 0 || counter > 8 ? "§c" : "§4") : "§f") + StringUtils.getDurationString(counter)).font(Key.key("jsplugins", "bossbar_offset")),
                    Component.text(gamePlayer.getScoreByName("Kill").getScore() + " ẍ"
                            + (respawning ? /*"\uDB00\uDC0F"*/ "   "
                            + gamePlayer.getScoreByName("Death").getScore() + " Ẍ" : "")).font(Key.key("jsplugins", "bossbar_offset")));



            if (counter == 26){
                gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer().getLocation(), "jsplugins:ending", 0.25F, 1F);
            }
        }
    }

    boolean timeIsUp = false;
    @Override
    public void onEnd(Task task) {
        Game game = task.getGame();

        for (GamePlayer gamePlayer : game.getParticipants()) {
            PlayerBossBar playerBossBar = PlayerBossBar.getOrCreateBossBar(gamePlayer.getOnlinePlayer().getUniqueId(), Component.text(""));

            Component component = MessageManager.get(gamePlayer, "bossbar.time_is_up").getTranslated()
                    .font(Key.key("jsplugins", "bossbar_offset"));
            playerBossBar.setName(component);


            MessageManager.get(gamePlayer, "title.time_is_up.title")
                    .send();
            gamePlayer.getOnlinePlayer().stopSound("jsplugins:ending");
            gamePlayer.getOnlinePlayer().playSound(gamePlayer.getOnlinePlayer().getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 20.0F, 20.0F);
        }
        game.endGame(null);

        timeIsUp = true;
    }

    private static final int CHARACTER_WIDTH = 6;
}
