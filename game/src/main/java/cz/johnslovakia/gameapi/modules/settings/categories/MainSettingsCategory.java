package cz.johnslovakia.gameapi.modules.settings.categories;

import cz.johnslovakia.gameapi.guis.ConfirmInventory;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.settings.*;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MainSettingsCategory implements SettingCategory {

    private static final String DEFAULT_LOCAL_KEYS_PATH = "local_settings.main_settings";

    private final String dbKey;
    private final MainSettingsConfig.LocalConfigProvider localCfg;
    private final List<MinigameSettingDefinition> definitions;
    private final String localKeysPath;

    public MainSettingsCategory(String dbKey, MainSettingsConfig.LocalConfigProvider localCfg, List<MinigameSettingDefinition> definitions) {
        this(dbKey, localCfg, definitions, DEFAULT_LOCAL_KEYS_PATH);
    }

    public MainSettingsCategory(String dbKey, MainSettingsConfig.LocalConfigProvider localCfg, List<MinigameSettingDefinition> definitions, String localKeysPath) {
        this.dbKey = dbKey;
        this.localCfg = localCfg;
        this.definitions = definitions;
        this.localKeysPath = localKeysPath;
    }

    @Override
    public String getName() { return "Game Settings"; }

    @Override
    public Material getIcon() { return Material.COMMAND_BLOCK; }

    @Override
    public String[] getLore() {
        return new String[]{"§7Timers, player counts", "§7and feature toggles."};
    }

    @Override
    public void open(Player player) { openMain(player); }

    private void openMain(Player player) {
        SettingPageGUI.open(player, "Game Settings",
                this::buildItems,
                p -> ModuleManager.getModule(SettingsModule.class).open(p),
                List.of(
                        new BottomAction(4, localStorageIcon(), this::openLocalStorage),
                        new BottomAction(8, resetIcon(), p -> new ConfirmInventory(
                                PlayerIdentityRegistry.get(p),
                                "§cReset Game Settings to default?",
                                pi -> {
                                    SettingsEditSession.runAction(p, () ->
                                            MainSettingsConfig.resetToDefault(dbKey, definitions, localCfg, localKeysPath));
                                    p.sendMessage("§cGame Settings were reset to default. Changes may not apply until the server is restarted.");
                                    p.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1F, 1.5F);
                                    openMain(p);
                                },
                                pi -> openMain(pi.getOnlinePlayer())
                        ).openGUI())
                ));
    }

    private List<SettingItem> buildItems() {
        MainSettingsConfig cfg = config();
        List<SettingItem> items = new ArrayList<>();
        for (MinigameSettingDefinition def : definitions) {
            if (def.getType() == MinigameSettingDefinition.Type.INT)
                items.add(buildIntItem(def, cfg.getInt(def.getKey(), def.getDefaultInt()), cfg.isLocal(def.getKey())));
            else
                items.add(buildBoolItem(def, cfg.getBoolean(def.getKey(), def.getDefaultBool()), cfg.isLocal(def.getKey())));
        }
        return items;
    }

    private SettingItem buildIntItem(MinigameSettingDefinition def, int value, boolean local) {
        ItemBuilder b = new ItemBuilder(def.getIcon());
        b.setName("§f" + def.getName() + ": §e" + value);
        b.removeLore();
        if (!def.getDescription().isEmpty()) b.addLoreLine(def.getDescription());
        b.addLoreLine("§8Range: " + def.getMin() + " - " + (def.getMax() == Integer.MAX_VALUE ? "unlimited" : def.getMax()));
        b.addLoreLine("");
        b.addLoreLine("§7Storage: " + storageLabel(local));
        b.addLoreLine("");
        b.addLoreLine("§fLeft: §a+1 §8| §fRight: §c-1");
        b.addLoreLine("§fShift+Left: §a+5 §8| §fShift+Right: §c-5");

        return SettingItem.of(b.toItemStack(), ctx -> {
            MainSettingsConfig cfg = config();
            int current = cfg.getInt(def.getKey(), def.getDefaultInt());
            int updated = Math.max(def.getMin(), Math.min(def.getMax(), current + ctx.delta(+1, -1, +5, -5)));
            cfg.setInt(def.getKey(), updated);
            MainSettingsConfig.save(cfg);
        });
    }

    private SettingItem buildBoolItem(MinigameSettingDefinition def, boolean value, boolean local) {
        ItemBuilder b = new ItemBuilder(def.getIcon());//new ItemBuilder(value ? Material.LIME_DYE : Material.GRAY_DYE);
        b.setName((value ? "§a" : "§7") + def.getName() + ": " + (value ? "§aEnabled" : "§cDisabled"));
        b.removeLore();
        if (!def.getDescription().isEmpty()) b.addLoreLine(def.getDescription());
        b.addLoreLine("");
        b.addLoreLine("§7Storage: " + storageLabel(local));
        b.addLoreLine("");
        b.addLoreLine("§a► Click to switch");

        return SettingItem.of(b.toItemStack(), ctx -> {
            MainSettingsConfig cfg = config();
            cfg.setBoolean(def.getKey(), !cfg.getBoolean(def.getKey(), def.getDefaultBool()));
            MainSettingsConfig.save(cfg);
        });
    }

    private void openLocalStorage(Player player) {
        SettingPageGUI.open(player, "Local Settings", this::buildLocalStorageItems, this::openMain);
    }

    private List<SettingItem> buildLocalStorageItems() {
        MainSettingsConfig cfg = config();
        List<SettingItem> items = new ArrayList<>();
        for (MinigameSettingDefinition def : definitions) {
            String valueStr = def.getType() == MinigameSettingDefinition.Type.INT
                    ? String.valueOf(cfg.getInt(def.getKey(), def.getDefaultInt()))
                    : (cfg.getBoolean(def.getKey(), def.getDefaultBool()) ? "Enabled" : "Disabled");
            items.add(buildLocalStorageItem(def.getKey(), def.getIcon(), def.getName(), valueStr, cfg.isLocal(def.getKey())));
        }
        return items;
    }

    private SettingItem buildLocalStorageItem(String key, Material icon, String name, String value, boolean local) {
        ItemBuilder b = new ItemBuilder(local ? Material.ENDER_CHEST : icon);
        b.setName((local ? "§aLocal §f" + name : "§bGlobal §f" + name));
        b.removeLore();
        b.addLoreLine("§7Current value: §f" + value);
        b.addLoreLine("§7Storage: " + storageLabel(local));
        b.addLoreLine("");
        b.addLoreLine(local ? "§a► Click to use global database storage" : "§a► Click to store this setting locally");

        return SettingItem.of(b.toItemStack(), ctx -> {
            config().setLocalStorage(key, !config().isLocal(key));
            ctx.player.playSound(ctx.player, Sound.UI_BUTTON_CLICK, 0.4F, 1.3F);
            openLocalStorage(ctx.player);
        });
    }

    private MainSettingsConfig config() {
        return MainSettingsConfig.get(dbKey, definitions, localCfg, localKeysPath);
    }

    private static ItemStack localStorageIcon() {
        return new ItemBuilder(Material.ENDER_CHEST)
                .setName("§bLocal Settings")
                .removeLore()
                .addLoreLine("§7Choose which settings are saved")
                .addLoreLine("§7locally and only apply to this server.")
                .toItemStack();
    }

    private static ItemStack resetIcon() {
        return new ItemBuilder(Material.BARRIER).setName("§cReset to Default").removeLore()
                .addLoreLine("").addLoreLine("§cClick to restore default settings").toItemStack();
    }

    private static String storageLabel(boolean local) {
        return local ? "§aLocal" : "§bGlobal database";
    }
}
