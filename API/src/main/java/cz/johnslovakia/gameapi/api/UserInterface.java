package cz.johnslovakia.gameapi.api;

import org.bukkit.entity.Player;

public interface UserInterface {

    void sendTitle(Player player, String title, String subtitle);
    void sendTitle(Player player, String title, String subtitle, int i1, int i2, int i3);
    //void sendAction(Player player, String text);
}
