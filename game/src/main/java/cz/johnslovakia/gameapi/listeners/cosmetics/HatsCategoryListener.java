package cz.johnslovakia.gameapi.listeners.cosmetics;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.events.GamePreparationEvent;
import cz.johnslovakia.gameapi.events.GameStartEvent;
import cz.johnslovakia.gameapi.events.GameStateChangeEvent;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;

import cz.johnslovakia.gameapi.utils.ItemBuilder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class HatsCategoryListener implements Listener {

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(e.getPlayer());
        CosmeticsModule cosmeticsModule = ModuleManager.getModule(CosmeticsModule.class);
        Cosmetic selected = cosmeticsModule.getPlayerSelectedCosmetic(gamePlayer, cosmeticsModule.getCategoryByName("Hats"));
        if (selected != null){
            selected.getSelectConsumer().accept(gamePlayer);
        }
    }

    @EventHandler
    public void onGamePreparation(GamePreparationEvent e) {
        CosmeticsModule cosmeticsModule = ModuleManager.getModule(CosmeticsModule.class);
        for (GamePlayer gamePlayer : e.getGame().getParticipants()) {
            Cosmetic selected = cosmeticsModule.getPlayerSelectedCosmetic(gamePlayer, cosmeticsModule.getCategoryByName("Hats"));
            if (selected != null) {
                selected.getSelectConsumer().accept(gamePlayer);
            }
            applyHatRemovalHint(gamePlayer);
        }
    }

    @EventHandler
    public void onGameStateChange(GameStateChangeEvent e) {
        CosmeticsModule cosmeticsModule = ModuleManager.getModule(CosmeticsModule.class);
        for (GamePlayer gamePlayer : e.getGame().getParticipants()) {
            Cosmetic selected = cosmeticsModule.getPlayerSelectedCosmetic(gamePlayer, cosmeticsModule.getCategoryByName("Hats"));
            if (selected != null) {
                selected.getSelectConsumer().accept(gamePlayer);
            }
        }
    }

    @EventHandler
    public void onGameStart(GameStartEvent e) {
        CosmeticsModule cosmeticsModule = ModuleManager.getModule(CosmeticsModule.class);
        for (GamePlayer gamePlayer : e.getGame().getParticipants()) {
            Cosmetic selected = cosmeticsModule.getPlayerSelectedCosmetic(gamePlayer, cosmeticsModule.getCategoryByName("Hats"));
            if (selected != null) {
                selected.getSelectConsumer().accept(gamePlayer);
            }
            applyHatRemovalHint(gamePlayer);
        }
    }

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
                player.getInventory().setHelmet(null);
            }else {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHelmetReplace(InventoryClickEvent e) {
        if (!Minigame.getInstance().getSettings().isCanDropCosmeticHat()) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;

        if (e.getSlotType() != InventoryType.SlotType.ARMOR) return;
        if (e.getSlot() != 39) return;

        ItemStack oldHelmet = e.getCurrentItem();
        ItemStack cursor = e.getCursor();

        if (oldHelmet == null || oldHelmet.getType() != Material.CARVED_PUMPKIN) return;
        if (cursor.getType() == Material.AIR) return;

        player.getInventory().setHelmet(null);
    }

    @EventHandler
    public void onHelmetEquip(PlayerInteractEvent e) {
        if (!Minigame.getInstance().getSettings().isCanDropCosmeticHat()) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = e.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (!hand.getType().name().endsWith("_HELMET")) return;

        ItemStack oldHelmet = player.getInventory().getHelmet();
        if (oldHelmet != null && oldHelmet.getType() == Material.CARVED_PUMPKIN) {
            player.getInventory().setHelmet(null);
        }
    }


    public void applyHatRemovalHint(GamePlayer gamePlayer){
        if (Minigame.getInstance().getSettings().isCanDropCosmeticHat()) {
            Player player = gamePlayer.getOnlinePlayer();
            ItemStack helmet = player.getInventory().getHelmet();

            if (helmet != null && helmet.getType() == Material.CARVED_PUMPKIN) {
                ItemMeta meta = helmet.getItemMeta();

                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add(LegacyComponentSerializer.legacySection().serialize(
                        ModuleManager.getModule(MessageModule.class)
                                .get(gamePlayer, "item.hat.q_to_remove")
                                .getTranslated()));

                meta.setLore(lore);
                helmet.setItemMeta(meta);
            }
        }
    }
}
