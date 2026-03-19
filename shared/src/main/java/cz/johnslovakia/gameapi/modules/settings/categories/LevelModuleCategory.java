package cz.johnslovakia.gameapi.modules.settings.categories;

import cz.johnslovakia.gameapi.guis.ConfirmInventory;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.modules.levels.LevelRange;
import cz.johnslovakia.gameapi.modules.levels.LevelReward;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.settings.SettingCategory;
import cz.johnslovakia.gameapi.modules.settings.SettingItem;
import cz.johnslovakia.gameapi.modules.settings.SettingPageGUI;
import cz.johnslovakia.gameapi.modules.settings.SettingsModule;
import cz.johnslovakia.gameapi.rewards.Reward;
import cz.johnslovakia.gameapi.rewards.RewardItem;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LevelModuleCategory implements SettingCategory {

    @Override public String getName() { return "Level System"; }
    @Override public Material getIcon() { return Material.EXPERIENCE_BOTTLE; }
    @Override public String[] getLore() {
        return new String[]{"§7Edit level ranges and", "§7per-level rewards."};
    }

    @Override
    public void open(Player player) { openSubMenu(player); }

    private static void openSubMenu(Player player) {
        SettingPageGUI.open(player, "Level System", () -> List.of(

                SettingItem.navigate(
                        new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                                .setName("§fLevel Ranges")
                                .removeLore()
                                .addLoreLine("§7Base XP, XP scaling and per-range reward.")
                                .addLoreLine("")
                                .addLoreLine("§a► Click to open")
                                .toItemStack(),
                        ctx -> openRangeList(ctx.player)
                ),

                SettingItem.navigate(
                        new ItemBuilder(Material.GOLD_INGOT)
                                .setName("§fLevel Rewards")
                                .removeLore()
                                .addLoreLine("§7Edit reward amounts per level group.")
                                .addLoreLine("")
                                .addLoreLine("§a► Click to open")
                                .toItemStack(),
                        ctx -> openRewardEditor(ctx.player)
                ),

                SettingItem.navigate(new ItemBuilder(Material.BARRIER)
                        .setName("§cRestore default settings")
                        .removeLore()
                        .addLoreLine("")
                        .addLoreLine("§c► Click to restore default settings")
                        .toItemStack(), ctx -> {
                    new ConfirmInventory(PlayerIdentityRegistry.get(ctx.player), "§cRestore default settings", playerIdentity -> {
                        LevelModule levelModule = LevelModule.createDefault();
                        LevelModule.saveLevelModule(levelModule);
                        ModuleManager.getInstance().destroyModule(LevelModule.class);
                        ModuleManager.getInstance().registerModule(levelModule);

                        ctx.player.sendMessage("§cYou have restored the Level System to default. Server restart recommended.");
                        openSubMenu(ctx.player);
                    }, playerIdentity -> openSubMenu(playerIdentity.getOnlinePlayer())).openGUI();
                })

        ), p -> ModuleManager.getModule(SettingsModule.class).open(p));
    }


    private static void openRangeList(Player player) {
        SettingPageGUI.open(player, "Level Ranges",
                LevelModuleCategory::buildRangeListItems,
                LevelModuleCategory::openSubMenu);
    }

    private static List<SettingItem> buildRangeListItems() {
        LevelModule module = ModuleManager.getModule(LevelModule.class);
        List<SettingItem> items = new ArrayList<>();

        for (int i = 0; i < module.getLevelRanges().size(); i++) {
            LevelRange range = module.getLevelRanges().get(i);
            final int idx = i;

            ItemBuilder b = new ItemBuilder(Material.CLOCK);
            b.setName("§fLevels §e" + range.startLevel() + "§f–§e" + range.endLevel());
            b.removeLore();
            b.addLoreLine("");
            b.addLoreLine("§7Base XP: §a" + StringUtils.betterNumberFormat(range.baseXP()));
            b.addLoreLine("§7Scaling: §a" + scalingLabel(range.scaling()));
            b.addLoreLine("§7Range reward: " + rewardSummary(range.reward()));
            b.addLoreLine("");
            b.addLoreLine("§a► Click to edit");

            items.add(SettingItem.navigate(b.toItemStack(), ctx -> openRangeDetail(ctx.player, idx)));
        }

        return items;
    }

    private static void openRangeDetail(Player player, int rangeIdx) {
        SettingPageGUI.open(player, "Range " + (rangeIdx + 1),
                () -> buildRangeDetailItems(rangeIdx),
                LevelModuleCategory::openRangeList);
    }

    private static List<SettingItem> buildRangeDetailItems(int rangeIdx) {
        LevelModule module = ModuleManager.getModule(LevelModule.class);
        LevelRange range = module.getLevelRanges().get(rangeIdx);
        List<SettingItem> items = new ArrayList<>();

        ItemBuilder xpItem = new ItemBuilder(Material.EXPERIENCE_BOTTLE);
        xpItem.setName("§fBase XP: §a" + range.baseXP());
        xpItem.removeLore();
        xpItem.addLoreLine("§7XP needed for the first level in this range.");
        xpItem.addLoreLine("§7Higher levels cost more according to the scaling.");
        xpItem.addLoreLine("");
        xpItem.addLoreLine("§fLeft: §a+100 §8| §fRight: §c-100");
        xpItem.addLoreLine("§fShift+Left: §a+500 §8| §fShift+Right: §c-500");
        items.add(SettingItem.of(xpItem.toItemStack(), ctx -> {
            int delta = ctx.delta(+100, -100, +500, -500);
            LevelModule m = ModuleManager.getModule(LevelModule.class);
            LevelRange r = m.getLevelRanges().get(rangeIdx);
            m.getLevelRanges().set(rangeIdx, new LevelRange(
                    r.startLevel(), r.endLevel(),
                    Math.max(1, r.baseXP() + delta),
                    r.scaling(), r.reward()
            ));
            LevelModule.saveLevelModule(m);
        }));

        LevelRange.XPScaling[] scalings = LevelRange.XPScaling.values();
        int curScalingIdx = 0;
        for (int s = 0; s < scalings.length; s++) {
            if (scalings[s] == range.scaling()) { curScalingIdx = s; break; }
        }
        final int capturedScalingIdx = curScalingIdx;
        LevelRange.XPScaling current = range.scaling();

        ItemBuilder scalingItem = new ItemBuilder(Material.COMPARATOR);
        scalingItem.setName("§fXP Scaling: §a" + scalingLabel(current));
        scalingItem.removeLore();
        scalingItem.addLoreLine("§7How quickly XP cost grows inside this range.");
        scalingItem.addLoreLine("");
        scalingItem.addLoreLine(scalingLine(current, LevelRange.XPScaling.FLAT, "every level requires the same amount of XP"));
        scalingItem.addLoreLine(scalingLine(current, LevelRange.XPScaling.LINEAR, "+8% per level"));
        scalingItem.addLoreLine(scalingLine(current, LevelRange.XPScaling.MILD_EXPONENTIAL, "×1.08 per level"));
        scalingItem.addLoreLine(scalingLine(current, LevelRange.XPScaling.EXPONENTIAL, "×1.12 per level"));
        scalingItem.addLoreLine(scalingLine(current, LevelRange.XPScaling.AGGRESSIVE_EXPONENTIAL, "×1.18 per level"));
        scalingItem.addLoreLine(scalingLine(current, LevelRange.XPScaling.QUADRATIC, "grows as level² (slow then fast)"));
        scalingItem.addLoreLine("");
        scalingItem.addLoreLine("§fLeft: §anext §8| §fRight: §cprevious");
        items.add(SettingItem.of(scalingItem.toItemStack(), ctx -> {
            int dir = ctx.delta(+1, -1, +1, -1);
            LevelRange.XPScaling[] sc = LevelRange.XPScaling.values();
            int next = ((capturedScalingIdx + dir) % sc.length + sc.length) % sc.length;
            LevelModule m = ModuleManager.getModule(LevelModule.class);
            LevelRange r = m.getLevelRanges().get(rangeIdx);
            m.getLevelRanges().set(rangeIdx, new LevelRange(
                    r.startLevel(), r.endLevel(),
                    r.baseXP(), sc[next], r.reward()
            ));
            LevelModule.saveLevelModule(m);
        }));

        ItemBuilder rewardNav = new ItemBuilder(Material.GOLD_INGOT);
        rewardNav.setName("§fRange Reward");
        rewardNav.removeLore();
        rewardNav.addLoreLine("§7Reward on every level-up in this range.");
        rewardNav.addLoreLine("");
        rewardNav.addLoreLine("§7Current: " + rewardSummary(range.reward()));
        rewardNav.addLoreLine("");
        rewardNav.addLoreLine("§a► Click to edit rewards");
        items.add(SettingItem.navigate(rewardNav.toItemStack(),
                ctx -> openRangeRewardList(ctx.player, rangeIdx)));

        return items;
    }

    private static String scalingLine(LevelRange.XPScaling current, LevelRange.XPScaling type, String description) {
        String color = current == type ? "§a" : "§8";
        return color + type.name() + " §7– " + description;
    }

    private static void openRangeRewardList(Player player, int rangeIdx) {
        SettingPageGUI.open(player, "Range " + (rangeIdx + 1) + " Rewards",
                () -> buildRewardNavItems(
                        ModuleManager.getModule(LevelModule.class)
                                .getLevelRanges().get(rangeIdx).reward().getRewardItems(),
                        (rewardIdx, p) -> openRangeRewardItemDetail(p, rangeIdx, rewardIdx)
                ),
                p -> openRangeDetail(p, rangeIdx));
    }

    private static void openRangeRewardItemDetail(Player player, int rangeIdx, int rewardIdx) {
        SettingPageGUI.open(player, "Reward Item",
                () -> buildRewardItemDetailItems(
                        () -> ModuleManager.getModule(LevelModule.class)
                                .getLevelRanges().get(rangeIdx).reward().getRewardItems().get(rewardIdx),
                        ri -> LevelModule.saveLevelModule(ModuleManager.getModule(LevelModule.class))
                ),
                p -> openRangeRewardList(p, rangeIdx));
    }

    private static void openRewardEditor(Player player) {
        SettingPageGUI.open(player, "Level Rewards",
                LevelModuleCategory::buildRewardItems,
                LevelModuleCategory::openSubMenu);
    }

    private static List<SettingItem> buildRewardItems() {
        LevelModule module = ModuleManager.getModule(LevelModule.class);
        List<SettingItem> items = new ArrayList<>();
        List<LevelReward> rewards = module.getLevelRewards();

        for (int i = 0; i < rewards.size(); i++) {
            LevelReward levelReward = rewards.get(i);
            final int index = i;

            StringBuilder levelsDesc = new StringBuilder("§8Levels: ");
            for (int lvl : levelReward.level()) levelsDesc.append(lvl).append(" ");

            ItemBuilder b = new ItemBuilder(Material.GOLD_NUGGET);
            b.setName("§fReward Group " + (i + 1));
            b.removeLore();
            b.addLoreLine(levelsDesc.toString());
            b.addLoreLine("");
            b.addLoreLine("§7Rewards:");
            if (!levelReward.reward().getRewardItems().isEmpty()) {
                for (RewardItem ri : levelReward.reward().getRewardItems()) {
                    b.addLoreLine(rewardItemLine(ri));
                }
            } else {
                b.addLoreLine(" §cNone");
            }
            b.addLoreLine("");
            b.addLoreLine("§a► Click to edit rewards");

            items.add(SettingItem.navigate(b.toItemStack(),
                    ctx -> openLevelRewardGroupEditor(ctx.player, index)));
        }

        return items;
    }

    private static void openLevelRewardGroupEditor(Player player, int groupIdx) {
        SettingPageGUI.open(player, "Reward Group " + (groupIdx + 1),
                () -> buildRewardNavItems(
                        ModuleManager.getModule(LevelModule.class)
                                .getLevelRewards().get(groupIdx).reward().getRewardItems(),
                        (rewardIdx, p) -> openLevelRewardItemDetail(p, groupIdx, rewardIdx)
                ),
                LevelModuleCategory::openRewardEditor);
    }

    private static void openLevelRewardItemDetail(Player player, int groupIdx, int rewardIdx) {
        SettingPageGUI.open(player, "Reward Item",
                () -> buildRewardItemDetailItems(
                        () -> ModuleManager.getModule(LevelModule.class)
                                .getLevelRewards().get(groupIdx).reward().getRewardItems().get(rewardIdx),
                        ri -> LevelModule.saveLevelModule(ModuleManager.getModule(LevelModule.class))
                ),
                p -> openLevelRewardGroupEditor(p, groupIdx));
    }


    @FunctionalInterface
    interface RewardOpenDetail { void open(int rewardIdx, Player player); }

    private static List<SettingItem> buildRewardNavItems(List<RewardItem> rewardItems,
                                                          RewardOpenDetail openDetail) {
        List<SettingItem> items = new ArrayList<>();
        for (int i = 0; i < rewardItems.size(); i++) {
            RewardItem ri = rewardItems.get(i);
            final int idx = i;
            Resource resource = ri.getResource();

            ItemBuilder b = new ItemBuilder(Material.GOLD_NUGGET);
            b.setName("§fReward: " + resource.getColor() + resource.getDisplayName());
            b.removeLore();
            b.addLoreLine("");
            b.addLoreLine("§7Amount: §a" + (ri.hasRandomAmount()
                    ? StringUtils.betterNumberFormat(ri.getRandomMinRange()) + "–" + StringUtils.betterNumberFormat(ri.getRandomMaxRange())
                    : StringUtils.betterNumberFormat((ri.getAmount()))));
            b.addLoreLine("§7Chance: §a" + ri.getChance() + "%");
            b.addLoreLine("");
            b.addLoreLine("§a► Click to edit");

            items.add(SettingItem.navigate(b.toItemStack(), ctx -> openDetail.open(idx, ctx.player)));
        }
        return items;
    }

    private static List<SettingItem> buildRewardItemDetailItems(Supplier<RewardItem> riGetter,
                                                                  Consumer<RewardItem> saver) {
        RewardItem ri = riGetter.get();
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
                RewardItem r = riGetter.get();
                r.setRandomMinRange(Math.max(0, r.getRandomMinRange() + delta));
                if (r.getRandomMaxRange() < r.getRandomMinRange())
                    r.setRandomMaxRange(r.getRandomMinRange());
                saver.accept(r);
            }));

            ItemBuilder maxItem = new ItemBuilder(Material.LIME_DYE);
            maxItem.setName("§fMax Amount: " + resource.getColor() + StringUtils.betterNumberFormat(ri.getRandomMaxRange()) + " " + resource.getDisplayName());
            maxItem.removeLore();
            maxItem.addLoreLine("");
            maxItem.addLoreLine("§fLeft: §a+1 §8| §fRight: §c-1");
            maxItem.addLoreLine("§fShift+Left: §a+10 §8| §fShift+Right: §c-10");
            items.add(SettingItem.of(maxItem.toItemStack(), ctx -> {
                int delta = ctx.delta(+1, -1, +10, -10);
                RewardItem r = riGetter.get();
                r.setRandomMaxRange(Math.max(r.getRandomMinRange(), r.getRandomMaxRange() + delta));
                saver.accept(r);
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
                RewardItem r = riGetter.get();
                r.setAmount(Math.max(1, r.getAmount() + delta));
                saver.accept(r);
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
            RewardItem r = riGetter.get();
            r.setChance(Math.max(1, Math.min(100, r.getChance() + delta)));
            saver.accept(r);
        }));

        return items;
    }

    private static String rewardSummary(Reward reward) {
        if (reward == null || reward.getRewardItems().isEmpty()) return "§8none";
        return reward.getRewardItems().stream()
                .map(LevelModuleCategory::rewardItemLine)
                .reduce((a, b) -> a + "§8, " + b)
                .orElse("§8none");
    }

    private static String rewardItemLine(RewardItem ri) {
        Resource resource = ri.getResource();
        String amount = ri.hasRandomAmount()
                ? StringUtils.betterNumberFormat(ri.getRandomMinRange()) + "–" + StringUtils.betterNumberFormat(ri.getRandomMaxRange())
                : StringUtils.betterNumberFormat(ri.getAmount());
        String chance = ri.getChance() < 100 ? " §7(" + ri.getChance() + "% chance)" : "";
        return " " + resource.getColor() + "+ " + amount + " " + resource.getDisplayName() + chance;
    }

    private static String scalingLabel(LevelRange.XPScaling scaling) {
        return switch (scaling) {
            case FLAT -> "Flat";
            case LINEAR -> "Linear (+8%/lvl)";
            case MILD_EXPONENTIAL -> "Mild Exp (×1.08)";
            case EXPONENTIAL -> "Exponential (×1.12)";
            case AGGRESSIVE_EXPONENTIAL -> "Aggressive Exp (×1.18)";
            case QUADRATIC -> "Quadratic (lvl²)";
        };
    }
}