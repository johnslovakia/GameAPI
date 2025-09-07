package cz.johnslovakia.gameapi.guis;

import com.cryptomorin.xseries.XEnchantment;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.quests.PlayerQuestData;
import cz.johnslovakia.gameapi.users.quests.Quest;
import cz.johnslovakia.gameapi.users.quests.QuestType;
import cz.johnslovakia.gameapi.utils.ItemBuilder;

import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;
import cz.johnslovakia.gameapi.utils.Utils;
import cz.johnslovakia.gameapi.utils.rewards.RewardItem;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.DailyMeterUnclaimedReward;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.QuestUnclaimedReward;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.UnclaimedReward;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class QuestInventory {

    private static ItemStack getEditedItem(GamePlayer gamePlayer, Quest quest){
        PlayerData data = gamePlayer.getPlayerData();
        boolean in_progress = data.getQuestData(quest).getStatus().equals(PlayerQuestData.Status.IN_PROGRESS);
        boolean completed = data.getQuestData(quest).isCompleted();
        int bonus = getBonus(gamePlayer);

        LocalDate today = LocalDate.now();
        LocalDate nextMonday = today.with(DayOfWeek.MONDAY);
        if (!today.isBefore(nextMonday)) {
            nextMonday = nextMonday.plusWeeks(1);
        }
        LocalDateTime nextMondayStart = nextMonday.atStartOfDay();


        Optional<QuestUnclaimedReward> unclaimedReward = gamePlayer.getPlayerData()
                .getUnclaimedRewards(UnclaimedReward.Type.QUEST).stream()
                .filter(QuestUnclaimedReward.class::isInstance)
                .map(QuestUnclaimedReward.class::cast)
                .filter(r -> r.getQuestType().equals(quest.getType())
                        && r.getQuestName().equalsIgnoreCase(quest.getName()))
                .findFirst();


        ItemBuilder item = new ItemBuilder(Material.ECHO_SHARD);
        if (!completed){
            item.setCustomModelData(1023);
        }else if (unclaimedReward.isPresent()){
            item.setCustomModelData(1024);
        }else{
            item = new ItemBuilder(Material.BARRIER); //TODO: item pro dokončený quest
        }

        item.setName(MessageManager.get(gamePlayer, "inventory.quests.name")
                .replace("%type%", MessageManager.get(gamePlayer, "quest_type." + quest.getType().toString().toLowerCase()).getTranslated())
                .replace("%name%", quest.getDisplayName())
                .getTranslated());
        item.removeLore();
        /*if (!completed && in_progress) {
            item.addEnchant(Enchantment.SHARPNESS, 1);
        }*/
        item.hideAllFlags();

        item.addLoreLine("");
        item.addLoreLine(net.kyori.adventure.text.Component.text("§7").append(MessageManager.get(gamePlayer, quest.getTranslationKey()).getTranslated()));
        item.addLoreLine("");
        item.addLoreLine(Utils.getStringProgressBar(data.getQuestData(quest).getProgress(), quest.getCompletionGoal()));
        item.addLoreLine(MessageManager.get(gamePlayer, "inventory.quests.progress")
                .replace("%progress%", (completed ? "#71c900" : "§f") + data.getQuestData(quest).getProgress() + "§8/§7" + quest.getCompletionGoal()).getTranslated());
        item.addLoreLine("");
        item.addLoreLine(MessageManager.get(gamePlayer, "inventory.quests.rewards").getTranslated());
        for (RewardItem rewardItem : quest.getReward().getRewardItems()) {
            Resource resource = rewardItem.getResource();
            item.addLoreLine(" " + resource.getColor() + "+ " + (!rewardItem.randomAmount() ? rewardItem.getAmount() : rewardItem.getRandomMinRange() + "-" + rewardItem.getRandomMaxRange()) + " " + resource.getDisplayName()  + (rewardItem.getChance() != 100 ? " §7(" + rewardItem.getChance() + "% " + LegacyComponentSerializer.legacySection().serialize(MessageManager.get(gamePlayer, "word.chance").getTranslated()) + ")" : ""));
            if (bonus != 0 && resource.isApplicableBonus()){
                item.addLoreLine(MessageManager.get(gamePlayer, "inventory.quests.bonus")
                        .replace("%bonus%", "" + bonus)
                        .getTranslated());
            }
        }

        if (unclaimedReward.isEmpty() || (quest.getType() == QuestType.DAILY && !unclaimedReward.get().getCreatedAt().toLocalDate().isBefore(today))
        || (quest.getType() == QuestType.WEEKLY && !unclaimedReward.get().getCreatedAt().toLocalDate().equals(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))))) {
            item.addLoreLine("");
            MessageManager.get(gamePlayer, "inventory.quests.resets_in")
                    .replace("%time%", (quest.getType().equals(QuestType.WEEKLY) ? StringUtils.getTimeLeftUntil(nextMondayStart) : StringUtils.getTimeLeftUntil(LocalDate.now().plusDays(1).atStartOfDay())))
                    .addToItemLore(item);
        }

        item.addLoreLine("");
        if (completed) {
            if (unclaimedReward.isPresent()) {
                MessageManager.get(gamePlayer, "inventory.unclaimed_rewards.click_to_claim").addToItemLore(item);
            }else{
                MessageManager.get(gamePlayer, "inventory.quests.completed").addToItemLore(item);
            }
        }else{
            MessageManager.get(gamePlayer, "inventory.quests.currently_active").addToItemLore(item);
        }

        return item.toItemStack();
    }

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
        Minigame.getInstance().getQuestManager().check(gamePlayer);

        PlayerData data = gamePlayer.getPlayerData();

        int bonus = getBonus(gamePlayer);


        GUI inventory = Component.gui()
                .title(net.kyori.adventure.text.Component.text("§f七七七七七七七七ㆼ").font(Key.key("jsplugins", "guis")))
                .rows(2)
                .prepare((gui, player) -> {
                    ItemBuilder back = new ItemBuilder(Material.ECHO_SHARD);
                    back.setCustomModelData(1016);
                    back.hideAllFlags();
                    back.setName(MessageManager.get(player, "inventory.item.go_back")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                    info.setCustomModelData(1018);
                    info.hideAllFlags();
                    info.setName(MessageManager.get(player, "inventory.info_item.quests_inventory.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.quests_inventory.lore").getTranslated());

                    gui.appendElement(0, Component.element(back.toItemStack()).addClick(i -> {
                        ProfileInventory.openGUI(gamePlayer);
                        player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());

                    List<Quest> sorted = data.getQuests().stream()
                            .sorted(Comparator.comparing(q -> q.getType() == QuestType.WEEKLY))
                            .toList();

                    int slot = 11;
                    for (Quest quest : sorted) {
                        Optional<QuestUnclaimedReward> unclaimedReward = gamePlayer.getPlayerData()
                                .getUnclaimedRewards(UnclaimedReward.Type.QUEST).stream()
                                .filter(QuestUnclaimedReward.class::isInstance)
                                .map(QuestUnclaimedReward.class::cast)
                                .filter(r -> r.getQuestType().equals(quest.getType())
                                        && r.getQuestName().equalsIgnoreCase(quest.getName()))
                                .findFirst();

                        Element element = Component.element(getEditedItem(gamePlayer, quest)).addClick(i -> {
                            if (data.getQuestData(quest).isCompleted()){

                                if (unclaimedReward.isPresent()){
                                    MessageManager.get(gamePlayer, "chat.unclaimed_reward.quest.claimed").send();
                                    if (bonus != 0) {
                                        unclaimedReward.get().setBonus(bonus);
                                    }
                                    unclaimedReward.get().claim();
                                    //Minigame.getInstance().getQuestManager().shouldReset(gamePlayer.getPlayerData().getQuestData(quest));
                                    openGUI(gamePlayer);
                                }else {
                                    MessageManager.get(player, "chat.quests.already_completed")
                                            .send();
                                }
                                player.playSound(player, Sounds.ANVIL_BREAK.bukkitSound(), 1F, 1F);
                            }/*else if (data.getQuestData(quest).getStatus().equals(PlayerQuestData.Status.IN_PROGRESS)){
                                MessageManager.get(player, "chat.quests.already_started")
                                        .replace("%progress%", data.getQuestData(quest).getProgress() + "/" + quest.getCompletionGoal())
                                        .send();
                                player.playSound(player, Sounds.ANVIL_BREAK.bukkitSound(), 1F, 1F);
                            }else{
                                data.getQuestData(quest).setStatus(PlayerQuestData.Status.IN_PROGRESS);
                                MessageManager.get(player, "chat.quests.started")
                                        .replace("%type%", StringUtils.stylizeText(quest.getType().name()))
                                        .replace("%name%", quest.getDisplayName())
                                        .send();
                                player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 1F, 1F);
                                openGUI(gamePlayer);
                            }*/
                        }).build();

                        gui.appendElement(slot, element);
                        if (slot == 12){
                            slot += 2;
                        }else {
                            slot++;
                        }
                    }
                }).build();

        inventory.open(gamePlayer.getOnlinePlayer());
    }
}
