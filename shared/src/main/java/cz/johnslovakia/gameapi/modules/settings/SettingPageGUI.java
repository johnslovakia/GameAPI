package cz.johnslovakia.gameapi.modules.settings;

import cz.johnslovakia.gameapi.utils.ItemBuilder;
import me.zort.containr.Component;
import me.zort.containr.GUI;
import me.zort.containr.PagedContainer;
import me.zort.containr.component.element.PagingArrowElement;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingPageGUI {

    public static void open(Player player, String title, Supplier<List<SettingItem>> itemsSupplier, Consumer<Player> back) {
        open(player, title, itemsSupplier, back, null);
    }

    public static void open(Player player, String title, Supplier<List<SettingItem>> itemsSupplier, Consumer<Player> back, List<BottomAction> bottomActions) {
        List<SettingItem> items = itemsSupplier.get();
        boolean paged = items.size() > 36;
        int rows = paged ? 6 : Math.min(6, Math.max(2, (int) Math.ceil((items.size() + 9) / 9.0) + 1));

        GUI gui = Component.gui()
                .title(net.kyori.adventure.text.Component.text("§8" + title))
                .rows(rows)
                .prepare((g, p) -> {
                    List<SettingItem> current = itemsSupplier.get();

                    if (paged) {
                        PagedContainer container = Component.pagedContainer()
                                .size(9, rows - 2)
                                .init(c -> {
                                    for (SettingItem si : current) {
                                        c.appendElement(Component.element(si.item).addClick(clickInfo -> {
                                            si.onClick.accept(new ClickContext(clickInfo.getPlayer(), clickInfo));
                                            clickInfo.getPlayer().playSound(clickInfo.getPlayer(), Sound.UI_BUTTON_CLICK, 0.6F, 1.2F);
                                            if (!si.navigate) {
                                                open(clickInfo.getPlayer(), title, itemsSupplier, back, bottomActions);
                                            }
                                        }).build());
                                    }
                                })
                                .build();
                        g.setContainer(0, container);

                        int bottomStart = (rows - 1) * 9;

                        g.setContainer(bottomStart - 9, Component.staticContainer()
                                .size(9, 1)
                                .init(c -> c.fillElement(Component.element(
                                        new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                                                .setName("§r").toItemStack()).build()))
                                .build());

                        g.appendElement(bottomStart + 2, new PagingArrowElement(container, true, "§c§l◄ Previous Page"));
                        g.appendElement(bottomStart + 6, new PagingArrowElement(container, false, "§a§lNext Page ►"));

                        ItemBuilder backBtn = new ItemBuilder(Material.ARROW).setName("§cGo Back");
                        g.appendElement(bottomStart, Component.element(backBtn.toItemStack()).addClick(e -> {
                            back.accept(e.getPlayer());
                            e.getPlayer().playSound(e.getPlayer(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                        }).build());

                        if (bottomActions != null) {
                            for (BottomAction action : bottomActions) {
                                g.appendElement(bottomStart + action.slotOffset, Component.element(action.icon).addClick(clickInfo -> {
                                    action.onClick.accept(clickInfo.getPlayer());
                                    clickInfo.getPlayer().playSound(clickInfo.getPlayer(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                                }).build());
                            }
                        }
                    } else {
                        for (int i = 0; i < current.size(); i++) {
                            SettingItem si = current.get(i);
                            g.appendElement(i, Component.element(si.item).addClick(clickInfo -> {
                                si.onClick.accept(new ClickContext(clickInfo.getPlayer(), clickInfo));
                                clickInfo.getPlayer().playSound(clickInfo.getPlayer(), Sound.UI_BUTTON_CLICK, 0.6F, 1.2F);
                                if (!si.navigate) {
                                    open(clickInfo.getPlayer(), title, itemsSupplier, back, bottomActions);
                                }
                            }).build());
                        }

                        int bottomStart = (rows - 1) * 9;

                        ItemBuilder backBtn = new ItemBuilder(Material.ARROW).setName("§cGo Back");
                        g.appendElement(bottomStart, Component.element(backBtn.toItemStack()).addClick(e -> {
                            back.accept(e.getPlayer());
                            e.getPlayer().playSound(e.getPlayer(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                        }).build());

                        if (bottomActions != null) {
                            for (BottomAction action : bottomActions) {
                                g.appendElement(bottomStart + action.slotOffset, Component.element(action.icon).addClick(clickInfo -> {
                                    action.onClick.accept(clickInfo.getPlayer());
                                    clickInfo.getPlayer().playSound(clickInfo.getPlayer(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                                }).build());
                            }
                        }
                    }
                })
                .build();

        gui.open(player);
    }
}