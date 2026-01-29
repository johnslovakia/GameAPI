package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.UpdateChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateCheckerListener implements Listener {



    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        Minigame minigame = Minigame.getInstance();
        
        if (minigame == null) return;
        if (!hasUpdatePermission(player, minigame)) return;

        UpdateChecker updateChecker = minigame.getUpdateChecker();

        Bukkit.getScheduler().runTaskLater(minigame.getPlugin(), task -> {
            if (updateChecker.isOutdated()) {
                sendOutdatedMessage(player, minigame, updateChecker);
            } else if (updateChecker.isUnreleased()) {
                sendUnreleasedMessage(player, minigame, updateChecker);
            }/*else if (updateChecker.getAnnouncement() != null) {
                sendAnnouncementOnly(player, minigame, updateChecker);
            }*/
        }, 15L);
    }

    private boolean hasUpdatePermission(Player player, Minigame minigame) {
        return player.hasPermission(minigame.getName() + ".admin")
                || player.hasPermission(minigame.getName().toLowerCase() + ".admin")
                || player.isOp()
                || player.hasPermission("*");
    }

    private void sendOutdatedMessage(Player player, Minigame minigame, UpdateChecker updateChecker) {
        Component hover = Component.text("§fLatest Version: §a" + updateChecker.getLatestVersion())
                .append(Component.text("\n§fYour Current Version: §c" + updateChecker.getCurrentVersion()));

        if (updateChecker.hasUpdateMessages()) {
            hover = hover.append(Component.text("\n\n" + updateChecker.getFormattedUpdateMessagesForHover()));
        }

        hover = hover.append(Component.text("\n\n§7Click to see detailed update history!"));

        Component message = Component.text(
                        "§c[!] §fYour version of the §a" + minigame.getName()
                                + " §fplugin is §coutdated! §fUpdating to the latest version "
                                + updateChecker.getLatestVersion()
                                + " is §arecommended! §6(hover for more)")
                .hoverEvent(HoverEvent.showText(hover));

        player.sendMessage(message);

        if (updateChecker.getAnnouncement() != null) {
            player.sendMessage(" §7↳ §r" + StringUtils.colorizer(updateChecker.getAnnouncement()));
        }
    }

    private void sendUnreleasedMessage(Player player, Minigame minigame, UpdateChecker updateChecker) {
        Component hover = Component.text("§fLatest Public Released Version: §a" + updateChecker.getLatestVersion())
                .append(Component.text("\n§fYour Current Version: §e" + updateChecker.getCurrentVersion()))
                .append(Component.text("\n"))
                .append(Component.text("\n§6⚠ This version is newer than the latest official release."))
                .append(Component.text("\n§7It may be unstable, experimental, or not fully tested."));

        Component message = Component.text(
                        "§e[!] §fYou are using an §eunreleased build §fof the §a"
                                + minigame.getName()
                                + " §fplugin! This version may not be stable. §7(hover for more)")
                .hoverEvent(HoverEvent.showText(hover));

        player.sendMessage(message);

        if (updateChecker.getAnnouncement() != null) {
            player.sendMessage(" §7↳ §r" + StringUtils.colorizer(updateChecker.getAnnouncement()));
        }
    }

    private void sendAnnouncementOnly(Player player, Minigame minigame, UpdateChecker updateChecker) {
        player.sendMessage("§a[" + minigame.getName() + "] §7" + StringUtils.colorizer(updateChecker.getAnnouncement()));
    }
}