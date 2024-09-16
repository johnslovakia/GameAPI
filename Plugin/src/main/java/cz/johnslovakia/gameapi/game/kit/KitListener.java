package cz.johnslovakia.gameapi.game.kit;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.utils.GameUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class KitListener implements Listener {


    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        PlayerData data = gamePlayer.getPlayerData();
        Game game = gamePlayer.getPlayerData().getGame();

        if (game.getState() != GameState.INGAME){
            return;
        }


        Kit kit = gamePlayer.getPlayerData().getKit();
        if (kit == null){
            return;
        }


        if (GameAPI.getInstance().getKitManager().isGiveAfterDeath()) {
            player.getInventory().setContents(data.getKitInventories().get(kit) != null ? data.getKitInventories().get(kit).getContents() : kit.getContent().getInventory().getContents());
            player.getInventory().setArmorContents(kit.getContent().getArmor().toArray(new ItemStack[0]));
            GameUtil.colorizeArmor(gamePlayer);
        }
    }
}
