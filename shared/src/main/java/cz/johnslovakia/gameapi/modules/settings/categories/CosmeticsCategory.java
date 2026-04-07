package cz.johnslovakia.gameapi.modules.settings.categories;

import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.guis.ConfirmInventory;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticPrices;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.settings.SettingCategory;
import cz.johnslovakia.gameapi.modules.settings.SettingItem;
import cz.johnslovakia.gameapi.modules.settings.SettingPageGUI;
import cz.johnslovakia.gameapi.modules.settings.SettingsModule;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CosmeticsCategory implements SettingCategory {

    @Override public String getName() { return "Cosmetics Prices"; }
    @Override public Material getIcon() { return Material.FIREWORK_ROCKET; }
    @Override public String[] getLore() {
        return new String[]{"§7Edit prices per rarity", "§7for each cosmetic type."};
    }

    @Override
    public void open(Player player) { openTypeList(player); }

    private record CosmeticType(String name, String key, Material icon) {}

    private static final List<CosmeticType> TYPES = List.of(
        new CosmeticType("Kill Sounds", "kill_sounds", Material.NOTE_BLOCK),
        new CosmeticType("Projectile Trails", "projectile_trails", Material.BLAZE_ROD),
        new CosmeticType("Kill Effects", "kill_effects", Material.BLAZE_POWDER),
        new CosmeticType("Kill Messages", "kill_messages", Material.WRITABLE_BOOK),
        new CosmeticType("Hats", "hats", Material.LEATHER_HELMET)
    );

    private static final String[] RARITIES = {"legendary", "epic", "rare", "uncommon", "common"};
    private static final Material[] RARITY_ICONS = {
        Material.YELLOW_DYE, Material.PURPLE_DYE, Material.BLUE_DYE,
        Material.GREEN_DYE, Material.WHITE_DYE
    };

    public static void openTypeList(Player player) {
        SettingPageGUI.open(player, "Cosmetics Prices",
                () -> buildTypeItems(player),
                p -> ModuleManager.getModule(SettingsModule.class).open(p));
    }

    private static List<SettingItem> buildTypeItems(Player player) {
        CosmeticsModule module = ModuleManager.getModule(CosmeticsModule.class);
        Resource resource = module.getMainResource();
        List<SettingItem> items = new ArrayList<>();

        for (CosmeticType type : TYPES) {
            CosmeticPrices.PriceSet set = module.getPrices().getByKey(type.key());
            ItemBuilder b = new ItemBuilder(type.icon());
            b.setName("§f" + type.name());
            b.removeLore();
            b.addLoreLine("");
            for (String rarity : RARITIES) {
                int price = set != null ? set.getByRarity(rarity) : 0;
                b.addLoreLine("§7" + capitalize(rarity) + ": " + resource.getColor() + StringUtils.betterNumberFormat(price) + " " + resource.getDisplayName());
            }
            b.addLoreLine("");
            b.addLoreLine("§a► Click to edit");
            items.add(SettingItem.navigate(b.toItemStack(), ctx -> openRarityEditor(ctx.player, type)));
        }

        items.add(SettingItem.navigate(new ItemBuilder(Material.BARRIER)
                .setName("§cRestore default settings")
                .removeLore()
                .addLoreLine("")
                .addLoreLine("§c► Click to restore default settings")
                .toItemStack(), ctx -> {
            new ConfirmInventory(PlayerIdentityRegistry.get(ctx.player), "§cRestore default settings", playerIdentity -> {
                CosmeticPrices defaultPrices = new CosmeticPrices();
                ModuleManager.getModule(CosmeticsModule.class).savePrices(defaultPrices);

                ctx.player.sendMessage("§cYou have restored the Cosmetic Prices to default. Server restart recommended.");
                openTypeList(ctx.player);
            }, playerIdentity -> openTypeList(playerIdentity.getOnlinePlayer())).openGUI();
        }));

        return items;
    }

    private static void openRarityEditor(Player player, CosmeticType type) {
        SettingPageGUI.open(player, type.name() + " Prices",
                () -> buildRarityItems(type),
                CosmeticsCategory::openTypeList);
    }

    private static List<SettingItem> buildRarityItems(CosmeticType type) {
        CosmeticsModule module = ModuleManager.getModule(CosmeticsModule.class);
        CosmeticPrices.PriceSet set = module.getPrices().getByKey(type.key());
        Resource resource = module.getMainResource();
        List<SettingItem> items = new ArrayList<>();

        for (int i = 0; i < RARITIES.length; i++) {
            final String rarity = RARITIES[i];
            int price = set != null ? set.getByRarity(rarity) : 0;

            ItemBuilder b = new ItemBuilder(RARITY_ICONS[i]);
            b.setName("§f" + capitalize(rarity) + " price §7(" + type.name() + ")");
            b.removeLore();
            b.addLoreLine("");
            b.addLoreLine("§7Price: " + resource.getColor() + StringUtils.betterNumberFormat(price) + " " + resource.getDisplayName());
            b.addLoreLine("");
            b.addLoreLine("§fLeft: §a+50 §8| §fRight: §c-50");
            b.addLoreLine("§fShift+Left: §a+500 §8| §fShift+Right: §c-500");

            items.add(SettingItem.of(b.toItemStack(), ctx -> {
                CosmeticPrices.PriceSet current = ModuleManager.getModule(CosmeticsModule.class).getPrices().getByKey(type.key());
                if (current == null) return;
                current.setByRarity(rarity, Math.max(0, current.getByRarity(rarity) + ctx.delta(+50, -50, +500, -500)));
                Bukkit.getScheduler().runTaskAsynchronously(Core.getInstance().getPlugin(),
                        () -> ModuleManager.getModule(CosmeticsModule.class).savePrices());
                openRarityEditor(ctx.player, type);
            }));
        }

        return items;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}