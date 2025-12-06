package cz.johnslovakia.gameapi.modules.kits;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class KitListener implements Listener {

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        GameInstance game = gamePlayer.getGame();
        Kit kit = gamePlayer.getGameSession().getSelectedKit();

        if (kit == null || game.getState() != GameState.INGAME || !gamePlayer.isRespawning()){
            return;
        }

        if (kit.isGiveAfterDead()) {
            new BukkitRunnable(){
                @Override
                public void run() {
                    if (!game.getState().equals(GameState.INGAME)) return;
                    kit.giveContent(gamePlayer);
                }
            }.runTaskLater(Minigame.getInstance().getPlugin(), 1L);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
            GameInstance game = gamePlayer.getGame();
            if (game == null)
                return;
            if (game.isPreparation()){
                gamePlayer.getMetadata().put("edited_kit_inventory", true);
            }
        }
    }
}
