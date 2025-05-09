package cz.johnslovakia.gameapi.guis;

import com.cryptomorin.xseries.XEnchantment;
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
import cz.johnslovakia.gameapi.utils.rewards.RewardItem;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;

public class QuestInventory {

    private static ItemStack getEditedItem(GamePlayer gamePlayer, Quest quest){
        PlayerData data = gamePlayer.getPlayerData();
        boolean in_progress = data.getQuestData(quest).getStatus().equals(PlayerQuestData.Status.IN_PROGRESS);
        boolean completed = data.getQuestData(quest).isCompleted();

        ItemBuilder item = new ItemBuilder((completed ? Material.MAP : Material.PAPER));
        item.setName(MessageManager.get(gamePlayer, "inventory.quests.name")
                .replace("%type%", MessageManager.get(gamePlayer, "quest_type." + quest.getType().toString().toLowerCase()).getTranslated())
                .replace("%name%", quest.getDisplayName())
                .getTranslated());
        item.removeLore();
        if (!completed && in_progress) {
            item.addEnchant(XEnchantment.DAMAGE_ALL.getEnchant(), 1);
        }
        item.hideAllFlags();

        item.addLoreLine("§7" + MessageManager.get(gamePlayer, quest.getTranslationKey()).getTranslated());
        if (in_progress || completed) {
            item.addLoreLine(MessageManager.get(gamePlayer, "inventory.quests.progress")
                    .replace("%progress%", (completed ? "#71c900" : "§f") + data.getQuestData(quest).getProgress() + "§8/§7" + quest.getCompletionGoal()).getTranslated());
        }
        item.addLoreLine("");
        item.addLoreLine(MessageManager.get(gamePlayer, "inventory.quests.rewards").getTranslated());
        for (RewardItem rewardItem : quest.getReward().getRewardItems()) {
            Resource resource = rewardItem.getResource();
            item.addLoreLine(" " + resource.getChatColor() + "+" + (!rewardItem.randomAmount() ? rewardItem.getAmount() : rewardItem.getRandomMinRange() + " - " + rewardItem.getRandomMaxRange()) + " §7" + resource.getName());
        }
        item.addLoreLine("");
        if (quest.getType() == QuestType.DAILY) {
            item.addLoreLine(MessageManager.get(gamePlayer, "inventory.quests.daily_info").getTranslated());
        } else {
            item.addLoreLine(MessageManager.get(gamePlayer, "inventory.quests.weekly_info").getTranslated());
        }
        item.addLoreLine("");
        if (completed) {
            item.addLoreLine(MessageManager.get(gamePlayer, "inventory.quests.completed").getTranslated());
        } else if (in_progress){
            item.addLoreLine(MessageManager.get(gamePlayer, "inventory.quests.started").getTranslated());
        }else{
            item.addLoreLine(MessageManager.get(gamePlayer, "inventory.quests.click_to_start").getTranslated());
        }

        return item.toItemStack();
    }

    public static void openGUI(GamePlayer gamePlayer){
        PlayerData data = gamePlayer.getPlayerData();

        GUI inventory = Component.gui()
                .title("§f七七七七七七七七ㆼ")
                .rows(2)
                .prepare((gui, player) -> {
                    ItemBuilder close = new ItemBuilder(Material.ECHO_SHARD);
                    close.setCustomModelData(1017);
                    close.hideAllFlags();
                    close.setName(MessageManager.get(player, "inventory.item.close")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                    info.setCustomModelData(1018);
                    info.hideAllFlags();
                    info.setName(MessageManager.get(player, "inventory.info_item.quests_inventory.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.quests_inventory.lore").getTranslated());

                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> {
                        gui.close(player);
                        player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());

                    List<Quest> sorted = data.getQuests().stream()
                            .sorted(Comparator.comparing(q -> q.getType() == QuestType.WEEKLY))
                            .toList();

                    int slot = 11;
                    for (Quest quest : sorted) {
                        Element element = Component.element(getEditedItem(gamePlayer, quest)).addClick(i -> {
                            if (data.getQuestData(quest).isCompleted()){
                                MessageManager.get(player, "chat.quests.already_completed")
                                        .send();
                                player.playSound(player, Sounds.ANVIL_BREAK.bukkitSound(), 1F, 1F);
                            }else if (data.getQuestData(quest).getStatus().equals(PlayerQuestData.Status.IN_PROGRESS)){
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
                            }
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
