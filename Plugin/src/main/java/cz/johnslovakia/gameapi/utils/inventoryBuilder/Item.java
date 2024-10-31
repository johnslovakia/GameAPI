package cz.johnslovakia.gameapi.utils.inventoryBuilder;

import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

@Getter
public class Item implements Listener {

    private final ItemStack item;
    private final int slot;
    private final String translateKey;
    private Consumer<GamePlayer> consumer;
    private boolean playerHead = false;

    public Item(ItemStack item, int slot, String translateKey, Consumer<GamePlayer> consumer) {
        this.slot = slot;
        this.item = item;
        this.translateKey = translateKey;
        this.consumer = consumer;
    }

    public Item(ItemStack item, int slot, String translateKey) {
        this.slot = slot;
        this.item = item;
        this.translateKey = translateKey;
    }

    public Item setItemAsPlayerHead(boolean b){
        this.playerHead = b;
        return this;
    }

    public void run(GamePlayer gamePlayer) { consumer.accept(gamePlayer); }

}