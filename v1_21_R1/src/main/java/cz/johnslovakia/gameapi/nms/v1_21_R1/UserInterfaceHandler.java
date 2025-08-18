package cz.johnslovakia.gameapi.nms.v1_21_R1;

import cz.johnslovakia.gameapi.api.UserInterface;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserInterfaceHandler implements UserInterface {
    @Override
    public void sendTitle(Player player, String title, String subtitle) {
        player.sendTitle(colorizer(title), colorizer(subtitle),5,50,5);
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int i1, int i2, int i3) {
        player.sendTitle(colorizer(title), colorizer(subtitle), i1, i2, i3);
    }

    /*@Override
    public void sendAction(Player player, String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(colorizer(text)));
    }*/

    public static String colorizer(String message) {
        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String hexCode = message.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace('#', 'x');

            char[] ch = replaceSharp.toCharArray();
            StringBuilder builder = new StringBuilder();
            for (char c : ch) {
                builder.append("&").append(c);
            }

            message = message.replace(hexCode, builder.toString());
            matcher = pattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
