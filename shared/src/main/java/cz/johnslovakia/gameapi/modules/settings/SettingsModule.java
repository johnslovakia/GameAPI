package cz.johnslovakia.gameapi.modules.settings;

import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.utils.ConfigAPI;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import lombok.Getter;
import me.zort.containr.Component;
import me.zort.containr.GUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SettingsModule implements Module {

    private List<SettingCategory> categories = new ArrayList<>();
    private ConfigAPI mainConfig; //TODO:

    public SettingsModule(ConfigAPI mainConfig) {
        this.mainConfig = mainConfig;
    }

    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {
        SettingsEditSession.flushAll();
        categories = null;
    }


    public void register(SettingCategory category) {
        categories.add(category);
    }

    public void open(Player player) {
        int visibleItems = categories.size() + 1;
        int rows = Math.min(6, Math.max(1, (int) Math.ceil(visibleItems / 9.0)) + 1);

        GUI gui = Component.gui()
                .title(net.kyori.adventure.text.Component.text("§8Settings"))
                .rows(rows)
                .prepare((g, p) -> {
                    for (int i = 0; i < categories.size(); i++) {
                        SettingCategory cat = categories.get(i);
                        ItemBuilder b = new ItemBuilder(cat.getIcon());
                        b.setName("§f" + cat.getName());
                        b.removeLore();
                        for (String line : cat.getLore()) b.addLoreLine(line);
                        b.addLoreLine("");
                        b.addLoreLine("§a► Click to open");

                        g.appendElement(i, Component.element(b.toItemStack()).addClick(e -> {
                            cat.open(e.getPlayer());
                            e.getPlayer().playSound(e.getPlayer(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                        }).build());
                    }

                    int bottomStart = (rows - 1) * 9;

                    g.appendElement(bottomStart + 8, Component.element(saveInfoItem()).build());

                    ItemBuilder close = new ItemBuilder(Material.BARRIER).setName("§cClose");
                    g.appendElement(bottomStart + 4, Component.element(close.toItemStack()).addClick(e ->
                            e.getPlayer().closeInventory()).build());

                    SettingsEditSession session = SettingsEditSession.get(p);
                    if (session.hasUndo()) {
                        g.appendElement(bottomStart + 7, Component.element(session.undoItem()).addClick(e -> {
                            if (SettingsEditSession.get(e.getPlayer()).undoLast(e.getPlayer())) {
                                open(e.getPlayer());
                            }
                        }).build());
                    }
                })
                .build();

        gui.onClose(GUI.CloseReason.BY_PLAYER, SettingsEditSession::finish);
        gui.open(player);
    }

    private ItemStack saveInfoItem() {
        return new ItemBuilder(Material.PAPER)
                .setName("§aSaving Info")
                .removeLore()
                .addLoreLine("§7Changes are saved while moving back")
                .addLoreLine("§7through settings pages and when")
                .addLoreLine("§7you close this GUI .")
                .addLoreLine("")
                .addLoreLine("§7Undo stays available while this")
                .addLoreLine("§7settings session is open.")
                .toItemStack();
    }
}
