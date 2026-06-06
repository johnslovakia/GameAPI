package cz.johnslovakia.gameapi.modules.settings.categories;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.modules.settings.SettingItem;
import cz.johnslovakia.gameapi.modules.settings.SettingPageGUI;
import cz.johnslovakia.gameapi.modules.settings.SettingsEditSession;
import cz.johnslovakia.gameapi.rewards.RewardItem;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class RewardSettingsHelper {

    private RewardSettingsHelper() {}

    @FunctionalInterface
    interface RewardOpenDetail {
        void open(int rewardIdx, Player player);
    }

    record ResourceAmountReward(String resourceName, int amount) {}

    static List<SettingItem> buildRewardNavItems(List<RewardItem> rewardItems, RewardOpenDetail openDetail) {
        return buildRewardNavItems(rewardItems, openDetail, null, null);
    }

    static List<SettingItem> buildRewardNavItems(List<RewardItem> rewardItems, RewardOpenDetail openDetail, RewardOpenDetail removeAction, Consumer<Player> back) {
        List<SettingItem> items = new ArrayList<>();
        for (int i = 0; i < rewardItems.size(); i++) {
            RewardItem ri = rewardItems.get(i);
            final int idx = i;
            ResourceDisplay resource = display(ri);

            ItemBuilder b = new ItemBuilder(resourceMaterial(resource.name()));
            b.setName("§fReward: " + resource.color() + resource.displayName());
            b.removeLore();
            b.addLoreLine("");
            b.addLoreLine("§7Amount: §a" + amountLabel(ri));
            b.addLoreLine("§7Chance: §a" + ri.getChance() + "%");
            b.addLoreLine("");
            b.addLoreLine("§a► Click to edit");
            if (removeAction != null) {
                b.addLoreLine("§c► Shift-click to remove");
            }

            items.add(SettingItem.navigate(b.toItemStack(), ctx -> {
                if (ctx.clickInfo.getClickType().isShiftClick() && removeAction != null) {
                    removeAction.open(idx, ctx.player);
                    ctx.player.sendMessage(StringUtils.colorizer("§cRemoved #df1c1c" + resource.displayName() + "§c reward."));
                    ctx.player.playSound(ctx.player, Sound.BLOCK_ANVIL_BREAK, 0.8F, 1F);
                    if (back != null) {
                        SettingsEditSession.afterCurrentAction(() -> back.accept(ctx.player));
                    }
                    return;
                }
                openDetail.open(idx, ctx.player);
            }));
        }
        return items;
    }

    static List<SettingItem> buildRewardItemDetailItems(Supplier<RewardItem> riGetter, Consumer<RewardItem> saver) {
        return buildRewardItemDetailItems(riGetter, saver, null, null);
    }

    static List<SettingItem> buildRewardItemDetailItems(Supplier<RewardItem> riGetter, Consumer<RewardItem> saver, Consumer<Player> remover, Consumer<Player> back) {
        RewardItem ri = riGetter.get();
        List<SettingItem> items = new ArrayList<>();
        if (ri == null) {
            items.add(SettingItem.display(new ItemBuilder(Material.BARRIER)
                    .setName("§cReward item not found")
                    .removeLore()
                    .toItemStack()));
            return items;
        }

        boolean isRandom = ri.hasRandomAmount();
        ResourceDisplay resource = display(ri);

        ItemBuilder amountTypeItem = new ItemBuilder(isRandom ? Material.COMPARATOR : Material.REPEATER);
        amountTypeItem.setName("§fAmount Type: §a" + (isRandom ? "Random" : "Fixed"));
        amountTypeItem.removeLore();
        amountTypeItem.addLoreLine("");
        amountTypeItem.addLoreLine("§a► Click to switch");
        items.add(SettingItem.of(amountTypeItem.toItemStack(), ctx -> {
            RewardItem r = riGetter.get();
            if (r.hasRandomAmount()) {
                r.setAmount(Math.max(1, r.getRandomMinRange()));
                r.setRandomMinRange(0);
                r.setRandomMaxRange(0);
            } else {
                int amount = Math.max(1, r.getAmount());
                r.setRandomMinRange(amount);
                r.setRandomMaxRange(amount);
            }
            saver.accept(r);
        }));

        if (isRandom) {
            ItemBuilder minItem = new ItemBuilder(Material.GREEN_DYE);
            minItem.setName("§fMin Amount: " + resource.color()
                    + StringUtils.betterNumberFormat(ri.getRandomMinRange()) + " " + resource.displayName());
            minItem.removeLore();
            minItem.addLoreLine("");
            minItem.addLoreLine("§fLeft: §a+1 §8| §fRight: §c-1");
            minItem.addLoreLine("§fShift+Left: §a+10 §8| §fShift+Right: §c-10");
            items.add(SettingItem.of(minItem.toItemStack(), ctx -> {
                int delta = ctx.delta(+1, -1, +10, -10);
                RewardItem r = riGetter.get();
                r.setRandomMinRange(Math.max(0, r.getRandomMinRange() + delta));
                if (r.getRandomMaxRange() < r.getRandomMinRange()) {
                    r.setRandomMaxRange(r.getRandomMinRange());
                }
                saver.accept(r);
            }));

            ItemBuilder maxItem = new ItemBuilder(Material.LIME_DYE);
            maxItem.setName("§fMax Amount: " + resource.color()
                    + StringUtils.betterNumberFormat(ri.getRandomMaxRange()) + " " + resource.displayName());
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
            amountItem.setName("§fAmount: " + resource.color()
                    + StringUtils.betterNumberFormat(ri.getAmount()) + " " + resource.displayName());
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
            r.setChance(Math.clamp(r.getChance() + delta, 1, 100));
            saver.accept(r);
        }));

        return items;
    }

    static List<SettingItem> buildResourceAmountRewardNavItems(List<ResourceAmountReward> rewardItems, RewardOpenDetail openDetail) {
        return buildResourceAmountRewardNavItems(rewardItems, openDetail, null, null);
    }

    static List<SettingItem> buildResourceAmountRewardNavItems(List<ResourceAmountReward> rewardItems, RewardOpenDetail openDetail, RewardOpenDetail removeAction, Consumer<Player> back) {
        List<SettingItem> items = new ArrayList<>();
        for (int i = 0; i < rewardItems.size(); i++) {
            ResourceAmountReward reward = rewardItems.get(i);
            final int idx = i;
            ResourceDisplay resource = display(reward.resourceName());

            ItemBuilder b = new ItemBuilder(resourceMaterial(resource.name()));
            b.setName("§fReward: " + resource.color() + resource.displayName());
            b.removeLore();
            b.addLoreLine("");
            b.addLoreLine("§7Amount: §a" + StringUtils.betterNumberFormat(reward.amount()));
            b.addLoreLine("");
            b.addLoreLine("§a► Click to edit");
            if (removeAction != null) {
                b.addLoreLine("§c► Shift-click to remove");
            }

            items.add(SettingItem.navigate(b.toItemStack(), ctx -> {
                if (ctx.clickInfo.getClickType().isShiftClick() && removeAction != null) {
                    removeAction.open(idx, ctx.player);
                    ctx.player.sendMessage(StringUtils.colorizer("§cRemoved #df1c1c" + resource.displayName() + "§c reward."));
                    ctx.player.playSound(ctx.player, Sound.BLOCK_ANVIL_BREAK, 0.8F, 1F);
                    if (back != null) {
                        SettingsEditSession.afterCurrentAction(() -> back.accept(ctx.player));
                    }
                    return;
                }
                openDetail.open(idx, ctx.player);
            }));
        }
        return items;
    }

    static List<SettingItem> buildResourceAmountRewardDetailItems(Supplier<ResourceAmountReward> rewardGetter, IntConsumer amountSetter, Consumer<Player> remover, Consumer<Player> back) {
        ResourceAmountReward reward = rewardGetter.get();
        List<SettingItem> items = new ArrayList<>();
        if (reward == null) {
            items.add(SettingItem.display(new ItemBuilder(Material.BARRIER)
                    .setName("§cReward item not found")
                    .removeLore()
                    .toItemStack()));
            return items;
        }

        ResourceDisplay resource = display(reward.resourceName());

        ItemBuilder amountItem = new ItemBuilder(Material.LIME_DYE);
        amountItem.setName("§fAmount: " + resource.color()
                + StringUtils.betterNumberFormat(reward.amount()) + " " + resource.displayName());
        amountItem.removeLore();
        amountItem.addLoreLine("");
        amountItem.addLoreLine("§fLeft: §a+1 §8| §fRight: §c-1");
        amountItem.addLoreLine("§fShift+Left: §a+5 §8| §fShift+Right: §c-5");
        items.add(SettingItem.of(amountItem.toItemStack(), ctx -> {
            ResourceAmountReward current = rewardGetter.get();
            if (current == null) return;
            int delta = ctx.delta(+1, -1, +5, -5);
            amountSetter.accept(Math.max(0, current.amount() + delta));
        }));

        return items;
    }

    static void openResourcePicker(Player player, String title, Supplier<List<RewardItem>> rewardItemsSupplier, Consumer<Resource> addResource, Consumer<Player> back) {
        SettingPageGUI.open(player, title,
                () -> buildResourcePickerItems(rewardItemsSupplier, addResource, back),
                back);
    }

    private static List<SettingItem> buildResourcePickerItems(Supplier<List<RewardItem>> rewardItemsSupplier, Consumer<Resource> addResource, Consumer<Player> back) {
        Set<String> alreadyAdded = new HashSet<>();
        List<RewardItem> rewardItems = rewardItemsSupplier.get();
        if (rewardItems != null) {
            for (RewardItem rewardItem : rewardItems) {
                if (rewardItem.getResourceName() != null) {
                    alreadyAdded.add(rewardItem.getResourceName().toLowerCase());
                }
            }
        }

        Collection<Resource> resources;
        try {
            resources = ModuleManager.getModule(ResourcesModule.class).getResources()
                    .stream()
                    .toList();
        } catch (Exception e) {
            resources = List.of();
        }

        List<SettingItem> items = new ArrayList<>();
        for (Resource res : resources) {
            if (alreadyAdded.contains(res.getName().toLowerCase())) {
                continue;
            }

            ItemBuilder b = new ItemBuilder(resourceMaterial(res.getName()));
            b.setName(res.getColor() + res.getDisplayName());
            b.removeLore();
            if (res.getName().equalsIgnoreCase("Souls"))
                b.addLoreLine("§7Currency used to purchase Perks");
            b.addLoreLine("");
            b.addLoreLine("§a► Click to add");

            items.add(SettingItem.navigate(b.toItemStack(), ctx -> {
                addResource.accept(res);
                ctx.player.sendMessage(StringUtils.colorizer("§aAdded #71c900" + res.getDisplayName() + "§a. Adjust the amount in the edit page."));
                ctx.player.playSound(ctx.player, Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 1.2F);
                SettingsEditSession.afterCurrentAction(() -> back.accept(ctx.player));
            }));
        }

        if (items.isEmpty()) {
            items.add(SettingItem.display(new ItemBuilder(Material.BARRIER)
                    .setName("§cNo resources available")
                    .removeLore()
                    .addLoreLine("§7All registered resources are already used here.")
                    .toItemStack()));
        }
        return items;
    }

    static ItemStack addResourceIcon() {
        return new ItemBuilder(Material.EMERALD)
                .setName("§aAdd Resource")
                .removeLore()
                .addLoreLine("§7Pick any registered resource to add.")
                .toItemStack();
    }

    static String rewardItemLine(RewardItem ri) {
        ResourceDisplay resource = display(ri);
        String chance = ri.getChance() < 100 ? " §7(" + ri.getChance() + "% chance)" : "";
        return " " + resource.color() + "+ " + amountLabel(ri) + " " + resource.displayName() + chance;
    }

    static String resourceRewardLine(String resourceName, int amount) {
        ResourceDisplay resource = display(resourceName);
        return " " + resource.color() + "+ " + StringUtils.betterNumberFormat(amount) + " " + resource.displayName();
    }

    static String resourceDisplayName(String resourceName) {
        return display(resourceName).displayName();
    }

    static String resourceColor(String resourceName) {
        return display(resourceName).color();
    }

    static Material resourceMaterial(String name) {
        if (name == null) {
            return Material.SUNFLOWER;
        }
        return switch (name.toLowerCase()) {
            case "coins" -> Material.GOLD_INGOT;
            case "souls" -> Material.CYAN_DYE;
            case "experiencepoints", "xp" -> Material.EXPERIENCE_BOTTLE;
            case "cosmetictokens", "tokens" -> Material.AMETHYST_SHARD;
            default -> Material.SUNFLOWER;
        };
    }

    private static String amountLabel(RewardItem ri) {
        if (ri.hasRandomAmount()) {
            return StringUtils.betterNumberFormat(ri.getRandomMinRange())
                    + "-" + StringUtils.betterNumberFormat(ri.getRandomMaxRange());
        }
        return StringUtils.betterNumberFormat(ri.getAmount());
    }

    private static ResourceDisplay display(RewardItem ri) {
        return display(ri.getResourceName());
    }

    private static ResourceDisplay display(String resourceName) {
        Resource resource = safeResource(resourceName);
        if (resource == null) {
            String name = resourceName == null ? "Unknown" : resourceName;
            return new ResourceDisplay(name, name, "§f");
        }
        return new ResourceDisplay(resource.getName(), resource.getDisplayName(), resource.getColor().toString());
    }

    private static Resource safeResource(String resourceName) {
        try {
            return ModuleManager.getModule(ResourcesModule.class).getResourceByName(resourceName);
        } catch (Exception e) {
            return null;
        }
    }

    private record ResourceDisplay(String name, String displayName, String color) {}
}
