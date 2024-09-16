package cz.johnslovakia.gameapi.api;

import org.bukkit.entity.Player;

public interface UserInterface {

    public void sendTitle(Player player, String title, String subtitle);
    public void sendTitle(Player player, String title, String subtitle, int i1, int i2, int i3);
    public void sendAction(Player player, String text);
}
