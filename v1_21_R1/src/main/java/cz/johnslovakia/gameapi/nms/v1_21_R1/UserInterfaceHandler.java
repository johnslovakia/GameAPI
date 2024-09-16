package cz.johnslovakia.gameapi.nms.v1_21_R1;

import cz.johnslovakia.gameapi.api.UserInterface;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class UserInterfaceHandler implements UserInterface {
    @Override
    public void sendTitle(Player player, String title, String subtitle) {
        player.sendTitle(title, subtitle,5,50,5);
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int i1, int i2, int i3) {
        player.sendTitle(title, subtitle,i1,i2,i3);
    }

    @Override
    public void sendAction(Player player, String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
    }
}
