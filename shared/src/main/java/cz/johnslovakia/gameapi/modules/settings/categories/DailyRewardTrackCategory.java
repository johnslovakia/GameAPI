package cz.johnslovakia.gameapi.modules.settings.categories;

import cz.johnslovakia.gameapi.guis.ConfirmInventory;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.dailyRewardTrack.DailyRewardTrackModule;
import cz.johnslovakia.gameapi.modules.dailyRewardTrack.DailyRewardTier;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.settings.SettingCategory;
import cz.johnslovakia.gameapi.modules.settings.SettingItem;
import cz.johnslovakia.gameapi.modules.settings.SettingPageGUI;
import cz.johnslovakia.gameapi.modules.settings.SettingsModule;
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
        SettingPageGUI.open(player, "Daily Reward Track",
                DailyRewardTrackCategory::buildMainItems,
                p -> ModuleManager.getModule(SettingsModule.class).open(p));
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
                    Resource resource = ri.getResource();
                    String amount = ri.hasRandomAmount()
                            ? StringUtils.betterNumberFormat(ri.getRandomMinRange()) + "–" + StringUtils.betterNumberFormat(ri.getRandomMaxRange())
                            : StringUtils.betterNumberFormat(ri.getAmount());
                    String chance = ri.getChance() < 100 ? " §7(" + ri.getChance() + "% chance)" : "";

                    b.addLoreLine(" " + resource.getColor() + "+ " + amount + " " + resource.getDisplayName() + chance);
                }
            } else {
                b.addLoreLine(" §cNone");
            }
            b.addLoreLine("");
            b.addLoreLine("§a► Click to edit XP threshold and rewards");

            items.add(SettingItem.navigate(b.toItemStack(), ctx -> openTierEditor(ctx.player, idx)));
        }

        items.add(SettingItem.navigate(new ItemBuilder(Material.BARRIER)
                .setName("§cRestore default settings")
                .removeLore()
                .addLoreLine("")
                .addLoreLine("§c► Click to restore default settings")
                .toItemStack(), ctx -> {
            new ConfirmInventory(PlayerIdentityRegistry.get(ctx.player), "§cRestore default settings", playerIdentity -> {
                DailyRewardTrackModule dailyRewardTrackModule = DailyRewardTrackModule.createDefault();

                DailyRewardTrackModule.saveDailyRewardTrackModule(dailyRewardTrackModule);
                ModuleManager.getInstance().destroyModule(DailyRewardTrackModule.class);
                ModuleManager.getInstance().registerModule(dailyRewardTrackModule);

                openMain(ctx.player);
                ctx.player.sendMessage("§cYou have restored the Daily Reward Track to default. Server restart recommended.");
            }, playerIdentity -> openMain(playerIdentity.getOnlinePlayer())).openGUI();
        }));

        return items;
    }

    private static void openTierEditor(Player player, int tierIdx) {
        SettingPageGUI.open(player, "Tier " + (tierIdx + 1),
                () -> buildTierItems(tierIdx),
                DailyRewardTrackCategory::openMain);
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
        for (int i = 0; i < rewardItems.size(); i++) {
            RewardItem ri = rewardItems.get(i);
            final int rewardIdx = i;

            Resource resource = ri.getResource();
            ItemBuilder rb = new ItemBuilder(Material.GOLD_NUGGET);
            rb.setName("§fReward: " + resource.getColor() + resource.getDisplayName());
            rb.removeLore();
            rb.addLoreLine("");
            rb.addLoreLine("§7Amount: §a" + (ri.hasRandomAmount()
                    ? StringUtils.betterNumberFormat(ri.getRandomMinRange()) + "–" + StringUtils.betterNumberFormat(ri.getRandomMaxRange())
                    : StringUtils.betterNumberFormat((ri.getAmount()))));
            rb.addLoreLine("§7Chance: §a" + ri.getChance() + "%");
            rb.addLoreLine("");
            rb.addLoreLine("§a► Click to edit");

            items.add(SettingItem.navigate(rb.toItemStack(),
                    ctx -> openRewardItemDetail(ctx.player, tierIdx, rewardIdx)));
        }

        return items;
    }

    private static void openRewardItemDetail(Player player, int tierIdx, int rewardIdx) {
        SettingPageGUI.open(player, "Reward Item",
                () -> buildRewardDetailItems(tierIdx, rewardIdx),
                p -> openTierEditor(p, tierIdx));
    }

    private static List<SettingItem> buildRewardDetailItems(int tierIdx, int rewardIdx) {
        DailyRewardTrackModule module = ModuleManager.getModule(DailyRewardTrackModule.class);
        RewardItem ri = module.getTiers().get(tierIdx).reward().getRewardItems().get(rewardIdx);
        boolean isRandom = ri.hasRandomAmount();
        Resource resource = ri.getResource();
        List<SettingItem> items = new ArrayList<>();

        if (isRandom) {
            ItemBuilder minItem = new ItemBuilder(Material.GREEN_DYE);
            minItem.setName("§fMin Amount: " + resource.getColor() + StringUtils.betterNumberFormat(ri.getRandomMinRange()) + " " + resource.getDisplayName());
            minItem.removeLore();
            minItem.addLoreLine("");
            minItem.addLoreLine("§fLeft: §a+1 §8| §fRight: §c-1");
            minItem.addLoreLine("§fShift+Left: §a+10 §8| §fShift+Right: §c-10");
            items.add(SettingItem.of(minItem.toItemStack(), ctx -> {
                int delta = ctx.delta(+1, -1, +10, -10);
                DailyRewardTrackModule m = ModuleManager.getModule(DailyRewardTrackModule.class);
                RewardItem r = m.getTiers().get(tierIdx).reward().getRewardItems().get(rewardIdx);
                r.setRandomMinRange(Math.max(0, r.getRandomMinRange() + delta));
                if (r.getRandomMaxRange() < r.getRandomMinRange())
                    r.setRandomMaxRange(r.getRandomMinRange());
                DailyRewardTrackModule.saveDailyRewardTrackModule(m);
            }));

            ItemBuilder maxItem = new ItemBuilder(Material.LIME_DYE);
            maxItem.setName("§fMax Amount: " + resource.getColor() +StringUtils.betterNumberFormat(ri.getRandomMaxRange()) + " " + resource.getDisplayName());
            maxItem.removeLore();
            maxItem.addLoreLine("");
            maxItem.addLoreLine("§fLeft: §a+1 §8| §fRight: §c-1");
            maxItem.addLoreLine("§fShift+Left: §a+10 §8| §fShift+Right: §c-10");
            items.add(SettingItem.of(maxItem.toItemStack(), ctx -> {
                int delta = ctx.delta(+1, -1, +10, -10);
                DailyRewardTrackModule m = ModuleManager.getModule(DailyRewardTrackModule.class);
                RewardItem r = m.getTiers().get(tierIdx).reward().getRewardItems().get(rewardIdx);
                r.setRandomMaxRange(Math.max(r.getRandomMinRange(), r.getRandomMaxRange() + delta));
                DailyRewardTrackModule.saveDailyRewardTrackModule(m);
            }));
        } else {
            ItemBuilder amountItem = new ItemBuilder(Material.LIME_DYE);
            amountItem.setName("§fAmount: " + resource.getColor() + StringUtils.betterNumberFormat(ri.getAmount()) + " " + resource.getDisplayName());
            amountItem.removeLore();
            amountItem.addLoreLine("");
            amountItem.addLoreLine("§fLeft: §a+1 §8| §fRight: §c-1");
            amountItem.addLoreLine("§fShift+Left: §a+10 §8| §fShift+Right: §c-10");
            items.add(SettingItem.of(amountItem.toItemStack(), ctx -> {
                int delta = ctx.delta(+1, -1, +10, -10);
                DailyRewardTrackModule m = ModuleManager.getModule(DailyRewardTrackModule.class);
                RewardItem r = m.getTiers().get(tierIdx).reward().getRewardItems().get(rewardIdx);
                r.setAmount(Math.max(1, r.getAmount() + delta));
                DailyRewardTrackModule.saveDailyRewardTrackModule(m);
            }));
        }

        ItemBuilder chanceItem = new ItemBuilder(ri.getChance() >= 100 ? Material.EMERALD : Material.GOLD_NUGGET);
        chanceItem.setName("§fChance: §a" + ri.getChance() + "%");
        chanceItem.removeLore();
        chanceItem.addLoreLine("§7Probability that this reward is given.");
        chanceItem.addLoreLine("§a100% §7= always given.");
        chanceItem.addLoreLine("");
        chanceItem.addLoreLine("§fLeft: §a+1% §8| §fRight: §c-1%");
        chanceItem.addLoreLine("§fShift+Left: §a+5% §8| §fShift+Right: §c-5%");
        items.add(SettingItem.of(chanceItem.toItemStack(), ctx -> {
            int delta = ctx.delta(+1, -1, +5, -5);
            DailyRewardTrackModule m = ModuleManager.getModule(DailyRewardTrackModule.class);
            RewardItem r = m.getTiers().get(tierIdx).reward().getRewardItems().get(rewardIdx);
            r.setChance(Math.max(1, Math.min(100, r.getChance() + delta)));
            DailyRewardTrackModule.saveDailyRewardTrackModule(m);
        }));

        return items;
    }
}