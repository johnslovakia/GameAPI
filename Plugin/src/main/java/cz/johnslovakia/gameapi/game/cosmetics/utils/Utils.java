package cz.johnslovakia.gameapi.game.cosmetics.utils;

import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.messages.MessageManager;
import org.bukkit.entity.Player;

public class Utils {

    private static void sendKillMessagePreview(Player player, Cosmetic cosmetic){
        String messageName = cosmetic.getName().toLowerCase().replaceAll(" ", "_");

        player.sendMessage("");
        player.sendMessage("§2§lKill messages §7- §a" + cosmetic.getName());
        player.sendMessage("");
        player.sendMessage("§a§lChat messages:");
        player.sendMessage("§aMelee Kill: " + MessageManager.get(player, "chat.kill_message." + messageName + ".melee").replace("%player_color%", "§c").replace("%dead%", "§cPlayer").replace("%killer_color%", "§c").replace("%killer%", "§cKiller").getTranslated());
        player.sendMessage("§aFall Damage Kill: " + MessageManager.get(player, "chat.kill_message." + messageName + ".fall").replace("%player_color%", "§c").replace("%dead%", "§cPlayer").replace("%killer_color%", "§c").replace("%killer%", "§cKiller").getTranslated());
        player.sendMessage("§aVoid Kill: " + MessageManager.get(player, "chat.kill_message." + messageName + ".void").replace("%player_color%", "§c").replace("%dead%", "§cPlayer").replace("%killer_color%", "§c").replace("%killer%", "§cKiller").getTranslated());
        player.sendMessage("§aRanged Kill: " + MessageManager.get(player, "chat.kill_message." + messageName + ".ranged").replace("%player_color%", "§c").replace("%dead%", "§cPlayer").replace("%killer_color%", "§c").replace("%killer%", "§cKiller").getTranslated());
        player.sendMessage("");
    }
}
