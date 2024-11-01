package cz.johnslovakia.gameapi.guis;

import com.cryptomorin.xseries.XEnchantment;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.economy.Economy;
import cz.johnslovakia.gameapi.game.perk.Perk;
import cz.johnslovakia.gameapi.game.perk.PerkLevel;
import cz.johnslovakia.gameapi.game.perk.PerkManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.users.quests.PlayerQuestData;
import cz.johnslovakia.gameapi.utils.ItemBuilder;

import cz.johnslovakia.gameapi.utils.Sounds;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class PerksInventory {

    private static ItemStack getEditedItem(GamePlayer gamePlayer, Perk perk){
        PerkManager perkManager = GameAPI.getInstance().getPerkManager();
        Economy economy = perkManager.getEconomy();

        PlayerData data = gamePlayer.getPlayerData();
        PerkLevel currentLevel = data.getPerkLevel(perk);
        PerkLevel nextLevel = perkManager.getNextPlayerPerkLevel(gamePlayer, perk);
        int balance = data.getBalance(economy);
        boolean canUpgrade = nextLevel != null;

        ItemBuilder item = new ItemBuilder(perk.getIcon());
        item.hideAllFlags();

        item.removeEnchantment(XEnchantment.ARROW_INFINITE.getEnchant());
        item.setName((canUpgrade && balance <= nextLevel.price() ? "§c§l" : "§a§l") + perk.getName());
        item.removeLore();

        if (MessageManager.existMessage(perk.getTranslationKey())) {
            MessageManager.get(gamePlayer, perk.getTranslationKey())
                    .replace("%improvement_integer%", String.valueOf((currentLevel != null ? currentLevel.improvement() : 0)))
                    .addToItemLore(item);
            item.addLoreLine("");
        }
        for(PerkLevel level : perk.getLevels()){
            item.addLoreLine("§7" + level.level() + ". §8- " + (currentLevel != null && currentLevel == level ? "#71c900" : "§7") + level.improvement() + perk.getType().getString() + (currentLevel != null && currentLevel == level ? " §a(" + MessageManager.get(gamePlayer, "word.active").getTranslated() + ")" : ""));
        }
        item.addLoreLine("");
        if (canUpgrade) {
            MessageManager.get(gamePlayer, "inventory.perks.price")
                    .replace("%price%", "" + nextLevel.price())
                    .replace("%economy_name%", economy.getName())
                    .addToItemLore(item);
            if (balance <= nextLevel.price()){
                MessageManager.get(gamePlayer, "inventory.perks.dont_have_enough")
                        .replace("%economy_name%", economy.getName())
                        .addToItemLore(item);
            }else {
                if (currentLevel != null && currentLevel.level() != 0) {
                    MessageManager.get(gamePlayer, "inventory.perks.click_to_upgrade")
                            .replace("%economy_name%", economy.getName())
                            .addToItemLore(item);
                } else {
                    MessageManager.get(gamePlayer, "inventory.perks.click_to_purchase")
                            .replace("%economy_name%", economy.getName())
                            .addToItemLore(item);
                }
            }
        }else{
            MessageManager.get(gamePlayer, "inventory.perks.reached_max_level")
                    .addToItemLore(item);
        }

        return item.toItemStack();
    }

    public static void openGUI(GamePlayer gamePlayer){
        GUI inventory = Component.gui()
                .title("Perks")
                .rows(3)
                .prepare((gui, player) -> {
                    ItemBuilder close = new ItemBuilder(Material.MAP);
                    close.setCustomModelData(1010);
                    close.hideAllFlags();
                    close.setName(MessageManager.get(player, "inventory.item.close")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.MAP);
                    info.setCustomModelData(1010);
                    info.hideAllFlags();
                    info.setName(MessageManager.get(player, "inventory.info_item.perks_inventory.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.perks_inventory.lore").getTranslated());

                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> gui.close(player)).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());


                    gui.setContainer(9, Component.staticContainer()
                            .size(9, 2)
                            .init(container -> {
                                for (Perk perk : GameAPI.getInstance().getPerkManager().getPerks()){
                                    Element element = Component.element(getEditedItem(gamePlayer, perk)).addClick(i -> {
                                        perk.purchase(gamePlayer);

                                        player.closeInventory();
                                        player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 20.0F, 20.0F);
                                    }).build();

                                    gui.appendElement(element);
                                }
                            }).build());

                }).build();
        inventory.open(gamePlayer.getOnlinePlayer());

    }

}
