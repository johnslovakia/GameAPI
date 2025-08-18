package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener {


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        Block block = e.getClickedBlock();

        if (!gamePlayer.isSpectator() || block == null)
            return;
        if (block.getType() == Material.CHEST){
            Chest chest = (Chest) block.getState();
            player.openInventory(copyInventory(chest.getBlockInventory()));
        }else if (block.getType() == Material.BARREL){
            Barrel barrel = (Barrel) block.getState();
            player.openInventory(copyInventory(barrel.getInventory()));
        }
    }

    public static Inventory copyInventory(Inventory original) {
        Inventory copy = Bukkit.createInventory(null, original.getSize(), Component.text("Inventory Content"));
        for (int i = 0; i < original.getSize(); i++) {
            ItemStack item = original.getItem(i);
            if (item != null) {
                copy.setItem(i, item.clone());
            }
        }
        return copy;
    }
}
