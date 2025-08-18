package cz.johnslovakia.gameapi.guis;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.datastorage.PlayerTable;
import cz.johnslovakia.gameapi.levelSystem.*;
import cz.johnslovakia.gameapi.messages.Language;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.achievements.PlayerAchievementData;
import cz.johnslovakia.gameapi.users.quests.QuestType;
import cz.johnslovakia.gameapi.users.resources.Resource;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
                .title(net.kyori.adventure.text.Component.text("§f七七七七七七七七").font(Key.key("jsplugins", "guis")))
                .rows(3)
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
                    info.setName(MessageManager.get(player, "inventory.info_item.perks_inventory.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.perks_inventory.lore").getTranslated());

                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> {
                        gui.close(player);
                        player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());


                    ItemBuilder playerInfo = new ItemBuilder(Material.PLAYER_HEAD);
                    playerInfo.hideAllFlags();


                    ItemBuilder profile = new ItemBuilder(Material.BOOK);
                    profile.hideAllFlags();
                    profile.setName("§aMy Profile");
                    profile.addLoreLine("");
                    for (Resource resource : Resource.getResources()){
                        profile.addLoreLine("§7" + resource.getDisplayName() + ": §a" + data.getBalance(resource));
                    }
                    gui.appendElement(9, Component.element(profile.toItemStack()).addClick(i -> {

                    }).build());

                    ItemBuilder quests = new ItemBuilder(Material.BOOK);
                    quests.hideAllFlags();
                    quests.setName("§aQuests");
                    quests.setLore(MessageManager.get(player, "inventory.profile_menu.quests.description").getTranslated());
                    quests.addLoreLine("");
                    quests.addLoreLine(MessageManager.get(player, "inventory.profile_menu.quests.click_to_view").getTranslated());
                    gui.appendElement(10, Component.element(quests.toItemStack()).addClick(i -> {

                    }).build());

                    ItemBuilder achievements = new ItemBuilder(Material.BOOK);
                    achievements.hideAllFlags();
                    achievements.setName("§aAchievements");
                    achievements.setLore(MessageManager.get(player, "inventory.profile_menu.achievements.achievements_unlocked")
                            .replace("%unlocked%", "" + data.getAchievementData().stream().filter(achievementData -> achievementData.getStatus().equals(PlayerAchievementData.Status.UNLOCKED)).toList().size())
                            .getTranslated());
                    achievements.addLoreLine("");
                    achievements.addLoreLine(MessageManager.get(player, "inventory.profile_menu.achievements.click_to_view").getTranslated());
                    gui.appendElement(11, Component.element(achievements.toItemStack()).addClick(i -> {

                    }).build());

                    List<Language> languageList = Language.getLanguages();
                    languageList.sort(Comparator.comparing(Language::getName));

                    ItemBuilder languages = new ItemBuilder(Material.BOOK);
                    languages.hideAllFlags();
                    languages.setName("§aLanguages");
                    if (languageList.size() <= 3){
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
                    gui.appendElement(12, Component.element(languages.toItemStack()).addClick(i -> {
                        if (languageList.size() <= 3){
                            int currentIndex = languageList.indexOf(gamePlayer.getLanguage());
                            Language nextLanguage = (currentIndex + 1 < languageList.size()) ? languageList.get(currentIndex + 1) : languageList.get(0);
                            gamePlayer.getGamePlayer().getPlayerData().setLanguage(nextLanguage, true);
                            openGUI(gamePlayer);
                            player.playSound(player, Sounds.LEVEL_UP.bukkitSound(), 1F, 1F);
                        }else{
                            player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
                        }
                    }).build());

                    int bonus = getBonus(gamePlayer);

                    //TODO: nezobrazovat pokud není LevelSystem
                    //TODO: může jich být více, seřazení
                    Optional<LevelUpUnclaimedReward> levelUpUnclaimedReward = gamePlayer.getPlayerData()
                            .getUnclaimedRewards(UnclaimedReward.Type.LEVELUP).stream()
                            .filter(r -> r instanceof LevelUpUnclaimedReward)
                            .map(r -> (LevelUpUnclaimedReward) r)
                            .findFirst();
                    LevelManager levelManager = Minigame.getInstance().getLevelManager();
                    LevelProgress levelProgress = levelManager.getLevelProgress(gamePlayer);
                    ItemBuilder level = new ItemBuilder(Material.BOOK);
                    level.hideAllFlags();
                    level.setName("§aLevel");
                    level.addLoreLine("");
                    level.addLoreLine(MessageManager.get(player, "inventory.profile_menu.your_level.your_level")
                            .replace("%level%", "" + (levelUpUnclaimedReward.map(LevelUpUnclaimedReward::getLevel).orElseGet(data::getLevel)))
                            .replace("%icon%", (levelUpUnclaimedReward.map(LevelUpUnclaimedReward -> levelManager.getLevelEvolution(LevelUpUnclaimedReward.getLevel()).getIcon()).orElseGet(() -> levelManager.getLevelEvolution(data.getLevel()).getIcon())))
                            .getTranslated());
                    if (levelUpUnclaimedReward.isEmpty()) {
                        level.addLoreLine(Utils.getStringProgressBar(levelProgress.xpOnCurrentLevel(), levelProgress.levelRange().neededXP()));
                        level.addLoreLine(MessageManager.get(player, "inventory.profile_menu.your_level.progress")
                                .replace("%xp%", "" + levelProgress.xpOnCurrentLevel())
                                .replace("%needed_xp%", "" + levelProgress.levelRange().neededXP())
                                .getTranslated());
                    }else{
                        int neededXP = levelManager.getLevelRange(levelUpUnclaimedReward.get().getLevel()).neededXP();
                        level.addLoreLine(Utils.getStringProgressBar(neededXP, neededXP));
                        level.addLoreLine(MessageManager.get(player, "inventory.profile_menu.your_level.progress")
                                .replace("%xp%", "§a" + neededXP)
                                .replace("%needed_xp%", "" + neededXP)
                                .getTranslated());
                    }
                    level.addLoreLine("");
                    level.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.your_level.rewards").getTranslated());
                    if (levelUpUnclaimedReward.isEmpty()) {
                        for (RewardItem rewardItem : levelManager.getReward(data.getLevel()).getRewardItems()) {
                            Resource resource = rewardItem.getResource();
                            level.addLoreLine(" " + resource.getColor() + "+ " + (!rewardItem.randomAmount() ? rewardItem.getAmount() : rewardItem.getRandomMinRange() + "-" + rewardItem.getRandomMaxRange()) + " " + resource.getDisplayName());
                        }
                    }else{
                        for (RewardItem rewardItem : levelUpUnclaimedReward.get().getReward().getRewardItems()) {
                            Resource resource = rewardItem.getResource();
                            level.addLoreLine(" " + resource.getColor() + "+ " + (!rewardItem.randomAmount() ? rewardItem.getAmount() : rewardItem.getRandomMinRange() + "-" + rewardItem.getRandomMaxRange()) + " " + resource.getDisplayName());
                        }
                    }
                    if (bonus != 0){
                        level.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.your_level.bonus")
                                .replace("%bonus%", "" + bonus)
                                .getTranslated());
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

                    ItemBuilder dailyRewards = new ItemBuilder(Material.BOOK);
                    dailyRewards.hideAllFlags();
                    dailyRewards.setName(MessageManager.get(player, "inventory.profile_menu.daily_rewards.name").getTranslated());
                    dailyRewards.addLoreLine("");
                    dailyRewards.addLoreLine(MessageManager.get(player, "inventory.profile_menu.daily_rewards.description").getTranslated());
                    if (dailyMeterClaim != null && dailyMeterClaim.tier() <= levelManager.getDailyMeter().getMaxTier()) {
                        dailyRewards.addLoreLine("");
                        if (dailyMeterUnclaimedReward.isEmpty()) {
                            dailyRewards.addLoreLine(Utils.getStringProgressBar(dailyMeter.getXpOnCurrentTier(gamePlayer), dailyMeterClaim.neededXP()));
                            dailyRewards.addLoreLine(MessageManager.get(player, "inventory.profile_menu.daily_rewards.progress")
                                    .replace("%xp%", "" + dailyMeter.getXpOnCurrentTier(gamePlayer))
                                    .replace("%needed_xp%", "" + dailyMeterClaim.neededXP())
                                    .getTranslated());
                        } else {
                            DailyMeter.DailyMeterTier unclaimedRewardTier = dailyMeter.getTiers().get(dailyMeterUnclaimedReward.get().getTier() - 1);
                            dailyRewards.addLoreLine(Utils.getStringProgressBar(unclaimedRewardTier.neededXP(), unclaimedRewardTier.neededXP()));
                            dailyRewards.addLoreLine(MessageManager.get(player, "inventory.profile_menu.daily_rewards.progress")
                                    .replace("%xp%", "§a" + unclaimedRewardTier.neededXP())
                                    .replace("%needed_xp%", "" + unclaimedRewardTier.neededXP())
                                    .getTranslated());
                        }
                    }
                    if (dailyMeterClaim != null && dailyMeterClaim.tier() <= levelManager.getDailyMeter().getMaxTier()) {
                        dailyRewards.addLoreLine("");
                        dailyRewards.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_rewards.rewards").getTranslated());
                        if (dailyMeterUnclaimedReward.isEmpty()) {
                            for (RewardItem rewardItem : dailyMeterClaim.reward().getRewardItems()) {
                                Resource resource = rewardItem.getResource();
                                dailyRewards.addLoreLine(" " + resource.getColor() + "+ " + (!rewardItem.randomAmount() ? rewardItem.getAmount() : rewardItem.getRandomMinRange() + "-" + rewardItem.getRandomMaxRange()) + " " + resource.getDisplayName());
                            }
                        } else {
                            for (RewardItem rewardItem : dailyMeterUnclaimedReward.get().getReward().getRewardItems()) {
                                Resource resource = rewardItem.getResource();
                                dailyRewards.addLoreLine(" " + resource.getColor() + "+ " + (!rewardItem.randomAmount() ? rewardItem.getAmount() : rewardItem.getRandomMinRange() + "-" + rewardItem.getRandomMaxRange()) + " " + resource.getDisplayName());
                            }
                        }
                        if (bonus != 0) {
                            dailyRewards.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_rewards.bonus")
                                    .replace("%bonus%", "" + bonus)
                                    .getTranslated());
                        }
                    }
                    dailyRewards.addLoreLine("");
                    if (dailyMeterUnclaimedReward.isEmpty() || !dailyMeterUnclaimedReward.get().getCreatedAt().toLocalDate().isBefore(today)) {
                        dailyRewards.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_rewards.resets_in")
                                .replace("%time%", StringUtils.getTimeLeftUntil(today.plusDays(1).atStartOfDay()))
                                .getTranslated());
                    }
                    if (dailyMeterClaim != null || dailyMeterUnclaimedReward.isPresent() ) {
                        if (dailyMeterUnclaimedReward.isEmpty() || !dailyMeterUnclaimedReward.get().getCreatedAt().toLocalDate().isBefore(today)){
                            dailyRewards.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_rewards.daily_claims")
                                    .replace("%claims%", "§f" + dailyMeterUnclaimedReward.map(unclaimedReward -> unclaimedReward.getTier() - 1).orElseGet(() -> dailyMeterClaim.tier() - 1))
                                    .getTranslated());
                        }else{
                            dailyRewards.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_rewards.old_daily_claim")
                                    .replace("%claim%", "" + dailyMeterUnclaimedReward.get().getTier())
                                    .replace("%date%", dailyMeterUnclaimedReward.get().getCreatedAt().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
                                    .getTranslated());
                        }
                    }else{
                        dailyRewards.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_rewards.daily_claims")
                                .replace("%claims%", "§a" + dailyMeter.getMaxTier())
                                .getTranslated());
                    }
                    if (dailyMeterClaim == null && dailyMeterUnclaimedReward.isEmpty()){
                        dailyRewards.addLoreLine("");
                        dailyRewards.addLoreLine(MessageManager.get(gamePlayer, "inventory.profile_menu.daily_rewards.wait_for_reset").getTranslated());
                    }
                    if (dailyMeterUnclaimedReward.isPresent()) {
                        dailyRewards.addLoreLine("");
                        dailyRewards.addLoreLine(MessageManager.get(gamePlayer, "inventory.unclaimed_rewards.click_to_claim").getTranslated());
                    }
                    gui.appendElement(14, Component.element(dailyRewards.toItemStack()).addClick(i -> {
                        dailyMeterUnclaimedReward.ifPresent(unclaimedReward -> {
                            MessageManager.get(gamePlayer, "chat.unclaimed_reward.daily_rewards.claimed").send();
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
}
