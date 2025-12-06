package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GameEndEvent;
import cz.johnslovakia.gameapi.events.GameJoinEvent;
import cz.johnslovakia.gameapi.users.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TestServerListener implements Listener {


    @EventHandler
    public void onGameJoin(GameJoinEvent e) {
        Player player = e.getGamePlayer().getOnlinePlayer();
        Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
            player.sendMessage("");
            player.sendMessage("§aWelcome to the Test Server!");
            player.sendMessage("§fWe appreciate you considering the purchase of this minigame.");
            player.sendMessage("");
            player.sendMessage("§7Have questions? Don’t hesitate to reach out.");
            player.sendMessage("§7To send feedback, use the command /rate <§c1§7-§a5§7> §8(1☆ is the worst and 5☆ is the best) §7or /rate <your feedback>");
            player.sendMessage("");
        }, 40L);
    }

    @EventHandler
    public void onGameEnd(GameEndEvent e) {
        for (GamePlayer gamePlayer : e.getGame().getParticipants()) {
            Player player = gamePlayer.getOnlinePlayer();

            Bukkit.getScheduler().runTaskLater(Minigame.getInstance().getPlugin(), task -> {
                sendFeedbackMessage(player);
            }, 75L);
        }
    }

    public void sendFeedbackMessage(Player player){
        Component message = Component.text("How would you rate this plugin? ").color(NamedTextColor.YELLOW);

        for (int i = 1; i <= 5; i++) {
            NamedTextColor color;
            switch (i) {
                case 1 -> color = NamedTextColor.RED;
                case 2 -> color = NamedTextColor.GOLD;
                case 3 -> color = NamedTextColor.YELLOW;
                case 4 -> color = NamedTextColor.GREEN;
                case 5 -> color = NamedTextColor.DARK_GREEN;
                default -> color = NamedTextColor.WHITE;
            }

            Component number = Component.text("[" + i + "☆]")
                    .color(color)
                    .clickEvent(ClickEvent.runCommand("/rate " + i))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to rate " + i + " stars")));

            message = message.append(Component.space()).append(number);
        }

        player.sendMessage(message);
        player.sendMessage("§fTo give written feedback, use the command §a/rate <your feedback>");
    }
}
