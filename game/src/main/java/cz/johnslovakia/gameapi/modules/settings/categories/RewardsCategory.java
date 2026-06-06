package cz.johnslovakia.gameapi.modules.settings.categories;

import cz.johnslovakia.gameapi.guis.ConfirmInventory;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.settings.*;
import cz.johnslovakia.gameapi.rewards.RewardItem;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RewardsCategory implements SettingCategory {

    private final String dbKey;
    private final List<RewardEventDefinition> eventDefinitions;
    private final RewardsConfig.DefaultsConfigurer defaultsConfigurer;

    public RewardsCategory(String dbKey, List<RewardEventDefinition> eventDefinitions, RewardsConfig.DefaultsConfigurer defaultsConfigurer) {
        this.dbKey = dbKey;
        this.eventDefinitions = eventDefinitions;
        this.defaultsConfigurer = defaultsConfigurer;
    }

    @Override
    public String getName() { return "Game Rewards"; }

    @Override
    public Material getIcon() { return Material.DIAMOND; }

    @Override
    public String[] getLore() { return new String[]{"§7Edit game rewards"}; }

    @Override
    public void open(Player player) { openMain(player); }

    private void openMain(Player player) {
        List<RewardEventDefinition> dailyEvents    = eventDefinitions.stream().filter(RewardEventDefinition::isDaily).toList();
        List<RewardEventDefinition> standardEvents = eventDefinitions.stream().filter(RewardEventDefinition::isStandard).toList();
        List<SettingItem> items = new ArrayList<>();

        if (!dailyEvents.isEmpty()) {
            items.add(SettingItem.navigate(
                    new ItemBuilder(Material.EXPERIENCE_BOTTLE).setName("§fWith Level System").removeLore()
                            .addLoreLine("§7Rewards when §aLevel System §7is §aenabled")
                            .addLoreLine("").addLoreLine("§aClick to edit").toItemStack(),
                    ctx -> openMode(ctx.player, "daily", dailyEvents, this::openMain)));
        }

        if (!standardEvents.isEmpty()) {
            items.add(SettingItem.navigate(
                    new ItemBuilder(Material.GOLD_INGOT).setName("§fWithout Level System").removeLore()
                            .addLoreLine("§7Rewards when §aLevel System §7is §cdisabled")
                            .addLoreLine("").addLoreLine("§aClick to edit").toItemStack(),
                    ctx -> openMode(ctx.player, "standard", standardEvents, this::openMain)));
        }

        SettingPageGUI.open(player, "Rewards",
                () -> items,
                p -> ModuleManager.getModule(SettingsModule.class).open(p));
    }

    private void openMode(Player player, String mode, List<RewardEventDefinition> events, Consumer<Player> back) {
        boolean isDaily = mode.equals("daily");
        String title = isDaily ? "With Level System Rewards" : "Without Level System Rewards";

        SettingPageGUI.open(player, title,
                () -> buildModeItems(events, mode, back),
                back,
                List.of(new BottomAction(8, resetIcon(), p -> new ConfirmInventory(
                        PlayerIdentityRegistry.get(p),
                        "§cReset " + (isDaily ? "Daily" : "Standard") + " rewards to default?",
                        pi -> {
                            SettingsEditSession.runAction(p, () -> {
                                RewardsConfig rc = config();
                                rc.resetMode(mode + ".");
                                RewardsConfig.save(rc);
                            });
                            p.sendMessage("§cGame Rewards were reset to default. Changes may not apply until the server is restarted.");
                            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1F, 1.5F);
                            openMode(p, mode, events, back);
                        },
                        pi -> openMode(pi.getOnlinePlayer(), mode, events, back)
                ).openGUI())));
    }

    private List<SettingItem> buildModeItems(List<RewardEventDefinition> events, String mode, Consumer<Player> back) {
        RewardsConfig rc = config();
        List<SettingItem> items = new ArrayList<>();

        for (RewardEventDefinition meta : events) {
            List<RewardsConfig.ResourceReward> rewards = rc.getRewards(meta.getEventKey());
            ItemBuilder b = new ItemBuilder(meta.getIcon());
            b.setName("§f" + meta.getLabel());
            b.removeLore();
            b.addLoreLine(meta.getDescription());
            b.addLoreLine("");

            if (rewards.isEmpty()) {
                b.addLoreLine("§cNo rewards configured!");
            } else {
                for (RewardsConfig.ResourceReward rr : rewards) {
                    b.addLoreLine(RewardSettingsHelper.resourceRewardLine(rr.getResourceName(), rr.getAmount()));
                }
            }
            b.addLoreLine("").addLoreLine("§a► Click to edit");

            Consumer<Player> backToMode = p -> openMode(p, mode, events, back);
            items.add(SettingItem.navigate(b.toItemStack(),
                    ctx -> openEventDetail(ctx.player, meta.getEventKey(), meta.getLabel(), backToMode)));
        }
        return items;
    }

    private void openEventDetail(Player player, String eventKey, String label, Consumer<Player> back) {
        SettingPageGUI.open(player, label,
                () -> buildEventDetailItems(eventKey, label, back),
                back,
                List.of(
                        new BottomAction(4, RewardSettingsHelper.addResourceIcon(), p -> openResourcePicker(p, eventKey, label, back)),
                        new BottomAction(8, resetEventIcon(), p -> new ConfirmInventory(
                                PlayerIdentityRegistry.get(p),
                                "§cReset \"" + label + "\" rewards to default?",
                                pi -> {
                                    SettingsEditSession.runAction(p, () -> {
                                        RewardsConfig rc = config();
                                        rc.resetEventToDefault(eventKey);
                                        RewardsConfig.save(rc);
                                    });
                                    p.sendMessage("§a\"" + label + "\" rewards reset to default.");
                                    p.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1F, 1.5F);
                                    openEventDetail(p, eventKey, label, back);
                                },
                                pi -> openEventDetail(pi.getOnlinePlayer(), eventKey, label, back)
                        ).openGUI())));
    }

    private List<SettingItem> buildEventDetailItems(String eventKey, String label, Consumer<Player> back) {
        RewardsConfig rc = config();
        List<RewardsConfig.ResourceReward> rewards = rc.getRewards(eventKey);
        List<SettingItem> items = new ArrayList<>();

        if (rewards.isEmpty()) {
            items.add(SettingItem.display(new ItemBuilder(Material.BARRIER)
                    .setName("§cNo rewards configured").removeLore()
                    .addLoreLine("§7Click the §aAdd Resource §7item to add one.").toItemStack()));
            return items;
        }

        List<RewardSettingsHelper.ResourceAmountReward> rewardItems = rewards.stream()
                .map(rr -> new RewardSettingsHelper.ResourceAmountReward(rr.getResourceName(), rr.getAmount()))
                .toList();
        items.addAll(RewardSettingsHelper.buildResourceAmountRewardNavItems(rewardItems,
                (rewardIdx, p) -> openEventRewardDetail(p, eventKey, label, rewardIdx, back),
                (rewardIdx, p) -> removeEventReward(eventKey, rewardIdx),
                p -> openEventDetail(p, eventKey, label, back)));
        return items;
    }

    private void openEventRewardDetail(Player player, String eventKey, String label, int rewardIdx, Consumer<Player> back) {
        SettingPageGUI.open(player, "Reward Item",
                () -> RewardSettingsHelper.buildResourceAmountRewardDetailItems(
                        () -> {
                            List<RewardsConfig.ResourceReward> rewards = config().getRewards(eventKey);
                            if (rewardIdx < 0 || rewardIdx >= rewards.size()) return null;
                            RewardsConfig.ResourceReward rr = rewards.get(rewardIdx);
                            return new RewardSettingsHelper.ResourceAmountReward(rr.getResourceName(), rr.getAmount());
                        },
                        amount -> {
                            RewardsConfig cfg = config();
                            List<RewardsConfig.ResourceReward> rewards = cfg.getRewards(eventKey);
                            if (rewardIdx >= 0 && rewardIdx < rewards.size()) {
                                rewards.get(rewardIdx).setAmount(amount);
                                RewardsConfig.save(cfg);
                            }
                        },
                        p -> {
                            removeEventReward(eventKey, rewardIdx);
                        },
                        p -> openEventDetail(p, eventKey, label, back)
                ),
                p -> openEventDetail(p, eventKey, label, back));
    }

    private void removeEventReward(String eventKey, int rewardIdx) {
        RewardsConfig cfg = config();
        cfg.removeReward(eventKey, rewardIdx);
        RewardsConfig.save(cfg);
    }

    private void openResourcePicker(Player player, String eventKey, String label, Consumer<Player> back) {
        Consumer<Player> backToDetail = p -> openEventDetail(p, eventKey, label, back);
        RewardSettingsHelper.openResourcePicker(player,
                "Add Resource - " + label,
                () -> config().getRewards(eventKey).stream()
                        .map(rr -> new RewardItem(rr.getResourceName(), rr.getAmount()))
                        .toList(),
                res -> {
                    RewardsConfig cfg = config();
                    cfg.addReward(eventKey, res.getName(), 10);
                    RewardsConfig.save(cfg);
                },
                backToDetail);
    }

    private RewardsConfig config() {
        return RewardsConfig.get(dbKey, defaultsConfigurer);
    }

    private static ItemStack resetEventIcon() {
        return new ItemBuilder(Material.BARRIER).setName("§cReset to Default").removeLore()
                .addLoreLine("§7Restores this reward to the original defaults.").toItemStack();
    }

    private static ItemStack resetIcon() {
        return new ItemBuilder(Material.BARRIER).setName("§cReset to Default").removeLore()
                .addLoreLine("").addLoreLine("§cClick to restore default rewards").toItemStack();
    }
}
