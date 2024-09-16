package cz.johnslovakia.gameapi.game.kit;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
public class KitContent {

    private final Inventory inventory;
    private List<ItemStack> armor = new ArrayList<>();

    public KitContent(Inventory inventory, List<ItemStack> armor) {
        this.inventory = inventory;
        this.armor = armor;
    }

    public KitContent(Inventory inventory) {
        this.inventory = inventory;
    }

    public KitContent(List<ItemStack> armor) {
        this.inventory = Bukkit.createInventory(null, InventoryType.PLAYER);
        this.armor = armor;
    }

    public KitContent(ItemStack... items) {
        Inventory inv = Bukkit.createInventory(null, InventoryType.PLAYER);
        List<ItemStack> armor = new ArrayList<>();
        for (ItemStack item : items){
            if (item.getType().toString().toLowerCase().contains("chestplate")
            || item.getType().toString().toLowerCase().contains("leggings")
            || item.getType().toString().toLowerCase().contains("boots")
            || item.getType().toString().toLowerCase().contains("helmet")){
                armor.add(item);
                continue;
            }
            inv.addItem(item);
        }

        this.inventory = inv;
        this.armor = armor;
    }
}
