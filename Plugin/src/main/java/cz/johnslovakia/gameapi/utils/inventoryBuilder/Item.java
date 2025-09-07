package cz.johnslovakia.gameapi.utils.inventoryBuilder;

import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.AbilityItem;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import javax.xml.validation.Validator;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
public class Item implements Listener {

    private final ItemStack item;
    private final int slot;
    private final String translateKey;
    private Consumer<PlayerInteractEvent> consumer;

    @Setter
    private Predicate<GamePlayer> blinking;
    @Setter
    private int blinkingItemCustomModelData;

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

    public void run(PlayerInteractEvent event) { consumer.accept(event); }

}