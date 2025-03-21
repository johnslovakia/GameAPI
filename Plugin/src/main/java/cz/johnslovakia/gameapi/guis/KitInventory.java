package cz.johnslovakia.gameapi.guis;

import com.cryptomorin.xseries.XEnchantment;
import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.kit.Kit;
import cz.johnslovakia.gameapi.game.kit.KitManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.ItemBuilder;
import cz.johnslovakia.gameapi.utils.Logger;
import cz.johnslovakia.gameapi.utils.Sounds;
import cz.johnslovakia.gameapi.utils.StringUtils;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class KitInventory implements Listener {

    public static void openKitInventory(GamePlayer gamePlayer) {
        KitManager kitManager = KitManager.getKitManager(gamePlayer.getPlayerData().getGame());

        int size = kitManager.getKits().size();
        char sizeChar = (size <= 9 ? 'ㆺ' : 'ẙ'/*'Ẕ'*/);
        GUI inventory = Component.gui()
                .title("§f七七七七七七七七" + sizeChar)
                .rows(size <= 9 ? 3 : 4)
                .prepare((gui, player) -> {
                    PlayerData playerData = gamePlayer.getPlayerData();
                    Game game = playerData.getGame();

                    Resource resource = kitManager.getResource();
                    int balance = playerData.getBalance(resource);

                    ItemBuilder close = new ItemBuilder(Material.ECHO_SHARD);
                    close.setCustomModelData(1017);
                    close.hideAllFlags();
                    close.setName(MessageManager.get(player, "inventory.item.close")
                            .getTranslated());

                    ItemBuilder info = new ItemBuilder(Material.ECHO_SHARD);
                    info.setCustomModelData(1018);
                    info.hideAllFlags();
                    info.setName(MessageManager.get(player, "inventory.info_item.kit_inventory.name")
                            .getTranslated());
                    info.setLore(MessageManager.get(player, "inventory.info_item.kit_inventory.lore").getTranslated());
                    if (kitManager.getGameMap() != null){
                        info.addLoreLine("");
                        info.addLoreLine("§8These kits are map-specific and not global.");
                    }


                    ItemBuilder reset = new ItemBuilder(Material.MAP);
                    reset.setCustomModelData(1010);
                    reset.hideAllFlags();
                    reset.setName(MessageManager.get(player, "inventory.kit.reset")
                            .getTranslated());
                    reset.removeLore();
                    MessageManager.get(player, "inventory.kit.reset_lore")
                            .addToItemLore(reset);



                    gui.appendElement(0, Component.element(close.toItemStack()).addClick(i -> {
                        gui.close(player);
                        player.playSound(player, Sounds.CLICK.bukkitSound(), 1F, 1F);
                    }).build());
                    gui.appendElement(8, Component.element(info.toItemStack()).build());

                    gui.setContainer((size <= 9 ? 20 : 29), Component.staticContainer()
                            .size(5, 1)
                            .init(container -> {
                                // Assign components to container
                                Element spacerElement = Component.element(reset.toItemStack()).addClick(i -> {
                                    if (playerData.getKit() != null) {
                                        playerData.getKit().unselect(gamePlayer, false);
                                        if (kitManager.getDefaultKit() != null){
                                            kitManager.getDefaultKit().select(gamePlayer);
                                        }
                                    }

                                    player.closeInventory();
                                    player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 10.0F, 10.0F);
                                    MessageManager.get(player, "chat.kit.reset")
                                            .send();
                                }).build();
                                container.fillElement(spacerElement);
                            }).build());

                    List<Kit> kits = kitManager.getKits();
                    kits.sort(Comparator.comparingInt((Kit kit) -> kit.equals(kitManager.getDefaultKit()) ? 0 : 1)
                            .thenComparing(Kit::getName));

                    gui.setContainer(9, Component.staticContainer()
                            .size(9, (size <= 9 ? 1 : 2))
                            .init(container -> {
                                for (Kit kit : kits) {
                                    //if (kitManager.getDefaultKit() != null && kitManager.getDefaultKit().equals(kit))
                                        //continue;

                                    String translateKey = "kit." + kit.getName().toLowerCase().replace(" ", "_");

                                    ItemBuilder item = new ItemBuilder(kit.getIcon());
                                    item.removeEnchantment(XEnchantment.ARROW_INFINITE.getEnchant());
                                    item.setName((balance >= kit.getPrice() || kit.getPrice() == 0 || kitManager.hasKitPermission(gamePlayer, kit) ? "§a§l" + WordUtils.capitalize(kit.getName().replace("_", " ")) : "§c§l" + kit.getName()));
                                    item.removeLore();
                                    if (kitManager.getGameMap() != null) {
                                        item.addLoreLine(MessageManager.get(gamePlayer, "inventory.kit.map")
                                                .replace("%map%", WordUtils.capitalize(WordUtils.capitalize(kitManager.getGameMap().replace("_", " "))))
                                                .getTranslated());
                                    }
                                    item.addLoreLine("");
                                    if (MessageManager.existMessage(translateKey)) {
                                        MessageManager.get(player, translateKey)
                                                .addToItemLore(item);
                                        item.addLoreLine("");
                                    }else{
                                        if (kit.getContent() != null) {
                                            if (kit.getContent().getContents() != null) {
                                                for (ItemStack kitItem : Arrays.stream(kit.getContent().getContents()).filter(Objects::nonNull).toList()) {
                                                    if (kitItem.getType().isAir()) continue;
                                                    item.addLoreLine("§7" + StringUtils.formatItemStackName(gamePlayer, kitItem));
                                                }
                                                item.addLoreLine("");
                                            }
                                        }
                                    }


                                    if (kit.getPrice() > 0) {
                                        if (player.hasPermission("kits.free")) {
                                            MessageManager.get(player, "inventory.kit.saved")
                                                    .replace("%price%", "" + kit.getPrice())
                                                    .replace("%economy_name%", resource.getName())
                                                    .addToItemLore(item);
                                        } else if ((playerData.getGame().getSettings().isEnabledChangingKitAfterStart() /*&& kitManager.getPurchasedThisGame().contains(gamePlayer)*/) //TODO: purchasedThisGame
                                                || (kitManager.isPurchaseKitForever() && kitManager.hasKitPermission(gamePlayer, kit))) {
                                            MessageManager.get(player, "inventory.kit.purchased_for")
                                                    .replace("%price%", StringUtils.betterNumberFormat(kit.getPrice()))
                                                    .replace("%economy_name%", resource.getName())
                                                    .addToItemLore(item);
                                        } else {
                                            MessageManager.get(player, "inventory.kit.price")
                                                    .replace("%balance%", (balance >= kit.getPrice() ? "§a" : "§c") + StringUtils.betterNumberFormat(balance))
                                                    .replace("%price%", StringUtils.betterNumberFormat(kit.getPrice()))
                                                    .replace("%economy_name%", resource.getName())
                                                    .addToItemLore(item);
                                        }
                                        item.addLoreLine("");
                                    }


                                    if (playerData.getKit() != null && playerData.getKit().equals(kit)) {
                                        item.addEnchant(XEnchantment.ARROW_INFINITE.getEnchant(), 1);
                                        MessageManager.get(player, "inventory.kit.selected")
                                                .addToItemLore(item);
                                    } else if ((playerData.getGame().getSettings().isEnabledChangingKitAfterStart() /*&& kit.getPurchasedThisGame().contains(gamePlayer)*/) //TODO: purchasedThisGame
                                            || (kitManager.isPurchaseKitForever() && kitManager.hasKitPermission(gamePlayer, kit))
                                            || player.hasPermission("kits.free")) {
                                        MessageManager.get(player, "inventory.kit.select")
                                                .addToItemLore(item);
                                    } else {
                                        item.addLoreLine(((kit.getPrice() == 0 || kitManager.hasKitPermission(gamePlayer, kit) || balance >= kit.getPrice()) ? MessageManager.get(player, "inventory.kit.purchase").getTranslated() : MessageManager.get(player, "inventory.kit.dont_have_enough").replace("%economy_name%", resource.getName()).getTranslated()));
                                    }


                                    if (GameAPI.getInstance().getMinigame().getSettings().isAllowDefaultKitSelection()) {
                                        if (playerData.getDefaultKit() == null) {
                                            if (kitManager.isPurchaseKitForever()) {
                                                if (kitManager.hasKitPermission(gamePlayer, kit)) {
                                                    MessageManager.get(player, "inventory.kit.default_kit.select")
                                                            .addToItemLore(item);
                                                }
                                            } else {
                                                MessageManager.get(player, "inventory.kit.default_kit.select")
                                                        .addToItemLore(item);
                                            }
                                        } else if (playerData.getDefaultKit().equals(kit)) {
                                            MessageManager.get(player, "inventory.kit.default_kit.selected")
                                                    .addToItemLore(item);
                                        } else {
                                            MessageManager.get(player, "inventory.kit.default_kit.select")
                                                    .addToItemLore(item);
                                        }
                                    }

                                    MessageManager.get(player, "inventory.kit.edit_inventory.action_info")
                                            .addToItemLore(item);

                                    item.hideAllFlags();


                                    Element spacerElement = Component.element(item.toItemStack()).addClick(i -> {
                                        if (i.getClickType().isLeftClick()) {
                                            if (game.getState() == GameState.STARTING || game.getState() == GameState.WAITING) {
                                                kit.select(gamePlayer);
                                            } else {
                                                kit.select(gamePlayer); //bylo false k tomu
                                                if (gamePlayer.getPlayerData().getKit() == kit) {
                                                    kit.activate(gamePlayer);
                                                }
                                            }
                                            player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 20.0F, 20.0F);
                                            player.closeInventory();
                                        } else {
                                            if (i.getClickType().isShiftClick()){
                                                //KitInventoryEditor.setKitInventory(gamePlayer, kit);
                                                KitInventoryEditor.setKitInventory(gamePlayer, kit);
                                                return;
                                            }


                                            if (GameAPI.getInstance().getMinigame().getSettings().isAllowDefaultKitSelection()) {
                                                if (kitManager.isPurchaseKitForever()) {
                                                    if (!kitManager.hasKitPermission(gamePlayer, kit)) {
                                                        player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 20.0F, 20.0F);
                                                        MessageManager.get(player, "chat.kit.default_kit.must_have_purchased")
                                                                .send();
                                                        player.closeInventory();
                                                        return;
                                                    }
                                                }
                                                gamePlayer.getPlayerData().setDefaultKit(kit);
                                                player.playSound(player.getLocation(), Sounds.CLICK.bukkitSound(), 20.0F, 20.0F);
                                                if (game.getState() == GameState.STARTING || game.getState() == GameState.WAITING) {
                                                    kit.select(gamePlayer);
                                                } else {
                                                    kit.select(gamePlayer); //bylo false k tomu
                                                    if (gamePlayer.getPlayerData().getKit() == kit) {
                                                        kit.activate(gamePlayer);
                                                    }
                                                }
                                                MessageManager.get(player, "chat.kit.default_kit.select")
                                                        .replace("%kit%", kit.getName())
                                                        .send();
                                                player.closeInventory();
                                            }
                                        }
                                    }).build();
                                    container.appendElement(spacerElement);
                                }
                            }).build());
                })
                .build();

        inventory.open(gamePlayer.getOnlinePlayer());
    }



    /*@Override
    public void build(Player player) {
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        KitManager kitManager = GameAPI.getInstance().getKitManager();
        Economy economy = kitManager.getEconomy();
        PlayerData playerData = gamePlayer.getPlayerData();
        int balance = playerData.getBalance(economy);


        ItemBuilder reset = new ItemBuilder(Material.MAP);
        reset.setCustomModelData(1010);
        reset.hideAllFlags();
        reset.setName(MessageManager.get(player, "inventory.kit.reset")
                .getTranslated());
        reset.removeLore();
        MessageManager.get(player, "inventory.kit.reset_lore")
                .addToItemLore(reset);

        setContainer(47, Component.staticContainer()
                .size(5, 1)
                .init(container -> {
                    // Assign components to container
                    Element spacerElement = Component.element(reset.toItemStack()).addClick(i -> {
                        if (playerData.getKit() != null) {
                            playerData.getKit().unselect(gamePlayer);
                            if (kitManager.getDefaultKit() != null){
                                kitManager.getDefaultKit().select(gamePlayer);
                            }
                        }

                        player.closeInventory();
                        player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 10.0F, 10.0F);
                        MessageManager.get(player, "chat.kit.reset")
                                .send();
                    }).build();
                    container.fillElement(spacerElement);
                }).build());


        for (Kit kit : kitManager.getKits()) {
            if (kitManager.getDefaultKit() != null && kitManager.getDefaultKit().equals(kit)) {
                continue;
            }
            String translateKey = "kit." + kit.getName().replace(" ", "_");

            ItemBuilder item = new ItemBuilder(kit.getIcon());
            item.hideAllFlags();

            item.removeEnchantment(XEnchantment.ARROW_INFINITE.getEnchant());
            item.setName((balance >= kit.getPrice() || kit.getPrice() == 0 || kitManager.hasKitPermission(gamePlayer, kit) ? "§a§l" + kit.getName() : "§c§l" + kit.getName()));
            item.removeLore();
            item.addLoreLine("");
            if (MessageManager.existMessage(translateKey)) {
                MessageManager.get(player, translateKey)
                        .addToItemLore(item);
                item.addLoreLine("");
            }


            if (player.hasPermission("kits.free")) {
                MessageManager.get(player, "inventory.kit.saved")
                        .replace("%price%", "" + kit.getPrice())
                        .replace("%economy_name%", economy.getName())
                        .addToItemLore(item);
            } else if ((playerData.getGame().getSettings().isEnabledChangingKitAfterStart() /*&& kitManager.getPurchasedThisGame().contains(gamePlayer)*) //TODO: purchasedThisGame
                    || (kit.getKitManager().isPurchaseKitForever() && kitManager.hasKitPermission(gamePlayer, kit))) {
                MessageManager.get(player, "inventory.kit.purchased_for")
                        .replace("%price%", (kit.getPrice() == 0 ? "" + 0 : "" + kit.getPrice()))
                        .replace("%economy_name%", economy.getName())
                        .addToItemLore(item);
            } else {
                MessageManager.get(player, "inventory.kit.price")
                        .replace("%price%", (kit.getPrice() == 0 ? "" + 0 : "" + kit.getPrice()))
                        .replace("%economy_name%", economy.getName())
                        .addToItemLore(item);
            }

            item.addLoreLine("");


            if (playerData.getKit() != null && playerData.getKit().equals(kit)) {
                item.addEnchant(XEnchantment.ARROW_INFINITE.getEnchant(), 1);
                MessageManager.get(player, "inventory.kit.selected")
                        .addToItemLore(item);
            } else if ((playerData.getGame().getSettings().isEnabledChangingKitAfterStart() /*&& kit.getPurchasedThisGame().contains(gamePlayer)*) //TODO: purchasedThisGame
                    || (kit.getKitManager().isPurchaseKitForever() && kitManager.hasKitPermission(gamePlayer, kit))
                    || player.hasPermission("kits.free")) {
                MessageManager.get(player, "inventory.kit.select")
                        .addToItemLore(item);
            } else {
                item.addLoreLine(((kit.getPrice() == 0 || kitManager.hasKitPermission(gamePlayer, kit) || balance >= kit.getPrice()) ? MessageManager.get(player, "inventory.kit.purchase").getTranslated() : MessageManager.get(player, "inventory.kit.dont_have_enough").replace("%economy_name%", economy.getName()).getTranslated()));
            }


            if (playerData.getDefaultKit() == null) {
                if (kit.getKitManager().isPurchaseKitForever()) {
                    if (kitManager.hasKitPermission(gamePlayer, kit)) {
                        MessageManager.get(player, "inventory.kit.default_kit.select")
                                .addToItemLore(item);
                    }
                } else {
                    MessageManager.get(player, "inventory.kit.default_kit.select")
                            .addToItemLore(item);
                }
            } else if (playerData.getDefaultKit().equals(kit)) {
                MessageManager.get(player, "inventory.kit.default_kit.selected")
                        .addToItemLore(item);
            } else {
                MessageManager.get(player, "inventory.kit.default_kit.select")
                        .addToItemLore(item);
            }


            setContainer(10, Component.staticContainer()
                    .size(7, 2)
                    .init(container -> {
                        // Assign components to container
                        Element spacerElement = Component.element(reset.toItemStack()).addClick(i -> {
                            if (playerData.getKit() != null) {
                                playerData.getKit().unselect(gamePlayer);
                                if (kitManager.getDefaultKit() != null){
                                    kitManager.getDefaultKit().select(gamePlayer);
                                }
                            }

                            player.closeInventory();
                            player.playSound(player.getLocation(), Sounds.ANVIL_BREAK.bukkitSound(), 10.0F, 10.0F);
                            MessageManager.get(player, "chat.kit.reset")
                                    .send();
                        }).build();
                        container.fillElement(spacerElement);
                    }).build());
        }
    }*/
}
