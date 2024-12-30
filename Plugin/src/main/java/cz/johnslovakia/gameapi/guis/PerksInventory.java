package cz.johnslovakia.gameapi.guis;

import com.cryptomorin.xseries.XEnchantment;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.game.perk.Perk;
import cz.johnslovakia.gameapi.game.perk.PerkLevel;
import cz.johnslovakia.gameapi.game.perk.PerkManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.ItemBuilder;

import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class PerksInventory {

    private static ItemStack getEditedItem(GamePlayer gamePlayer, Perk perk){
        PerkManager perkManager = GameAPI.getInstance().getPerkManager();
        Resource resource = perkManager.getResource();

        PlayerData data = gamePlayer.getPlayerData();
        PerkLevel currentLevel = data.getPerkLevel(perk);
        PerkLevel nextLevel = perkManager.getNextPlayerPerkLevel(gamePlayer, perk);
        int balance = data.getBalance(resource);
        boolean canUpgrade = nextLevel != null;

        ItemBuilder item = new ItemBuilder(perk.getIcon());
        item.hideAllFlags();

        item.removeEnchantment(XEnchantment.ARROW_INFINITE.getEnchant());
        item.setName((canUpgrade && balance <= nextLevel.price() ? "§c§l" : "§a§l") + perk.getName() + " §8(" + Objects.requireNonNullElse(currentLevel, 0) + "/" + perk.getLevels().size() + ")");
        item.removeLore();

        if (MessageManager.existMessage(perk.getTranslationKey())) {
            MessageManager.get(gamePlayer, perk.getTranslationKey())
                    .replace("%improvement_integer%", String.valueOf((currentLevel != null ? currentLevel.improvement() : 0)))
                    .addToItemLore(item);
            item.addLoreLine("");
        }
        for(PerkLevel level : perk.getLevels()){
            item.addLoreLine("§7" + level.level() + ". §8- " + (currentLevel != null && currentLevel.level() == level.level() ? "#71c900" : "§7") + level.improvement() + perk.getType().getString() + (currentLevel != null && currentLevel.level() == level.level() ? " §a(" + MessageManager.get(gamePlayer, "word.active").getTranslated() + ")" : ""));
        }
        item.addLoreLine("");
        if (canUpgrade) {
            MessageManager.get(gamePlayer, "inventory.perks.price")
                    .replace("%balance%", (balance >= nextLevel.price() ? "§a" : "§c") + StringUtils.betterNumberFormat(balance))
                    .replace("%price%", StringUtils.betterNumberFormat(nextLevel.price()))
                    .replace("%economy_name%", resource.getName())
                    .addToItemLore(item);
            if (balance <= nextLevel.price()){
                MessageManager.get(gamePlayer, "inventory.perks.dont_have_enough")
                        .replace("%economy_name%", resource.getName())
                        .addToItemLore(item);
            }else {
                if (currentLevel != null && currentLevel.level() != 0) {
                    MessageManager.get(gamePlayer, "inventory.perks.click_to_upgrade")
                            .replace("%economy_name%", resource.getName())
                            .addToItemLore(item);
                } else {
                    MessageManager.get(gamePlayer, "inventory.perks.click_to_purchase")
                            .replace("%economy_name%", resource.getName())
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
                .title("§f七七七七七七七七ㆻ")
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
                    info.setName(MessageManager.get(player, "inventory.info_item.perks_inventory.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.perks_inventory.lore").getTranslated());

                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> {
                        gui.close(player);
                        player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());


                    gui.setContainer(9, Component.staticContainer()
                            .size(9, 1)
                            .init(container -> {
                                for (Perk perk : GameAPI.getInstance().getPerkManager().getPerks()){
                                    Element element = Component.element(getEditedItem(gamePlayer, perk)).addClick(i -> {
                                        perk.purchase(gamePlayer);
                                        player.closeInventory();
                                    }).build();

                                    container.appendElement(element);
                                }
                            }).build());

                }).build();
        inventory.open(gamePlayer.getOnlinePlayer());

    }

}
