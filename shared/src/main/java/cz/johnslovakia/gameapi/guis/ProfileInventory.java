package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.Core;
import cz.johnslovakia.gameapi.modules.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.modules.dailyRewardTrack.DailyRewardTier;
import cz.johnslovakia.gameapi.modules.dailyRewardTrack.DailyRewardTrackModule;
import cz.johnslovakia.gameapi.modules.levels.LevelEvolution;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.modules.levels.PlayerLevelData;
import cz.johnslovakia.gameapi.modules.messages.Language;
import cz.johnslovakia.gameapi.modules.resources.Resource;
import cz.johnslovakia.gameapi.modules.resources.ResourceComparator;
import cz.johnslovakia.gameapi.modules.resources.ResourcesModule;
import cz.johnslovakia.gameapi.rewards.RewardItem;
import cz.johnslovakia.gameapi.rewards.unclaimed.DailyMeterUnclaimedReward;
import cz.johnslovakia.gameapi.rewards.unclaimed.LevelUpUnclaimedReward;
import cz.johnslovakia.gameapi.rewards.unclaimed.UnclaimedRewardType;
import cz.johnslovakia.gameapi.rewards.unclaimed.UnclaimedRewardsModule;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.Utils;

import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileInventory {

    private static final Set<UUID> claimingDailyReward = ConcurrentHashMap.newKeySet();

    private static int getBonus(PlayerIdentity gamePlayer) {
        if (gamePlayer.getMetadata().get("quest_reward_bonus") == null) {
            List<Integer> percentages = Arrays.asList(5, 7, 10, 12, 15, 17, 20, 25, 30, 35, 40, 45, 50, 75, 100);
            for (Integer percent : percentages) {
                if (gamePlayer.getOnlinePlayer().hasPermission("vip.bonus" + percent)) {
                    gamePlayer.getMetadata().put("quest_reward_bonus", percent);
                    return percent;
                }
            }
            return 0;
        }
        return (int) gamePlayer.getMetadata().get("quest_reward_bonus");
    }


    public static void openGUI(PlayerIdentity gamePlayer) {
        String verChar;
        if (ModuleManager.getInstance().hasModule(LevelModule.class) && ModuleManager.getInstance().hasModule(DailyRewardTrackModule.class)) {
            verChar = "\uE000";
        } else if (ModuleManager.getInstance().hasModule(LevelModule.class)) {
            verChar = "\uE002";
        } else {
            verChar = "\uE003";
        }

        GUI inventory = Component.gui()
                .title(net.kyori.adventure.text.Component.text("§f七七七七七七七七" + verChar).font(Key.key("jsplugins", "guis")))
                .rows(5)
                .prepare((gui, player) -> {
                    ResourcesModule resourcesModule = ModuleManager.getModule(ResourcesModule.class);
                    UnclaimedRewardsModule unclaimedRewardsModule = ModuleManager.getModule(UnclaimedRewardsModule.class);
                    MessageModule messageModule = ModuleManager.getModule(MessageModule.class);

                    ItemBuilder close = new ItemBuilder(Material.ECHO_SHARD);
                    close.setCustomModelData(1017);
                    close.hideAllFlags();
                    close.setName(messageModule.getMessage(player, "inventory.item.close").toComponent());

                    ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                    info.setCustomModelData(1018);
                    info.hideAllFlags();
                    info.setName(messageModule.getMessage(player, "inventory.info_item.player_menu.name").toComponent());
                    info.setLore(messageModule.getMessage(player, "inventory.info_item.player_menu.lore").toComponent());

                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> {
                        gui.close(player);
                        player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());


                    ItemBuilder profile = new ItemBuilder(Utils.getPlayerHead(player));
                    profile.hideAllFlags();
                    profile.setName("§aMy Profile");
                    profile.addLoreLine("");
                    for (Resource resource : resourcesModule.getResources().stream().sorted(new ResourceComparator()).toList()) {
                        int balance = resourcesModule.getPlayerBalanceCached(gamePlayer, resource);
                        profile.addLoreLine("§7" + resource.getDisplayName() + ": §a" + StringUtils.betterNumberFormat(balance));
                    }

                    int profileSlot = 13;
                    if (ModuleManager.getInstance().hasModule(DailyRewardTrackModule.class) && ModuleManager.getInstance().hasModule(LevelModule.class)) {
                        profileSlot = 11;
                    } else if (ModuleManager.getInstance().hasModule(LevelModule.class)) {
                        profileSlot = 12;
                    }

                    gui.appendElement(profileSlot, Component.element(profile.toItemStack()).addClick(i -> {
                    }).build());


                    int bonus = getBonus(gamePlayer);


                    if (ModuleManager.getInstance().hasModule(LevelModule.class)) {
                        Optional<LevelUpUnclaimedReward> levelUpUnclaimedReward = unclaimedRewardsModule
                                .getPlayerUnclaimedRewardsByType(gamePlayer, UnclaimedRewardType.LEVELUP).stream()
                                .filter(r -> r instanceof LevelUpUnclaimedReward)
                                .map(r -> (LevelUpUnclaimedReward) r)
                                .findFirst();
                        LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
                        PlayerLevelData levelProgress = levelModule.getPlayerData(gamePlayer);
                        boolean isMaxLevel = levelProgress.getLevel() >= levelModule.getLevelRanges().getLast().endLevel() && levelUpUnclaimedReward.isEmpty();


                        ItemBuilder level = new ItemBuilder(Material.ECHO_SHARD);
                        level.setCustomModelData(levelUpUnclaimedReward
                                .map(r -> levelModule.getLevelEvolution(r.getLevel()).blinkingItemCustomModelData())
                                .orElseGet(() -> levelModule.getLevelEvolution(levelProgress.getLevel()).itemCustomModelData()));

                        level.hideAllFlags();
                        level.setName(messageModule.getMessage(gamePlayer, "inventory.profile_menu.your_level.name")
                                .replace("%level%", "" + levelProgress.getLevel()).toComponent());
                        level.addLoreLine("");
                        level.addLoreLine(messageModule.getMessage(player, "inventory.profile_menu.your_level.your_level")
                                .replace("%level%", levelModule.getLevelColored(levelUpUnclaimedReward.map(LevelUpUnclaimedReward::getLevel).orElseGet(levelProgress::getLevel)))
                                .replace("%icon%", levelUpUnclaimedReward
                                        .map(r -> levelModule.getLevelEvolution(r.getLevel()).getIcon())
                                        .orElseGet(() -> levelModule.getLevelEvolution(levelProgress.getLevel()).getIcon()))
                                .toComponent());
                        if (!isMaxLevel) {
                            level.addLoreLine("");
                            if (levelUpUnclaimedReward.isEmpty()) {
                                level.addLoreLine(Utils.getStringProgressBar(levelProgress.getXpOnCurrentLevel(), levelProgress.getXpToNextLevel()));
                                level.addLoreLine(messageModule.getMessage(player, "inventory.profile_menu.your_level.progress")
                                        .replace("%level%", "" + (levelProgress.getLevel() + 1))
                                        .replace("%xp%", StringUtils.betterNumberFormat(levelProgress.getXpOnCurrentLevel()))
                                        .replace("%needed_xp%", StringUtils.betterNumberFormat(levelProgress.getXpToNextLevel()))
                                        .toComponent());
                            } else {
                                int rewardLevel = levelUpUnclaimedReward.get().getLevel();
                                int completedLevel = rewardLevel - 1;
                                int neededXP = levelModule.getLevelRange(completedLevel).getXPForLevel(completedLevel);
                                level.addLoreLine(Utils.getStringProgressBar(neededXP, neededXP));
                                level.addLoreLine(messageModule.getMessage(player, "inventory.profile_menu.your_level.progress")
                                        .replace("%level%", "" + (levelUpUnclaimedReward.get().getLevel()))
                                        .replace("%xp%", "§a" + StringUtils.betterNumberFormat(neededXP))
                                        .replace("%needed_xp%", StringUtils.betterNumberFormat(neededXP))
                                        .toComponent());
                            }
                            level.addLoreLine("");
                            level.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.profile_menu.your_level.rewards").toComponent());
                            if (levelUpUnclaimedReward.isEmpty()) {
                                for (RewardItem rewardItem : levelModule.getRewardForLevel(levelProgress.getLevel() + 1).getRewardItems()) {
                                    Resource resource = rewardItem.getResource();
                                    level.addLoreLine(" " + resource.getColor() + "+ " + (!rewardItem.hasRandomAmount() ? rewardItem.getAmount() : rewardItem.getRandomMinRange() + "-" + rewardItem.getRandomMaxRange()) + " " + resource.getDisplayName() + (rewardItem.getChance() != 100 ? " §7(" + rewardItem.getChance() + "% " + LegacyComponentSerializer.legacySection().serialize(messageModule.getMessage(gamePlayer, "word.chance").toComponent()) + ")" : ""));
                                    if (bonus != 0 && resource.isApplicableBonus())
                                        level.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.profile_menu.your_level.bonus").replace("%bonus%", "" + bonus).toComponent());
                                }
                            } else {
                                for (RewardItem rewardItem : levelUpUnclaimedReward.get().getReward().getRewardItems()) {
                                    Resource resource = rewardItem.getResource();
                                    level.addLoreLine(" " + resource.getColor() + "+ " + (!rewardItem.hasRandomAmount() ? rewardItem.getAmount() : rewardItem.getRandomMinRange() + "-" + rewardItem.getRandomMaxRange()) + " " + resource.getDisplayName() + (rewardItem.getChance() != 100 ? " §7(" + rewardItem.getChance() + "% " + LegacyComponentSerializer.legacySection().serialize(messageModule.getMessage(gamePlayer, "word.chance").toComponent()) + ")" : ""));
                                    if (bonus != 0 && resource.isApplicableBonus())
                                        level.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.profile_menu.your_level.bonus").replace("%bonus%", "" + bonus).toComponent());
                                }
                            }
                            LevelEvolution nextEvolution = levelModule.getNextLevelEvolution(levelUpUnclaimedReward.map(LevelUpUnclaimedReward::getLevel).orElseGet(levelProgress::getLevel));
                            if (nextEvolution != null) {
                                level.addLoreLine("");
                                level.addLoreLine(messageModule.getMessage(player, "inventory.profile_menu.your_level.next_evolution")
                                        .replace("%next_evolution%", levelModule.getLevelColored(nextEvolution.startLevel()).append(nextEvolution.getIcon()))
                                        .toComponent());
                            }
                        }
                        if (levelUpUnclaimedReward.isPresent()) {
                            level.addLoreLine("");
                            level.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.unclaimed_rewards.click_to_claim").toComponent());
                        }else if (isMaxLevel){
                            level.addLoreLine("");
                            level.addLoreLine(messageModule.getMessage(player, "inventory.profile_menu.your_level.max_level").toComponent());
                        }

                        gui.appendElement(ModuleManager.getInstance().hasModule(DailyRewardTrackModule.class) ? 13 : 14,
                                Component.element(level.toItemStack()).addClick(i -> {
                                    levelUpUnclaimedReward.ifPresent(unclaimedReward -> {
                                        messageModule.getMessage(gamePlayer, "chat.unclaimed_reward.levelup.claimed").send();
                                        if (bonus != 0) unclaimedReward.setBonus(bonus);
                                        unclaimedReward.claim();
                                        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
                                        openGUI(gamePlayer);
                                    });
                                }).build());
                    }


                    if (ModuleManager.getInstance().hasModule(DailyRewardTrackModule.class)) {
                        Optional<DailyMeterUnclaimedReward> dailyMeterUnclaimedReward = unclaimedRewardsModule
                                .getPlayerUnclaimedRewardsByType(gamePlayer, UnclaimedRewardType.DAILYMETER).stream()
                                .filter(DailyMeterUnclaimedReward.class::isInstance)
                                .map(DailyMeterUnclaimedReward.class::cast)
                                .findFirst();

                        LocalDate today = LocalDate.now();
                        DailyRewardTrackModule dailyMeter = ModuleManager.getModule(DailyRewardTrackModule.class);
                        DailyRewardTier dailyMeterClaim = dailyMeter.getPlayerCurrentTargetTier(gamePlayer);

                        ItemBuilder dailyRewardTrack = new ItemBuilder(Material.ECHO_SHARD);
                        dailyRewardTrack.setCustomModelData(dailyMeterUnclaimedReward.isPresent() ? 1053 : 1052);
                        dailyRewardTrack.hideAllFlags();
                        dailyRewardTrack.setName(messageModule.getMessage(player, "inventory.profile_menu.daily_reward_track.name").toComponent());
                        dailyRewardTrack.addLoreLine("");
                        dailyRewardTrack.addLoreLine(messageModule.getMessage(player, "inventory.profile_menu.daily_reward_track.description").toComponent());
                        if (dailyMeterClaim != null && dailyMeterClaim.tier() <= dailyMeter.getMaxTier()) {
                            dailyRewardTrack.addLoreLine("");
                            if (dailyMeterUnclaimedReward.isEmpty()) {
                                dailyRewardTrack.addLoreLine(Utils.getStringProgressBar(dailyMeter.getXpProgressOnCurrentTier(gamePlayer), dailyMeterClaim.neededXP()));
                                dailyRewardTrack.addLoreLine(messageModule.getMessage(player, "inventory.profile_menu.daily_reward_track.progress")
                                        .replace("%xp%", StringUtils.betterNumberFormat(dailyMeter.getXpProgressOnCurrentTier(gamePlayer)))
                                        .replace("%needed_xp%", StringUtils.betterNumberFormat(dailyMeterClaim.neededXP()))
                                        .toComponent());
                            } else {
                                int tierIndex = dailyMeterUnclaimedReward.get().getTier() - 1;
                                if (tierIndex >= 0 && tierIndex < dailyMeter.getTiers().size()) {
                                    DailyRewardTier unclaimedRewardTier = dailyMeter.getTiers().get(tierIndex);
                                    dailyRewardTrack.addLoreLine(Utils.getStringProgressBar(unclaimedRewardTier.neededXP(), unclaimedRewardTier.neededXP()));
                                    dailyRewardTrack.addLoreLine(messageModule.getMessage(player, "inventory.profile_menu.daily_reward_track.progress")
                                            .replace("%xp%", "§a" + StringUtils.betterNumberFormat(unclaimedRewardTier.neededXP()))
                                            .replace("%needed_xp%", StringUtils.betterNumberFormat(unclaimedRewardTier.neededXP()))
                                            .toComponent());
                                }
                            }
                        }
                        if ((dailyMeterClaim != null && dailyMeterClaim.tier() <= dailyMeter.getMaxTier())
                                || dailyMeterUnclaimedReward.isPresent()) {
                            dailyRewardTrack.addLoreLine("");
                            dailyRewardTrack.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.profile_menu.daily_reward_track.rewards").toComponent());
                            if (dailyMeterUnclaimedReward.isEmpty()) {
                                for (RewardItem rewardItem : dailyMeterClaim.reward().getRewardItems()) {
                                    Resource resource = rewardItem.getResource();
                                    dailyRewardTrack.addLoreLine(" " + resource.getColor() + "+ " + (!rewardItem.hasRandomAmount() ? rewardItem.getAmount() : rewardItem.getRandomMinRange() + "-" + rewardItem.getRandomMaxRange()) + " " + resource.getDisplayName() + (rewardItem.getChance() != 100 ? " §7(" + rewardItem.getChance() + "% " + LegacyComponentSerializer.legacySection().serialize(messageModule.getMessage(gamePlayer, "word.chance").toComponent()) + ")" : ""));
                                    if (bonus != 0 && resource.isApplicableBonus())
                                        dailyRewardTrack.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.profile_menu.daily_reward_track.bonus").replace("%bonus%", "" + bonus).toComponent());
                                }
                            } else {
                                for (RewardItem rewardItem : dailyMeterUnclaimedReward.get().getReward().getRewardItems()) {
                                    Resource resource = rewardItem.getResource();
                                    dailyRewardTrack.addLoreLine(" " + resource.getColor() + "+ " + (!rewardItem.hasRandomAmount() ? rewardItem.getAmount() : rewardItem.getRandomMinRange() + "-" + rewardItem.getRandomMaxRange()) + " " + resource.getDisplayName() + (rewardItem.getChance() != 100 ? " §7(" + rewardItem.getChance() + "% " + LegacyComponentSerializer.legacySection().serialize(messageModule.getMessage(gamePlayer, "word.chance").toComponent()) + ")" : ""));
                                    if (bonus != 0 && resource.isApplicableBonus())
                                        dailyRewardTrack.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.profile_menu.daily_reward_track.bonus").replace("%bonus%", "" + bonus).toComponent());
                                }
                            }
                        }
                        dailyRewardTrack.addLoreLine("");
                        if (dailyMeterClaim != null || dailyMeterUnclaimedReward.isPresent()) {
                            if (dailyMeterUnclaimedReward.isEmpty() || !dailyMeterUnclaimedReward.get().getCreatedAt().toLocalDate().isBefore(today)) {
                                dailyRewardTrack.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.profile_menu.daily_reward_track.daily_claims")
                                        .replace("%claims%", "§f" + dailyMeterUnclaimedReward.map(r -> r.getTier() - 1).orElseGet(() -> dailyMeterClaim.tier() - 1))
                                        .toComponent());
                            } else {
                                dailyRewardTrack.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.profile_menu.daily_reward_track.old_daily_claim")
                                        .replace("%claim%", "" + dailyMeterUnclaimedReward.get().getTier())
                                        .replace("%date%", dailyMeterUnclaimedReward.get().getCreatedAt().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
                                        .toComponent());
                            }
                        } else {
                            dailyRewardTrack.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.profile_menu.daily_reward_track.daily_claims")
                                    .replace("%claims%", "§a" + dailyMeter.getMaxTier())
                                    .toComponent());
                        }
                        if (dailyMeterUnclaimedReward.isEmpty() || !dailyMeterUnclaimedReward.get().getCreatedAt().toLocalDate().isBefore(today)) {
                            dailyRewardTrack.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.profile_menu.daily_reward_track.resets_in")
                                    .replace("%time%", StringUtils.getTimeLeftUntil(today.plusDays(1).atStartOfDay()))
                                    .toComponent());
                        }
                        if (dailyMeterClaim == null && dailyMeterUnclaimedReward.isEmpty()) {
                            dailyRewardTrack.addLoreLine("");
                            dailyRewardTrack.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.profile_menu.daily_reward_track.wait_for_reset").toComponent());
                        }
                        if (dailyMeterUnclaimedReward.isPresent()) {
                            dailyRewardTrack.addLoreLine("");
                            dailyRewardTrack.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.unclaimed_rewards.click_to_claim").toComponent());
                        }
                        gui.appendElement(15, Component.element(dailyRewardTrack.toItemStack()).addClick(i -> {
                            dailyMeterUnclaimedReward.ifPresent(unclaimedReward -> {
                                UUID uuid = player.getUniqueId();
                                if (!claimingDailyReward.add(uuid)) return;
                                try {
                                    messageModule.getMessage(gamePlayer, "chat.unclaimed_reward.daily_reward_track.claimed").send();
                                    if (bonus != 0) unclaimedReward.setBonus(bonus);
                                    unclaimedReward.claim();
                                    player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
                                    int claimedTier = unclaimedReward.getTier();
                                    if (claimedTier > dailyMeter.getPlayerDailyClaims(gamePlayer) && unclaimedReward.getCreatedAt().toLocalDate().equals(LocalDate.now())) {
                                        dailyMeter.setPlayerDailyRewardsClaim(gamePlayer, claimedTier);
                                    }
                                    openGUI(gamePlayer);
                                } finally {
                                    claimingDailyReward.remove(uuid);
                                }
                            });
                        }).build());
                    }

                    gui.setContainer(28, Component.staticContainer()
                            .size(3, 2)
                            .init(container -> {
                                CosmeticsModule cosmeticsManager = ModuleManager.getModule(CosmeticsModule.class);
                                for (CosmeticsCategory category : cosmeticsManager.getCategories()) {
                                    Cosmetic selectedCosmetic = cosmeticsManager.getPlayerSelectedCosmetic(gamePlayer, category);
                                    if (selectedCosmetic != null) {
                                        Element element = Component.element(getCosmeticEditedItem(gamePlayer, selectedCosmetic)).addClick(i -> {
                                            CosmeticsInventory.openCategory(gamePlayer, category);
                                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 10.0F, 10.0F);
                                        }).build();
                                        container.appendElement(element);
                                    } else {
                                        ItemBuilder categoryItem = new ItemBuilder(Material.BARRIER);
                                        categoryItem.setName("§a" + category.getName());
                                        categoryItem.removeLore();
                                        categoryItem.hideAllFlags();
                                        categoryItem.addLoreLine("");
                                        messageModule.getMessage(player, "inventory.cosmetics.click_to_view").addToItemLore(categoryItem);
                                        Element element = Component.element(categoryItem.toItemStack()).addClick(i -> {
                                            CosmeticsInventory.openCategory(gamePlayer, category);
                                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 10.0F, 10.0F);
                                        }).build();
                                        container.appendElement(element);
                                    }
                                }
                            }).build());

                    List<Language> languageList = Language.getLanguages();
                    languageList.sort(Comparator.comparing(Language::getName));

                    ItemBuilder languages = new ItemBuilder(Material.ECHO_SHARD);
                    languages.setCustomModelData(1051);
                    languages.hideAllFlags();
                    languages.setName("§aLanguages");
                    if (languageList.size() <= 5) {
                        languages.addLoreLine("");
                        for (Language language : languageList) {
                            languages.addLoreLine((messageModule.getPlayerLanguage(gamePlayer).equals(language) ? "§a" : "§7") + org.apache.commons.lang3.StringUtils.capitalize(language.getName()));
                        }
                    } else {
                        languages.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.profile_menu.language.selected_language")
                                .replace("%language%", org.apache.commons.lang3.StringUtils.capitalize(messageModule.getPlayerLanguage(gamePlayer).getName()))
                                .toComponent());
                    }
                    languages.addLoreLine("");
                    languages.addLoreLine(messageModule.getMessage(gamePlayer, "inventory.profile_menu.language.click_to_change").toComponent());
                    gui.appendElement(32, Component.element(languages.toItemStack()).addClick(i -> {
                        if (languageList.size() <= 5) {
                            int currentIndex = languageList.indexOf(messageModule.getPlayerLanguage(gamePlayer));
                            Language nextLanguage = (currentIndex + 1 < languageList.size()) ? languageList.get(currentIndex + 1) : languageList.get(0);
                            messageModule.setPlayerLanguage(gamePlayer, nextLanguage, true);
                            openGUI(gamePlayer);
                            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
                        } else {
                            player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 1F);
                        }
                    }).build());
                }).build();

        inventory.open(gamePlayer.getOnlinePlayer());

        if (ModuleManager.getInstance().hasModule(DailyRewardTrackModule.class)){
            DailyRewardTrackModule module = ModuleManager.getModule(DailyRewardTrackModule.class);

            inventory.onClose(GUI.CloseReason.BY_PLAYER, player -> {
                int claims = module.getPlayerDailyClaims(gamePlayer);
                Bukkit.getScheduler().runTaskAsynchronously(Core.getInstance().getPlugin(), task -> {
                    module.savePlayerDailyRewardsClaim(gamePlayer, claims);
                });
            });
            inventory.onClose(GUI.CloseReason.BY_METHOD, player -> {
                int claims = module.getPlayerDailyClaims(gamePlayer);
                Bukkit.getScheduler().runTaskAsynchronously(Core.getInstance().getPlugin(), task -> {
                    module.savePlayerDailyRewardsClaim(gamePlayer, claims);
                });
            });
        }
    }


    private static ItemStack getCosmeticEditedItem(PlayerIdentity gamePlayer, Cosmetic cosmetic) {
        MessageModule messageModule = ModuleManager.getModule(MessageModule.class);

        ItemBuilder item = new ItemBuilder(cosmetic.getIcon());
        item.setName("§a§l" + cosmetic.getName());
        item.removeLore();
        if (PlainTextComponentSerializer.plainText().serialize(messageModule.getMessage(gamePlayer, cosmetic.getRarity().getTranslateKey()).toComponent()).length() == 1) {
            item.addLoreLine(net.kyori.adventure.text.Component.text("§f").append(messageModule.getMessage(gamePlayer, cosmetic.getRarity().getTranslateKey()).toComponent()));
        }
        item.addLoreLine("§8" + cosmetic.getCategory().getName());
        item.removeEnchantment(Enchantment.SHARPNESS);
        item.addLoreLine("");
        if (PlainTextComponentSerializer.plainText().serialize(messageModule.getMessage(gamePlayer, cosmetic.getRarity().getTranslateKey()).toComponent()).length() > 1) {
            messageModule.getMessage(gamePlayer, "inventory.cosmetics.rarity")
                    .replace("%rarity%", messageModule.getMessage(gamePlayer, cosmetic.getRarity().getTranslateKey()).toComponent())
                    .addToItemLore(item);
            item.addLoreLine("");
        }
        item.hideAllFlags();
        if (cosmetic.getPreviewConsumer() != null) {
            messageModule.getMessage(gamePlayer, "inventory.cosmetics.click_to_change").addToItemLore(item);
        }
        return item.toItemStack();
    }
}