package cz.johnslovakia.gameapi.modules.kits;

import cz.johnslovakia.gameapi.utils.ItemBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@Getter
public class KitContent {

    private final Inventory inventory;

    public KitContent(Inventory inventory) {
        this.inventory = inventory;
    }

    public KitContent(ItemStack... items) {
        this.inventory = Bukkit.createInventory(null, InventoryType.PLAYER);
        addItem(items);
    }

    public KitContent() {
        this.inventory = Bukkit.createInventory(null, InventoryType.PLAYER);
    }


    public KitContent setItem(int slot, ItemBuilder item){
        setItem(slot, item.toItemStack());
        return this;
    }

    public KitContent setItem(int slot, ItemStack item){
        inventory.setItem(slot, item);
        return this;
    }

    public KitContent addItem(ItemBuilder... items){
        for (ItemBuilder itemBuilder : items){
            addItem(itemBuilder.toItemStack());
        }
        return this;
    }

    public KitContent addItem(ItemStack... items){
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
        return this;
    }

    public ItemStack[] getContents(){
        return inventory.getContents();
    }
}
