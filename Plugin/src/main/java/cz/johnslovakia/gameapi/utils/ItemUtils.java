package cz.johnslovakia.gameapi.utils;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class ItemUtils {

    @Getter
    public static ItemUtils instance;
    private final NamespacedKey noPickupKey;

    public ItemUtils(Plugin plugin) {
        instance = this;
        this.noPickupKey = new NamespacedKey(plugin, "no_pickup");
    }

    public ItemStack markAsNoPickup(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return itemStack;

        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(noPickupKey, PersistentDataType.BYTE, (byte) 1);
        itemStack.setItemMeta(meta);

        return itemStack;
    }

    public boolean isNoPickup(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return false;

        ItemMeta meta = itemStack.getItemMeta();
        return meta.getPersistentDataContainer().has(noPickupKey, PersistentDataType.BYTE);
    }
}