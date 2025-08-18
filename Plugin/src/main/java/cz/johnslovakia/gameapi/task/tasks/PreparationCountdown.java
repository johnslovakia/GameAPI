package cz.johnslovakia.gameapi.task.tasks;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.task.Task;
import cz.johnslovakia.gameapi.task.TaskInterface;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.PlayerBossBar;
import cz.johnslovakia.gameapi.utils.Utils;
import cz.johnslovakia.gameapi.utils.Sounds;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
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

    @Override
    public void onCount(Task task) {
        Game game = task.getGame();

        for (GamePlayer gamePlayer : game.getParticipants()){
            PlayerBossBar playerBossBar = PlayerBossBar.getOrCreateBossBar(gamePlayer.getOnlinePlayer().getUniqueId(), Component.text(""));

            Component component = MessageManager.get(gamePlayer, "bossbar.battle_begings_in")
                        .replace("%time%", Utils.getDurationString(task.getCounter())).getTranslated()
                    .font(Key.key("jsplugins", "bossbar_offset"));
            playerBossBar.setName(component);
            

            Player player = gamePlayer.getOnlinePlayer();

            if (task.getCounter() <= 5 && task.getCounter() > 0) {
                float volume = 0.3F + (0.8F - 0.3F) * ((float) (5 - task.getCounter()) / 5.0F);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, volume, volume);
            }

            if (task.getCounter() <= 3 && task.getCounter() > 0) {
                ChatColor[] colors = {ChatColor.GREEN, ChatColor.AQUA, ChatColor.YELLOW};
                //GameAPI.getInstance().getUserInterface().sendTitle(player, colors[task.getCounter() - 1] + "► " + task.getCounter() + " ◄", MessageManager.get(player, "title.battle_begings_in.subtitle").getTranslated());
                player.showTitle(Title.title(Component.text(colors[task.getCounter() - 1] + "► " + task.getCounter() + " ◄"), MessageManager.get(player, "title.battle_begings_in.subtitle").getTranslated()));
            }
        }
    }

    @Override
    public void onEnd(Task task) {
        Game game = task.getGame();

        for (GamePlayer gamePlayer : game.getParticipants()) {
            Player player = gamePlayer.getOnlinePlayer();
            MessageManager.get(gamePlayer, "title.battle_started")
                    .send();
            player.playSound(player, "jsplugins:gamestart", 20.0F, 20.0F);

            PlayerData data = gamePlayer.getPlayerData();
            if (gamePlayer.getMetadata().containsKey("edited_kit_inventory") && (boolean) gamePlayer.getMetadata().get("edited_kit_inventory")){
                data.setKitInventory(gamePlayer.getKit(), Utils.copyPlayerInventory(player));
                Bukkit.getScheduler().runTaskAsynchronously(Minigame.getInstance().getPlugin(), task2 -> data.saveKitInventories());
                gamePlayer.getMetadata().remove("edited_kit_inventory");
            }

            gamePlayer.getMetadata().remove("last_opened_cosmetic_category");
        }
        game.getStartingProcessHandler().startGame();
    }
}
