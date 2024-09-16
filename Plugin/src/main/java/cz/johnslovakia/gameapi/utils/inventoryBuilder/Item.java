package cz.johnslovakia.gameapi.utils.inventoryBuilder;

import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class Item implements Listener {

    private final ItemStack item;
    private final int slot;
    private final String translateKey;
    private Consumer<PlayerInteractEvent> consumer;
    private boolean playerHead = false;

    public Item(ItemStack item, int slot, String translateKey, Consumer<PlayerInteractEvent> consumer) {
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

    public boolean isPlayerHead() {
        return playerHead;
    }

    public void run(PlayerInteractEvent e) { consumer.accept(e); }

    public String getTranslateKey() {
        return translateKey;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getSlot() {
        return slot;
    }

    public Consumer<PlayerInteractEvent> getConsumer() {
        return consumer;
    }
}