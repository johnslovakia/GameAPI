package cz.johnslovakia.gameapi.game.cosmetics.listeners;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GamePreparationEvent;
import cz.johnslovakia.gameapi.events.GameStateChangeEvent;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class HatsCategoryListener implements Listener {
    @EventHandler
    public void onEntityDropItem(PlayerDropItemEvent e) {
        if (e.getItemDrop().getItemStack().getType().equals(Material.CARVED_PUMPKIN) && e.getItemDrop().getItemStack().hasItemMeta() && e.getItemDrop().getItemStack().getItemMeta().hasCustomModelData()){
            if (Minigame.getInstance().getSettings().isCanDropCosmeticHat() && PlayerManager.getGamePlayer(e.getPlayer()).getGame().getState().equals(GameState.INGAME)){
                e.getItemDrop().remove();
            }else {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        Cosmetic selected = Minigame.getInstance().getCosmeticsManager().getSelectedCosmetic(gamePlayer, Minigame.getInstance().getCosmeticsManager().getCategoryByName("Hats"));
        if (selected != null){
            selected.getSelectConsumer().accept(gamePlayer);
        }
    }

    @EventHandler
    public void onGamePreparation(GamePreparationEvent e) {
        for (GamePlayer gamePlayer : e.getGame().getParticipants()) {
            Cosmetic selected = Minigame.getInstance().getCosmeticsManager().getSelectedCosmetic(gamePlayer, Minigame.getInstance().getCosmeticsManager().getCategoryByName("Hats"));
            if (selected != null) {
                selected.getSelectConsumer().accept(gamePlayer);
            }
        }
    }

    @EventHandler
    public void onGameStateChange(GameStateChangeEvent e) {
        for (GamePlayer gamePlayer : e.getGame().getParticipants()) {
            Cosmetic selected = Minigame.getInstance().getCosmeticsManager().getSelectedCosmetic(gamePlayer, Minigame.getInstance().getCosmeticsManager().getCategoryByName("Hats"));
            if (selected != null) {
                selected.getSelectConsumer().accept(gamePlayer);
            }
        }
    }

    @EventHandler
    public void onItemClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            ItemStack currentItem = e.getCurrentItem();
            if (currentItem == null)
                return;
            if (PlayerManager.getGamePlayer(player).getGame() == null)
                return;
            if (!(currentItem.getType().equals(Material.CARVED_PUMPKIN) && currentItem.hasItemMeta() && currentItem.getItemMeta().hasCustomModelData()))
                return;

            if (Minigame.getInstance().getSettings().isCanDropCosmeticHat()
                    && PlayerManager.getGamePlayer(player).getGame().getState().equals(GameState.INGAME)
                    && (e.getClick().equals(ClickType.DROP) || e.getClick().equals(ClickType.CONTROL_DROP))) {
                currentItem.setType(Material.AIR);
                return;
            }

            e.setCancelled(true);
        }
    }
}
