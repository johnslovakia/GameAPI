package cz.johnslovakia.gameapi.modules.settings;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class BottomAction {

    public final int slotOffset;
    public final ItemStack icon;
    public final Consumer<Player> onClick;

    public BottomAction(int slotOffset, ItemStack icon, Consumer<Player> onClick) {
        this.slotOffset = slotOffset;
        this.icon = icon;
        this.onClick = onClick;
    }
}