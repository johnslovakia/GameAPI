package cz.johnslovakia.gameapi.guis;

import com.cryptomorin.xseries.XEnchantment;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.economy.Economy;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.quests.PlayerQuestData;
import cz.johnslovakia.gameapi.users.quests.Quest;
import cz.johnslovakia.gameapi.users.quests.QuestType;
import cz.johnslovakia.gameapi.utils.ItemBuilder;

import cz.johnslovakia.gameapi.utils.Sounds;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

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
            item.addLoreLine(MessageManager.get(gamePlayer, "inventory.quests.progress").replace("%progress%", (completed ? "#71c900" : "§f") + data.getQuestData(quest).getProgress() + "§8/§7" + quest.getCompletionGoal()).getTranslated());
        }
        item.addLoreLine("");
        item.addLoreLine(MessageManager.get(gamePlayer, "inventory.quests.rewards").getTranslated());
        for (Economy economy : quest.getRewards().keySet()) {
            item.addLoreLine(" " + economy.getChatColor() + "+" + quest.getRewards().get(economy) + " §7" + economy.getName());
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
                    ItemBuilder close = new ItemBuilder(Material.MAP);
                    close.setCustomModelData(1010);
                    close.hideAllFlags();
                    close.setName(MessageManager.get(player, "inventory.item.close")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.MAP);
                    info.setCustomModelData(1010);
                    info.hideAllFlags();
                    info.setName(MessageManager.get(player, "inventory.info_item.quests_inventory.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.quests_inventory.lore").getTranslated());

                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> gui.close(player)).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());

                    int slot = 11;
                    for (Quest quest : data.getQuests()) {
                        Element element = Component.element(getEditedItem(gamePlayer, quest)).addClick(i -> {
                            if (data.getQuestData(quest).isCompleted()){
                                MessageManager.get(player, "chat.quests.already_completed")
                                        .send();
                            }else if (data.getQuestData(quest).getStatus().equals(PlayerQuestData.Status.IN_PROGRESS)){
                                MessageManager.get(player, "chat.quests.already_started")
                                        .send();
                            }else{
                                data.getQuestData(quest).setStatus(PlayerQuestData.Status.IN_PROGRESS);
                                MessageManager.get(player, "chat.quests.started")
                                        .replace("%name%", quest.getDisplayName())
                                        .send();

                                openGUI(gamePlayer);
                            }
                            player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 20.0F, 20.0F);
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
