package cz.johnslovakia.gameapi.GUIs;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import me.zort.containr.GUI;
import org.bukkit.entity.Player;

public class KitInventory extends  GUI {

    public KitInventory(GamePlayer gamePlayer) {
        super(MessageManager.get(gamePlayer, "inventory.kit.title").getTranslated(), (GameAPI.getInstance().getKitManager().getKits().size() + 7) / 8 + 3);
    }

    @Override
    public void build(Player player) {
        player.getOpenInventory().setTitle(MessageManager.get(player, "inventory.kit.title").getTranslated());

    }
}
