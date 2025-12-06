package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.utils.ItemUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class ItemPickupListener implements Listener {

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (ItemUtils.getInstance().isNoPickup(item)) {
            event.setCancelled(true);
        }
    }
}
