package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.datastorage.PlayerTable;
import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsManager;
import cz.johnslovakia.gameapi.levelSystem.*;
import cz.johnslovakia.gameapi.messages.Language;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.achievements.PlayerAchievementData;
import cz.johnslovakia.gameapi.users.quests.QuestType;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.users.resources.ResourcesManager;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.Utils;
import cz.johnslovakia.gameapi.utils.rewards.RewardItem;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.DailyMeterUnclaimedReward;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.LevelUpUnclaimedReward;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.QuestUnclaimedReward;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.UnclaimedReward;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import me.zort.sqllib.SQLDatabaseConnection;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ProfileInventory {

    private static int getBonus(GamePlayer gamePlayer){
        int bonus;
        if (gamePlayer.getMetadata().get("quest_reward_bonus") == null) {
            List<Integer> percentages = Arrays.asList(5, 7, 10, 12, 15, 17, 20, 25, 30);
            for (Integer percent : percentages) {
                if (gamePlayer.getOnlinePlayer().hasPermission("vip.bonus" + percent)) {
                    gamePlayer.getMetadata().put("quest_reward_bonus", percent);
                    return percent;
                }
            }
        } else {
            return (int) gamePlayer.getMetadata().get("quest_reward_bonus");
        }
        return 0;
    }

    public static void openGUI(GamePlayer gamePlayer){
        GUI inventory = Component.gui()
                .title(net.kyori.adventure.text.Component.text("§f七七七七七七七七\uE000").font(Key.key("jsplugins", "guis")))
                .rows(5)
                .prepare((gui, player) -> {
                    PlayerData data = gamePlayer.getPlayerData();

                    ItemBuilder close = new ItemBuilder(Material.ECHO_SHARD);
                    close.setCustomModelData(1017);
                    close.hideAllFlags();
                    close.setName(MessageManager.get(player, "inventory.item.close")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                    info.setCustomModelData(1018);
                    info.hideAllFlags();
                    info.setName(MessageManager.get(player, "inventory.info_item.player_menu.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.player_menu.lore").getTranslated());

                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> {
                        gui.close(player);
                        player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());




                    ItemBuilder profile = new ItemBuilder(Utils.getPlayerHead(player));
                    profile.hideAllFlags();
                    profile.setName("§aMy Profile");
                    profile.addLoreLine("");
                    for (Resource resource : ResourcesManager.getResources()){
                        profile.addLoreLine("§7" + resource.getDisplayName() + ": §a" + StringUtils.betterNumberFormat(data.getBalance(resource)));
                    }
                    gui.appendElement(11, Component.element(profile.toItemStack()).addClick(i -> {

                    }).build());

                    int bonus = getBonus(gamePlayer);

                    //TODO: nezobrazovat pokud není LevelSystem
                    Optional<LevelUpUnclaimedReward> levelUpUnclaimedReward = gamePlayer.getPlayerData()
                            .getUnclaimedRewards(UnclaimedReward.Type.LEVELUP).stream()
                            .filter(r -> r instanceof LevelUpUnclaimedReward)
                            .map(r -> (LevelUpUnclaimedReward) r)
                            .findFirst();
                    LevelManager levelManager = Minigame.getInstance().getLevelManager();
                    LevelProgress levelProgress = levelManager.getLevelProgress(gamePlayer);
                    ItemBuilder level = new ItemBuilder(Material.ECHO_SHARD);
                    level.setCustomModelData((levelUpUnclaimedReward.map(LevelUpUnclaimedReward -> levelManager.getLevelEvolution(LevelUpUnclaimedReward.getLevel()).blinkingItemCustomModelData()).orElseGet(() -> levelManager.getLevelEvolution(data.getLevel()).itemCustomModelData())));
                    level.hideAllFlags();
                    level.setName(MessageManager.get(gamePlayer, "inventory.profile_menu.your_level.name").replace("%level%", "" + levelProgress.level()).getTranslated());
                    level.addLoreLine("");
                    level.addLoreLine(MessageManager.get(player, "inventory.profile_menu.your_level.your_level")
                            .replace("%level%", "" + (levelUpUnclaimedReward.map(LevelUpUnclaimedReward::getLevel).orElseGet(data::getLevel)))
                            .replace("%icon%", (levelUpUnclaimedReward.map(LevelUpUnclaimedReward -> levelManager.getLevelEvolution(LevelUpUnclaimedReward.getLevel()).getIcon()).orElseGet(() -> levelManager.getLevelEvolution(data.getLevel()).getIcon())))
                            .getTranslated());
                    level.addLoreLine("");
                    if (levelUpUnclaimedReward.isEmpty()) {
                        level.addLoreLine(Utils.getStringProgressBar(levelProgress.xpOnCurrentLevel(), levelProgress.levelRange().neededXP()));
                        level.addLoreLine(MessageManager.get(player, "inventory.profile_menu.your_level.progress")
                                .replace("%level%", "" + levelProgress.level() + 1)
                                .replace("%xp%", "" + levelProgress.xpOnCurrentLevel())
                                .replace("%needed_xp%", "" + levelProgress.levelRange().neededXP())
                                .getTranslated());
                    }else{
                        int neededXP = levelManager.getLevelRange(levelUpUnclaimedReward.get().getLevel()).neededXP();
                        level.addLoreLine(Utils.getStringProgressBar(neededXP, neededXP));
                        level.addLoreLine(MessageManager.get(player, "inventory.profile_menu.your_level.progress")
                                .replace("%level%", "" + levelProgress.level() + 1)
                                .replace("%xp%", "§a" + neededXP)
                                .replace("%needed_xp%", "" + neededXP)
                                .getTranslated());
                    }
                    level.addLoreLine("");
                    level.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.your_level.rewards").getTranslated());
                    if (levelUpUnclaimedReward.isEmpty()) {
                        for (RewardItem rewardItem : levelManager.getReward(levelProgress.level() + 1).getRewardItems()) {
                            Resource resource = rewardItem.getResource();
                            level.addLoreLine(" " + resource.getColor() + "+ " + (!rewardItem.randomAmount() ? rewardItem.getAmount() : rewardItem.getRandomMinRange() + "-" + rewardItem.getRandomMaxRange()) + " " + resource.getDisplayName() + (rewardItem.getChance() != 100 ? " §7(" + rewardItem.getChance() + "% " + LegacyComponentSerializer.legacySection().serialize(MessageManager.get(gamePlayer, "word.chance").getTranslated()) + ")" : ""));
                            if (bonus != 0 && resource.isApplicableBonus())
                                level.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.your_level.bonus")
                                        .replace("%bonus%", "" + bonus)
                                        .getTranslated());
                        }
                    }else{
                        for (RewardItem rewardItem : levelUpUnclaimedReward.get().getReward().getRewardItems()) {
                            Resource resource = rewardItem.getResource();
                            level.addLoreLine(" " + resource.getColor() + "+ " + (!rewardItem.randomAmount() ? rewardItem.getAmount() : rewardItem.getRandomMinRange() + "-" + rewardItem.getRandomMaxRange()) + " " + resource.getDisplayName() + (rewardItem.getChance() != 100 ? " §7(" + rewardItem.getChance() + "% " + LegacyComponentSerializer.legacySection().serialize(MessageManager.get(gamePlayer, "word.chance").getTranslated()) + ")" : ""));
                            if (bonus != 0 && resource.isApplicableBonus())
                                level.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.your_level.bonus")
                                        .replace("%bonus%", "" + bonus)
                                        .getTranslated());
                        }
                    }
                    LevelEvolution nextEvolution = levelManager.getNextEvolution(levelUpUnclaimedReward.map(LevelUpUnclaimedReward::getLevel).orElseGet(data::getLevel));
                    if (nextEvolution != null) {
                        level.addLoreLine("");
                        level.addLoreLine(MessageManager.get(player, "inventory.profile_menu.your_level.next_evolution")
                                .replace("%next_evolution%", net.kyori.adventure.text.Component.text(nextEvolution.startLevel() + "").append(nextEvolution.getIcon()))
                                .getTranslated());
                    }
                    if (levelUpUnclaimedReward.isPresent()) {
                        level.addLoreLine("");
                        level.addLoreLine(MessageManager.get(gamePlayer, "inventory.unclaimed_rewards.click_to_claim").getTranslated());
                    }
                    gui.appendElement(13, Component.element(level.toItemStack()).addClick(i -> {
                        levelUpUnclaimedReward.ifPresent(unclaimedReward -> {
                            MessageManager.get(gamePlayer, "chat.unclaimed_reward.levelup.claimed").send();
                            if (bonus != 0){
                                unclaimedReward.setBonus(bonus);
                            }
                            unclaimedReward.claim();
                            player.playSound(player, Sounds.LEVEL_UP.bukkitSound(), 1F, 1F);
                            openGUI(gamePlayer);
                        });
                    }).build());




                    Optional<DailyMeterUnclaimedReward> dailyMeterUnclaimedReward = gamePlayer.getPlayerData()
                            .getUnclaimedRewards(UnclaimedReward.Type.DAILYMETER).stream()
                            .filter(DailyMeterUnclaimedReward.class::isInstance)
                            .map(DailyMeterUnclaimedReward.class::cast)
                            .findFirst();

                    LocalDate today = LocalDate.now();
                    DailyMeter dailyMeter = Minigame.getInstance().getLevelManager().getDailyMeter();
                    DailyMeter.DailyMeterTier dailyMeterClaim = data.getDailyMeterTier();

                    ItemBuilder dailyRewardTrack = new ItemBuilder(Material.ECHO_SHARD);
                    dailyRewardTrack.setCustomModelData(dailyMeterUnclaimedReward.isPresent() ? 1053 : 1052);
                    dailyRewardTrack.hideAllFlags();
                    dailyRewardTrack.setName(MessageManager.get(player, "inventory.profile_menu.daily_reward_track.name").getTranslated());
                    dailyRewardTrack.addLoreLine("");
                    dailyRewardTrack.addLoreLine(MessageManager.get(player, "inventory.profile_menu.daily_reward_track.description").getTranslated());
                    if (dailyMeterClaim != null && dailyMeterClaim.tier() <= levelManager.getDailyMeter().getMaxTier()) {
                        dailyRewardTrack.addLoreLine("");
                        if (dailyMeterUnclaimedReward.isEmpty()) {
                            dailyRewardTrack.addLoreLine(Utils.getStringProgressBar(dailyMeter.getXpOnCurrentTier(gamePlayer), dailyMeterClaim.neededXP()));
                            dailyRewardTrack.addLoreLine(MessageManager.get(player, "inventory.profile_menu.daily_reward_track.progress")
                                    .replace("%xp%", "" + dailyMeter.getXpOnCurrentTier(gamePlayer))
                                    .replace("%needed_xp%", "" + dailyMeterClaim.neededXP())
                                    .getTranslated());
                        } else {
                            DailyMeter.DailyMeterTier unclaimedRewardTier = dailyMeter.getTiers().get(dailyMeterUnclaimedReward.get().getTier() - 1);
                            dailyRewardTrack.addLoreLine(Utils.getStringProgressBar(unclaimedRewardTier.neededXP(), unclaimedRewardTier.neededXP()));
                            dailyRewardTrack.addLoreLine(MessageManager.get(player, "inventory.profile_menu.daily_reward_track.progress")
                                    .replace("%xp%", "§a" + unclaimedRewardTier.neededXP())
                                    .replace("%needed_xp%", "" + unclaimedRewardTier.neededXP())
                                    .getTranslated());
                        }
                    }
                    if (dailyMeterClaim != null && dailyMeterClaim.tier() <= levelManager.getDailyMeter().getMaxTier()) {
                        dailyRewardTrack.addLoreLine("");
                        dailyRewardTrack.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_reward_track.rewards").getTranslated());
                        if (dailyMeterUnclaimedReward.isEmpty()) {
                            for (RewardItem rewardItem : dailyMeterClaim.reward().getRewardItems()) {
                                Resource resource = rewardItem.getResource();
                                dailyRewardTrack.addLoreLine(" " + resource.getColor() + "+ " + (!rewardItem.randomAmount() ? rewardItem.getAmount() : rewardItem.getRandomMinRange() + "-" + rewardItem.getRandomMaxRange()) + " " + resource.getDisplayName() + (rewardItem.getChance() != 100 ? " §7(" + rewardItem.getChance() + "% " + LegacyComponentSerializer.legacySection().serialize(MessageManager.get(gamePlayer, "word.chance").getTranslated()) + ")" : ""));
                                if (bonus != 0 && resource.isApplicableBonus()) {
                                    dailyRewardTrack.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_reward_track.bonus")
                                            .replace("%bonus%", "" + bonus)
                                            .getTranslated());
                                }
                            }
                        } else {
                            for (RewardItem rewardItem : dailyMeterUnclaimedReward.get().getReward().getRewardItems()) {
                                Resource resource = rewardItem.getResource();
                                dailyRewardTrack.addLoreLine(" " + resource.getColor() + "+ " + (!rewardItem.randomAmount() ? rewardItem.getAmount() : rewardItem.getRandomMinRange() + "-" + rewardItem.getRandomMaxRange()) + " " + resource.getDisplayName() + (rewardItem.getChance() != 100 ? " §7(" + rewardItem.getChance() + "% " + LegacyComponentSerializer.legacySection().serialize(MessageManager.get(gamePlayer, "word.chance").getTranslated()) + ")" : ""));
                                if (bonus != 0 && resource.isApplicableBonus()) {
                                    dailyRewardTrack.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_reward_track.bonus")
                                            .replace("%bonus%", "" + bonus)
                                            .getTranslated());
                                }
                            }
                        }
                    }
                    dailyRewardTrack.addLoreLine("");
                    if (dailyMeterClaim != null || dailyMeterUnclaimedReward.isPresent() ) {
                        if (dailyMeterUnclaimedReward.isEmpty() || !dailyMeterUnclaimedReward.get().getCreatedAt().toLocalDate().isBefore(today)){
                            dailyRewardTrack.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_reward_track.daily_claims")
                                    .replace("%claims%", "§f" + dailyMeterUnclaimedReward.map(unclaimedReward -> unclaimedReward.getTier() - 1).orElseGet(() -> dailyMeterClaim.tier() - 1))
                                    .getTranslated());
                        }else{
                            dailyRewardTrack.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_reward_track.old_daily_claim")
                                    .replace("%claim%", "" + dailyMeterUnclaimedReward.get().getTier())
                                    .replace("%date%", dailyMeterUnclaimedReward.get().getCreatedAt().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
                                    .getTranslated());
                        }
                    }else{
                        dailyRewardTrack.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_reward_track.daily_claims")
                                .replace("%claims%", "§a" + dailyMeter.getMaxTier())
                                .getTranslated());
                    }
                    if (dailyMeterUnclaimedReward.isEmpty() || !dailyMeterUnclaimedReward.get().getCreatedAt().toLocalDate().isBefore(today)) {
                        dailyRewardTrack.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_reward_track.resets_in")
                                .replace("%time%", StringUtils.getTimeLeftUntil(today.plusDays(1).atStartOfDay()))
                                .getTranslated());
                    }
                    if (dailyMeterClaim == null && dailyMeterUnclaimedReward.isEmpty()){
                        dailyRewardTrack.addLoreLine("");
                        dailyRewardTrack.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_reward_track.wait_for_reset").getTranslated());
                    }
                    if (dailyMeterUnclaimedReward.isPresent()) {
                        dailyRewardTrack.addLoreLine("");
                        dailyRewardTrack.addLoreLine(MessageManager.get(gamePlayer, "inventory.unclaimed_rewards.click_to_claim").getTranslated());
                    }
                    gui.appendElement(15, Component.element(dailyRewardTrack.toItemStack()).addClick(i -> {
                        dailyMeterUnclaimedReward.ifPresent(unclaimedReward -> {
                            MessageManager.get(gamePlayer, "chat.unclaimed_reward.daily_reward_track.claimed").send();
                            if (bonus != 0){
                                unclaimedReward.setBonus(bonus);
                            }
                            unclaimedReward.claim();
                            player.playSound(player, Sounds.LEVEL_UP.bukkitSound(), 1F, 1F);

                            if (!unclaimedReward.getCreatedAt().toLocalDate().isBefore(today)) {
                                if (dailyMeterClaim.tier() > gamePlayer.getPlayerData().getDailyRewardsClaims())
                                    gamePlayer.getPlayerData().setDailyRewardsClaims(dailyMeterClaim.tier());
                            }
                            openGUI(gamePlayer);
                        });
                    }).build());

                    gui.setContainer(28, Component.staticContainer()
                            .size(3, 2)
                            .init(container -> {
                                CosmeticsManager cosmeticsManager = Minigame.getInstance().getCosmeticsManager();
                                for (CosmeticsCategory category : cosmeticsManager.getCategories()){
                                    Cosmetic selectedCosmetic = cosmeticsManager.getSelectedCosmetic(gamePlayer, category);
                                    if (selectedCosmetic != null){
                                        Element element = Component.element(getCosmeticEditedItem(gamePlayer, selectedCosmetic)).addClick(i -> {
                                            CosmeticsInventory.openCategory(gamePlayer, category);
                                            player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 10.0F, 10.0F);
                                        }).build();
                                        container.appendElement(element);
                                    }else {
                                        ItemBuilder categoryItem = new ItemBuilder(Material.BARRIER);
                                        categoryItem.setName("§a" + category.getName());
                                        categoryItem.removeLore();
                                        categoryItem.hideAllFlags();

                                        categoryItem.addLoreLine("");
                                        MessageManager.get(player, "inventory.cosmetics.click_to_view")
                                                .addToItemLore(categoryItem);

                                        Element element = Component.element(categoryItem.toItemStack()).addClick(i -> {
                                            CosmeticsInventory.openCategory(gamePlayer, category);
                                            player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 10.0F, 10.0F);
                                        }).build();
                                        container.appendElement(element);
                                    }
                                }
                            }).build());


                    ItemBuilder quests = new ItemBuilder(Material.BOOK);
                    quests.hideAllFlags();
                    quests.setName("§aQuests");
                    quests.setLore(MessageManager.get(player, "inventory.profile_menu.quests.description").getTranslated());
                    quests.addLoreLine("");
                    quests.addLoreLine(MessageManager.get(player, "inventory.profile_menu.quests.click_to_view").getTranslated());
                    if (!data.getUnclaimedRewards(UnclaimedReward.Type.QUEST).isEmpty()){
                        quests.setCustomModelData(1010);
                    }
                    gui.appendElement(34, Component.element(quests.toItemStack()).addClick(i -> {
                        QuestInventory.openGUI(gamePlayer);
                    }).build());

                    ItemBuilder achievements = new ItemBuilder(Material.ECHO_SHARD);
                    achievements.setCustomModelData(1049);
                    achievements.hideAllFlags();
                    achievements.setName("§aAchievements");
                    achievements.setLore(MessageManager.get(player, "inventory.profile_menu.achievements.achievements_unlocked")
                            .replace("%unlocked%", "" + data.getAchievementData().stream().filter(achievementData -> achievementData.getStatus().equals(PlayerAchievementData.Status.UNLOCKED)).toList().size())
                            .getTranslated());
                    achievements.addLoreLine("");
                    //achievements.addLoreLine(MessageManager.get(player, "inventory.profile_menu.achievements.click_to_view").getTranslated());
                    achievements.addLoreLine(MessageManager.get(player, "coming_soon").getTranslated());
                    gui.appendElement(33, Component.element(achievements.toItemStack()).addClick(i -> {

                    }).build());

                    List<Language> languageList = Language.getLanguages();
                    languageList.sort(Comparator.comparing(Language::getName));

                    ItemBuilder languages = new ItemBuilder(Material.ECHO_SHARD);
                    languages.setCustomModelData(1051);
                    languages.hideAllFlags();
                    languages.setName("§aLanguages");
                    if (languageList.size() <= 5){
                        languages.addLoreLine("");
                        for (Language language : languageList){
                            languages.addLoreLine((data.getLanguage().equals(language) ? "§a" : "§7") + org.apache.commons.lang3.StringUtils.capitalize(language.getName()));
                        }
                    }else{
                        languages.addLoreLine(MessageManager.get(player, "inventory.profile_menu.language.selected_language")
                                .replace("%language%", org.apache.commons.lang3.StringUtils.capitalize(data.getLanguage().getName()))
                                .getTranslated());
                    }
                    languages.addLoreLine("");
                    languages.addLoreLine(MessageManager.get(player, "inventory.profile_menu.language.click_to_change").getTranslated());
                    gui.appendElement(32, Component.element(languages.toItemStack()).addClick(i -> {
                        if (languageList.size() <= 5){
                            int currentIndex = languageList.indexOf(gamePlayer.getLanguage());
                            Language nextLanguage = (currentIndex + 1 < languageList.size()) ? languageList.get(currentIndex + 1) : languageList.get(0);
                            gamePlayer.getGamePlayer().getPlayerData().setLanguage(nextLanguage, true);
                            openGUI(gamePlayer);
                            player.playSound(player, Sounds.LEVEL_UP.bukkitSound(), 1F, 1F);
                        }else{
                            player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
                        }
                    }).build());


                    //TODO: statistics
                    //TODO: udělat, že pravé kliknutí bude tato minihra a levé všechny minihry, možnost zakázat v configu nebo shift + kliknutí budou všechny minihry

                }).build();
        inventory.open(gamePlayer.getOnlinePlayer());

        inventory.onClose(GUI.CloseReason.BY_PLAYER, player -> {
            gamePlayer.getPlayerData().saveDailyRewardsClaims();
        });
        inventory.onClose(GUI.CloseReason.BY_METHOD, player -> {
            gamePlayer.getPlayerData().saveDailyRewardsClaims();
        });

    }


    private static ItemStack getCosmeticEditedItem(GamePlayer gamePlayer, Cosmetic cosmetic){
        PlayerData data = gamePlayer.getPlayerData();

        ItemBuilder item = new ItemBuilder(cosmetic.getIcon()/*cosmetic.hasPlayer(gamePlayer) ? cosmetic.getIcon() : new ItemBuilder(Material.ECHO_SHARD).setCustomModelData(1021).toItemStack()*/);
        item.setName("§a§l" + cosmetic.getName());
        item.removeLore();
        if (PlainTextComponentSerializer.plainText().serialize(MessageManager.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated()).length() == 1){
            item.addLoreLine(net.kyori.adventure.text.Component.text("§f").append(MessageManager.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated()));
        }
        item.addLoreLine("§8" + cosmetic.getCategory().getName());
        item.removeEnchantment(Enchantment.SHARPNESS);


        item.addLoreLine("");
        if (PlainTextComponentSerializer.plainText().serialize(MessageManager.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated()).length() > 1) {
            MessageManager.get(gamePlayer, "inventory.cosmetics.rarity")
                    .replace("%rarity%", MessageManager.get(gamePlayer, cosmetic.getRarity().getTranslateKey()).getTranslated())
                    .addToItemLore(item);
            item.addLoreLine("");
        }

        /*if (cosmetic.getLoreKey() != null && MessageManager.existMessage(cosmetic.getLoreKey())) {
            MessageManager.get(gamePlayer, cosmetic.getLoreKey())
                    .addToItemLore(item);
            item.addLoreLine("");
        }*/


        item.hideAllFlags();

        if (cosmetic.getPreviewConsumer() != null){
            MessageManager.get(gamePlayer, "inventory.cosmetics.click_to_change")
                    .addToItemLore(item);
        }

        return item.toItemStack();
    }
}
