package cz.johnslovakia.gameapi.game.kit;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@Getter
public class KitContent {

    private Inventory inventory = Bukkit.createInventory(null, InventoryType.PLAYER);

    public KitContent(Inventory inventory) {
        this.inventory = inventory;
    }

    public KitContent(ItemStack... items) {
        addItem(items);
    }

    public void addItem(ItemStack... items){
        for (ItemStack item : items){
            if (item.getType().toString().toLowerCase().contains("helmet")){
                inventory.setItem(39, item);
            }else if (item.getType().toString().toLowerCase().contains("chestplate")){
                inventory.setItem(38, item);
            }else if (item.getType().toString().toLowerCase().contains("leggings")){
                inventory.setItem(37, item);
            }else if (item.getType().toString().toLowerCase().contains("boots")){
                inventory.setItem(36, item);
            }else {
                inventory.addItem(item);
            }
        }
    }

    public ItemStack[] getContents(){
        return inventory.getContents();
    }
}
