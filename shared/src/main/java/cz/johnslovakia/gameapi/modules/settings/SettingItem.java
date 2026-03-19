package cz.johnslovakia.gameapi.modules.settings;

import org.bukkit.inventory.ItemStack;
import java.util.function.Consumer;

public class SettingItem {

    public final ItemStack item;
    public final Consumer<ClickContext> onClick;
    public final boolean navigate;

    private SettingItem(ItemStack item, Consumer<ClickContext> onClick, boolean navigate) {
        this.item = item;
        this.onClick = onClick;
        this.navigate = navigate;
    }

    public static SettingItem of(ItemStack item, Consumer<ClickContext> onClick) {
        return new SettingItem(item, onClick, false);
    }

    public static SettingItem navigate(ItemStack item, Consumer<ClickContext> onClick) {
        return new SettingItem(item, onClick, true);
    }

    public static SettingItem display(ItemStack item) {
        return new SettingItem(item, ctx -> {}, false);
    }
}