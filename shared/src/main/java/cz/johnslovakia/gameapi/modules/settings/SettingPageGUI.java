package cz.johnslovakia.gameapi.modules.settings;

import cz.johnslovakia.gameapi.utils.ItemBuilder;
import me.zort.containr.Component;
import me.zort.containr.GUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingPageGUI {

    public static void open(Player player, String title,
                             Supplier<List<SettingItem>> itemsSupplier,
                             Consumer<Player> back) {
        List<SettingItem> items = itemsSupplier.get();
        int rows = Math.min(6, Math.max(2, (int) Math.ceil((items.size() + 9) / 9.0) + 1));

        GUI gui = Component.gui()
                .title(net.kyori.adventure.text.Component.text("§8" + title))
                .rows(rows)
                .prepare((g, p) -> {
                    List<SettingItem> current = itemsSupplier.get();

                    for (int i = 0; i < current.size(); i++) {
                        SettingItem si = current.get(i);
                        g.appendElement(i, Component.element(si.item).addClick(clickInfo -> {
                            si.onClick.accept(new ClickContext(clickInfo.getPlayer(), clickInfo));
                            clickInfo.getPlayer().playSound(clickInfo.getPlayer(), Sound.UI_BUTTON_CLICK, 0.6F, 1.2F);
                            if (!si.navigate) {
                                open(clickInfo.getPlayer(), title, itemsSupplier, back);
                            }
                        }).build());
                    }

                    int bottomStart = (rows - 1) * 9;

                    ItemBuilder backBtn = new ItemBuilder(Material.ARROW);
                    backBtn.setName("§cGo Back");
                    g.appendElement(bottomStart, Component.element(backBtn.toItemStack()).addClick(e -> {
                        back.accept(e.getPlayer());
                        e.getPlayer().playSound(e.getPlayer(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                    }).build());
                })
                .build();

        gui.open(player);
    }
}