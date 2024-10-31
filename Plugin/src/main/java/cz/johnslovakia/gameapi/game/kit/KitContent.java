package cz.johnslovakia.gameapi.game.kit;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

@Getter
public class KitContent {

    private final Inventory inventory;

    public KitContent(Inventory inventory, List<ItemStack> armor) {
        this.inventory = inventory;
    }

    public KitContent(Inventory inventory) {
        this.inventory = inventory;
    }

    public KitContent(ItemStack... items) {
        Inventory inv = Bukkit.createInventory(null, InventoryType.PLAYER);

        for (ItemStack item : items){
            if (item.getType().toString().toLowerCase().contains("helmet")){
                inv.setItem(39, item);
            }else if (item.getType().toString().toLowerCase().contains("chestplate")){
                inv.setItem(38, item);
            }else if (item.getType().toString().toLowerCase().contains("leggings")){
                inv.setItem(37, item);
            }else if (item.getType().toString().toLowerCase().contains("boots")){
                inv.setItem(36, item);
            }else {
                inv.addItem(item);
            }
        }

        this.inventory = inv;
    }

    public ItemStack[] getContents(){
        return inventory.getContents();
    }
}
