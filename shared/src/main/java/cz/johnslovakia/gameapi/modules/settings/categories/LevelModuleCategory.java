package cz.johnslovakia.gameapi.modules.settings.categories;

import cz.johnslovakia.gameapi.guis.ConfirmInventory;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticPrices;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.modules.levels.LevelRange;
import cz.johnslovakia.gameapi.modules.levels.LevelReward;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.settings.*;
import cz.johnslovakia.gameapi.rewards.Reward;
import cz.johnslovakia.gameapi.rewards.RewardItem;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
        List<BottomAction> actions = List.of(
                new BottomAction(8, new ItemBuilder(Material.BARRIER)
                        .setName("§cRestore default settings")
                        .removeLore()
                        .addLoreLine("")
                        .addLoreLine("§c► Click to restore default settings")
                        .toItemStack(), p -> {
                    new ConfirmInventory(PlayerIdentityRegistry.get(p), "§cRestore default settings", playerIdentity -> {
                        SettingsEditSession.runAction(p, () -> {
                            LevelModule levelModule = LevelModule.createDefault();
                            LevelModule.saveLevelModule(levelModule);
                            ModuleManager.getInstance().destroyModule(LevelModule.class);
                            ModuleManager.getInstance().registerModule(levelModule);
                        });

                        p.sendMessage("§cLevel system settings were reset to default. Changes may not apply until the server is restarted.");
                        openSubMenu(p);
                    }, playerIdentity -> openSubMenu(playerIdentity.getOnlinePlayer())).openGUI();
                })
        );

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
                        )
                ), p -> ModuleManager.getModule(SettingsModule.class).open(p),
                actions);
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
            b.addLoreLine("");
            addRewardLore(b, range.reward());
            b.addLoreLine("");
            b.addLoreLine("§a► Click to edit");

            items.add(SettingItem.navigate(b.toItemStack(), ctx -> openRangeDetail(ctx.player, idx)));
        }

        return items;
    }

    private static void openRangeDetail(Player player, int rangeIdx) {
        if (!hasRange(rangeIdx)) {
            openRangeList(player);
            return;
        }
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
        addRewardLore(rewardNav, range.reward());
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
        if (!hasRange(rangeIdx)) {
            openRangeList(player);
            return;
        }
        SettingPageGUI.open(player, "Range " + (rangeIdx + 1) + " Rewards",
                () -> RewardSettingsHelper.buildRewardNavItems(
                        ModuleManager.getModule(LevelModule.class)
                                .getLevelRanges().get(rangeIdx).reward().getRewardItems(),
                        (rewardIdx, p) -> openRangeRewardItemDetail(p, rangeIdx, rewardIdx),
                        (rewardIdx, p) -> removeRangeRewardItem(rangeIdx, rewardIdx),
                        p -> openRangeRewardList(p, rangeIdx)
                ),
                p -> openRangeDetail(p, rangeIdx),
                List.of(new BottomAction(4, RewardSettingsHelper.addResourceIcon(),
                        p -> openRangeRewardResourcePicker(p, rangeIdx))));
    }

    private static void openRangeRewardResourcePicker(Player player, int rangeIdx) {
        if (!hasRange(rangeIdx)) {
            openRangeList(player);
            return;
        }
        RewardSettingsHelper.openResourcePicker(player,
                "Add Range Reward",
                () -> ModuleManager.getModule(LevelModule.class)
                        .getLevelRanges().get(rangeIdx).reward().getRewardItems(),
                resource -> {
                    LevelModule module = ModuleManager.getModule(LevelModule.class);
                    LevelRange range = module.getLevelRanges().get(rangeIdx);
                    range.reward().addRewardItem(new RewardItem(resource, 10));
                    LevelModule.saveLevelModule(module);
                },
                p -> openRangeRewardList(p, rangeIdx));
    }

    private static void openRangeRewardItemDetail(Player player, int rangeIdx, int rewardIdx) {
        if (!hasRange(rangeIdx) || !hasRangeRewardItem(rangeIdx, rewardIdx)) {
            openRangeRewardList(player, rangeIdx);
            return;
        }
        SettingPageGUI.open(player, "Reward Item",
                () -> RewardSettingsHelper.buildRewardItemDetailItems(
                        () -> ModuleManager.getModule(LevelModule.class)
                                .getLevelRanges().get(rangeIdx).reward().getRewardItems().get(rewardIdx),
                        ri -> LevelModule.saveLevelModule(ModuleManager.getModule(LevelModule.class)),
                        p -> removeRangeRewardItem(rangeIdx, rewardIdx),
                        p -> openRangeRewardList(p, rangeIdx)
                ),
                p -> openRangeRewardList(p, rangeIdx));
    }

    private static void removeRangeRewardItem(int rangeIdx, int rewardIdx) {
        if (!hasRange(rangeIdx)) {
            return;
        }
        LevelModule module = ModuleManager.getModule(LevelModule.class);
        List<RewardItem> rewardItems = module.getLevelRanges().get(rangeIdx).reward().getRewardItems();
        if (rewardIdx >= 0 && rewardIdx < rewardItems.size()) {
            rewardItems.remove(rewardIdx);
            LevelModule.saveLevelModule(module);
        }
    }

    private static void openRewardEditor(Player player) {
        SettingPageGUI.open(player, "Level Rewards",
                LevelModuleCategory::buildRewardItems,
                LevelModuleCategory::openSubMenu,
                List.of(new BottomAction(4, addLevelRewardIcon(), LevelModuleCategory::addLevelReward)));
    }

    private static List<SettingItem> buildRewardItems() {
        LevelModule module = ModuleManager.getModule(LevelModule.class);
        List<SettingItem> items = new ArrayList<>();
        List<LevelReward> rewards = module.getLevelRewards();

        if (rewards.isEmpty()) {
            items.add(SettingItem.display(new ItemBuilder(Material.BARRIER)
                    .setName("§cNo level rewards configured")
                    .removeLore()
                    .addLoreLine("§7Use the Add Level Reward button below.")
                    .toItemStack()));
            return items;
        }

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
            b.addLoreLine("§c► Shift-click to remove reward group");

            items.add(SettingItem.navigate(b.toItemStack(),
                    ctx -> {
                        if (ctx.clickInfo.getClickType().isShiftClick()) {
                            removeLevelRewardGroup(ctx.player, index);
                            SettingsEditSession.afterCurrentAction(() -> openRewardEditor(ctx.player));
                            return;
                        }
                        openLevelRewardGroupEditor(ctx.player, index);
                    }));
        }

        return items;
    }

    private static void openLevelRewardGroupEditor(Player player, int groupIdx) {
        if (!hasLevelRewardGroup(groupIdx)) {
            openRewardEditor(player);
            return;
        }
        SettingPageGUI.open(player, "Reward Group " + (groupIdx + 1),
                () -> buildLevelRewardGroupItems(groupIdx),
                LevelModuleCategory::openRewardEditor,
                List.of(new BottomAction(4, RewardSettingsHelper.addResourceIcon(),
                        p -> openLevelRewardResourcePicker(p, groupIdx))));
    }

    private static List<SettingItem> buildLevelRewardGroupItems(int groupIdx) {
        LevelModule module = ModuleManager.getModule(LevelModule.class);
        LevelReward levelReward = module.getLevelRewards().get(groupIdx);
        List<SettingItem> items = new ArrayList<>();

        ItemBuilder levelsItem = new ItemBuilder(Material.EXPERIENCE_BOTTLE);
        levelsItem.setName("§fReward Levels");
        levelsItem.removeLore();
        levelsItem.addLoreLine(levelsDescription(levelReward.level()));
        levelsItem.addLoreLine("");
        levelsItem.addLoreLine("§a► Click to edit target levels");
        items.add(SettingItem.navigate(levelsItem.toItemStack(),
                ctx -> openLevelRewardLevelsEditor(ctx.player, groupIdx)));

        List<RewardItem> rewardItems = levelReward.reward().getRewardItems();
        if (rewardItems.isEmpty()) {
            items.add(SettingItem.display(new ItemBuilder(Material.BARRIER)
                    .setName("§cNo rewards configured")
                    .removeLore()
                    .addLoreLine("§7Use the Add Resource button below.")
                    .toItemStack()));
        } else {
            items.addAll(RewardSettingsHelper.buildRewardNavItems(rewardItems,
                    (rewardIdx, p) -> openLevelRewardItemDetail(p, groupIdx, rewardIdx),
                    (rewardIdx, p) -> removeLevelRewardItem(groupIdx, rewardIdx),
                    p -> openLevelRewardGroupEditor(p, groupIdx)));
        }

        return items;
    }

    private static void openLevelRewardResourcePicker(Player player, int groupIdx) {
        if (!hasLevelRewardGroup(groupIdx)) {
            openRewardEditor(player);
            return;
        }
        RewardSettingsHelper.openResourcePicker(player,
                "Add Level Reward",
                () -> ModuleManager.getModule(LevelModule.class)
                        .getLevelRewards().get(groupIdx).reward().getRewardItems(),
                resource -> {
                    LevelModule module = ModuleManager.getModule(LevelModule.class);
                    module.getLevelRewards().get(groupIdx).reward().addRewardItem(new RewardItem(resource, 10));
                    LevelModule.saveLevelModule(module);
                },
                p -> openLevelRewardGroupEditor(p, groupIdx));
    }

    private static void openLevelRewardLevelsEditor(Player player, int groupIdx) {
        if (!hasLevelRewardGroup(groupIdx)) {
            openRewardEditor(player);
            return;
        }
        SettingPageGUI.open(player, "Reward Group Levels",
                () -> buildLevelRewardLevelItems(groupIdx),
                p -> openLevelRewardGroupEditor(p, groupIdx),
                List.of(new BottomAction(4, addLevelTargetIcon(),
                        p -> addLevelTargetToRewardGroup(p, groupIdx))));
    }

    private static List<SettingItem> buildLevelRewardLevelItems(int groupIdx) {
        LevelModule module = ModuleManager.getModule(LevelModule.class);
        LevelReward levelReward = module.getLevelRewards().get(groupIdx);
        List<SettingItem> items = new ArrayList<>();

        if (levelReward.level().length == 0) {
            items.add(SettingItem.display(new ItemBuilder(Material.BARRIER)
                    .setName("§cNo target levels")
                    .removeLore()
                    .addLoreLine("§7Use the Add Level button below.")
                    .toItemStack()));
            return items;
        }

        for (int i = 0; i < levelReward.level().length; i++) {
            int level = levelReward.level()[i];
            final int levelIdx = i;

            ItemBuilder b = new ItemBuilder(Material.EXPERIENCE_BOTTLE);
            b.setName("§fLevel: §a" + level);
            b.removeLore();
            b.addLoreLine("§7This reward group is granted on this level.");
            b.addLoreLine("");
            b.addLoreLine("§fLeft: §a+1 §8| §fRight: §c-1");
            b.addLoreLine("§fShift+Left: §a+5 §8| §fShift+Right: §c-5");
            items.add(SettingItem.of(b.toItemStack(), ctx -> {
                int delta = ctx.delta(+1, -1, +5, -5);
                adjustLevelRewardLevel(ctx.player, groupIdx, levelIdx, delta);
            }));
        }

        return items;
    }

    private static void addLevelReward(Player player) {
        LevelModule module = ModuleManager.getModule(LevelModule.class);
        int level = findFirstUnusedRewardLevel(module);
        if (level < 0) {
            player.sendMessage("§cAll configured levels already have a level reward group.");
            player.playSound(player, Sound.BLOCK_ANVIL_LAND, 0.6F, 0.8F);
            openRewardEditor(player);
            return;
        }

        module.getLevelRewards().add(new LevelReward(new Reward(), level));
        LevelModule.saveLevelModule(module);

        int groupIdx = module.getLevelRewards().size() - 1;
        player.sendMessage("§aCreated level reward group for level §f" + level + "§a.");
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 1.2F);
        SettingsEditSession.afterCurrentAction(() -> openLevelRewardGroupEditor(player, groupIdx));
    }

    private static void addLevelTargetToRewardGroup(Player player, int groupIdx) {
        if (!hasLevelRewardGroup(groupIdx)) {
            openRewardEditor(player);
            return;
        }
        LevelModule module = ModuleManager.getModule(LevelModule.class);
        int level = findFirstUnusedRewardLevel(module);
        if (level < 0) {
            player.sendMessage("§cNo free level target is available.");
            player.playSound(player, Sound.BLOCK_ANVIL_LAND, 0.6F, 0.8F);
            openLevelRewardLevelsEditor(player, groupIdx);
            return;
        }

        LevelReward levelReward = module.getLevelRewards().get(groupIdx);
        int[] levels = Arrays.copyOf(levelReward.level(), levelReward.level().length + 1);
        levels[levels.length - 1] = level;
        setLevelRewardLevels(module, groupIdx, levels);

        player.sendMessage("§aAdded level §f" + level + "§a to this reward group.");
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 1.2F);
        SettingsEditSession.afterCurrentAction(() -> openLevelRewardLevelsEditor(player, groupIdx));
    }

    private static void adjustLevelRewardLevel(Player player, int groupIdx, int levelIdx, int delta) {
        if (!hasLevelRewardGroup(groupIdx)) {
            return;
        }
        LevelModule module = ModuleManager.getModule(LevelModule.class);
        LevelReward levelReward = module.getLevelRewards().get(groupIdx);
        if (levelIdx < 0 || levelIdx >= levelReward.level().length) {
            return;
        }
        int[] levels = levelReward.level().clone();
        int current = levels[levelIdx];
        int requested = clamp(current + delta, minRewardLevel(module), maxRewardLevel(module));
        if (requested == current) {
            return;
        }
        if (isRewardLevelUsed(module, groupIdx, levelIdx, requested)) {
            player.sendMessage("§cThat level already has a reward target.");
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.8F);
            return;
        }
        levels[levelIdx] = requested;
        setLevelRewardLevels(module, groupIdx, levels);
    }

    private static void setLevelRewardLevels(LevelModule module, int groupIdx, int[] levels) {
        if (groupIdx < 0 || groupIdx >= module.getLevelRewards().size()) {
            return;
        }
        LevelReward current = module.getLevelRewards().get(groupIdx);
        int[] normalized = normalizeRewardLevels(module, levels);
        if (normalized.length == 0) {
            int fallbackLevel = findAvailableRewardLevel(module, groupIdx, minRewardLevel(module), +1);
            normalized = new int[]{fallbackLevel};
        }
        module.getLevelRewards().set(groupIdx, new LevelReward(current.reward(), normalized));
        LevelModule.saveLevelModule(module);
    }

    private static int[] normalizeRewardLevels(LevelModule module, int[] levels) {
        Set<Integer> normalized = new LinkedHashSet<>();
        Arrays.stream(levels)
                .map(level -> clamp(level, minRewardLevel(module), maxRewardLevel(module)))
                .forEach(normalized::add);
        return normalized.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int findFirstUnusedRewardLevel(LevelModule module) {
        Set<Integer> used = usedRewardLevels(module, -1);
        for (int level = minRewardLevel(module); level <= maxRewardLevel(module); level++) {
            if (!used.contains(level)) {
                return level;
            }
        }
        return -1;
    }

    private static int findAvailableRewardLevel(LevelModule module, int groupIdx, int requested, int direction) {
        int min = minRewardLevel(module);
        int max = maxRewardLevel(module);
        int clamped = clamp(requested, min, max);
        Set<Integer> used = usedRewardLevels(module, groupIdx);
        if (!used.contains(clamped)) {
            return clamped;
        }

        int step = direction >= 0 ? 1 : -1;
        for (int level = clamped; level >= min && level <= max; level += step) {
            if (!used.contains(level)) {
                return level;
            }
        }

        for (int level = min; level <= max; level++) {
            if (!used.contains(level)) {
                return level;
            }
        }
        return clamped;
    }

    private static Set<Integer> usedRewardLevels(LevelModule module, int ignoredGroupIdx) {
        Set<Integer> used = new LinkedHashSet<>();
        List<LevelReward> levelRewards = module.getLevelRewards();
        for (int i = 0; i < levelRewards.size(); i++) {
            if (i == ignoredGroupIdx) {
                continue;
            }
            for (int level : levelRewards.get(i).level()) {
                used.add(level);
            }
        }
        return used;
    }

    private static boolean isRewardLevelUsed(LevelModule module, int editedGroupIdx, int editedLevelIdx, int level) {
        List<LevelReward> levelRewards = module.getLevelRewards();
        for (int groupIdx = 0; groupIdx < levelRewards.size(); groupIdx++) {
            int[] levels = levelRewards.get(groupIdx).level();
            for (int levelIdx = 0; levelIdx < levels.length; levelIdx++) {
                if (groupIdx == editedGroupIdx && levelIdx == editedLevelIdx) {
                    continue;
                }
                if (levels[levelIdx] == level) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int minRewardLevel(LevelModule module) {
        if (module.getLevelRanges().isEmpty()) {
            return 1;
        }
        return Math.max(1, module.getLevelRanges().getFirst().startLevel());
    }

    private static int maxRewardLevel(LevelModule module) {
        if (module.getLevelRanges().isEmpty()) {
            return 100;
        }
        return Math.max(minRewardLevel(module), module.getLevelRanges().getLast().endLevel());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String levelsDescription(int[] levels) {
        if (levels.length == 0) {
            return "§8Levels: none";
        }
        StringBuilder levelsDesc = new StringBuilder("§8Levels: ");
        for (int lvl : levels) levelsDesc.append(lvl).append(" ");
        return levelsDesc.toString();
    }

    private static org.bukkit.inventory.ItemStack addLevelRewardIcon() {
        return new ItemBuilder(Material.EMERALD)
                .setName("§aAdd Level Reward")
                .removeLore()
                .addLoreLine("§7Creates a new configurable level reward group.")
                .toItemStack();
    }

    private static org.bukkit.inventory.ItemStack addLevelTargetIcon() {
        return new ItemBuilder(Material.EMERALD)
                .setName("§aAdd Level")
                .removeLore()
                .addLoreLine("§7Adds another level target to this reward group.")
                .toItemStack();
    }

    private static void removeLevelRewardGroup(Player player, int groupIdx) {
        LevelModule module = ModuleManager.getModule(LevelModule.class);
        if (groupIdx >= 0 && groupIdx < module.getLevelRewards().size()) {
            module.getLevelRewards().remove(groupIdx);
            LevelModule.saveLevelModule(module);
            player.sendMessage("§cLevel reward group removed.");
            player.playSound(player, Sound.BLOCK_ANVIL_BREAK, 0.8F, 1F);
        }
    }

    private static void openLevelRewardItemDetail(Player player, int groupIdx, int rewardIdx) {
        if (!hasLevelRewardGroup(groupIdx) || !hasLevelRewardItem(groupIdx, rewardIdx)) {
            openLevelRewardGroupEditor(player, groupIdx);
            return;
        }
        SettingPageGUI.open(player, "Reward Item",
                () -> RewardSettingsHelper.buildRewardItemDetailItems(
                        () -> ModuleManager.getModule(LevelModule.class)
                                .getLevelRewards().get(groupIdx).reward().getRewardItems().get(rewardIdx),
                        ri -> LevelModule.saveLevelModule(ModuleManager.getModule(LevelModule.class)),
                        p -> removeLevelRewardItem(groupIdx, rewardIdx),
                        p -> openLevelRewardGroupEditor(p, groupIdx)
                ),
                p -> openLevelRewardGroupEditor(p, groupIdx));
    }

    private static void removeLevelRewardItem(int groupIdx, int rewardIdx) {
        if (!hasLevelRewardGroup(groupIdx)) {
            return;
        }
        LevelModule module = ModuleManager.getModule(LevelModule.class);
        List<RewardItem> rewardItems = module.getLevelRewards().get(groupIdx).reward().getRewardItems();
        if (rewardIdx >= 0 && rewardIdx < rewardItems.size()) {
            rewardItems.remove(rewardIdx);
            LevelModule.saveLevelModule(module);
        }
    }

    private static boolean hasRange(int rangeIdx) {
        return rangeIdx >= 0 && rangeIdx < ModuleManager.getModule(LevelModule.class).getLevelRanges().size();
    }

    private static boolean hasRangeRewardItem(int rangeIdx, int rewardIdx) {
        if (!hasRange(rangeIdx)) {
            return false;
        }
        List<RewardItem> rewardItems = ModuleManager.getModule(LevelModule.class)
                .getLevelRanges().get(rangeIdx).reward().getRewardItems();
        return rewardIdx >= 0 && rewardIdx < rewardItems.size();
    }

    private static boolean hasLevelRewardGroup(int groupIdx) {
        return groupIdx >= 0 && groupIdx < ModuleManager.getModule(LevelModule.class).getLevelRewards().size();
    }

    private static boolean hasLevelRewardItem(int groupIdx, int rewardIdx) {
        if (!hasLevelRewardGroup(groupIdx)) {
            return false;
        }
        List<RewardItem> rewardItems = ModuleManager.getModule(LevelModule.class)
                .getLevelRewards().get(groupIdx).reward().getRewardItems();
        return rewardIdx >= 0 && rewardIdx < rewardItems.size();
    }


    @FunctionalInterface
    interface RewardOpenDetail { void open(int rewardIdx, Player player); }

    private static List<SettingItem> buildRewardNavItems(List<RewardItem> rewardItems, RewardOpenDetail openDetail) {
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

    private static void addRewardLore(ItemBuilder builder, Reward reward) {
        builder.addLoreLine("§7Rewards:");
        if (reward != null && !reward.getRewardItems().isEmpty()) {
            for (RewardItem ri : reward.getRewardItems()) {
                builder.addLoreLine(rewardItemLine(ri));
            }
        } else {
            builder.addLoreLine(" §cNone");
        }
    }

    private static String rewardItemLine(RewardItem ri) {
        return RewardSettingsHelper.rewardItemLine(ri);
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
