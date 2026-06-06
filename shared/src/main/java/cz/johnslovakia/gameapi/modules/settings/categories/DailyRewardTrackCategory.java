package cz.johnslovakia.gameapi.modules.settings.categories;

import cz.johnslovakia.gameapi.guis.ConfirmInventory;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.dailyRewardTrack.DailyRewardTrackModule;
import cz.johnslovakia.gameapi.modules.dailyRewardTrack.DailyRewardTier;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.settings.*;
import cz.johnslovakia.gameapi.rewards.RewardItem;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DailyRewardTrackCategory implements SettingCategory {

    @Override public String getName() { return "Daily Reward Track"; }
    @Override public Material getIcon() { return Material.GOLD_INGOT; }
    @Override public String[] getLore() {
        return new String[]{"§7Edit daily reward tiers,", "§7XP thresholds and rewards."};
    }

    @Override
    public void open(Player player) {
        openMain(player);
    }

    private static void openMain(Player player) {
        List<BottomAction> actions = List.of(
                new BottomAction(8, new ItemBuilder(Material.BARRIER)
                        .setName("§cRestore default settings")
                        .removeLore()
                        .addLoreLine("")
                        .addLoreLine("§c► Click to restore default settings")
                        .toItemStack(), p -> {
                    new ConfirmInventory(PlayerIdentityRegistry.get(p), "§cRestore default settings", playerIdentity -> {
                        SettingsEditSession.runAction(p, () -> {
                            DailyRewardTrackModule dailyRewardTrackModule = DailyRewardTrackModule.createDefault();
                            DailyRewardTrackModule.saveDailyRewardTrackModule(dailyRewardTrackModule);
                            ModuleManager.getInstance().destroyModule(DailyRewardTrackModule.class);
                            ModuleManager.getInstance().registerModule(dailyRewardTrackModule);
                        });

                        openMain(p);
                        p.sendMessage("§cDaily Reward Track settings were reset to default. Changes may not apply until the server is restarted.");
                    }, playerIdentity -> openMain(playerIdentity.getOnlinePlayer())).openGUI();
                })
        );

        SettingPageGUI.open(player, "Daily Reward Track",
                DailyRewardTrackCategory::buildMainItems,
                p -> ModuleManager.getModule(SettingsModule.class).open(p),
                actions);
    }

    private static List<SettingItem> buildMainItems() {
        DailyRewardTrackModule module = ModuleManager.getModule(DailyRewardTrackModule.class);
        List<SettingItem> items = new ArrayList<>();

        /*ItemBuilder maxTierItem = new ItemBuilder(Material.COMPARATOR);
        maxTierItem.setName("§fMax Tiers: §a" + module.getMaxTier());
        maxTierItem.removeLore();
        maxTierItem.addLoreLine("§7Maximum number of daily reward tiers.");
        maxTierItem.addLoreLine("");
        maxTierItem.addLoreLine("§fLeft: §a+1 §8| §fRight: §c-1");
        items.add(SettingItem.of(maxTierItem.toItemStack(), ctx -> {
            DailyRewardTrackModule m = ModuleManager.getModule(DailyRewardTrackModule.class);
            m.setMaxTier(Math.max(1, m.getMaxTier() + ctx.delta(+1, -1, +5, -5)));
            DailyRewardTrackModule.saveDailyRewardTrackModule(m);
        }));*/

        List<DailyRewardTier> tiers = module.getTiers();
        for (int i = 0; i < tiers.size(); i++) {
            DailyRewardTier tier = tiers.get(i);
            final int idx = i;

            ItemBuilder b = new ItemBuilder(Material.EXPERIENCE_BOTTLE);
            b.setName("§fTier " + tier.tier());
            b.removeLore();
            b.addLoreLine("");
            b.addLoreLine("§7XP needed: §a" + StringUtils.betterNumberFormat(tier.neededXP()));
            b.addLoreLine("");
            b.addLoreLine("§7Rewards:");
            if (!tier.reward().getRewardItems().isEmpty()) {
                for (RewardItem ri : tier.reward().getRewardItems()) {
                    b.addLoreLine(RewardSettingsHelper.rewardItemLine(ri));
                }
            } else {
                b.addLoreLine(" §cNone");
            }
            b.addLoreLine("");
            b.addLoreLine("§a► Click to edit XP threshold and rewards");

            items.add(SettingItem.navigate(b.toItemStack(), ctx -> openTierEditor(ctx.player, idx)));
        }

        return items;
    }

    private static void openTierEditor(Player player, int tierIdx) {
        if (!hasTier(tierIdx)) {
            openMain(player);
            return;
        }
        SettingPageGUI.open(player, "Tier " + (tierIdx + 1),
                () -> buildTierItems(tierIdx),
                DailyRewardTrackCategory::openMain,
                List.of(new BottomAction(4, RewardSettingsHelper.addResourceIcon(),
                        p -> openTierRewardResourcePicker(p, tierIdx))));
    }

    private static void openTierRewardResourcePicker(Player player, int tierIdx) {
        if (!hasTier(tierIdx)) {
            openMain(player);
            return;
        }
        RewardSettingsHelper.openResourcePicker(player,
                "Add Tier Reward",
                () -> ModuleManager.getModule(DailyRewardTrackModule.class)
                        .getTiers().get(tierIdx).reward().getRewardItems(),
                resource -> {
                    DailyRewardTrackModule module = ModuleManager.getModule(DailyRewardTrackModule.class);
                    module.getTiers().get(tierIdx).reward().addRewardItem(new RewardItem(resource, 10));
                    DailyRewardTrackModule.saveDailyRewardTrackModule(module);
                },
                p -> openTierEditor(p, tierIdx));
    }

    private static List<SettingItem> buildTierItems(int tierIdx) {
        DailyRewardTrackModule module = ModuleManager.getModule(DailyRewardTrackModule.class);
        DailyRewardTier tier = module.getTiers().get(tierIdx);
        List<SettingItem> items = new ArrayList<>();

        ItemBuilder xpItem = new ItemBuilder(Material.CLOCK);
        xpItem.setName("§fNeeded XP: §a" + StringUtils.betterNumberFormat(tier.neededXP()));
        xpItem.removeLore();
        xpItem.addLoreLine("§7XP required to reach this tier.");
        xpItem.addLoreLine("");
        xpItem.addLoreLine("§fLeft: §a+50 §8| §fRight: §c-50");
        xpItem.addLoreLine("§fShift+Left: §a+250 §8| §fShift+Right: §c-250");
        items.add(SettingItem.of(xpItem.toItemStack(), ctx -> {
            DailyRewardTrackModule m = ModuleManager.getModule(DailyRewardTrackModule.class);
            DailyRewardTier t = m.getTiers().get(tierIdx);
            int newXP = Math.max(1, t.neededXP() + ctx.delta(+50, -50, +250, -250));
            m.getTiers().set(tierIdx, new DailyRewardTier(t.tier(), newXP, t.reward()));
            DailyRewardTrackModule.saveDailyRewardTrackModule(m);
        }));

        List<RewardItem> rewardItems = tier.reward().getRewardItems();
        if (rewardItems.isEmpty()) {
            items.add(SettingItem.display(new ItemBuilder(Material.BARRIER)
                    .setName("§cNo rewards configured")
                    .removeLore()
                    .addLoreLine("§7Use the Add Resource button below.")
                    .toItemStack()));
        } else {
            items.addAll(RewardSettingsHelper.buildRewardNavItems(rewardItems,
                    (rewardIdx, p) -> openRewardItemDetail(p, tierIdx, rewardIdx),
                    (rewardIdx, p) -> removeTierRewardItem(tierIdx, rewardIdx),
                    p -> openTierEditor(p, tierIdx)));
        }

        return items;
    }

    private static void openRewardItemDetail(Player player, int tierIdx, int rewardIdx) {
        if (!hasTier(tierIdx) || !hasTierRewardItem(tierIdx, rewardIdx)) {
            openTierEditor(player, tierIdx);
            return;
        }
        SettingPageGUI.open(player, "Reward Item",
                () -> RewardSettingsHelper.buildRewardItemDetailItems(
                        () -> ModuleManager.getModule(DailyRewardTrackModule.class)
                                .getTiers().get(tierIdx).reward().getRewardItems().get(rewardIdx),
                        ri -> DailyRewardTrackModule.saveDailyRewardTrackModule(
                                ModuleManager.getModule(DailyRewardTrackModule.class)),
                        p -> removeTierRewardItem(tierIdx, rewardIdx),
                        p -> openTierEditor(p, tierIdx)
                ),
                p -> openTierEditor(p, tierIdx));
    }

    private static void removeTierRewardItem(int tierIdx, int rewardIdx) {
        if (!hasTier(tierIdx)) {
            return;
        }
        DailyRewardTrackModule module = ModuleManager.getModule(DailyRewardTrackModule.class);
        List<RewardItem> rewardItems = module.getTiers().get(tierIdx).reward().getRewardItems();
        if (rewardIdx >= 0 && rewardIdx < rewardItems.size()) {
            rewardItems.remove(rewardIdx);
            DailyRewardTrackModule.saveDailyRewardTrackModule(module);
        }
    }

    private static boolean hasTier(int tierIdx) {
        return tierIdx >= 0 && tierIdx < ModuleManager.getModule(DailyRewardTrackModule.class).getTiers().size();
    }

    private static boolean hasTierRewardItem(int tierIdx, int rewardIdx) {
        if (!hasTier(tierIdx)) {
            return false;
        }
        List<RewardItem> rewardItems = ModuleManager.getModule(DailyRewardTrackModule.class)
                .getTiers().get(tierIdx).reward().getRewardItems();
        return rewardIdx >= 0 && rewardIdx < rewardItems.size();
    }
}
